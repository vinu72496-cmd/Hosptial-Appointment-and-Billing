package clinicflow.util;

/**
 * Validation utilities for patient registration and editing.
 */
public final class Validator {

    private Validator() {
        // Prevent instantiation
    }

    public static class Result {
        private final boolean valid;
        private final String message;

        public Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    /**
     * Validates patient name (required, non-empty, at least 2 characters).
     */
    public static Result validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Result.fail("Patient name is required.");
        }
        if (name.trim().length() < 2) {
            return Result.fail("Patient name must be at least 2 characters long.");
        }
        return Result.ok();
    }

    /**
     * Validates patient age (0 to 120 inclusive).
     */
    public static Result validateAge(String ageStr) {
        if (ageStr == null || ageStr.trim().isEmpty()) {
            return Result.fail("Patient age is required.");
        }
        try {
            int age = Integer.parseInt(ageStr.trim());
            return validateAge(age);
        } catch (NumberFormatException e) {
            return Result.fail("Age must be a valid whole number.");
        }
    }

    /**
     * Validates patient age integer (0 to 120 inclusive).
     */
    public static Result validateAge(int age) {
        if (age < 0 || age > 120) {
            return Result.fail("Age must be between 0 and 120.");
        }
        return Result.ok();
    }

    /**
     * Validates phone number (optional; if provided, must be exactly 10 digits).
     */
    public static Result validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Result.ok(); // Optional
        }
        String clean = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");
        if (!clean.matches("^\\d{10}$")) {
            return Result.fail("Phone number must be exactly 10 digits if provided.");
        }
        return Result.ok();
    }
}
