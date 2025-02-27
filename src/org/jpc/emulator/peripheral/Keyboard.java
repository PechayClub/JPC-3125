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

package org.jpc.emulator.peripheral;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jpc.emulator.AbstractHardwareComponent;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.Hibernatable;
import org.jpc.emulator.memory.LinearAddressSpace;
import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.motherboard.IODevice;
import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.emulator.motherboard.InterruptController;
import org.jpc.emulator.processor.Processor;
import org.jpc.j2se.Option;

/**
 * @author Chris Dennis
 */
public class Keyboard extends AbstractHardwareComponent implements IODevice {
    private static final Logger LOGGING = Logger.getLogger(Keyboard.class.getName());
    private static final boolean LOG_ACCESS = false;

    /* Keyboard Controller Commands */
    private static final byte KBD_CCMD_READ_MODE = (byte)0x20; /* Read mode bits */
    private static final byte KBD_CCMD_WRITE_MODE = (byte)0x60; /* Write mode bits */
    private static final byte KBD_CCMD_GET_VERSION = (byte)0xA1; /* Get controller version */
    private static final byte KBD_CCMD_MOUSE_DISABLE = (byte)0xA7; /* Disable mouse interface */
    private static final byte KBD_CCMD_MOUSE_ENABLE = (byte)0xA8; /* Enable mouse interface */
    private static final byte KBD_CCMD_TEST_MOUSE = (byte)0xA9; /* Mouse interface test */
    private static final byte KBD_CCMD_SELF_TEST = (byte)0xAA; /* Controller self test */
    private static final byte KBD_CCMD_KBD_TEST = (byte)0xAB; /* Keyboard interface test */
    private static final byte KBD_CCMD_KBD_DISABLE = (byte)0xAD; /* Keyboard interface disable */
    private static final byte KBD_CCMD_KBD_ENABLE = (byte)0xAE; /* Keyboard interface enable */
    private static final byte KBD_CCMD_READ_INPORT = (byte)0xC0; /* read input port */
    private static final byte KBD_CCMD_READ_OUTPORT = (byte)0xD0; /* read output port */
    private static final byte KBD_CCMD_WRITE_OUTPORT = (byte)0xD1; /* write output port */
    private static final byte KBD_CCMD_WRITE_OBUF = (byte)0xD2;
    private static final byte KBD_CCMD_WRITE_AUX_OBUF = (byte)0xD3; /* Write to output buffer as if initiated by the auxiliary device */
    private static final byte KBD_CCMD_WRITE_MOUSE = (byte)0xD4; /* Write the following byte to the mouse */
    private static final byte KBD_CCMD_DISABLE_A20 = (byte)0xDD; /* HP vectra only ? */
    private static final byte KBD_CCMD_ENABLE_A20 = (byte)0xDF; /* HP vectra only ? */
    private static final byte KBD_CCMD_RESET = (byte)0xFE;

    /* Keyboard Commands */
    private static final byte KBD_CMD_SET_LEDS = (byte)0xED; /* Set keyboard leds */
    private static final byte KBD_CMD_ECHO = (byte)0xEE;
    private static final byte KBD_CMD_GET_ID = (byte)0xF2; /* get keyboard ID */
    private static final byte KBD_CMD_SET_RATE = (byte)0xF3; /* Set typematic rate */
    private static final byte KBD_CMD_ENABLE = (byte)0xF4; /* Enable scanning */
    private static final byte KBD_CMD_RESET_DISABLE = (byte)0xF5; /* reset and disable scanning */
    private static final byte KBD_CMD_RESET_ENABLE = (byte)0xF6; /* reset and enable scanning */
    private static final byte KBD_CMD_RESET = (byte)0xFF; /* Reset */

    /* Keyboard Replies */
    private static final byte KBD_REPLY_POR = (byte)0xAA; /* Power on reset */
    private static final byte KBD_REPLY_ACK = (byte)0xFA; /* Command ACK */
    private static final byte KBD_REPLY_RESEND = (byte)0xFE; /* Command NACK, send the cmd again */

