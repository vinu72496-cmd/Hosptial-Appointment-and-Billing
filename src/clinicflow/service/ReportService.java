package clinicflow.service;

import clinicflow.model.*;
import clinicflow.util.TimeUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service calculating daily analytics, averages, bottlenecks, hourly arrivals,
 * and generating exportable reports.
 */
public class ReportService {

    public static class DailyReport {
        private final LocalDate date;
        private int registeredCount;
        private int completedCount;
        private int activeCount;
        private int cancelledCount;

        private Duration averageWaitTime = Duration.ZERO;
        private Duration averageConsultTime = Duration.ZERO;
        private Duration averageTotalVisitTime = Duration.ZERO;

        private Patient longestWaitPatient;
        private Duration longestWaitDuration = Duration.ZERO;

        private int busiestHour = -1;
        private int busiestHourCount = 0;

        private final Map<Integer, Integer> hourlyArrivals = new TreeMap<>();
        private final Map<String, Integer> doctorCounts = new TreeMap<>();
        private final List<Patient> patientList = new ArrayList<>();

        public DailyReport(LocalDate date) {
            this.date = date;
            for (int h = 8; h <= 20; h++) {
                hourlyArrivals.put(h, 0);
            }
        }

        public LocalDate getDate() { return date; }
        public int getRegisteredCount() { return registeredCount; }
        public int getCompletedCount() { return completedCount; }
        public int getActiveCount() { return activeCount; }
        public int getCancelledCount() { return cancelledCount; }
        public Duration getAverageWaitTime() { return averageWaitTime; }
        public Duration getAverageConsultTime() { return averageConsultTime; }
        public Duration getAverageTotalVisitTime() { return averageTotalVisitTime; }
        public Patient getLongestWaitPatient() { return longestWaitPatient; }
        public Duration getLongestWaitDuration() { return longestWaitDuration; }
        public int getBusiestHour() { return busiestHour; }
        public int getBusiestHourCount() { return busiestHourCount; }
        public Map<Integer, Integer> getHourlyArrivals() { return hourlyArrivals; }
        public Map<String, Integer> getDoctorCounts() { return doctorCounts; }
        public List<Patient> getPatientList() { return patientList; }
    }

    private final PatientService patientService;

