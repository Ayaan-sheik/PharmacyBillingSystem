package pharmasync.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pharmasync.threads.LowStockNotifier;

public class PharmaSyncApp extends Application {
    private static Stage mainStage;
    private LowStockNotifier notifier;

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        primaryStage.setTitle("PharmaSync - Modern Medical Billing System");
        
        // Start Daemon background thread
        notifier = new LowStockNotifier();
        notifier.setDaemon(true);
        notifier.start();

        // Launch Login Screen
        LoginScreen loginScreen = new LoginScreen(this);
        Scene scene = new Scene(loginScreen.getView(), 600, 400);
        
        // Apply responsive animations and custom CSS
        try {
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS could not be loaded: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void navigateTo(Scene newScene) {
        try {
            newScene.getStylesheets().add(PharmaSyncApp.class.getResource("styles.css").toExternalForm());
        } catch (Exception e) {}
        mainStage.setScene(newScene);
    }

    @Override
    public void stop() throws Exception {
        if (notifier != null) {
            notifier.stopNotifier(); // Clean up threads before closing
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
