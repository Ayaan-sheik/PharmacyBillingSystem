package pharmasync.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pharmasync.managers.InventoryManager;
import pharmasync.managers.SessionManager;
import pharmasync.models.CartItem;
import pharmasync.models.Medicine;

import java.util.ArrayList;
import java.util.List;

public class PharmacistDashboard {
    private BorderPane view;
    private TableView<Medicine> inventoryTable;
    private ListView<String> cartList;
    private List<CartItem<Medicine, Integer>> cart;
    private Label lblTotal;

    public PharmacistDashboard() {
        cart = new ArrayList<>();
        view = new BorderPane();
        view.setPadding(new Insets(20));

        // Header
        Label headerLabel = new Label("Pharmacist Billing Terminal");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        Button btnLogout = new Button("Logout");
        btnLogout.setOnAction(e -> {
            SessionManager.getInstance().logout();
            PharmaSyncApp.navigateTo(new Scene(new LoginScreen(null).getView(), 600, 400));
        });
        HBox topBox = new HBox(20, headerLabel, btnLogout);
        view.setTop(topBox);

        // Inventory Table
        inventoryTable = new TableView<>();
        setupTable();
        refreshTable();
        InventoryManager.getInstance().addUpdateListener(this::refreshTable);

        // Cart side
        VBox cartVBox = new VBox(10);
        cartList = new ListView<>();
        lblTotal = new Label("Total: $0.00");
        lblTotal.setStyle("-fx-font-size:18px;-fx-text-fill:#e94560;");
        Button btnCheckout = new Button("Checkout & Print");

        btnCheckout.setOnAction(e -> handleCheckout());

        cartVBox.getChildren().addAll(new Label("Current Cart"), cartList, lblTotal, btnCheckout);
        cartVBox.setPrefWidth(250);

        // Interaction - Search and Add
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Search medicines...");
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            inventoryTable.setItems(FXCollections.observableArrayList(
                InventoryManager.getInstance().searchByName(newVal)
            ));
        });

        Button btnAddToCart = new Button("Add Selected to Cart");
        ComboBox<Integer> qtyBox = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,10));
        qtyBox.setValue(1);

        btnAddToCart.setOnAction(e -> {
            Medicine sel = inventoryTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                cart.add(new CartItem<>(sel, qtyBox.getValue()));
                updateCartUI();
            }
        });

        HBox controlBox = new HBox(15, txtSearch, qtyBox, btnAddToCart);
        controlBox.setPadding(new Insets(10, 0, 10, 0));

        VBox centerBox = new VBox(controlBox, inventoryTable);
        view.setCenter(centerBox);
        view.setRight(cartVBox);
    }

    private void updateCartUI() {
        cartList.getItems().clear();
        double runningTotal = 0;
        for (CartItem<Medicine, Integer> c : cart) {
            cartList.getItems().add(c.getItem().getName() + " x" + c.getQuantity());
            runningTotal += (c.getItem().getPrice() * c.getQuantity());
        }
        lblTotal.setText(String.format("Total: $%.2f", runningTotal));
    }

    private void handleCheckout() {
        if(cart.isEmpty()) return;
        boolean success = InventoryManager.getInstance().processCheckout(cart);
        if (success) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Checkout successful! Receipt saved.");
            a.show();
            cart.clear();
            updateCartUI();
            refreshTable();
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR, "Insufficient stock for an item.");
            a.show();
        }
    }

    private void setupTable() {
        TableColumn<Medicine, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("medicineID"));
        
        TableColumn<Medicine, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<Medicine, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Medicine, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));

        inventoryTable.getColumns().addAll(idCol, nameCol, priceCol, stockCol);

        // Row selection highlighting
        inventoryTable.setRowFactory(tv -> {
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
    }

    private void refreshTable() {
        ObservableList<Medicine> data = FXCollections.observableArrayList(InventoryManager.getInstance().getInventory());
        inventoryTable.setItems(data);
    }

    public BorderPane getView() {
        return view;
    }
}
