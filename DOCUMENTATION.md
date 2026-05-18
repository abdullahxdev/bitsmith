# Bitsmith Project Documentation

## Executive Summary

Bitsmith is a Java Swing computer architecture visualizer focused on the MIPS ALU and datapath. It was built as a final semester Computer Architecture project to make the behavior of a processor visible instead of abstract. The project now includes two coordinated views:

- ALU Internals, which shows how arithmetic and logic operations are executed at the bit and gate level.
- MIPS Data Flow, which shows register access, ALU execution, memory access, writeback, hazard detection, and cycle labels.

The application is designed for live demonstrations, class presentations, and viva discussion. It is report-friendly because the code is organized into clear modules that map directly to Computer Architecture concepts.

## 1. Project Purpose

The main goal of Bitsmith is to teach how a MIPS-like CPU works internally. A typical textbook diagram shows the ALU, register file, memory, and control unit as blocks. Bitsmith expands those blocks into an interactive visual model.

The project helps answer questions such as:

- What happens inside the ALU when add or sub is executed?
- How does a ripple-carry adder propagate carry from bit 0 to bit 7?
- How are load and store instructions translated into register reads, address calculation, memory access, and writeback?
- What is a RAW hazard and why does it cause a stall?
- What does forwarding mean in a pipeline?

This is why the project is a good fit for Computer Architecture coursework. It connects the theoretical datapath diagrams from the textbook to an interactive software simulation.

## 2. Main Features

### 2.1 ALU Internals

The ALU tab provides an 8-bit ALU visualizer with the following capabilities:

- Binary, decimal, and hexadecimal operand input
- ADD, SUB, AND, OR, XOR, NOR, SLT, SLL, SRL, and SRA
- Ripple-carry adder animation
- Bit-by-bit register style visualizations for logic and shift operations
- Status flags: Zero, Negative, Carry, Overflow
- ALU control code display using textbook style 4-bit operation codes

### 2.2 MIPS Data Flow

The Data Flow tab provides a higher level CPU simulation view:

- Instruction parsing for add, sub, and, or, slt, addi, lw, sw
- Register file visualization for all 32 registers
- Data memory view with address, hex value, and ASCII column
- ALU box with live inputs and outputs
- Execution log with step-by-step trace
- Animated wires and moving value bubbles
- Pipeline stage strip showing IF, ID, EX, MEM, WB
- Hazard detection for RAW and load-use dependencies
- Automatic stall insertion for load-use hazards
- Visual cues for stall, bubble, and forwarding suggestions

### 2.3 UI and Presentation Improvements

The project also includes a cleaner visual style:

- Softer background colors
- Better section titles and borders
- Hazard banner in the instruction area
- Improved readability of register, ALU, memory, and log panels

## 3. Computer Architecture Concepts Covered

Bitsmith is not just a user interface. Each module maps to specific Computer Architecture concepts.

### 3.1 Number Systems

The application accepts and displays values in multiple formats:

- Binary
- Decimal
- Hexadecimal

This supports lessons on base conversion and machine representation.

### 3.2 Two's Complement

Negative numbers are represented using two's complement. The project demonstrates this through subtraction, signed decimal display, and arithmetic right shift behavior.

### 3.3 Logic Gates

The ALU shows the use of:

- AND
- OR
- XOR
- NOR

These are the building blocks of arithmetic and logic operations.

### 3.4 Full Adder and Ripple-Carry Adder

The ADD and SUB operations are built from a ripple-carry adder model. This shows:

- Sum calculation
- Carry propagation
- Carry out from the most significant bit
- Why subtraction can be implemented as addition with inverted B and carry-in 1

### 3.5 Status Flags

The tool shows the standard flags used by many instructions and branches:

- Z for zero
- N for negative
- C for carry out
- V for signed overflow

### 3.6 Shift Operations

The project includes all three common shift styles:

- SLL, shift left logical
- SRL, shift right logical
- SRA, shift right arithmetic

### 3.7 ALU Control

The ALU control panel shows how a 4-bit control signal selects the operation. This is directly tied to the MIPS ALU control unit concept.

### 3.8 Datapath and Execution Flow

The Data Flow tab demonstrates how an instruction moves through the register file, ALU, memory, and writeback path.

### 3.9 Hazards and Pipelining

The project now includes pipeline-oriented concepts:

- IF, ID, EX, MEM, WB stage labels
- RAW hazard detection
- Load-use hazard detection
- Stall and bubble insertion
- Forwarding suggestions

## 4. System Architecture

Bitsmith can be divided into four layers.

### 4.1 User Interface Layer

This is the Swing layer that the user sees. It includes the main window, tabs, panels, buttons, sliders, labels, and visual components.

