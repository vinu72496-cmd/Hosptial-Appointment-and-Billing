package clinicflow.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for formatting and parsing dates, times, and durations.
 */
public final class TimeUtil {

    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    public static final DateTimeFormatter DISPLAY_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
    public static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeUtil() {
        // Prevent instantiation
    }

    /**
     * Formats a duration into mm:ss or hh:mm:ss for timer displays.
     *
     * @param duration the duration to format
     * @return formatted string, e.g. "04:15" or "01:23:45"
     */
    public static String formatTimer(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "00:00";
        }
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats duration into human-readable text (e.g. "18 min", "1 hr 12 min").
     *
     * @param duration the duration
     * @return formatted text
     */
    public static String formatHuman(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "0 min";
        }
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%d hr %d min", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d min", minutes);
        } else {
            return "< 1 min";
        }
    }

    /**
     * Formats a LocalDateTime for display.
     */
    public static String formatTime(LocalDateTime dt) {
        if (dt == null) return "--:--";
        return dt.format(DISPLAY_TIME_FORMATTER);
    }

    /**
     * Formats a LocalDateTime with date and time.
     */
    public static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "--";
        return dt.format(DISPLAY_DATETIME_FORMATTER);
    }

    /**
     * Formats a LocalDateTime into standard ISO string for CSV storage.
     */
    public static String toIsoString(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(ISO_FORMATTER);
    }

    /**
     * Parses an ISO string back to LocalDateTime. Returns null on empty or invalid string.
     */
    public static LocalDateTime parseIsoString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(str.trim(), ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
