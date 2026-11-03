package clinicflow.ui;

import clinicflow.model.*;
import clinicflow.service.AuthService;
import clinicflow.service.PatientService;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Kanban Board container rendering the 5 workflow stage columns side by side.
 */
public class BoardPanel extends JPanel {

    private final PatientService patientService;
    private final AuthService authService;
    private final Settings settings;
    private final Runnable onDataChangedCallback;

    private final Map<Stage, ColumnPanel> columnMap = new EnumMap<>(Stage.class);

    // Current filter states
    private String searchQuery = "";
    private String doctorFilter = "All Doctors";
    private Priority priorityFilter = null;
    private Stage stageFilter = null;

    public BoardPanel(PatientService patientService, AuthService authService,
                      Settings settings, Runnable onDataChangedCallback) {
        this.patientService = patientService;
        this.authService = authService;
        this.settings = settings;
        this.onDataChangedCallback = onDataChangedCallback;

        setLayout(new GridLayout(1, 5, 0, 0));
        setOpaque(true);
        setBackground(Theme.getBackground());

        initColumns();
    }

    private void initColumns() {
        for (Stage stage : Stage.values()) {
            ColumnPanel col = new ColumnPanel(stage, settings, patientService, authService, onDataChangedCallback);
            columnMap.put(stage, col);
            add(col);
        }
    }

    /**
     * Applies filters and updates the cards across all 5 columns.
     */
    public void refreshBoard(String search, String doctor, Priority priority, Stage stageFilter) {
        this.searchQuery = search;
        this.doctorFilter = doctor;
        this.priorityFilter = priority;
        this.stageFilter = stageFilter;

        Patient nextPatient = patientService.getNextPatientInQueue();
        String nextToken = (nextPatient != null) ? nextPatient.getToken() : null;

        for (Stage s : Stage.values()) {
            ColumnPanel col = columnMap.get(s);
            if (col != null) {
                if (stageFilter != null && stageFilter != s) {
                    col.setVisible(false);
                } else {
                    col.setVisible(true);
                    List<Patient> patients = patientService.getPatientsForStage(s, searchQuery, doctorFilter, priorityFilter);
                    col.setPatients(patients, nextToken);
                }
            }
        }
    }

    /**
     * Updates timers on all columns without re-rendering the cards.
     */
    public void updateTimers() {
        for (ColumnPanel col : columnMap.values()) {
            if (col.isVisible()) {
                col.updateTimers();
            }
        }
    }
}
