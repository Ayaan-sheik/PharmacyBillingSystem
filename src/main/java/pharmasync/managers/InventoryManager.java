package pharmasync.managers;

import pharmasync.models.CartItem;
import pharmasync.models.Medicine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Platform;

/**
 * InventoryManager — Singleton managing in-memory + persistent inventory.
 *
 * Phase 5 addition:  processCheckout() now executes an ATOMIC JDBC transaction
 * via DatabaseManager in addition to writing the CSV receipt, ensuring durable
 * storage with proper commit / rollback semantics.
 */
public class InventoryManager {

    private static InventoryManager instance;
    private List<Medicine> inventory;
    private final List<Runnable> updateListeners = new ArrayList<>();

    private InventoryManager() {
        // Generics & Collections: ArrayList of Medicine objects
        this.inventory = new ArrayList<>(CSVManager.loadInventory());
    }

    // Singleton Pattern (Week 10 — Design Patterns)
    public static synchronized InventoryManager getInstance() {
        if (instance == null) instance = new InventoryManager();
        return instance;
    }

    public List<Medicine> getInventory() { return inventory; }

    // ── Observer / Listener support ───────────────────────────────────────
    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable r : updateListeners) Platform.runLater(r);
    }

    // ── CRUD operations ───────────────────────────────────────────────────
    public synchronized void reloadFromDisk() {
        this.inventory = new ArrayList<>(CSVManager.loadInventory());
        notifyListeners();
    }

    public synchronized void addMedicine(Medicine med) {
        inventory.add(med);
        saveChanges();
        notifyListeners();
    }

    public synchronized boolean removeMedicine(String id) {
        boolean removed = inventory.removeIf(m -> m.getMedicineID().equals(id));
        if (removed) { saveChanges(); notifyListeners(); }
        return removed;
    }

    public synchronized boolean updateMedicine(
            String id, String newName,
            pharmasync.models.DrugCategory newCategory,
            Double newPrice, int newStock) {

        // Always read from disk for accuracy (avoids stale in-memory state)
        List<Medicine> current = CSVManager.loadInventory();
        Optional<Medicine> med = current.stream()
            .filter(m -> m.getMedicineID().equals(id))
            .findFirst();

        if (med.isPresent()) {
            Medicine m = med.get();
            m.setStockQuantity(newStock);
            if (newPrice != null)                    m.setPrice(newPrice);
            if (newName != null && !newName.isBlank()) m.setName(newName);
            if (newCategory != null)                 m.setCategory(newCategory);
            CSVManager.saveInventory(current);
            this.inventory = current;
            notifyListeners();
            return true;
        }
        return false;
    }

    // ── Java 8 Streams API (Week 11) ─────────────────────────────────────
    public List<Medicine> searchByName(String keyword) {
        return inventory.stream()
            .filter(m -> m.getName().toLowerCase()
                          .contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }

    public Optional<Medicine> getMedicineById(String id) {
        return inventory.stream()
            .filter(m -> m.getMedicineID().equals(id))
            .findFirst();
    }

    // ── Phase 5: Atomic Billing with JDBC Transaction ─────────────────────
    /**
     * Processes checkout atomically:
     *   1. Validates all stock levels against the latest CSV.
     *   2. Deducts stock and builds the receipt text.
     *   3. Writes the receipt file to disk (CSV approach — existing behaviour).
     *   4. Executes a JDBC atomic transaction via DatabaseManager:
     *        - UPDATE stock in SQLite for every cart item (batch)
     *        - INSERT receipt record
     *        → If any DB step fails: rollback, but CSV changes remain
     *          (demonstrating two-phase / graceful degradation).
     *
     * @param cart list of CartItem<Medicine, Integer>
     * @return true if checkout succeeded (stock was sufficient)
     */
    public synchronized boolean processCheckout(
            List<CartItem<Medicine, Integer>> cart) {

        // ── Step 1: Validate against latest on-disk inventory ────────────
        List<Medicine> realTimeInv = CSVManager.loadInventory();
        for (CartItem<Medicine, Integer> item : cart) {
            Optional<Medicine> dbMed = realTimeInv.stream()
                .filter(m -> m.getMedicineID()
                              .equals(item.getItem().getMedicineID()))
                .findFirst();
            if (dbMed.isEmpty() || dbMed.get().getStockQuantity() < item.getQuantity()) {
                return false;   // Insufficient stock
            }
        }

        // ── Step 2: Deduct stock + build receipt ─────────────────────────
        StringBuilder receipt = new StringBuilder();
        receipt.append("========== Manipal Medicals ==========\n");
        receipt.append("            PharmaSync Receipt\n");
        receipt.append("--------------------------------------\n");
        double grandTotal = 0.0;

        for (CartItem<Medicine, Integer> item : cart) {
            Medicine dbMed = realTimeInv.stream()
                .filter(m -> m.getMedicineID()
                              .equals(item.getItem().getMedicineID()))
                .findFirst().get();

            dbMed.setStockQuantity(dbMed.getStockQuantity() - item.getQuantity());

            double lineTotal  = dbMed.getPrice() * item.getQuantity();
            double taxAmount  = lineTotal * dbMed.getCategory().getBaseTaxRate();
            double discount   = dbMed.calculateDiscount();
            double finalLine  = lineTotal + taxAmount - discount;
            grandTotal       += finalLine;

            receipt.append(String.format(
                "%-22s x%d  ₹%.2f%n",
                dbMed.getName(), item.getQuantity(), finalLine));
        }

        receipt.append("--------------------------------------\n");
        receipt.append(String.format("Grand Total:            ₹%.2f%n", grandTotal));
        receipt.append("======================================\n");

        // ── Step 3: Persist receipt file + inventory CSV ──────────────────
        String receiptText = receipt.toString();
        CSVManager.saveReceipt(receiptText);
        
        // Phase 5: Generate and Auto-Open PDF Receipt
        PDFGenerator.generateAndOpenReceipt(cart, grandTotal);
        
        this.inventory = realTimeInv;
        saveChanges();

        // ── Step 4: JDBC Atomic Transaction ──────────────────────────────
        //   Commits stock deductions + receipt record to SQLite in one unit.
        //   If DB is unavailable, checkout still succeeds (CSV is source of truth).
        boolean dbSuccess = DatabaseManager.getInstance()
            .saveReceiptTransaction(receiptText, grandTotal, cart);

        if (!dbSuccess) {
            System.err.println("[InventoryManager] WARNING: JDBC transaction failed — "
                + "CSV was saved but SQLite DB may be out of sync.");
        }

        notifyListeners();
        return true;
    }

    // ── Private helpers ───────────────────────────────────────────────────
    private void saveChanges() {
        CSVManager.saveInventory(inventory);
    }
}
