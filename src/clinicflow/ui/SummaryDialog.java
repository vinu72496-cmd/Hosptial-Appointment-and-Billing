package clinicflow.ui;

import clinicflow.model.Patient;
import clinicflow.service.ReportService;
import clinicflow.service.ReportService.DailyReport;
import clinicflow.util.TimeUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Daily Summary & Reporting dialog with KPI cards, wait time statistics,
 * custom Graphics2D hourly arrival chart, and report export functions.
 */
public class SummaryDialog extends JDialog {

    private final ReportService reportService;
    private DailyReport currentReport;
    private HourlyBarChartPanel chartPanel;

    private JLabel regCountLabel;
    private JLabel doneCountLabel;
    private JLabel activeCountLabel;
    private JLabel cancelCountLabel;
    private JLabel avgWaitLabel;
    private JLabel avgConsultLabel;
    private JLabel avgVisitLabel;
    private JLabel longestWaitLabel;
    private JLabel busiestHourLabel;

    public SummaryDialog(Window owner, ReportService reportService) {
        super(owner, "Daily Summary & Performance Analytics", ModalityType.APPLICATION_MODAL);
        this.reportService = reportService;

        setSize(780, 680);
        setMinimumSize(new Dimension(700, 600));
        setLocationRelativeTo(owner);

        initUI();
        refreshReport();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Theme.getBackground());
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        // Header: Title & Date & Refresh
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Clinic Daily Analytics Report");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.getTextPrimary());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel dateLabel = new JLabel("Report Date: " + LocalDate.now());
        dateLabel.setFont(Theme.FONT_SUBHEADER);
        dateLabel.setForeground(Theme.getTextSecondary());
        headerPanel.add(dateLabel, BorderLayout.EAST);

        root.add(headerPanel, BorderLayout.NORTH);

        // Center: Scrollable Dashboard Content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Section 1: KPI Volume Cards (4 cards in grid)
        JPanel volumeGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        volumeGrid.setOpaque(false);
        volumeGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        regCountLabel = new JLabel("0");
        doneCountLabel = new JLabel("0");
        activeCountLabel = new JLabel("0");
        cancelCountLabel = new JLabel("0");

        volumeGrid.add(createKpiCard("TOTAL REGISTERED", regCountLabel, new Color(59, 130, 246)));
        volumeGrid.add(createKpiCard("COMPLETED VISITS", doneCountLabel, new Color(16, 185, 129)));
        volumeGrid.add(createKpiCard("CURRENTLY ACTIVE", activeCountLabel, new Color(245, 158, 11)));
        volumeGrid.add(createKpiCard("CANCELLED VISITS", cancelCountLabel, new Color(239, 68, 68)));

        contentPanel.add(volumeGrid);
        contentPanel.add(Box.createVerticalStrut(12));

        // Section 2: Time Efficiency Metrics (3 cards)
        JPanel timeGrid = new JPanel(new GridLayout(1, 3, 10, 0));
        timeGrid.setOpaque(false);
        timeGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        avgWaitLabel = new JLabel("0 min");
        avgConsultLabel = new JLabel("0 min");
        avgVisitLabel = new JLabel("0 min");

        timeGrid.add(createKpiCard("AVG WAITING TIME", avgWaitLabel, new Color(14, 165, 233)));
        timeGrid.add(createKpiCard("AVG CONSULTATION", avgConsultLabel, new Color(168, 85, 247)));
        timeGrid.add(createKpiCard("AVG TOTAL VISIT", avgVisitLabel, new Color(107, 114, 128)));

        contentPanel.add(timeGrid);
        contentPanel.add(Box.createVerticalStrut(12));

