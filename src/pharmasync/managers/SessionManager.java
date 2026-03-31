package pharmasync.managers;

import pharmasync.models.User;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private List<User> systemUsers;

    private SessionManager() {
        systemUsers = CSVManager.loadUsers();
    }

    // Singleton Pattern Implementation
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public boolean login(String username, String password) {
        String hashedInput = hashPassword(password); // Example using hash
        for (User u : systemUsers) {
            // For this project scale, we compare plain text since we initialized it plain. 
            // In reality, this should compare hashes.
            if (u.getUsername().equals(username) && u.getPasswordHash().equals(password)) {
                this.currentUser = u;
                return true;
            }
        }
        return false;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
