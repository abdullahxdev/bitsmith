# Bitsmith

Bitsmith is a Java Swing computer architecture visualizer for the MIPS ALU and datapath. It is built for teaching, demos, and lab presentations, with the goal of making the hardware flow visible instead of hidden inside a black box.

## What is new

The current build goes beyond a basic ALU demo and now includes:

- A polished two-tab interface with ALU Internals and MIPS Data Flow
- Cycle-aware execution labels for IF, ID, EX, MEM, and WB
- Hazard detection for RAW and load-use dependencies
- Visual hazard cues for stall, bubble, and forwarding suggestions
- A cleaner UI theme across the register file, ALU, memory, and execution log
- Instruction parsing and animated data movement for add, sub, and, or, slt, addi, lw, and sw

## What this project shows

Bitsmith presents two connected views:

- ALU Internals - an 8-bit ALU visualizer that shows addition, subtraction, logic, shifts, status flags, and the ALU control code.
- MIPS Data Flow - an instruction-level visualizer that animates register reads, ALU activity, memory access, writeback, and hazard detection.

The project is designed to be easy to demo live. You can run an instruction, watch the datapath react, and use the Open Internals button to jump into the ALU view for a closer look.

## Highlight features

- Clean Java Swing interface with two coordinated tabs
- ALU control signal display using textbook-style operation codes
- Ripple-carry adder animation for ADD, SUB, and SLT
- Bitwise and shift visualizations for AND, OR, XOR, NOR, SLL, SRL, and SRA
- MIPS-like register file and data memory panels
- Instruction parsing for add, sub, and, or, slt, addi, lw, and sw
- Step-based execution log for the data flow view
- Cycle labels for IF, ID, EX, MEM, and WB
- Hazard detection for RAW and load-use cases
- Visual cues for stall, bubble, and forwarding suggestions

## How to run

From the project root:

```bat
javac -d out src\aluviz\*.java
java -cp out aluviz.Main
```

If you prefer a packaged build, you can also use the existing build script if your environment supports it.

## Demo instructions

To see a hazard, run these instructions one after the other in the MIPS Data Flow tab:

```asm
lw  $t0, 0($t4)
add $t2, $t0, $t3
```

You should see the hazard message in the execution log, the related registers highlighted in the register file, and the hazard banner at the top of the instruction bar.

## Project layout

```text
src/aluviz/
├── Main.java
├── AppPanel.java
├── MainPanel.java
├── ALUCore.java
├── AdderSchematicPanel.java
├── RegisterViewPanel.java
├── DataFlowPanel.java
├── MachineState.java
├── ParsedInstruction.java
├── InstructionParser.java
├── ExecutionStep.java
├── InstructionExecutor.java
├── HazardDetector.java
├── RegisterFilePanel.java
├── MemoryPanel.java
├── MiniALUPanel.java
├── ExecutionLogPanel.java
├── WireAnimation.java
└── WiresOverlay.java
```

## Contributors

- abdullahxdev
- saadhtiwana
- ahmadmustafa02

## Notes

- The ALU is intentionally 8-bit so the bit-level animation stays readable on screen.
- The data flow visualizer is independent from MARS, but the logic was written to stay close to a MIPS teaching model.
- The UI is kept intentionally compact and classroom-friendly so it can be used in live presentations.

## Signed

Signed by saadhtiwana
