/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

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

    Details (including contact information) can be found at:

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    End of licence header
*/

package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.fpu64.FpuState64;

public class Pointer extends Address {
    final int base, index;
    final int scale;
    final int offset;
    final int segment;
    final boolean addrSize;

    public Pointer(Instruction.Operand op, int adr_mode) {
        if (op.base != null)
            base = Processor.getRegIndex(op.base);
        else
            base = -1;
        if (op.index != null)
            index = Processor.getRegIndex(op.index);
        else
            index = -1;
        if (op.scale != 0)
            scale = (int)op.scale;
        else if (op.scale == 0 && index != -1)
            scale = 1;
        else
            scale = 0;
        if (op.offset == 16)
            offset = (short)op.lval;
        else if (op.offset == 8)
            offset = (byte)op.lval;
        else
            offset = (int)op.lval;
        if (op.seg != null) {
            segment = Processor.getSegmentIndex(op.seg);
        } else
            segment = Processor.getSegmentIndex(getImplicitSegment(op));
        addrSize = adr_mode == 32;
    }

    public Pointer(int base, int index, int scale, int offset, int segment, boolean addrSize) {
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.offset = offset;
        this.segment = segment;
        this.addrSize = addrSize;
    }

    private static String getImplicitSegment(Instruction.Operand operand) {
        // :-)
        if (operand.toString().toLowerCase().contains("bp") || operand.toString().toLowerCase().contains("sp"))
            return "ss";
        else
            return "ds";
    }

    @Override
    public int getBase(Processor cpu) {
        return cpu.segs[segment].getBase();
    }

    @Override
    public int get(Processor cpu) {
        return get(cpu, 0);
    }

    public int get(Processor cpu, int off) {
        int addr = offset + off;
        if (addrSize) {
            if (base != -1)
                addr += cpu.regs[base].get32();
            if (scale != 0)
                addr += scale * cpu.regs[index].get32();
        } else {
            if (base != -1)
                addr += cpu.regs[base].get16();
            if (scale != 0)
                addr += scale * cpu.regs[index].get16();
            addr &= 0xFFFF;
        }
        return addr;
    }

    public double getF64(Processor cpu) {
        return Double.longBitsToDouble(get64(cpu));
    }

    public byte[] getF80(Processor cpu) {
        byte[] data = new byte[10];
        for (int i = 0; i < 10; i++)
            data[i] = get8(cpu, i);
        return data;
    }

    public void setF80(Processor cpu, byte[] val) {
        for (int i = 0; i < 10; i++)
            set8(cpu, i, val[i]);
    }

    public void setF80(Processor cpu, double val) {
        byte[] b = FpuState64.doubleToExtended(val, false);
        for (int i = 0; i < 10; i++)
            set8(cpu, i, b[i]);
    }

    public void setF64(Processor cpu, double val) {
        set64(cpu, Double.doubleToRawLongBits(val));
    }

    public long get64(Processor cpu) {
        return cpu.segs[segment].getQuadWord(get(cpu));
    }

    public void set64(Processor cpu, long val) {
        cpu.segs[segment].setQuadWord(get(cpu), val);
    }

    public float getF32(Processor cpu) {
        return Float.intBitsToFloat(get32(cpu));
    }

    public void setF32(Processor cpu, float val) {
        set32(cpu, Float.floatToRawIntBits(val));
    }

    public int get32(Processor cpu, int offset) {
        return cpu.segs[segment].getDoubleWord(get(cpu, offset));
    }

    public int get32(Processor cpu) {
        return cpu.segs[segment].getDoubleWord(get(cpu));
    }

    public void set32(Processor cpu, int off, int val) {
        cpu.segs[segment].setDoubleWord(get(cpu, off), val);
    }

    public void set32(Processor cpu, int val) {
        cpu.segs[segment].setDoubleWord(get(cpu), val);
    }

    public short get16(Processor cpu, int off) {
        return cpu.segs[segment].getWord(get(cpu, off));
    }

    public short get16(Processor cpu) {
        return cpu.segs[segment].getWord(get(cpu));
    }

    public void set16(Processor cpu, int off, short val) {
        cpu.segs[segment].setWord(get(cpu, off), val);
    }

    public void set16(Processor cpu, short val) {
        cpu.segs[segment].setWord(get(cpu), val);
    }

    public byte get8(Processor cpu, int off) {
        return cpu.segs[segment].getByte(get(cpu, off));
    }

    public byte get8(Processor cpu) {
        return cpu.segs[segment].getByte(get(cpu));
    }

    public void set8(Processor cpu, int off, byte val) {
        cpu.segs[segment].setByte(get(cpu, off), val);
    }

    public void set8(Processor cpu, byte val) {
        cpu.segs[segment].setByte(get(cpu), val);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (segment != -1)
            b.append(Processor.getSegmentString(segment) + ":");
        if (base != -1)
            b.append(Processor.getRegString(base));
        if (index != -1 && scale != 0) {
            b.append(String.format("+%s", Processor.getRegString(index)));
            if (scale != 1)
                b.append(String.format("*%d", scale));
        }
        if (offset != 0) {
            String fmt = base != -1 ? "%s%d" : addrSize ? "%s%08x" : "%s%04x";
            b.append(String.format(fmt, base != -1 && offset > 0 ? "+" : "", offset));
        }
        return b.toString();
    }
}
