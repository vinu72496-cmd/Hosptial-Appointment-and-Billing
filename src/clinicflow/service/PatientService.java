package clinicflow.service;

import clinicflow.model.*;
import clinicflow.storage.DataStore;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Core business service managing patient lifecycle, Kanban stage transitions,
 * queue prioritization, search/filtering, and automatic file persistence.
 */
public class PatientService {

    private final DataStore dataStore;
    private final Settings settings;
    private final TokenGenerator tokenGenerator;
    private final List<Patient> patients;
    private final List<StageRecord> allHistory;
    private final List<PatientListener> listeners;
    private final AtomicInteger nextPatientId;

    public PatientService(DataStore dataStore, Settings settings) {
        this.dataStore = dataStore;
        this.settings = settings;
        this.listeners = new CopyOnWriteArrayList<>();
        this.patients = new ArrayList<>();
        this.allHistory = new ArrayList<>();
        this.nextPatientId = new AtomicInteger(1);

        loadInitialData();
        this.tokenGenerator = new TokenGenerator(settings.getTokenPrefix(), this.patients);

        if (this.patients.isEmpty()) {
            seedSamplePatients();
        }
    }

    private synchronized void loadInitialData() {
        List<Patient> loadedPatients = dataStore.loadPatients();
        List<StageRecord> loadedHistory = dataStore.loadHistory();

        this.allHistory.addAll(loadedHistory);

        // Group history records by patientId
        Map<Integer, List<StageRecord>> historyByPatient = new HashMap<>();
        for (StageRecord r : loadedHistory) {
            historyByPatient.computeIfAbsent(r.getPatientId(), k -> new ArrayList<>()).add(r);
        }

        int maxId = 0;
        for (Patient p : loadedPatients) {
            List<StageRecord> pHistory = historyByPatient.get(p.getId());
            if (pHistory != null) {
                p.setHistoryRecords(pHistory);
            }
            this.patients.add(p);
            if (p.getId() > maxId) {
                maxId = p.getId();
            }
        }
        this.nextPatientId.set(maxId + 1);
    }

    private synchronized void seedSamplePatients() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Emergency in Waiting
        Patient p1 = new Patient(nextPatientId.getAndIncrement(), tokenGenerator.nextToken(),
            "Vikram Singh", 42, "9876543210", "Severe chest pain & dizziness", "Stretcher assistance provided",
            settings.getDoctors().get(0), Priority.EMERGENCY, Stage.WAITING, now.minusMinutes(12));

        // 2. Senior in Waiting
        Patient p2 = new Patient(nextPatientId.getAndIncrement(), tokenGenerator.nextToken(),
            "Savitri Devi", 68, "9812345678", "Joint pain & high blood pressure checkup", "Wheelchair requested",
            settings.getDoctors().get(0), Priority.SENIOR_CHILD, Stage.WAITING, now.minusMinutes(22));

        // 3. Normal in Waiting (Over 30 mins -> triggers warning highlight)
        Patient p3 = new Patient(nextPatientId.getAndIncrement(), tokenGenerator.nextToken(),
            "Rahul Verma", 29, "9988776655", "Persistent viral fever & body ache", "Fasting for test if required",
            settings.getDoctors().get(1), Priority.NORMAL, Stage.WAITING, now.minusMinutes(35));

        // 4. With Doctor
        Patient p4 = new Patient(nextPatientId.getAndIncrement(), tokenGenerator.nextToken(),
            "Ananya Iyer", 34, "9123456780", "Routine diabetic consultation", "Brought previous lab reports",
            settings.getDoctors().get(0), Priority.NORMAL, Stage.WITH_DOCTOR, now.minusMinutes(20));
        p4.moveToStage(Stage.WITH_DOCTOR, now.minusMinutes(7));

        // 5. At Tests
        Patient p5 = new Patient(nextPatientId.getAndIncrement(), tokenGenerator.nextToken(),
            "Aarav Mehta", 8, "9871122334", "Stomach ache & vomiting", "Sample sent to pathology lab",
            settings.getDoctors().get(1), Priority.SENIOR_CHILD, Stage.TESTS, now.minusMinutes(45));
        p5.moveToStage(Stage.WITH_DOCTOR, now.minusMinutes(30));
        p5.moveToStage(Stage.TESTS, now.minusMinutes(14));

        // 6. At Billing
        Patient p6 = new Patient(nextPatientId.getAndIncrement(), tokenGenerator.nextToken(),
            "Sunita Rao", 51, "9765432109", "ECG follow-up & medication renewal", "Insurance claim form stamped",
            settings.getDoctors().get(2), Priority.NORMAL, Stage.BILLING, now.minusMinutes(50));
        p6.moveToStage(Stage.WITH_DOCTOR, now.minusMinutes(35));
        p6.moveToStage(Stage.BILLING, now.minusMinutes(5));

