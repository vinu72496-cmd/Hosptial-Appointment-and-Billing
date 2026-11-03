package clinicflow.ui;

import clinicflow.model.*;
import clinicflow.service.AuthService;
import clinicflow.service.PatientService;
import clinicflow.util.TimeUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.*;
import java.time.Duration;
import java.util.List;

/**
 * Kanban Patient Card component rendering token, priority, patient details,
 * live stage timers, wait threshold alerts, drag-and-drop, and context actions.
 */
public class CardPanel extends JPanel {

    private final Patient patient;
    private final boolean isNextInQueue;
    private final Settings settings;
    private final PatientService patientService;
    private final AuthService authService;
    private final Runnable onActionCallback;

    private JLabel timerStageLabel;
    private JLabel timerTotalLabel;
    private JLabel alertBadgeLabel;
    private JPanel badgeRow;

    public CardPanel(Patient patient, boolean isNextInQueue, Settings settings,
                     PatientService patientService, AuthService authService,
                     Runnable onActionCallback) {
        this.patient = patient;
        this.isNextInQueue = isNextInQueue;
        this.settings = settings;
        this.patientService = patientService;
        this.authService = authService;
        this.onActionCallback = onActionCallback;

        setLayout(new BorderLayout(6, 6));
        setOpaque(true);
        initCardUI();
        setupDragAndDrop();
        setupContextMenu();
    }

    public Patient getPatient() {
        return patient;
    }

