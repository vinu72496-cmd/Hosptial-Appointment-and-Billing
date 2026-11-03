package clinicflow.ui;

import clinicflow.model.Settings;
import clinicflow.storage.DataStore;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings configuration dialog for clinic name, doctors, wait thresholds,
 * audio alerts, and theme preferences.
 */
public class SettingsDialog extends JDialog {

    private final Settings settings;
    private final DataStore dataStore;
    private final Runnable onSettingsSavedCallback;

    private JTextField clinicNameField;
    private DefaultListModel<String> doctorsListModel;
    private JList<String> doctorsList;
    private JSpinner warningSpinner;
    private JSpinner dangerSpinner;
    private JCheckBox soundAlertsCheck;
    private JCheckBox darkModeCheck;

    public SettingsDialog(Window owner, Settings settings, DataStore dataStore, Runnable onSettingsSavedCallback) {
        super(owner, "Clinic Flow Settings", ModalityType.APPLICATION_MODAL);
        this.settings = settings;
        this.dataStore = dataStore;
        this.onSettingsSavedCallback = onSettingsSavedCallback;

        setSize(520, 560);
        setLocationRelativeTo(owner);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Theme.getSurface());
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Header
        JLabel titleLabel = new JLabel("System & Flow Settings");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.getTextPrimary());
        root.add(titleLabel, BorderLayout.NORTH);

        // Center Content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Clinic Name
        JPanel namePanel = new JPanel(new BorderLayout(6, 4));
        namePanel.setOpaque(false);
        JLabel nameLbl = new JLabel("Clinic Name:");
        nameLbl.setFont(Theme.FONT_REGULAR_BOLD);
        clinicNameField = new JTextField(settings.getClinicName());
        clinicNameField.setFont(Theme.FONT_REGULAR);
        namePanel.add(nameLbl, BorderLayout.NORTH);
        namePanel.add(clinicNameField, BorderLayout.CENTER);
        contentPanel.add(namePanel);
        contentPanel.add(Box.createVerticalStrut(12));

        // Doctors List Management
        JPanel docPanel = new JPanel(new BorderLayout(6, 4));
        docPanel.setOpaque(false);
        JLabel docLbl = new JLabel("Doctors & Consultation Rooms:");
        docLbl.setFont(Theme.FONT_REGULAR_BOLD);
        docPanel.add(docLbl, BorderLayout.NORTH);

        doctorsListModel = new DefaultListModel<>();
        for (String d : settings.getDoctors()) {
            doctorsListModel.addElement(d);
        }
        doctorsList = new JList<>(doctorsListModel);
        doctorsList.setFont(Theme.FONT_REGULAR);
        JScrollPane docScroll = new JScrollPane(doctorsList);
        docScroll.setPreferredSize(new Dimension(300, 110));
        docPanel.add(docScroll, BorderLayout.CENTER);

        JPanel docBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        docBtnRow.setOpaque(false);

        JButton addDocBtn = new JButton("+ Add Doctor");
        addDocBtn.setFont(Theme.FONT_SMALL);
        addDocBtn.addActionListener(e -> {
            String newDoc = JOptionPane.showInputDialog(this, "Enter Doctor name & specialization (e.g. Dr. Roy (Cardiology)):");
            if (newDoc != null && !newDoc.trim().isEmpty()) {
                doctorsListModel.addElement(newDoc.trim());
            }
        });
        docBtnRow.add(addDocBtn);

        JButton remDocBtn = new JButton("- Remove");
        remDocBtn.setFont(Theme.FONT_SMALL);
        remDocBtn.addActionListener(e -> {
            int idx = doctorsList.getSelectedIndex();
            if (idx >= 0) {
                doctorsListModel.remove(idx);
            }
        });
        docBtnRow.add(remDocBtn);

        docPanel.add(docBtnRow, BorderLayout.SOUTH);
        contentPanel.add(docPanel);
        contentPanel.add(Box.createVerticalStrut(12));

        // Wait Thresholds Panel
        JPanel thresholdPanel = new JPanel(new GridLayout(2, 2, 10, 8));
        thresholdPanel.setOpaque(false);
        thresholdPanel.setBorder(BorderFactory.createTitledBorder("Wait Time Alert Thresholds (Minutes)"));

        JLabel warnLbl = new JLabel("Warning Alert (Amber):");
        warnLbl.setFont(Theme.FONT_REGULAR);
        warningSpinner = new JSpinner(new SpinnerNumberModel(settings.getWaitWarningMinutes(), 5, 120, 5));

        JLabel dangerLbl = new JLabel("Critical Alert (Red):");
        dangerLbl.setFont(Theme.FONT_REGULAR);
        dangerSpinner = new JSpinner(new SpinnerNumberModel(settings.getWaitDangerMinutes(), 10, 240, 5));

        thresholdPanel.add(warnLbl);
        thresholdPanel.add(warningSpinner);
        thresholdPanel.add(dangerLbl);
        thresholdPanel.add(dangerSpinner);

        contentPanel.add(thresholdPanel);
        contentPanel.add(Box.createVerticalStrut(12));

        // Audio & Appearance Preferences
        JPanel prefPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        prefPanel.setOpaque(false);
        prefPanel.setBorder(BorderFactory.createTitledBorder("Preferences"));

        soundAlertsCheck = new JCheckBox("Enable audible alert chime when patient wait threshold is exceeded", settings.isSoundAlertsEnabled());
        soundAlertsCheck.setFont(Theme.FONT_REGULAR);
        soundAlertsCheck.setOpaque(false);

        darkModeCheck = new JCheckBox("Dark Theme Mode", settings.isDarkMode());
        darkModeCheck.setFont(Theme.FONT_REGULAR);
        darkModeCheck.setOpaque(false);

        prefPanel.add(soundAlertsCheck);
        prefPanel.add(darkModeCheck);

        contentPanel.add(prefPanel);
        root.add(contentPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(Theme.FONT_REGULAR);
        cancelBtn.addActionListener(e -> dispose());
        buttonRow.add(cancelBtn);

        JButton saveBtn = new JButton("Save Settings");
        saveBtn.setFont(Theme.FONT_REGULAR_BOLD);
        saveBtn.setBackground(Theme.getPrimary());
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> onSave());
        buttonRow.add(saveBtn);

        root.add(buttonRow, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void onSave() {
        String clinicName = clinicNameField.getText().trim();
        if (clinicName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Clinic name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> docs = new ArrayList<>();
        for (int i = 0; i < doctorsListModel.size(); i++) {
            docs.add(doctorsListModel.getElementAt(i));
        }
        if (docs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one doctor to the clinic list.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int warnMin = (Integer) warningSpinner.getValue();
        int dangerMin = (Integer) dangerSpinner.getValue();
        if (dangerMin <= warnMin) {
            JOptionPane.showMessageDialog(this, "Critical alert threshold must be greater than warning threshold.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        settings.setClinicName(clinicName);
        settings.setDoctors(docs);
        settings.setWaitWarningMinutes(warnMin);
        settings.setWaitDangerMinutes(dangerMin);
        settings.setSoundAlertsEnabled(soundAlertsCheck.isSelected());
        settings.setDarkMode(darkModeCheck.isSelected());

        Theme.setDark(settings.isDarkMode());

        try {
            dataStore.saveSettings(settings);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save settings to disk: " + e.getMessage(), "Storage Error", JOptionPane.ERROR_MESSAGE);
        }

        if (onSettingsSavedCallback != null) {
            onSettingsSavedCallback.run();
        }

        dispose();
    }
}
