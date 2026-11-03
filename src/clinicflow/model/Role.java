package clinicflow.model;

/**
 * Roles for staff members in the clinic.
 */
public enum Role {
    ADMIN("Administrator", "Full system access, all transitions, reports & settings"),
    RECEPTIONIST("Receptionist", "Registers patients, moves cards, manages waiting queue"),
    DOCTOR("Doctor", "Calls patients, manages consultations, orders tests & billing");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean canManageSettings() {
        return this == ADMIN;
    }

    public boolean canExportReports() {
        return this == ADMIN || this == RECEPTIONIST;
    }

    public boolean canRegisterPatients() {
        return this == ADMIN || this == RECEPTIONIST;
    }

    public boolean canCancelPatients() {
        return this == ADMIN || this == RECEPTIONIST;
    }

    public boolean canAdminOverride() {
        return this == ADMIN;
    }
}
