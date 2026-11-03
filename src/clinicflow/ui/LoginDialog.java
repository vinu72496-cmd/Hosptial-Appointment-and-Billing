package clinicflow.ui;

import clinicflow.model.User;
import clinicflow.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Authentication and quick user switching dialog supporting SHA-256 hashed credentials.
 */
public class LoginDialog extends JDialog {

    private final AuthService authService;
    private final Runnable onLoginSuccess;
    private boolean authenticated = false;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;

    public LoginDialog(Window owner, AuthService authService, Runnable onLoginSuccess) {
        super(owner, "Staff Authentication & User Switching", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;

        setSize(420, 360);
        setLocationRelativeTo(owner);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Theme.getSurface());
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("Clinic Staff Login");
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Theme.getTextPrimary());

        JLabel subLbl = new JLabel("Authenticate or switch account role");
        subLbl.setFont(Theme.FONT_SMALL);
        subLbl.setForeground(Theme.getTextSecondary());

        headerPanel.add(titleLbl);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(subLbl);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Quick Switcher Combo
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel quickLbl = new JLabel("Quick Account Select:");
        quickLbl.setFont(Theme.FONT_SMALL_BOLD);
        quickLbl.setForeground(Theme.getTextSecondary());
        formPanel.add(quickLbl, gbc);

        gbc.gridx = 1;
        List<User> users = authService.getAllUsers();
        JComboBox<User> userCombo = new JComboBox<>(users.toArray(new User[0]));
        userCombo.setFont(Theme.FONT_REGULAR);
        userCombo.addActionListener(e -> {
            User selected = (User) userCombo.getSelectedItem();
            if (selected != null) {
                usernameField.setText(selected.getUsername());
                // Pre-fill demo password for easy testing
                if (selected.getUsername().equals("admin")) passwordField.setText("admin123");
                else if (selected.getUsername().equals("reception")) passwordField.setText("rec123");
                else passwordField.setText("doc123");
            }
        });
        formPanel.add(userCombo, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(Theme.FONT_REGULAR_BOLD);
        userLbl.setForeground(Theme.getTextPrimary());
        formPanel.add(userLbl, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        usernameField.setFont(Theme.FONT_REGULAR);
        if (authService.getCurrentUser() != null) {
            usernameField.setText(authService.getCurrentUser().getUsername());
        }
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(Theme.FONT_REGULAR_BOLD);
        passLbl.setForeground(Theme.getTextPrimary());
        formPanel.add(passLbl, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        passwordField.setFont(Theme.FONT_REGULAR);
        if (usernameField.getText().equals("admin")) {
            passwordField.setText("admin123");
        }
        passwordField.addActionListener(e -> performLogin());
        formPanel.add(passwordField, gbc);

        // Error message
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(Theme.FONT_SMALL_BOLD);
        errorLabel.setForeground(Theme.getDanger());
        formPanel.add(errorLabel, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(Theme.FONT_REGULAR);
        cancelBtn.addActionListener(e -> dispose());
        buttonRow.add(cancelBtn);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(Theme.FONT_REGULAR_BOLD);
        loginBtn.setBackground(Theme.getPrimary());
        loginBtn.setForeground(Color.WHITE);
        loginBtn.addActionListener(e -> performLogin());
        buttonRow.add(loginBtn);

        root.add(buttonRow, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void performLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());

        User user = authService.login(u, p);
        if (user != null) {
            authenticated = true;
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
            dispose();
        } else {
            errorLabel.setText("⚠ Invalid username or password.");
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