    /* Status Register Bits */
    private static final byte KBD_STAT_OBF = (byte)0x01; /* Keyboard output buffer full */
    private static final byte KBD_STAT_IBF = (byte)0x02; /* Keyboard input buffer full */
    private static final byte KBD_STAT_SELFTEST = (byte)0x04; /* Self test successful */
    private static final byte KBD_STAT_CMD = (byte)0x08; /* Last write was a command write (0=data) */
    private static final byte KBD_STAT_UNLOCKED = (byte)0x10; /* Zero if keyboard locked */
    private static final byte KBD_STAT_MOUSE_OBF = (byte)0x20; /* Mouse output buffer full */
    private static final byte KBD_STAT_GTO = (byte)0x40; /* General receive/xmit timeout */
    private static final byte KBD_STAT_PERR = (byte)0x80; /* Parity error */

    /* Controller Mode Register Bits */
    private static final int KBD_MODE_KBD_INT = 0x01; /* Keyboard data generate IRQ1 */
    private static final int KBD_MODE_MOUSE_INT = 0x02; /* Mouse data generate IRQ12 */
    private static final int KBD_MODE_SYS = 0x04; /* The system flag (?) */
    private static final int KBD_MODE_NO_KEYLOCK = 0x08; /* The keylock doesn't affect the keyboard if set */
    private static final int KBD_MODE_DISABLE_KBD = 0x10; /* Disable keyboard interface */
    private static final int KBD_MODE_DISABLE_MOUSE = 0x20; /* Disable mouse interface */
    private static final int KBD_MODE_KCC = 0x40; /* Scan code conversion to PC format */
    private static final int KBD_MODE_RFU = 0x80;

    /* Mouse Commands */
    private static final byte AUX_SET_SCALE11 = (byte)0xE6; /* Set 1:1 scaling */
    private static final byte AUX_SET_SCALE21 = (byte)0xE7; /* Set 2:1 scaling */
    private static final byte AUX_SET_RES = (byte)0xE8; /* Set resolution */
    private static final byte AUX_GET_SCALE = (byte)0xE9; /* Get scaling factor */
    private static final byte AUX_SET_STREAM = (byte)0xEA; /* Set stream mode */
    private static final byte AUX_POLL = (byte)0xEB; /* Poll */
    private static final byte AUX_RESET_WRAP = (byte)0xEC; /* Reset wrap mode */
    private static final byte AUX_SET_WRAP = (byte)0xEE; /* Set wrap mode */
    private static final byte AUX_SET_REMOTE = (byte)0xF0; /* Set remote mode */
    private static final byte AUX_GET_TYPE = (byte)0xF2; /* Get type */
    private static final byte AUX_SET_SAMPLE = (byte)0xF3; /* Set sample rate */
    private static final byte AUX_ENABLE_DEV = (byte)0xF4; /* Enable aux device */
    private static final byte AUX_DISABLE_DEV = (byte)0xF5; /* Disable aux device */
    private static final byte AUX_SET_DEFAULT = (byte)0xF6;
    private static final byte AUX_RESET = (byte)0xFF; /* Reset aux device */
    private static final byte AUX_ACK = (byte)0xFA; /* Command byte ACK. */

    private static final byte MOUSE_STATUS_REMOTE = (byte)0x40;
    private static final byte MOUSE_STATUS_ENABLED = (byte)0x20;
    private static final byte MOUSE_STATUS_SCALE21 = (byte)0x10;

    private static final int MOUSE_TYPE = 0; /* 0 = PS2, 3 = IMPS/2, 4 = IMEX */

    private static final int KBD_QUEUE_SIZE = 256;

    //Instance Variables
    private final KeyboardQueue queue;

    private byte commandWrite;
    private byte status;
    private int mode;
    /* keyboard state */
    private int keyboardWriteCommand;
    private boolean keyboardScanEnabled;
    /* mouse state */ //Split this off?
    private int mouseWriteCommand;
    private int mouseStatus;
    private int mouseResolution;
    private int mouseSampleRate;
    private boolean mouseWrap;
    private int mouseDetectState;
    private int mouseDx;
    private int mouseDy;
    private int mouseDz;
    private int mouseButtons;

    private boolean ioportRegistered;

    private InterruptController irqDevice;
    private Processor cpu;
    private PhysicalAddressSpace physicalAddressSpace;
    private LinearAddressSpace linearAddressSpace;

