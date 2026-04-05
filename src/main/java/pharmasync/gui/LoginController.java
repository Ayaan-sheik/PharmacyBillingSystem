package pharmasync.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import pharmasync.managers.SessionManager;
import pharmasync.models.User;

public class LoginController {

    @FXML private TextField     txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Button        btnLogin;
    @FXML private Label         lblError;

    @FXML
    private void initialize() {
        lblError.setText("");
    }

    @FXML
    private void handleLogin() {
        String username = txtUser.getText().trim();
        String password = txtPass.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Please enter username and password.");
            return;
        }

        boolean success = SessionManager.getInstance().login(username, password);
        if (success) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            try {
                if (currentUser.getRole() == User.Role.ADMIN) {
                    FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("AdminDashboard.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 1100, 720);
                    PharmaSyncApp.getMainStage().setScene(scene);
                } else {
                    FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("PharmacistDashboard.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 1000, 680);
                    PharmaSyncApp.getMainStage().setScene(scene);
                }
            } catch (Exception ex) {
                lblError.setText("Error loading dashboard: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            lblError.setText("Invalid credentials. Try admin / admin123");
            txtPass.clear();
        }
    }
}
