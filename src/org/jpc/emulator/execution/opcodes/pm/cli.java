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
import org.jpc.emulator.processor.ProcessorException;

public class cli extends Executable {

    public cli(int blockStart, int eip, int prefices, PeekableInputStream input) {
        super(blockStart, eip);
    }

    @Override
    public Branch execute(Processor cpu) {
        if (Processor.cpuLevel >= 5) {
            if ((cpu.getCR4() & 2) != 0 && cpu.getCPL() == 3) // Protected mode Virtual Interrupts enabled
            {
                if (cpu.getIOPrivilegeLevel() < 3) {
                    cpu.eflagsVirtualInterrupt = false;
                    return Branch.None;
                }
            }
        }
        if (Processor.cpuLevel < 5 || !((cpu.getCR4() & 2) != 0 && cpu.getCPL() == 3)) {
            if (cpu.getIOPrivilegeLevel() < cpu.getCPL())
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
        }
        cpu.eflagsInterruptEnable = false;
        return Branch.None;
    }

    @Override
    public boolean isBranch() {
        return false;
    }

    @Override
    public String toString() {
        return "cli";
    }
}
