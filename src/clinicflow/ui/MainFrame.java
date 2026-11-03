package clinicflow.ui;

import clinicflow.model.*;
import clinicflow.service.AuthService;
import clinicflow.service.PatientListener;
import clinicflow.service.PatientService;
import clinicflow.service.ReportService;
import clinicflow.storage.DataStore;
import clinicflow.util.TimeUtil;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Primary application window for Clinic Patient Flow Board.
 * Coordinates top navigation bar, quick search & filters, 5-column Kanban board,
 * live timer ticking on EDT, audio threshold alerts, and real-time status bar.
 */
public class MainFrame extends JFrame implements PatientListener {

    private final PatientService patientService;
    private final AuthService authService;
    private final ReportService reportService;
    private final DataStore dataStore;
    private final Settings settings;

    // UI Components
    private JLabel clinicNameLabel;
    private JLabel liveClockLabel;
    private JLabel userRoleLabel;
    private JTextField searchField;
    private JComboBox<String> doctorFilterCombo;
    private JComboBox<String> priorityFilterCombo;
    private JComboBox<String> stageFilterCombo;
    private JButton addPatientBtn;
    private JButton callNextBtn;
    private BoardPanel boardPanel;
    private JLabel statusBarSummaryLabel;
    private JLabel statusBarNextLabel;
    private DisplayFrame displayFrame;

    private Timer uiTimer;
    private final Set<Integer> alertedWarningPatients = new HashSet<>();
    private final Set<Integer> alertedDangerPatients = new HashSet<>();

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("hh:mm:ss a");

    public MainFrame(PatientService patientService, AuthService authService,
                     ReportService reportService, DataStore dataStore, Settings settings) {
        super(settings.getClinicName() + " - Patient Flow Kanban Board");
        this.patientService = patientService;
        this.authService = authService;
        this.reportService = reportService;
        this.dataStore = dataStore;
        this.settings = settings;

        Theme.setDark(settings.isDarkMode());

        setSize(1320, 800);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        patientService.addPatientListener(this);
        refreshAll();

        startLiveTimer();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.getBackground());

        // 1. Top Navigation & App Bar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        topContainer.add(createHeaderPanel());
        topContainer.add(createToolbarPanel());

        root.add(topContainer, BorderLayout.NORTH);

        // 2. Center: 5 Columns Kanban Board
        boardPanel = new BoardPanel(patientService, authService, settings, this::refreshAll);
        root.add(boardPanel, BorderLayout.CENTER);