        // Section 3: Bottlenecks & Insights Banner
        JPanel insightBanner = new JPanel(new GridLayout(1, 2, 10, 0));
        insightBanner.setOpaque(true);
        insightBanner.setBackground(Theme.getSurface());
        insightBanner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.getBorderColor(), 1),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        insightBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        longestWaitLabel = new JLabel("Longest Wait: None");
        longestWaitLabel.setFont(Theme.FONT_REGULAR_BOLD);
        longestWaitLabel.setForeground(Theme.getTextPrimary());

        busiestHourLabel = new JLabel("Busiest Hour: N/A");
        busiestHourLabel.setFont(Theme.FONT_REGULAR_BOLD);
        busiestHourLabel.setForeground(Theme.getTextPrimary());

        insightBanner.add(longestWaitLabel);
        insightBanner.add(busiestHourLabel);

        contentPanel.add(insightBanner);
        contentPanel.add(Box.createVerticalStrut(12));

        // Section 4: Graphics2D Hourly Arrival Chart
        chartPanel = new HourlyBarChartPanel(null);
        chartPanel.setBorder(BorderFactory.createLineBorder(Theme.getBorderColor(), 1));
        contentPanel.add(chartPanel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.getBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);

        // Footer: Export Buttons & Close
        JPanel footerPanel = new JPanel(new BorderLayout(10, 0));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JPanel exportRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        exportRow.setOpaque(false);

        JButton exportTxtBtn = new JButton("📄 Export Report (.txt)");
        exportTxtBtn.setFont(Theme.FONT_REGULAR);
        exportTxtBtn.addActionListener(e -> exportTxt());
        exportRow.add(exportTxtBtn);

        JButton exportCsvBtn = new JButton("📊 Export Data (.csv)");
        exportCsvBtn.setFont(Theme.FONT_REGULAR);
        exportCsvBtn.addActionListener(e -> exportCsv());
        exportRow.add(exportCsvBtn);

        footerPanel.add(exportRow, BorderLayout.WEST);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(Theme.FONT_REGULAR);
        closeBtn.addActionListener(e -> dispose());
        footerPanel.add(closeBtn, BorderLayout.EAST);

        root.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createKpiCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setOpaque(true);
        card.setBackground(Theme.getSurface());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 0, 0, accent),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel titleLbl = new JLabel(label);
        titleLbl.setFont(Theme.FONT_SMALL_BOLD);
        titleLbl.setForeground(Theme.getTextSecondary());
        card.add(titleLbl, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(Theme.getTextPrimary());
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void refreshReport() {
        currentReport = reportService.generateReport(LocalDate.now());

        regCountLabel.setText(String.valueOf(currentReport.getRegisteredCount()));
        doneCountLabel.setText(String.valueOf(currentReport.getCompletedCount()));
        activeCountLabel.setText(String.valueOf(currentReport.getActiveCount()));
        cancelCountLabel.setText(String.valueOf(currentReport.getCancelledCount()));

        avgWaitLabel.setText(TimeUtil.formatHuman(currentReport.getAverageWaitTime()));
        avgConsultLabel.setText(TimeUtil.formatHuman(currentReport.getAverageConsultTime()));
        avgVisitLabel.setText(TimeUtil.formatHuman(currentReport.getAverageTotalVisitTime()));

        if (currentReport.getLongestWaitPatient() != null) {
            longestWaitLabel.setText(String.format("Longest Wait: %s (%s - %s)",
                TimeUtil.formatHuman(currentReport.getLongestWaitDuration()),
                currentReport.getLongestWaitPatient().getToken(),
                currentReport.getLongestWaitPatient().getName()));
        } else {
            longestWaitLabel.setText("Longest Wait: None");
        }

        if (currentReport.getBusiestHour() >= 0) {
            busiestHourLabel.setText(String.format("Busiest Hour: %02d:00 - %02d:00 (%d arrivals)",
                currentReport.getBusiestHour(), currentReport.getBusiestHour() + 1, currentReport.getBusiestHourCount()));
        } else {
            busiestHourLabel.setText("Busiest Hour: N/A");
        }

        chartPanel.setData(currentReport.getHourlyArrivals());
    }

    private void exportTxt() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("daily_report_" + LocalDate.now() + ".txt"));
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                reportService.exportToTxt(currentReport, f);
                JOptionPane.showMessageDialog(this, "Daily summary report saved to:\n" + f.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to export report: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("patient_flow_" + LocalDate.now() + ".csv"));
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                reportService.exportToCsv(currentReport, f);
                JOptionPane.showMessageDialog(this, "Patient flow data exported to:\n" + f.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
