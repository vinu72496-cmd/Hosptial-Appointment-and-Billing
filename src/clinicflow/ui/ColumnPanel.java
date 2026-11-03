package clinicflow.ui;

import clinicflow.model.*;
import clinicflow.service.AuthService;
import clinicflow.service.PatientService;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Kanban Column component representing one workflow stage.
 * Features stage header with count badge, scrollable cards container,
 * drop target handling, and drag-and-drop validation.
 */
public class ColumnPanel extends JPanel implements DropTargetListener {

    private final Stage stage;
    private final Settings settings;
    private final PatientService patientService;
    private final AuthService authService;
    private final Runnable onDataChangedCallback;

    private JLabel countLabel;
    private JPanel cardsContainer;
    private JScrollPane scrollPane;
    private final List<CardPanel> cardPanels = new ArrayList<>();
    private boolean isDragOver = false;

    public ColumnPanel(Stage stage, Settings settings, PatientService patientService,
                       AuthService authService, Runnable onDataChangedCallback) {
        this.stage = stage;
        this.settings = settings;
        this.patientService = patientService;
        this.authService = authService;
        this.onDataChangedCallback = onDataChangedCallback;

        setLayout(new BorderLayout(0, 0));
        setOpaque(true);
        setBackground(Theme.getColumnBackground());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.getBorderColor()));

        initHeader();
        initCardsContainer();
        new DropTarget(this, DnDConstants.ACTION_MOVE, this, true, null);
    }

    public Stage getStage() {
        return stage;
    }

    private void initHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(6, 0));
        headerPanel.setOpaque(true);
        headerPanel.setBackground(Theme.getSurface());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 1, 0, Theme.getStageAccent(stage)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Left: Stage Title
        JLabel titleLabel = new JLabel(stage.getTitle().toUpperCase());
        titleLabel.setFont(Theme.FONT_HEADER);
        titleLabel.setForeground(Theme.getTextPrimary());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Right: Count Badge & Action Button
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightHeader.setOpaque(false);

        countLabel = new JLabel("0");
        countLabel.setFont(Theme.FONT_SUBHEADER);
        countLabel.setForeground(Theme.getTextSecondary());
        countLabel.setOpaque(true);
        countLabel.setBackground(Theme.getColumnBackground());
        countLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        rightHeader.add(countLabel);

        // Quick "Call Next" button in Waiting header
        if (stage == Stage.WAITING) {
            JButton callNextBtn = new JButton("Call Next");
            callNextBtn.setFont(Theme.FONT_SMALL_BOLD);
            callNextBtn.setBackground(new Color(16, 185, 129));
            callNextBtn.setForeground(Color.WHITE);
            callNextBtn.setFocusPainted(false);
            callNextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            callNextBtn.setMargin(new Insets(2, 6, 2, 6));
            callNextBtn.addActionListener(e -> {
                String error = patientService.callNext(null, authService.getCurrentUser());
                if (error != null) {
                    JOptionPane.showMessageDialog(this, error, "Call Next", JOptionPane.INFORMATION_MESSAGE);
                } else if (onDataChangedCallback != null) {
                    onDataChangedCallback.run();
                }
            });
            rightHeader.add(callNextBtn);
        }

        headerPanel.add(rightHeader, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
    }

    private void initCardsContainer() {
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setOpaque(true);
        cardsContainer.setBackground(Theme.getColumnBackground());
        cardsContainer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.getColumnBackground());

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Refreshes the patient cards in this column.
     */
    public void setPatients(List<Patient> patients, String nextWaitingToken) {
        cardsContainer.removeAll();
        cardPanels.clear();

        int totalCount = patientService.getCountForStage(stage);
        if (patients.size() == totalCount) {
            countLabel.setText(String.valueOf(patients.size()));
        } else {
            countLabel.setText(patients.size() + " / " + totalCount);
        }

        if (patients.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setOpaque(false);
            emptyPanel.setBorder(BorderFactory.createEmptyBorder(30, 10, 30, 10));

            JLabel emptyLabel = new JLabel("No patients in " + stage.getTitle());
            emptyLabel.setFont(Theme.FONT_REGULAR);
            emptyLabel.setForeground(Theme.getTextMuted());
            emptyPanel.add(emptyLabel);

            cardsContainer.add(emptyPanel);
        } else {
            for (Patient p : patients) {
                boolean isNext = (stage == Stage.WAITING) && p.getToken().equals(nextWaitingToken);
                CardPanel card = new CardPanel(p, isNext, settings, patientService, authService, onDataChangedCallback);
                cardPanels.add(card);

                cardsContainer.add(card);
                cardsContainer.add(Box.createVerticalStrut(8));
            }
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    /**
     * Updates timer labels on all active card panels.
     */
    public void updateTimers() {
        for (CardPanel cp : cardPanels) {
            cp.updateTimerDisplay();
        }
    }

    // -------------------------------------------------------------
    // DropTargetListener Implementation (FR-6, FR-8)
    // -------------------------------------------------------------

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        isDragOver = true;
        setBackground(new Color(219, 234, 254)); // Soft light blue hover
        repaint();
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // Continue tracking hover
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        isDragOver = false;
        setBackground(Theme.getColumnBackground());
        repaint();
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        isDragOver = false;
        setBackground(Theme.getColumnBackground());
        repaint();

        try {
            if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                String idStr = (String) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int patientId = Integer.parseInt(idStr);

                String error = patientService.movePatient(patientId, this.stage, authService.getCurrentUser());
                if (error != null) {
                    JOptionPane.showMessageDialog(this, error, "Move Blocked", JOptionPane.WARNING_MESSAGE);
                } else if (onDataChangedCallback != null) {
                    onDataChangedCallback.run();
                }
                dtde.dropComplete(true);
            } else {
                dtde.rejectDrop();
            }
        } catch (Exception e) {
            dtde.rejectDrop();
        }
    }
}
