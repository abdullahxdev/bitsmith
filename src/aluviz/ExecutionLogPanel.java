package aluviz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Scrolling textual log of execution steps for the Data Flow tab.
 * Each step the orchestrator runs gets one line here.
 */
public class ExecutionLogPanel extends JPanel {

    private static final Color BG       = new Color(0xFAFAFA);
    private static final Color LOG_BG   = Color.WHITE;
    private static final Color LOG_FG   = new Color(0x212121);
    private static final Color MUTED    = new Color(0x9E9E9E);

    private final JTextArea log = new JTextArea();
    private int stepCounter = 0;
    private int instrCounter = 0;

    public ExecutionLogPanel() {
        super(new BorderLayout(0, 4));
        setBackground(BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Execution Log"),
            new EmptyBorder(4, 8, 4, 8)));

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setForeground(LOG_FG);
        log.setBackground(LOG_BG);
        log.setLineWrap(false);

        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE0E0E0)));
        scroll.setPreferredSize(new Dimension(800, 120));
        add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("Each instruction is split into atomic steps. Click Run or Step.");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(MUTED);
        hint.setBorder(new EmptyBorder(2, 4, 0, 4));
        add(hint, BorderLayout.SOUTH);
    }

    public void appendInstructionHeader(String instr) {
        instrCounter++;
        stepCounter = 0;
        log.append("\n──────────────────────────────────────────────────\n");
        log.append("Instruction " + instrCounter + ":  " + instr + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    public void appendStep(ExecutionStep step) {
        stepCounter++;
        String prefix = "  [" + stepCounter + "] ";
        log.append(prefix + step.description + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    public void clear() {
        log.setText("");
        stepCounter = 0;
        instrCounter = 0;
    }
}