    public Keyboard() {
        ioportRegistered = false;
        queue = new KeyboardQueue();
        physicalAddressSpace = null;
        linearAddressSpace = null;
        cpu = null;
        reset();
    }

    @Override
    public void saveState(DataOutput output) throws IOException {
        output.writeByte(commandWrite);
        output.writeByte(status);
        output.writeByte(mode);
        output.writeInt(keyboardWriteCommand);
        output.writeBoolean(keyboardScanEnabled);
        output.writeInt(mouseWriteCommand);
        output.writeInt(mouseStatus);
        output.writeInt(mouseResolution);
        output.writeInt(mouseSampleRate);
        output.writeBoolean(mouseWrap);
        output.writeInt(mouseDetectState);
        output.writeInt(mouseDx);
        output.writeInt(mouseDy);
        output.writeInt(mouseDz);
        output.writeInt(mouseButtons);
        //dump keyboard queue
        queue.saveState(output);
    }

    @Override
    public void loadState(DataInput input) throws IOException {
        ioportRegistered = false;
        commandWrite = input.readByte();
        status = input.readByte();
        mode = input.readByte();
        keyboardWriteCommand = input.readInt();
        keyboardScanEnabled = input.readBoolean();
        mouseWriteCommand = input.readInt();
        mouseStatus = input.readInt();
        mouseResolution = input.readInt();
        mouseSampleRate = input.readInt();
        mouseWrap = input.readBoolean();
        mouseDetectState = input.readInt();
        mouseDx = input.readInt();
        mouseDy = input.readInt();
        mouseDz = input.readInt();
        mouseButtons = input.readInt();
        //load keyboard queue
        queue.loadState(input);
    }

    //IODevice Methods
    @Override
    public int[] ioPortsRequested() {
        return new int[] { 0x60, 0x64 };
    }

    @Override
    public int ioPortRead8(int address) {
        switch (address) {
        case 0x60:
            int res = readData();
            if (LOG_ACCESS)
                System.out.printf("Keyboard read address=%02x, result=%02x status=%02x\n", address, res, status);
            return res;
        case 0x64:
//                if (LOG_ACCESS)
//                    System.out.printf("Keyboard read address=%02x, result=%02x\n", address, status);
            return 0xff & status;
        default:
            return 0xffffffff;
        }
    }

    @Override
    public int ioPortRead16(int address) {
        return 0xff & ioPortRead8(address) | 0xff00 & ioPortRead8(address + 1);
    }

    @Override
    public int ioPortRead32(int address) {
        return 0xffffffff;
    }

    @Override
    public void ioPortWrite8(int address, int data) {
        switch (address) {
        case 0x60:
            writeData((byte)data);
            if (LOG_ACCESS)
                System.out.printf("Keyboard write address=%02x, data=%02x status=%02x\n", address, data, status);
            break;
        case 0x64:
            writeCommand((byte)data);
            if (LOG_ACCESS)
                System.out.printf("Keyboard write address=%02x, data=%02x status=%02x\n", address, data, status);
            break;
        default:
        }
    }

    @Override
    public void ioPortWrite16(int address, int data) {
        ioPortWrite8(address, data);
        ioPortWrite8(address + 1, data >> 8);
    }

    @Override
    public void ioPortWrite32(int address, int data) {
        ioPortWrite16(address, data);
        ioPortWrite16(address + 2, data >> 16);
    }

    @Override
    public void reset() {
        irqDevice = null;
        cpu = null;
        physicalAddressSpace = null;
        linearAddressSpace = null;
        ioportRegistered = false;

        keyboardWriteCommand = -1;
        mouseWriteCommand = -1;
        mode = KBD_MODE_KBD_INT | KBD_MODE_MOUSE_INT;
        status = (byte)(KBD_STAT_CMD | KBD_STAT_UNLOCKED);
        queue.reset();

        commandWrite = 0;
        keyboardWriteCommand = 0;
        keyboardScanEnabled = false;
        mouseWriteCommand = 0;
        mouseStatus = 0;
        mouseResolution = 0;
        mouseSampleRate = 0;
        mouseWrap = false;
        mouseDetectState = 0;
        mouseDx = 0;
        mouseDy = 0;
        mouseDz = 0;
        mouseButtons = 0;
    }

