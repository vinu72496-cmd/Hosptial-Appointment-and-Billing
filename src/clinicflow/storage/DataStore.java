package clinicflow.storage;

import clinicflow.model.*;
import clinicflow.util.TimeUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles persistent file storage using CSV and properties files with atomic writes.
 */
public class DataStore {

    private final Path dataDirectory;
    private final Path patientsFile;
    private final Path historyFile;
    private final Path usersFile;
    private final Path settingsFile;

    public DataStore() {
        this(Paths.get("data"));
    }

    public DataStore(Path dataDir) {
        this.dataDirectory = dataDir;
        this.patientsFile = dataDir.resolve("patients.csv");
        this.historyFile = dataDir.resolve("history.csv");
        this.usersFile = dataDir.resolve("users.csv");
        this.settingsFile = dataDir.resolve("settings.properties");
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Atomic Write Utility
    // -------------------------------------------------------------

    private synchronized void writeLinesAtomically(Path targetPath, List<String> lines) throws IOException {
        ensureDirectoryExists();
        Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp");
        Path backupPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".bak");

        // Write to temp file
        try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        // Backup existing file if present
        if (Files.exists(targetPath)) {
            try {
                Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }

        // Atomic move
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // -------------------------------------------------------------
    // CSV Utilities
    // -------------------------------------------------------------

    public static String escapeCsv(String value) {
        if (value == null) return "";
        boolean containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (containsSpecial) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) return result;

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                        sb.append('\"');
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '\"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(sb.toString().trim());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        result.add(sb.toString().trim());
        return result;
    }

    // -------------------------------------------------------------
    // Patients Storage
    // -------------------------------------------------------------

    public synchronized void savePatients(List<Patient> patients) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id,token,name,age,phone,reason,notes,doctor,priority,stage,registeredAt,cancelled,cancellationReason");

        for (Patient p : patients) {
            String line = String.join(",",
                String.valueOf(p.getId()),
                escapeCsv(p.getToken()),
                escapeCsv(p.getName()),
                String.valueOf(p.getAge()),
                escapeCsv(p.getPhone()),
                escapeCsv(p.getReason()),
                escapeCsv(p.getNotes()),
                escapeCsv(p.getDoctor()),
                p.getPriority().name(),
                p.getStage().name(),
                escapeCsv(TimeUtil.toIsoString(p.getRegisteredAt())),
                String.valueOf(p.isCancelled()),
                escapeCsv(p.getCancellationReason())
            );
            lines.add(line);
        }

        writeLinesAtomically(patientsFile, lines);
    }

