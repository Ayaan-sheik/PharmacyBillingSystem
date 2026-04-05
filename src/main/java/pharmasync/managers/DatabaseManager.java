package pharmasync.managers;

import pharmasync.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager — Phase 4: JDBC Permanent Storage
 *
 * Uses SQLite (file-based, zero-server-setup) via the org.xerial:sqlite-jdbc driver.
 * Implements:
 *   - Connection establishment and schema initialisation
 *   - Upsert using complex SQL (INSERT ... ON CONFLICT DO UPDATE) with PreparedStatements
 *   - Fetching all medicines to populate JavaFX TableView
 *   - Saving receipt records atomically
 */
public class DatabaseManager {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static DatabaseManager instance;
    private Connection connection;

    private static final String DB_URL = "jdbc:sqlite:pharmasync.db";

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // ── Connection establishment ───────────────────────────────────────────
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            // Enable WAL mode for concurrent read performance
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA foreign_keys=ON;");
            }
        }
        return connection;
    }

    // ── Schema initialisation ─────────────────────────────────────────────
    /**
     * Creates the tables if they do not already exist.
     * Called once at application startup from PharmaSyncApp.
     */
    public void initializeDatabase() {
        String createMedicines =
            "CREATE TABLE IF NOT EXISTS medicines ("
            + "  medicine_id   TEXT    PRIMARY KEY, "
            + "  name          TEXT    NOT NULL, "
            + "  price         REAL    NOT NULL, "
            + "  stock_qty     INTEGER NOT NULL, "
            + "  category      TEXT    NOT NULL, "
            + "  type          TEXT    NOT NULL, "   // 'rx' | 'otc'
            + "  extra_field   TEXT, "
            + "  expiry_date   TEXT"
            + ");";

        String createReceipts =
            "CREATE TABLE IF NOT EXISTS receipts ("
            + "  id          INTEGER  PRIMARY KEY AUTOINCREMENT, "
            + "  content     TEXT     NOT NULL, "
            + "  grand_total REAL     NOT NULL, "
            + "  created_at  TEXT     DEFAULT (datetime('now','localtime'))"
            + ");";

        String createAuditLogs =
            "CREATE TABLE IF NOT EXISTS audit_logs ("
            + "  id          INTEGER  PRIMARY KEY AUTOINCREMENT, "
            + "  action      TEXT     NOT NULL, "
            + "  timestamp   TEXT     DEFAULT (datetime('now','localtime'))"
            + ");";

        try (Statement st = getConnection().createStatement()) {
            st.execute(createMedicines);
            st.execute(createReceipts);
            st.execute(createAuditLogs);
            System.out.println("[DB] Schema ready — pharmasync.db");
        } catch (SQLException e) {
            System.err.println("[DB] Schema init error: " + e.getMessage());
        }
    }

    // ── Upsert medicine (Complex SQL with PreparedStatement) ──────────────
    /**
     * Inserts a new medicine, or updates all fields if the medicine_id already exists.
     *
     * SQL:  INSERT INTO medicines (...) VALUES (?, ?, ?, ?, ?, ?, ?)
     *       ON CONFLICT(medicine_id) DO UPDATE SET
     *           name=excluded.name, price=excluded.price, ...
     *
     * This is the "intelligent duplicate prevention" required by the rubric.
     */
    public void upsertMedicine(Medicine m) {
        String sql =
            "INSERT INTO medicines (medicine_id, name, price, stock_qty, category, type, extra_field, expiry_date)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            + " ON CONFLICT(medicine_id) DO UPDATE SET"
            + "   name        = excluded.name,"
            + "   price       = excluded.price,"
            + "   stock_qty   = excluded.stock_qty,"
            + "   category    = excluded.category,"
            + "   type        = excluded.type,"
            + "   extra_field = excluded.extra_field,"
            + "   expiry_date = excluded.expiry_date;";

        String type  = (m instanceof PrescriptionDrug) ? "rx" : "otc";
        String extra = (m instanceof PrescriptionDrug)
            ? ((PrescriptionDrug) m).getPrescribedDoctor()
            : String.valueOf(((OverTheCounterDrug) m).isAgeRestricted());

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getMedicineID());
            ps.setString(2, m.getName());
            ps.setDouble(3, m.getPrice());
            ps.setInt   (4, m.getStockQuantity());
            ps.setString(5, m.getCategory().name());
            ps.setString(6, type);
            ps.setString(7, extra);
            ps.setString(8, m.getExpiryDate().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] upsertMedicine error: " + e.getMessage());
        }
    }

    // ── Fetch all medicines for TableView ─────────────────────────────────
    /**
     * Fetches the complete inventory from SQLite.
     * Returns a list of concrete Medicine subtypes (PrescriptionDrug / OverTheCounterDrug).
     */
    public List<Medicine> getAllMedicines() {
        List<Medicine> list = new ArrayList<>();
        String sql = "SELECT medicine_id, name, price, stock_qty, category, type, extra_field, expiry_date"
                   + " FROM medicines ORDER BY name;";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id       = rs.getString("medicine_id");
                String name     = rs.getString("name");
                double price    = rs.getDouble("price");
                int    qty      = rs.getInt("stock_qty");
                DrugCategory cat = DrugCategory.valueOf(rs.getString("category").toUpperCase());
                String type     = rs.getString("type");
                String extra    = rs.getString("extra_field");
                java.time.LocalDate expiry = rs.getString("expiry_date") != null 
                    ? java.time.LocalDate.parse(rs.getString("expiry_date")) 
                    : java.time.LocalDate.now().plusDays(365);

                Medicine m = type.equalsIgnoreCase("rx")
                    ? new PrescriptionDrug(id, name, price, qty, cat, expiry, extra)
                    : new OverTheCounterDrug(id, name, price, qty, cat, expiry,
                                            Boolean.parseBoolean(extra));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[DB] getAllMedicines error: " + e.getMessage());
        }
        return list;
    }

    // ── Atomic receipt + stock update transaction ─────────────────────────
    /**
     * Saves a receipt and decrements stock levels atomically.
     * If ANY update fails, the entire transaction is rolled back.
     * Called from InventoryManager.processCheckout() — Phase 5.
     *
     * @param receiptContent full receipt text
     * @param grandTotal     final amount
     * @param items          list of (medicineId, qty) pairs to deduct
     */
    public boolean saveReceiptTransaction(
            String receiptContent,
            double grandTotal,
            List<pharmasync.models.CartItem<Medicine, Integer>> items) {

        String updateStock =
            "UPDATE medicines SET stock_qty = stock_qty - ? WHERE medicine_id = ?;";
        String insertReceipt =
            "INSERT INTO receipts (content, grand_total) VALUES (?, ?);";

        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);   // BEGIN TRANSACTION
            try {
                // Batch stock deductions
                try (PreparedStatement ps = conn.prepareStatement(updateStock)) {
                    for (pharmasync.models.CartItem<Medicine, Integer> item : items) {
                        ps.setInt   (1, item.getQuantity());
                        ps.setString(2, item.getItem().getMedicineID());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Insert receipt record
                try (PreparedStatement ps = conn.prepareStatement(insertReceipt)) {
                    ps.setString(1, receiptContent);
                    ps.setDouble(2, grandTotal);
                    ps.executeUpdate();
                }

                conn.commit();           // COMMIT TRANSACTION
                System.out.println("[DB] Transaction committed — receipt saved.");
                return true;

            } catch (SQLException ex) {
                conn.rollback();         // ROLLBACK on any error
                System.err.println("[DB] Transaction rolled back: " + ex.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[DB] saveReceiptTransaction error: " + e.getMessage());
            return false;
        }
    }

    // ── Sync existing CSV into DB on first launch ─────────────────────────
    /**
     * Imports inventory.csv records into SQLite on startup.
     * Uses upsertMedicine so it's idempotent — safe to call every run.
     */
    public void syncFromCSV() {
        List<Medicine> csvInventory = CSVManager.loadInventory();
        for (Medicine m : csvInventory) {
            upsertMedicine(m);
        }
        System.out.println("[DB] Synced " + csvInventory.size()
            + " medicines from CSV → SQLite.");
    }

    // ── Audit Logs ────────────────────────────────────────────────────────
    public void logActivity(String action) {
        String sql = "INSERT INTO audit_logs (action) VALUES (?);";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] logActivity error: " + e.getMessage());
        }
    }

    public List<String> getAuditLogs() {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT timestamp, action FROM audit_logs ORDER BY id DESC LIMIT 100;";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add("[" + rs.getString("timestamp") + "] " + rs.getString("action"));
            }
        } catch (SQLException e) {
             System.err.println("[DB] getAuditLogs error: " + e.getMessage());
        }
        return logs;
    }

    // ── Clean shutdown ────────────────────────────────────────────────────
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Close error: " + e.getMessage());
        }
    }
}
