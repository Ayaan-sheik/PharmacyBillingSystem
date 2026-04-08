package pharmasync.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import pharmasync.managers.DatabaseManager;
import pharmasync.managers.InventoryManager;
import pharmasync.managers.SessionManager;
import pharmasync.models.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminDashboardController {

    /* ── Top bar ── */
    @FXML
    private Label lblUserInfo;

    /* ── Navigation / Tabs ── */
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab tabManage;
    @FXML
    private Tab tabAnalytics;
    @FXML
    private Tab tabAudit;

    /* ── Inventory TableView ── */
    @FXML
    private TableView<Medicine> adminTable;
    @FXML
    private TableColumn<Medicine, String> colId;
    @FXML
    private TableColumn<Medicine, String> colName;
    @FXML
    private TableColumn<Medicine, String> colCategory;
    @FXML
    private TableColumn<Medicine, Double> colPrice;
    @FXML
    private TableColumn<Medicine, Integer> colStock;
    @FXML
    private TableColumn<Medicine, String> colType;

    /* ── Inventory Management ── */
    @FXML
    private TextField txtAdminSearch;
    @FXML
    private TextField tfId;
    @FXML
    private TextField tfName;
    @FXML
    private TextField tfPrice;
    @FXML
    private TextField tfQty;
    @FXML
    private TextField tfDoctor;
    @FXML
    private ComboBox<DrugCategory> cbCategory;
    @FXML
    private ComboBox<String> cbType;
    @FXML
    private Label lblStatus;

    /* ── Charts & Filters ── */
    @FXML
    private PieChart pieChart;
    @FXML
    private BarChart<String, Number> revenueChart;

    /* ── Audit Logs ── */
    @FXML
    private ListView<String> lvAuditLogs;

    /*
     * ═══════════════════════════════════════════
     * initialize() — called automatically by FXML
     * ═══════════════════════════════════════════
     */
    @FXML
    private void initialize() {
        // Current user info
        if (SessionManager.getInstance().getCurrentUser() != null) {
            lblUserInfo.setText("Logged in as: " +
                    SessionManager.getInstance().getCurrentUser().getUsername());
        }

        setupTableColumns();
        setupFormCombos();
        refreshTable();
        loadCharts();

        // Live Search
        if (txtAdminSearch != null) {
            txtAdminSearch.textProperty().addListener((obs, old, keyword) -> {
                adminTable.setItems(FXCollections.observableArrayList(
                        InventoryManager.getInstance().searchByName(keyword)));
            });
        }

        // Auto-fill form when a row is selected
        adminTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null)
                        fillFormFrom(selected);
                });

        // Live-reload from InventoryManager listener
        InventoryManager.getInstance().addUpdateListener(this::refreshTable);
    }

    /* ─── Table column wiring ─── */
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("medicineID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getCategory().name()));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        colType.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                (cell.getValue() instanceof PrescriptionDrug) ? "Rx" : "OTC"));

        // Smart Inventory Highlights (Low Stock + Expiry)
        adminTable.setRowFactory(tv -> {
            TableRow<Medicine> row = new TableRow<>() {
                @Override
                protected void updateItem(Medicine item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("row-low-stock", "row-expiring");
                    if (item != null && !empty) {
                        if (item.getStockQuantity() < 20) {
                            getStyleClass().add("row-low-stock");
                        } else if (item.getExpiryDate() != null
                                && item.getExpiryDate().isBefore(java.time.LocalDate.now().plusDays(30))) {
                            getStyleClass().add("row-expiring");
                        }
                    }
                }
            };
            return row;
        });
    }

    /* ─── Combo boxes ─── */
    private void setupFormCombos() {
        cbCategory.setItems(FXCollections.observableArrayList(DrugCategory.values()));
        cbCategory.getSelectionModel().selectFirst();
        cbType.setItems(FXCollections.observableArrayList("OTC", "Rx (Prescription)"));
        cbType.getSelectionModel().selectFirst();
    }

    /* ─── Populate form from selected row ─── */
    private void fillFormFrom(Medicine m) {
        tfId.setText(m.getMedicineID());
        tfName.setText(m.getName());
        tfPrice.setText(String.valueOf(m.getPrice()));
        tfQty.setText(String.valueOf(m.getStockQuantity()));
        cbCategory.setValue(m.getCategory());
        if (m instanceof PrescriptionDrug) {
            cbType.setValue("Rx (Prescription)");
            tfDoctor.setText(((PrescriptionDrug) m).getPrescribedDoctor());
        } else {
            cbType.setValue("OTC");
            tfDoctor.clear();
        }
    }

    /*
     * ═══════════════════════════════════════════
     * HANDLER — Add or Update medicine
     * Intelligent duplicate prevention via upsert
     * ═══════════════════════════════════════════
     */
    @FXML
    private void handleAddOrUpdate() {
        try {
            String id = tfId.getText().trim();
            String name = tfName.getText().trim();
            String doctor = tfDoctor.getText().trim();
            double price = Double.parseDouble(tfPrice.getText().trim());
            int qty = Integer.parseInt(tfQty.getText().trim());
            DrugCategory cat = cbCategory.getValue();
            boolean isRx = cbType.getValue() != null &&
                    cbType.getValue().startsWith("Rx");

            if (id.isEmpty() || name.isEmpty()) {
                setStatus("⚠ ID and Name are required.", true);
                return;
            }

            Optional<Medicine> existing = InventoryManager.getInstance().getMedicineById(id);

            if (existing.isPresent()) {
                // UPDATE — intelligent duplicate prevention
                InventoryManager.getInstance()
                        .updateMedicine(id, name, cat, price, qty);
                existing.get().setExpiryDate(java.time.LocalDate.now().plusDays(365)); // Simple update mock
                DatabaseManager.getInstance()
                        .upsertMedicine(existing.get());
                DatabaseManager.getInstance().logActivity("Admin updated medicine " + id + " (" + name + ")");
                setStatus("✔ Medicine '" + id + "' updated successfully.", false);
            } else {
                // INSERT — new medicine
                Medicine med = isRx
                        ? new PrescriptionDrug(id, name, price, qty, cat, java.time.LocalDate.now().plusDays(365),
                                doctor.isEmpty() ? "N/A" : doctor)
                        : new OverTheCounterDrug(id, name, price, qty, cat, java.time.LocalDate.now().plusDays(365),
                                false);
                InventoryManager.getInstance().addMedicine(med);
                DatabaseManager.getInstance().upsertMedicine(med);
                DatabaseManager.getInstance().logActivity("Admin added new medicine " + id + " (" + name + ")");
                setStatus("✔ Medicine '" + name + "' added to inventory.", false);
            }
            refreshTable();

        } catch (NumberFormatException ex) {
            setStatus("⚠ Price and Quantity must be valid numbers.", true);
        } catch (Exception ex) {
            setStatus("⚠ Error: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        Medicine selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("⚠ Select a medicine row first.", true);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete '" + selected.getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                InventoryManager.getInstance().removeMedicine(selected.getMedicineID());
                DatabaseManager.getInstance().logActivity("Admin deleted medicine " + selected.getMedicineID());
                handleClear();
                setStatus("✔ Medicine deleted.", false);
            }
        });
    }

    @FXML
    private void handleClear() {
        tfId.clear();
        tfName.clear();
        tfPrice.clear();
        tfQty.clear();
        tfDoctor.clear();
        cbCategory.getSelectionModel().selectFirst();
        cbType.getSelectionModel().selectFirst();
        adminTable.getSelectionModel().clearSelection();
        lblStatus.setText("");
    }

    /* ─── Tab navigation from sidebar buttons ─── */
    @FXML
    private void showInventoryTab() {
        tabPane.getSelectionModel().select(tabManage);
    }

    @FXML
    private void showAnalyticsTab() {
        loadCharts();
        tabPane.getSelectionModel().select(tabAnalytics);
    }

    @FXML
    private void refreshAuditLogs() {
        if (lvAuditLogs != null) {
            lvAuditLogs.setItems(FXCollections.observableArrayList(
                    DatabaseManager.getInstance().getAuditLogs()));
        }
    }

    /* ─── Logout & Theme ─── */
    private boolean isDarkMode = false;

    @FXML
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        Scene scene = tabPane.getScene();
        if (scene != null && scene.getRoot() != null) {
            scene.getRoot().getStylesheets().clear();
            String css = getClass().getResource(isDarkMode ? "styles-dark.css" : "styles.css").toExternalForm();
            scene.getRoot().getStylesheets().add(css);
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("LoginScreen.fxml"));
            Parent root = loader.load();
            PharmaSyncApp.getMainStage().setScene(new Scene(root, 600, 450));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ─── Table refresh ─── */
    private void refreshTable() {
        ObservableList<Medicine> data = FXCollections.observableArrayList(
                InventoryManager.getInstance().getInventory());
        adminTable.setItems(data);
        adminTable.refresh();
    }

    private void loadCharts() {
        pieChart.setAnimated(false);
        revenueChart.setAnimated(false);

        // ── PieChart — stock by category ──────────────────────────────────────
        pieChart.getData().clear();
        pieChart.setLegendVisible(false);   // FIX: removes the cluttered left-side legend

        Map<pharmasync.models.DrugCategory, Integer> catStock =
            InventoryManager.getInstance().getInventory().stream()
                .collect(Collectors.groupingBy(
                    Medicine::getCategory,
                    Collectors.summingInt(Medicine::getStockQuantity)));

        catStock.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .forEach(e -> {
                PieChart.Data slice = new PieChart.Data(
                    e.getKey().name() + "\n" + e.getValue() + " units", e.getValue());
                pieChart.getData().add(slice);
            });

        // Add tooltips to each slice after data is committed to the chart
        javafx.application.Platform.runLater(() -> {
            for (PieChart.Data slice : pieChart.getData()) {
                Tooltip tip = new Tooltip(slice.getName() + "\n"
                    + (int) slice.getPieValue() + " units");
                Tooltip.install(slice.getNode(), tip);
            }
        });

        // ── BarChart — revenue from SQLite receipts ────────────────────────────
        // FIX: PDFGenerator saves Receipt_*.pdf into invoices/, NOT .txt files.
        // Reading from DatabaseManager (receipts table) which stores structured data.
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (₹)");

        // Pull receipt content rows from DB and parse them
        Map<String, Double> revenueMap = new HashMap<>();
        List<String> receiptRows = DatabaseManager.getInstance().getReceiptLines();

        for (String line : receiptRows) {
            // Receipt lines are formatted as: "%-22s x%d  ₹%.2f"
            // e.g.  "Amoxicillin            x2  ₹31.00"
            int xi = line.lastIndexOf(" x");
            if (xi < 0) continue;

            int di = line.indexOf("₹");
            if (di < 0) di = line.indexOf("$");
            if (di < 0 || di <= xi) continue;

            String mname = line.substring(0, xi).trim();
            try {
                double val = Double.parseDouble(line.substring(di + 1).trim());
                revenueMap.merge(mname, val, Double::sum);
            } catch (NumberFormatException ignored) {}
        }

        revenueMap.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));

        revenueChart.getData().add(series);
    }

    /* ─── Helper ─── */
    private void setStatus(String msg, boolean isError) {
        lblStatus.setText(msg);
        lblStatus.setStyle(isError
                ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;"
                : "-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }
}
