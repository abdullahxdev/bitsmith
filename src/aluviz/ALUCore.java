package aluviz;

import java.util.ArrayList;
import java.util.List;

public class ALUCore {

    public static final int WIDTH = 8;

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

        public final String controlBits;
        public final String label;
        Op(String controlBits, String label) {
            this.controlBits = controlBits;
            this.label = label;
        }
    }

    public static class FullAdderStep {
        public final int index;
        public final int a, b, carryIn;
        public final int sum, carryOut;
        public FullAdderStep(int index, int a, int b, int carryIn, int sum, int carryOut) {
            this.index = index;
            this.a = a; this.b = b; this.carryIn = carryIn;
            this.sum = sum; this.carryOut = carryOut;
        }
    }

    public static class Result {
        public final Op op;
        public final int a, b;
        public final int aEffective, bEffective;
        public final int result;
        public final boolean zero, negative, carryOut, overflow;
        public final List<FullAdderStep> adderTrace;

        public Result(Op op, int a, int b, int aEffective, int bEffective,
                      int result, boolean zero, boolean negative,
                      boolean carryOut, boolean overflow,
                      List<FullAdderStep> adderTrace) {
            this.op = op;
            this.a = a; this.b = b;
            this.aEffective = aEffective; this.bEffective = bEffective;
            this.result = result;
            this.zero = zero; this.negative = negative;
            this.carryOut = carryOut; this.overflow = overflow;
            this.adderTrace = adderTrace;
        }
    }

    public static Result compute(Op op, int a, int b) {
        a = mask(a); b = mask(b);
        switch (op) {
            case ADD: return doAdd(op, a, b, false);
            case SUB: return doAdd(op, a, b, true);
            case AND: return doBitwise(op, a, b, a & b);
            case OR:  return doBitwise(op, a, b, a | b);
            case XOR: return doBitwise(op, a, b, a ^ b);
            case NOR: return doBitwise(op, a, b, mask(~(a | b)));
            case SLT: return doSlt(a, b);
            case SLL: return doShift(op, a, b);
            case SRL: return doShift(op, a, b);
            case SRA: return doShift(op, a, b);
        }
        throw new IllegalArgumentException("Unknown op " + op);
    }

    private static Result doAdd(Op op, int a, int b, boolean subtract) {
        int bEff = subtract ? mask(~b) : b;
        int carry = subtract ? 1 : 0;
        List<FullAdderStep> trace = new ArrayList<>();
        int sum = 0;
        for (int i = 0; i < WIDTH; i++) {
            int ai = (a >> i) & 1;
            int bi = (bEff >> i) & 1;
            int s = ai ^ bi ^ carry;
            int cOut = (ai & bi) | (carry & (ai ^ bi));
            sum |= (s << i);
            trace.add(new FullAdderStep(i, ai, bi, carry, s, cOut));
            carry = cOut;
        }
        int signA = (a >> (WIDTH - 1)) & 1;
        int signB = (bEff >> (WIDTH - 1)) & 1;
        int signR = (sum >> (WIDTH - 1)) & 1;
        boolean overflow = (signA == signB) && (signA != signR);
        boolean carryOut = carry == 1;
        boolean zero = sum == 0;
        boolean negative = signR == 1;
        return new Result(op, a, b, a, bEff, sum, zero, negative, carryOut, overflow, trace);
    }

    private static Result doBitwise(Op op, int a, int b, int r) {
        r = mask(r);
        boolean zero = r == 0;
        boolean negative = ((r >> (WIDTH - 1)) & 1) == 1;
        return new Result(op, a, b, a, b, r, zero, negative, false, false, null);
    }

    private static Result doSlt(int a, int b) {
        Result sub = doAdd(Op.SUB, a, b, true);
        int less = (sub.negative ^ sub.overflow) ? 1 : 0;
        boolean zero = less == 0;
        return new Result(Op.SLT, a, b, a, b, less, zero, false, false, false, sub.adderTrace);
    }

    private static Result doShift(Op op, int a, int b) {
        int shamt = b & (WIDTH - 1);
        int r;
        if (op == Op.SLL)      r = mask(a << shamt);
        else if (op == Op.SRL) r = (a & mask(0xFFFFFFFF)) >>> shamt;
        else { // SRA — sign-extend first
            int signed = (a << (32 - WIDTH)) >> (32 - WIDTH);
            r = mask(signed >> shamt);
        }
        boolean zero = r == 0;
        boolean negative = ((r >> (WIDTH - 1)) & 1) == 1;
        return new Result(op, a, b, a, b, r, zero, negative, false, false, null);
    }

    public static int mask(int v) { return v & ((1 << WIDTH) - 1); }

    public static String toBinary(int v) {
        StringBuilder sb = new StringBuilder();
        for (int i = WIDTH - 1; i >= 0; i--) sb.append((v >> i) & 1);
        return sb.toString();
    }

    public static int parseOperand(String s) {
        s = s.trim().toLowerCase();
        if (s.isEmpty()) return 0;
        if (s.startsWith("0x")) return mask(Integer.parseInt(s.substring(2), 16));
        if (s.startsWith("0b")) return mask(Integer.parseInt(s.substring(2), 2));
        // 8-character all-binary string treated as binary (e.g. "00001101")
        if (s.matches("[01]{8}")) return mask(Integer.parseInt(s, 2));
        // Negative decimals allowed (e.g. "-5" → 8-bit two's complement)
        return mask(Integer.parseInt(s));
    }
}
