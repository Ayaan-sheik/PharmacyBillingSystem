package pharmasync.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pharmasync.managers.DatabaseManager;
import pharmasync.threads.LowStockNotifier;

public class PharmaSyncApp extends Application {

    private static Stage mainStage;
    private LowStockNotifier notifier;

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        primaryStage.setTitle("Manipal Medicals — PharmaSync");
        primaryStage.setResizable(true);

        // Phase 4: Initialize SQLite database on startup
        DatabaseManager.getInstance().initializeDatabase();
        DatabaseManager.getInstance().syncFromCSV();

        // Start background daemon thread (Week 12 — Multithreading)
        notifier = new LowStockNotifier();
        notifier.setDaemon(true);
        notifier.start();

        // Load Login screen via FXML
        try {
            FXMLLoader loader = new FXMLLoader(
                PharmaSyncApp.class.getResource("LoginScreen.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 450);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Failed to load LoginScreen.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Returns the application's primary Stage for navigation from controllers. */
    public static Stage getMainStage() {
        return mainStage;
    }

    @Override
    public void stop() throws Exception {
        if (notifier != null) notifier.stopNotifier();
        DatabaseManager.getInstance().close();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
