package clinicflow.model;

/**
 * Patient priority levels for queue ordering and visual badges.
 */
public enum Priority {
    NORMAL("Normal", 1, "Standard queue priority"),
    SENIOR_CHILD("Senior / Child", 2, "Priority for elderly (60+) & pediatric patients (<12)"),
    EMERGENCY("Emergency", 3, "High-acuity emergency case; always prioritized at top");

    private final String displayName;
    private final int rank;
    private final String description;

    Priority(String displayName, int rank, String description) {
        this.displayName = displayName;
        this.rank = rank;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRank() {
        return rank;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEmergency() {
        return this == EMERGENCY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
