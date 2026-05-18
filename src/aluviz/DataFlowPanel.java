package aluviz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/**
 * Tab 2 — MIPS Data Flow Visualizer.
 * Layout:
 *   NORTH:   instruction bar
 *   CENTER:  JLayeredPane with content (3 panels) and a wires overlay on top
 *   SOUTH:   execution log
 */
public class DataFlowPanel extends JPanel {

    private static final Color BG = new Color(0xF4F7FB);
    private static final Color SURFACE = Color.WHITE;
    private static final Color SURFACE_BORDER = new Color(0xD8E0EA);
    private static final Color ACCENT = new Color(0x1F6FEB);
    private static final Color ACCENT_DARK = new Color(0x124A9C);
    private static final Color MUTED = new Color(0x5F6B7A);
    private static final Color HAZARD_BG = new Color(0xFFF2E9);
    private static final Color HAZARD_BORDER = new Color(0xF0A15D);

    private static final String[] EXAMPLES = {
        "add  $t0, $t1, $t2",
        "sub  $t0, $t1, $t2",
        "and  $t0, $t5, $t1",
        "or   $t0, $t5, $t1",
        "slt  $t0, $t2, $t1",
        "addi $t0, $t1, 5",
        "addi $t0, $t1, -3",
        "lw   $t0, 0($t4)",
        "lw   $t0, 4($t4)",
        "sw   $t1, 12($t4)"
    };

    private final MachineState state = new MachineState();
    private final RegisterFilePanel regFile;
    private final MiniALUPanel aluPanel;
    private final MemoryPanel memoryPanel;
    private final ExecutionLogPanel logPanel;
    private final PipelineStagePanel stagePanel;
    private final WiresOverlay wiresOverlay = new WiresOverlay();

    private final JComboBox<String> exampleCombo = new JComboBox<>(EXAMPLES);
    private final JTextField customField = new JTextField(EXAMPLES[0].trim(), 24);
    private final JButton runBtn   = new JButton("▶ Run");
    private final JButton stepBtn  = new JButton("⏭ Step");
    private final JButton resetBtn = new JButton("↻ Reset");
    private final JSlider speedSlider = new JSlider(120, 1400, 600);
    private final JLabel hazardBanner = new JLabel("Hazard status: idle");

    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel centerContent = new JPanel(new GridBagLayout());

    private AppPanel appPanel;

    // Animation state machine
    private List<ExecutionStep> pendingSteps;
    private int stepCursor;
    private ParsedInstruction currentInstr;
    private ParsedInstruction lastCompletedInstruction = null;
    private HazardDetector.Result currentHazard;
    private Timer frameTimer;
    private WireAnimation currentAnim;
    private boolean stepMode = false;
    private boolean busy = false;

    public DataFlowPanel() {
        super(new BorderLayout(8, 8));
        setBackground(BG);
        setBorder(new EmptyBorder(8, 10, 8, 10));

        regFile     = new RegisterFilePanel(state);
        aluPanel    = new MiniALUPanel();
        memoryPanel = new MemoryPanel(state);
        logPanel    = new ExecutionLogPanel();
        stagePanel  = new PipelineStagePanel();

        add(buildTopSection(), BorderLayout.NORTH);
        add(buildLayeredCenter(),  BorderLayout.CENTER);
        add(logPanel,              BorderLayout.SOUTH);

        // Wire up actions
        exampleCombo.addActionListener(e -> {
            String sel = (String) exampleCombo.getSelectedItem();
            if (sel != null) customField.setText(sel.trim());
        });
        runBtn.addActionListener(this::onRun);
        stepBtn.addActionListener(this::onStep);
        resetBtn.addActionListener(this::onReset);

        aluPanel.onOpenInternals(e -> {
            if (appPanel != null) appPanel.showALUInternals();
        });

        // Single frame timer for all animations (~60 FPS)
        frameTimer = new Timer(16, e -> tick());
        frameTimer.start();
    }

    public void setAppPanel(AppPanel p) {
        this.appPanel = p;
    }

    /* ─────────────  Layout  ───────────── */