### 4.2 Simulation Core Layer

This layer contains the actual logic for the ALU and instruction execution. It is independent of Swing so the code is easier to reason about and test.

### 4.3 Pipeline and Hazard Layer

This layer adds cycle labels, stage views, and hazard detection to the data flow simulation.

### 4.4 Machine State Layer

This layer stores the simulated register file and memory so that instructions can read and write data.

## 5. File-by-File Documentation

This section is written so it can be used directly in a project report.

### 5.1 `src/aluviz/Main.java`

Entry point of the application.

Responsibilities:

- Sets the Swing look and feel
- Creates the main JFrame
- Places `AppPanel` inside the frame
- Displays the application window

### 5.2 `src/aluviz/AppPanel.java`

Root content panel of the application.

Responsibilities:

- Hosts the top-level tab layout
- Contains the ALU Internals tab
- Contains the MIPS Data Flow tab
- Provides methods for switching between tabs

### 5.3 `src/aluviz/MainPanel.java`

Main panel for ALU Internals.

Responsibilities:

- Accepts two operands and an operation
- Calls the ALU core logic
- Displays the result in binary, decimal, and hex
- Displays the four status flags
- Chooses between schematic and register style visualizations
- Shows the 4-bit ALU control code

### 5.4 `src/aluviz/ALUCore.java`

The core arithmetic and logic engine.

Responsibilities:

- Defines supported ALU operations
- Computes arithmetic and logic results
- Implements ripple-carry addition
- Handles subtraction using two's complement
- Computes flags
- Parses operands from decimal, hex, binary, and negative forms
- Produces bit-level adder trace data for animation

### 5.5 `src/aluviz/AdderSchematicPanel.java`

Visualizes ADD, SUB, and SLT.

Responsibilities:

- Draws 8 full adder cells
- Animates carry propagation
- Shows the internal gates used by each full adder
- Displays the final sum and carry out

### 5.6 `src/aluviz/RegisterViewPanel.java`

Visualizes bitwise and shift operations.

Responsibilities:

- Draws bit rows for A, B, and Result
- Animates bit-by-bit operation
- Shows gate boxes for logic operations
- Shows direction of shifts
- Explains arithmetic right shift sign handling

### 5.7 `src/aluviz/MachineState.java`

Represents the simulated CPU state.

Responsibilities:

- Stores 32 registers
- Stores sparse memory using a map
- Provides reset values for demo instructions
- Provides register name and index conversion helpers
- Enforces `$zero` as a hardwired zero register

### 5.8 `src/aluviz/ParsedInstruction.java`

Data class for parsed assembly instructions.

Responsibilities:

- Stores instruction kind
- Stores mnemonic
- Stores source and destination registers
- Stores immediate values
- Provides a human readable string representation

### 5.9 `src/aluviz/InstructionParser.java`

Parses the instruction text entered by the user.

Responsibilities:

- Parses R-type instructions: add, sub, and, or, slt
- Parses addi
- Parses lw and sw using offset(base) syntax
- Converts register names to indices
- Converts immediates from decimal and hex

### 5.10 `src/aluviz/ExecutionStep.java`

Represents one atomic visual step.

Responsibilities:

- Defines step types such as register read, immediate, ALU compute, memory read, memory write, writeback
- Defines cycle steps such as IF, ID, EX, MEM, WB
- Defines hazard related steps such as STALL, BUBBLE, FORWARD
- Stores all values needed by the UI for animation and logging

### 5.11 `src/aluviz/HazardDetector.java`

Analyzes dependencies between two instructions.

Responsibilities:

- Detects RAW hazards
- Detects load-use hazards
- Returns the writer register and reading registers
- Returns a short suggestion for the UI and log

### 5.12 `src/aluviz/InstructionExecutor.java`

Builds the instruction execution sequence.

Responsibilities:

- Turns a parsed instruction into a list of `ExecutionStep` objects
- Adds cycle labels for IF, ID, EX, MEM, WB
- Inserts stall and bubble steps for load-use hazards
- Adds forwarding cue steps for other RAW hazards
- Mutates machine state during execution

### 5.13 `src/aluviz/DataFlowPanel.java`

Orchestrates the Data Flow tab.

Responsibilities:

- Hosts the instruction input bar
- Hosts the register file, ALU, and memory panels
- Hosts the execution log
- Hosts the pipeline stage view
- Manages the animation timer
- Calls the parser, hazard detector, and executor
- Shows hazard banner text
- Synchronizes stage highlight and value animations

### 5.14 `src/aluviz/PipelineStagePanel.java`

Shows the pipeline stage strip.