    public ReportService(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Generates a comprehensive summary report for a given calendar date.
     */
    public DailyReport generateReport(LocalDate date) {
        if (date == null) date = LocalDate.now();
        DailyReport report = new DailyReport(date);

        List<Patient> all = patientService.getAllPatients();
        List<Patient> dayPatients = new ArrayList<>();

        for (Patient p : all) {
            if (p.getRegisteredAt() != null && p.getRegisteredAt().toLocalDate().equals(date)) {
                dayPatients.add(p);
            }
        }
        report.patientList.addAll(dayPatients);
        report.registeredCount = dayPatients.size();

        long totalWaitSeconds = 0;
        int waitRecordCount = 0;

        long totalConsultSeconds = 0;
        int consultRecordCount = 0;

        long totalVisitSeconds = 0;
        int completedVisitCount = 0;

        for (Patient p : dayPatients) {
            if (p.isCancelled()) {
                report.cancelledCount++;
            } else if (p.getStage() == Stage.DONE) {
                report.completedCount++;
            } else {
                report.activeCount++;
            }

            // Hourly distribution
            int hour = p.getRegisteredAt().getHour();
            report.hourlyArrivals.put(hour, report.hourlyArrivals.getOrDefault(hour, 0) + 1);

            // Doctor distribution
            if (p.getDoctor() != null && !p.getDoctor().trim().isEmpty()) {
                report.doctorCounts.put(p.getDoctor(), report.doctorCounts.getOrDefault(p.getDoctor(), 0) + 1);
            }

            // Stage durations
            for (StageRecord r : p.getHistory()) {
                if (r.getStage() == Stage.WAITING) {
                    Duration d = r.getDuration();
                    totalWaitSeconds += d.getSeconds();
                    waitRecordCount++;

                    if (d.compareTo(report.longestWaitDuration) > 0) {
                        report.longestWaitDuration = d;
                        report.longestWaitPatient = p;
                    }
                } else if (r.getStage() == Stage.WITH_DOCTOR) {
                    Duration d = r.getDuration();
                    totalConsultSeconds += d.getSeconds();
                    consultRecordCount++;
                }
            }

            // Total visit time for completed visits
            if (p.getStage() == Stage.DONE) {
                Duration visitDuration = p.getTotalVisitTime();
                totalVisitSeconds += visitDuration.getSeconds();
                completedVisitCount++;
            }
        }

        // Calculate averages
        if (waitRecordCount > 0) {
            report.averageWaitTime = Duration.ofSeconds(totalWaitSeconds / waitRecordCount);
        }
        if (consultRecordCount > 0) {
            report.averageConsultTime = Duration.ofSeconds(totalConsultSeconds / consultRecordCount);
        }
        if (completedVisitCount > 0) {
            report.averageTotalVisitTime = Duration.ofSeconds(totalVisitSeconds / completedVisitCount);
        }

        // Calculate busiest hour
        for (Map.Entry<Integer, Integer> entry : report.hourlyArrivals.entrySet()) {
            if (entry.getValue() > report.busiestHourCount) {
                report.busiestHourCount = entry.getValue();
                report.busiestHour = entry.getKey();
            }
        }

        return report;
    }

    /**
     * Exports the daily report to a readable plain text file.
     */
    public void exportToTxt(DailyReport report, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("=================================================================\n");
            writer.write("            CLINIC PATIENT FLOW - DAILY SUMMARY REPORT           \n");
            writer.write("=================================================================\n");
            writer.write("Date: " + report.getDate() + " | Generated At: " + TimeUtil.formatDateTime(LocalDateTime.now()) + "\n\n");

            writer.write("1. PATIENT VOLUME METRICS\n");
            writer.write("-----------------------------------------------------------------\n");
            writer.write(String.format("  - Total Patients Registered : %d\n", report.getRegisteredCount()));
            writer.write(String.format("  - Completed (Done)          : %d\n", report.getCompletedCount()));
            writer.write(String.format("  - Currently Active          : %d\n", report.getActiveCount()));
            writer.write(String.format("  - Cancelled Visits          : %d\n\n", report.getCancelledCount()));

            writer.write("2. WAIT TIMES & EFFICIENCY METRICS\n");
            writer.write("-----------------------------------------------------------------\n");
            writer.write(String.format("  - Average Wait Time (Waiting stage)    : %s\n", TimeUtil.formatHuman(report.getAverageWaitTime())));
            writer.write(String.format("  - Average Consultation Time            : %s\n", TimeUtil.formatHuman(report.getAverageConsultTime())));
            writer.write(String.format("  - Average Total Visit Time (Done)      : %s\n", TimeUtil.formatHuman(report.getAverageTotalVisitTime())));
            if (report.getLongestWaitPatient() != null) {
                writer.write(String.format("  - Longest Wait Today                   : %s (%s - %s)\n",
                    TimeUtil.formatHuman(report.getLongestWaitDuration()),
                    report.getLongestWaitPatient().getToken(),
                    report.getLongestWaitPatient().getName()));
            } else {
                writer.write("  - Longest Wait Today                   : None\n");
            }
            if (report.getBusiestHour() >= 0) {
                writer.write(String.format("  - Busiest Arrival Hour                 : %02d:00 - %02d:00 (%d patients)\n\n",
                    report.getBusiestHour(), report.getBusiestHour() + 1, report.getBusiestHourCount()));
            } else {
                writer.write("  - Busiest Arrival Hour                 : N/A\n\n");
            }

            writer.write("3. HOURLY PATIENT ARRIVALS\n");
            writer.write("-----------------------------------------------------------------\n");
            for (Map.Entry<Integer, Integer> e : report.getHourlyArrivals().entrySet()) {
                String bar = "=".repeat(Math.max(0, e.getValue() * 2));
                writer.write(String.format("  %02d:00 - %02d:00 | %2d patients | %s\n", e.getKey(), e.getKey() + 1, e.getValue(), bar));
            }
            writer.write("\n");

            writer.write("4. DOCTOR CONSULTATION BREAKDOWN\n");
            writer.write("-----------------------------------------------------------------\n");
            for (Map.Entry<String, Integer> e : report.getDoctorCounts().entrySet()) {
                writer.write(String.format("  - %-35s : %d patients\n", e.getKey(), e.getValue()));
            }
            writer.write("\n");

            writer.write("5. PATIENT VISIT LOG\n");
            writer.write("-----------------------------------------------------------------\n");
            writer.write(String.format("%-7s | %-18s | %-4s | %-12s | %-14s | %-8s | %-10s\n",
                "Token", "Name", "Age", "Priority", "Stage", "Wait", "Doctor"));
            writer.write("-----------------------------------------------------------------\n");
            for (Patient p : report.getPatientList()) {
                Duration wait = Duration.ZERO;
                for (StageRecord r : p.getHistory()) {
                    if (r.getStage() == Stage.WAITING) {
                        wait = r.getDuration();
                        break;
                    }
                }
                writer.write(String.format("%-7s | %-18s | %-4d | %-12s | %-14s | %-8s | %s\n",
                    p.getToken(),
                    truncate(p.getName(), 18),
                    p.getAge(),
                    p.getPriority().getDisplayName(),
                    p.isCancelled() ? "CANCELLED" : p.getStage().getTitle(),
                    TimeUtil.formatHuman(wait),
                    truncate(p.getDoctor(), 20)));
            }
            writer.write("=================================================================\n");
            writer.write("End of report\n");
        }
    }

    /**
     * Exports the daily summary report to CSV format.
     */
    public void exportToCsv(DailyReport report, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Token,Name,Age,Phone,Priority,Stage,RegisteredAt,WaitMinutes,ConsultMinutes,TotalVisitMinutes,Doctor,Reason,Cancelled,Notes\n");
            for (Patient p : report.getPatientList()) {
                long waitMin = 0;
                long consultMin = 0;
                for (StageRecord r : p.getHistory()) {
                    if (r.getStage() == Stage.WAITING) {
                        waitMin += r.getDuration().toMinutes();
                    } else if (r.getStage() == Stage.WITH_DOCTOR) {
                        consultMin += r.getDuration().toMinutes();
                    }
                }
                long totalMin = p.getTotalVisitTime().toMinutes();

                writer.write(String.join(",",
                    escape(p.getToken()),
                    escape(p.getName()),
                    String.valueOf(p.getAge()),
                    escape(p.getPhone()),
                    escape(p.getPriority().name()),
                    escape(p.getStage().name()),
                    escape(TimeUtil.toIsoString(p.getRegisteredAt())),
                    String.valueOf(waitMin),
                    String.valueOf(consultMin),
                    String.valueOf(totalMin),
                    escape(p.getDoctor()),
                    escape(p.getReason()),
                    String.valueOf(p.isCancelled()),
                    escape(p.getNotes())
                ));
                writer.newLine();
            }
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }
}
