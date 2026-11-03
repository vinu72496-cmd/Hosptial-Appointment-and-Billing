package clinicflow.ui;

import clinicflow.model.Priority;
import clinicflow.model.Stage;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Modern design system and color palette supporting Light and Dark modes
 * with high-contrast accessibility and clean typography.
 */
public final class Theme {

    private static boolean dark = false;

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_REGULAR_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_TOKEN = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_DISPLAY_TOKEN = new Font("Segoe UI", Font.BOLD, 36);
    public static final Font FONT_DISPLAY_TITLE = new Font("Segoe UI", Font.BOLD, 28);

    private Theme() {}

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean darkMode) {
        dark = darkMode;
    }

    // Canvas & Containers
    public static Color getBackground() {
        return dark ? new Color(26, 32, 44) : new Color(245, 247, 250);
    }

    public static Color getSurface() {
        return dark ? new Color(38, 47, 63) : new Color(255, 255, 255);
    }

    public static Color getColumnBackground() {
        return dark ? new Color(30, 39, 53) : new Color(238, 242, 246);
    }

    public static Color getCardBackground() {
        return dark ? new Color(45, 55, 72) : new Color(255, 255, 255);
    }

    public static Color getCardHoverBackground() {
        return dark ? new Color(54, 65, 84) : new Color(250, 252, 255);
    }

    // Typography
    public static Color getTextPrimary() {
        return dark ? new Color(240, 243, 246) : new Color(30, 41, 59);
    }

    public static Color getTextSecondary() {
        return dark ? new Color(160, 174, 192) : new Color(100, 116, 139);
    }

    public static Color getTextMuted() {
        return dark ? new Color(113, 128, 150) : new Color(148, 163, 184);
    }

    // Borders & Dividers
    public static Color getBorderColor() {
        return dark ? new Color(58, 71, 92) : new Color(226, 232, 240);
    }

    public static Color getBorderHoverColor() {
        return dark ? new Color(90, 110, 140) : new Color(180, 195, 215);
    }

    // Brand & Status Colors
    public static Color getPrimary() {
        return new Color(37, 99, 235); // Blue 600
    }

    public static Color getPrimaryDark() {
        return new Color(29, 78, 216);
    }

    public static Color getSuccess() {
        return new Color(22, 163, 74); // Green 600
    }

    public static Color getWarning() {
        return new Color(217, 119, 6); // Amber 600
    }

    public static Color getDanger() {
        return new Color(220, 38, 38); // Red 600
    }

    public static Color getInfo() {
        return new Color(14, 165, 233); // Sky 500
    }

    // Stage Accent Colors
    public static Color getStageAccent(Stage stage) {
        if (stage == null) return getPrimary();
        switch (stage) {
            case WAITING:
                return new Color(59, 130, 246);     // Blue
            case WITH_DOCTOR:
                return new Color(16, 185, 129);     // Emerald green
            case TESTS:
                return new Color(245, 158, 11);     // Amber / orange
            case BILLING:
                return new Color(139, 92, 246);     // Purple
            case DONE:
                return new Color(107, 114, 128);    // Slate grey
            default:
                return getPrimary();
        }
    }

    // Priority Colors
    public static Color getPriorityBackground(Priority priority) {
        if (priority == null) return new Color(241, 245, 249);
        switch (priority) {
            case EMERGENCY:
                return dark ? new Color(127, 29, 29) : new Color(254, 226, 226);
            case SENIOR_CHILD:
                return dark ? new Color(120, 53, 15) : new Color(254, 243, 199);
            case NORMAL:
            default:
                return dark ? new Color(30, 58, 138) : new Color(224, 242, 254);
        }
    }

    public static Color getPriorityForeground(Priority priority) {
        if (priority == null) return getTextPrimary();
        switch (priority) {
            case EMERGENCY:
                return dark ? new Color(254, 202, 202) : new Color(185, 28, 28);
            case SENIOR_CHILD:
                return dark ? new Color(253, 230, 138) : new Color(180, 83, 9);
            case NORMAL:
            default:
                return dark ? new Color(186, 230, 253) : new Color(3, 105, 161);
        }
    }

    // Wait Alerts
    public static Color getWaitWarningBorder() {
        return new Color(245, 158, 11); // Amber
    }

    public static Color getWaitDangerBorder() {
        return new Color(239, 68, 68); // Red
    }

    // Custom Rounded Border
    public static Border createRoundedBorder(Color color, int radius) {
        return new Border() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
                g2.dispose();
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
            }

            @Override
            public boolean isBorderOpaque() {
                return false;
            }
        };
    }

    public static Border createCompoundCardBorder(Color borderColor, int thickness, int padding) {
        Border line = BorderFactory.createLineBorder(borderColor, thickness);
        Border empty = BorderFactory.createEmptyBorder(padding, padding, padding, padding);
        return BorderFactory.createCompoundBorder(line, empty);
    }
}