    private void setGateA20State(boolean value) {
        physicalAddressSpace.setGateA20State(value);
    }

    private byte readData() {
        byte val = queue.readData();
        updateIRQ();
        return val;
    }

    private void writeData(byte data) {
        if (LOG_ACCESS)
            System.out.printf("Write Data: commandWrite=%08x ", commandWrite);
        switch (commandWrite) {
        case 0:
            writeKeyboard(data);
            break;
        case KBD_CCMD_WRITE_MODE:
            mode = 0xff & data;
            updateIRQ();
            break;
        case KBD_CCMD_WRITE_OBUF:
            queue.writeData(data, (byte)0);
            break;
        case KBD_CCMD_WRITE_AUX_OBUF:
            queue.writeData(data, (byte)1);
            break;
        case KBD_CCMD_WRITE_OUTPORT:
            setGateA20State((data & 0x2) != 0);
            if (0x1 != (data & 0x1))
                cpu.reset();
            break;
        case KBD_CCMD_WRITE_MOUSE:
            writeMouse(data);
            break;
        default:
            break;
        }
        commandWrite = (byte)0x00;
        status &= ~KBD_STAT_CMD;
    }

    private void writeCommand(byte data) {
        switch (data) {
        case KBD_CCMD_READ_MODE:
            queue.writeData((byte)mode, (byte)0);
            break;
        case KBD_CCMD_WRITE_MODE:
        case KBD_CCMD_WRITE_OBUF:
        case KBD_CCMD_WRITE_AUX_OBUF:
        case KBD_CCMD_WRITE_MOUSE:
        case KBD_CCMD_WRITE_OUTPORT:
            commandWrite = data;
            break;
        case KBD_CCMD_MOUSE_DISABLE:
            mode |= KBD_MODE_DISABLE_MOUSE;
            break;
        case KBD_CCMD_MOUSE_ENABLE:
            mode &= ~KBD_MODE_DISABLE_MOUSE;
            break;
        case KBD_CCMD_TEST_MOUSE:
            queue.writeData((byte)0x00, (byte)0);
            break;
        case KBD_CCMD_SELF_TEST:
            status = (byte)(status & ~KBD_STAT_OBF | KBD_STAT_SELFTEST);
            queue.writeData((byte)0x55, (byte)0);
            break;
        case KBD_CCMD_KBD_TEST:
            queue.writeData((byte)0x00, (byte)0);
            break;
        case KBD_CCMD_KBD_DISABLE:
            mode |= KBD_MODE_DISABLE_KBD;
            updateIRQ();
            break;
        case KBD_CCMD_KBD_ENABLE:
            mode &= ~KBD_MODE_DISABLE_KBD;
            updateIRQ();
            break;
        case KBD_CCMD_READ_INPORT:
            queue.writeData((byte)0x00, (byte)0);
            break;
        case KBD_CCMD_READ_OUTPORT:
            /* XXX: check that */
            data = (byte)(0x01 | (physicalAddressSpace.getGateA20State() ? 0x02 : 0x00));
            if (0 != (status & KBD_STAT_OBF))
                data |= 0x10;
            if (0 != (status & KBD_STAT_MOUSE_OBF))
                data |= 0x20;
            queue.writeData(data, (byte)0);
            break;
        case KBD_CCMD_ENABLE_A20:
            setGateA20State(true);
            break;
        case KBD_CCMD_DISABLE_A20:
            setGateA20State(false);
            break;
        case KBD_CCMD_RESET:
            cpu.reset();
            break;
        case (byte)0xff:
            /* ignore that - I don't know what is its use */
            break;
        default:
            LOGGING.log(Level.INFO, "unsupported command 0x{0}", Integer.toHexString(0xff & data));
            break;
        }
    }

