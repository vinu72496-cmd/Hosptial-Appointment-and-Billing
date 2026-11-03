package clinicflow.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure Graphics2D custom bar chart rendering hourly patient arrivals
 * without any external chart libraries.
 */
public class HourlyBarChartPanel extends JPanel {

    private Map<Integer, Integer> hourlyData;
    private int startHour = 8;
    private int endHour = 20;

    public HourlyBarChartPanel(Map<Integer, Integer> data) {
        this.hourlyData = (data != null) ? new TreeMap<>(data) : new TreeMap<>();
        setPreferredSize(new Dimension(550, 260));
        setOpaque(true);
    }

    public void setData(Map<Integer, Integer> data) {
        this.hourlyData = (data != null) ? new TreeMap<>(data) : new TreeMap<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(Theme.getSurface());
        g2.fillRect(0, 0, width, height);

        // Calculate max value for scaling
        int maxVal = 0;
        int totalVal = 0;
        int activeHourCount = 0;
        int peakHour = -1;

        for (int h = startHour; h <= endHour; h++) {
            int count = hourlyData.getOrDefault(h, 0);
            if (count > maxVal) {
                maxVal = count;
                peakHour = h;
            }
            totalVal += count;
            activeHourCount++;
        }

        // Ensure minimum scale of 5 for nice grid
        int yMax = Math.max(5, ((maxVal + 2) / 2) * 2);
        double avgVal = activeHourCount > 0 ? (double) totalVal / activeHourCount : 0.0;

        // Chart area margins
        int marginLeft = 45;
        int marginRight = 25;
        int marginTop = 35;
        int marginBottom = 40;

        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;

        if (chartWidth <= 0 || chartHeight <= 0) {
            g2.dispose();
            return;
        }

        // Title
        g2.setFont(Theme.FONT_SUBHEADER);
        g2.setColor(Theme.getTextPrimary());
        g2.drawString("Patient Registrations by Hour", marginLeft, 22);

        // Horizontal grid lines and Y-axis labels
        g2.setFont(Theme.FONT_SMALL);
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            int yVal = (yMax * i) / gridLines;
            int yPos = marginTop + chartHeight - (int) ((double) yVal / yMax * chartHeight);

            g2.setColor(Theme.getBorderColor());
            g2.drawLine(marginLeft, yPos, marginLeft + chartWidth, yPos);

            g2.setColor(Theme.getTextSecondary());
            String lbl = String.valueOf(yVal);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, marginLeft - fm.stringWidth(lbl) - 8, yPos + 4);
        }

        // Draw Bars
        int barSlots = (endHour - startHour + 1);
        double slotWidth = (double) chartWidth / barSlots;
        double barWidth = Math.max(12, slotWidth * 0.65);

        for (int h = startHour; h <= endHour; h++) {
            int count = hourlyData.getOrDefault(h, 0);
            int slotIndex = h - startHour;
            int xCenter = (int) (marginLeft + slotIndex * slotWidth + slotWidth / 2);
            int xLeft = (int) (xCenter - barWidth / 2);

            int barHeight = (int) ((double) count / yMax * chartHeight);
            int yTop = marginTop + chartHeight - barHeight;

            // Bar fill
            if (count > 0) {
                Color c1, c2;
                if (h == peakHour && count > 1) {
                    c1 = new Color(245, 158, 11); // Amber peak
                    c2 = new Color(217, 119, 6);
                } else {
                    c1 = new Color(59, 130, 246); // Blue
                    c2 = new Color(37, 99, 235);
                }

                GradientPaint gp = new GradientPaint(xLeft, yTop, c1, xLeft, yTop + barHeight, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(xLeft, yTop, (int) barWidth, barHeight, 6, 6);

                // Bar outline
                g2.setColor(c2.darker());
                g2.drawRoundRect(xLeft, yTop, (int) barWidth, barHeight, 6, 6);

                // Count label above bar
                g2.setColor(Theme.getTextPrimary());
                g2.setFont(Theme.FONT_SMALL_BOLD);
                String valStr = String.valueOf(count);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(valStr, xCenter - fm.stringWidth(valStr) / 2, yTop - 4);
            }

            // X-axis Hour Label
            g2.setColor(Theme.getTextSecondary());
            g2.setFont(Theme.FONT_SMALL);
            String hourLabel = String.format("%02d", h);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hourLabel, xCenter - fm.stringWidth(hourLabel) / 2, marginTop + chartHeight + 16);
        }

        // X-axis axis baseline
        g2.setColor(Theme.getTextSecondary());
        g2.drawLine(marginLeft, marginTop + chartHeight, marginLeft + chartWidth, marginTop + chartHeight);

        // Average line
        if (avgVal > 0) {
            int yAvg = marginTop + chartHeight - (int) (avgVal / yMax * chartHeight);
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
            g2.setColor(new Color(239, 68, 68)); // Red dashed average line
            g2.drawLine(marginLeft, yAvg, marginLeft + chartWidth, yAvg);
            g2.setStroke(oldStroke);

            g2.setFont(Theme.FONT_SMALL);
            g2.drawString(String.format("Avg: %.1f", avgVal), marginLeft + chartWidth - 55, yAvg - 4);
        }

        g2.dispose();
    }
}
