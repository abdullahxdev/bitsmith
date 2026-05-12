package aluviz;

/**
 * Parses a small subset of MIPS assembly into ParsedInstruction objects.
 * Supported instructions:
 *   R-type:  add, sub, and, or, slt    (form: OP rd, rs, rt)
 *   I-type:  addi                       (form: addi rt, rs, imm)
 *   Load:    lw                         (form: lw rt, imm(rs))
 *   Store:   sw                         (form: sw rt, imm(rs))
 */
public class InstructionParser {

    public static ParsedInstruction parse(String line) {
        String s = line.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Empty instruction");

        // Split mnemonic from the rest
        int space = s.indexOf(' ');
        if (space < 0) throw new IllegalArgumentException("No operands: " + line);
        String mnem = s.substring(0, space).toLowerCase();
        String rest = s.substring(space + 1).trim();

        switch (mnem) {
            case "add": return parseRType(mnem, rest, ALUCore.Op.ADD);
            case "sub": return parseRType(mnem, rest, ALUCore.Op.SUB);
            case "and": return parseRType(mnem, rest, ALUCore.Op.AND);
            case "or":  return parseRType(mnem, rest, ALUCore.Op.OR);
            case "slt": return parseRType(mnem, rest, ALUCore.Op.SLT);
            case "addi": return parseITypeArith(mnem, rest, ALUCore.Op.ADD);
            case "lw":  return parseMemory(mnem, rest, ParsedInstruction.Kind.LOAD);
            case "sw":  return parseMemory(mnem, rest, ParsedInstruction.Kind.STORE);
            default: throw new IllegalArgumentException("Unsupported instruction: " + mnem);
        }
    }

    private static ParsedInstruction parseRType(String mnem, String rest, ALUCore.Op op) {
        // rd, rs, rt
        String[] parts = rest.split(",");
        if (parts.length != 3) throw new IllegalArgumentException("R-type expects 3 operands: " + mnem);
        int rd = MachineState.regIndex(parts[0]);
        int rs = MachineState.regIndex(parts[1]);
        int rt = MachineState.regIndex(parts[2]);
        return new ParsedInstruction(ParsedInstruction.Kind.R_TYPE, mnem, op, rs, rt, rd, 0);
    }

    private static ParsedInstruction parseITypeArith(String mnem, String rest, ALUCore.Op op) {
        // rt, rs, imm
        String[] parts = rest.split(",");
        if (parts.length != 3) throw new IllegalArgumentException("addi expects 3 operands");
        int rt = MachineState.regIndex(parts[0]);
        int rs = MachineState.regIndex(parts[1]);
        int imm = parseImmediate(parts[2]);
        return new ParsedInstruction(ParsedInstruction.Kind.I_TYPE_ARITH, mnem, op, rs, rt, -1, imm);
    }

    private static ParsedInstruction parseMemory(String mnem, String rest, ParsedInstruction.Kind kind) {
        // rt, imm(rs)
        int comma = rest.indexOf(',');
        if (comma < 0) throw new IllegalArgumentException("Memory op missing comma");
        int rt = MachineState.regIndex(rest.substring(0, comma));
        String rest2 = rest.substring(comma + 1).trim();

        int paren = rest2.indexOf('(');
        int close = rest2.indexOf(')');
        if (paren < 0 || close < 0) throw new IllegalArgumentException("Memory op needs imm(rs) form");
        int imm = parseImmediate(rest2.substring(0, paren));
        int rs  = MachineState.regIndex(rest2.substring(paren + 1, close));
        return new ParsedInstruction(kind, mnem, ALUCore.Op.ADD, rs, rt, -1, imm);
    }

    private static int parseImmediate(String s) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("-0x")) {
            boolean neg = s.startsWith("-");
            String hex = neg ? s.substring(3) : s.substring(2);
            int v = Integer.parseInt(hex, 16);
            return neg ? -v : v;
        }
        return Integer.parseInt(s);
    }
}