    private void writeKeyboard(byte data) {
        switch (keyboardWriteCommand) {
        default:
        case -1:
            switch (data) {
            case 0x00:
                queue.writeData(KBD_REPLY_ACK, (byte)0);
                break;
            case 0x05:
                queue.writeData(KBD_REPLY_RESEND, (byte)0);
                break;
            case KBD_CMD_GET_ID:
                synchronized (queue) {
                    queue.writeData(KBD_REPLY_ACK, (byte)0);
                    queue.writeData((byte)0xab, (byte)0);
                    queue.writeData((byte)0x83, (byte)0);
                }
                break;
            case KBD_CMD_ECHO:
                queue.writeData(KBD_CMD_ECHO, (byte)0);
                break;
            case KBD_CMD_ENABLE:
                keyboardScanEnabled = true;
                queue.writeData(KBD_REPLY_ACK, (byte)0);
                break;
            case KBD_CMD_SET_LEDS:
            case KBD_CMD_SET_RATE:
                keyboardWriteCommand = data;
                queue.writeData(KBD_REPLY_ACK, (byte)0);
                break;
            case KBD_CMD_RESET_DISABLE:
                resetKeyboard();
                keyboardScanEnabled = false;
                queue.writeData(KBD_REPLY_ACK, (byte)0);
                break;
            case KBD_CMD_RESET_ENABLE:
                resetKeyboard();
                keyboardScanEnabled = true;
                queue.writeData(KBD_REPLY_ACK, (byte)0);
                break;
            case KBD_CMD_RESET:
                resetKeyboard();
                synchronized (queue) {
                    queue.writeData(KBD_REPLY_ACK, (byte)0);
                    queue.writeData(KBD_REPLY_POR, (byte)0);
                }
                break;
            default:
                queue.writeData(KBD_REPLY_ACK, (byte)0);
                break;
            }
            break;
        case KBD_CMD_SET_LEDS:
            queue.writeData(KBD_REPLY_ACK, (byte)0);
            keyboardWriteCommand = -1;
            break;
        case KBD_CMD_SET_RATE:
            queue.writeData(KBD_REPLY_ACK, (byte)0);
            keyboardWriteCommand = -1;
            break;
        }
    }

    private void writeMouse(byte data) {
        switch (mouseWriteCommand) {
        default:
        case -1:
            /* mouse command */
            if (mouseWrap) {
                if (data == AUX_RESET_WRAP) {
                    mouseWrap = false;
                    queue.writeData(AUX_ACK, (byte)1);
                    return;
                } else if (data != AUX_RESET) {
                    queue.writeData(data, (byte)1);
                    return;
                }
            }
            switch (data) {
            case AUX_SET_SCALE11:
                mouseStatus &= ~MOUSE_STATUS_SCALE21;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_SET_SCALE21:
                mouseStatus |= MOUSE_STATUS_SCALE21;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_SET_STREAM:
                mouseStatus &= ~MOUSE_STATUS_REMOTE;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_SET_WRAP:
                mouseWrap = true;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_SET_REMOTE:
                mouseStatus |= MOUSE_STATUS_REMOTE;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_GET_TYPE:
                synchronized (queue) {
                    queue.writeData(AUX_ACK, (byte)1);
                    queue.writeData((byte)MOUSE_TYPE, (byte)1);
                }
                break;
            case AUX_SET_RES:
            case AUX_SET_SAMPLE:
                mouseWriteCommand = data;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_GET_SCALE:
                synchronized (queue) {
                    queue.writeData(AUX_ACK, (byte)1);
                    queue.writeData((byte)mouseStatus, (byte)1);
                    queue.writeData((byte)mouseResolution, (byte)1);
                    queue.writeData((byte)mouseSampleRate, (byte)1);
                }
                break;
            case AUX_POLL:
                synchronized (queue) {
                    queue.writeData(AUX_ACK, (byte)1);
                    mouseSendPacket();
                }
                break;
            case AUX_ENABLE_DEV:
                mouseStatus |= MOUSE_STATUS_ENABLED;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_DISABLE_DEV:
                mouseStatus &= ~MOUSE_STATUS_ENABLED;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_SET_DEFAULT:
                mouseSampleRate = 100;
                mouseResolution = 2;
                mouseStatus = 0;
                queue.writeData(AUX_ACK, (byte)1);
                break;
            case AUX_RESET:
                mouseSampleRate = 100;
                mouseResolution = 2;
                mouseStatus = 0;
                synchronized (queue) {
                    queue.writeData(AUX_ACK, (byte)1);
                    queue.writeData((byte)0xaa, (byte)1);
                    queue.writeData((byte)MOUSE_TYPE, (byte)1);
                }
                break;
            default:
                break;
            }
            break;
        case AUX_SET_SAMPLE:
            mouseSampleRate = data;
            queue.writeData(AUX_ACK, (byte)1);
            mouseWriteCommand = -1;
            break;
        case AUX_SET_RES:
            mouseResolution = data;
            queue.writeData(AUX_ACK, (byte)1);
            mouseWriteCommand = -1;
            break;
        }
    }

