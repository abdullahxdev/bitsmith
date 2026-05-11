package aluviz;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;

public class MainPanel extends JPanel {

    private final JTextField fieldA = new JTextField("0b00001101", 12);
    private final JTextField fieldB = new JTextField("0b00000110", 12);
    private final JComboBox<ALUCore.Op> opCombo = new JComboBox<>(ALUCore.Op.values());
    private final JSlider speedSlider = new JSlider(50, 800, 250);
    private final JButton runBtn = new JButton("Compute & Animate");

    private final JLabel resultBin = new JLabel("--------");
    private final JLabel resultDec = new JLabel("--");
    private final JLabel resultHex = new JLabel("0x--");

    private final FlagLamp lampZ = new FlagLamp("Z", "Zero");
    private final FlagLamp lampN = new FlagLamp("N", "Negative");
    private final FlagLamp lampC = new FlagLamp("C", "Carry");
    private final FlagLamp lampV = new FlagLamp("V", "Overflow");

    private final JLabel controlBits = new JLabel("----");
    private final JLabel controlOpName = new JLabel("(none)");

    private final AdderSchematicPanel adderPanel = new AdderSchematicPanel();
    private final RegisterViewPanel registerPanel = new RegisterViewPanel();
    private final JPanel vizCard;
    private final CardLayout vizLayout = new CardLayout();

