/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

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

    Details (including contact information) can be found at:

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.emulator.motherboard;

import java.io.DataInput;
import java.io.IOException;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.processor.Processor;

/**
 * I/O Device mapped to port 0x92 that controls the enabled status of the 20th address line.
 * @author Chris Dennis
 */
public class GateA20Handler extends AbstractHardwareComponent implements IODevice {
    private Processor cpu;
    private PhysicalAddressSpace physicalAddressSpace;
    private boolean ioportRegistered;

    public GateA20Handler() {
        ioportRegistered = false;
        cpu = null;
        physicalAddressSpace = null;
    }

    @Override
    public void loadState(DataInput input) throws IOException {
        ioportRegistered = false;
    }

    private void setGateA20State(boolean value) {
        physicalAddressSpace.setGateA20State(value);
    }

    /**
     * Writes a byte into the handler. Bit 1 controls the A20 state, if high A20 is enabled, if low A20
     * is disabled. If bit 0 is high then the processor will be reset.
     * @param address location being written to
     * @param data byte value being written
     */
    @Override
    public void ioPortWrite8(int address, int data) {
        setGateA20State((data & 0x02) != 0);
        if ((data & 0x01) != 0)
            cpu.reset();
    }

    @Override
    public void ioPortWrite16(int address, int data) {
        ioPortWrite8(address, data);
    }

    @Override
    public void ioPortWrite32(int address, int data) {
        ioPortWrite8(address, data);
    }

    /**
     * Reads a byte from the handler. If A20 is enabled then this will return 0x02, else it will return
     * 0x00.
     * @param address location being read
     * @return byte value read
     */
    @Override
    public int ioPortRead8(int address) {
        return physicalAddressSpace.getGateA20State() ? 0x02 : 0x00;
    }

    @Override
    public int ioPortRead16(int address) {
        return ioPortRead8(address) | 0xff00;
    }

    @Override
    public int ioPortRead32(int address) {
        return ioPortRead8(address) | 0xffffff00;
    }

    @Override
    public int[] ioPortsRequested() {
        return new int[] { 0x92 };
    }

    @Override
    public boolean initialised() {
        return ioportRegistered && cpu != null && physicalAddressSpace != null;
    }

    @Override
    public boolean updated() {
        return ioportRegistered && cpu.updated() && physicalAddressSpace.updated();
    }

    @Override
    public void updateComponent(HardwareComponent component) {
        if (component instanceof IOPortHandler) {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    @Override
    public void acceptComponent(HardwareComponent component) {
        if (component instanceof IOPortHandler && component.initialised()) {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }

        if (component instanceof PhysicalAddressSpace)
            physicalAddressSpace = (PhysicalAddressSpace)component;

        if (component instanceof Processor && component.initialised())
            cpu = (Processor)component;
    }

    @Override
    public void reset() {
        ioportRegistered = false;
        physicalAddressSpace = null;
        cpu = null;
    }
}
