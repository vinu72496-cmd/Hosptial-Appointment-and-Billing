package clinicflow.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Audit record for tracking a patient's movement into and out of a workflow stage.
 */
public class StageRecord {
    private final int patientId;
    private final Stage stage;
    private final LocalDateTime enteredAt;
    private LocalDateTime exitedAt;

    public StageRecord(int patientId, Stage stage, LocalDateTime enteredAt) {
        this(patientId, stage, enteredAt, null);
    }

    public StageRecord(int patientId, Stage stage, LocalDateTime enteredAt, LocalDateTime exitedAt) {
        this.patientId = patientId;
        this.stage = stage;
        this.enteredAt = enteredAt;
        this.exitedAt = exitedAt;
    }

    public int getPatientId() {
        return patientId;
    }

    public Stage getStage() {
        return stage;
    }

    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }

    public LocalDateTime getExitedAt() {
        return exitedAt;
    }

    public void setExitedAt(LocalDateTime exitedAt) {
        this.exitedAt = exitedAt;
    }

    public boolean isActive() {
        return exitedAt == null;
    }

    /**
     * Calculates the duration spent in this stage.
     * If the patient is still in this stage, returns duration from enteredAt to current time.
     */
    public Duration getDuration() {
        LocalDateTime end = (exitedAt != null) ? exitedAt : LocalDateTime.now();
        if (enteredAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(enteredAt, end);
    }
}