Responsibilities:

- Displays IF, ID, EX, MEM, WB
- Displays STALL, BUBBLE, and FORWARD cues
- Highlights the current active stage
- Shows the current instruction and hazard summary

### 5.15 `src/aluviz/RegisterFilePanel.java`

Visualizes the 32-register file.

Responsibilities:

- Displays registers in a grid
- Supports register editing by clicking a cell
- Highlights read registers
- Highlights written registers
- Highlights registers involved in a hazard

### 5.16 `src/aluviz/MemoryPanel.java`

Visualizes data memory.

Responsibilities:

- Displays memory rows with addresses and values
- Shows ASCII interpretation of each word
- Highlights memory read and write operations
- Supports automatic scrolling to active addresses

### 5.17 `src/aluviz/MiniALUPanel.java`

Compact ALU summary view used inside the Data Flow tab.

Responsibilities:

- Displays operation name
- Displays inputs and result
- Displays flags
- Provides the Open Internals button
- Communicates with the main ALU tab

### 5.18 `src/aluviz/ExecutionLogPanel.java`

Text log for the data flow execution.

Responsibilities:

- Logs each instruction
- Logs each step
- Logs hazard messages
- Logs cycle notes
- Provides a simple narrative of what the CPU is doing

### 5.19 `src/aluviz/WireAnimation.java`

Represents one moving value bubble.

Responsibilities:

- Stores source and destination points
- Stores animation label and color
- Calculates progress over time
- Applies easing for smoother motion

### 5.20 `src/aluviz/WiresOverlay.java`

Draws wiring and the active animated bubble.

Responsibilities:

- Renders static connector lines
- Renders the active wire path
- Renders the moving value bubble on top of the datapath

## 6. Execution Flow

This section explains what happens when a user runs the application.

### 6.1 Application startup

1. `Main.main()` starts the Swing UI.
2. `AppPanel` creates the two tabs.
3. `MainPanel` and `DataFlowPanel` are initialized.
4. The application waits for the user to choose a tab and start a demo.

### 6.2 ALU Internals flow

1. The user enters two operands.
2. The user selects an operation.
3. `ALUCore.parseOperand()` converts input text into an 8-bit integer.
4. `ALUCore.compute()` performs the chosen operation.
5. The result and flags are shown.
6. The appropriate visual panel animates the computation.

### 6.3 Data Flow flow

1. The user selects or types a MIPS-like instruction.
2. `InstructionParser.parse()` converts the text into a `ParsedInstruction`.
3. `HazardDetector.analyze()` checks the new instruction against the previously completed instruction.
4. `InstructionExecutor.plan()` creates the cycle and data movement steps.
5. `DataFlowPanel` sends the plan to the stage panel and log.
6. `ExecutionStep` objects are played one by one.
7. Register file, ALU, memory, and wires update visually.
8. The machine state is updated at the correct writeback step.

### 6.4 Hazard flow

1. The previous instruction wrote to a register.
2. The next instruction reads that same register.
3. `HazardDetector` marks the dependency.
4. `DataFlowPanel` shows a hazard banner.
5. `ExecutionLogPanel` prints a hazard message.
6. `RegisterFilePanel` highlights the involved registers.
7. `InstructionExecutor` inserts a stall and bubble for load-use hazards.
8. `PipelineStagePanel` shows the stall inserted into the stage flow.

## 7. Algorithms Used

### 7.1 Ripple-carry addition

The adder works bit by bit from least significant bit to most significant bit. Each bit uses the carry from the previous bit. This is the simplest correct hardware model and matches classroom diagrams well.

### 7.2 Two's complement subtraction

Subtraction is implemented as:

`A - B = A + (~B) + 1`

This lets one adder handle both add and subtract.

### 7.3 Signed overflow detection

Signed overflow is detected when the sign of the two operands matches and the sign of the result differs.

### 7.4 Hazard detection

The hazard detector follows a simple dependency rule:

- Find the destination register of the previous instruction.
- Collect the source registers of the current instruction.
- If they overlap, a RAW hazard exists.
- If the previous instruction was a load, classify it as a load-use hazard.

### 7.5 Automatic stall insertion

For a load-use hazard, the executor inserts:

- a STALL cycle
- a BUBBLE cycle

This models the extra waiting time needed before the dependent instruction can safely continue.

## 8. User Interface Design

The interface is intentionally structured for clarity.

### 8.1 Color and layout choices

- Light background surfaces reduce visual noise
- Blue is used for active normal stages
- Orange and red are used for hazard-related warnings
- Panels are separated by borders and headers for readability

### 8.2 Why the UI is report friendly

The panels clearly separate different architectural components:

