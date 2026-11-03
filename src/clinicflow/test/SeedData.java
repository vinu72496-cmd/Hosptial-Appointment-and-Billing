package clinicflow.test;

import clinicflow.model.Settings;
import clinicflow.service.AuthService;
import clinicflow.service.PatientService;
import clinicflow.storage.DataStore;

import java.nio.file.Paths;

/**
 * Helper to pre-seed the data folder for immediate exploration.
 */
public class SeedData {
    public static void main(String[] args) {
        DataStore ds = new DataStore(Paths.get("data"));
        Settings settings = ds.loadSettings();
        AuthService auth = new AuthService(ds);
        PatientService ps = new PatientService(ds, settings);
        System.out.println("Data folder initialized successfully!");
        System.out.println("Total patients seeded: " + ps.getAllPatients().size());
        System.out.println("Total users seeded: " + auth.getAllUsers().size());
    }
}
