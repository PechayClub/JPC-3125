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

package org.jpc.emulator.execution.codeblock;

/**
 * @author Ian Preston
 */
public class OptimisedCompiler implements CodeBlockCompiler {

    public OptimisedCompiler() {

    }

    @Override
    public RealModeCodeBlock getRealModeCodeBlock(CodeBlock source) {
        return (RealModeCodeBlock)source;
    }

    @Override
    public ProtectedModeCodeBlock getProtectedModeCodeBlock(CodeBlock block) {

        return (ProtectedModeCodeBlock)block;
    }

    @Override
    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(CodeBlock block) {

        return (Virtual8086ModeCodeBlock)block;
    }
}
