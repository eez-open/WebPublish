/************************************************************************
 *
 *  UnicodeTableHandler.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2009-02-17) 
 * 
 */

package writer2latex.latex.i18n;

import java.util.Hashtable;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

// Helper classs: SAX handler to parse symbols.xml from jar
class UnicodeTableHandler extends DefaultHandler{
    private Hashtable tableSet; // collection of all tables
    private UnicodeTable table; // the current table
    private String sSymbolSets;
    private boolean bGlobalReadThisSet;
    private boolean bReadThisSet;
    private int nGlobalFontencs = 0; // The global fontencodings for current symbol set
    private int nFontencs = 0; // The currently active fontencodings
    private boolean b8bit = false;
    
    UnicodeTableHandler(Hashtable tableSet, String sSymbolSets){
        this.sSymbolSets = sSymbolSets;
        this.tableSet = tableSet;
    }
    
    public void startElement(String nameSpace, String localName, String qName, Attributes attributes){
        if (qName.equals("symbols")) {
            //root element - create root table!
            table = new UnicodeTable(null);
            tableSet.put("root",table);
        }
        else if (qName.equals("symbol-set")) {
            // start a new symbol set; maybe we want to include it?
            bGlobalReadThisSet = sSymbolSets.indexOf(attributes.getValue("name")) >= 0;
            bReadThisSet = bGlobalReadThisSet;
            // Change global and current fontencodings
            nGlobalFontencs = ClassicI18n.readFontencs(attributes.getValue("fontenc"));
            nFontencs = nGlobalFontencs;
        }
        else if (qName.equals("special-symbol-set")) {
            // start a new special symbol set; this requires a new table
            table = new UnicodeTable((UnicodeTable) tableSet.get("root"));
            tableSet.put(attributes.getValue("name"),table);

            // Read it if it requires nothing, or something we read
            bGlobalReadThisSet = attributes.getValue("requires")==null ||
                                 sSymbolSets.indexOf(attributes.getValue("requires")) >= 0;
            bReadThisSet = bGlobalReadThisSet;
            b8bit = "true".equals(attributes.getValue("eight-bit"));
            // Change global and current fontencodings
            nGlobalFontencs = ClassicI18n.readFontencs(attributes.getValue("fontenc"));
            nFontencs = nGlobalFontencs;
        }
        else if (qName.equals("symbol-subset")) {
            // Do we requires something here?
            if (attributes.getValue("requires")!=null) {
                bReadThisSet = sSymbolSets.indexOf(attributes.getValue("requires")) >= 0;
            }
            // Change current fontencodings
            nFontencs = ClassicI18n.readFontencs(attributes.getValue("fontenc"));
        }
        else if (qName.equals("symbol")) {
            if (bReadThisSet) {
                char c=(char)Integer.parseInt(attributes.getValue("char"),16);
                String sEqChar=attributes.getValue("eq-char");
                if (sEqChar!=null) { // copy existing definitions, if any
                    char eqc = (char)Integer.parseInt(sEqChar,16);
                    if (table.getCharType(eqc)!=UnicodeCharacter.UNKNOWN) {
                        table.addCharType(c,table.getCharType(eqc));
                    }
                    if (table.hasMathChar(eqc)) {
                        table.addMathChar(c,table.getMathChar(eqc));
                    }
                    if (table.hasTextChar(eqc)) {
                        table.addTextChar(c,table.getTextChar(eqc),table.getFontencs(eqc),table.isDashes(eqc));
                    }
                }
                else {
                    String sType=attributes.getValue("char-type");
                    String sMath=attributes.getValue("math");
                    String sText=attributes.getValue("text");
                    boolean bDashes="true".equals(attributes.getValue("dashes"));
                    if (sType!=null) table.addCharType(c,sType);
                    if (sMath!=null) table.addMathChar(c,sMath);
                    if (sText!=null) table.addTextChar(c,sText,nFontencs,bDashes);
                }
            }
        }
        else if (qName.equals("preserve-symbol")) {
            if (bReadThisSet) {
                String sMode=attributes.getValue("mode");
                char c=(char)Integer.parseInt(attributes.getValue("char"),16);
                table.addCharType(c,attributes.getValue("char-type"));
                if ("math".equals(sMode) || "both".equals(sMode)) {
                    table.addMathChar(c,Character.toString(c));
                }
                if ("text".equals(sMode) || "both".equals(sMode)) {
                    table.addTextChar(c,Character.toString(c),nFontencs,false);
                }
            }
        }
        else if (qName.equals("preserve-symbols")) {
            if (bReadThisSet) {
                String sMode=attributes.getValue("mode");
                String sType=attributes.getValue("char-type");
                char c1=(char)Integer.parseInt(attributes.getValue("first-char"),16);
                char c2=(char)Integer.parseInt(attributes.getValue("last-char"),16);
                boolean bMath = "math".equals(sMode) || "both".equals(sMode);
                boolean bText = "text".equals(sMode) || "both".equals(sMode);
                for (char c=c1; c<=c2; c++) {
                    table.addCharType(c,sType);
                    if (bMath) {
                        table.addMathChar(c,Character.toString(c));
                    }
                    if (bText) {
                        table.addTextChar(c,Character.toString(c),nFontencs,false);
                    }
                }
            }
        }
    }

    public void endElement(String nameSpace, String localName, String qName){
        if (qName.equals("symbol-subset")) {
            // Revert to global setting of reading status
            bReadThisSet = bGlobalReadThisSet;
            // Revert to global fontencoding
            nFontencs = nGlobalFontencs;
        }
        else if (qName.equals("special-symbol-set")) {
            if (b8bit) {
               // Row 0 = Row 240 (F0)
               // Note: 8-bit fonts are supposed to be relocated to F000..F0FF
               // This may fail on import from msword, hence this hack
               table.table[0] = table.table[240];               
            }
            b8bit = false;
        }
    }

}

