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
import org.jpc.emulator.execution.decoder.Modrm;
import org.jpc.emulator.execution.decoder.PeekableInputStream;
import org.jpc.emulator.processor.Processor;

public class call_o16_Ap extends Executable {
    final int cs, targetEip;
    final int blockLength;
    final int instructionLength;

    public call_o16_Ap(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
        targetEip = Modrm.jmpOffset(prefices, input);
        cs = Modrm.jmpCs(input);
        instructionLength = (int)input.getAddress() - eip;
        blockLength = eip - blockStart + instructionLength;
    }

    @Override
    public Branch execute(Processor cpu) {
        cpu.eip += blockLength;
        cpu.callFar(cs, (short)targetEip);
        return Branch.Call_Unknown;
    }

    @Override
    public boolean isBranch() {
        return true;
    }

    @Override
    public String toString() {
        return "call_o16" + " " + Integer.toHexString(cs) + ":" + Integer.toHexString(targetEip);
    }
}
