package aluviz;

import javax.swing.*;
import java.awt.*;

/**
 * Root content pane for the application. Hosts the top-level tabs:
 *   Tab 1: ALU Internals  (existing tool — MainPanel)
 *   Tab 2: MIPS Data Flow (new — DataFlowPanel)
 */
public class AppPanel extends JPanel {

    private final MainPanel aluInternalsPanel;
    private final DataFlowPanel dataFlowPanel;
    private final JTabbedPane tabs;

    public AppPanel() {
        super(new BorderLayout());

        aluInternalsPanel = new MainPanel();
        dataFlowPanel = new DataFlowPanel();
        dataFlowPanel.setAppPanel(this);

        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 13f));
        tabs.addTab("ALU Internals", aluInternalsPanel);
        tabs.addTab("MIPS Data Flow", dataFlowPanel);

        add(tabs, BorderLayout.CENTER);
    }

    /** Switch to the ALU Internals tab — used by Data Flow tab's "Open Internals" button. */
    public void showALUInternals() {
        tabs.setSelectedIndex(0);
    }

    /** Switch to the Data Flow tab programmatically. */
    public void showDataFlow() {
        tabs.setSelectedIndex(1);
    }

    public MainPanel getALUInternalsPanel() { return aluInternalsPanel; }
    public DataFlowPanel getDataFlowPanel() { return dataFlowPanel; }
}
