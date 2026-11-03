package clinicflow.service;

/**
 * Observer interface notified when patient cards are added, modified, moved, or deleted.
 */
public interface PatientListener {
    /**
     * Invoked when any patient data or board state changes.
     */
    void onPatientDataChanged();
}
