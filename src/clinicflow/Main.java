package clinicflow;

import clinicflow.model.Settings;
import clinicflow.service.AuthService;
import clinicflow.service.PatientService;
import clinicflow.service.ReportService;
import clinicflow.storage.DataStore;
import clinicflow.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Paths;

/**
 * Main application entry point for Clinic Patient Flow Board (Kanban for Waiting Rooms).
 */
public class Main {

    public static void main(String[] args) {
        // Set system look and feel with fallbacks
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fallback to default Swing Look & Feel
        }

        // Configure smooth font rendering and antialiasing hints across Swing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize storage and core services
                DataStore dataStore = new DataStore(Paths.get("data"));
                Settings settings = dataStore.loadSettings();

                AuthService authService = new AuthService(dataStore);
                PatientService patientService = new PatientService(dataStore, settings);
                ReportService reportService = new ReportService(patientService);

                // Launch Main Application Window
                MainFrame mainFrame = new MainFrame(patientService, authService, reportService, dataStore, settings);
                mainFrame.setVisible(true);

                System.out.println("=================================================");
                System.out.println("  Clinic Patient Flow Board started successfully!");
                System.out.println("  Clinic: " + settings.getClinicName());
                System.out.println("  Active User: " + authService.getCurrentUser());
                System.out.println("=================================================");

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Failed to launch Clinic Patient Flow Board:\n" + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
