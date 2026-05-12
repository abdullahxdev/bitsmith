# ALU Internals Visualizer — Complete Documentation

This document is a deep walkthrough of the project — written for someone who has studied basic Computer Architecture but wants to understand **every part** of how the project works, what's happening in the code, and which CA concepts underpin each piece.

The structure is:

1. **What the project is and why it exists**
2. **Computer Architecture foundations** — every CA concept used, explained from the basics
3. **Code walkthrough** — every file, every important function
4. **End-to-end data flow** — what happens between clicking "Compute" and seeing the animation
5. **Likely viva questions and how to answer them**

---

# 1. What the project is and why it exists

## The problem we're solving

When you draw the MIPS single-cycle or multi-cycle datapath in your CA textbook, the **ALU** appears as a single box with two inputs (A and B), one output (Result), one control signal (4-bit ALU operation code), and a Zero flag wire coming out.

But that's a black box. Inside that box are gates, adders, multiplexers, and control logic that actually *compute* the operation. A student who only studies the datapath diagram never sees what's inside.

## What our tool does

The ALU Internals Visualizer is a Java desktop application (built with Swing) that:

1. Lets the user enter two 8-bit operands (in binary, decimal, or hex) and choose an ALU operation.
2. **Animates** the computation step by step — showing carry propagation in the adder, bit-by-bit logical operations, sign preservation in shifts, etc.
3. Displays the 4-bit ALU control signal that selects the operation (matching textbook conventions).
4. Updates the status flags (Zero, Negative, Carry-out, Overflow) live.
5. Uses two complementary visual styles — a **schematic view** with visible XOR/AND/OR gates for the adder, and a **register view** with bit-rows-and-gates for everything else.

## Why 8-bit instead of 32-bit

A 32-bit adder has 32 full-adder cells. On a typical laptop screen, drawing 32 schematic cells with gates inside each would be unreadable. We use 8 bits so each full adder cell is large enough to show its internal gates clearly. **Every concept transfers identically to 32-bit** — the only difference is the width of the data path.

## What "single-cycle and multi-cycle" connection means

The instructor's project scope is "single-cycle datapath and multi-cycle datapath." The ALU is the central computational element in both:

- In the **single-cycle datapath**, the ALU executes once per instruction. The whole instruction must finish in one clock cycle, so the cycle time is limited by the slowest path (usually a `lw` that goes through the ALU and then memory).
- In the **multi-cycle datapath**, the same ALU is reused across multiple cycles for different purposes: in the IF stage it computes `PC + 4`, in the EX stage it does the arithmetic/logic op or computes the branch target, in the MEM stage it computes the effective memory address. Reusing one ALU is what saves hardware.

Our tool *is* the inside of that ALU box. When we say the ALU control signal is `0010` for ADD, that's the same control bus that the main control unit drives in both datapath designs.

---

# 2. Computer Architecture foundations

This section explains every CA concept the project touches, starting from the basics.

## 2.1 Number representations

A computer stores everything as bits (0s and 1s). The same 8-bit pattern can be interpreted three ways:

