package pharmasync.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import pharmasync.managers.SessionManager;
import pharmasync.models.User;

public class LoginScreen {
    private final VBox view;
    private final PharmaSyncApp app;

    public LoginScreen(PharmaSyncApp app) {
        this.app = app;
        
        view = new VBox(20);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(40));

        Label title = new Label("Manipal Medicals");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        title.setStyle("-fx-text-fill: white;");

        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(15);
        grid.setAlignment(Pos.CENTER);

        // GUI controls aligning with Week 9 JavaFX
        Label lblUser = new Label("Username:");
        TextField txtUser = new TextField();
        txtUser.setPromptText("Enter username");

        Label lblPass = new Label("Password:");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Enter password");

        Button btnLogin = new Button("Login Securely");
        btnLogin.setDefaultButton(true);
        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ff5773;");

        // Event Handling aligning with Week 9 constraints
        btnLogin.setOnAction(e -> {
            boolean success = SessionManager.getInstance().login(txtUser.getText(), txtPass.getText());
            if (success) {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser.getRole() == User.Role.ADMIN) {
                    AdminDashboard adminBoard = new AdminDashboard();
                    PharmaSyncApp.navigateTo(new Scene(adminBoard.getView(), 900, 600));
                } else {
                    PharmacistDashboard pharmaBoard = new PharmacistDashboard();
                    PharmaSyncApp.navigateTo(new Scene(pharmaBoard.getView(), 900, 600));
                }
            } else {
                lblError.setText("Invalid credentials. Try admin:admin123");
            }
        });

        grid.add(lblUser, 0, 0);
        grid.add(txtUser, 1, 0);
        grid.add(lblPass, 0, 1);
        grid.add(txtPass, 1, 1);
        grid.add(btnLogin, 1, 2);

        view.getChildren().addAll(title, grid, lblError);
    }

    public VBox getView() {
        return view;
    }
}
