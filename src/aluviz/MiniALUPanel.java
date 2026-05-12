package aluviz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Compact ALU panel for the Data Flow tab. Shows operands, operation, result,
 * and flags. Includes an "Open Internals" button that hands off to the existing
 * ALU Internals tab (Tab 1) for the gate-level view of the same operation.
 */
public class MiniALUPanel extends JPanel {

    private static final Color BG          = new Color(0xFAFAFA);
    private static final Color BORDER      = new Color(0xE0E0E0);
    private static final Color BOX_BG      = Color.WHITE;
    private static final Color BOX_BORDER  = new Color(0xBDBDBD);
    private static final Color ACTIVE_BG   = new Color(0xFFE0B2);
    private static final Color ACTIVE_BRD  = new Color(0xF57C00);
    private static final Color TEXT        = new Color(0x212121);
    private static final Color TEXT_MUTED  = new Color(0x757575);
    private static final Color FLAG_ON     = new Color(0xFFC107);
    private static final Color FLAG_OFF    = new Color(0xEEEEEE);

    private final JLabel opLabel       = new JLabel("—");
    private final JLabel inputA        = new JLabel("0x00000000");
    private final JLabel inputB        = new JLabel("0x00000000");
    private final JLabel resultLabel   = new JLabel("0x00000000");
    private final JLabel resultDecimal = new JLabel(" ");

    private final Lamp lampZ = new Lamp("Z");
    private final Lamp lampN = new Lamp("N");
    private final Lamp lampC = new Lamp("C");
    private final Lamp lampV = new Lamp("V");

    private final JButton openInternals = new JButton("Open Internals →");
    private final JPanel aluBox;

    private boolean active = false;

    public MiniALUPanel() {
        super(new BorderLayout(0, 6));
        setBackground(BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("ALU"),
            new EmptyBorder(6, 8, 6, 8)));

        // Inputs row (top)
        JPanel inputs = new JPanel(new GridLayout(2, 1, 0, 3));
        inputs.setBackground(BG);
        inputs.add(operandRow("Input A:", inputA));
        inputs.add(operandRow("Input B:", inputB));

        // ALU box (center) — operation + result
        aluBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color bg = active ? ACTIVE_BG : BOX_BG;
                Color brd = active ? ACTIVE_BRD : BOX_BORDER;
                g.setColor(bg);
                g.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g.setColor(brd);
                g.setStroke(new BasicStroke(active ? 2.2f : 1.2f));
                g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            }
        };
        aluBox.setOpaque(false);
        aluBox.setPreferredSize(new Dimension(220, 130));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;
        opLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        opLabel.setForeground(new Color(0xE65100));
        aluBox.add(opLabel, gc);
        gc.gridy = 1;
        JLabel arrow = new JLabel("↓");
        arrow.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
        arrow.setForeground(TEXT_MUTED);
        aluBox.add(arrow, gc);
        gc.gridy = 2;
        resultLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        aluBox.add(resultLabel, gc);
        gc.gridy = 3;
        resultDecimal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        resultDecimal.setForeground(TEXT_MUTED);
        aluBox.add(resultDecimal, gc);

        // Flags (right)
        JPanel flags = new JPanel(new GridLayout(1, 4, 4, 4));
        flags.setBackground(BG);
        flags.setBorder(BorderFactory.createTitledBorder("Flags"));
        flags.add(lampZ); flags.add(lampN); flags.add(lampC); flags.add(lampV);

        // Button row (bottom)
        openInternals.setFont(openInternals.getFont().deriveFont(Font.BOLD));
        openInternals.setFocusable(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        buttons.setBackground(BG);
        buttons.add(openInternals);

        // Assemble
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setBackground(BG);
        center.add(inputs, BorderLayout.NORTH);
        center.add(aluBox, BorderLayout.CENTER);
        center.add(flags, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    public void onOpenInternals(ActionListener l) {
        for (ActionListener old : openInternals.getActionListeners()) {
            openInternals.removeActionListener(old);
        }
        openInternals.addActionListener(l);
    }

    public void setInputs(Integer a, Integer b) {
        inputA.setText(a == null ? "—" : String.format("0x%08X", a));
        inputB.setText(b == null ? "—" : String.format("0x%08X", b));
    }

    public void setOperation(ALUCore.Op op) {
        opLabel.setText(op == null ? "—" : op.label);
    }

    public void setResult(int value, boolean z, boolean n, boolean c, boolean v) {
        resultLabel.setText(String.format("0x%08X", value));
        int signed = value;
        if (((value >> 31) & 1) == 1) signed = value | 0xFFFFFFFF;
        resultDecimal.setText("(signed: " + signed + ")");
        lampZ.setOn(z); lampN.setOn(n); lampC.setOn(c); lampV.setOn(v);
    }

    public void clearResult() {
        resultLabel.setText("0x00000000");
        resultDecimal.setText(" ");
        lampZ.setOn(false); lampN.setOn(false); lampC.setOn(false); lampV.setOn(false);
    }

    public void setActive(boolean isActive) {
        this.active = isActive;
        aluBox.repaint();
    }

    /** Connection point for incoming A wire (left edge, upper). */
    public Point inputAConnectionPoint() {
        return new Point(0, aluBox.getY() + 30);
    }

    /** Connection point for incoming B wire (left edge, lower). */
    public Point inputBConnectionPoint() {
        return new Point(0, aluBox.getY() + aluBox.getHeight() - 30);
    }

    /** Connection point for outgoing result wire (right edge, middle). */
    public Point outputConnectionPoint() {
        return new Point(getWidth(), aluBox.getY() + aluBox.getHeight() / 2);
    }

    private JPanel operandRow(String labelText, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG);
        JLabel l = new JLabel(labelText);
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        l.setForeground(TEXT_MUTED);
        valueLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        valueLabel.setForeground(TEXT);
        row.add(l, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private static class Lamp extends JPanel {
        private final JLabel label = new JLabel();
        private boolean on;
        Lamp(String name) {
            super(new BorderLayout());
            label.setText(name);
            label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            add(label, BorderLayout.CENTER);
            setBorder(BorderFactory.createLineBorder(new Color(0xCCCCCC)));
            setOn(false);
        }
        void setOn(boolean v) {
            this.on = v;
            setBackground(v ? FLAG_ON : FLAG_OFF);
            label.setForeground(v ? new Color(0x6D4C00) : new Color(0x9E9E9E));
            repaint();
        }
    }
}
