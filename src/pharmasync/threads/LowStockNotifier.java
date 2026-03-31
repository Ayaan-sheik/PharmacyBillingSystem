package pharmasync.threads;

import pharmasync.managers.InventoryManager;
import pharmasync.models.Medicine;
import java.util.List;
import java.io.File;

public class LowStockNotifier extends Thread {
    private boolean active = true;
    private static final int LOW_STOCK_THRESHOLD = 5;

    public void stopNotifier() {
        this.active = false;
        this.interrupt();
    }

    @Override
    public void run() {
        File invFile = new File("inventory.csv");
        long lastModified = invFile.exists() ? invFile.lastModified() : 0;

        while (active) {
            try {
                // Background daemon pausing for 3 seconds before re-checking
                Thread.sleep(3000);

                long currentModified = invFile.exists() ? invFile.lastModified() : 0;
                if (currentModified > lastModified) {
                    lastModified = currentModified;
                    InventoryManager.getInstance().reloadFromDisk(); // Notifies UI
                }

                List<Medicine> inventory = InventoryManager.getInstance().getInventory();
                
                // Advanced Streams filtering
                long lowStockCount = inventory.stream()
                        .filter(m -> m.getStockQuantity() < LOW_STOCK_THRESHOLD)
                        .count();

                if (lowStockCount > 0) {
                    System.out.println("[LowStockNotifier] WARNING: " + lowStockCount + " items are critically low on stock!");
                    // Ideally, trigger Observer pattern to UI here
                }

            } catch (InterruptedException e) {
                System.out.println("LowStockNotifier thread interrupted and shutting down.");
            }
        }
    }
}
