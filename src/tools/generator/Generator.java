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

package tools.generator;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Generator {
    public static void main(String[] cmd) throws IOException {
        OpcodeWriter writer = new OpcodeWriter();
        int rm = opcodeParse(parseXML("RMPMVM"), "rm", writer);
        rm += opcodeParse(parseXML("RMVM"), "rm", writer);
        rm += opcodeParse(parseXML("RM"), "rm", writer);
        int vm = opcodeParse(parseXML("RMPMVM"), "vm", writer);
        vm += opcodeParse(parseXML("RMVM"), "vm", writer);
        vm += opcodeParse(parseXML("VM"), "vm", writer);
        int pm = opcodeParse(parseXML("RMPMVM"), "pm", writer);
        pm += opcodeParse(parseXML("PM"), "pm", writer);
        System.out.printf("Generated %d RM opcodes, %d VM opcodes and %d PM opcodes\n", rm, vm, pm);
    }

    public static int opcodeParse(Document dom, String mode, Callable call) {
        int count = 0;
        NodeList properties = dom.getElementsByTagName("jcc");
        String jcc = null;
        for (int i = 0; i < properties.getLength(); i++) {
            Node n = properties.item(i);
            String content = n.getTextContent();
            if (content.trim().length() > 0)
                jcc = content;
        }

        NodeList list = dom.getElementsByTagName("opcode");
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            String mnemonic = n.getAttributes().getNamedItem("mnemonic").getNodeValue();
            Node seg = n.getAttributes().getNamedItem("segment");
            boolean segment = seg != null && seg.getNodeValue().equals("true");
            Node memNode = n.getAttributes().getNamedItem("mem");
            boolean singleType = memNode != null;
            boolean mem = memNode != null && memNode.getNodeValue().equals("true");
            NodeList children = n.getChildNodes();
            String ret = null, snippet = null;
            // get return and snippet
            for (int j = 0; j < children.getLength(); j++) {
                Node c = children.item(j);
                if (c.getNodeName().equals("return"))
                    ret = c.getTextContent().trim();
                if (c.getNodeName().equals("snippet"))
                    snippet = c.getTextContent();
                if (c.getNodeName().equals("jcc"))
                    snippet += jcc;
            }
            if (ret == null)
                throw new IllegalStateException("No return value for " + mnemonic);
            if (snippet == null)
                throw new IllegalStateException("No snippet for " + mnemonic);

            // get each opcode definition
            for (int j = 0; j < children.getLength(); j++) {
                Node c = children.item(j);
                if (!c.getNodeName().equals("args"))
                    continue;
                String argsText = c.getTextContent();
                int size = Integer.parseInt(c.getAttributes().getNamedItem("size").getNodeValue());
                String[] args = argsText.split(";");
                if (argsText.length() == 0)
                    args = new String[0];
                List<Opcode> ops = Opcode.get(mnemonic, args, size, snippet, ret, segment, singleType, mem);
                for (Opcode op : ops) {
                    call.call(op, mode);
                    count++;
                }
            }
        }
        return count;
    }

    public static Document parseXML(String mode) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse("src/tools/generator/Opcodes_" + mode + ".xml");
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }
}
