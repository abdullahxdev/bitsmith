package aluviz;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable simulated state: 32 MIPS registers + a sparse memory.
 * Word-addressed (4-byte aligned) for simplicity.
 */
public class MachineState {

    public static final int NUM_REGS = 32;
    public static final int MEM_BASE = 0x1000;
    public static final int MEM_WORDS_VISIBLE = 16;

    private final int[] registers = new int[NUM_REGS];
    private final Map<Integer, Integer> memory = new LinkedHashMap<>();

    public MachineState() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < NUM_REGS; i++) registers[i] = 0;
        memory.clear();

        // Demo defaults so example instructions show meaningful values
        registers[8]  = 0x00000000;       // $t0
        registers[9]  = 0x0000000A;       // $t1 = 10
        registers[10] = 0x00000006;       // $t2 = 6
        registers[11] = 0xFFFFFFFD;       // $t3 = -3
        registers[12] = MEM_BASE;         // $t4 = 0x1000 (memory base for lw/sw demos)
        registers[13] = 0x000000FF;       // $t5

        memory.put(MEM_BASE,       0xDEADBEEF);
        memory.put(MEM_BASE + 4,   0x12345678);
        memory.put(MEM_BASE + 8,   0x00000042);
        memory.put(MEM_BASE + 12,  0xCAFEBABE);
    }

    public int readReg(int idx) {
        if (idx < 0 || idx >= NUM_REGS) throw new IllegalArgumentException("Bad register " + idx);
        return registers[idx];
    }

    public void writeReg(int idx, int value) {
        if (idx < 0 || idx >= NUM_REGS) throw new IllegalArgumentException("Bad register " + idx);
        if (idx == 0) return;            // $zero is hardwired to 0
        registers[idx] = value;
    }

    public int readMem(int address) {
        return memory.getOrDefault(address, 0);
    }

    public void writeMem(int address, int value) {
        memory.put(address, value);
    }

    public int[] snapshotRegisters() {
        return registers.clone();
    }

    public Map<Integer, Integer> snapshotMemory() {
        return new LinkedHashMap<>(memory);
    }

    /** Human-readable name for register index (e.g. "$t0" for 8). */
    public static String regName(int idx) {
        switch (idx) {
            case 0: return "$zero";
            case 1: return "$at";
            case 2: case 3: return "$v" + (idx - 2);
            case 4: case 5: case 6: case 7: return "$a" + (idx - 4);
            case 28: return "$gp";
            case 29: return "$sp";
            case 30: return "$fp";
            case 31: return "$ra";
            default:
                if (idx >= 8 && idx <= 15) return "$t" + (idx - 8);
                if (idx >= 16 && idx <= 23) return "$s" + (idx - 16);
                if (idx >= 24 && idx <= 25) return "$t" + (idx - 24 + 8);
                if (idx >= 26 && idx <= 27) return "$k" + (idx - 26);
                return "$" + idx;
        }
    }

    /** Resolve a name like "$t0" or "$8" to its index. Throws on unknown. */
    public static int regIndex(String name) {
        String n = name.trim();
        if (n.startsWith("$")) n = n.substring(1);

        // Numeric form: $0..$31
        if (n.matches("\\d+")) {
            int idx = Integer.parseInt(n);
            if (idx >= 0 && idx < NUM_REGS) return idx;
            throw new IllegalArgumentException("Register out of range: $" + idx);
        }

        switch (n) {
            case "zero": return 0;
            case "at":   return 1;
            case "v0":   return 2;
            case "v1":   return 3;
            case "a0":   return 4;
            case "a1":   return 5;
            case "a2":   return 6;
            case "a3":   return 7;
            case "gp":   return 28;
            case "sp":   return 29;
            case "fp":   return 30;
            case "ra":   return 31;
        }
        if (n.startsWith("t") && n.length() > 1) {
            int t = Integer.parseInt(n.substring(1));
            if (t >= 0 && t <= 7) return 8 + t;
            if (t == 8 || t == 9) return 24 + (t - 8);
        }
        if (n.startsWith("s") && n.length() > 1) {
            int s = Integer.parseInt(n.substring(1));
            if (s >= 0 && s <= 7) return 16 + s;
        }
        if (n.startsWith("k") && n.length() > 1) {
            int k = Integer.parseInt(n.substring(1));
            if (k == 0 || k == 1) return 26 + k;
        }
        throw new IllegalArgumentException("Unknown register: " + name);
    }
}
