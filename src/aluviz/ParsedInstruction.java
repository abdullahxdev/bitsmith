package aluviz;

/**
 * A parsed MIPS instruction in a structured form. Only enough fields for the
 * 5 instruction shapes we support: R-type arith, addi, lw, sw.
 */
public class ParsedInstruction {

    public enum Kind { R_TYPE, I_TYPE_ARITH, LOAD, STORE }

    public final Kind kind;
    public final String mnemonic;     // "add", "sub", "addi", "lw", "sw", etc.
    public final ALUCore.Op aluOp;    // ALU operation this triggers
    public final int rs;              // source register 1 (always used)
    public final int rt;              // source register 2 (R-type) or destination (I-type)
    public final int rd;              // destination register (R-type only; -1 otherwise)
    public final int immediate;       // immediate value (I-type) or 0

    public ParsedInstruction(Kind kind, String mnemonic, ALUCore.Op aluOp,
                             int rs, int rt, int rd, int immediate) {
        this.kind = kind;
        this.mnemonic = mnemonic;
        this.aluOp = aluOp;
        this.rs = rs;
        this.rt = rt;
        this.rd = rd;
        this.immediate = immediate;
    }

    /** Returns the destination register's index (rd for R-type, rt for I-type loads/arith). */
    public int destinationRegister() {
        switch (kind) {
            case R_TYPE: return rd;
            case I_TYPE_ARITH:
            case LOAD: return rt;
            case STORE: return -1;
        }
        return -1;
    }

    @Override
    public String toString() {
        switch (kind) {
            case R_TYPE:
                return mnemonic + " " + MachineState.regName(rd) + ", "
                     + MachineState.regName(rs) + ", " + MachineState.regName(rt);
            case I_TYPE_ARITH:
                return mnemonic + " " + MachineState.regName(rt) + ", "
                     + MachineState.regName(rs) + ", " + immediate;
            case LOAD:
                return mnemonic + " " + MachineState.regName(rt) + ", "
                     + immediate + "(" + MachineState.regName(rs) + ")";
            case STORE:
                return mnemonic + " " + MachineState.regName(rt) + ", "
                     + immediate + "(" + MachineState.regName(rs) + ")";
        }
        return mnemonic;
    }
}
