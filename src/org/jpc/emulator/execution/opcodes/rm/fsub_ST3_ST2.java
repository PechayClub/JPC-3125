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

package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.processor.Processor;

public class fsub_ST3_ST2 extends Executable {

    public fsub_ST3_ST2(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        int modrm = input.readU8();
    }

    @Override
    public Branch execute(Processor cpu) {
        double freg0 = cpu.fpu.ST(3);
        double freg1 = cpu.fpu.ST(2);
        if (freg0 == Double.NEGATIVE_INFINITY && freg1 == Double.NEGATIVE_INFINITY
            || freg0 == Double.POSITIVE_INFINITY && freg1 == Double.POSITIVE_INFINITY)
            cpu.fpu.setInvalidOperation();
        cpu.fpu.setST(3, freg0 - freg1);
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "fsub" + " " + "ST3" + ", " + "ST2";
    }
}
