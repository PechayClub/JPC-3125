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
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.processor.Processor;

public class fdivrp_ST4_ST7 extends Executable {

    public fdivrp_ST4_ST7(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        int modrm = input.readU8();
    }

    @Override
    public Branch execute(Processor cpu) {
        double freg0 = cpu.fpu.ST(4);
        double freg1 = cpu.fpu.ST(7);
        if (freg0 == 0.0 && freg1 == 0.0 || Double.isInfinite(freg0) && Double.isInfinite(freg1))
            cpu.fpu.setInvalidOperation();
        if (freg0 == 0.0 && !Double.isNaN(freg1) && !Double.isInfinite(freg1))
            cpu.fpu.setZeroDivide();
        cpu.fpu.setST(4, freg1 / freg0);
        cpu.fpu.pop();
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "fdivrp" + " " + "ST4" + ", " + "ST7";
    }
}
