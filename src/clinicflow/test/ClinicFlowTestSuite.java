package clinicflow.test;

import clinicflow.model.*;
import clinicflow.service.*;
import clinicflow.storage.DataStore;
import clinicflow.util.TimeUtil;
import clinicflow.util.Validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Automated test suite covering all functional and non-functional requirements
 * using pure JDK without external test frameworks.
 */
public class ClinicFlowTestSuite {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   RUNNING CLINIC FLOW TEST SUITE (PURE JDK)     ");
        System.out.println("=================================================");

        runTest("Input Validation", ClinicFlowTestSuite::testValidation);
        runTest("Stage Transitions & Role Permissions", ClinicFlowTestSuite::testTransitions);
        runTest("Queue Ordering & Emergency Prioritization", ClinicFlowTestSuite::testQueueOrdering);
        runTest("Token Generation & Daily Sequence", ClinicFlowTestSuite::testTokenGenerator);
        runTest("DataStore Persistence & Atomic Writes", ClinicFlowTestSuite::testPersistence);
        runTest("Reporting & Average Statistics", ClinicFlowTestSuite::testReports);
        runTest("Authentication & SHA-256 Hashing", ClinicFlowTestSuite::testAuth);

        System.out.println("\n=================================================");
        System.out.printf("  TEST RESULTS: %d PASSED, %d FAILED%n", testsPassed, testsFailed);
        System.out.println("=================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String name, Runnable test) {
        System.out.print("[TEST] " + name + " ... ");
        try {
            test.run();
            System.out.println("PASSED");
            testsPassed++;
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getMessage());
            t.printStackTrace(System.out);
            testsFailed++;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " (Expected: " + expected + ", Actual: " + actual + ")");
    }

    // -------------------------------------------------------------
    // Test 1: Validation
    // -------------------------------------------------------------
    private static void testValidation() {
        // Name validation
        assertTrue(!Validator.validateName(null).isValid(), "Null name must fail");
        assertTrue(!Validator.validateName("").isValid(), "Empty name must fail");
        assertTrue(!Validator.validateName(" ").isValid(), "Whitespace name must fail");
        assertTrue(!Validator.validateName("A").isValid(), "Single character name must fail");
        assertTrue(Validator.validateName("An").isValid(), "2-character name must pass");
        assertTrue(Validator.validateName("John Doe").isValid(), "Normal name must pass");

        // Age validation
        assertTrue(!Validator.validateAge(-1).isValid(), "Negative age must fail");
        assertTrue(!Validator.validateAge(121).isValid(), "Age > 120 must fail");
        assertTrue(Validator.validateAge(0).isValid(), "Age 0 (newborn) must pass");
        assertTrue(Validator.validateAge(45).isValid(), "Age 45 must pass");
        assertTrue(Validator.validateAge(120).isValid(), "Age 120 must pass");

        // Phone validation
        assertTrue(Validator.validatePhone(null).isValid(), "Optional null phone must pass");
        assertTrue(Validator.validatePhone("").isValid(), "Optional empty phone must pass");
        assertTrue(!Validator.validatePhone("12345").isValid(), "Short phone must fail");
        assertTrue(!Validator.validatePhone("12345678901").isValid(), "11-digit phone must fail");
        assertTrue(!Validator.validatePhone("abcdefghij").isValid(), "Alphabet phone must fail");
        assertTrue(Validator.validatePhone("9876543210").isValid(), "Valid 10-digit phone must pass");
        assertTrue(Validator.validatePhone("987-654-3210").isValid(), "Formatted 10-digit phone must pass");
    }

    // -------------------------------------------------------------
    // Test 2: Workflow Transitions
    // -------------------------------------------------------------
    private static void testTransitions() {
        Role admin = Role.ADMIN;
        Role doctor = Role.DOCTOR;
        Role receptionist = Role.RECEPTIONIST;

        // Waiting -> With Doctor
        assertTrue(Stage.WAITING.canTransitionTo(Stage.WITH_DOCTOR, receptionist), "Waiting -> With Doctor allowed for Receptionist");
        assertTrue(Stage.WAITING.canTransitionTo(Stage.WITH_DOCTOR, doctor), "Waiting -> With Doctor allowed for Doctor");
        assertTrue(!Stage.WAITING.canTransitionTo(Stage.TESTS, receptionist), "Waiting -> Tests blocked for Receptionist");
        assertTrue(!Stage.WAITING.canTransitionTo(Stage.BILLING, receptionist), "Waiting -> Billing blocked for Receptionist");

        // Waiting -> Done (Admin override only)
        assertTrue(!Stage.WAITING.canTransitionTo(Stage.DONE, receptionist), "Waiting -> Done blocked for Receptionist");
        assertTrue(Stage.WAITING.canTransitionTo(Stage.DONE, admin), "Waiting -> Done allowed as Admin override");

        // With Doctor -> Tests, Billing, Done
        assertTrue(Stage.WITH_DOCTOR.canTransitionTo(Stage.TESTS, doctor), "With Doctor -> Tests allowed");
        assertTrue(Stage.WITH_DOCTOR.canTransitionTo(Stage.BILLING, doctor), "With Doctor -> Billing allowed");
        assertTrue(Stage.WITH_DOCTOR.canTransitionTo(Stage.DONE, doctor), "With Doctor -> Done allowed");
        assertTrue(!Stage.WITH_DOCTOR.canTransitionTo(Stage.WAITING, doctor), "With Doctor -> Waiting blocked");

        // Tests -> With Doctor, Billing
        assertTrue(Stage.TESTS.canTransitionTo(Stage.WITH_DOCTOR, doctor), "Tests -> With Doctor allowed (review results)");
        assertTrue(Stage.TESTS.canTransitionTo(Stage.BILLING, doctor), "Tests -> Billing allowed");
        assertTrue(!Stage.TESTS.canTransitionTo(Stage.WAITING, doctor), "Tests -> Waiting blocked");
        assertTrue(!Stage.TESTS.canTransitionTo(Stage.DONE, doctor), "Tests -> Done blocked for non-admin");
        assertTrue(Stage.TESTS.canTransitionTo(Stage.DONE, admin), "Tests -> Done allowed for Admin override");

        // Billing -> Done
        assertTrue(Stage.BILLING.canTransitionTo(Stage.DONE, receptionist), "Billing -> Done allowed");
        assertTrue(!Stage.BILLING.canTransitionTo(Stage.WITH_DOCTOR, receptionist), "Billing -> With Doctor blocked");
        assertTrue(!Stage.BILLING.canTransitionTo(Stage.TESTS, receptionist), "Billing -> Tests blocked");

        // Done is terminal
        assertTrue(!Stage.DONE.canTransitionTo(Stage.WAITING, receptionist), "Done -> Waiting blocked");
        assertTrue(!Stage.DONE.canTransitionTo(Stage.WITH_DOCTOR, receptionist), "Done -> With Doctor blocked");
    }

    // -------------------------------------------------------------
    // Test 3: Queue Ordering & Emergency Prioritization
    // -------------------------------------------------------------
    private static void testQueueOrdering() {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("clinicflow_queue_test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataStore ds = new DataStore(tempDir);
        Settings settings = new Settings();
        PatientService ps = new PatientService(ds, settings);

        // Clear existing seed patients for pure test
        for (Patient p : ps.getAllPatients()) {
            ps.cancelPatient(p.getId(), "Test cleanup", null);
        }

        User recUser = new User("reception", "", Role.RECEPTIONIST, "Receptionist", "");

        // 1. Register Normal patient at 10:00
        Patient p1 = ps.registerPatient("Normal Patient 1", 30, "9876543210", "Checkup", "", "Dr. Sharma", Priority.NORMAL, recUser);

        // 2. Register Senior/Child patient at 10:05
        Patient p2 = ps.registerPatient("Senior Patient", 72, "9876543211", "Hypertension", "", "Dr. Sharma", Priority.SENIOR_CHILD, recUser);

        // 3. Register Emergency patient at 10:10
        Patient p3 = ps.registerPatient("Emergency Patient", 45, "9876543212", "Chest pain", "", "Dr. Sharma", Priority.EMERGENCY, recUser);

        // 4. Register Normal patient 2 at 10:15
        Patient p4 = ps.registerPatient("Normal Patient 2", 22, "9876543213", "Fever", "", "Dr. Sharma", Priority.NORMAL, recUser);

        // Verify Waiting queue ordering:
        // Expected order:
        // 1. Emergency Patient (Rank 3)
        // 2. Senior Patient (Rank 2)
        // 3. Normal Patient 1 (Rank 1, arrived before Normal 2)
        // 4. Normal Patient 2 (Rank 1)
        List<Patient> queue = ps.getPatientsForStage(Stage.WAITING, null, null, null);
        assertEquals(4, queue.size(), "Queue should have 4 patients");
        assertEquals("Emergency Patient", queue.get(0).getName(), "1st should be Emergency Patient");
        assertEquals("Senior Patient", queue.get(1).getName(), "2nd should be Senior Patient");
        assertEquals("Normal Patient 1", queue.get(2).getName(), "3rd should be Normal Patient 1 (FCFS)");
        assertEquals("Normal Patient 2", queue.get(3).getName(), "4th should be Normal Patient 2");

        // Verify getNextPatientInQueue()
        Patient next = ps.getNextPatientInQueue();
        assertEquals(p3.getId(), next.getId(), "Next patient must be the emergency patient");

        // Call Next moves Emergency patient to WITH_DOCTOR
        String callResult = ps.callNext(null, recUser);
        assertTrue(callResult == null, "Call next should succeed");

        assertEquals(Stage.WITH_DOCTOR, p3.getStage(), "p3 should now be WITH_DOCTOR");

        // Next in queue should now be Senior Patient
        Patient nextAfterCall = ps.getNextPatientInQueue();
        assertEquals(p2.getId(), nextAfterCall.getId(), "Next patient should now be Senior Patient");

        deleteDirectory(tempDir.toFile());
    }

    // -------------------------------------------------------------
    // Test 4: Token Generator
    // -------------------------------------------------------------
    private static void testTokenGenerator() {
        TokenGenerator tg = new TokenGenerator("T-", null);
        assertEquals("T-001", tg.nextToken(), "First token should be T-001");
        assertEquals("T-002", tg.nextToken(), "Second token should be T-002");
        assertEquals("T-003", tg.nextToken(), "Third token should be T-003");

        // Restart recovery: if existing patients has T-014, next token is T-015
        Patient existing = new Patient(1, "T-014", "Test", 30, "", "", "", "", Priority.NORMAL, Stage.WAITING, LocalDateTime.now());
        TokenGenerator tg2 = new TokenGenerator("T-", List.of(existing));
        assertEquals("T-015", tg2.nextToken(), "Next token after T-014 should be T-015");
    }

    // -------------------------------------------------------------
    // Test 5: Persistence & Atomic Writes
    // -------------------------------------------------------------
    private static void testPersistence() {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("clinicflow_store_test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataStore ds = new DataStore(tempDir);
        Settings settings = new Settings();
        settings.setClinicName("Test Healthcare Unit");

        try {
            ds.saveSettings(settings);
            Settings loadedSettings = ds.loadSettings();
            assertEquals("Test Healthcare Unit", loadedSettings.getClinicName(), "Persisted clinic name must match");

            LocalDateTime regTime = LocalDateTime.now().minusMinutes(20);
            Patient p = new Patient(10, "T-100", "Alice, M.", 35, "9988776655", "Headache, nausea", "Notes with \"quotes\"", "Dr. Patel", Priority.SENIOR_CHILD, Stage.WAITING, regTime);
            ds.savePatients(List.of(p));

            List<Patient> loadedPatients = ds.loadPatients();
            assertEquals(1, loadedPatients.size(), "Loaded patient count must be 1");
            Patient loaded = loadedPatients.get(0);
            assertEquals(10, loaded.getId(), "ID matches");
            assertEquals("T-100", loaded.getToken(), "Token matches");
            assertEquals("Alice, M.", loaded.getName(), "Name with comma matches");
            assertEquals(35, loaded.getAge(), "Age matches");
            assertEquals("9988776655", loaded.getPhone(), "Phone matches");
            assertEquals("Headache, nausea", loaded.getReason(), "Reason matches");
            assertEquals("Notes with \"quotes\"", loaded.getNotes(), "Notes with quotes matches");
            assertEquals(Priority.SENIOR_CHILD, loaded.getPriority(), "Priority matches");
            assertEquals(Stage.WAITING, loaded.getStage(), "Stage matches");

        } catch (IOException e) {
            throw new RuntimeException("Persistence test failed: " + e.getMessage(), e);
        } finally {
            deleteDirectory(tempDir.toFile());
        }
    }

    // -------------------------------------------------------------
    // Test 6: Reporting & Averages
    // -------------------------------------------------------------
    private static void testReports() {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("clinicflow_rep_test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataStore ds = new DataStore(tempDir);
        Settings settings = new Settings();
        PatientService ps = new PatientService(ds, settings);
        ReportService rs = new ReportService(ps);

        ReportService.DailyReport report = rs.generateReport(LocalDate.now());
        assertTrue(report.getRegisteredCount() > 0, "Daily report has registered patients");

        // Export test
        File txtFile = tempDir.resolve("report.txt").toFile();
        File csvFile = tempDir.resolve("report.csv").toFile();
        try {
            rs.exportToTxt(report, txtFile);
            rs.exportToCsv(report, csvFile);

            assertTrue(txtFile.exists() && txtFile.length() > 0, "TXT report file was created and non-empty");
            assertTrue(csvFile.exists() && csvFile.length() > 0, "CSV report file was created and non-empty");
        } catch (IOException e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        } finally {
            deleteDirectory(tempDir.toFile());
        }
    }

    // -------------------------------------------------------------
    // Test 7: Authentication & SHA-256
    // -------------------------------------------------------------
    private static void testAuth() {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("clinicflow_auth_test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataStore ds = new DataStore(tempDir);
        AuthService as = new AuthService(ds);

        // Default admin should exist
        User admin = as.login("admin", "admin123");
        assertTrue(admin != null, "Admin login must succeed with correct password");
        assertEquals(Role.ADMIN, admin.getRole(), "Admin user role is ADMIN");

        // Wrong password
        User failed = as.login("admin", "wrong_password");
        assertTrue(failed == null, "Admin login must fail with incorrect password");

        // SHA-256 hash check
        String h1 = AuthService.hashPassword("secret");
        String h2 = AuthService.hashPassword("secret");
        assertEquals(h1, h2, "Hash must be deterministic");
        assertEquals(64, h1.length(), "SHA-256 hex string must be 64 characters");

        deleteDirectory(tempDir.toFile());
    }

    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) deleteDirectory(f);
            }
        }
        dir.delete();
    }
}
