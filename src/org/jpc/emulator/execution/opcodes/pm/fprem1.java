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

public class fprem1 extends Executable {

    public fprem1(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
    }

    @Override
    public Branch execute(Processor cpu) {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(1);
        int d = Math.getExponent(freg0) - Math.getExponent(freg1);
        if (d < 64) {
            // full remainder
            cpu.fpu.conditionCode &= ~4; // clear C2
            double z = Math.IEEEremainder(freg0, freg1);
            // compute least significant bits -> C0 C3 C1
            long i = (long)Math.rint(freg0 / freg1);
            cpu.fpu.conditionCode &= 4;
            if ((i & 1) != 0)
                cpu.fpu.conditionCode |= 2;
            if ((i & 2) != 0)
                cpu.fpu.conditionCode |= 8;
            if ((i & 4) != 0)
                cpu.fpu.conditionCode |= 1;
            cpu.fpu.setST(0, z);
        } else {
            // partial remainder
            cpu.fpu.conditionCode |= 4; // set C2
            int n = 63; // implementation dependent in manual
            double f = Math.pow(2.0, d - n);
            double z = freg0 / freg1 / f;
            double qq = z < 0 ? Math.ceil(z) : Math.floor(z);
            cpu.fpu.setST(0, freg0 - freg1 * qq * f);
        }
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "fprem1";
    }
}
