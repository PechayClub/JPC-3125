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

package org.jpc.emulator.execution.opcodes.pm;

import static org.jpc.emulator.processor.Processor.getRegString;

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.Modrm;
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.Processor.Reg;
import org.jpc.emulator.processor.ProcessorException;
import org.jpc.emulator.processor.Segment;

public class ltr_Ew extends Executable {
    final int op1Index;

    public ltr_Ew(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1Index = Modrm.Ew(modrm);
    }

    @Override
    public Branch execute(Processor cpu) {
        Reg op1 = cpu.regs[op1Index];
        int selector = op1.get16();
        if ((selector & 0x4) != 0) //must be gdtr table
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

        Segment tempSegment = cpu.getSegment(selector);

        if (tempSegment.getType() != 0x01 && tempSegment.getType() != 0x09 || !tempSegment.isPresent())
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, selector, true);

        long descriptor = cpu.readSupervisorQuadWord(cpu.gdtr, selector & 0xfff8) | 0x1L << 41; // set busy flag in segment descriptor
        cpu.setSupervisorQuadWord(cpu.gdtr, selector & 0xfff8, descriptor);

        //reload segment
        cpu.tss = cpu.getSegment(selector);
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "ltr" + " " + getRegString(op1Index);
    }
}
