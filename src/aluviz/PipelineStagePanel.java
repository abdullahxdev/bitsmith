package aluviz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact pipeline stage strip for the Data Flow tab.
 * Shows IF, ID, EX, MEM, WB and any inserted stall/bubble/forward cues.
 */
public class PipelineStagePanel extends JPanel {

    private static final Color BG = new Color(0xF4F7FB);
    private static final Color BORDER = new Color(0xD8E0EA);
    private static final Color ACTIVE_BG = new Color(0xDCEBFF);
    private static final Color ACTIVE_BORDER = new Color(0x1F6FEB);
    private static final Color TEXT = new Color(0x1F2937);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color STALL_BG = new Color(0xFFF2E9);
    private static final Color STALL_BORDER = new Color(0xE07A24);
    private static final Color BUBBLE_BG = new Color(0xF5E8D7);
    private static final Color BUBBLE_BORDER = new Color(0xB66A2B);
    private static final Color FORWARD_BG = new Color(0xE7F7EC);
    private static final Color FORWARD_BORDER = new Color(0x2E7D32);

    private final JLabel title = new JLabel("Pipeline Stage View");
    private final JLabel note = new JLabel("Idle");
    private final Map<String, StageBadge> stages = new LinkedHashMap<>();

    public PipelineStagePanel() {
        super(new BorderLayout(8, 8));
        setBackground(BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(8, 12, 8, 12)));

        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(new Color(0x124A9C));

        note.setFont(note.getFont().deriveFont(Font.PLAIN, 11f));
        note.setForeground(MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(note, BorderLayout.EAST);

        JPanel row = new JPanel(new GridLayout(1, 8, 6, 0));
        row.setOpaque(false);
        addStage(row, "IF");
        addStage(row, "ID");
        addStage(row, "EX");
        addStage(row, "MEM");
        addStage(row, "WB");
        addStage(row, "STALL");
        addStage(row, "BUBBLE");
        addStage(row, "FORWARD");

        add(header, BorderLayout.NORTH);
        add(row, BorderLayout.CENTER);
    }

    private void addStage(JPanel row, String name) {
        StageBadge badge = new StageBadge(name);
        stages.put(name, badge);
        row.add(badge);
    }

    public void setPlan(ParsedInstruction instr, HazardDetector.Result hazard, List<ExecutionStep> plannedSteps) {
        clearActive();
        StringBuilder msg = new StringBuilder();
        msg.append(instr == null ? "No instruction" : instr.toString());
        if (hazard != null && hazard.type != HazardDetector.HazardType.NONE) {
            msg.append(" | ").append(hazard.type);
            if (hazard.type == HazardDetector.HazardType.LOAD_USE) {
                msg.append(" detected: stall inserted automatically");
            } else {
                msg.append(" detected: forwarding suggested");
            }
        } else {
            msg.append(" | no hazard");
        }
        note.setText(msg.toString());

        if (plannedSteps == null) return;
        for (ExecutionStep step : plannedSteps) {
            if (step.type == ExecutionStep.Type.STALL) stages.get("STALL").setVisible(true);
            if (step.type == ExecutionStep.Type.BUBBLE) stages.get("BUBBLE").setVisible(true);
            if (step.type == ExecutionStep.Type.FORWARD) stages.get("FORWARD").setVisible(true);
        }
    }

    public void setActiveStep(ExecutionStep.Type type) {
        clearHighlightOnly();
        String key = mapStage(type);
        if (key != null) {
            stages.get(key).setActive(true);
            note.setText("Active stage: " + key);
        }
    }

    public void clearActive() {
        for (StageBadge badge : stages.values()) {
            badge.setActive(false);
            badge.setVisible(true);
        }
        note.setText("Idle");
    }

    private void clearHighlightOnly() {
        for (StageBadge badge : stages.values()) {
            badge.setActive(false);
        }
    }

    private String mapStage(ExecutionStep.Type type) {
        if (type == null) return null;
        switch (type) {
            case IF: return "IF";
            case ID: return "ID";
            case EX: return "EX";
            case MEM: return "MEM";
            case WB: return "WB";
            case STALL: return "STALL";
            case BUBBLE: return "BUBBLE";
            case FORWARD: return "FORWARD";
            default: return null;
        }
    }

    private static class StageBadge extends JPanel {
        private final JLabel label = new JLabel();
        private final String name;
        private boolean active;

        StageBadge(String name) {
            super(new BorderLayout());
            this.name = name;
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(BORDER));
            setPreferredSize(new Dimension(96, 34));
            label.setText(name);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
            label.setForeground(TEXT);
            add(label, BorderLayout.CENTER);
            refresh();
        }

        void setActive(boolean active) {
            this.active = active;
            refresh();
        }

        private void refresh() {
            Color bg = Color.WHITE;
            Color border = BORDER;
            Color fg = TEXT;
            if (active) {
                bg = ACTIVE_BG;
                border = ACTIVE_BORDER;
                fg = new Color(0x124A9C);
            } else if ("STALL".equals(name)) {
                bg = STALL_BG;
                border = STALL_BORDER;
                fg = new Color(0x8A3B00);
            } else if ("BUBBLE".equals(name)) {
                bg = BUBBLE_BG;
                border = BUBBLE_BORDER;
                fg = new Color(0x7A4A18);
            } else if ("FORWARD".equals(name)) {
                bg = FORWARD_BG;
                border = FORWARD_BORDER;
                fg = new Color(0x1B5E20);
            }
            setBackground(bg);
            setBorder(BorderFactory.createLineBorder(border));
            label.setForeground(fg);
        }
    }
}