    private JComponent buildInstructionBar() {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setBackground(BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SURFACE_BORDER),
            new EmptyBorder(10, 12, 10, 12)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Instruction Control");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(ACCENT_DARK);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 4; bar.add(title, c);

        JLabel subtitle = new JLabel("Run two dependent instructions to surface RAW hazard detection and mitigation cues.");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        subtitle.setForeground(MUTED);
        c.gridy = 1; bar.add(subtitle, c);

        c.gridwidth = 1;
        c.gridy = 2; c.gridx = 0; bar.add(new JLabel("Example:"), c);
        c.gridx = 1; bar.add(exampleCombo, c);
        c.gridx = 2; bar.add(new JLabel("Or type:"), c);
        c.gridx = 3; bar.add(customField, c);

        c.gridx = 0; c.gridy = 3; bar.add(new JLabel("Speed (slower → faster):"), c);
        c.gridx = 1; c.gridwidth = 1; bar.add(speedSlider, c);
        c.gridx = 2; bar.add(runBtn, c);
        c.gridx = 3;
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rightBtns.setBackground(BG);
        rightBtns.add(stepBtn);
        rightBtns.add(resetBtn);
        bar.add(rightBtns, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 4;
        hazardBanner.setOpaque(true);
        hazardBanner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(HAZARD_BORDER),
            new EmptyBorder(6, 10, 6, 10)));
        hazardBanner.setBackground(new Color(0xF7FAFF));
        hazardBanner.setForeground(ACCENT_DARK);
        hazardBanner.setFont(hazardBanner.getFont().deriveFont(Font.BOLD, 11f));
        bar.add(hazardBanner, c);

