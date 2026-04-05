package pharmasync.models;

public class User {
    private String username;
    private String passwordHash;
    private Role role;

    public enum Role {
        ADMIN, PHARMACIST
    }

    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
