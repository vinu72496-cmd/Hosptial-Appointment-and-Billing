package clinicflow.service;

import clinicflow.model.Patient;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique daily sequential token numbers (e.g. T-001, T-002),
 * resetting each calendar day and maintaining state across app restarts.
 */
public class TokenGenerator {
    private final String prefix;
    private LocalDate currentDate;
    private final AtomicInteger counter;

    public TokenGenerator(String prefix, List<Patient> existingPatients) {
        this.prefix = (prefix != null && !prefix.trim().isEmpty()) ? prefix.trim() : "T-";
        this.currentDate = LocalDate.now();
        this.counter = new AtomicInteger(0);

        initFromExisting(existingPatients);
    }

    private synchronized void initFromExisting(List<Patient> existingPatients) {
        int maxSeqToday = 0;
        LocalDate today = LocalDate.now();

        if (existingPatients != null) {
            for (Patient p : existingPatients) {
                if (p.getRegisteredAt() != null && p.getRegisteredAt().toLocalDate().equals(today)) {
                    String token = p.getToken();
                    if (token != null && token.startsWith(prefix)) {
                        String numPart = token.substring(prefix.length());
                        try {
                            int seq = Integer.parseInt(numPart);
                            if (seq > maxSeqToday) {
                                maxSeqToday = seq;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        counter.set(maxSeqToday);
    }

    /**
     * Generates the next sequential token number for today.
     * Automatically resets if calendar day has changed.
     */
    public synchronized String nextToken() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            currentDate = today;
            counter.set(0);
        }
        int next = counter.incrementAndGet();
        return String.format("%s%03d", prefix, next);
    }

    /**
     * Resets the token counter manually if needed.
     */
    public synchronized void reset() {
        counter.set(0);
        currentDate = LocalDate.now();
    }
}