    private void resetKeyboard() {
        keyboardScanEnabled = true;
    }

    private void mouseSendPacket() {
        int dx1 = mouseDx;
        int dy1 = mouseDy;
        int dz1 = mouseDz;
        /* XXX: increase range to 8 bits ? */
        if (dx1 > 127)
            dx1 = 127;
        else if (dx1 < -127)
            dx1 = -127;
        if (dy1 > 127)
            dy1 = 127;
        else if (dy1 < -127)
            dy1 = -127;
        int x = 0;
        int y = 0;
        if (dx1 < 0)
            x = 1;
        if (dy1 < 0)
            y = 1;
        byte b = (byte)(0x08 | x << 4 | y << 5 | mouseButtons & 0x07);

        synchronized (queue) {
            queue.writeData(b, (byte)1);
            queue.writeData((byte)dx1, (byte)1);
            queue.writeData((byte)dy1, (byte)1);
            /* extra byte for IMPS/2 or IMEX */
            switch (MOUSE_TYPE) {
            default:
                break;
            case 3:
                if (dz1 > 127)
                    dz1 = 127;
                else if (dz1 < -127)
                    dz1 = -127;
                queue.writeData((byte)dz1, (byte)1);
                break;
            case 4:
                if (dz1 > 7)
                    dz1 = 7;
                else if (dz1 < -7)
                    dz1 = -7;
                b = (byte)(dz1 & 0x0f | (mouseButtons & 0x18) << 1);
                queue.writeData(b, (byte)1);
                break;
            }
        }
        /* update deltas */
        mouseDx -= dx1;
        mouseDy -= dy1;
        mouseDz -= dz1;
    }

    private void updateIRQ() {
        int irq1Level = 0;
        int irq12Level = 0;
        status = (byte)(status & ~(KBD_STAT_OBF | KBD_STAT_MOUSE_OBF));
        synchronized (queue) {
            if (queue.length != 0) {
                status = (byte)(status | KBD_STAT_OBF);
                if (0 != queue.getAux()) {
                    status = (byte)(status | KBD_STAT_MOUSE_OBF);
                    if (0 != (mode & KBD_MODE_MOUSE_INT))
                        if (!Option.useBochs.isSet())
                            irq12Level = 1;
                } else if (0 != (mode & KBD_MODE_KBD_INT) && 0 == (mode & KBD_MODE_DISABLE_KBD))
                    irq1Level = 1;
            }
        }
        irqDevice.setIRQ(1, irq1Level);
        irqDevice.setIRQ(12, irq12Level);
    }

    private class KeyboardQueue implements Hibernatable {
        private byte[] aux;
        private byte[] data;
        private int readPosition;
        private int writePosition;
        private int length;

        public KeyboardQueue() {
            aux = new byte[KBD_QUEUE_SIZE];
            data = new byte[KBD_QUEUE_SIZE];
            readPosition = 0;
            writePosition = 0;
            length = 0;
        }

        @Override
        public void saveState(DataOutput output) throws IOException {
            output.writeInt(aux.length);
            output.write(aux);
            output.writeInt(data.length);
            output.write(data);
            output.writeInt(readPosition);
            output.writeInt(writePosition);
            output.writeInt(length);
        }

        @Override
        public void loadState(DataInput input) throws IOException {
            int len = input.readInt();
            aux = new byte[len];
            input.readFully(aux, 0, len);
            len = input.readInt();
            data = new byte[len];
            input.readFully(data, 0, len);
            readPosition = input.readInt();
            writePosition = input.readInt();
            length = input.readInt();
        }

