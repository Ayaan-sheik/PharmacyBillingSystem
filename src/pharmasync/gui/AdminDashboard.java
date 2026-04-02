package pharmasync.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pharmasync.managers.InventoryManager;
import pharmasync.managers.SessionManager;
import pharmasync.models.DrugCategory;
import pharmasync.models.Medicine;
import pharmasync.models.OverTheCounterDrug;
import pharmasync.models.PrescriptionDrug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
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
        HBox topBar = new HBox(20, headerLabel, btnLogout);
        topBar.setStyle("-fx-background-color: #0a1931; -fx-padding: 15px;");
        topBar.setAlignment(Pos.CENTER_LEFT);
        view.setTop(topBar);

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

        // Row selection highlighting
        adminTable.setRowFactory(tv -> {
            TableRow<Medicine> row = new TableRow<>();
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-background-color: #1b4f72; -fx-text-fill: white;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });

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
                String enteredId = tfId.getText();
                double enteredPrice = Double.parseDouble(tfPrice.getText());
                int enteredQty = Integer.parseInt(tfQty.getText());

                // Check if medicine ID already exists — update instead of duplicating
                java.util.Optional<Medicine> existing = InventoryManager.getInstance().getMedicineById(enteredId);
                if (existing.isPresent()) {
                    InventoryManager.getInstance().updateMedicine(enteredId, tfName.getText(), cbCat.getValue(), enteredPrice, enteredQty);
                    refreshTable();
                    new Alert(Alert.AlertType.INFORMATION, "Medicine ID '" + enteredId + "' already existed — record updated.").show();
                } else {
                    Medicine m = new OverTheCounterDrug(
                            enteredId, tfName.getText(),
                            enteredPrice, enteredQty,
                            cbCat.getValue(), false);
                    InventoryManager.getInstance().addMedicine(m);
                    refreshTable();
                }
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
                    InventoryManager.getInstance().updateMedicine(sel.getMedicineID(), tfName.getText(), cbCat.getValue(), newPrice, newQty);
                    tfQty.clear();
                    tfPrice.clear();
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Enter valid numbers for price and stock.").show();
                }
            } else {
                new Alert(Alert.AlertType.WARNING, "Select a medicine and ensure Price and Qty are filled.").show();
            }
        });

        // Heading labels above input fields
        HBox labelBox = new HBox(10);
        Label lblId = new Label("ID");
        lblId.setPrefWidth(tfId.getPrefWidth());
        lblId.setMinWidth(60);
        Label lblName = new Label("Name");
        lblName.setPrefWidth(tfName.getPrefWidth());
        lblName.setMinWidth(60);
        Label lblPrice = new Label("Price");
        lblPrice.setPrefWidth(tfPrice.getPrefWidth());
        lblPrice.setMinWidth(60);
        Label lblQty = new Label("Qty");
        lblQty.setPrefWidth(tfQty.getPrefWidth());
        lblQty.setMinWidth(60);
        Label lblCat = new Label("Category");
        lblCat.setPrefWidth(cbCat.getPrefWidth());
        lblCat.setMinWidth(60);
        labelBox.getChildren().addAll(lblId, lblName, lblPrice, lblQty, lblCat);

        addBox.getChildren().addAll(tfId, tfName, tfPrice, tfQty, cbCat, btnAdd, btnUpdate, btnDel);
        vbox.getChildren().addAll(adminTable, labelBox, addBox);
        return vbox;
    }

    private VBox createAnalyticsView() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));

        PieChart pieChart = new PieChart();
        pieChart.setTitle("Inventory Distribution by Category");
        Map<DrugCategory, Integer> categoryStock = InventoryManager.getInstance().getInventory()
                .stream()
                .collect(Collectors.groupingBy(Medicine::getCategory, Collectors.summingInt(Medicine::getStockQuantity)));

        for (Map.Entry<DrugCategory, Integer> entry : categoryStock.entrySet()) {
            pieChart.getData().add(new PieChart.Data(entry.getKey().name(), entry.getValue()));
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Medicine Name");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Revenue ($)");

        BarChart<String, Number> revenueChart = new BarChart<>(xAxis, yAxis);
        revenueChart.setTitle("Revenue based on Orders");
        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue ($)");

        Map<String, Double> revenueMap = new HashMap<>();
        File dir = new File(".");
        File[] receipts = dir.listFiles((d, name) -> name.startsWith("Receipt_") && name.endsWith(".txt"));
        if (receipts != null) {
            for (File r : receipts) {
                try {
                    for (String line : Files.readAllLines(r.toPath())) {
                        int xIndex = line.lastIndexOf(" x");
                        int dollarIndex = line.indexOf("$");
                        if(xIndex > 0 && dollarIndex > xIndex) {
                            String mName = line.substring(0, xIndex).trim();
                            try {
                                double val = Double.parseDouble(line.substring(dollarIndex + 1).trim());
                                revenueMap.put(mName, revenueMap.getOrDefault(mName, 0.0) + val);
                            } catch(NumberFormatException ignored) {}
                        }
                    }
                } catch(IOException ignored) {}
            }
        }

        for (Map.Entry<String, Double> entry : revenueMap.entrySet()) {
            revenueSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        revenueChart.getData().add(revenueSeries);
        
        // Ensure white text styling for the charts if necessary
        pieChart.setStyle("-fx-text-fill: white;");
        revenueChart.setStyle("-fx-text-fill: white;");

        ScrollPane scroll = new ScrollPane();
        VBox innerBox = new VBox(30, pieChart, revenueChart);
        innerBox.setPadding(new Insets(10));
        scroll.setContent(innerBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        innerBox.setStyle("-fx-background-color: transparent;");

        VBox.setVgrow(scroll, Priority.ALWAYS);
        vbox.getChildren().add(scroll);
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
