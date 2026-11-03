package clinicflow.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * System settings and configurable thresholds.
 */
public class Settings {
    private String clinicName;
    private List<String> doctors;
    private int waitWarningMinutes;
    private int waitDangerMinutes;
    private boolean soundAlertsEnabled;
    private boolean darkMode;
    private String tokenPrefix;

    public Settings() {
        this.clinicName = "Apex Care Clinic";
        this.doctors = new ArrayList<>(Arrays.asList(
            "Dr. A. Sharma (General Medicine)",
            "Dr. P. Patel (Pediatrics)",
            "Dr. S. Roy (Cardiology)",
            "Dr. M. Gupta (Orthopedics)"
        ));
        this.waitWarningMinutes = 30;
        this.waitDangerMinutes = 60;
        this.soundAlertsEnabled = true;
        this.darkMode = false;
        this.tokenPrefix = "T-";
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public List<String> getDoctors() {
        return new ArrayList<>(doctors);
    }

    public void setDoctors(List<String> doctors) {
        this.doctors = new ArrayList<>(doctors);
    }

    public int getWaitWarningMinutes() {
        return waitWarningMinutes;
    }

    public void setWaitWarningMinutes(int waitWarningMinutes) {
        this.waitWarningMinutes = Math.max(1, waitWarningMinutes);
    }

    public int getWaitDangerMinutes() {
        return waitDangerMinutes;
    }

    public void setWaitDangerMinutes(int waitDangerMinutes) {
        this.waitDangerMinutes = Math.max(waitWarningMinutes + 1, waitDangerMinutes);
    }

    public boolean isSoundAlertsEnabled() {
        return soundAlertsEnabled;
    }

    public void setSoundAlertsEnabled(boolean soundAlertsEnabled) {
        this.soundAlertsEnabled = soundAlertsEnabled;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }
}
