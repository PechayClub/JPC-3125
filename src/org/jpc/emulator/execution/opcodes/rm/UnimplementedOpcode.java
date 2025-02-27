package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.Disassembler;
import org.jpc.emulator.execution.decoder.Instruction;
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.execution.decoder.Prefices;
import org.jpc.emulator.processor.Processor;

public class UnimplementedOpcode extends Executable {
    final int blockLength;
    final int instructionLength;
    String error;

    public UnimplementedOpcode(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        instructionLength = (int)input.getAddress() - eip;
        blockLength = (int)input.getAddress() - blockStart;
        input.seek(-instructionLength);
        Instruction in = Disassembler.disassemble(input, Prefices.isAddr16(prefices) ? 16 : 32);
        error = in.toString() + " (" + in.getGeneralClassName(false, false) + "), x86 byte = "
            + Disassembler.getRawBytes(input, eip - blockStart);
    }

    @Override
    public Branch execute(Processor cpu) {
        if (true)
            throw new IllegalStateException("Unimplemented opcode: " + error);
        return Branch.Jmp_Unknown;
    }

    @Override
    public boolean isBranch() {
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
