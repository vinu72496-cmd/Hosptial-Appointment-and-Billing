package clinicflow.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core patient entity tracking personal details, priority, stage, and workflow history.
 */
public class Patient {
    private final int id;
    private final String token;
    private String name;
    private int age;
    private String phone;
    private String reason;
    private String notes;
    private String doctor;
    private Priority priority;
    private Stage stage;
    private final LocalDateTime registeredAt;
    private boolean cancelled;
    private String cancellationReason;
    private final List<StageRecord> history;

    public Patient(int id, String token, String name, int age, String phone,
                   String reason, String notes, String doctor, Priority priority,
                   Stage stage, LocalDateTime registeredAt) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.age = age;
        this.phone = (phone != null) ? phone.trim() : "";
        this.reason = (reason != null) ? reason.trim() : "";
        this.notes = (notes != null) ? notes.trim() : "";
        this.doctor = (doctor != null) ? doctor.trim() : "";
        this.priority = (priority != null) ? priority : Priority.NORMAL;
        this.stage = (stage != null) ? stage : Stage.WAITING;
        this.registeredAt = (registeredAt != null) ? registeredAt : LocalDateTime.now();
        this.cancelled = false;
        this.cancellationReason = "";
        this.history = new ArrayList<>();

        // Initialize first stage record if history is empty
        this.history.add(new StageRecord(this.id, this.stage, this.registeredAt));
    }

    public int getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = (phone != null) ? phone.trim() : "";
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = (reason != null) ? reason.trim() : "";
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes.trim() : "";
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = (doctor != null) ? doctor.trim() : "";
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Stage getStage() {
        return stage;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public List<StageRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Replaces or populates historical records from file storage.
     */
    public void setHistoryRecords(List<StageRecord> records) {
        this.history.clear();
        if (records != null && !records.isEmpty()) {
            this.history.addAll(records);
        } else {
            this.history.add(new StageRecord(this.id, this.stage, this.registeredAt));
        }
    }

    /**
     * Gets the active stage record (exitedAt == null) or the latest record.
     */
    public StageRecord getCurrentStageRecord() {
        if (history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            StageRecord rec = history.get(i);
            if (rec.isActive() && rec.getStage() == this.stage) {
                return rec;
            }
        }
        return history.get(history.size() - 1);
    }

    /**
     * Calculates time elapsed in the current workflow stage.
     */
    public Duration getTimeInCurrentStage() {
        StageRecord current = getCurrentStageRecord();
        if (current == null) {
            return Duration.ZERO;
        }
        return current.getDuration();
    }

    /**
     * Calculates total visit time from registration until visit completion (or now).
     */
    public Duration getTotalVisitTime() {
        if (registeredAt == null) {
            return Duration.ZERO;
        }
        if (stage == Stage.DONE || cancelled) {
            StageRecord last = getCurrentStageRecord();
            if (last != null && last.getExitedAt() != null) {
                return Duration.between(registeredAt, last.getExitedAt());
            }
        }
        return Duration.between(registeredAt, LocalDateTime.now());
    }

    /**
     * Moves patient to a new stage, stamping the previous stage record exit
     * and creating a new active stage record.
     */
    public void moveToStage(Stage newStage, LocalDateTime timestamp) {
        if (newStage == null || newStage == this.stage) {
            return;
        }
        LocalDateTime now = (timestamp != null) ? timestamp : LocalDateTime.now();

        // Close current active record
        StageRecord current = getCurrentStageRecord();
        if (current != null && current.isActive()) {
            current.setExitedAt(now);
        }

        // Set new stage and start new stage record
        this.stage = newStage;
        StageRecord newRecord = new StageRecord(this.id, newStage, now);
        if (newStage == Stage.DONE) {
            // Visit is done, exitedAt is stamped as completed
            newRecord.setExitedAt(now);
        }
        this.history.add(newRecord);
    }

    /**
     * Cancels the patient visit with a recorded reason.
     */
    public void cancel(String reason) {
        this.cancelled = true;
        this.cancellationReason = (reason != null) ? reason.trim() : "Cancelled by staff";
        StageRecord current = getCurrentStageRecord();
        if (current != null && current.isActive()) {
            current.setExitedAt(LocalDateTime.now());
        }
    }
}
