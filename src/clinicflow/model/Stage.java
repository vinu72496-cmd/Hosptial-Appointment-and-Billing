package clinicflow.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Workflow stages representing the 5 columns of the Kanban board.
 */
public enum Stage {
    WAITING("Waiting", "Registered and waiting for the doctor", 1),
    WITH_DOCTOR("With Doctor", "Currently being consulted", 2),
    TESTS("Tests", "Sent for lab tests, scans, or procedures", 3),
    BILLING("Billing", "Consultation finished, payment pending", 4),
    DONE("Done", "Visit completed and left the clinic", 5);

    private final String title;
    private final String description;
    private final int sequence;

    Stage(String title, String description, int sequence) {
        this.title = title;
        this.description = description;
        this.sequence = sequence;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getSequence() {
        return sequence;
    }

    /**
     * Checks if a transition from this stage to target stage is allowed.
     *
     * @param target the target stage
     * @param role the current user role
     * @return true if allowed, false otherwise
     */
    public boolean canTransitionTo(Stage target, Role role) {
        if (target == null || target == this) {
            return false;
        }

        // Admin override: Any active stage -> Done is allowed (e.g. patient left early)
        if (role == Role.ADMIN && target == DONE && this != DONE) {
            return true;
        }

        // Standard workflow transitions
        switch (this) {
            case WAITING:
                // Allowed: Waiting -> With Doctor
                return target == WITH_DOCTOR;

            case WITH_DOCTOR:
                // Allowed: With Doctor -> Tests, Billing, or Done
                return target == TESTS || target == BILLING || target == DONE;

            case TESTS:
                // Allowed: Tests -> With Doctor (return for result review) or Billing
                return target == WITH_DOCTOR || target == BILLING;

            case BILLING:
                // Allowed: Billing -> Done
                return target == DONE;

            case DONE:
                // Visit completed; no transitions allowed from Done
                return false;

            default:
                return false;
        }
    }

    /**
     * Gets a list of allowed next stages from this stage for a given role.
     */
    public List<Stage> getAllowedNextStages(Role role) {
        List<Stage> nextStages = new ArrayList<>();
        for (Stage candidate : Stage.values()) {
            if (canTransitionTo(candidate, role)) {
                nextStages.add(candidate);
            }
        }
        return Collections.unmodifiableList(nextStages);
    }

    /**
     * Provides an explanation when a transition is blocked.
     */
    public String getTransitionBlockedReason(Stage target, Role role) {
        if (target == this) {
            return "Patient is already in " + title + ".";
        }
        if (this == DONE) {
            return "Patient visit is already marked Done and cannot be moved.";
        }
        if (this == WAITING && target != WITH_DOCTOR) {
            return "Patients in 'Waiting' must first be seen by a doctor ('With Doctor') before tests, billing, or completion.";
        }
        if (this == TESTS && target == DONE) {
            return role == Role.ADMIN 
                ? "Admin override allows moving to Done directly." 
                : "Patients at 'Tests' must return to 'With Doctor' or proceed to 'Billing' before completing their visit.";
        }
        if (this == BILLING && target != DONE) {
            return "Patients at 'Billing' can only transition to 'Done' once payment is handled.";
        }
        return "Direct transition from '" + title + "' to '" + target.getTitle() + "' is not permitted in clinic workflow.";
    }

    @Override
    public String toString() {
        return title;
    }
}
