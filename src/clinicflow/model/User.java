package clinicflow.model;

/**
 * Staff user account for login and role-based permissions.
 */
public class User {
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final String displayName;
    private final String doctorName;

    public User(String username, String passwordHash, Role role, String displayName, String doctorName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.doctorName = doctorName;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public boolean isDoctor() {
        return role == Role.DOCTOR;
    }

    @Override
    public String toString() {
        return displayName + " (" + role.getDisplayName() + ")";
    }
}
