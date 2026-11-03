package clinicflow.service;

import clinicflow.model.Role;
import clinicflow.model.User;
import clinicflow.storage.DataStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Service managing user authentication, password hashing with SHA-256,
 * and session state.
 */
public class AuthService {
    private final DataStore dataStore;
    private final Map<String, User> userMap;
    private User currentUser;

    public AuthService(DataStore dataStore) {
        this.dataStore = dataStore;
        this.userMap = new HashMap<>();
        loadOrSeedUsers();
    }

    private void loadOrSeedUsers() {
        List<User> loaded = dataStore.loadUsers();
        if (loaded.isEmpty()) {
            // Seed default users
            loaded = createDefaultUsers();
            try {
                dataStore.saveUsers(loaded);
            } catch (Exception e) {
                System.err.println("Failed to seed default users: " + e.getMessage());
            }
        }
        for (User u : loaded) {
            userMap.put(u.getUsername().toLowerCase(), u);
        }
        // Default to Admin or Receptionist initially
        this.currentUser = userMap.get("admin");
        if (this.currentUser == null && !loaded.isEmpty()) {
            this.currentUser = loaded.get(0);
        }
    }

    private List<User> createDefaultUsers() {
        List<User> list = new ArrayList<>();
        list.add(new User("admin", hashPassword("admin123"), Role.ADMIN, "System Administrator", ""));
        list.add(new User("reception", hashPassword("rec123"), Role.RECEPTIONIST, "Front Desk Receptionist", ""));
        list.add(new User("dr_sharma", hashPassword("doc123"), Role.DOCTOR, "Dr. A. Sharma", "Dr. A. Sharma (General Medicine)"));
        list.add(new User("dr_patel", hashPassword("doc123"), Role.DOCTOR, "Dr. P. Patel", "Dr. P. Patel (Pediatrics)"));
        return list;
    }

    public static String hashPassword(String password) {
        if (password == null) password = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Authenticates a user with username and raw password.
     */
    public synchronized User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        User user = userMap.get(username.trim().toLowerCase());
        if (user == null) {
            return null;
        }
        String hashed = hashPassword(password);
        if (user.getPasswordHash().equalsIgnoreCase(hashed)) {
            this.currentUser = user;
            return user;
        }
        return null;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(userMap.values());
    }
}