- Register file
- ALU
- Memory
- Log
- Pipeline stage strip

This makes it easy to explain each CPU unit in a presentation.

## 9. Demo and Test Scenarios

The project is ready for classroom demonstrations. These are the best sample cases.

### 9.1 Basic ALU demo

- `0b00001101` and `0b00000110` with ADD
- Shows ripple carry and a normal positive result

### 9.2 Overflow demo

- `127` and `1` with ADD
- Shows signed overflow and the V flag

### 9.3 Subtraction demo

- `10` and `3` with SUB
- Shows two's complement subtraction

### 9.4 Logic demo

- `0xCC` and `0xF0` with AND or XOR
- Shows bitwise logic behavior

### 9.5 Shift demo

- `0b11110000` with SRA and SRL
- Shows the difference between logical and arithmetic shift right

### 9.6 Hazard demo

Use the following pair in the MIPS Data Flow tab:

```asm
lw  $t0, 0($t4)
add $t2, $t0, $t3
```

Expected result:

- Hazard banner appears
- Execution log shows a hazard message
- Register file highlights the dependency
- Pipeline stage strip shows a stall and bubble for load-use hazard

## 10. Build and Run Instructions

Compile from the project root:

```bat
javac -d out src\aluviz\*.java
java -cp out aluviz.Main
```

If the environment supports the existing build script, it can also be used for packaging.

## 11. Current Scope and Limitations

The project is intentionally scoped for a final semester demo.

Current limitations:

- The ALU is 8-bit instead of 32-bit for readability
- The simulation is educational rather than a full hardware-accurate CPU emulator
- It does not implement the entire MIPS instruction set
- Branch and jump handling are not the main focus yet
- The pipeline stage strip is a pedagogical view rather than a full cycle-accurate simulator for overlapping instructions

These are not weaknesses for the project report. They are design choices that keep the tool understandable and demo-friendly.

## 12. Future Enhancements

If more time is available, the next improvements could be:

- Branch and jump visualization
- Full forwarding path controls
- Explicit register forwarding toggle
- More instructions such as `beq`, `bne`, `andi`, `ori`
- Test suite for parser, ALU, and hazard detection
- Export of execution trace as a report or JSON file

## 13. Contributors

