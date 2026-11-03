package clinicflow.ui;

import clinicflow.model.*;
import clinicflow.util.Validator;

import javax.swing.*;
import java.awt.*;

/**
 * Modal dialog for patient registration and editing with input validation.
 */
public class PatientDialog extends JDialog {

    private final Settings settings;
    private final Patient existingPatient;
    private boolean saved = false;

    private JTextField nameField;
    private JTextField ageField;
    private JTextField phoneField;
    private JComboBox<Priority> priorityCombo;
    private JComboBox<String> doctorCombo;
    private JTextField reasonField;
    private JTextArea notesArea;
    private JLabel errorLabel;

    public PatientDialog(Window owner, Settings settings, Patient existingPatient) {
        super(owner, existingPatient == null ? "Register New Patient" : "Edit Patient Details", ModalityType.APPLICATION_MODAL);
        this.settings = settings;
        this.existingPatient = existingPatient;

        setSize(480, 520);
        setLocationRelativeTo(owner);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel contentPanel = new JPanel(new BorderLayout(12, 12));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        contentPanel.setBackground(Theme.getSurface());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(existingPatient == null ? "New Patient Registration" : "Edit Patient: " + existingPatient.getToken());
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.getTextPrimary());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        contentPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Fields (GridBagLayout)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Name
        formPanel.add(createFieldLabel("Full Name * :"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        nameField.setFont(Theme.FONT_REGULAR);
        if (existingPatient != null) nameField.setText(existingPatient.getName());
        formPanel.add(nameField, gbc);

        // Age
        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(createFieldLabel("Age (0-120) * :"), gbc);
        gbc.gridx = 1;
        ageField = new JTextField(10);
        ageField.setFont(Theme.FONT_REGULAR);
        if (existingPatient != null) ageField.setText(String.valueOf(existingPatient.getAge()));
        formPanel.add(ageField, gbc);

        // Phone
        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(createFieldLabel("Phone Number (10 digits) :"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(Theme.FONT_REGULAR);
        if (existingPatient != null) phoneField.setText(existingPatient.getPhone());
        formPanel.add(phoneField, gbc);

        // Priority
        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(createFieldLabel("Priority Level :"), gbc);
        gbc.gridx = 1;
        priorityCombo = new JComboBox<>(Priority.values());
        priorityCombo.setFont(Theme.FONT_REGULAR);
        if (existingPatient != null) {
            priorityCombo.setSelectedItem(existingPatient.getPriority());
        } else {
            priorityCombo.setSelectedItem(Priority.NORMAL);
        }
        formPanel.add(priorityCombo, gbc);

        // Assigned Doctor
        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(createFieldLabel("Assigned Doctor :"), gbc);
        gbc.gridx = 1;
        doctorCombo = new JComboBox<>(settings.getDoctors().toArray(new String[0]));
        doctorCombo.setFont(Theme.FONT_REGULAR);
        if (existingPatient != null && existingPatient.getDoctor() != null) {
            doctorCombo.setSelectedItem(existingPatient.getDoctor());
        }
        formPanel.add(doctorCombo, gbc);

        // Reason for Visit
        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(createFieldLabel("Reason for Visit :"), gbc);
        gbc.gridx = 1;
        reasonField = new JTextField(20);
        reasonField.setFont(Theme.FONT_REGULAR);
        if (existingPatient != null) reasonField.setText(existingPatient.getReason());
        formPanel.add(reasonField, gbc);

        // Clinical Notes
        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(createFieldLabel("Clinical Notes :"), gbc);
        gbc.gridx = 1;
        notesArea = new JTextArea(3, 20);
        notesArea.setFont(Theme.FONT_REGULAR);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        if (existingPatient != null) notesArea.setText(existingPatient.getNotes());
        JScrollPane notesScroll = new JScrollPane(notesArea);
        formPanel.add(notesScroll, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        // Footer: Error Message + Buttons
        JPanel footerPanel = new JPanel(new BorderLayout(6, 6));
        footerPanel.setOpaque(false);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.FONT_SMALL_BOLD);
        errorLabel.setForeground(Theme.getDanger());
        footerPanel.add(errorLabel, BorderLayout.NORTH);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(Theme.FONT_REGULAR);
        cancelBtn.addActionListener(e -> dispose());
        buttonRow.add(cancelBtn);

        JButton saveBtn = new JButton(existingPatient == null ? "Register Patient" : "Save Changes");
        saveBtn.setFont(Theme.FONT_REGULAR_BOLD);
        saveBtn.setBackground(Theme.getPrimary());
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> onSave());
        buttonRow.add(saveBtn);

        footerPanel.add(buttonRow, BorderLayout.SOUTH);
        contentPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_REGULAR_BOLD);
        lbl.setForeground(Theme.getTextPrimary());
        return lbl;
    }

    private void onSave() {
        String name = nameField.getText().trim();
        String ageStr = ageField.getText().trim();
        String phone = phoneField.getText().trim();

        // Validate name
        Validator.Result nameRes = Validator.validateName(name);
        if (!nameRes.isValid()) {
            errorLabel.setText("⚠ " + nameRes.getMessage());
            nameField.requestFocus();
            return;
        }

        // Validate age
        Validator.Result ageRes = Validator.validateAge(ageStr);
        if (!ageRes.isValid()) {
            errorLabel.setText("⚠ " + ageRes.getMessage());
            ageField.requestFocus();
            return;
        }

        // Validate phone
        Validator.Result phoneRes = Validator.validatePhone(phone);
        if (!phoneRes.isValid()) {
            errorLabel.setText("⚠ " + phoneRes.getMessage());
            phoneField.requestFocus();
            return;
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    public String getNameInput() {
        return nameField.getText().trim();
    }

    public int getAgeInput() {
        try {
            return Integer.parseInt(ageField.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getPhoneInput() {
        return phoneField.getText().trim();
    }

    public Priority getPriorityInput() {
        return (Priority) priorityCombo.getSelectedItem();
    }

    public String getDoctorInput() {
        Object item = doctorCombo.getSelectedItem();
        return item != null ? item.toString() : "";
    }

    public String getReasonInput() {
        return reasonField.getText().trim();
    }

    public String getNotesInput() {
        return notesArea.getText().trim();
    }
}
