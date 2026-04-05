package pharmasync.managers;

import pharmasync.models.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CSVManager {
    private static final String INVENTORY_FILE = "inventory.csv";
    private static final String USERS_FILE = "users.csv";

    // Static initialization to ensure the files exist with some dummy data if empty
    static {
        createFileIfNotExists(INVENTORY_FILE, "type,id,name,price,quantity,category,extra_field,expiry_date\n" +
                "rx,MED001,Amoxicillin,15.50,100,TABLET,Dr. Smith,2027-12-31\n" +
                "otc,MED002,Ibuprofen,8.99,50,TABLET,false,2027-12-31\n" +
                "otc,MED003,Cough Syrup,12.00,10,SYRUP,false,2027-12-31\n");

        // Assuming SHA-256 hashes roughly. For demo, we use simple hashes or plaintext for convenience
        // admin:admin, user:user
        // We'll just store plain text for absolute simplicity, but ideally hash here.
        createFileIfNotExists(USERS_FILE, "username,password,role\n" +
                "admin,admin123,ADMIN\n" +
                "pharmacist,rx123,PHARMACIST\n");
    }

    private static void createFileIfNotExists(String filePath, String defaultContent) {
        File file = new File(filePath);
        if (!file.exists()) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(defaultContent);
            } catch (IOException e) {
                System.out.println("Error initializing file " + filePath + ": " + e.getMessage());
            }
        }
    }

    public static List<Medicine> loadInventory() {
        List<Medicine> inventory = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(INVENTORY_FILE))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 8) continue;

                String type = parts[0];
                String id = parts[1];
                String name = parts[2];
                Double price = Double.parseDouble(parts[3]);
                Integer quantity = Integer.parseInt(parts[4]);
                DrugCategory refCat = DrugCategory.valueOf(parts[5].toUpperCase());
                String extra = parts[6];
                java.time.LocalDate expiry = java.time.LocalDate.parse(parts[7]);

                if (type.equalsIgnoreCase("rx")) {
                    inventory.add(new PrescriptionDrug(id, name, price, quantity, refCat, expiry, extra));
                } else {
                    boolean ageRestricted = Boolean.parseBoolean(extra);
                    inventory.add(new OverTheCounterDrug(id, name, price, quantity, refCat, expiry, ageRestricted));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading inventory: " + e.getMessage());
        }
        return inventory;
    }

    public static void saveInventory(List<Medicine> inventory) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(INVENTORY_FILE))) {
            pw.println("type,id,name,price,quantity,category,extra_field,expiry_date");
            for (Medicine m : inventory) {
                String type = (m instanceof PrescriptionDrug) ? "rx" : "otc";
                String extra = (m instanceof PrescriptionDrug) 
                        ? ((PrescriptionDrug) m).getPrescribedDoctor() 
                        : String.valueOf(((OverTheCounterDrug) m).isAgeRestricted());
                
                pw.printf("%s,%s,%s,%.2f,%d,%s,%s,%s\n", 
                        type, m.getMedicineID(), m.getName(), m.getPrice(), 
                        m.getStockQuantity(), m.getCategory().name(), extra, m.getExpiryDate().toString());
            }
        } catch (IOException e) {
            System.out.println("Error saving inventory: " + e.getMessage());
        }
    }

    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                users.add(new User(parts[0], parts[1], User.Role.valueOf(parts[2].toUpperCase())));
            }
        } catch (IOException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
        return users;
    }

    public static void saveReceipt(String content) {
        // Saves receipt text into a specific file
        String fileName = "Receipt_" + System.currentTimeMillis() + ".txt";
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(content);
            System.out.println("Receipt successfully saved as " + fileName);
        } catch (IOException e) {
            System.out.println("Error saving receipt: " + e.getMessage());
        }
    }
}
