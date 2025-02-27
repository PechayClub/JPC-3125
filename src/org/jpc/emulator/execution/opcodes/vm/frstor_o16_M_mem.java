/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 3.0

    A project by Ian Preston, ianopolous AT gmail.com

    Copyright (C) 2012-2013 Ian Preston

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Details (including current contact information) can be found at:

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    End of licence header
*/

package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.Modrm;
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.execution.decoder.Pointer;
import org.jpc.emulator.processor.Processor;

public class frstor_o16_M_mem extends Executable {
    final Pointer op1;

    public frstor_o16_M_mem(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    @Override
    public Branch execute(Processor cpu) {
        System.out.println("Warning: Using incomplete opcode: FRSTOR_94");
        int addr = op1.get(cpu);
        cpu.fpu.setControl(cpu.linearMemory.getWord(addr));
        cpu.fpu.setStatus(cpu.linearMemory.getWord(addr + 2));
        cpu.fpu.setTagWord(cpu.linearMemory.getWord(addr + 4));
        //cpu.linearMemory.setWord(addr + 6, (short) 0 /* cpu.fpu.getIP()  offset*/);
        //cpu.linearMemory.setWord(addr + 8, (short) 0 /* (selector & 0xFFFF)*/);
        //cpu.linearMemory.setWord(addr + 10, (short) 0 /* operand pntr offset*/);
        //cpu.linearMemory.setWord(addr + 12, (short) 0 /* operand pntr selector & 0xFFFF*/);
        //for (int i = 0; i < 8; i++) {
        //    byte[] extended = FpuState64.doubleToExtended(fpu.ST(i), false /* this is WRONG!!!!!!! */);
        //    for (int j = 0; j < 10; j++)
        //       seg0.setByte(addr0 + 14 + j + (10 * i), extended[j]);
        //}
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "frstor_o16" + " " + "[" + op1.toString() + "]";
    }
}
