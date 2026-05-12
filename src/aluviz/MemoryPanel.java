package aluviz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data memory view. Shows a window of word-aligned addresses with values
 * and an ASCII column. Highlights active read (green) and write (pink) rows.
 */
public class MemoryPanel extends JPanel {

    private static final int ROWS_VISIBLE   = 12;
    private static final int ROW_H          = 26;

    private static final Color BG            = new Color(0xFAFAFA);
    private static final Color ROW_BG        = Color.WHITE;
    private static final Color ROW_ALT       = new Color(0xF5F5F5);
    private static final Color BORDER        = new Color(0xE0E0E0);
    private static final Color TEXT          = new Color(0x212121);
    private static final Color TEXT_MUTED    = new Color(0x9E9E9E);
    private static final Color HEADER_BG     = new Color(0xEEEEEE);
    private static final Color READ_BG       = new Color(0xC8E6C9);
    private static final Color READ_BORDER   = new Color(0x388E3C);
    private static final Color WRITE_BG      = new Color(0xF8BBD0);
    private static final Color WRITE_BORDER  = new Color(0xC2185B);

    private final MachineState state;
    private final RowsCanvas canvas;
    private final JScrollPane scroller;
    private final JLabel statusLine = new JLabel(" ");

    private int activeAddress = -1;
    private boolean activeIsWrite = false;

    public MemoryPanel(MachineState state) {
        super(new BorderLayout(0, 6));
        this.state = state;
        setBackground(BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Data Memory"),
            new EmptyBorder(6, 8, 6, 8)));

        canvas = new RowsCanvas();
        scroller = new JScrollPane(canvas,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(BorderFactory.createLineBorder(BORDER));
        scroller.setPreferredSize(new Dimension(290, ROWS_VISIBLE * ROW_H + 28));
        scroller.getViewport().setBackground(ROW_BG);

        add(headerPanel(), BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);

        statusLine.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusLine.setForeground(TEXT_MUTED);
        statusLine.setBorder(new EmptyBorder(4, 6, 0, 6));
        add(statusLine, BorderLayout.SOUTH);

        refreshRows();
    }

    private JComponent headerPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HEADER_BG);
        p.setBorder(BorderFactory.createLineBorder(BORDER));
        JLabel h = new JLabel(" Address       Value (hex)   ASCII");
        h.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        h.setForeground(TEXT);
        p.add(h, BorderLayout.WEST);
        return p;
    }

    public void markRead(int address) {
        activeAddress = address;
        activeIsWrite = false;
        refreshRows();
        scrollToAddress(address);
        updateStatus();
    }

    public void markWrite(int address, int value) {
        state.writeMem(address, value);
        activeAddress = address;
        activeIsWrite = true;
        refreshRows();
        scrollToAddress(address);
        updateStatus();
    }

    public void clearHighlights() {
        activeAddress = -1;
        refreshRows();
        updateStatus();
    }

    public void refreshRows() {
        canvas.refreshFromState();
        canvas.repaint();
    }

    /** Connection point for the wires overlay (left edge, middle of memory panel). */
    public Point connectionPoint() {
        Point p = scroller.getLocation();
        return new Point(p.x, p.y + scroller.getHeight() / 2);
    }

    private void scrollToAddress(int address) {
        int row = canvas.indexOfAddress(address);
        if (row < 0) {
            canvas.addRow(address, state.readMem(address));
            row = canvas.indexOfAddress(address);
        }
        Rectangle r = new Rectangle(0, row * ROW_H, canvas.getWidth(), ROW_H);
        canvas.scrollRectToVisible(r);
    }

    private void updateStatus() {
        if (activeAddress < 0) {
            statusLine.setText("(idle)");
            return;
        }
        statusLine.setText(
            (activeIsWrite ? "Wrote to " : "Read from ") + "0x"
            + String.format("%08X", activeAddress));
    }

    private class RowsCanvas extends JPanel implements Scrollable {

        private final List<int[]> rows = new ArrayList<>();   // each entry: [address, value]

        RowsCanvas() {
            setBackground(ROW_BG);
            refreshFromState();
        }

        void refreshFromState() {
            rows.clear();
            // Show seeded memory + a window around MEM_BASE
            Map<Integer, Integer> mem = state.snapshotMemory();
            for (Map.Entry<Integer, Integer> e : mem.entrySet()) {
                rows.add(new int[]{e.getKey(), e.getValue()});
            }
            // Sort by address ascending
            rows.sort((a, b) -> Integer.compareUnsigned(a[0], b[0]));
            // Ensure base window is visible
            ensureRange(MachineState.MEM_BASE, MachineState.MEM_WORDS_VISIBLE);
            setPreferredSize(new Dimension(280, Math.max(rows.size(), ROWS_VISIBLE) * ROW_H));
            revalidate();
        }

        private void ensureRange(int base, int wordCount) {
            for (int i = 0; i < wordCount; i++) {
                int addr = base + i * 4;
                if (indexOfAddress(addr) < 0) {
                    rows.add(new int[]{addr, state.readMem(addr)});
                }
            }
            rows.sort((a, b) -> Integer.compareUnsigned(a[0], b[0]));
        }

        void addRow(int address, int value) {
            if (indexOfAddress(address) >= 0) return;
            rows.add(new int[]{address, value});
            rows.sort((a, b) -> Integer.compareUnsigned(a[0], b[0]));
            setPreferredSize(new Dimension(280, rows.size() * ROW_H));
            revalidate();
        }

        int indexOfAddress(int address) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i)[0] == address) return i;
            }
            return -1;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int y = 0;
            for (int i = 0; i < rows.size(); i++) {
                int[] row = rows.get(i);
                boolean isActive = row[0] == activeAddress;
                Color bg = (i % 2 == 0) ? ROW_BG : ROW_ALT;
                Color border = null;
                if (isActive) {
                    bg = activeIsWrite ? WRITE_BG : READ_BG;
                    border = activeIsWrite ? WRITE_BORDER : READ_BORDER;
                }
                g.setColor(bg);
                g.fillRect(0, y, w, ROW_H);
                if (border != null) {
                    g.setColor(border);
                    g.setStroke(new BasicStroke(2f));
                    g.drawRect(1, y + 1, w - 3, ROW_H - 3);
                }

                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                g.setColor(TEXT);
                String addr  = String.format("0x%08X", row[0]);
                String value = String.format("%08X", row[1]);
                String ascii = bytesToAscii(row[1]);
                g.drawString(addr,  8, y + ROW_H / 2 + 5);
                g.drawString(value, 112, y + ROW_H / 2 + 5);
                g.setColor(TEXT_MUTED);
                g.drawString(ascii, 200, y + ROW_H / 2 + 5);

                y += ROW_H;
            }
        }

        private String bytesToAscii(int v) {
            byte[] bytes = {
                (byte) ((v >> 24) & 0xFF),
                (byte) ((v >> 16) & 0xFF),
                (byte) ((v >> 8) & 0xFF),
                (byte) (v & 0xFF)
            };
            StringBuilder sb = new StringBuilder(4);
            for (byte b : bytes) {
                char c = (char) (b & 0xFF);
                sb.append((c >= 32 && c < 127) ? c : '·');
            }
            return sb.toString();
        }

        @Override public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(280, ROWS_VISIBLE * ROW_H);
        }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return ROW_H; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return ROW_H * 4; }
        @Override public boolean getScrollableTracksViewportWidth()  { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