        public void reset() {
            synchronized (this) {
                readPosition = 0;
                writePosition = 0;
                length = 0;
            }
        }

        public byte getAux() {
            synchronized (this) {
                return aux[readPosition];
            }
        }

        public byte readData() {
            synchronized (this) {
                if (length == 0) {
                    /* NOTE: if no data left, we return the last keyboard one (needed for EMM386) */
                    /* XXX: need a timer to do things correctly */
                    int index = readPosition - 1;
                    if (index < 0)
                        index = KBD_QUEUE_SIZE - 1;
                    return data[index];
                }
                byte auxValue = this.aux[readPosition];
                byte dataValue = this.data[readPosition];
                if (++readPosition == KBD_QUEUE_SIZE)
                    readPosition = 0;
                length--;
                /* reading deasserts IRQ */
                if (0 != auxValue)
                    Keyboard.this.irqDevice.setIRQ(12, 0);
                else
                    Keyboard.this.irqDevice.setIRQ(1, 0);
                return dataValue;
            }
        }

        public void writeData(byte data, byte aux) {
            synchronized (this) {
                if (length >= KBD_QUEUE_SIZE) {
                    LOGGING.log(Level.INFO, "Keyboard buffer full, ignoring keycode: " + data);
                    return;
                }
                this.aux[writePosition] = aux;
                this.data[writePosition] = data;
                if (++writePosition == KBD_QUEUE_SIZE)
                    writePosition = 0;
                length++;
            }
            Keyboard.this.updateIRQ();
        }
    }

    public void keyPressed(byte scancode) {
        switch (scancode) {
        case (byte)0xff:
            synchronized (queue) {
                putKeyboardEvent((byte)0xe1);
                putKeyboardEvent((byte)0x1d);
                putKeyboardEvent((byte)0x45);
                putKeyboardEvent((byte)0xe1);
                putKeyboardEvent((byte)0x9d);
                putKeyboardEvent((byte)0xc5);
            }
            return;
        default:
            synchronized (queue) {
                if (scancode < 0)
                    putKeyboardEvent((byte)0xe0);
                putKeyboardEvent((byte)(scancode & 0x7f));
            }
        }
    }

    public void keyReleased(byte scancode) {
        synchronized (queue) {
            if (scancode < 0)
                putKeyboardEvent((byte)0xe0);
            putKeyboardEvent((byte)(scancode | 0x80));
        }
    }

    private void putKeyboardEvent(byte keycode) {
        queue.writeData(keycode, (byte)0);
    }

    public void putMouseEvent(int dx, int dy, int dz, int buttons) {
        if (0 == (mouseStatus & MOUSE_STATUS_ENABLED))
            return;

        mouseDx += dx;
        mouseDy -= dy;
        mouseDz += dz;

        mouseButtons = buttons;

        synchronized (queue) {
            if (0 == (mouseStatus & MOUSE_STATUS_REMOTE) && queue.length < KBD_QUEUE_SIZE - 16)
                while (true) {
                    /* if not remote, send event.  Multiple events are sent if too big deltas */
                    mouseSendPacket();
                    if (mouseDx == 0 && mouseDy == 0 && mouseDz == 0)
                        break;
                }
        }
    }

    @Override
    public boolean initialised() {
        return ioportRegistered && irqDevice != null && cpu != null && physicalAddressSpace != null && linearAddressSpace != null;
    }

    @Override
    public boolean updated() {
        return ioportRegistered && irqDevice.updated() && cpu.updated() && physicalAddressSpace.updated() && linearAddressSpace.updated();
    }

    @Override
    public void updateComponent(HardwareComponent component) {
        if (component instanceof IOPortHandler && component.updated()) {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    @Override
    public void acceptComponent(HardwareComponent component) {
        if (component instanceof InterruptController && component.initialised())
            irqDevice = (InterruptController)component;

        if (component instanceof IOPortHandler && component.initialised()) {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }

        if (component instanceof Processor && component.initialised())
            cpu = (Processor)component;

        if (component instanceof PhysicalAddressSpace)
            physicalAddressSpace = (PhysicalAddressSpace)component;

        if (component instanceof LinearAddressSpace)
            linearAddressSpace = (LinearAddressSpace)component;
    }
}