| Bit pattern | Binary | Decimal (unsigned) | Decimal (signed, two's complement) | Hex |
|---|---|---|---|---|
| `0000 0101` | 5 | 5 | 5 | 0x05 |
| `0111 1111` | 127 | 127 | 127 | 0x7F |
| `1000 0000` | 128 | 128 | **-128** | 0x80 |
| `1111 1111` | 255 | 255 | **-1** | 0xFF |

The hardware doesn't know or care which interpretation you want. The same adder circuit works for both unsigned and signed addition — only the interpretation of the result and the overflow detection differ.

**In our project**: `ALUCore.parseOperand` accepts all three input formats. `MainPanel.showResult` displays the result in all three.

## 2.2 Two's complement (how computers represent negative numbers)

To get the two's complement of a number:
1. Invert every bit (1s complement).
2. Add 1.

Example, 8-bit representation of `-5`:
```
   5 in binary:       0000 0101
   Invert all bits:   1111 1010
   Add 1:             1111 1011  ← this is -5 in two's complement
```

**Why this matters**: with two's complement, the same hardware adder can compute `A - B` by computing `A + (~B) + 1`. No separate subtractor needed. This is the single most important reuse in the ALU.

**In our project**: `ALUCore.doAdd` takes a `subtract` flag. If true, it inverts B and starts the carry chain with 1 instead of 0. Same code path as addition — that's what hardware reuse looks like in software.

## 2.3 Logic gates

The four gates we use:

| Gate | Symbol | Truth table |
|---|---|---|
| AND | A·B | `00→0  01→0  10→0  11→1` |
| OR  | A+B | `00→0  01→1  10→1  11→1` |
| XOR | A⊕B | `00→0  01→1  10→1  11→0` |
| NOR | ¬(A+B) | `00→1  01→0  10→0  11→0` |

These are physical circuits (transistors arranged a certain way). Everything else in the ALU is built from combinations of these.

**In our project**: the register view shows truth tables for each bitwise operation. The schematic view shows XOR and AND/OR clusters inside each full adder.

## 2.4 Half adder and full adder

A **half adder** adds two single bits A and B and produces a Sum bit and a Carry-out bit. It needs no carry-in.

```
A ──┬── XOR ──── Sum
    │   │
    │  XOR (same gate)
B ──┼── │
    │   │
    └── AND ──── Cout
```

Truth table:
```
A B | Sum Cout
0 0 |  0   0
0 1 |  1   0
1 0 |  1   0
1 1 |  0   1   ← carry happens when both bits are 1
```

A **full adder** adds three bits: A, B, and a Carry-in (Cin) from the previous bit. It produces a Sum bit and a Carry-out bit.

```
Sum  = A XOR B XOR Cin
Cout = (A AND B) OR (Cin AND (A XOR B))
```

In words: the sum bit is 1 whenever an odd number of the three inputs are 1. The carry-out is 1 whenever at least two of the three inputs are 1.

You can build a full adder from two half adders plus an OR gate. In our schematic view, each full adder cell shows two gate boxes — the top "XOR" box computes the sum, and the bottom "AND/OR" box computes the carry-out.

**In our project**: `ALUCore.doAdd` implements exactly the full-adder formulas above in its inner loop:
```java
int s    = ai ^ bi ^ carry;                       // Sum  = A XOR B XOR Cin
int cOut = (ai & bi) | (carry & (ai ^ bi));       // Cout = (A·B) + (Cin · (A⊕B))
```

## 2.5 Ripple-carry adder

To add two 8-bit numbers, you chain 8 full adders together: the carry-out of bit *i* becomes the carry-in of bit *i+1*. The carry "ripples" from the least significant bit (LSB, right side) to the most significant bit (MSB, left side).

```
        bit 7    bit 6    bit 5    ...    bit 1    bit 0
        ┌────┐  ┌────┐  ┌────┐         ┌────┐  ┌────┐
A[7]───►│ FA │  │ FA │  │ FA │   ...   │ FA │  │ FA │◄───A[0]
B[7]───►│    │  │    │  │    │         │    │  │    │◄───B[0]
        │    │  │    │  │    │         │    │  │    │◄─── Cin=0 (or 1 for SUB)
        └─┬──┘  └─┬──┘  └─┬──┘         └─┬──┘  └─┬──┘
          │       │       │              │       │
        Sum[7]  Sum[6]  Sum[5]   ...   Sum[1]  Sum[0]

  ◄── Cout       ◄── ripple carry chain ──
```

The downside is that the MSB cell can't compute its Sum until the carry has rippled all the way from the LSB — so total delay is proportional to the width. Faster designs (carry-lookahead, carry-select) exist, but ripple-carry is the simplest and most textbook-standard.

**In our project**: the schematic view animates this ripple. Bit 0 (rightmost) lights up first, then bit 1, then bit 2, ... until bit 7 (leftmost). The carry-out arrow turns red if the carry is 1, gray if 0.

## 2.6 Status flags

After an ALU operation, four flags summarize the result:

- **Z (Zero)**: 1 if the result is exactly zero. Used by `beq` and `bne` branch instructions.
- **N (Negative)**: 1 if the MSB of the result is 1 (i.e., the result is negative under signed interpretation).
- **C (Carry-out)**: 1 if the carry rippled out of the MSB. For unsigned addition this signals overflow; for unsigned subtraction it indicates no borrow.
- **V (Overflow)**: 1 if **signed** overflow happened. This is different from carry-out. Signed overflow occurs when you add two same-signed numbers and get a result with the opposite sign.

**The overflow formula** (used in our code): `V = 1` iff the sign of A equals the sign of B but the sign of the result differs.

Why this works: if both inputs are positive, the true sum is positive, so a negative result means we exceeded the positive range. If both are negative, the true sum is negative, so a positive result means we underflowed.

**In our project**: `ALUCore.doAdd` computes all four flags. `MainPanel.FlagLamp` is a custom Swing component that displays each flag as a colored "lamp" — yellow when on, gray when off.

## 2.7 Shifts: SLL, SRL, SRA

Three shift operations exist in MIPS:

- **SLL — Shift Left Logical**: shift all bits left by the shift amount; fill the new low bits with 0. Equivalent to multiplying by 2^shift_amount.
- **SRL — Shift Right Logical**: shift all bits right; fill the new high bits with 0. Treats the operand as unsigned.
- **SRA — Shift Right Arithmetic**: shift all bits right; fill the new high bits with the **original sign bit**. Preserves the sign so that a signed division by 2^shift_amount works correctly.

Example with `1111 0000` (= -16 signed):
- SRL by 2: `0011 1100` (= 60 unsigned) — the sign is lost.
- SRA by 2: `1111 1100` (= -4 signed) — the sign is preserved.

**In our project**: `ALUCore.doShift` implements all three. The trick for SRA in Java is to sign-extend the 8-bit operand to 32 bits first, then use Java's `>>` (arithmetic shift), then mask back to 8 bits.

## 2.8 SLT — Set Less Than

`SLT $rd, $rs, $rt` sets `$rd` to 1 if `$rs < $rt` (signed comparison), else to 0. This is how MIPS implements `<` since there's no `blt` instruction in pure MIPS — instead the assembler expands `blt $a, $b, label` into `slt $t0, $a, $b; bne $t0, $zero, label`.

**How the ALU computes SLT**: it does `A - B` internally and looks at the sign of the result. With one subtlety — if signed overflow happened, the sign of the subtraction is misleading, so the actual rule is:

```
SLT result = (Negative XOR Overflow) ? 1 : 0
```

If no overflow, this reduces to "result is 1 iff the subtraction came out negative." If overflow, the XOR corrects for the wrap-around.

**In our project**: `ALUCore.doSlt` calls `doAdd` in subtraction mode, then applies the XOR rule.

## 2.9 ALU control signals

The ALU supports many operations, but the **main control unit** of the MIPS processor only outputs a 2-bit signal called **ALUOp**. This is because the main control unit doesn't want to know the difference between, say, `add` and `sub` — both are R-type instructions, both go through the EX stage the same way, both write the result to a register.

So the main control sets `ALUOp = 10` for R-type instructions, meaning "look at the funct field." A small dedicated piece of logic called the **ALU control unit** then takes the 2-bit ALUOp plus the 6-bit funct field (the bottom 6 bits of an R-type instruction) and produces a 4-bit ALU operation code that drives the ALU itself.

The textbook 4-bit ALU operation encoding is:

| Operation | 4-bit code |
|---|---|
| AND | 0000 |
| OR  | 0001 |
| ADD | 0010 |
| SUB | 0110 |
| SLT | 0111 |

We extended this with codes for XOR, NOR, and the three shifts. The exact codes for those vary by textbook — we picked unique 4-bit patterns.

**In our project**: each entry of the `ALUCore.Op` enum carries its 4-bit `controlBits` string. The `MainPanel` control-signal display shows it live whenever the user picks an operation. The text label below says these signals come from "ALUOp + funct" — making the textbook connection explicit.

## 2.10 The ALU in the single-cycle and multi-cycle datapaths

In both datapaths, the ALU sits between the **register file** (which provides operands `$rs` and `$rt`) and the **memory** / **register-file write port** (which receives the result).

- In the **single-cycle datapath**: there is exactly one ALU. Every instruction uses it exactly once. The cycle time must accommodate ALU + memory + register-file write delay all in one shot.
- In the **multi-cycle datapath**: there is still one ALU, but it's used **across multiple cycles** for different sub-tasks. The instruction is broken into IF, ID, EX, MEM, WB stages, and the ALU may be used in IF (PC+4), EX (the actual operation), and MEM (effective address). The clock cycle can be shorter because each cycle does less work.

**In our project's demo**, when you stand in front of the instructor, you say: "Our tool is the inside of the ALU box that appears in both these datapath diagrams. The same hardware we visualize is reused multiple times per instruction in the multi-cycle design."

---

# 3. Code walkthrough — file by file

The project has 5 Java files in `src/aluviz/`. Total around 750 lines.

## 3.1 `Main.java` — application entry point

This is the smallest file and the simplest. Its job:

1. Set the Swing look-and-feel to the system default (so the window looks native on Mac/Windows/Linux).
2. Create a JFrame (the main window).
3. Set the content pane to a new `MainPanel`.
4. Size the window, center it, show it.

```java
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ALU Internals Visualizer ...");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new MainPanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setMinimumSize(new Dimension(1100, 720));
            frame.setVisible(true);
        });
    }
}
```

**Why `SwingUtilities.invokeLater`**: Swing is not thread-safe. All UI construction must happen on the Event Dispatch Thread (EDT), and `invokeLater` is the standard way to switch from `main`'s thread onto the EDT.

## 3.2 `ALUCore.java` — the brain of the project

This is the most important file. It contains all the actual ALU logic — pure Java, no GUI dependencies. The reason for this separation: the GUI just visualizes whatever `ALUCore` computes. Anyone (including a future MARS plugin) could call `ALUCore.compute(op, a, b)` to get a result.

### 3.2.1 The `Op` enum

Defines every operation our ALU supports, with its textbook 4-bit control code and human label:

```java
public enum Op {
    ADD("0010", "ADD"),
    SUB("0110", "SUB"),
    AND("0000", "AND"),
    OR ("0001", "OR"),
    XOR("1101", "XOR"),
    NOR("1100", "NOR"),
    SLT("0111", "SLT"),
    SLL("1000", "SLL"),
    SRL("1001", "SRL"),
    SRA("1010", "SRA");
    ...
}
```

These 4-bit codes are what the ALU control unit would put on the wires going into the ALU in a real MIPS datapath.

### 3.2.2 The `FullAdderStep` class

Records what happened at one bit position during an addition. Captures: bit index, A bit, B bit, Cin, Sum bit, Cout. The `AdderSchematicPanel` reads a list of these to animate the schematic.

```java
public static class FullAdderStep {
    public final int index;
    public final int a, b, carryIn;
    public final int sum, carryOut;
    ...
}
```

### 3.2.3 The `Result` class

What `compute()` returns. Contains: the operation, the original operands, the *effective* operands (B might be inverted for SUB), the result, all four flags, and the per-bit trace (only filled for additions).

### 3.2.4 `compute(op, a, b)` — the dispatcher

A switch statement that routes to the right sub-routine. This is the single public entry point for the whole simulation.

### 3.2.5 `doAdd(op, a, b, subtract)` — the heart of the ALU

This is where ripple-carry addition is implemented. Annotated:

```java
private static Result doAdd(Op op, int a, int b, boolean subtract) {
    int bEff = subtract ? mask(~b) : b;        // ← invert B for subtraction
    int carry = subtract ? 1 : 0;              // ← carry-in is 1 for subtraction
    List<FullAdderStep> trace = new ArrayList<>();
    int sum = 0;

    for (int i = 0; i < WIDTH; i++) {          // ← loop bit 0 → bit 7
        int ai = (a >> i) & 1;                 // ← extract bit i of A
        int bi = (bEff >> i) & 1;              // ← extract bit i of (possibly inverted) B
        int s    = ai ^ bi ^ carry;            // ← full adder Sum formula
        int cOut = (ai & bi) | (carry & (ai ^ bi));  // ← full adder Cout formula
        sum |= (s << i);                       // ← stitch bit s into the result
        trace.add(new FullAdderStep(i, ai, bi, carry, s, cOut));
        carry = cOut;                          // ← ripple the carry to next iteration
    }

    int signA = (a    >> (WIDTH - 1)) & 1;     // ← MSB of A
    int signB = (bEff >> (WIDTH - 1)) & 1;     // ← MSB of effective B
    int signR = (sum  >> (WIDTH - 1)) & 1;     // ← MSB of result
    boolean overflow = (signA == signB) && (signA != signR);
    boolean carryOut = carry == 1;
    boolean zero = sum == 0;
    boolean negative = signR == 1;
    return new Result(op, a, b, a, bEff, sum, zero, negative, carryOut, overflow, trace);
}
```

The loop body is literally a software full adder. Each iteration is one full-adder cell from the hardware diagram.

The overflow check uses the rule from §2.6: same-sign inputs producing opposite-sign result.

### 3.2.6 `doBitwise(op, a, b, r)` — for AND, OR, XOR, NOR

Just stores the precomputed result and computes Z and N flags. No carry, no overflow concept for bitwise ops.

### 3.2.7 `doSlt(a, b)` — SLT

Calls `doAdd` in subtraction mode, then applies the rule:

```java
int less = (sub.negative ^ sub.overflow) ? 1 : 0;
```

This is exactly what real ALU hardware does — it has an XOR gate fed by the sign bit and the overflow flag, and that single output bit is the SLT result.

### 3.2.8 `doShift(op, a, b)` — SLL, SRL, SRA

Uses Java's shift operators after sign-extending for SRA:

```java
int shamt = b & (WIDTH - 1);                   // ← only low bits of B matter (textbook uses 5 for 32-bit; here 3 for 8-bit)
if      (op == Op.SLL) r = mask(a << shamt);
else if (op == Op.SRL) r = (a & mask(0xFFFFFFFF)) >>> shamt;
else { // SRA — sign-extend first
    int signed = (a << (32 - WIDTH)) >> (32 - WIDTH);   // sign-extend 8-bit to 32-bit
    r = mask(signed >> shamt);                          // arithmetic shift, then mask back
}
```

The `>>>` operator is Java's logical right shift; `>>` is arithmetic. The sign extension trick: shift left by 24, then arithmetic-shift right by 24, which propagates the original bit-7 (the 8-bit sign bit) through bits 8-31.

### 3.2.9 Utility methods

- `mask(v)`: chops `v` down to 8 bits.
- `toBinary(v)`: formats a value as 8-character binary string for display.
- `parseOperand(s)`: parses input text accepting decimal, `0xHH` hex, `0bBBBBBBBB` binary, or 8-character binary without prefix.

## 3.3 `MainPanel.java` — the main UI shell

This is the root Swing panel that fills the JFrame. It uses a `BorderLayout`:

- **NORTH**: input section (operands, operation dropdown, animation speed, Compute button).
- **CENTER**: a `CardLayout` panel that swaps between the schematic adder view and the register view based on which operation is selected.
- **SOUTH**: three side-by-side panels — Result display, Status Flags display, ALU Control display.

### Key methods in `MainPanel`

**`buildTop()`**: assembles the operand input section using `GridBagLayout`. Creates `JTextField`s for A and B, a `JComboBox` for the operation, a `JSlider` for animation speed, and the Compute button.

**`buildBottom()`**: assembles the bottom three side-by-side panels. The flags panel contains four `FlagLamp` instances (one per flag).

**`updateControlPreview()`**: called whenever the user changes the operation in the dropdown. Updates the 4-bit ALU control signal display so the user can see the control code even before clicking Compute.

**`onRun(ActionEvent)`**: the Compute button handler. Steps:
1. Parse the two operands via `ALUCore.parseOperand`. If parsing throws, show an error dialog.
2. Call `ALUCore.compute(op, a, b)` to get the result.
3. Call `showResult(r)` to update the bottom panel.
4. Decide which visualization to show: schematic (for ADD/SUB/SLT) or register view (for everything else). Use `CardLayout.show()` to swap, then call the panel's `animate(r, speed)`.

**`showResult(r)`**: updates the binary/decimal/hex result labels, the four flag lamps, and the ALU control bits. The signed decimal conversion is done by checking the MSB and subtracting `2^WIDTH` if it's set.

### The `FlagLamp` inner class

A small custom Swing panel that displays one flag name in big bold text with a description below. Has a `setOn(boolean)` method that switches the background to yellow when active or gray when inactive. The visual metaphor is a real status LED on a circuit board.

## 3.4 `AdderSchematicPanel.java` — the centerpiece visualization

This is the schematic view used for ADD, SUB, and SLT. It draws 8 full-adder cells in a row, each with visible XOR and AND/OR gate boxes, and animates them lighting up one by one as the carry ripples from LSB to MSB.

### Animation mechanism

The animation uses a `javax.swing.Timer`. The timer fires every `delay` milliseconds (computed from the user's speed slider). On each tick, it increments `highlightedBit` and calls `repaint()`. The `paintComponent()` method redraws all 8 cells, but only cells with `index <= highlightedBit` are drawn in the "active" colors (blue/green); the rest are drawn faded gray. When `highlightedBit` reaches 8, the timer stops.

This is the classic Swing animation pattern: a Timer drives state changes, `repaint()` triggers re-rendering, and `paintComponent` reads the current state to decide what to draw.

### `paintComponent(Graphics g0)`

The main drawing routine. After casting to `Graphics2D` and enabling anti-aliasing:

1. If `current` is null (no computation done yet), draw a help message.
2. Draw a title at the top describing the operation (e.g., "SUB — A + (~B) + 1   (reuses the adder by inverting B and carry-in = 1)" if this is a subtraction).
3. Draw row labels at the left (A, B, Cin, Sum).
4. Loop over the 8 bit positions and call `drawFullAdder()` for each, placing bit 0 on the far right and bit 7 on the far left (standard schematic convention).
5. Draw the final Cout arrow on the far left of the chain.
6. Draw the summary text at the bottom — A, effective B (showing the inversion for SUB), and the result, all in binary.

### `drawFullAdder(...)`

Draws one full adder cell. Contents:
- A rounded rectangle for the cell border (blue if active, gray if inactive).
- The bit index label above the cell.
- The two input bits (A, B) at the top, the carry-in arrow at the right.
- Two gate boxes inside: an "XOR" box (sum gate) and an "AND/OR" box (carry gate).
- The Sum bit at the bottom.
- The Cout arrow exiting on the left, with the arrow tip filled.

Active cells use saturated colors (green for 1, gray for 0). Inactive cells (those the carry hasn't reached yet) are faded. The Cout arrow is red when the carry is 1, gray when 0 — making the ripple-carry chain visually obvious.

## 3.5 `RegisterViewPanel.java` — visualization for bitwise ops and shifts

Used for AND, OR, XOR, NOR, SLL, SRL, SRA. The visual style is different from the schematic: instead of showing gates explicitly, it shows the operand bits as a row of boxes, the gate operation as a row of gate boxes between A and B, and the result as a third row.

### `paintComponent(Graphics g0)`

Dispatches to `drawBitwise()` or `drawShift()` depending on the operation.

### `drawBitwise(Graphics2D g)`

Layout:
```
A      [0][1][1][0][1][1][0][0]
B      [1][0][1][0][1][1][1][1]
        AND AND AND AND AND AND AND AND     ← row of gate boxes
Result [0][0][1][0][1][1][0][0]
        Truth table:  0·0=0  0·1=0  1·0=0  1·1=1
```

Each gate box is animated — bits and the gate light up one-by-one from right (bit 0) to left (bit 7), reinforcing that this is a bitwise (per-bit independent) operation. The truth table at the bottom is a textbook reminder.

### `drawShift(Graphics2D g)`

Layout:
```
A (input)   [1][1][1][1][0][0][0][0]
             ←  ←  ←  ←  ←  ←  ←        ← arrows showing direction
Result      [1][1][1][1][1][1][0][0]    (for SRA by 2)
```

For SRA, an extra note at the bottom says "Sign bit (MSB) was 1 — SRA fills vacated MSBs with that." This is the visual proof that arithmetic shift preserves sign.

### Helper methods

- **`drawLabeledRow`**: draws a row label, 8 bit cells, and the decimal value at the right.
- **`drawBitCell`**: draws one bit box with the bit value and a small "bit i" index. Green if the bit is 1, gray if 0; faded if not yet activated by the animation.
- **`drawGateBox`**: draws a gate box with a label like "AND" or "XOR" inside.
- **`truthTable(op)`**: returns the truth table string for the current bitwise op.

---

# 4. End-to-end data flow

What happens between clicking the **Compute & Animate** button and seeing the result:

```
1.  User types A = "0b00001101", B = "0b00000110", selects ADD, clicks Compute.
                                    │
                                    ▼
2.  MainPanel.onRun() runs on the Event Dispatch Thread.
                                    │
                                    ▼
3.  ALUCore.parseOperand("0b00001101")  →  13
    ALUCore.parseOperand("0b00000110")  →  6
                                    │
                                    ▼
4.  ALUCore.compute(Op.ADD, 13, 6)  →  doAdd(ADD, 13, 6, false)
       Loop runs 8 times. At each bit i:
         ai = bit i of 13
         bi = bit i of 6
         sum_bit = ai XOR bi XOR carry
         cOut    = (ai AND bi) OR (carry AND (ai XOR bi))
         trace.add(FullAdderStep(i, ai, bi, carry, sum_bit, cOut))
         carry = cOut
       Builds the result (19 = 0b00010011), computes flags
       (Z=0, N=0, C=0, V=0), returns Result with full trace.
                                    │
                                    ▼
5.  MainPanel.showResult(r)
       resultBin.setText("00010011")
       resultDec.setText("19  (signed: 19)")
       resultHex.setText("0x13")
       lampZ.setOn(false); lampN.setOn(false); ...
       controlBits.setText("0010")
                                    │
                                    ▼
6.  Op is ADD, so vizLayout.show(vizCard, "adder")
    adderPanel.animate(r, speedSlider.getValue())
                                    │
                                    ▼
7.  AdderSchematicPanel.animate
       current = r
       highlightedBit = -1
       Start a Swing Timer firing every (900 - speed) ms.
       Each tick:  highlightedBit++; repaint()
       Stop when highlightedBit reaches WIDTH (8).
                                    │
                                    ▼
8.  paintComponent() runs on each repaint.
    Loops bit 0..7, calls drawFullAdder() for each.
    Cells with index <= highlightedBit drawn in saturated colors;
    others drawn faded. Carry arrows drawn red (1) or gray (0).
                                    │
                                    ▼
9.  User sees the carry ripple from right to left.
    When the animation completes, all 8 cells are fully lit
    and the result row at the bottom shows the binary sum.
```

The same flow works for every operation. Only step 6 differs — for bitwise ops and shifts, `registerPanel` is shown instead of `adderPanel`.

---

# 5. Likely viva questions and how to answer them

**Q: Why is the ALU important?**
A: It's the part of the processor that actually performs arithmetic and logic. Every MIPS instruction that does any computation — `add`, `sub`, `and`, `or`, `slt`, `lw`/`sw` (for address calculation), `beq` (for the zero comparison), even `jal` (for PC+4) — uses the ALU.

**Q: What does your visualizer show that a normal MARS simulation does not?**
A: MARS shows the ALU as a black box: you see `$rd` updated after `add $rd, $rs, $rt` runs, but nothing about how that addition happened. Our tool shows the internal full adders, the carry propagation, the gates, and how subtraction and SLT reuse the same adder hardware.

**Q: What is two's complement and why does it matter for the ALU?**
A: Two's complement is the encoding for signed integers where negating a number means inverting every bit and adding 1. It matters because it lets one adder hardware handle both addition and subtraction. Subtraction `A - B` becomes `A + (~B) + 1` — just invert B and set the carry-in to 1.

**Q: What's the difference between carry-out and overflow?**
A: Carry-out is an unsigned concept — it indicates that the unsigned sum exceeded the largest representable unsigned value. Overflow is a signed concept — it indicates that the signed sum stepped outside the representable signed range. They're set by different conditions and used for different things. For example, `0xFF + 0x01 = 0x00` produces carry-out (unsigned overflow) but no signed overflow because `-1 + 1 = 0` is correct in signed math.

**Q: Why is SLT done using subtraction?**
A: Because the sign of `A - B` tells you which is bigger. If `A - B < 0` then `A < B`. The XOR-with-overflow correction is needed because if signed overflow happens during the subtraction, the sign bit lies — XORing with the overflow flag corrects for it.

**Q: Why does SRA exist when SRL already does right shift?**
A: SRL fills the new high bits with 0, which is correct only if the operand is unsigned. For a signed negative number, that destroys the sign. SRA fills with the original sign bit, which keeps the number negative. SRA on a signed value is equivalent to dividing by 2^shamt (rounding toward negative infinity).

**Q: How does the ALU know which operation to perform?**
A: A 4-bit control signal called the ALU operation code. This signal is generated by the **ALU control unit**, which takes a 2-bit ALUOp signal from the main control unit (which only distinguishes broad instruction categories like R-type, load/store, branch) and the 6-bit funct field from the instruction (which distinguishes specific R-type operations).

**Q: What's the difference between single-cycle and multi-cycle datapaths in terms of the ALU?**
A: Single-cycle uses the ALU exactly once per instruction. Multi-cycle uses the same ALU multiple times across stages — IF (PC+4), EX (the actual operation or branch target), MEM (effective address). Single-cycle has one long clock cycle; multi-cycle has shorter cycles but more of them per instruction.

**Q: What would it take to make this a 32-bit ALU?**
A: Change the `WIDTH` constant in `ALUCore` from 8 to 32, and adjust the screen layout in `AdderSchematicPanel` and `RegisterViewPanel`. The simulation logic already uses `WIDTH` everywhere, so no code changes there. The visualization would need a horizontal scroll bar since 32 cells won't fit on a single screen.

**Q: Why isn't this integrated with MARS?**
A: It could be — MARS has a `mars.tools.AbstractMarsToolAndApplication` API for plugins that observe a running MIPS program. The `ALUCore` class is intentionally decoupled from the GUI so that a future plugin could call `ALUCore.compute()` whenever it detects an ALU instruction in the MARS execution trace. For the current scope of the lab project, the standalone tool was simpler and sufficient.

**Q: What's the biggest CA concept this project does *not* cover?**
A: Multiplication and division. Booth's algorithm (signed multiplication) and restoring division are typically separate, larger circuits inside an ALU. We scoped them out for time. The rest of the ALU — arithmetic, logical, shifts, comparison — is fully covered.

---

# 6. The Data Flow Tab (project expansion)

After the ALU Internals tool was reviewed as "too basic," we extended the project with a second tab: the **MIPS Data Flow Visualizer**. It uses the ALU we already built as the centerpiece of a fuller animated execution view — showing where operands come from (the register file), where the result goes (back to the register file, or to data memory), and animating value-blobs traveling along wires between the three boxes.

## 6.1 What the Data Flow tab shows

For any of these instructions:
- `add`, `sub`, `and`, `or`, `slt` (R-type)
- `addi` (I-type arithmetic)
- `lw` (load word)
- `sw` (store word)

…the tab animates the data flow step by step:

1. **Register read**: the cell of the source register (`$rs`, `$rt`) glows blue in the register file panel, and a yellow value-blob slides along a wire to the ALU's input port.
2. **Immediate** (for I-type): a purple blob appears near the ALU's input B with the sign-extended immediate value.
3. **ALU compute**: the ALU panel glows orange, displays the operation, and the result appears with its status flags (Z, N, C, V).
4. **Memory access** (for `lw`/`sw` only): a blob carries the computed address from the ALU to the memory panel; the active memory row glows green (read) or pink (write).
5. **Writeback**: a blob carries the final value back to the destination register, which glows orange and updates its displayed value.

A scrolling execution log at the bottom captures each step in text form, so the instructor sees the same story two ways: visually (animations) and textually (log).

## 6.2 Why this extension matters

The original ALU Internals tab answered: "How does the ALU compute the operation at the gate level?"
The Data Flow tab answers: "Where does the ALU's operand come from, and where does the result go?"

Together they cover:
- **Instruction formats** — the input bar accepts MIPS assembly; the executor decodes R-type vs I-type vs load/store and reads the corresponding fields (`rs`, `rt`, `rd`, `immediate`).
- **Datapath components** — the three main visible components in the textbook single-cycle datapath are present: **register file**, **ALU**, **data memory**.
- **Data flow** — animated wires show how values travel between components, which is the literal definition of "data path."
- **Hardware reuse** — the ALU is used for both arithmetic (`add`) and address calculation (`lw`/`sw`), demonstrating that the same physical ALU serves multiple instruction types.
- **Cross-tab linking** — the "Open Internals →" button hands the current operation to Tab 1, letting the instructor see the same ADD instruction *both* at the data-flow level and at the gate-level view.

## 6.3 New code — file by file

### `AppPanel.java`
Root content pane that hosts the two tabs (a `JTabbedPane`). Provides `showALUInternals()` and `showDataFlow()` methods so panels can switch tabs programmatically (used by the "Open Internals" button).

### `MachineState.java`
The simulated machine state: 32 registers (with `$zero` hardwired to 0) and a sparse `Map<address, value>` memory. Comes pre-seeded with demo values:
- `$t1 = 10`, `$t2 = 6`, `$t3 = -3`, `$t4 = 0x1000` (memory base), `$t5 = 0xFF`
- Memory at `0x1000` = `0xDEADBEEF`, `0x1004` = `0x12345678`, etc.

Static utilities `regName(idx)` and `regIndex(name)` convert between register indices and MIPS conventional names like `$t0`, `$sp`, `$ra`, etc.

### `ParsedInstruction.java`
A structured representation of one MIPS instruction. Fields: `kind` (R_TYPE / I_TYPE_ARITH / LOAD / STORE), `mnemonic`, `aluOp`, `rs`, `rt`, `rd`, `immediate`.

### `InstructionParser.java`
Hand-rolled parser for a small MIPS subset. Switches on the mnemonic, calls one of three sub-parsers (`parseRType`, `parseITypeArith`, `parseMemory`), produces a `ParsedInstruction`. Accepts `0x` hex, `0b` binary, or decimal immediates including negatives.

### `ExecutionStep.java`
One step in the animation. Has a `Type` enum (`READ_REG`, `IMMEDIATE`, `ALU_COMPUTE`, `MEM_READ`, `MEM_WRITE`, `WRITEBACK`) plus the fields each type needs (register index, address, value, ALU op, port name, etc.). Static factory methods (`readReg`, `immediate`, `aluCompute`, `memRead`, `memWrite`, `writebackFromALU`, `writebackFromMem`) build them.

### `InstructionExecutor.java`
The bridge between a `ParsedInstruction` + `MachineState` and a list of `ExecutionStep`s. For each instruction kind it emits the appropriate sequence:
- R-type → READ_REG (rs, A), READ_REG (rt, B), ALU_COMPUTE, WRITEBACK (rd)
- I-type arith → READ_REG (rs, A), IMMEDIATE, ALU_COMPUTE, WRITEBACK (rt)
- Load → READ_REG (rs, A), IMMEDIATE, ALU_COMPUTE (address), MEM_READ, WRITEBACK from MEM
- Store → READ_REG (rs, A), IMMEDIATE, ALU_COMPUTE (address), READ_REG (rt as data), MEM_WRITE

It also mutates the `MachineState` so that subsequent instructions see the result.

### `RegisterFilePanel.java`
The 32-register grid. 4 rows × 8 columns. Each cell shows the register number, current hex value, and conventional name. Methods `markRead(idx, port)`, `markWrite(idx, value)`, `clearHighlights()`, `refreshAll()`. Clicking a cell opens a dialog to edit its value (except `$zero`).

### `MemoryPanel.java`
Scrollable list of word-aligned memory rows with address, value, and ASCII columns. Methods `markRead(addr)`, `markWrite(addr, value)`, `clearHighlights()`. Auto-scrolls so the active address is visible.

### `MiniALUPanel.java`
A compact ALU representation for this tab. Shows operands, operation name, result, and flag lamps. The **"Open Internals →"** button triggers a callback (wired by `DataFlowPanel`) that switches the JTabbedPane back to Tab 1 with the same operands and operation.

### `ExecutionLogPanel.java`
Scrolling `JTextArea` with an instruction header line, then numbered steps. Automatically scrolls to the bottom as new steps arrive.

### `WireAnimation.java`
One in-flight animation. Holds source point, destination point, value label, color, duration. Tracks elapsed time and exposes `easedProgress()` (ease-in-out using a cosine curve) and `currentPos()` (interpolated point). One animation = one moving value-blob.

### `WiresOverlay.java`
Transparent JPanel that sits on top of the central content via a `JLayeredPane`. In `paintComponent`, draws static wires (light gray, if configured) and the current active `WireAnimation` — the wire highlighted in amber, plus a pill-shaped blob with the value text traveling along it.

### `DataFlowPanel.java`
The orchestrator. Holds all six new GUI panels, the instruction bar (combo + text field + speed slider + Run/Step/Reset buttons), the `WiresOverlay`, and the animation state machine:
- `pendingSteps` — the list of steps for the current instruction
- `stepCursor` — which step we're on
- `currentAnim` — the in-flight `WireAnimation`
- A 16ms `Timer` advances the animation each frame; when it completes, `advanceToNextStep()` moves on

The `Run` button plays all steps automatically; `Step` advances one at a time. `Reset` restores `MachineState` to its initial values and clears the log.

## 6.4 Animation flow per instruction

### `add $t0, $t1, $t2`
1. `$t1` cell glows blue → blob travels along top wire to ALU input A
2. `$t2` cell glows blue → blob travels along bottom wire to ALU input B
3. ALU box glows orange → result computed (16), flags update
4. Result blob travels along writeback wire → `$t0` cell glows orange and updates to 16

### `addi $t0, $t1, 5`
1. `$t1` cell glows blue → blob to ALU input A
2. Purple "imm" blob appears at ALU input B (representing sign-extended immediate)
3. ALU glows orange → result 15
4. Writeback to `$t0`

### `lw $t0, 4($t4)`
1. `$t4` cell glows blue → blob (0x1000) to ALU input A
2. Purple "imm" blob (4) to ALU input B
3. ALU computes address 0x1004
4. Address blob (green) travels to memory; row at 0x1004 glows green; value 0x12345678 is read
5. Writeback blob (orange) travels from memory back to `$t0`; cell glows orange with new value

### `sw $t1, 12($t4)`
1. `$t4` cell glows blue → blob to ALU input A
2. Purple "imm" blob (12) to ALU input B
3. ALU computes address 0x100C
4. `$t1` cell glows blue → data blob (pink) goes around the ALU toward memory
5. Address blob (pink) reaches memory; row at 0x100C glows pink with the new value

## 6.5 Demo script for the new tab

1. Open the tool, switch to **MIPS Data Flow** tab.
2. Pick `add $t0, $t1, $t2` from the dropdown, hit **Run**. Point out: $t1 lights up, blob travels, ALU lights up, result appears, blob goes back to $t0. *That's the R-type data path.*
3. Pick `addi $t0, $t1, 5`. Point out the purple immediate blob appearing at input B instead of a second register read. *That's the I-type data path.*
4. Pick `lw $t0, 4($t4)`. Point out the address calculation in the ALU, then the green blob to memory, then the orange writeback. *That's the load data path.*
5. Pick `sw $t1, 12($t4)`. Point out that nothing writes back to the register file — only memory updates. *That's the store data path.*
6. With the result still on screen, click **"Open Internals →"** in the ALU panel. Switches to Tab 1 with the same operands. *Now we see the same ADD operation at the gate level — full adders, carry propagation, the works.*

That's a complete narrative: instruction → format decode → component activation → ALU operation → writeback, plus the ability to drill into the ALU's internals on demand.

# Quick reference — file map

```
src/aluviz/
│
├── ── Bootstrap ──
├── Main.java                   Entry point: creates JFrame, sets up Swing
├── AppPanel.java               Tab host (ALU Internals + MIPS Data Flow)
│
├── ── Tab 1: ALU Internals ──
├── MainPanel.java              Inputs, results, flags, ALU control display
├── ALUCore.java                Pure simulation: full adder, all ops, flags, trace
├── AdderSchematicPanel.java    Schematic view (ADD/SUB/SLT) with visible gates
├── RegisterViewPanel.java      Register view (AND/OR/XOR/NOR/shifts)
│
├── ── Tab 2: MIPS Data Flow ──
├── DataFlowPanel.java          Orchestrator + instruction bar + animation state machine
├── MachineState.java           Simulated 32 registers + sparse memory
├── ParsedInstruction.java      Structured MIPS instruction
├── InstructionParser.java      Assembly text → ParsedInstruction
├── ExecutionStep.java          One animation/log step
├── InstructionExecutor.java    ParsedInstruction + MachineState → step sequence
├── RegisterFilePanel.java      32-register grid with read/write highlights
├── MemoryPanel.java            Scrollable memory view, read/write highlights
├── MiniALUPanel.java           Compact ALU view + "Open Internals" button
├── ExecutionLogPanel.java      Scrolling step log
├── WireAnimation.java          One in-flight value-blob (ease-in-out)
└── WiresOverlay.java           Transparent overlay drawing animated wires
```

# Quick reference — operation table

| Op  | ALU code | Internal mechanism |
|-----|----------|-------------------|
| ADD | 0010     | Ripple-carry add of A and B with Cin=0 |
| SUB | 0110     | Ripple-carry add of A and (~B) with Cin=1 |
| AND | 0000     | Bit-by-bit AND |
| OR  | 0001     | Bit-by-bit OR |
| XOR | 1101     | Bit-by-bit XOR |
| NOR | 1100     | Bit-by-bit NOR |
| SLT | 0111     | Subtract then output (N XOR V) |
| SLL | 1000     | Shift left, fill with 0 |
| SRL | 1001     | Shift right unsigned, fill with 0 |
| SRA | 1010     | Shift right signed, fill with sign bit |

# Quick reference — flag table

| Flag | When set | Used by |
|------|----------|---------|
| Z (Zero) | Result == 0 | `beq`, `bne` |
| N (Negative) | MSB of result is 1 | branch-on-less-than expansions, SLT |
| C (Carry) | Carry rippled out of MSB | unsigned overflow detection |
| V (Overflow) | Same-sign inputs, opposite-sign result | signed overflow detection, SLT correction |
