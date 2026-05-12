package aluviz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * 32-register grid visualization with read/write highlighting.
 * 4 rows × 8 columns. Each cell is a small JPanel with reg index, value, name.
 */
public class RegisterFilePanel extends JPanel {

    public static final int COLS = 8;
    public static final int ROWS = 4;
    public static final int CELL_W = 84;
    public static final int CELL_H = 64;

    private static final Color BG          = new Color(0xFAFAFA);
    private static final Color CELL_BG     = Color.WHITE;
    private static final Color CELL_BORDER = new Color(0xE0E0E0);
    private static final Color TEXT        = new Color(0x212121);
    private static final Color TEXT_MUTED  = new Color(0x757575);
    private static final Color ZERO_BG     = new Color(0xF5F5F5);   // $zero is special — slightly muted
    private static final Color READ_BG     = new Color(0xBBDEFB);   // light blue
    private static final Color READ_BORDER = new Color(0x1976D2);   // strong blue
    private static final Color WRITE_BG    = new Color(0xFFE0B2);   // light orange
    private static final Color WRITE_BORDER = new Color(0xF57C00);  // strong orange

    private final MachineState state;
    private final Cell[] cells = new Cell[MachineState.NUM_REGS];
    private int readA = -1, readB = -1, writeIdx = -1;
    private int lastWriteValue = 0;

    private final JLabel statusLine = new JLabel(" ");

    public RegisterFilePanel(MachineState state) {
        super(new BorderLayout(0, 6));
        this.state = state;
        setBackground(BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Register File (32 × 32-bit)"),
            new EmptyBorder(6, 8, 6, 8)));

        JPanel grid = new JPanel(new GridLayout(ROWS, COLS, 4, 4));
        grid.setBackground(BG);
        for (int idx = 0; idx < MachineState.NUM_REGS; idx++) {
            cells[idx] = new Cell(idx);
            grid.add(cells[idx]);
        }
        add(grid, BorderLayout.CENTER);

        statusLine.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusLine.setForeground(TEXT_MUTED);
        statusLine.setBorder(new EmptyBorder(4, 6, 0, 6));
        add(statusLine, BorderLayout.SOUTH);

        refreshAll();
    }

    /** Highlight a register as being read into the given ALU port ("A", "B", or "data"). */
    public void markRead(int idx, String port) {
        if (port.equalsIgnoreCase("A") || port.equalsIgnoreCase("data")) readA = idx;
        else if (port.equalsIgnoreCase("B"))                              readB = idx;
        repaintCells();
        updateStatus();
    }

    /** Highlight a register as being written to (and update its value visually). */
    public void markWrite(int idx, int value) {
        writeIdx = idx;
        lastWriteValue = value;
        cells[idx].setValueDisplay(value);
        state.writeReg(idx, value);
        repaintCells();
        updateStatus();
    }

    /** Clear all highlights and revert visual styling to idle. */
    public void clearHighlights() {
        readA = -1; readB = -1; writeIdx = -1;
        repaintCells();
        updateStatus();
    }

    /** Refresh all displayed values from state (after reset, etc.). */
    public void refreshAll() {
        for (int i = 0; i < MachineState.NUM_REGS; i++) {
            cells[i].setValueDisplay(state.readReg(i));
        }
        repaintCells();
    }

    /** Bounds of a register cell in panel coordinates — used by WiresOverlay. */
    public Point connectionPoint(int regIdx, boolean writeSide) {
        Cell c = cells[regIdx];
        Point p = SwingUtilities.convertPoint(c.getParent(), c.getLocation(), this);
        if (writeSide) return new Point(p.x + c.getWidth(), p.y + c.getHeight() / 2);
        return new Point(p.x + c.getWidth(), p.y + c.getHeight() / 2);
    }

    private void repaintCells() {
        for (Cell c : cells) c.repaint();
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        if (readA >= 0) sb.append("Reading ").append(MachineState.regName(readA))
                          .append(" → port A   ");
        if (readB >= 0) sb.append("Reading ").append(MachineState.regName(readB))
                          .append(" → port B   ");
        if (writeIdx >= 0) sb.append("Writing 0x").append(String.format("%08X", lastWriteValue))
                            .append(" → ").append(MachineState.regName(writeIdx));
        if (sb.length() == 0) sb.append("(idle)");
        statusLine.setText(sb.toString());
    }

    /** A single register cell. */
    private class Cell extends JComponent {
        private final int idx;
        private int displayValue;

        Cell(int idx) {
            this.idx = idx;
            setPreferredSize(new Dimension(CELL_W, CELL_H));
            this.displayValue = 0;

            // Click to edit (except $zero)
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (idx == 0) return;
                    String input = JOptionPane.showInputDialog(Cell.this,
                        "Edit value of " + MachineState.regName(idx)
                          + " (decimal, 0xHEX, or 0bBIN):",
                        Integer.toString(displayValue));
                    if (input == null) return;
                    try {
                        int v = parseValue(input);
                        state.writeReg(idx, v);
                        displayValue = v;
                        repaint();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(Cell.this, "Bad number: " + input,
                            "Parse error", JOptionPane.WARNING_MESSAGE);
                    }
                }
            });
        }

        void setValueDisplay(int v) {
            this.displayValue = v;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            boolean isRead  = (idx == readA || idx == readB);
            boolean isWrite = (idx == writeIdx);
            boolean isZero  = idx == 0;

            Color bg = CELL_BG, border = CELL_BORDER;
            if (isWrite) {
                bg = WRITE_BG; border = WRITE_BORDER;
            } else if (isRead) {
                bg = READ_BG; border = READ_BORDER;
            } else if (isZero) {
                bg = ZERO_BG;
            }
            g.setColor(bg);
            g.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g.setColor(border);
            g.setStroke(new BasicStroke(isRead || isWrite ? 2f : 1f));
            g.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

            // Register index (top-left)
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            g.drawString("$" + idx, 6, 12);

            // Register name (top-right)
            String name = MachineState.regName(idx);
            FontMetrics fm = g.getFontMetrics();
            int nameW = fm.stringWidth(name);
            g.drawString(name, w - nameW - 6, 12);

            // Value (center) — hex
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            g.setColor(isRead || isWrite ? new Color(0x0D47A1) : TEXT);
            if (isWrite) g.setColor(new Color(0xE65100));
            String hex = String.format("0x%08X", displayValue);
            FontMetrics fm2 = g.getFontMetrics();
            int hexW = fm2.stringWidth(hex);
            int hexY = h / 2 + 8;
            g.drawString(hex, (w - hexW) / 2, hexY);

            // Decimal hint (bottom)
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            String dec = "= " + displayValue;
            int decW = g.getFontMetrics().stringWidth(dec);
            g.drawString(dec, (w - decW) / 2, h - 6);
        }

        private int parseValue(String s) {
            s = s.trim().toLowerCase();
            if (s.startsWith("0x")) return (int) Long.parseLong(s.substring(2), 16);
            if (s.startsWith("0b")) return Integer.parseInt(s.substring(2), 2);
            return Integer.parseInt(s);
        }
    }
}
