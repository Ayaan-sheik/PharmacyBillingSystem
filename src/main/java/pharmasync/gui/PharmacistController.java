package pharmasync.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import pharmasync.managers.InventoryManager;
import pharmasync.managers.SessionManager;
import pharmasync.models.CartItem;
import pharmasync.models.Medicine;

import java.util.ArrayList;
import java.util.List;

public class PharmacistController {

    /* ── Top bar ── */
    @FXML private Label lblUserInfo;

    /* ── Inventory search + table ── */
    @FXML private TextField               txtSearch;
    @FXML private TextField               qtyField;
    @FXML private TableView<Medicine>     inventoryTable;
    @FXML private TableColumn<Medicine,String>  colId;
    @FXML private TableColumn<Medicine,String>  colName;
    @FXML private TableColumn<Medicine,String>  colCat;
    @FXML private TableColumn<Medicine,Double>  colPrice;
    @FXML private TableColumn<Medicine,Integer> colStock;

    /* ── Cart panel ── */
    @FXML private ListView<String> cartList;
    @FXML private Label            lblTotal;
    @FXML private Label            lblCartStatus;

    /* ── Internal cart state ── */
    private final List<CartItem<Medicine, Integer>> cart = new ArrayList<>();

    /* ════════════════════════════════════════
       initialize() — auto-called by FXMLLoader
       ════════════════════════════════════════ */
    @FXML
    private void initialize() {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            lblUserInfo.setText(
                SessionManager.getInstance().getCurrentUser().getUsername());
        }

        setupTableColumns();
        refreshTable();

        // Live search
        txtSearch.textProperty().addListener((obs, old, keyword) -> {
            inventoryTable.setItems(FXCollections.observableArrayList(
                InventoryManager.getInstance().searchByName(keyword)));
        });

        // Live inventory sync
        InventoryManager.getInstance().addUpdateListener(this::refreshTable);

        lblCartStatus.setText("");
    }

    /* ─── Table column wiring ─── */
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("medicineID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCat.setCellValueFactory(cell ->
            new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getCategory().name()));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));

        // Neumorphic row selection styling
        inventoryTable.setRowFactory(tv -> {
            TableRow<Medicine> row = new TableRow<>();
            row.selectedProperty().addListener((obs, was, now) ->
                row.setStyle(now
                    ? "-fx-background-color: rgba(74,78,140,0.15);"
                    : ""));
            return row;
        });
    }

    /* ─── Cart operations ─── */
    @FXML
    private void handleAddToCart() {
        Medicine selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setCartStatus("⚠ Select a medicine from the table first.", true);
            return;
        }
        
        int qty = 1;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) throw new NumberFormatException("Zero or negative");
        } catch (Exception ex) {
            setCartStatus("⚠ Quantity must be a valid positive number.", true);
            return;
        }

        for (CartItem<Medicine, Integer> ci : cart) {
            if (ci.getItem().getMedicineID().equals(selected.getMedicineID())) {
                ci.setQuantity(ci.getQuantity() + qty);
                updateCartUI();
                setCartStatus("✔ " + selected.getName() + " quantity updated to " + ci.getQuantity(), false);
                return;
            }
        }

        cart.add(new CartItem<>(selected, qty));
        updateCartUI();
        setCartStatus("✔ " + selected.getName() + " × " + qty + " added.", false);
    }

    @FXML
    private void handleCheckout() {
        if (cart.isEmpty()) {
            setCartStatus("⚠ Cart is empty.", true);
            return;
        }
        boolean success = InventoryManager.getInstance().processCheckout(cart);
        if (success) {
            new Alert(Alert.AlertType.INFORMATION,
                "✔ Checkout successful! Receipt saved.")
                .show();
            cart.clear();
            updateCartUI();
            refreshTable();
            setCartStatus("", false);
        } else {
            new Alert(Alert.AlertType.ERROR,
                "✖ Insufficient stock for one or more items.")
                .show();
        }
    }

    @FXML
    private void handleClearCart() {
        cart.clear();
        updateCartUI();
        setCartStatus("Cart cleared.", false);
    }

    @FXML
    private void handleRemoveFromCart() {
        int index = cartList.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < cart.size()) {
            CartItem<Medicine, Integer> removed = cart.remove(index);
            updateCartUI();
            setCartStatus("Removed " + removed.getItem().getName() + " from cart.", false);
        } else {
            setCartStatus("⚠ Select an item in the cart to remove.", true);
        }
    }

    /* ─── Logout & Theme ─── */
    private boolean isDarkMode = false;

    @FXML
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        Scene scene = inventoryTable.getScene();
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

    /* ─── Helpers ─── */
    private void refreshTable() {
        ObservableList<Medicine> data = FXCollections.observableArrayList(
            InventoryManager.getInstance().getInventory());
        inventoryTable.setItems(data);
    }

    private void updateCartUI() {
        cartList.getItems().clear();
        double total = 0;
        for (CartItem<Medicine, Integer> item : cart) {
            double lineTotal = item.getItem().getPrice() * item.getQuantity();
            double withTax = lineTotal + (lineTotal * item.getItem().getCategory().getBaseTaxRate())
                             - item.getItem().calculateDiscount();
            total += withTax;
            cartList.getItems().add(String.format(
                "%s  ×%d  —  ₹%.2f",
                item.getItem().getName(), item.getQuantity(), withTax));
        }
        lblTotal.setText(String.format("Total: ₹%.2f", total));
    }

    private void setCartStatus(String msg, boolean isError) {
        lblCartStatus.setText(msg);
        lblCartStatus.setStyle(isError
            ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;"
            : "-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }
}