    public MainPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTop(), BorderLayout.NORTH);

        vizCard = new JPanel(vizLayout);
        vizCard.add(new JScrollPane(adderPanel), "adder");
        vizCard.add(new JScrollPane(registerPanel), "register");
        vizCard.setBorder(BorderFactory.createTitledBorder("Operation Visualization"));
        add(vizCard, BorderLayout.CENTER);

        add(buildBottom(), BorderLayout.SOUTH);

        runBtn.addActionListener(this::onRun);
        opCombo.addActionListener(e -> updateControlPreview());
        updateControlPreview();
    }

    private JComponent buildTop() {
        JPanel input = new JPanel(new GridBagLayout());
        input.setBorder(BorderFactory.createTitledBorder("Operands & Operation"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; input.add(new JLabel("A:"), c);
        c.gridx = 1; input.add(fieldA, c);
        c.gridx = 2; input.add(new JLabel("(decimal, 0x hex, or 0b binary — 8-bit)"), c);

        c.gridx = 0; c.gridy = 1; input.add(new JLabel("B:"), c);
        c.gridx = 1; input.add(fieldB, c);
        c.gridx = 2; input.add(new JLabel("(for shifts, B is the shift amount)"), c);

        c.gridx = 0; c.gridy = 2; input.add(new JLabel("Operation:"), c);
        c.gridx = 1; input.add(opCombo, c);

        c.gridx = 0; c.gridy = 3; input.add(new JLabel("Animation speed:"), c);
        c.gridx = 1; input.add(speedSlider, c);
        c.gridx = 2; input.add(new JLabel("(left = slow, right = fast)"), c);

        c.gridx = 1; c.gridy = 4; input.add(runBtn, c);

        return input;
    }

    private JComponent buildBottom() {
        JPanel south = new JPanel(new GridLayout(1, 3, 8, 8));

        JPanel resultPanel = new JPanel(new GridBagLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Result"));
        GridBagConstraints rc = new GridBagConstraints();
        rc.insets = new Insets(3, 6, 3, 6); rc.anchor = GridBagConstraints.WEST;
        rc.gridx = 0; rc.gridy = 0; resultPanel.add(new JLabel("Binary:"), rc);
        rc.gridx = 1; resultBin.setFont(monospace(16)); resultPanel.add(resultBin, rc);
        rc.gridx = 0; rc.gridy = 1; resultPanel.add(new JLabel("Decimal (signed):"), rc);
        rc.gridx = 1; resultDec.setFont(monospace(14)); resultPanel.add(resultDec, rc);
        rc.gridx = 0; rc.gridy = 2; resultPanel.add(new JLabel("Hex:"), rc);
        rc.gridx = 1; resultHex.setFont(monospace(14)); resultPanel.add(resultHex, rc);
        south.add(resultPanel);

        JPanel flags = new JPanel(new GridLayout(1, 4, 6, 6));
        flags.setBorder(BorderFactory.createTitledBorder("Status Flags"));
        flags.add(lampZ); flags.add(lampN); flags.add(lampC); flags.add(lampV);
        south.add(flags);

        JPanel ctl = new JPanel(new GridBagLayout());
        ctl.setBorder(BorderFactory.createTitledBorder("ALU Control Signal"));
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(3, 6, 3, 6); cc.anchor = GridBagConstraints.WEST;
        cc.gridx = 0; cc.gridy = 0; ctl.add(new JLabel("4-bit ALU Op:"), cc);
        cc.gridx = 1; controlBits.setFont(monospace(20)); controlBits.setForeground(new Color(0, 100, 180));
        ctl.add(controlBits, cc);
        cc.gridx = 0; cc.gridy = 1; ctl.add(new JLabel("Operation:"), cc);
        cc.gridx = 1; controlOpName.setFont(monospace(14)); ctl.add(controlOpName, cc);
        cc.gridx = 0; cc.gridy = 2; cc.gridwidth = 2;
        JLabel hint = new JLabel("<html><i>In MIPS: generated by the ALU Control unit from<br>2-bit ALUOp (from main control) + 6-bit funct field</i></html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        ctl.add(hint, cc);
        south.add(ctl);

        return south;
    }

    private void updateControlPreview() {
        ALUCore.Op op = (ALUCore.Op) opCombo.getSelectedItem();
        if (op == null) return;
        controlBits.setText(op.controlBits);
        controlOpName.setText(op.label);
    }

    private void onRun(ActionEvent e) {
        int a, b;
        try {
            a = ALUCore.parseOperand(fieldA.getText());
            b = ALUCore.parseOperand(fieldB.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not parse operand. Use decimal, 0xHEX, or 0bBIN.",
                "Bad input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ALUCore.Op op = (ALUCore.Op) opCombo.getSelectedItem();
        ALUCore.Result r = ALUCore.compute(op, a, b);
        showResult(r);

        if (op == ALUCore.Op.ADD || op == ALUCore.Op.SUB || op == ALUCore.Op.SLT) {
            vizLayout.show(vizCard, "adder");
            adderPanel.animate(r, speedSlider.getValue());
        } else {
            vizLayout.show(vizCard, "register");
            registerPanel.animate(r, speedSlider.getValue());
        }
    }

    private void showResult(ALUCore.Result r) {
        resultBin.setText(ALUCore.toBinary(r.result));
        int signedDec = r.result;
        if (((r.result >> (ALUCore.WIDTH - 1)) & 1) == 1) {
            signedDec = r.result - (1 << ALUCore.WIDTH);
        }
        resultDec.setText(r.result + "  (signed: " + signedDec + ")");
        resultHex.setText(String.format("0x%02X", r.result));
        lampZ.setOn(r.zero);
        lampN.setOn(r.negative);
        lampC.setOn(r.carryOut);
        lampV.setOn(r.overflow);
        controlBits.setText(r.op.controlBits);
        controlOpName.setText(r.op.label);
    }

    static Font monospace(int size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    static class FlagLamp extends JPanel {
        private final JLabel name = new JLabel();
        private final JLabel desc = new JLabel();
        private boolean on;
        FlagLamp(String n, String d) {
            super(new BorderLayout());
            name.setText(n);
            name.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
            name.setHorizontalAlignment(SwingConstants.CENTER);
            desc.setText(d);
            desc.setHorizontalAlignment(SwingConstants.CENTER);
            desc.setFont(desc.getFont().deriveFont(11f));
            add(name, BorderLayout.CENTER);
            add(desc, BorderLayout.SOUTH);
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            setOn(false);
        }
        void setOn(boolean v) {
            this.on = v;
            if (v) {
                setBackground(new Color(255, 220, 80));
                name.setForeground(new Color(120, 60, 0));
            } else {
                setBackground(new Color(240, 240, 240));
                name.setForeground(new Color(160, 160, 160));
            }
            repaint();
        }
    }
}
