package aluviz;

import javax.swing.*;
import java.awt.*;

public class RegisterViewPanel extends JPanel {

    private ALUCore.Result current;
    private int highlightedBit = -1;
    private Timer timer;

    private static final int BIT_W = 60;
    private static final int BIT_H = 50;
    private static final int ROW_GAP = 18;
    private static final int MARGIN_X = 60;
    private static final int MARGIN_Y = 50;

    public RegisterViewPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(
            MARGIN_X * 2 + BIT_W * ALUCore.WIDTH + 80,
            MARGIN_Y * 2 + (BIT_H + ROW_GAP) * 5 + 80));
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
                highlightedBit = ALUCore.WIDTH;
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
            g.drawString("Pick AND/OR/XOR/NOR/SLL/SRL/SRA and click Compute.",
                30, 60);
            return;
        }

        ALUCore.Op op = current.op;
        switch (op) {
            case AND: case OR: case XOR: case NOR:
                drawBitwise(g);
                break;
            case SLL: case SRL: case SRA:
                drawShift(g);
                break;
            default:
                g.setColor(Color.GRAY);
                g.drawString("This operation is shown in the schematic view tab.", 30, 60);
        }
    }

    private void drawBitwise(Graphics2D g) {
        String title;
        switch (current.op) {
            case AND: title = "AND — bitwise A·B    (bit is 1 iff both A and B bits are 1)"; break;
            case OR:  title = "OR  — bitwise A+B    (bit is 1 iff either bit is 1)"; break;
            case XOR: title = "XOR — bitwise A⊕B    (bit is 1 iff bits differ)"; break;
            case NOR: title = "NOR — bitwise ¬(A+B) (bit is 1 iff both bits are 0)"; break;
            default:  title = "";
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(new Color(20, 70, 130));
        g.drawString(title, MARGIN_X - 30, 22);

        int y = MARGIN_Y;
        drawLabeledRow(g, "A",      current.a,      y, true); y += BIT_H + ROW_GAP;
        drawLabeledRow(g, "B",      current.b,      y, true); y += BIT_H + ROW_GAP;

        // Gate row
        int gateY = y;
        for (int i = 0; i < ALUCore.WIDTH; i++) {
            int col = (ALUCore.WIDTH - 1) - i;
            int x = MARGIN_X + col * BIT_W;
            boolean active = i <= highlightedBit;
            drawGateBox(g, x + 6, gateY + 6, BIT_W - 12, BIT_H - 12, opShortName(current.op), active);
        }
        y += BIT_H + ROW_GAP;

        drawLabeledRow(g, "Result", current.result, y, true);

        // Truth table reminder
        int ttY = y + BIT_H + 20;
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g.setColor(new Color(80, 80, 80));
        g.drawString(truthTable(current.op), MARGIN_X - 30, ttY);
    }

    private void drawShift(Graphics2D g) {
        int shamt = current.b & (ALUCore.WIDTH - 1);
        String desc;
        switch (current.op) {
            case SLL: desc = "SLL — Shift Left Logical by " + shamt + " (fills with 0 on right)"; break;
            case SRL: desc = "SRL — Shift Right Logical by " + shamt + " (fills with 0 on left)"; break;
            case SRA: desc = "SRA — Shift Right Arithmetic by " + shamt + " (preserves sign bit)"; break;
            default:  desc = "";
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(new Color(20, 70, 130));
        g.drawString(desc, MARGIN_X - 30, 22);

        int y = MARGIN_Y;
        drawLabeledRow(g, "A (input)",  current.a,      y, true); y += BIT_H + ROW_GAP;

        // Arrow row showing direction
        int arrowY = y + BIT_H / 2;
        g.setColor(new Color(20, 90, 170));
        g.setStroke(new BasicStroke(2.5f));
        boolean leftShift = current.op == ALUCore.Op.SLL;
        for (int i = 0; i < ALUCore.WIDTH - 1; i++) {
            int col = (ALUCore.WIDTH - 1) - i;
            int x1 = MARGIN_X + col * BIT_W + BIT_W / 2;
            int x2 = x1 + (leftShift ? -BIT_W : BIT_W);
            g.drawLine(x1, arrowY, x2, arrowY);
            int xa = x2;
            int dir = leftShift ? -1 : 1;
            g.fillPolygon(new int[]{xa, xa - 8 * dir, xa - 8 * dir},
                          new int[]{arrowY, arrowY - 5, arrowY + 5}, 3);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        g.setColor(new Color(80, 80, 80));
        g.drawString(leftShift ? "shift  ←" : "shift  →",
            MARGIN_X + BIT_W * ALUCore.WIDTH + 10, arrowY + 4);
        y += BIT_H + ROW_GAP;

        drawLabeledRow(g, "Result", current.result, y, true);

        if (current.op == ALUCore.Op.SRA) {
            int signBit = (current.a >> (ALUCore.WIDTH - 1)) & 1;
            g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            g.setColor(new Color(120, 60, 0));
            g.drawString("Sign bit (MSB) was " + signBit + " — SRA fills vacated MSBs with that.",
                MARGIN_X - 30, y + BIT_H + 25);
        }
    }

    private void drawLabeledRow(Graphics2D g, String label, int value, int y, boolean fullyActive) {
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        g.setColor(Color.DARK_GRAY);
        g.drawString(label, MARGIN_X - 50, y + BIT_H / 2 + 6);

        for (int i = 0; i < ALUCore.WIDTH; i++) {
            int col = (ALUCore.WIDTH - 1) - i;
            int x = MARGIN_X + col * BIT_W;
            int bit = (value >> i) & 1;
            boolean active = fullyActive ? (i <= highlightedBit) : true;
            drawBitCell(g, x, y, bit, active, i);
        }

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        g.setColor(new Color(120, 120, 120));
        g.drawString("= " + value,
            MARGIN_X + BIT_W * ALUCore.WIDTH + 10, y + BIT_H / 2 + 4);
    }

    private void drawBitCell(Graphics2D g, int x, int y, int bit, boolean active, int bitIndex) {
        Color fill = active ? (bit == 1 ? new Color(180, 230, 200) : new Color(245, 245, 245))
                            : new Color(252, 252, 252);
        Color border = active ? (bit == 1 ? new Color(0, 130, 60) : new Color(150, 150, 150))
                              : new Color(220, 220, 220);
        g.setColor(fill);
        g.fillRoundRect(x + 4, y, BIT_W - 8, BIT_H, 10, 10);
        g.setColor(border);
        g.setStroke(new BasicStroke(active ? 2f : 1f));
        g.drawRoundRect(x + 4, y, BIT_W - 8, BIT_H, 10, 10);

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        g.setColor(active ? Color.BLACK : new Color(210, 210, 210));
        FontMetrics fm = g.getFontMetrics();
        String s = String.valueOf(bit);
        int tw = fm.stringWidth(s);
        g.drawString(s, x + (BIT_W - tw) / 2, y + BIT_H / 2 + 7);

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
        g.setColor(active ? new Color(100, 100, 100) : new Color(220, 220, 220));
        g.drawString("bit " + bitIndex, x + 10, y + BIT_H - 4);
    }

    private void drawGateBox(Graphics2D g, int x, int y, int w, int h, String label, boolean active) {
        g.setColor(active ? new Color(255, 245, 200) : new Color(248, 248, 248));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(active ? new Color(170, 110, 0) : new Color(210, 210, 210));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        g.setColor(active ? new Color(120, 60, 0) : new Color(200, 200, 200));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label);
        g.drawString(label, x + (w - tw) / 2, y + h / 2 + 4);
    }

    private String opShortName(ALUCore.Op op) {
        switch (op) {
            case AND: return "AND";
            case OR:  return "OR";
            case XOR: return "XOR";
            case NOR: return "NOR";
            default:  return op.name();
        }
    }

    private String truthTable(ALUCore.Op op) {
        switch (op) {
            case AND: return "Truth table:  0·0=0  0·1=0  1·0=0  1·1=1";
            case OR:  return "Truth table:  0+0=0  0+1=1  1+0=1  1+1=1";
            case XOR: return "Truth table:  0⊕0=0  0⊕1=1  1⊕0=1  1⊕1=0";
            case NOR: return "Truth table:  ¬(0+0)=1  ¬(0+1)=0  ¬(1+0)=0  ¬(1+1)=0";
            default:  return "";
        }
    }
}
