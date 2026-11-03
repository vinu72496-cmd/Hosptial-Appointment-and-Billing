package clinicflow.ui;

import clinicflow.model.*;
import clinicflow.service.PatientListener;
import clinicflow.service.PatientService;
import clinicflow.util.TimeUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separate full-screen / monitor TV Waiting Room Display.
 * Displays only token numbers and stages (no personal details for patient privacy).
 * Automatically updates when patient workflow changes.
 */
public class DisplayFrame extends JFrame implements PatientListener {

    private final PatientService patientService;
    private final Settings settings;

    private JLabel clinicTitleLabel;
    private JLabel clockLabel;
    private JPanel nowServingContainer;
    private JPanel nextInLineContainer;
    private Timer clockTimer;
    private boolean isFullScreen = false;

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    public DisplayFrame(PatientService patientService, Settings settings) {
        super(settings.getClinicName() + " - Waiting Room Display");
        this.patientService = patientService;
        this.settings = settings;

        setSize(1100, 700);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initUI();
        patientService.addPatientListener(this);
        refreshDisplay();

        // Start clock timer
        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();

        // F11 full-screen hotkey
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullScreen();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && isFullScreen) {
                    toggleFullScreen();
                }
            }
        });
        setFocusable(true);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(15, 23, 42)); // Slate 900 dark background

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setBackground(new Color(30, 41, 59)); // Slate 800
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        clinicTitleLabel = new JLabel(settings.getClinicName());
        clinicTitleLabel.setFont(Theme.FONT_DISPLAY_TITLE);
        clinicTitleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("PATIENT QUEUE & CALL SYSTEM  •  PRESS F11 FOR FULLSCREEN");
        subLabel.setFont(Theme.FONT_SMALL_BOLD);
        subLabel.setForeground(new Color(148, 163, 184));

        titleBox.add(clinicTitleLabel);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subLabel);
        headerPanel.add(titleBox, BorderLayout.WEST);

        // Right Header: Live Clock & Fullscreen button
        JPanel clockBox = new JPanel();
        clockBox.setLayout(new BoxLayout(clockBox, BoxLayout.Y_AXIS));
        clockBox.setOpaque(false);

        clockLabel = new JLabel(LocalDateTime.now().format(CLOCK_FMT));
        clockLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        clockLabel.setForeground(new Color(56, 189, 248)); // Sky 400
        clockLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel dateLabel = new JLabel(LocalDateTime.now().format(DATE_FMT));
        dateLabel.setFont(Theme.FONT_REGULAR);
        dateLabel.setForeground(new Color(203, 213, 225));
        dateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        clockBox.add(clockLabel);
        clockBox.add(dateLabel);
        headerPanel.add(clockBox, BorderLayout.EAST);

        root.add(headerPanel, BorderLayout.NORTH);

        // Center: Two Main Columns (NOW SERVING & NEXT IN LINE)
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Column 1: NOW SERVING
        JPanel servingPanel = createSectionPanel("NOW SERVING", new Color(16, 185, 129));
        nowServingContainer = new JPanel();
        nowServingContainer.setLayout(new BoxLayout(nowServingContainer, BoxLayout.Y_AXIS));
        nowServingContainer.setOpaque(false);

        JScrollPane servingScroll = new JScrollPane(nowServingContainer);
        servingScroll.setOpaque(false);
        servingScroll.getViewport().setOpaque(false);
        servingScroll.setBorder(BorderFactory.createEmptyBorder());
        servingPanel.add(servingScroll, BorderLayout.CENTER);
        contentPanel.add(servingPanel);

        // Column 2: NEXT IN LINE
        JPanel nextPanel = createSectionPanel("NEXT IN LINE (WAITING ROOM)", new Color(59, 130, 246));
        nextInLineContainer = new JPanel();
        nextInLineContainer.setLayout(new BoxLayout(nextInLineContainer, BoxLayout.Y_AXIS));
        nextInLineContainer.setOpaque(false);

        JScrollPane nextScroll = new JScrollPane(nextInLineContainer);
        nextScroll.setOpaque(false);
        nextScroll.getViewport().setOpaque(false);
        nextScroll.setBorder(BorderFactory.createEmptyBorder());
        nextPanel.add(nextScroll, BorderLayout.CENTER);
        contentPanel.add(nextPanel);

        root.add(contentPanel, BorderLayout.CENTER);

        // Footer banner
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(30, 41, 59));
        JLabel footLabel = new JLabel("Please proceed to the designated consultation room or desk when your token is called.");
        footLabel.setFont(Theme.FONT_REGULAR);
        footLabel.setForeground(new Color(203, 213, 225));
        footer.add(footLabel);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createSectionPanel(String title, Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(true);
        panel.setBackground(new Color(30, 41, 59));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(4, 0, 0, 0, accentColor),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Color.WHITE);
        panel.add(titleLbl, BorderLayout.NORTH);

        return panel;
    }

    private void updateClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT));
    }

    public void toggleFullScreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        dispose();
        if (!isFullScreen) {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
            isFullScreen = true;
        } else {
            setUndecorated(false);
            gd.setFullScreenWindow(null);
            setSize(1100, 700);
            setLocationRelativeTo(null);
            isFullScreen = false;
        }
        setVisible(true);
        requestFocus();
    }

    /**
     * Refreshes the display tokens in real time.
     */
    public void refreshDisplay() {
        SwingUtilities.invokeLater(() -> {
            clinicTitleLabel.setText(settings.getClinicName());

            // 1. Populate NOW SERVING (With Doctor, Tests, Billing)
            nowServingContainer.removeAll();
            List<Patient> withDoctor = patientService.getPatientsForStage(Stage.WITH_DOCTOR, null, null, null);
            List<Patient> atTests = patientService.getPatientsForStage(Stage.TESTS, null, null, null);
            List<Patient> atBilling = patientService.getPatientsForStage(Stage.BILLING, null, null, null);

            boolean hasServing = false;

            for (Patient p : withDoctor) {
                hasServing = true;
                nowServingContainer.add(createServingCard(p.getToken(), "CONSULTATION", p.getDoctor(), new Color(16, 185, 129)));
                nowServingContainer.add(Box.createVerticalStrut(10));
            }

            for (Patient p : atTests) {
                hasServing = true;
                nowServingContainer.add(createServingCard(p.getToken(), "DIAGNOSTIC TESTS / LAB", "Pathology / Radiology", new Color(245, 158, 11)));
                nowServingContainer.add(Box.createVerticalStrut(10));
            }

            for (Patient p : atBilling) {
                hasServing = true;
                nowServingContainer.add(createServingCard(p.getToken(), "BILLING & CHECKOUT", "Cashier Counter", new Color(139, 92, 246)));
                nowServingContainer.add(Box.createVerticalStrut(10));
            }

            if (!hasServing) {
                JLabel empty = new JLabel("No active consultations at this moment.");
                empty.setFont(Theme.FONT_SUBHEADER);
                empty.setForeground(new Color(148, 163, 184));
                empty.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
                nowServingContainer.add(empty);
            }

            // 2. Populate NEXT IN LINE (Top 5 from Waiting queue)
            nextInLineContainer.removeAll();
            List<Patient> waiting = patientService.getPatientsForStage(Stage.WAITING, null, null, null);

            if (waiting.isEmpty()) {
                JLabel empty = new JLabel("Waiting room queue is currently empty.");
                empty.setFont(Theme.FONT_SUBHEADER);
                empty.setForeground(new Color(148, 163, 184));
                empty.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
                nextInLineContainer.add(empty);
            } else {
                int displayLimit = Math.min(5, waiting.size());
                for (int i = 0; i < displayLimit; i++) {
                    Patient p = waiting.get(i);
                    boolean isImmediateNext = (i == 0);
                    nextInLineContainer.add(createNextCard(p.getToken(), i + 1, isImmediateNext, p.getPriority().isEmergency()));
                    nextInLineContainer.add(Box.createVerticalStrut(10));
                }
            }

            nowServingContainer.revalidate();
            nowServingContainer.repaint();
            nextInLineContainer.revalidate();
            nextInLineContainer.repaint();
        });
    }

    private JPanel createServingCard(String token, String stageTitle, String location, Color accent) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setOpaque(true);
        card.setBackground(new Color(51, 65, 85)); // Slate 700
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 6, 0, 0, accent),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Token Number (Huge)
        JLabel tokenLabel = new JLabel(token);
        tokenLabel.setFont(Theme.FONT_DISPLAY_TOKEN);
        tokenLabel.setForeground(Color.WHITE);
        card.add(tokenLabel, BorderLayout.WEST);

        // Stage & Room Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel stageLabel = new JLabel(stageTitle);
        stageLabel.setFont(Theme.FONT_SUBHEADER);
        stageLabel.setForeground(accent);

        JLabel locLabel = new JLabel(location != null && !location.isEmpty() ? location : "Main Clinic");
        locLabel.setFont(Theme.FONT_REGULAR);
        locLabel.setForeground(new Color(226, 232, 240));

        infoPanel.add(stageLabel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(locLabel);

        card.add(infoPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createNextCard(String token, int position, boolean isImmediateNext, boolean isEmergency) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setOpaque(true);
        card.setBackground(new Color(51, 65, 85)); // Slate 700

        Color accent = isEmergency ? new Color(239, 68, 68) : (isImmediateNext ? new Color(16, 185, 129) : new Color(59, 130, 246));

        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 6, 0, 0, accent),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left: Position
        JLabel posLabel = new JLabel("#" + position);
        posLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        posLabel.setForeground(new Color(148, 163, 184));
        card.add(posLabel, BorderLayout.WEST);

        // Center: Token
        JLabel tokenLabel = new JLabel(token);
        tokenLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        tokenLabel.setForeground(Color.WHITE);
        card.add(tokenLabel, BorderLayout.CENTER);

        // Right: Status Badge
        JPanel badgeBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeBox.setOpaque(false);

        if (isEmergency) {
            JLabel badge = new JLabel(" EMERGENCY ");
            badge.setFont(Theme.FONT_REGULAR_BOLD);
            badge.setOpaque(true);
            badge.setBackground(new Color(220, 38, 38));
            badge.setForeground(Color.WHITE);
            badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            badgeBox.add(badge);
        } else if (isImmediateNext) {
            JLabel badge = new JLabel(" PREPARE TO ENTER ");
            badge.setFont(Theme.FONT_REGULAR_BOLD);
            badge.setOpaque(true);
            badge.setBackground(new Color(16, 185, 129));
            badge.setForeground(Color.WHITE);
            badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            badgeBox.add(badge);
        }

        card.add(badgeBox, BorderLayout.EAST);
        return card;
    }

    @Override
    public void onPatientDataChanged() {
        refreshDisplay();
    }

    @Override
    public void dispose() {
        patientService.removePatientListener(this);
        if (clockTimer != null) {
            clockTimer.stop();
        }
        super.dispose();
    }
}
