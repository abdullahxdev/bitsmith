package aluviz;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a ParsedInstruction and current MachineState, produces an ordered list
 * of ExecutionSteps that the GUI animates, AND mutates the MachineState to
 * reflect the final result.
 */
public class InstructionExecutor {

    public static List<ExecutionStep> execute(ParsedInstruction instr, MachineState state) {
        List<ExecutionStep> steps = new ArrayList<>();

        switch (instr.kind) {
            case R_TYPE:
                executeRType(instr, state, steps);
                break;
            case I_TYPE_ARITH:
                executeITypeArith(instr, state, steps);
                break;
            case LOAD:
                executeLoad(instr, state, steps);
                break;
            case STORE:
                executeStore(instr, state, steps);
                break;
        }

        return steps;
    }

    private static void executeRType(ParsedInstruction instr, MachineState state,
                                     List<ExecutionStep> steps) {
        int a = state.readReg(instr.rs);
        int b = state.readReg(instr.rt);
        steps.add(ExecutionStep.readReg(instr.rs, a, "A"));
        steps.add(ExecutionStep.readReg(instr.rt, b, "B"));

        ALUCore.Result r = ALUCore.compute(instr.aluOp, a, b);
        steps.add(ExecutionStep.aluCompute(instr.aluOp, a, b, r.result));

        state.writeReg(instr.rd, r.result);
        steps.add(ExecutionStep.writebackFromALU(instr.rd, r.result));
    }

    private static void executeITypeArith(ParsedInstruction instr, MachineState state,
                                          List<ExecutionStep> steps) {
        int a = state.readReg(instr.rs);
        steps.add(ExecutionStep.readReg(instr.rs, a, "A"));
        steps.add(ExecutionStep.immediate(instr.immediate, "B"));

        ALUCore.Result r = ALUCore.compute(instr.aluOp, a, instr.immediate);
        steps.add(ExecutionStep.aluCompute(instr.aluOp, a, instr.immediate, r.result));

        state.writeReg(instr.rt, r.result);
        steps.add(ExecutionStep.writebackFromALU(instr.rt, r.result));
    }

    private static void executeLoad(ParsedInstruction instr, MachineState state,
                                    List<ExecutionStep> steps) {
        int base = state.readReg(instr.rs);
        steps.add(ExecutionStep.readReg(instr.rs, base, "A"));
        steps.add(ExecutionStep.immediate(instr.immediate, "B"));

        int address = base + instr.immediate;
        steps.add(ExecutionStep.aluCompute(ALUCore.Op.ADD, base, instr.immediate, address));

        int loaded = state.readMem(address);
        steps.add(ExecutionStep.memRead(address, loaded));

        state.writeReg(instr.rt, loaded);
        steps.add(ExecutionStep.writebackFromMem(instr.rt, loaded));
    }

    private static void executeStore(ParsedInstruction instr, MachineState state,
                                     List<ExecutionStep> steps) {
        int base = state.readReg(instr.rs);
        steps.add(ExecutionStep.readReg(instr.rs, base, "A"));
        steps.add(ExecutionStep.immediate(instr.immediate, "B"));

        int address = base + instr.immediate;
        steps.add(ExecutionStep.aluCompute(ALUCore.Op.ADD, base, instr.immediate, address));

        int data = state.readReg(instr.rt);
        steps.add(ExecutionStep.readReg(instr.rt, data, "data"));

        state.writeMem(address, data);
        steps.add(ExecutionStep.memWrite(address, data));
    }
}
