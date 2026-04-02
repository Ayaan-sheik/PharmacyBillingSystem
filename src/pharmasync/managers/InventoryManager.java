package pharmasync.managers;

import pharmasync.models.CartItem;
import pharmasync.models.Medicine;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Platform;

public class InventoryManager {
    private static InventoryManager instance;
    private List<Medicine> inventory;
    private List<Runnable> updateListeners = new ArrayList<>();

    private InventoryManager() {
        // Generics & Collections: ArrayList of Medicines
        this.inventory = new ArrayList<>(CSVManager.loadInventory());
    }

    public static synchronized InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    public List<Medicine> getInventory() {
        return inventory;
    }

    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable r : updateListeners) {
            Platform.runLater(r);
        }
    }

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
        if (removed) {
            saveChanges();
            notifyListeners();
        }
        return removed;
    }

    public synchronized boolean updateMedicine(String id, String newName, pharmasync.models.DrugCategory newCategory, Double newPrice, int newStock) {
        List<Medicine> currentInv = CSVManager.loadInventory();
        Optional<Medicine> med = currentInv.stream()
                .filter(m -> m.getMedicineID().equals(id))
                .findFirst();
        
        if (med.isPresent()) {
            med.get().setStockQuantity(newStock);
            if (newPrice != null) {
                med.get().setPrice(newPrice);
            }
            if (newName != null && !newName.trim().isEmpty()) {
                med.get().setName(newName);
            }
            if (newCategory != null) {
                med.get().setCategory(newCategory);
            }
            CSVManager.saveInventory(currentInv);
            this.inventory = currentInv;
            notifyListeners();
            return true;
        }
        return false;
    }

    // Advanced Feature: Java 8 Streams API for searching
    public List<Medicine> searchByName(String keyword) {
        return inventory.stream()
                .filter(m -> m.getName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Optional<Medicine> getMedicineById(String id) {
        return inventory.stream()
                .filter(m -> m.getMedicineID().equals(id))
                .findFirst();
    }

    // Multithreading Synchronization: Ensuring billing deducts safely
    public synchronized boolean processCheckout(List<CartItem<Medicine, Integer>> cart) {
        // Load latest inventory explicitly to prevent multi-instance race conditions
        List<Medicine> realTimeInv = CSVManager.loadInventory();

        // Validate stock against the latest loaded inventory
        for (CartItem<Medicine, Integer> item : cart) {
            Medicine requestMed = item.getItem();
            Optional<Medicine> dbMed = realTimeInv.stream()
                .filter(m -> m.getMedicineID().equals(requestMed.getMedicineID()))
                .findFirst();

            if (dbMed.isEmpty() || dbMed.get().getStockQuantity() < item.getQuantity()) {
                return false; // Insufficient stock or item missing
            }
        }

        // Deduct logic
        StringBuilder receipt = new StringBuilder();
        receipt.append("---- PharmaSync Receipt ----\n");
        double total = 0.0;

        for (CartItem<Medicine, Integer> item : cart) {
            Medicine requestMed = item.getItem();
            Medicine dbMed = realTimeInv.stream()
                .filter(m -> m.getMedicineID().equals(requestMed.getMedicineID()))
                .findFirst().get();

            dbMed.setStockQuantity(dbMed.getStockQuantity() - item.getQuantity());
            
            double lineTotal = dbMed.getPrice() * item.getQuantity();
            double finalPrice = lineTotal - dbMed.calculateDiscount() + (lineTotal * dbMed.getCategory().getBaseTaxRate());
            total += finalPrice;

            receipt.append(String.format("%s x%d: $%.2f\n", dbMed.getName(), item.getQuantity(), finalPrice));
        }

        receipt.append(String.format("Grand Total: $%.2f\n", total));
        receipt.append("-----------------------------\n");
        
        CSVManager.saveReceipt(receipt.toString());
        this.inventory = realTimeInv; 
        saveChanges();
        notifyListeners();
        return true;
    }

    private void saveChanges() {
        CSVManager.saveInventory(inventory);
    }
}
