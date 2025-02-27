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

import static org.jpc.emulator.processor.Processor.getRegString;

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.Modrm;
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.Processor.Reg;

public class ror_Eb_Ib extends Executable {
    final int op1Index;
    final int immb;

    public ror_Eb_Ib(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1Index = Modrm.Eb(modrm);
        immb = Modrm.Ib(input);
    }

    @Override
    public Branch execute(Processor cpu) {
        Reg op1 = cpu.regs[op1Index];
        int shift = immb & 8 - 1;
        int reg0 = 0xFF & op1.get8();
        int res = reg0 >>> shift | reg0 << 8 - shift;
        op1.set8((byte)res);
        boolean bit30 = (res & 1 << 8 - 2) != 0;
        boolean bit31 = (res & 1 << 8 - 1) != 0;
        if (shift > 0) {
            cpu.cf = bit31;
            cpu.of = bit30 ^ bit31;
            cpu.flagStatus &= NOFCF;
        }
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "ror" + " " + getRegString(op1Index) + ", " + Integer.toHexString(immb);
    }
}
