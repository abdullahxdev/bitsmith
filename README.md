# ALU Internals Visualizer

**Computer Architecture Final Lab Project**
An interactive Java/Swing visualization tool that opens up the "ALU box" hidden inside the single-cycle and multi-cycle MIPS datapaths and shows how each operation is actually computed at the bit and gate level.

## How to run

```
java -jar ALUVisualizer.jar
```

Requires Java 11 or newer.

To rebuild from source:
```
./build.sh
```

## What the tool shows

The MIPS datapath diagrams in the textbook show the ALU as a single box labeled "ALU." This tool **opens that box** and shows what's inside.

### Two visualization modes

| Mode | Used for | What it shows |
|---|---|---|
| **Schematic view** | ADD, SUB, SLT | 8-bit ripple-carry adder as a chain of 8 full adders, each containing XOR and AND/OR gates. Animates carry propagation from LSB to MSB. |
| **Register view** | AND, OR, XOR, NOR, SLL, SRL, SRA | 8-bit operand rows, gate boxes between them, animated bit-by-bit. Shifts show bits sliding visually. |

### Always visible

- **Status flags**: Zero (Z), Negative (N), Carry-out (C), Overflow (V) — the same flags used by branch instructions (`beq`, `bne`, `blt`, etc.)
- **ALU control signal**: the 4-bit operation code (textbook values) for the selected operation
- **Result**: shown in binary, signed decimal, and hex

## Computer Architecture concepts covered

| Concept | Where it appears |
|---|---|
| Binary, decimal, hex representations | Input parsing; result display in all three |
| Two's complement | Subtraction visualization (A + ~B + 1); negative decimal display |
| Half adder / full adder | Schematic shows XOR + AND/OR gates inside each cell |
| Ripple-carry adder | The whole adder schematic |
| Carry propagation | Animated left-to-right (LSB → MSB) |
| Logic gates (XOR, AND, OR, NOR) | Visible in schematic; bit-by-bit gate boxes in register view |
| Hardware reuse | One adder serves both ADD and SUB (just invert B and set Cin=1) |
| ALU operation control | 4-bit signal panel — shows how ADD=0010, SUB=0110, AND=0000, etc. |
| Status flags (Z, N, C, V) | Live flag indicators update per operation |
| SLT implementation | Internally does subtraction, then takes (sign XOR overflow) as the result bit |
| Logical vs arithmetic shift | SRA preserves the sign bit when shifting right; SRL fills with zeros |
| Role of ALU in single-cycle datapath | Tool *is* the inside of that single ALU box in the diagram |
| Role of ALU in multi-cycle datapath | Same hardware reused across cycles (PC+4 in IF, address calc in MEM, branch target in EX, etc.) |

## Demo / test cases

Run these in order during the demo:

| # | A | B | Op | Expected result | What it demonstrates |
|---|---|---|---|---|---|
| 1 | `0b00001101` (13) | `0b00000110` (6) | ADD | `0b00010011` (19), no flags | Basic ripple-carry, single carry propagation |
| 2 | `127` | `1` | ADD | `-128`, V=1, N=1 | **Signed overflow** — same-sign inputs producing opposite-sign result |
| 3 | `0b11111111` | `0b00000001` | ADD | `0`, Z=1, C=1 | **Unsigned carry-out** without signed overflow |
| 4 | `10` | `3` | SUB | `7` (no flags) | Subtraction via inverted B + carry-in 1 (adder reuse) |
| 5 | `5` | `10` | SUB | `-5`, N=1 | Negative result; A + ~B + 1 still works |
| 6 | `0xCC` | `0xF0` | AND | `0xC0` | Bitwise AND, truth table reminder |
| 7 | `0xCC` | `0xF0` | XOR | `0x3C` | Bitwise XOR |
| 8 | `0b00000101` | `2` | SLL | `0b00010100` (20) | Logical left shift = multiply by 2^shamt |
| 9 | `0b11110000` (-16) | `2` | SRA | `0b11111100` (-4) | **Arithmetic right shift** preserves sign |
| 10 | `0b11110000` | `2` | SRL | `0b00111100` (60) | Logical right shift fills with 0 |
| 11 | `5` | `10` | SLT | `1` | Set Less Than — uses sign of subtraction result |

## Demo script (5 minutes)

1. **Open with the textbook diagram in mind.** Say: "the MIPS single-cycle and multi-cycle datapaths both contain a single ALU box. Our project opens that box."
2. **Run test #1 (basic ADD).** Point out: each full adder cell, XOR computing the sum bit, AND/OR cluster computing the carry, carry propagating from right to left, status flags lighting up.
3. **Run test #2 (overflow).** Show that the V flag lights up — explain why same-sign inputs producing opposite-sign output means overflow.
4. **Run test #4 (SUB).** Highlight: the B row now shows the *inverted* B value, and carry-in starts at 1. Same adder hardware, different control.
5. **Run test #9 (SRA).** Show how the sign bit is preserved on the left side — contrast with test #10 (SRL).
6. **Point to the ALU control panel.** Each operation has a unique 4-bit code (textbook values from the MIPS ALU control unit). Explain: this signal comes from the ALU control unit, which decodes the 2-bit ALUOp (from the main control) plus the 6-bit funct field of R-type instructions.
7. **Close with the multi-cycle connection.** In single-cycle, this ALU runs once per instruction. In multi-cycle, this same hardware is reused across cycles — computing PC+4 in IF, address calculation in MEM, branch target in EX, and so on.

## Project structure

```
src/aluviz/
├── Main.java                  — JFrame entry point
├── MainPanel.java             — Root panel: inputs, results, flags, control signal
├── ALUCore.java               — Pure-Java ALU simulation logic + per-bit trace
├── AdderSchematicPanel.java   — Schematic visualization for ADD/SUB/SLT
└── RegisterViewPanel.java     — Register-style visualization for the rest
```

The simulation core (`ALUCore.java`) is fully decoupled from the GUI — it could be reused by a future MARS Tool plugin that observes the ALU during MIPS program execution.

## Limitations / scope notes

- **8-bit width** (not 32-bit): fits 8 full adders on screen and keeps the visualization legible. Concepts are identical to 32-bit.
- **No multiplication/division**: Booth's algorithm and restoring division are scoped out for this version. Could be added as additional tabs.
- **Standalone, not a MARS plugin**: The current version runs independently. Wiring it as a MARS `AbstractMarsToolAndApplication` so it observes a running MIPS program is the natural next extension.
