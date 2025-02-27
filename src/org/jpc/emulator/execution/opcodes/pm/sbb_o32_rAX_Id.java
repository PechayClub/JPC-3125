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

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.UCodes;
import org.jpc.emulator.execution.decoder.Modrm;
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.processor.Processor;

public class sbb_o32_rAX_Id extends Executable {
    final int immd;

    public sbb_o32_rAX_Id(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        immd = Modrm.Id(input);
    }

    @Override
    public Branch execute(Processor cpu) {
        int add = cpu.cf() ? 1 : 0;
        cpu.flagOp1 = cpu.r_eax.get32();
        cpu.flagOp2 = immd;
        cpu.flagResult = cpu.flagOp1 - (cpu.flagOp2 + add);
        cpu.r_eax.set32(cpu.flagResult);
        cpu.flagIns = UCodes.SBB32;
        cpu.flagStatus = OSZAPC;
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "sbb_o32" + " " + "rAX" + ", " + Integer.toHexString(immd);
    }
}
