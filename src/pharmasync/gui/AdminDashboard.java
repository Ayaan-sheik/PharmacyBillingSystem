package pharmasync.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pharmasync.managers.InventoryManager;
import pharmasync.managers.SessionManager;
import pharmasync.models.DrugCategory;
import pharmasync.models.Medicine;
import pharmasync.models.OverTheCounterDrug;
import pharmasync.models.PrescriptionDrug;

import java.util.Map;
import java.util.stream.Collectors;

public class AdminDashboard {
    private BorderPane view;
    private TableView<Medicine> adminTable;

    public AdminDashboard() {
        view = new BorderPane();
        view.setPadding(new Insets(20));

        // Header
        Label headerLabel = new Label("Admin Dashboard - Analytics & Inventory");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        Button btnLogout = new Button("Logout");
        btnLogout.setOnAction(e -> {
            SessionManager.getInstance().logout();
            PharmaSyncApp.navigateTo(new Scene(new LoginScreen(null).getView(), 600, 400));
        });
        view.setTop(new HBox(20, headerLabel, btnLogout));

        // Center Content - TabPane
        TabPane tabPane = new TabPane();
        Tab tabManage = new Tab("Manage Inventory", createManageView());
        Tab tabAnalytics = new Tab("Analytics (Streams API)", createAnalyticsView());
        tabManage.setClosable(false);
        tabAnalytics.setClosable(false);
        
        tabPane.getTabs().addAll(tabManage, tabAnalytics);
        view.setCenter(tabPane);
    }

    private VBox createManageView() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15, 0, 0, 0));

        adminTable = new TableView<>();
        TableColumn<Medicine, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("medicineID"));
        TableColumn<Medicine, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Medicine, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        TableColumn<Medicine, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        adminTable.getColumns().addAll(idCol, nameCol, priceCol, stockCol);
        
        refreshTable();
        InventoryManager.getInstance().addUpdateListener(this::refreshTable);

        // Control bar to add medication
        HBox addBox = new HBox(10);
        TextField tfId = new TextField(); tfId.setPromptText("ID");
        TextField tfName = new TextField(); tfName.setPromptText("Name");
        TextField tfPrice = new TextField(); tfPrice.setPromptText("Price");
        TextField tfQty = new TextField(); tfQty.setPromptText("Qty");
        ComboBox<DrugCategory> cbCat = new ComboBox<>(FXCollections.observableArrayList(DrugCategory.values()));
        cbCat.setPromptText("Category");

        // Auto-fill fields on table selection
        adminTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                tfId.setText(newSel.getMedicineID());
                tfName.setText(newSel.getName());
                tfPrice.setText(String.valueOf(newSel.getPrice()));
                tfQty.setText(String.valueOf(newSel.getStockQuantity()));
                cbCat.setValue(newSel.getCategory());
            }
        });

        Button btnAdd = new Button("Add OTC Drug");
        btnAdd.setOnAction(e -> {
            try {
                Medicine m = new OverTheCounterDrug(
                        tfId.getText(), tfName.getText(),
                        Double.parseDouble(tfPrice.getText()),
                        Integer.parseInt(tfQty.getText()),
                        cbCat.getValue(), false);
                InventoryManager.getInstance().addMedicine(m);
                refreshTable();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input for adding medicine.").show();
            }
        });

        Button btnDel = new Button("Delete Selected");
        btnDel.setOnAction(e -> {
            Medicine sel = adminTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                InventoryManager.getInstance().removeMedicine(sel.getMedicineID());
                // table auto updates via listener!
            }
        });

        Button btnUpdate = new Button("Update Medicine");
        btnUpdate.setOnAction(e -> {
            Medicine sel = adminTable.getSelectionModel().getSelectedItem();
            if (sel != null && !tfQty.getText().isEmpty() && !tfPrice.getText().isEmpty()) {
                try {
                    int newQty = Integer.parseInt(tfQty.getText());
                    double newPrice = Double.parseDouble(tfPrice.getText());
                    InventoryManager.getInstance().updateMedicine(sel.getMedicineID(), newPrice, newQty);
                    tfQty.clear();
                    tfPrice.clear();
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Enter valid numbers for price and stock.").show();
                }
            } else {
                new Alert(Alert.AlertType.WARNING, "Select a medicine and ensure Price and Qty are filled.").show();
            }
        });

        addBox.getChildren().addAll(tfId, tfName, tfPrice, tfQty, cbCat, btnAdd, btnUpdate, btnDel);
        vbox.getChildren().addAll(adminTable, addBox);
        return vbox;
    }

    private VBox createAnalyticsView() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Drug Category");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Stock");

        BarChart<String, Number> bc = new BarChart<>(xAxis, yAxis);
        bc.setTitle("Inventory Distribution by Category");
        bc.setStyle("-fx-text-fill: white;");

        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1.setName("Current Stock Levels");

        // Advanced: Using Java 8 Streams to aggregate stock grouping by Category
        Map<DrugCategory, Integer> categoryStock = InventoryManager.getInstance().getInventory()
                .stream()
                .collect(Collectors.groupingBy(Medicine::getCategory, Collectors.summingInt(Medicine::getStockQuantity)));

        for (Map.Entry<DrugCategory, Integer> entry : categoryStock.entrySet()) {
            series1.getData().add(new XYChart.Data<>(entry.getKey().name(), entry.getValue()));
        }

        bc.getData().add(series1);
        vbox.getChildren().add(bc);
        return vbox;
    }

    private void refreshTable() {
        ObservableList<Medicine> data = FXCollections.observableArrayList(InventoryManager.getInstance().getInventory());
        adminTable.setItems(data);
    }

    public BorderPane getView() {
        return view;
    }
}
