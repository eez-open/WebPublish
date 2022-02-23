/************************************************************************
 *
 *  MathConverter.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0.1 (2010-02-28)
 *
 */

package writer2latex.xhtml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import writer2latex.office.*;

public class MathConverter extends ConverterHelper {

    private boolean bSupportMathML;
	
    public MathConverter(OfficeReader ofr, XhtmlConfig config, Converter converter,
        boolean bSupportMathML) {

        super(ofr,config,converter);
        this.bSupportMathML = bSupportMathML;
    }
	
    public void convert(Node onode, Node hnode) {
        if (bSupportMathML) {
            convertNode(onode,hnode);
        }
        else {
            Document htmlDOM = hnode.getOwnerDocument();
            NodeList annotationList = ((Element) onode).getElementsByTagName(XMLString.ANNOTATION); // Since OOo 3.2
            if (annotationList.getLength()==0) {
            	annotationList = ((Element) onode).getElementsByTagName(XMLString.MATH_ANNOTATION);
            }
            if (annotationList.getLength()>0 && annotationList.item(0).hasChildNodes()) {
                // Insert the StarMath annotation as a kbd element
                Element kbd = htmlDOM.createElement("kbd");
                hnode.appendChild(kbd);
                NodeList list = annotationList.item(0).getChildNodes();
                int nLen = list.getLength();
                for (int i=0; i<nLen; i++) {
                    Node child = list.item(i);
                    if (child.getNodeType()==Node.TEXT_NODE) {
                        kbd.appendChild(htmlDOM.createTextNode(child.getNodeValue()));
                    }
                }
            }
            else {
                hnode.appendChild(htmlDOM.createTextNode("[Warning: formula ignored]"));
            }
        }
    }

    public void convertNode(Node onode, Node hnode) {
        if (onode.getNodeType()==Node.ELEMENT_NODE) {
            if (onode.getNodeName().equals(XMLString.SEMANTICS)) { // Since OOo 3.2
                // ignore this construction
                convertNodeList(onode.getChildNodes(),hnode);
            }
            else if (onode.getNodeName().equals(XMLString.MATH_SEMANTICS)) {
                // ignore this construction
                convertNodeList(onode.getChildNodes(),hnode);
            }
            else if (onode.getNodeName().equals(XMLString.ANNOTATION)) { // Since OOo 3.2
                // ignore the annotation (StarMath) completely
                // (mozilla renders it for some reason)
            }
            else if (onode.getNodeName().equals(XMLString.MATH_ANNOTATION)) {
                // ignore the annotation (StarMath) completely
                // (mozilla renders it for some reason)
            }
            else {
                String sElementName = stripNamespace(onode.getNodeName());
                Element newNode = hnode.getOwnerDocument().createElement(sElementName);
                hnode.appendChild(newNode);
                if (onode.hasAttributes()) {
                    NamedNodeMap attr = onode.getAttributes();
                    int nLen = attr.getLength();
                    for (int i=0; i<nLen; i++) {
                        String sName = attr.item(i).getNodeName();
                        if (sName.equals("xmlns:math")) { sName="xmlns"; }
                        else { sName = stripNamespace(sName); }
                        String sValue = attr.item(i).getNodeValue();
                        newNode.setAttribute(sName,replacePrivateChars(sValue));
                    }
                }            
                convertNodeList(onode.getChildNodes(),newNode);
            }
        }
        else if (onode.getNodeType()==Node.TEXT_NODE) {
            String s = replacePrivateChars(onode.getNodeValue());
            hnode.appendChild(hnode.getOwnerDocument().createTextNode(s));
        }
    }
	
    private void convertNodeList(NodeList list, Node hnode) {
        if (list==null) { return; }
        int nLen = list.getLength();
        for (int i=0; i<nLen; i++) {
            convertNode(list.item(i),hnode);
        }
    }
	
    private String stripNamespace(String s) {
        int nPos = s.indexOf(':');
        if (nPos>-1) { return s.substring(nPos+1); }
        else { return s; }
    }
	
    // OOo exports some characters (from the OpenSymbol/StarSymbol font)
    // in the private use area of unicode. These should be replaced
    // with real unicode positions.
    private String replacePrivateChars(String s) {        
        int nLen = s.length();
        StringBuffer buf = new StringBuffer(nLen);
        for (int i=0; i<nLen; i++) {
            buf.append(replacePrivateChar(s.charAt(i)));
        }
        return buf.toString();
    }