    public synchronized List<Patient> loadPatients() {
        List<Patient> list = new ArrayList<>();
        if (!Files.exists(patientsFile)) {
            return list;
        }

        try (BufferedReader reader = Files.newBufferedReader(patientsFile, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    List<String> cols = parseCsvLine(line);
                    if (cols.size() < 11) continue;

                    int id = Integer.parseInt(cols.get(0));
                    String token = cols.get(1);
                    String name = cols.get(2);
                    int age = Integer.parseInt(cols.get(3));
                    String phone = cols.get(4);
                    String reason = cols.get(5);
                    String notes = cols.get(6);
                    String doctor = cols.get(7);
                    Priority priority = Priority.valueOf(cols.get(8));
                    Stage stage = Stage.valueOf(cols.get(9));
                    LocalDateTime registeredAt = TimeUtil.parseIsoString(cols.get(10));

                    Patient p = new Patient(id, token, name, age, phone, reason, notes, doctor, priority, stage, registeredAt);
                    if (cols.size() >= 12) {
                        p.setCancelled(Boolean.parseBoolean(cols.get(11)));
                    }
                    if (cols.size() >= 13) {
                        p.setCancellationReason(cols.get(12));
                    }
                    list.add(p);
                } catch (Exception e) {
                    System.err.println("Warning: Skipping malformed patient CSV line: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading patients file: " + e.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------
    // History (Stage Transitions) Storage
    // -------------------------------------------------------------

    public synchronized void saveHistory(List<StageRecord> records) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("patientId,stage,enteredAt,exitedAt");

        for (StageRecord r : records) {
            String line = String.join(",",
                String.valueOf(r.getPatientId()),
                r.getStage().name(),
                escapeCsv(TimeUtil.toIsoString(r.getEnteredAt())),
                escapeCsv(TimeUtil.toIsoString(r.getExitedAt()))
            );
            lines.add(line);
        }

        writeLinesAtomically(historyFile, lines);
    }

    public synchronized List<StageRecord> loadHistory() {
        List<StageRecord> list = new ArrayList<>();
        if (!Files.exists(historyFile)) {
            return list;
        }

        try (BufferedReader reader = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    List<String> cols = parseCsvLine(line);
                    if (cols.size() < 3) continue;

                    int patientId = Integer.parseInt(cols.get(0));
                    Stage stage = Stage.valueOf(cols.get(1));
                    LocalDateTime enteredAt = TimeUtil.parseIsoString(cols.get(2));
                    LocalDateTime exitedAt = cols.size() >= 4 ? TimeUtil.parseIsoString(cols.get(3)) : null;

                    list.add(new StageRecord(patientId, stage, enteredAt, exitedAt));
                } catch (Exception e) {
                    System.err.println("Warning: Skipping malformed history line: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading history file: " + e.getMessage());
        }
        return list;
    }

    // -------------------------------------------------------------
    // Settings Storage
    // -------------------------------------------------------------

    public synchronized void saveSettings(Settings settings) throws IOException {
        Properties props = new Properties();
        props.setProperty("clinic.name", settings.getClinicName());
        props.setProperty("clinic.doctors", String.join(";", settings.getDoctors()));
        props.setProperty("wait.warning.minutes", String.valueOf(settings.getWaitWarningMinutes()));
        props.setProperty("wait.danger.minutes", String.valueOf(settings.getWaitDangerMinutes()));
        props.setProperty("alerts.sound.enabled", String.valueOf(settings.isSoundAlertsEnabled()));
        props.setProperty("theme.dark", String.valueOf(settings.isDarkMode()));
        props.setProperty("token.prefix", settings.getTokenPrefix());

        Path tempPath = settingsFile.resolveSibling("settings.properties.tmp");
        try (OutputStream os = Files.newOutputStream(tempPath)) {
            props.store(os, "Clinic Flow Board Configuration");
        }
        try {
            Files.move(tempPath, settingsFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPath, settingsFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized Settings loadSettings() {
        Settings settings = new Settings();
        if (!Files.exists(settingsFile)) {
            try {
                saveSettings(settings);
            } catch (IOException ignored) {}
            return settings;
        }

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(settingsFile)) {
            props.load(is);
            if (props.containsKey("clinic.name")) {
                settings.setClinicName(props.getProperty("clinic.name"));
            }
            if (props.containsKey("clinic.doctors")) {
                String docStr = props.getProperty("clinic.doctors");
                String[] docs = docStr.split(";");
                List<String> docList = new ArrayList<>();
                for (String d : docs) {
                    if (!d.trim().isEmpty()) docList.add(d.trim());
                }
                if (!docList.isEmpty()) {
                    settings.setDoctors(docList);
                }
            }
            if (props.containsKey("wait.warning.minutes")) {
                settings.setWaitWarningMinutes(Integer.parseInt(props.getProperty("wait.warning.minutes")));
            }
            if (props.containsKey("wait.danger.minutes")) {
                settings.setWaitDangerMinutes(Integer.parseInt(props.getProperty("wait.danger.minutes")));
            }
            if (props.containsKey("alerts.sound.enabled")) {
                settings.setSoundAlertsEnabled(Boolean.parseBoolean(props.getProperty("alerts.sound.enabled")));
            }
            if (props.containsKey("theme.dark")) {
                settings.setDarkMode(Boolean.parseBoolean(props.getProperty("theme.dark")));
            }
            if (props.containsKey("token.prefix")) {
                settings.setTokenPrefix(props.getProperty("token.prefix"));
            }
        } catch (Exception e) {
            System.err.println("Error reading settings, using defaults: " + e.getMessage());
        }
        return settings;
    }

    // -------------------------------------------------------------
    // Users Storage
    // -------------------------------------------------------------

    public synchronized void saveUsers(List<User> users) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("username,passwordHash,role,displayName,doctorName");
        for (User u : users) {
            lines.add(String.join(",",
                escapeCsv(u.getUsername()),
                escapeCsv(u.getPasswordHash()),
                u.getRole().name(),
                escapeCsv(u.getDisplayName()),
                escapeCsv(u.getDoctorName() != null ? u.getDoctorName() : "")
            ));
        }
        writeLinesAtomically(usersFile, lines);
    }

    public synchronized List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        if (!Files.exists(usersFile)) {
            return list;
        }

        try (BufferedReader reader = Files.newBufferedReader(usersFile, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    List<String> cols = parseCsvLine(line);
                    if (cols.size() < 4) continue;
                    String username = cols.get(0);
                    String passHash = cols.get(1);
                    Role role = Role.valueOf(cols.get(2));
                    String displayName = cols.get(3);
                    String doctorName = cols.size() >= 5 ? cols.get(4) : "";
                    list.add(new User(username, passHash, role, displayName, doctorName));
                } catch (Exception e) {
                    System.err.println("Warning: Skipping malformed user line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading users file: " + e.getMessage());
        }
        return list;
    }
}