    private void initCardUI() {
        boolean isDark = Theme.isDark();
        boolean isDone = patient.getStage() == Stage.DONE;
        boolean isEmergency = patient.getPriority().isEmergency();

        setBackground(isDone ? (isDark ? new Color(35, 42, 54) : new Color(241, 245, 249)) : Theme.getCardBackground());

        // Header Panel: Token, "NEXT" badge, Priority badge
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        headerPanel.setOpaque(false);

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftHeader.setOpaque(false);

        JLabel tokenLabel = new JLabel(patient.getToken());
        tokenLabel.setFont(Theme.FONT_TOKEN);
        tokenLabel.setForeground(isDone ? Theme.getTextMuted() : Theme.getTextPrimary());
        leftHeader.add(tokenLabel);

        if (isNextInQueue && patient.getStage() == Stage.WAITING) {
            JLabel nextBadge = createBadge("NEXT", new Color(16, 185, 129), Color.WHITE);
            leftHeader.add(nextBadge);
        }

        headerPanel.add(leftHeader, BorderLayout.WEST);

        // Right header: Priority badge
        JLabel priorityBadge = createBadge(
            patient.getPriority().getDisplayName().toUpperCase(),
            Theme.getPriorityBackground(patient.getPriority()),
            Theme.getPriorityForeground(patient.getPriority())
        );
        headerPanel.add(priorityBadge, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Body Panel: Name, Age, Reason, Doctor, Notes
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);

        // Name & Age
        String nameAgeText = patient.getName() + " (" + patient.getAge() + " yrs)";
        JLabel nameLabel = new JLabel(nameAgeText);
        nameLabel.setFont(Theme.FONT_REGULAR_BOLD);
        nameLabel.setForeground(isDone ? Theme.getTextMuted() : Theme.getTextPrimary());
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bodyPanel.add(nameLabel);

        bodyPanel.add(Box.createVerticalStrut(3));

        // Phone if present
        if (patient.getPhone() != null && !patient.getPhone().isEmpty()) {
            JLabel phoneLabel = new JLabel("📞 " + patient.getPhone());
            phoneLabel.setFont(Theme.FONT_SMALL);
            phoneLabel.setForeground(Theme.getTextSecondary());
            phoneLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bodyPanel.add(phoneLabel);
            bodyPanel.add(Box.createVerticalStrut(2));
        }

        // Reason
        if (patient.getReason() != null && !patient.getReason().isEmpty()) {
            JLabel reasonLabel = new JLabel("Reason: " + truncate(patient.getReason(), 36));
            reasonLabel.setFont(Theme.FONT_SMALL);
            reasonLabel.setForeground(Theme.getTextSecondary());
            reasonLabel.setToolTipText("Reason: " + patient.getReason());
            reasonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bodyPanel.add(reasonLabel);
            bodyPanel.add(Box.createVerticalStrut(2));
        }

        // Doctor
        if (patient.getDoctor() != null && !patient.getDoctor().isEmpty()) {
            JLabel docLabel = new JLabel("Dr: " + truncate(patient.getDoctor(), 28));
            docLabel.setFont(Theme.FONT_SMALL_BOLD);
            docLabel.setForeground(isDark ? new Color(147, 197, 253) : new Color(30, 64, 175));
            docLabel.setToolTipText("Doctor: " + patient.getDoctor());
            docLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bodyPanel.add(docLabel);
            bodyPanel.add(Box.createVerticalStrut(2));
        }

        // Notes if present
        if (patient.getNotes() != null && !patient.getNotes().isEmpty()) {
            JLabel notesLabel = new JLabel("📝 " + truncate(patient.getNotes(), 32));
            notesLabel.setFont(Theme.FONT_SMALL);
            notesLabel.setForeground(isDark ? new Color(253, 230, 138) : new Color(161, 98, 7));
            notesLabel.setToolTipText("Notes: " + patient.getNotes());
            notesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bodyPanel.add(notesLabel);
            bodyPanel.add(Box.createVerticalStrut(2));
        }

        add(bodyPanel, BorderLayout.CENTER);

        // Footer Panel: Timers & Alert Badges & Quick Action
        JPanel footerPanel = new JPanel(new BorderLayout(4, 4));
        footerPanel.setOpaque(false);

        JPanel timerBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        timerBox.setOpaque(false);

        timerStageLabel = new JLabel("⏱ 00:00");
        timerStageLabel.setFont(Theme.FONT_REGULAR_BOLD);
        timerStageLabel.setForeground(Theme.getTextPrimary());

        timerTotalLabel = new JLabel("(Total: 0m)");
        timerTotalLabel.setFont(Theme.FONT_SMALL);
        timerTotalLabel.setForeground(Theme.getTextSecondary());

        timerBox.add(timerStageLabel);
        timerBox.add(timerTotalLabel);

        footerPanel.add(timerBox, BorderLayout.WEST);

        badgeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        badgeRow.setOpaque(false);
        alertBadgeLabel = new JLabel("");
        alertBadgeLabel.setFont(Theme.FONT_SMALL_BOLD);
        badgeRow.add(alertBadgeLabel);

        footerPanel.add(badgeRow, BorderLayout.EAST);

        // Quick Move Button Row (Bottom)
        if (!isDone) {
            JPanel actionRow = createQuickActionRow();
            if (actionRow != null) {
                JPanel bottomContainer = new JPanel(new BorderLayout());
                bottomContainer.setOpaque(false);
                bottomContainer.add(footerPanel, BorderLayout.NORTH);
                bottomContainer.add(Box.createVerticalStrut(4), BorderLayout.CENTER);
                bottomContainer.add(actionRow, BorderLayout.SOUTH);
                add(bottomContainer, BorderLayout.SOUTH);
            } else {
                add(footerPanel, BorderLayout.SOUTH);
            }
        } else {
            add(footerPanel, BorderLayout.SOUTH);
        }

        updateBorderAndAlerts();
    }

    private JPanel createQuickActionRow() {
        Role role = (authService.getCurrentUser() != null) ? authService.getCurrentUser().getRole() : Role.ADMIN;
        List<Stage> nextStages = patient.getStage().getAllowedNextStages(role);

        if (nextStages.isEmpty()) {
            return null;
        }

        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        row.setOpaque(false);

        for (Stage nextStage : nextStages) {
            JButton btn = new JButton("→ " + nextStage.getTitle());
            btn.setFont(Theme.FONT_SMALL_BOLD);
            btn.setMargin(new Insets(2, 6, 2, 6));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Custom color styling per next stage
            Color accent = Theme.getStageAccent(nextStage);
            btn.setBackground(accent);
            btn.setForeground(Color.WHITE);

            btn.addActionListener(e -> {
                String error = patientService.movePatient(patient.getId(), nextStage, authService.getCurrentUser());
                if (error != null) {
                    JOptionPane.showMessageDialog(this, error, "Move Blocked", JOptionPane.WARNING_MESSAGE);
                } else if (onActionCallback != null) {
                    onActionCallback.run();
                }
            });
            row.add(btn);
        }

        return row;
    }