        // 3. Bottom: Status Bar
        root.add(createStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // -------------------------------------------------------------
    // Top Header & App Bar
    // -------------------------------------------------------------

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setOpaque(true);
        header.setBackground(Theme.getSurface());
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.getBorderColor()),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left: Clinic Name & Subtitle
        JPanel brandBox = new JPanel();
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));
        brandBox.setOpaque(false);

        clinicNameLabel = new JLabel("🏥 " + settings.getClinicName());
        clinicNameLabel.setFont(Theme.FONT_TITLE);
        clinicNameLabel.setForeground(Theme.getTextPrimary());

        JLabel subLabel = new JLabel("Patient Flow Board • Real-time Waiting Room Kanban");
        subLabel.setFont(Theme.FONT_SMALL);
        subLabel.setForeground(Theme.getTextSecondary());

        brandBox.add(clinicNameLabel);
        brandBox.add(Box.createVerticalStrut(2));
        brandBox.add(subLabel);
        header.add(brandBox, BorderLayout.WEST);

        // Center: Live Digital Clock
        liveClockLabel = new JLabel(LocalDateTime.now().format(CLOCK_FORMAT));
        liveClockLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        liveClockLabel.setForeground(Theme.getPrimary());
        liveClockLabel.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(liveClockLabel, BorderLayout.CENTER);

        // Right: User Profile & Utility Buttons
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        // Current User badge
        User user = authService.getCurrentUser();
        String roleText = (user != null) ? user.getDisplayName() + " [" + user.getRole().getDisplayName() + "]" : "Guest";
        userRoleLabel = new JLabel("👤 " + roleText);
        userRoleLabel.setFont(Theme.FONT_REGULAR_BOLD);
        userRoleLabel.setForeground(Theme.getTextPrimary());
        userRoleLabel.setOpaque(true);
        userRoleLabel.setBackground(Theme.getColumnBackground());
        userRoleLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        rightActions.add(userRoleLabel);

        // Switch User Button
        JButton switchUserBtn = new JButton("Switch User");
        switchUserBtn.setFont(Theme.FONT_SMALL);
        switchUserBtn.addActionListener(e -> showLoginDialog());
        rightActions.add(switchUserBtn);

        // Waiting Room Display Button
        JButton displayBtn = new JButton("📺 Display Screen");
        displayBtn.setFont(Theme.FONT_REGULAR);
        displayBtn.setBackground(new Color(15, 23, 42));
        displayBtn.setForeground(Color.WHITE);
        displayBtn.addActionListener(e -> openDisplayScreen());
        rightActions.add(displayBtn);

        // Summary & Reports Button
        JButton summaryBtn = new JButton("📊 Daily Summary");
        summaryBtn.setFont(Theme.FONT_REGULAR);
        summaryBtn.addActionListener(e -> showSummaryDialog());
        rightActions.add(summaryBtn);

        // Settings Button
        JButton settingsBtn = new JButton("⚙ Settings");
        settingsBtn.setFont(Theme.FONT_REGULAR);
        settingsBtn.addActionListener(e -> showSettingsDialog());
        rightActions.add(settingsBtn);

        header.add(rightActions, BorderLayout.EAST);
        return header;
    }

    // -------------------------------------------------------------
    // Toolbar: Search, Filters & Primary Actions
    // -------------------------------------------------------------

    private JPanel createToolbarPanel() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(true);
        toolbar.setBackground(Theme.getSurface());
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.getBorderColor()),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        // Left Toolbar: Search & Filters
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);

        // Search Input (FR-17)
        JLabel searchIcon = new JLabel("🔍");
        filterRow.add(searchIcon);

        searchField = new JTextField(14);
        searchField.setFont(Theme.FONT_REGULAR);
        searchField.setToolTipText("Search by Patient Name, Token (e.g. T-001), or Phone");
        searchField.addActionListener(e -> applyFilters());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
        filterRow.add(searchField);

        JButton clearSearchBtn = new JButton("✕");
        clearSearchBtn.setFont(Theme.FONT_SMALL);
        clearSearchBtn.setMargin(new Insets(1, 4, 1, 4));
        clearSearchBtn.addActionListener(e -> searchField.setText(""));
        filterRow.add(clearSearchBtn);

        // Doctor Filter (FR-18)
        filterRow.add(new JLabel("Doctor:"));
        doctorFilterCombo = new JComboBox<>();
        doctorFilterCombo.setFont(Theme.FONT_REGULAR);
        populateDoctorFilter();
        doctorFilterCombo.addActionListener(e -> applyFilters());
        filterRow.add(doctorFilterCombo);

        // Priority Filter (FR-18)
        filterRow.add(new JLabel("Priority:"));
        priorityFilterCombo = new JComboBox<>(new String[]{"All Priorities", "Emergency", "Senior / Child", "Normal"});
        priorityFilterCombo.setFont(Theme.FONT_REGULAR);
        priorityFilterCombo.addActionListener(e -> applyFilters());
        filterRow.add(priorityFilterCombo);

        // Stage Filter (FR-18)
        filterRow.add(new JLabel("Stage:"));
        stageFilterCombo = new JComboBox<>(new String[]{"All Stages", "Waiting", "With Doctor", "Tests", "Billing", "Done"});
        stageFilterCombo.setFont(Theme.FONT_REGULAR);
        stageFilterCombo.addActionListener(e -> applyFilters());
        filterRow.add(stageFilterCombo);

        toolbar.add(filterRow, BorderLayout.WEST);

        // Right Toolbar: Actions (Add Patient & Call Next)
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionRow.setOpaque(false);

        // Call Next Button (FR-12)
        callNextBtn = new JButton("📢 Call Next Patient");
        callNextBtn.setFont(Theme.FONT_REGULAR_BOLD);
        callNextBtn.setBackground(new Color(16, 185, 129)); // Green
        callNextBtn.setForeground(Color.WHITE);
        callNextBtn.setFocusPainted(false);
        callNextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        callNextBtn.addActionListener(e -> onCallNext());
        actionRow.add(callNextBtn);

        // Add Patient Button (FR-1)
        addPatientBtn = new JButton("+ Register Patient");
        addPatientBtn.setFont(Theme.FONT_REGULAR_BOLD);
        addPatientBtn.setBackground(Theme.getPrimary());
        addPatientBtn.setForeground(Color.WHITE);
        addPatientBtn.setFocusPainted(false);
        addPatientBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addPatientBtn.addActionListener(e -> onAddPatient());
        actionRow.add(addPatientBtn);

        toolbar.add(actionRow, BorderLayout.EAST);

        return toolbar;
    }

    private void populateDoctorFilter() {
        doctorFilterCombo.removeAllItems();
        doctorFilterCombo.addItem("All Doctors");
        for (String doc : settings.getDoctors()) {
            doctorFilterCombo.addItem(doc);
        }
    }

    // -------------------------------------------------------------
    // Bottom Status Bar
    // -------------------------------------------------------------

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout(15, 0));
        status.setOpaque(true);
        status.setBackground(Theme.getSurface());
        status.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.getBorderColor()),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        statusBarSummaryLabel = new JLabel("Today: 0 registered | 0 completed | 0 active | Avg Wait: 0 min");
        statusBarSummaryLabel.setFont(Theme.FONT_REGULAR);
        statusBarSummaryLabel.setForeground(Theme.getTextPrimary());
        status.add(statusBarSummaryLabel, BorderLayout.WEST);

        statusBarNextLabel = new JLabel("Next in line: None");
        statusBarNextLabel.setFont(Theme.FONT_REGULAR_BOLD);
        statusBarNextLabel.setForeground(Theme.getPrimary());
        status.add(statusBarNextLabel, BorderLayout.CENTER);

        JLabel syncLabel = new JLabel("● Auto-saved to disk");
        syncLabel.setFont(Theme.FONT_SMALL);
        syncLabel.setForeground(new Color(16, 185, 129));
        status.add(syncLabel, BorderLayout.EAST);

        return status;
    }

    // -------------------------------------------------------------
    // Live EDT Timer & Audio Threshold Alerts
    // -------------------------------------------------------------

    private void startLiveTimer() {
        uiTimer = new Timer(1000, e -> {
            // 1. Update clock
            liveClockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));

            // 2. Update card timers
            boardPanel.updateTimers();

            // 3. Check wait thresholds for sound alerts (FR-23)
            checkWaitThresholdAlerts();
        });
        uiTimer.start();
    }

    private void checkWaitThresholdAlerts() {
        if (!settings.isSoundAlertsEnabled()) {
            return;
        }

        List<Patient> waiting = patientService.getPatientsForStage(Stage.WAITING, null, null, null);
        for (Patient p : waiting) {
            long minutes = p.getTimeInCurrentStage().toMinutes();

            if (minutes >= settings.getWaitDangerMinutes()) {
                if (!alertedDangerPatients.contains(p.getId())) {
                    alertedDangerPatients.add(p.getId());
                    Toolkit.getDefaultToolkit().beep();
                }
            } else if (minutes >= settings.getWaitWarningMinutes()) {
                if (!alertedWarningPatients.contains(p.getId())) {
                    alertedWarningPatients.add(p.getId());
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }

    // -------------------------------------------------------------
    // User Action Handlers
    // -------------------------------------------------------------

    private void onAddPatient() {
        User user = authService.getCurrentUser();
        if (user != null && !user.getRole().canRegisterPatients()) {
            JOptionPane.showMessageDialog(this, "Your user role (" + user.getRole().getDisplayName() +
                ") does not have permission to register new patients.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PatientDialog dialog = new PatientDialog(this, settings, null);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            patientService.registerPatient(
                dialog.getNameInput(),
                dialog.getAgeInput(),
                dialog.getPhoneInput(),
                dialog.getReasonInput(),
                dialog.getNotesInput(),
                dialog.getDoctorInput(),
                dialog.getPriorityInput(),
                authService.getCurrentUser()
            );
            refreshAll();
        }
    }

    private void onCallNext() {
        User user = authService.getCurrentUser();
        String assignedDoctor = null;

        if (user != null && user.isDoctor() && user.getDoctorName() != null && !user.getDoctorName().isEmpty()) {
            assignedDoctor = user.getDoctorName();
        } else {
            String selected = (String) doctorFilterCombo.getSelectedItem();
            if (selected != null && !selected.equals("All Doctors")) {
                assignedDoctor = selected;
            }
        }

        String result = patientService.callNext(assignedDoctor, user);
        if (result != null) {
            JOptionPane.showMessageDialog(this, result, "Call Next", JOptionPane.INFORMATION_MESSAGE);
        } else {
            refreshAll();
        }
    }

    private void applyFilters() {
        String search = searchField.getText().trim();
        String doctor = (String) doctorFilterCombo.getSelectedItem();

        Priority priority = null;
        String prioStr = (String) priorityFilterCombo.getSelectedItem();
        if ("Emergency".equals(prioStr)) priority = Priority.EMERGENCY;
        else if ("Senior / Child".equals(prioStr)) priority = Priority.SENIOR_CHILD;
        else if ("Normal".equals(prioStr)) priority = Priority.NORMAL;

        Stage stageFilter = null;
        String stageStr = (String) stageFilterCombo.getSelectedItem();
        if ("Waiting".equals(stageStr)) stageFilter = Stage.WAITING;
        else if ("With Doctor".equals(stageStr)) stageFilter = Stage.WITH_DOCTOR;
        else if ("Tests".equals(stageStr)) stageFilter = Stage.TESTS;
        else if ("Billing".equals(stageStr)) stageFilter = Stage.BILLING;
        else if ("Done".equals(stageStr)) stageFilter = Stage.DONE;

        boardPanel.refreshBoard(search, doctor, priority, stageFilter);
    }

    private void refreshAll() {
        applyFilters();
        updateStatusBar();
        updateRoleDisplay();
    }

    private void updateRoleDisplay() {
        User user = authService.getCurrentUser();
        String roleText = (user != null) ? user.getDisplayName() + " [" + user.getRole().getDisplayName() + "]" : "Guest";
        userRoleLabel.setText("👤 " + roleText);

        // Adjust UI permissions
        boolean canReg = (user == null || user.getRole().canRegisterPatients());
        addPatientBtn.setEnabled(canReg);
    }

    private void updateStatusBar() {
        ReportService.DailyReport report = reportService.generateReport(java.time.LocalDate.now());

        String statusText = String.format("Today: %d registered | %d completed | %d active | %d cancelled | Avg Wait: %s",
            report.getRegisteredCount(),
            report.getCompletedCount(),
            report.getActiveCount(),
            report.getCancelledCount(),
            TimeUtil.formatHuman(report.getAverageWaitTime())
        );
        statusBarSummaryLabel.setText(statusText);

        Patient next = patientService.getNextPatientInQueue();
        if (next != null) {
            statusBarNextLabel.setText("📢 Next in queue: " + next.getToken() + " (" + next.getName() + " - " + next.getPriority().getDisplayName() + ")");
        } else {
            statusBarNextLabel.setText("Next in queue: None (Waiting room clear)");
        }
    }

    private void showLoginDialog() {
        LoginDialog dialog = new LoginDialog(this, authService, () -> {
            updateRoleDisplay();
            refreshAll();
        });
        dialog.setVisible(true);
    }

    private void openDisplayScreen() {
        if (displayFrame == null || !displayFrame.isDisplayable()) {
            displayFrame = new DisplayFrame(patientService, settings);
        }
        displayFrame.setVisible(true);
        displayFrame.toFront();
    }

    private void showSummaryDialog() {
        SummaryDialog dialog = new SummaryDialog(this, reportService);
        dialog.setVisible(true);
    }

    private void showSettingsDialog() {
        User user = authService.getCurrentUser();
        if (user != null && !user.getRole().canManageSettings()) {
            JOptionPane.showMessageDialog(this, "Only Administrators can modify clinic settings.",
                "Admin Permission Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SettingsDialog dialog = new SettingsDialog(this, settings, dataStore, () -> {
            clinicNameLabel.setText("🏥 " + settings.getClinicName());
            setTitle(settings.getClinicName() + " - Patient Flow Kanban Board");
            populateDoctorFilter();
            refreshAll();
        });
        dialog.setVisible(true);
    }

    @Override
    public void onPatientDataChanged() {
        SwingUtilities.invokeLater(this::refreshAll);
    }
}