    // This method maps {Open|Star}Symbol private use area to real unicode
    // positions. This is the same table as in w2l/latex/style/symbols.xml.
    // The named entities list is contributed by Bruno Mascret
    private char replacePrivateChar(char c) {
        switch (c) {
            case '\uE002': return '\u2666';
            case '\uE003': return '\u25C6';
            case '\uE005': return '\u274D';
            case '\uE006': return '\u2794';
            case '\uE007': return '\u2713';
            case '\uE008': return '\u25CF';
            case '\uE009': return '\u274D';
            case '\uE00A': return '\u25FC';
            case '\uE00B': return '\u2752';
            case '\uE00D': return '\u2756';
            case '\uE013': return '\u2742';
            case '\uE01B': return '\u270D';
            case '\uE01E': return '\u2022';
            case '\uE021': return '\u00A9';
            case '\uE024': return '\u00AE';
            case '\uE025': return '\u21E8';
            case '\uE026': return '\u21E9';
            case '\uE027': return '\u21E6';
            case '\uE028': return '\u21E7';
            case '\uE02B': return '\u279E';
            case '\uE032': return '\u2741';
            case '\uE036': return '\u0028';
            case '\uE037': return '\u0029';
            case '\uE03A': return '\u20AC';
            case '\uE080': return '\u2030';
            case '\uE081': return '\uFE38'; // underbrace
            case '\uE082': return '\uFE37'; // overbrace
            case '\uE083': return '\u002B';
            case '\uE084': return '\u003C';
            case '\uE085': return '\u003E';
            case '\uE086': return '\u2264';
            case '\uE087': return '\u2265';
            case '\uE089': return '\u2208';
            case '\uE08B': return '\u2026';
            case '\uE08C': return '\u2192';
            case '\uE090': return '\u2225';
            case '\uE091': return '\u005E';
            case '\uE092': return '\u02C7';
            case '\uE093': return '\u02D8';
            case '\uE094': return '\u00B4';
            case '\uE095': return '\u0060';
            case '\uE096': return '\u02DC'; // or 007E
            case '\uE097': return '\u00AF';
            case '\uE098': return '\u2192'; // or 21E1
            case '\uE09B': return '\u20DB'; // triple dot, neither MathPlayer nor Mozilla understands this glyph
            case '\uE09E': return '\u0028';
            case '\uE09F': return '\u0029';
            case '\uE0A0': return '\u2221';
            case '\uE0AA': return '\u2751';
            case '\uE0AC': return '\u0393';
            case '\uE0AD': return '\u0394';
            case '\uE0AE': return '\u0398';
            case '\uE0AF': return '\u039B';
            case '\uE0B0': return '\u039E';
            case '\uE0B1': return '\u03A0';
            case '\uE0B2': return '\u03A3';
            case '\uE0B3': return '\u03A5';
            case '\uE0B4': return '\u03A6';
            case '\uE0B5': return '\u03A8';
            case '\uE0B6': return '\u03A9';
            case '\uE0B7': return '\u03B1';
            case '\uE0B8': return '\u03B2';
            case '\uE0B9': return '\u03B3';
            case '\uE0BA': return '\u03B4';
            case '\uE0BB': return '\u03F5';
            case '\uE0BC': return '\u03B6';
            case '\uE0BD': return '\u03B7';
            case '\uE0BE': return '\u03B8';
            case '\uE0BF': return '\u03B9';
            case '\uE0C0': return '\u03BA';
            case '\uE0C1': return '\u03BB';
            case '\uE0C2': return '\u03BC';
            case '\uE0C3': return '\u03BD';
            case '\uE0C4': return '\u03BE';
            case '\uE0C5': return '\u03BF';
            case '\uE0C6': return '\u03C0';
            case '\uE0C7': return '\u03C1';
            case '\uE0C8': return '\u03C3';
            case '\uE0C9': return '\u03C4';
            case '\uE0CA': return '\u03C5';
            case '\uE0CB': return '\u03D5';
            case '\uE0CC': return '\u03C7';
            case '\uE0CD': return '\u03C8';
            case '\uE0CE': return '\u03C9';
            case '\uE0CF': return '\u03B5';
            case '\uE0D0': return '\u03D1';
            case '\uE0D1': return '\u03D6';
            case '\uE0D3': return '\u03C2';
            case '\uE0D4': return '\u03C6';
            case '\uE0D5': return '\u2202';
            case '\uE0D9': return '\u22A4';
            case '\uE0DB': return '\u2190';
            case '\uE0DC': return '\u2191';
            case '\uE0DD': return '\u2193';
            default: 
                return c;
        }
    }

}