- [abdullahxdev](https://github.com/abdullahxdev)
- [saadhtiwana](https://github.com/saadhtiwana)
- [ahmadmustafa02](https://github.com/ahmadmustafa02)

## 14. Short Report Summary

Bitsmith is an interactive MIPS ALU and datapath visualizer that demonstrates arithmetic, logic, shifts, register flow, memory flow, control signals, cycle stages, and hazard handling. The project is written in Java Swing and is suitable for a Computer Architecture final project because it connects textbook concepts with visible, animated behavior.

Signed by saadhtiwana

## 15. Detailed Viva Walkthrough of the Code

This section is written so a student can revise the whole project directly from the document. It explains the logic of the source files in a line-by-line style, but grouped by meaning so it is easier to study and remember during viva.

### 15.1 `Main.java` line-by-line logic

The role of `Main.java` is only to start the application.

1. `public class Main` declares the entry class.
2. `public static void main(String[] args)` is the Java entry point.
3. `UIManager.setLookAndFeel(...)` tells Swing to use the system theme.
4. The `try/catch` protects the app if the look-and-feel is unavailable.
5. `SwingUtilities.invokeLater(...)` moves UI creation onto the Event Dispatch Thread.
6. Inside the lambda, a `JFrame` is created with the title of the project.
7. `setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)` closes the app fully when the window closes.
8. `setContentPane(new AppPanel())` loads the whole application UI.
9. `pack()` sizes the frame according to the preferred sizes of panels.
10. `setLocationRelativeTo(null)` centers the window.
11. `setMinimumSize(...)` prevents the window from becoming too small.
12. `setVisible(true)` finally displays the application.

Viva line: `Main.java` does not perform ALU logic itself. It only starts the Swing interface safely.

### 15.2 `AppPanel.java` line-by-line logic

`AppPanel` is the root panel that holds the two tabs.

1. `extends JPanel` means it is a reusable Swing container.
2. `private final MainPanel aluInternalsPanel;` stores the ALU tab.
3. `private final DataFlowPanel dataFlowPanel;` stores the datapath tab.
4. `private final JTabbedPane tabs;` creates the tab control.
5. In the constructor, `super(new BorderLayout())` sets the layout.
6. `aluInternalsPanel = new MainPanel();` creates the ALU internals UI.
7. `dataFlowPanel = new DataFlowPanel();` creates the datapath UI.
8. `dataFlowPanel.setAppPanel(this);` gives the child panel a reference back to switch tabs.
9. `tabs = new JTabbedPane(JTabbedPane.TOP);` places tabs at the top.
10. `tabs.setFont(...)` makes the tab text bold and readable.
11. `tabs.addTab("ALU Internals", aluInternalsPanel);` adds the first tab.
12. `tabs.addTab("MIPS Data Flow", dataFlowPanel);` adds the second tab.
13. `add(tabs, BorderLayout.CENTER);` puts the tabbed pane into the root panel.
14. `showALUInternals()` changes the selected tab to index 0.
15. `showDataFlow()` changes the selected tab to index 1.

Viva line: `AppPanel` is the switchboard of the whole application.

### 15.3 `MainPanel.java` line-by-line logic

`MainPanel` is the ALU Internals screen.

1. Text fields `fieldA` and `fieldB` store operand input.
2. `opCombo` gives the user the ALU operation choices.
3. `speedSlider` controls animation speed.
4. `runBtn` starts the computation and animation.
5. `resultBin`, `resultDec`, `resultHex` display the final answer in three formats.
6. `lampZ`, `lampN`, `lampC`, `lampV` are the four custom status indicators.
7. `controlBits` and `controlOpName` show the ALU control code and name.
8. `adderPanel` and `registerPanel` are the two visualization modes.
9. `vizLayout` is a `CardLayout`, so only one visualization is visible at a time.
10. In the constructor, the panel uses a `BorderLayout`.
11. `buildTop()` creates the operand and operation entry area.
12. `buildBottom()` creates the result, flags, and control-signal area.
13. `vizCard` stores the schematic and register visualization panels.
14. `runBtn.addActionListener(this::onRun);` binds the button to the run logic.
15. `opCombo.addActionListener(...)` updates the control preview whenever the operation changes.
16. `updateControlPreview()` copies the selected operation's 4-bit control code and label into the UI.

Important logic in `onRun(ActionEvent e)`:

1. Read text from the A and B fields.
2. Use `ALUCore.parseOperand(...)` to convert the text to integers.
3. If parsing fails, show a warning dialog and stop.
4. Get the selected ALU operation from `opCombo`.
5. Call `ALUCore.compute(op, a, b)`.
6. Pass the result to `showResult(r)`.
7. If the operation is ADD, SUB, or SLT, show the schematic panel.
8. For logic and shift operations, show the register-style panel.
9. Start the selected panel animation with the slider speed.

Important logic in `showResult(ALUCore.Result r)`:

1. `ALUCore.toBinary(r.result)` formats the result as an 8-bit binary string.
2. Signed decimal is derived from the MSB if the result is negative.
3. Hex is formatted with `String.format("0x%02X", r.result)`.
4. Each flag lamp is switched on or off using the computed flag values.
5. The control code and operation label are updated again for clarity.

Viva line: `MainPanel` does not implement ALU logic itself. It only sends values to `ALUCore` and shows the result.

### 15.4 `ALUCore.java` line-by-line logic

This file contains the actual ALU calculation logic.

#### 15.4.1 Width constant

`public static final int WIDTH = 8;`

This means the project uses 8-bit values for visualization. It keeps the schematic readable.

#### 15.4.2 `Op` enum

Each operation stores two pieces of information:

1. `controlBits` - the 4-bit ALU control code.
2. `label` - the display name used in the UI.

For example:

- ADD uses `0010`
- SUB uses `0110`
- AND uses `0000`
- OR uses `0001`

#### 15.4.3 `FullAdderStep`

This class stores the state of one full-adder bit:

- `index` is the bit position.
- `a` is the bit from operand A.
- `b` is the bit from operand B.
- `carryIn` is the carry from the previous bit.
- `sum` is the output bit.
- `carryOut` is the next carry.

This trace is what makes the carry animation possible.

#### 15.4.4 `Result`

This class stores everything returned by the ALU:

- operation used
- original operands
- effective operands after transformation
- final result
- flags
- adder trace

This is a report-friendly design because it separates calculation from presentation.

#### 15.4.5 `compute(Op op, int a, int b)`

This is the dispatcher method.

1. Mask both operands to 8 bits.
2. Check the selected operation.
3. Route to the correct helper method.
4. Return the correct `Result` object.

This method is the single public entry point to the ALU.

#### 15.4.6 `doAdd(...)`

This is the main arithmetic engine.

1. If subtracting, invert B and set the initial carry to 1.
2. Create an empty trace list.
3. Initialize `sum` to 0.
4. Loop from bit 0 to bit 7.
5. Extract the current bit of A and B.
6. Compute the sum bit using XOR.
7. Compute the carry-out using the full adder formula.
8. Store the sum bit into the correct position of `sum`.
9. Add a `FullAdderStep` to the trace.
10. Move the carry forward to the next bit.
11. After the loop, compute sign bits for A, B, and result.
12. Detect signed overflow.
13. Detect carry out.
14. Detect zero and negative flags.
15. Return a complete `Result`.

Viva explanation: this is exactly the ripple-carry adder taught in Computer Architecture.

#### 15.4.7 `doBitwise(...)`

1. Mask the result to 8 bits.
2. Set zero if the result is 0.
3. Set negative based on the MSB.
4. Return a `Result` with no carry or overflow.

#### 15.4.8 `doSlt(...)`

1. Perform subtraction internally using `doAdd` in subtraction mode.
2. Apply the rule `negative XOR overflow`.
3. If true, result is 1, otherwise 0.
4. Return the SLT result.

#### 15.4.9 `doShift(...)`

1. Extract the shift amount from B.
2. If SLL, shift left and mask.
3. If SRL, use logical right shift.
4. If SRA, sign-extend first, then use arithmetic right shift.
5. Compute zero and negative flags.
6. Return the shift result.

#### 15.4.10 `mask(...)`, `toBinary(...)`, and `parseOperand(...)`

- `mask` keeps only 8 bits.
- `toBinary` prints an 8-bit binary string.
- `parseOperand` accepts decimal, hex, binary, and negative decimal input.

Viva line: `ALUCore` is pure logic. It has no Swing dependency, so it is easy to test and explain.

### 15.5 `HazardDetector.java` line-by-line logic

This file checks whether the current instruction depends on the previous one.

1. `HazardType` lists the supported hazard kinds.
2. `Result` stores the detected hazard type, the writer register, the readers, and a suggestion.
3. `analyze(prev, next)` is the main method.
4. If either instruction is missing, return `NONE`.
5. Ask the previous instruction which register it writes to.
6. If it does not write a register, no hazard is possible.
7. Build a list of source registers used by the next instruction.
8. Compare the writer against the reader list.
9. If no overlap exists, return `NONE`.
10. If the previous instruction is a load, classify the problem as `LOAD_USE`.
11. Otherwise classify it as `RAW`.

Viva line: a hazard is detected when one instruction needs a value that was just produced by the previous instruction.

### 15.6 `InstructionParser.java` line-by-line logic

This file converts text into a structured instruction.

1. Trim the input line.
2. If it is empty, throw an error.
3. Separate mnemonic and operand list.
4. Convert the mnemonic to lowercase.
5. Use a switch to decide instruction type.
6. For R-type instructions, parse `rd, rs, rt`.
7. For `addi`, parse `rt, rs, imm`.
8. For `lw` and `sw`, parse `rt, imm(rs)`.
9. Convert register names with `MachineState.regIndex(...)`.
10. Convert immediates with `parseImmediate(...)`.

Viva line: the parser acts like a tiny assembler for the subset of MIPS that Bitsmith supports.

### 15.7 `ParsedInstruction.java` line-by-line logic

This class is a data holder.

1. `Kind` identifies whether the instruction is R-type, I-type arithmetic, load, or store.
2. `mnemonic` stores the original instruction name.
3. `aluOp` stores the ALU operation to use.
4. `rs`, `rt`, and `rd` store register indices.
5. `immediate` stores the immediate constant.
6. `destinationRegister()` returns the destination register depending on instruction kind.
7. `toString()` formats the instruction in readable assembly form.

Viva line: this class is the structured version of the assembly line entered by the user.

### 15.8 `InstructionExecutor.java` line-by-line logic

This file turns a parsed instruction into a visible execution plan.

1. `HazardAction` describes the optional hazard response.
2. `PipelinePlan` stores the final step list and the hazard result.
3. `execute(...)` is still available as a convenience wrapper.
4. `plan(...)` is the main method used by the UI.
5. Start with an empty `steps` list.
6. Add IF and ID cycle steps.
7. If a hazard exists, insert STALL and BUBBLE for load-use, or FORWARD for RAW.
8. For R-type and addi, add EX and WB stages.
9. For load, add EX, MEM, and WB stages.
10. For store, add EX and MEM stages.
11. After cycle planning, execute the instruction-specific state changes.
12. `executeRType` reads source registers, performs ALU computation, then writes back.
13. `executeITypeArith` does the same but uses the immediate value.
14. `executeLoad` calculates the effective address, reads memory, and writes to a register.
15. `executeStore` calculates the address, reads the data register, and writes memory.

Viva line: this class is like the micro-operation planner for a CPU cycle sequence.

### 15.9 `DataFlowPanel.java` line-by-line logic

This is the main orchestrator for the datapath tab.

1. Constants define the background and accent colors.
2. `EXAMPLES` stores the preset instructions shown in the combo box.
3. `state` is the machine state shared by register and memory panels.
4. `regFile`, `aluPanel`, `memoryPanel`, and `logPanel` are the visible components.
5. `stagePanel` shows the pipeline stages.
6. `exampleCombo`, `customField`, `runBtn`, `stepBtn`, `resetBtn`, and `speedSlider` are the instruction controls.
7. `hazardBanner` shows hazard messages in a prominent place.
8. `pendingSteps` stores the current execution plan.
9. `stepCursor` tracks the current step index.
10. `currentInstr` stores the instruction being executed.
11. `lastCompletedInstruction` is needed for hazard checking.
12. `currentHazard` stores the current hazard analysis result.
13. `frameTimer` drives animation.
14. `currentAnim` stores the active wire animation.
15. `busy` prevents overlapping instruction runs.

Important constructor logic:

1. Set a soft background and padding.
2. Create all child panels.
3. Add the top control section.
4. Add the center datapath view.
5. Add the log at the bottom.
6. Attach button listeners.
7. Connect the Open Internals button to the ALU tab.
8. Start the timer for animation updates.

Important planning logic in `startInstruction()`:

1. Parse the text from the instruction field.
2. Run hazard detection against the previous instruction.
3. If hazard exists, log it and highlight affected registers.
4. Update the hazard banner text.
5. Build the execution plan using `InstructionExecutor.plan(...)`.
6. Send the plan to the pipeline stage panel.
7. Reset the step cursor.
8. Clear old visual highlights.
9. Set ALU panel operation and reset its display.
10. Append the instruction header to the log.
11. Mark the panel busy and begin the first step.

Important animation logic in `beginCurrentStep()`:

1. Read the current step from the step list.
2. Highlight the corresponding stage in `stagePanel`.
3. Write the step description to the log.
4. For register reads, mark the register file and animate wire travel to the correct destination.
5. For cycle steps such as IF, ID, EX, MEM, WB, STALL, BUBBLE, and FORWARD, create a short labeled animation cue.
6. For ALU compute, set the ALU active and display the result immediately.
7. For memory access, animate data movement between ALU and memory.
8. For writeback, animate movement into the register file.

Important ending logic in `finishInstruction()`:

1. Set busy to false.
2. Turn off the ALU highlight.
3. Clear register and memory highlights.
4. Clear the wires overlay.
5. Refresh the register file and memory display.
6. Clear hazard highlighting.
7. Clear the pipeline stage panel.
8. Remember the instruction as the previous instruction for future hazard detection.

Viva line: `DataFlowPanel` is the control tower of the Data Flow tab.

### 15.10 `PipelineStagePanel.java` line-by-line logic

This panel was added to show the stage flow in a compact way.

1. The panel uses a soft background and border like the rest of the UI.
2. `title` shows the name of the strip.
3. `note` shows the current instruction and hazard message.
4. `stages` stores the badge components in order.
5. The constructor adds badges for IF, ID, EX, MEM, WB, STALL, BUBBLE, and FORWARD.
6. `setPlan(...)` updates the summary text and makes stall/bubble/forward visible when needed.
7. `setActiveStep(...)` highlights the current stage.
8. `clearActive()` resets everything to idle.
9. `mapStage(...)` converts execution step types to display labels.

Viva line: this panel makes the instruction progression visible in one glance.

### 15.11 `RegisterFilePanel.java` line-by-line logic

This panel shows all 32 registers.

1. A grid is created with 4 rows and 8 columns.
2. Each cell is a custom `Cell` component.
3. `markRead(...)` highlights the register being read into ALU input A or B.
4. `markWrite(...)` highlights the destination register and updates its displayed value.
5. `markHazard(...)` stores the hazard writer and the readers involved.
6. `clearHazard()` removes the hazard highlight.
7. `refreshAll()` copies current values from machine state.
8. The nested `Cell` handles click-to-edit behavior.
9. In `paintComponent`, the cell chooses colors based on read, write, or hazard state.
10. The register name and numeric index are drawn in the top corners.
11. The hex value is drawn in the center.
12. The decimal hint is drawn at the bottom.

Viva line: the register file is both a storage view and a teaching view of register dependencies.

### 15.12 `MemoryPanel.java` line-by-line logic

This panel shows data memory.

1. `RowsCanvas` stores memory as a list of address-value pairs.
2. `refreshFromState()` repopulates the visible memory rows.
3. `ensureRange(...)` makes sure the demo addresses around `MEM_BASE` stay visible.
4. `markRead(...)` highlights a memory read and scrolls to the address.
5. `markWrite(...)` writes into machine state, highlights the row, and scrolls to it.
6. `paintComponent(...)` draws address, value, and ASCII columns.
7. The ASCII conversion turns non-printable bytes into dots.

Viva line: the memory panel shows how load and store interact with the datapath.

### 15.13 `MiniALUPanel.java` line-by-line logic

This is the compact ALU summary inside the datapath tab.

1. It shows ALU input A and input B.
2. It shows the operation label.
3. It shows the result and signed decimal interpretation.
4. It shows the four flags using lamp style indicators.
5. The ALU box changes color when active.
6. `setOperation(...)`, `setInputs(...)`, and `setResult(...)` update the view.
7. `onOpenInternals(...)` wires the button to the main ALU tab.

Viva line: this panel gives a quick summary before jumping to the full ALU view.

### 15.14 `ExecutionLogPanel.java` line-by-line logic

This panel gives the textual story of the simulation.

1. The constructor creates a scrolling text area.
2. `appendInstructionHeader(...)` starts a new instruction section.
3. `appendStep(...)` logs atomic execution steps.
4. `appendHazard(...)` writes hazard warnings.
5. `appendCycleNote(...)` writes cycle labels such as IF or STALL.
6. `clear()` resets the log when the user presses Reset.

Viva line: the execution log is the easiest place to explain the CPU behavior step by step.

### 15.15 `WireAnimation.java` line-by-line logic

This class animates a value as it moves between components.

1. It stores source point, destination point, label, and color.
2. `start()` stores the start time.
3. `linearProgress()` measures raw progress from 0 to 1.
4. `easedProgress()` makes the motion smoother.
5. `isDone()` checks whether animation time is finished.
6. `currentPos()` calculates the current bubble position.

Viva line: this class gives motion to the datapath so the flow feels alive.

### 15.16 `WiresOverlay.java` line-by-line logic

This panel draws the wires above the datapath.

1. It is transparent because it sits on top of other panels.
2. `setStaticWires(...)` stores fixed lines if needed.
3. `setActive(...)` stores the current moving animation.
4. `clearActive()` removes the current animation.
5. In `paintComponent(...)`, the panel draws the static lines first.
6. Then it draws the active highlighted wire.
7. Finally it draws the moving value bubble with a label.

Viva line: the overlay is what makes the data flow visibly move between components.

## 16. What to Say in Viva

If the examiner asks for a short explanation, you can answer in this order:

1. Bitsmith is a Java Swing visualizer for MIPS ALU internals and data flow.
2. It demonstrates arithmetic, logic, shift, and control signal concepts from Computer Architecture.
3. It includes a second tab for instruction-level datapath simulation.
4. The project now shows hazards, pipeline stages, stalls, and forwarding cues.
5. The code is divided into a simulation core and a UI layer.
6. `ALUCore` performs the actual operations.
7. `DataFlowPanel` controls the instruction flow visualization.
8. `HazardDetector` checks RAW and load-use dependencies.
9. `InstructionExecutor` turns an instruction into visible execution steps.
10. The project is meant for teaching and demonstration rather than full CPU emulation.

## 17. Short Line-by-Line Revision Notes

Use these as quick revision prompts:

- `Main.java` starts the app.
- `AppPanel.java` holds the tabs.
- `MainPanel.java` runs and visualizes ALU operations.
- `ALUCore.java` computes the results.
- `AdderSchematicPanel.java` shows carry flow.
- `RegisterViewPanel.java` shows logic and shifts.
- `MachineState.java` stores registers and memory.
- `InstructionParser.java` understands assembly text.
- `ParsedInstruction.java` stores the parsed instruction.
- `HazardDetector.java` finds dependencies.
- `InstructionExecutor.java` builds execution steps.
- `ExecutionStep.java` stores step metadata.
- `DataFlowPanel.java` orchestrates the whole datapath tab.
- `PipelineStagePanel.java` shows stage flow.
- `RegisterFilePanel.java` shows registers.
- `MemoryPanel.java` shows memory.
- `MiniALUPanel.java` shows the compact ALU view.
- `ExecutionLogPanel.java` writes the step trace.
- `WireAnimation.java` moves the data bubble.
- `WiresOverlay.java` draws the wires.

## 18. Conclusion for the Report

Bitsmith is a complete teaching tool for a Computer Architecture final project because it shows both the internal ALU logic and the higher level MIPS datapath behavior. The added pipeline stage view and hazard handling make the project stronger because they connect the ALU model to real processor design ideas such as execution stages, dependency checking, stalls, and forwarding.
