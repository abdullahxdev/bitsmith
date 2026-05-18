package aluviz;

/**
 * Simple hazard detector for pairs of instructions.
 * Detects RAW hazards and load-use (special RAW where previous instruction was a load).
 */
public class HazardDetector {

    public enum HazardType { NONE, RAW, LOAD_USE }

    public static class Result {
        public final HazardType type;
        public final int writer;   // destination reg index of producing instr (or -1)
        public final int[] readers; // source regs indices of consuming instr
        public final String suggestion;

        public Result(HazardType t, int w, int[] r, String s) {
            this.type = t; this.writer = w; this.readers = r; this.suggestion = s;
        }
    }

    /** Analyze two instructions: prev -> next. Returns NONE if no hazard. */
    public static Result analyze(ParsedInstruction prev, ParsedInstruction next) {
        if (prev == null || next == null) return new Result(HazardType.NONE, -1, new int[0], "");
        int w = prev.destinationRegister();
        if (w < 0) return new Result(HazardType.NONE, -1, new int[0], "");

        // Collect reader registers from `next`.
        java.util.List<Integer> readers = new java.util.ArrayList<>();
        switch (next.kind) {
            case R_TYPE:
                readers.add(next.rs); readers.add(next.rt); break;
            case I_TYPE_ARITH:
                readers.add(next.rs); break;
            case LOAD:
                readers.add(next.rs); break; // base register
            case STORE:
                readers.add(next.rs); readers.add(next.rt); break; // base and data reg
        }

        // Check overlap
        boolean raw = false;
        for (int r : readers) if (r == w) raw = true;
        if (!raw) return new Result(HazardType.NONE, -1, new int[0], "");

        // If prev is a load (LOAD kind), this is a load-use hazard
        if (prev.kind == ParsedInstruction.Kind.LOAD) {
            return new Result(HazardType.LOAD_USE, w, readers.stream().mapToInt(Integer::intValue).toArray(),
                "Load-use RAW hazard: destination " + MachineState.regName(w));
        }

        return new Result(HazardType.RAW, w, readers.stream().mapToInt(Integer::intValue).toArray(),
            "RAW hazard: destination " + MachineState.regName(w));
    }
}