        // 7. Completed (Done)
        Patient p7 = new Patient(nextPatientId.getAndIncrement(), tokenGenerator.nextToken(),
            "Karan Malhotra", 25, "9955443322", "Allergy rash consultation", "Prescription dispensed",
            settings.getDoctors().get(0), Priority.NORMAL, Stage.DONE, now.minusMinutes(90));
        p7.moveToStage(Stage.WITH_DOCTOR, now.minusMinutes(70));
        p7.moveToStage(Stage.BILLING, now.minusMinutes(50));
        p7.moveToStage(Stage.DONE, now.minusMinutes(40));

        this.patients.addAll(Arrays.asList(p1, p2, p3, p4, p5, p6, p7));
        persistAll();
    }

    public synchronized void addPatientListener(PatientListener listener) {
        listeners.add(listener);
    }

    public synchronized void removePatientListener(PatientListener listener) {
        listeners.remove(listener);
    }

    private void firePatientDataChanged() {
        for (PatientListener l : listeners) {
            try {
                l.onPatientDataChanged();
            } catch (Exception e) {
                System.err.println("Listener error: " + e.getMessage());
            }
        }
    }

    private synchronized void persistAll() {
        try {
            dataStore.savePatients(this.patients);

            List<StageRecord> currentHistory = new ArrayList<>();
            for (Patient p : this.patients) {
                currentHistory.addAll(p.getHistory());
            }
            dataStore.saveHistory(currentHistory);
        } catch (IOException e) {
            System.err.println("Failed to auto-save patient flow data: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Core Patient Operations
    // -------------------------------------------------------------

    /**
     * Registers a new patient into the WAITING stage.
     */
    public synchronized Patient registerPatient(String name, int age, String phone,
                                                String reason, String notes, String doctor,
                                                Priority priority, User user) {
        int id = nextPatientId.getAndIncrement();
        String token = tokenGenerator.nextToken();
        LocalDateTime now = LocalDateTime.now();

        Patient patient = new Patient(id, token, name, age, phone, reason, notes, doctor, priority, Stage.WAITING, now);
        this.patients.add(patient);

        persistAll();
        firePatientDataChanged();
        return patient;
    }

    /**
     * Moves a patient to a target stage after validating workflow rules and role permissions.
     *
     * @return null if successful, or an error message explaining why the move was blocked.
     */
    public synchronized String movePatient(int patientId, Stage targetStage, User user) {
        Patient patient = findPatientById(patientId);
        if (patient == null) {
            return "Patient not found (ID: " + patientId + ").";
        }
        if (patient.isCancelled()) {
            return "Cannot move a cancelled patient.";
        }

        Role role = (user != null) ? user.getRole() : Role.ADMIN;
        Stage currentStage = patient.getStage();

        if (currentStage == targetStage) {
            return "Patient " + patient.getToken() + " is already in " + targetStage.getTitle() + ".";
        }

        // Validate transition
        if (!currentStage.canTransitionTo(targetStage, role)) {
            return patient.getToken() + " (" + patient.getName() + "): " +
                   currentStage.getTransitionBlockedReason(targetStage, role);
        }

        // Doctor role check: Doctors can move patients to With Doctor, Tests, Billing, Done
        if (role == Role.DOCTOR && currentStage == Stage.WAITING && targetStage != Stage.WITH_DOCTOR) {
            return "Doctors can only call patients from Waiting into 'With Doctor'.";
        }

        // Execute move
        patient.moveToStage(targetStage, LocalDateTime.now());

        persistAll();
        firePatientDataChanged();
        return null; // success
    }

    /**
     * Calls the top patient in Waiting into 'With Doctor'.
     */
    public synchronized String callNext(String doctorName, User user) {
        Patient next = getNextPatientInQueue();
        if (next == null) {
            return "No waiting patients in queue.";
        }

        if (doctorName != null && !doctorName.trim().isEmpty()) {
            next.setDoctor(doctorName.trim());
        } else if (user != null && user.isDoctor() && user.getDoctorName() != null && !user.getDoctorName().isEmpty()) {
            next.setDoctor(user.getDoctorName());
        }

        return movePatient(next.getId(), Stage.WITH_DOCTOR, user);
    }

    /**
     * Edits patient personal details and priority.
     */
    public synchronized void editPatient(int patientId, String name, int age, String phone,
                                         String reason, String notes, String doctor,
                                         Priority priority, User user) {
        Patient patient = findPatientById(patientId);
        if (patient == null) return;

        patient.setName(name);
        patient.setAge(age);
        patient.setPhone(phone);
        patient.setReason(reason);
        patient.setNotes(notes);
        patient.setDoctor(doctor);
        patient.setPriority(priority);

        persistAll();
        firePatientDataChanged();
    }

    /**
     * Cancels or removes a patient with a documented reason.
     */
    public synchronized boolean cancelPatient(int patientId, String reason, User user) {
        Patient patient = findPatientById(patientId);
        if (patient == null) return false;

        patient.cancel(reason);
        persistAll();
        firePatientDataChanged();
        return true;
    }

    /**
     * Updates notes for a patient.
     */
    public synchronized void updateNotes(int patientId, String notes) {
        Patient patient = findPatientById(patientId);
        if (patient == null) return;
        patient.setNotes(notes);
        persistAll();
        firePatientDataChanged();
    }

    public synchronized Patient findPatientById(int id) {
        for (Patient p : patients) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    public synchronized List<Patient> getAllPatients() {
        return new ArrayList<>(patients);
    }

    public synchronized List<Patient> getTodayPatients() {
        LocalDate today = LocalDate.now();
        return patients.stream()
            .filter(p -> p.getRegisteredAt() != null && p.getRegisteredAt().toLocalDate().equals(today))
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------
    // Queue Ordering & Filtering
    // -------------------------------------------------------------

    /**
     * Gets next patient in Waiting queue (Emergency first, then FCFS).
     */
    public synchronized Patient getNextPatientInQueue() {
        List<Patient> waiting = getPatientsForStage(Stage.WAITING, null, null, null);
        return waiting.isEmpty() ? null : waiting.get(0);
    }

    /**
     * Retrieves patients for a specific stage, sorted according to clinic queue rules:
     * - WAITING stage: Priority rank descending (EMERGENCY > SENIOR_CHILD > NORMAL),
     *   then registration time ascending (FCFS).
     * - Other stages: Priority rank descending (Emergency first), then current stage entry time ascending.
     * Applies search queries and doctor/priority filters.
     */
    public synchronized List<Patient> getPatientsForStage(Stage stage, String search, String doctor, Priority priority) {
        return patients.stream()
            .filter(p -> !p.isCancelled())
            .filter(p -> p.getStage() == stage)
            .filter(p -> matchesFilter(p, search, doctor, priority))
            .sorted((p1, p2) -> {
                if (stage == Stage.WAITING) {
                    // 1. Priority descending (EMERGENCY = 3, SENIOR_CHILD = 2, NORMAL = 1)
                    int pComp = Integer.compare(p2.getPriority().getRank(), p1.getPriority().getRank());
                    if (pComp != 0) return pComp;
                    // 2. Registration time ascending (FCFS)
                    return p1.getRegisteredAt().compareTo(p2.getRegisteredAt());
                } else {
                    // For other stages: Emergency first, then enteredAt ascending
                    int pComp = Integer.compare(p2.getPriority().getRank(), p1.getPriority().getRank());
                    if (pComp != 0 && (p1.getPriority().isEmergency() || p2.getPriority().isEmergency())) {
                        return pComp;
                    }
                    StageRecord r1 = p1.getCurrentStageRecord();
                    StageRecord r2 = p2.getCurrentStageRecord();
                    LocalDateTime t1 = (r1 != null && r1.getEnteredAt() != null) ? r1.getEnteredAt() : p1.getRegisteredAt();
                    LocalDateTime t2 = (r2 != null && r2.getEnteredAt() != null) ? r2.getEnteredAt() : p2.getRegisteredAt();
                    return t1.compareTo(t2);
                }
            })
            .collect(Collectors.toList());
    }

    public synchronized int getCountForStage(Stage stage) {
        return (int) patients.stream()
            .filter(p -> !p.isCancelled() && p.getStage() == stage)
            .count();
    }

    private boolean matchesFilter(Patient p, String search, String doctor, Priority priority) {
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            boolean matchToken = p.getToken() != null && p.getToken().toLowerCase().contains(q);
            boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(q);
            boolean matchPhone = p.getPhone() != null && p.getPhone().toLowerCase().contains(q);
            if (!matchToken && !matchName && !matchPhone) {
                return false;
            }
        }

        if (doctor != null && !doctor.trim().isEmpty() && !doctor.equalsIgnoreCase("All Doctors")) {
            if (p.getDoctor() == null || !p.getDoctor().equalsIgnoreCase(doctor.trim())) {
                return false;
            }
        }

        if (priority != null) {
            if (p.getPriority() != priority) {
                return false;
            }
        }

        return true;
    }
}
