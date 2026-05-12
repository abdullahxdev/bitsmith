package aluviz;

/**
 * One animation/log step in the execution of a MIPS instruction.
 * A single instruction is broken into a sequence of these — the GUI plays
 * them one at a time, animating wires and highlighting components.
 */
public class ExecutionStep {

    public enum Type {
        READ_REG,         // value flowing FROM register file → ALU input
        IMMEDIATE,        // sign-extended immediate flowing → ALU input B
        ALU_COMPUTE,      // ALU performs its operation
        MEM_READ,         // memory → ALU/register (for lw)
        MEM_WRITE,        // ALU/register → memory (for sw)
        WRITEBACK         // ALU result or memory value → register file
    }

    public final Type type;
    public final String description;       // human-readable, shown in execution log
    public final int regIndex;              // valid for READ_REG, WRITEBACK
    public final int address;               // valid for MEM_READ, MEM_WRITE
    public final int value;                 // the actual data on the wire
    public final ALUCore.Op aluOp;          // valid for ALU_COMPUTE
    public final int aluA, aluB;            // valid for ALU_COMPUTE
    public final String aluPort;            // "A" or "B" for READ_REG / IMMEDIATE (which ALU input)
    public final String writebackSource;    // "ALU" or "MEM" for WRITEBACK

    private ExecutionStep(Type type, String description, int regIndex, int address,
                          int value, ALUCore.Op aluOp, int aluA, int aluB,
                          String aluPort, String writebackSource) {
        this.type = type;
        this.description = description;
        this.regIndex = regIndex;
        this.address = address;
        this.value = value;
        this.aluOp = aluOp;
        this.aluA = aluA;
        this.aluB = aluB;
        this.aluPort = aluPort;
        this.writebackSource = writebackSource;
    }

    public static ExecutionStep readReg(int regIndex, int value, String aluPort) {
        return new ExecutionStep(Type.READ_REG,
            "Read " + MachineState.regName(regIndex) + " → 0x" + hex(value)
            + "   (to ALU input " + aluPort + ")",
            regIndex, 0, value, null, 0, 0, aluPort, null);
    }

    public static ExecutionStep immediate(int value, String aluPort) {
        return new ExecutionStep(Type.IMMEDIATE,
            "Sign-extend immediate " + value + " → 0x" + hex(value)
            + "   (to ALU input " + aluPort + ")",
            -1, 0, value, null, 0, 0, aluPort, null);
    }

    public static ExecutionStep aluCompute(ALUCore.Op op, int a, int b, int result) {
        return new ExecutionStep(Type.ALU_COMPUTE,
            "ALU " + op.label + ": 0x" + hex(a) + " " + op.label + " 0x" + hex(b)
            + " = 0x" + hex(result),
            -1, 0, result, op, a, b, null, null);
    }

    public static ExecutionStep memRead(int address, int value) {
        return new ExecutionStep(Type.MEM_READ,
            "Memory read at 0x" + hex(address) + " → 0x" + hex(value),
            -1, address, value, null, 0, 0, null, null);
    }

    public static ExecutionStep memWrite(int address, int value) {
        return new ExecutionStep(Type.MEM_WRITE,
            "Memory write at 0x" + hex(address) + " ← 0x" + hex(value),
            -1, address, value, null, 0, 0, null, null);
    }

    public static ExecutionStep writebackFromALU(int regIndex, int value) {
        return new ExecutionStep(Type.WRITEBACK,
            "Writeback ALU result 0x" + hex(value) + " → " + MachineState.regName(regIndex),
            regIndex, 0, value, null, 0, 0, null, "ALU");
    }

    public static ExecutionStep writebackFromMem(int regIndex, int value) {
        return new ExecutionStep(Type.WRITEBACK,
            "Writeback memory value 0x" + hex(value) + " → " + MachineState.regName(regIndex),
            regIndex, 0, value, null, 0, 0, null, "MEM");
    }

    private static String hex(int v) {
        return String.format("%08X", v);
    }
}