    private JLabel createBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(" " + text + " ");
        badge.setFont(Theme.FONT_SMALL_BOLD);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(fg);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return badge;
    }

    /**
     * Updates live timer displays and checks wait time thresholds.
     */
    public void updateTimerDisplay() {
        Duration stageDuration = patient.getTimeInCurrentStage();
        Duration totalDuration = patient.getTotalVisitTime();

        timerStageLabel.setText("⏱ " + TimeUtil.formatTimer(stageDuration));
        timerTotalLabel.setText("(Total: " + TimeUtil.formatHuman(totalDuration) + ")");

        updateBorderAndAlerts();
    }

    private void updateBorderAndAlerts() {
        boolean isDone = patient.getStage() == Stage.DONE;
        boolean isEmergency = patient.getPriority().isEmergency();
        Duration waitDuration = patient.getTimeInCurrentStage();
        long minutesInStage = waitDuration.toMinutes();

        Color borderColor = Theme.getBorderColor();
        int thickness = 1;

        if (isDone) {
            borderColor = Theme.getBorderColor();
            alertBadgeLabel.setText("");
        } else if (isEmergency) {
            borderColor = Theme.getDanger();
            thickness = 2;
            if (patient.getStage() == Stage.WAITING && minutesInStage >= settings.getWaitWarningMinutes()) {
                alertBadgeLabel.setText("⚠ " + minutesInStage + "m");
                alertBadgeLabel.setForeground(Theme.getDanger());
            }
        } else if (patient.getStage() == Stage.WAITING) {
            if (minutesInStage >= settings.getWaitDangerMinutes()) {
                borderColor = Theme.getWaitDangerBorder();
                thickness = 2;
                alertBadgeLabel.setText("🚨 CRITICAL: " + minutesInStage + "m");
                alertBadgeLabel.setForeground(Theme.getDanger());
            } else if (minutesInStage >= settings.getWaitWarningMinutes()) {
                borderColor = Theme.getWaitWarningBorder();
                thickness = 2;
                alertBadgeLabel.setText("⚠ WAIT: " + minutesInStage + "m");
                alertBadgeLabel.setForeground(Theme.getWarning());
            } else {
                alertBadgeLabel.setText("");
            }
        } else {
            alertBadgeLabel.setText("");
        }

        setBorder(Theme.createCompoundCardBorder(borderColor, thickness, 8));
    }

    // -------------------------------------------------------------
    // Drag and Drop (FR-6)
    // -------------------------------------------------------------

    private void setupDragAndDrop() {
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, new DragGestureListener() {
            @Override
            public void dragGestureRecognized(DragGestureEvent dge) {
                if (patient.getStage() == Stage.DONE) {
                    return; // Done cards cannot be dragged
                }
                StringSelection transferable = new StringSelection(String.valueOf(patient.getId()));
                dge.startDrag(DragSource.DefaultMoveDrop, transferable);
            }
        });
    }

    // -------------------------------------------------------------
    // Context Menu (FR-7, FR-14, FR-15, FR-16)
    // -------------------------------------------------------------

    private void setupContextMenu() {
        JPopupMenu popup = new JPopupMenu();

        // Stage Transitions Submenu
        JMenu moveMenu = new JMenu("Move to Stage...");
        moveMenu.setFont(Theme.FONT_REGULAR);
        Role role = (authService.getCurrentUser() != null) ? authService.getCurrentUser().getRole() : Role.ADMIN;

        for (Stage s : Stage.values()) {
            if (s == patient.getStage()) continue;
            boolean allowed = patient.getStage().canTransitionTo(s, role);
            JMenuItem item = new JMenuItem(s.getTitle());
            item.setFont(Theme.FONT_REGULAR);
            item.setEnabled(allowed);
            if (!allowed) {
                item.setToolTipText(patient.getStage().getTransitionBlockedReason(s, role));
            }
            item.addActionListener(e -> {
                String error = patientService.movePatient(patient.getId(), s, authService.getCurrentUser());
                if (error != null) {
                    JOptionPane.showMessageDialog(this, error, "Move Blocked", JOptionPane.WARNING_MESSAGE);
                } else if (onActionCallback != null) {
                    onActionCallback.run();
                }
            });
            moveMenu.add(item);
        }
        popup.add(moveMenu);
        popup.addSeparator();

        // Edit Patient
        JMenuItem editItem = new JMenuItem("Edit Patient Details...");
        editItem.setFont(Theme.FONT_REGULAR);
        editItem.addActionListener(e -> showEditDialog());
        popup.add(editItem);

        // Add / Edit Note
        JMenuItem noteItem = new JMenuItem("Add / Edit Note...");
        noteItem.setFont(Theme.FONT_REGULAR);
        noteItem.addActionListener(e -> showNoteDialog());
        popup.add(noteItem);

        // Change Priority
        JMenu priorityMenu = new JMenu("Change Priority...");
        priorityMenu.setFont(Theme.FONT_REGULAR);
        for (Priority p : Priority.values()) {
            JMenuItem pItem = new JMenuItem(p.getDisplayName());
            pItem.setFont(Theme.FONT_REGULAR);
            pItem.addActionListener(e -> {
                patientService.editPatient(patient.getId(), patient.getName(), patient.getAge(),
                    patient.getPhone(), patient.getReason(), patient.getNotes(),
                    patient.getDoctor(), p, authService.getCurrentUser());
                if (onActionCallback != null) onActionCallback.run();
            });
            priorityMenu.add(pItem);
        }
        popup.add(priorityMenu);
        popup.addSeparator();

        // Audit History
        JMenuItem historyItem = new JMenuItem("View Visit History...");
        historyItem.setFont(Theme.FONT_REGULAR);
        historyItem.addActionListener(e -> showHistoryDialog());
        popup.add(historyItem);

        // Cancel Patient
        if (patient.getStage() != Stage.DONE && !patient.isCancelled()) {
            JMenuItem cancelItem = new JMenuItem("Cancel Patient Visit...");
            cancelItem.setFont(Theme.FONT_REGULAR);
            cancelItem.setForeground(Theme.getDanger());
            cancelItem.addActionListener(e -> showCancelDialog());
            popup.add(cancelItem);
        }

        // Attach popup to mouse listener
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            private void showMenu(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        };

        addMouseListener(mouseAdapter);
    }

    private void showEditDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        PatientDialog dialog = new PatientDialog(parent, settings, patient);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            patientService.editPatient(patient.getId(), dialog.getNameInput(), dialog.getAgeInput(),
                dialog.getPhoneInput(), dialog.getReasonInput(), dialog.getNotesInput(),
                dialog.getDoctorInput(), dialog.getPriorityInput(), authService.getCurrentUser());
            if (onActionCallback != null) onActionCallback.run();
        }
    }

    private void showNoteDialog() {
        String note = JOptionPane.showInputDialog(this,
            "Enter or update clinical note for " + patient.getName() + " (" + patient.getToken() + "):",
            patient.getNotes());
        if (note != null) {
            patientService.updateNotes(patient.getId(), note.trim());
            if (onActionCallback != null) onActionCallback.run();
        }
    }

    private void showCancelDialog() {
        String reason = JOptionPane.showInputDialog(this,
            "Are you sure you want to cancel visit for " + patient.getName() + " (" + patient.getToken() + ")?\nReason for cancellation:",
            "Patient left without consultation");
        if (reason != null && !reason.trim().isEmpty()) {
            patientService.cancelPatient(patient.getId(), reason.trim(), authService.getCurrentUser());
            if (onActionCallback != null) onActionCallback.run();
        }
    }

    private void showHistoryDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("Visit History for ").append(patient.getName()).append(" (").append(patient.getToken()).append(")\n");
        sb.append("Registered: ").append(TimeUtil.formatDateTime(patient.getRegisteredAt())).append("\n\n");
        sb.append("Stage Transitions:\n");

        for (StageRecord rec : patient.getHistory()) {
            sb.append(" • ").append(rec.getStage().getTitle()).append("\n");
            sb.append("    Entered: ").append(TimeUtil.formatDateTime(rec.getEnteredAt())).append("\n");
            if (rec.getExitedAt() != null) {
                sb.append("    Exited:  ").append(TimeUtil.formatDateTime(rec.getExitedAt())).append("\n");
            } else {
                sb.append("    Status:  CURRENT STAGE\n");
            }
            sb.append("    Duration: ").append(TimeUtil.formatHuman(rec.getDuration())).append("\n\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(420, 300));
        JOptionPane.showMessageDialog(this, scroll, "Patient Visit History", JOptionPane.INFORMATION_MESSAGE);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }
}