        return bar;
    }

    private JComponent buildTopSection() {
        JPanel top = new JPanel();
        top.setBackground(BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildInstructionBar());
        top.add(Box.createVerticalStrut(8));
        top.add(stagePanel);
        return top;
    }

    private JComponent buildLayeredCenter() {
        centerContent.setBackground(BG);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.gridx = 0; c.weightx = 0.45; centerContent.add(regFile, c);
        c.gridx = 1; c.weightx = 0.25; centerContent.add(aluPanel, c);
        c.gridx = 2; c.weightx = 0.30; centerContent.add(memoryPanel, c);

        layeredPane.add(centerContent, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(wiresOverlay,  JLayeredPane.PALETTE_LAYER);
        layeredPane.setPreferredSize(new Dimension(1140, 380));
        layeredPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                centerContent.setBounds(0, 0, w, h);
                wiresOverlay.setBounds(0, 0, w, h);
                centerContent.revalidate();
            }
        });

        return layeredPane;
    }

    /* ─────────────  Run / Step / Reset  ───────────── */

    private void onRun(ActionEvent e) {
        if (busy) return;
        stepMode = false;
        startInstruction();
    }

    private void onStep(ActionEvent e) {
        if (busy) {
            advanceToNextStep();
            return;
        }
        stepMode = true;
        startInstruction();
    }

    private void onReset(ActionEvent e) {
        cancelAnimation();
        state.reset();
        regFile.refreshAll();
        regFile.clearHighlights();
        memoryPanel.refreshRows();
        memoryPanel.clearHighlights();
        aluPanel.clearResult();
        aluPanel.setOperation(null);
        aluPanel.setInputs(null, null);
        aluPanel.setActive(false);
        wiresOverlay.clearActive();
        logPanel.clear();
    }

    private void startInstruction() {
        try {
            currentInstr = InstructionParser.parse(customField.getText());
            // Detect hazards with previously completed instruction (if any)
            currentHazard = HazardDetector.analyze(lastCompletedInstruction, currentInstr);
            if (currentHazard.type != HazardDetector.HazardType.NONE) {
                // Log and visually mark hazard
                logPanel.appendHazard(currentHazard.type + ": " + currentHazard.suggestion + "  Suggestion: stall or forwarding.");
                // highlight affected registers in register file
                int[] readers = currentHazard.readers == null ? new int[0] : currentHazard.readers;
                regFile.markHazard(currentHazard.writer, readers);
                setHazardBanner("Hazard detected: " + currentHazard.type + " on " + MachineState.regName(currentHazard.writer)
                    + " -> consider stall, bubble, or forwarding.", true);
            } else {
                regFile.clearHazard();
                setHazardBanner("Hazard status: no dependency detected for this instruction pair.", false);
            }

            pendingSteps = InstructionExecutor.plan(currentInstr, state, currentHazard).steps;
            stagePanel.setPlan(currentInstr, currentHazard, pendingSteps);
            stepCursor = 0;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not run: " + ex.getMessage(),
                "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        regFile.clearHighlights();
        memoryPanel.clearHighlights();
        aluPanel.clearResult();
        aluPanel.setOperation(currentInstr.aluOp);
        aluPanel.setInputs(null, null);
        logPanel.appendInstructionHeader(currentInstr.toString());
        busy = true;
        beginCurrentStep();
    }

    private void setHazardBanner(String text, boolean isHazard) {
        hazardBanner.setText(text);
        if (isHazard) {
            hazardBanner.setBackground(HAZARD_BG);
            hazardBanner.setForeground(new Color(0x8A3B00));
        } else {
            hazardBanner.setBackground(new Color(0xEEF5FF));
            hazardBanner.setForeground(ACCENT_DARK);
        }
        hazardBanner.repaint();
    }

    private void advanceToNextStep() {
        if (pendingSteps == null) return;
        stepCursor++;
        if (stepCursor >= pendingSteps.size()) {
            finishInstruction();
        } else {
            beginCurrentStep();
        }
    }

    private void beginCurrentStep() {
        ExecutionStep step = pendingSteps.get(stepCursor);
        stagePanel.setActiveStep(step.type);
        if (step.type == ExecutionStep.Type.IF || step.type == ExecutionStep.Type.ID || step.type == ExecutionStep.Type.EX
            || step.type == ExecutionStep.Type.MEM || step.type == ExecutionStep.Type.WB
            || step.type == ExecutionStep.Type.STALL || step.type == ExecutionStep.Type.BUBBLE
            || step.type == ExecutionStep.Type.FORWARD) {
            logPanel.appendCycleNote(step.description);
        } else {
            logPanel.appendStep(step);
        }

        Point src = null, dst = null;
        Color color = new Color(0xFFC107);
        String label = "0x" + String.format("%08X", step.value);

        switch (step.type) {
            case READ_REG: {
                regFile.markRead(step.regIndex, step.aluPort);
                src = pointInOverlay(regFile, regFileOutputPoint());
                if ("A".equalsIgnoreCase(step.aluPort)) {
                    dst = pointInOverlay(aluPanel, aluInputAPoint());
                } else if ("B".equalsIgnoreCase(step.aluPort)) {
                    dst = pointInOverlay(aluPanel, aluInputBPoint());
                } else {
                    // "data" port for sw — goes around the ALU toward memory
                    dst = pointInOverlay(memoryPanel, memoryInputPoint());
                    color = new Color(0xC2185B);
                }
                break;
            }
            case IF:
            case ID:
            case EX:
            case MEM:
            case WB:
            case STALL:
            case BUBBLE:
            case FORWARD: {
                Color cycleColor = new Color(0x1976D2);
                if (step.type == ExecutionStep.Type.STALL) cycleColor = new Color(0xC62828);
                if (step.type == ExecutionStep.Type.BUBBLE) cycleColor = new Color(0x6D4C41);
                if (step.type == ExecutionStep.Type.FORWARD) cycleColor = new Color(0x2E7D32);
                currentAnim = new WireAnimation(new Point(0, 0), new Point(0, 0), step.type.name(), cycleColor, currentDuration());
                currentAnim.start();
                wiresOverlay.clearActive();
                break;
            }
            case IMMEDIATE: {
                Point srcLocal = new Point(0, aluPanel.getHeight() / 2 + 30);
                src = pointInOverlay(aluPanel, srcLocal);
                src.x -= 40;
                dst = pointInOverlay(aluPanel, aluInputBPoint());
                label = "imm 0x" + String.format("%X", step.value);
                color = new Color(0x7B1FA2);
                break;
            }
            case ALU_COMPUTE: {
                aluPanel.setActive(true);
                aluPanel.setInputs(step.aluA, step.aluB);
                ALUCore.Result r = ALUCore.compute(step.aluOp, step.aluA, step.aluB);
                aluPanel.setResult(r.result, r.zero, r.negative, r.carryOut, r.overflow);
                // No travel — just a pause for the ALU pulse
                startPulse();
                return;
            }
            case MEM_READ: {
                memoryPanel.markRead(step.address);
                src = pointInOverlay(aluPanel, aluOutputPoint());
                dst = pointInOverlay(memoryPanel, memoryInputPoint());
                label = "addr 0x" + String.format("%X", step.address);
                color = new Color(0x388E3C);
                break;
            }
            case MEM_WRITE: {
                memoryPanel.markWrite(step.address, step.value);
                src = pointInOverlay(aluPanel, aluOutputPoint());
                dst = pointInOverlay(memoryPanel, memoryInputPoint());
                label = "addr 0x" + String.format("%X", step.address);
                color = new Color(0xC2185B);
                break;
            }
            case WRITEBACK: {
                Point srcRaw = "MEM".equals(step.writebackSource)
                    ? pointInOverlay(memoryPanel, memoryOutputPoint())
                    : pointInOverlay(aluPanel, aluOutputPoint());
                src = srcRaw;
                dst = pointInOverlay(regFile, regFileWritePoint(step.regIndex));
                color = new Color(0xF57C00);
                regFile.markWrite(step.regIndex, step.value);
                break;
            }
        }

        if (src != null && dst != null) {
            currentAnim = new WireAnimation(src, dst, label, color, currentDuration());
            currentAnim.start();
            wiresOverlay.setActive(currentAnim);
        } else {
            advanceToNextStep();
        }
    }

    private void startPulse() {
        currentAnim = new WireAnimation(new Point(0, 0), new Point(0, 0), "",
            new Color(0xF57C00), currentDuration());
        currentAnim.start();
        wiresOverlay.clearActive();
    }

    private void tick() {
        if (!busy || currentAnim == null) return;
        if (currentAnim.isDone()) {
            wiresOverlay.clearActive();
            // Step's effects are applied in beginCurrentStep before the animation runs.
            // After ALU_COMPUTE we want the ALU glow to fade once we move on.
            ExecutionStep s = pendingSteps.get(stepCursor);
            if (s.type == ExecutionStep.Type.ALU_COMPUTE) {
                // Keep glow until next non-ALU step starts
            }
            if (stepMode) {
                // Wait for user to hit Step again
                return;
            }
            advanceToNextStep();
        } else {
            wiresOverlay.repaint();
        }
    }

    private void finishInstruction() {
        busy = false;
        aluPanel.setActive(false);
        regFile.clearHighlights();
        memoryPanel.clearHighlights();
        wiresOverlay.clearActive();
        regFile.refreshAll();
        memoryPanel.refreshRows();
        regFile.clearHazard();
        stagePanel.clearActive();
        // remember last completed instruction for hazard detection with next instruction
        lastCompletedInstruction = currentInstr;
    }

    private void cancelAnimation() {
        busy = false;
        stepCursor = 0;
        pendingSteps = null;
        currentAnim = null;
        stagePanel.clearActive();
    }

    private int currentDuration() {
        // slider: 120 (slow, big duration) .. 1400 (fast, small duration)
        // Translate: low slider → long duration. duration ~ 1500 - slider
        return Math.max(120, 1500 - speedSlider.getValue());
    }

    /* ─────────────  Connection point helpers (panel-local)  ───────────── */

    private Point regFileOutputPoint() {
        return new Point(regFile.getWidth(), regFile.getHeight() / 2);
    }

    private Point regFileWritePoint(int regIndex) {
        return new Point(regFile.getWidth(), regFile.getHeight() / 2 + 20);
    }

    private Point aluInputAPoint() {
        return new Point(0, aluPanel.getHeight() / 2 - 30);
    }

    private Point aluInputBPoint() {
        return new Point(0, aluPanel.getHeight() / 2 + 10);
    }

    private Point aluOutputPoint() {
        return new Point(aluPanel.getWidth(), aluPanel.getHeight() / 2);
    }

    private Point memoryInputPoint() {
        return new Point(0, memoryPanel.getHeight() / 2);
    }

    private Point memoryOutputPoint() {
        return new Point(0, memoryPanel.getHeight() / 2 + 20);
    }

    /** Convert a component-local point to the wires overlay coordinate space. */
    private Point pointInOverlay(JComponent comp, Point localPoint) {
        Point inCenter = SwingUtilities.convertPoint(comp, localPoint, centerContent);
        // centerContent and overlay share the same origin (both at 0,0 in layered pane)
        return inCenter;
    }
}
