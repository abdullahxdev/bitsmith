package aluviz;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdderSchematicPanel extends JPanel {

    private ALUCore.Result current;
    private int highlightedBit = -1;
    private Timer timer;

    private static final int CELL_W = 130;
    private static final int CELL_H = 220;
    private static final int MARGIN_X = 30;
    private static final int MARGIN_Y = 40;

    public AdderSchematicPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(
            MARGIN_X * 2 + CELL_W * ALUCore.WIDTH,
            MARGIN_Y * 2 + CELL_H + 120));
    }

    public void animate(ALUCore.Result r, int speedMs) {
        if (timer != null && timer.isRunning()) timer.stop();
        this.current = r;
        this.highlightedBit = -1;
        repaint();

        final int delay = Math.max(40, 900 - speedMs);
        timer = new Timer(delay, null);
        timer.addActionListener(e -> {
            highlightedBit++;
            if (highlightedBit >= ALUCore.WIDTH) {
                timer.stop();
                highlightedBit = ALUCore.WIDTH; // all done
            }
            repaint();
        });
        timer.setInitialDelay(200);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (current == null) {
            g.setColor(Color.GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 16));
            g.drawString("Pick ADD, SUB, or SLT and click Compute to see the ripple-carry adder.",
                30, 60);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            g.drawString("Each full adder = (A XOR B XOR Cin) for Sum, and (A·B + Cin·(A XOR B)) for Cout.",
                30, 90);
            return;
        }

        boolean isSub = current.op == ALUCore.Op.SUB || current.op == ALUCore.Op.SLT;
        String title = (current.op == ALUCore.Op.SLT)
            ? "SLT — internally A - B; result is sign-bit XOR overflow"
            : (isSub ? "SUB — A + (~B) + 1   (reuses the adder by inverting B and carry-in = 1)"
                     : "ADD — A + B");
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(new Color(20, 70, 130));
        g.drawString(title, MARGIN_X, 22);

        // Label legend at left
        int textY0 = MARGIN_Y + 30;
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g.drawString("bit i:", 4, MARGIN_Y + 10);
        g.drawString("A:",    4, textY0);
        g.drawString("B" + (isSub ? "(inv)" : "") + ":", 4, textY0 + 18);
        g.drawString("Cin:",  4, textY0 + 36);
        g.drawString("Sum:",  4, textY0 + 200);

        List<ALUCore.FullAdderStep> trace = current.adderTrace;
        if (trace == null) return;

        // Draw from LSB on the right to MSB on the left (matches schematic convention)
        for (int idx = 0; idx < ALUCore.WIDTH; idx++) {
            int displayCol = (ALUCore.WIDTH - 1) - idx; // bit 0 (LSB) on far right
            int x = MARGIN_X + displayCol * CELL_W;
            ALUCore.FullAdderStep step = trace.get(idx);
            boolean active = idx <= highlightedBit;
            drawFullAdder(g, x, MARGIN_Y, step, active, idx);
        }

        // Final carry-out arrow on the far left
        int xCout = MARGIN_X - 4;
        int yCout = MARGIN_Y + 110;
        ALUCore.FullAdderStep msb = trace.get(ALUCore.WIDTH - 1);
        boolean msbActive = highlightedBit >= ALUCore.WIDTH - 1;
        g.setColor(msbActive ? (msb.carryOut == 1 ? new Color(200, 30, 30) : new Color(120, 120, 120))
                             : new Color(200, 200, 200));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(MARGIN_X, yCout, xCout - 4, yCout);
        g.fillPolygon(new int[]{xCout - 4, xCout - 12, xCout - 12},
                      new int[]{yCout, yCout - 5, yCout + 5}, 3);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        g.drawString("Cout=" + (msbActive ? msb.carryOut : "?"), xCout - 78, yCout - 6);

        // Bottom summary row
        int yBot = MARGIN_Y + CELL_H + 30;
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        g.setColor(Color.BLACK);
        g.drawString("A      = " + ALUCore.toBinary(current.aEffective),  MARGIN_X, yBot);
        g.drawString("B" + (isSub ? "(inv)" : "    ") + " = " + ALUCore.toBinary(current.bEffective),
            MARGIN_X, yBot + 18);
        if (isSub) g.drawString("(+1 carry-in)", MARGIN_X + 280, yBot + 18);
        g.drawString("Result = " + ALUCore.toBinary(current.result), MARGIN_X, yBot + 36);

        g.setColor(new Color(80, 80, 80));
        g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        g.drawString("Carry propagates left → right of MSB. Highlighted cells have already computed.",
            MARGIN_X, yBot + 62);
    }

    private void drawFullAdder(Graphics2D g, int x, int y, ALUCore.FullAdderStep step,
                               boolean active, int bitIndex) {
        Color box = active ? new Color(220, 240, 255) : new Color(245, 245, 245);
        Color border = active ? new Color(20, 90, 170) : new Color(180, 180, 180);

        g.setColor(box);
        g.fillRoundRect(x + 4, y, CELL_W - 14, CELL_H, 14, 14);
        g.setColor(border);
        g.setStroke(new BasicStroke(active ? 2.0f : 1.0f));
        g.drawRoundRect(x + 4, y, CELL_W - 14, CELL_H, 14, 14);

        // Bit index label
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        g.setColor(active ? new Color(20, 70, 130) : Color.GRAY);
        g.drawString("bit " + bitIndex, x + 12, y - 4);

        // Inputs A, B (top edge), Cin (right side)
        int xCenter = x + CELL_W / 2;
        drawBitValue(g, x + 30, y + 30, "A", step.a, active);
        drawBitValue(g, x + 75, y + 30, "B", step.b, active);
        drawBitValue(g, x + CELL_W - 20, y + 100, "Cin", step.carryIn, active);

        // Two gates inside: XOR for sum, AND/OR cluster for carry
        // XOR gate (sum) — top half
        int gx = x + 28, gy = y + 60;
        drawGate(g, gx, gy, "XOR", active);
        // AND/OR cluster (carry) — bottom half
        drawGate(g, gx, gy + 80, "AND/OR", active);

        // Sum output (bottom edge)
        drawBitValue(g, xCenter - 10, y + CELL_H - 12, "S", step.sum, active);
        // Carry out (left edge — goes to next adder)
        drawBitValue(g, x + 2, y + 110, "Cout", step.carryOut, active);

        // Carry-out arrow pointing left
        if (active) {
            g.setColor(step.carryOut == 1 ? new Color(200, 30, 30) : new Color(120, 120, 120));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(x + 4, y + 110, x - 6, y + 110);
            g.fillPolygon(new int[]{x - 6, x + 2, x + 2},
                          new int[]{y + 110, y + 106, y + 114}, 3);
        }
    }

    private void drawGate(Graphics2D g, int x, int y, String label, boolean active) {
        g.setColor(active ? new Color(255, 250, 220) : new Color(250, 250, 250));
        g.fillRoundRect(x, y, 70, 30, 10, 10);
        g.setColor(active ? new Color(170, 110, 0) : new Color(190, 190, 190));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, 70, 30, 10, 10);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        g.setColor(active ? new Color(120, 60, 0) : Color.GRAY);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label);
        g.drawString(label, x + (70 - tw) / 2, y + 20);
    }

    private void drawBitValue(Graphics2D g, int x, int y, String label, int bit, boolean active) {
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        g.setColor(active ? Color.DARK_GRAY : new Color(180, 180, 180));
        g.drawString(label, x - 12, y - 12);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        if (active) {
            g.setColor(bit == 1 ? new Color(0, 130, 60) : new Color(100, 100, 100));
        } else {
            g.setColor(new Color(210, 210, 210));
        }
        g.drawString(String.valueOf(bit), x, y);
    }
}
