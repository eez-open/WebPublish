/************************************************************************
 *
 *  MathmlConverter.java
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
 *  Version 1.0.2 (2010-04-29)
 *
 */

package writer2latex.latex;

//import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import writer2latex.latex.i18n.I18n;
import writer2latex.office.MIMETypes;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;
import writer2latex.xmerge.EmbeddedObject;
import writer2latex.xmerge.EmbeddedXMLObject;

/**
 *  This class converts mathml nodes to LaTeX.
 *  (Actually it only converts the starmath annotation currently, if available).
 */
public final class MathmlConverter extends ConverterHelper {
    
    private StarMathConverter smc;
	
    private boolean bContainsFormulas = false;
	
    public MathmlConverter(OfficeReader ofr,LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        smc = new StarMathConverter(palette.getI18n(),config);
    }

    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bContainsFormulas) {
            if (config.useOoomath()) {
                pack.append("\\usepackage{ooomath}").nl();
            }
            else {
                smc.appendDeclarations(pack,decl);
            }
        }
    }
	
    public String convert(Node settings, Node formula) {
        // TODO: Use settings to determine display mode/text mode
        // formula must be a math:math node
        // First try to find a StarMath annotation
    	Node semantics = Misc.getChildByTagName(formula,XMLString.SEMANTICS); // Since OOo 3.2
    	if (semantics==null) {
    		semantics = Misc.getChildByTagName(formula,XMLString.MATH_SEMANTICS);
    	}
		if (semantics!=null) {
			Node annotation = Misc.getChildByTagName(semantics,XMLString.ANNOTATION); // Since OOo 3.2
			if (annotation==null) {
				annotation = Misc.getChildByTagName(semantics,XMLString.MATH_ANNOTATION);
			}
            if (annotation!=null) {
                String sStarMath = "";
                if (annotation.hasChildNodes()) {
                    NodeList anl = annotation.getChildNodes();
                    int nLen = anl.getLength();
                    for (int i=0; i<nLen; i++) {
                        if (anl.item(i).getNodeType() == Node.TEXT_NODE) {
                            sStarMath+=anl.item(i).getNodeValue();
                        }
                    }
                    bContainsFormulas = true;      
                    return smc.convert(sStarMath);
                }
            }
        }
        // No annotation was found. In this case we should convert the mathml,
        // but currently we ignore the problem.
        // TODO: Investigate if Vasil I. Yaroshevich's MathML->LaTeX
        // XSL transformation could be used here. (Potential problem:
        // OOo uses MathML 1.01, not MathML 2)
        return "\\text{Warning: No StarMath annotation}";
    }
	
    // Data for display equations
    private Element theEquation = null;
    private Element theSequence = null;

    /**Try to convert a paragraph as a display equation:
     * A paragraph which contains exactly one formula + at most one sequence
     * number is treated as a display equation. Other content must be brackets
     * or whitespace (possible with formatting).
     * @param node the paragraph
     * @param ldp the LaTeXDocumentPortion to contain the converted equation
     * @return true if the conversion was succesful, false if the paragraph
     * did not contain a display equation
     */
    public boolean handleDisplayEquation(Element node, LaTeXDocumentPortion ldp) {
        theEquation = null;
        theSequence = null;
        if (parseDisplayEquation(node) && theEquation!=null) {
            if (theSequence!=null) {
                // Numbered equation
                ldp.append("\\begin{equation}");
                palette.getFieldCv().handleSequenceLabel(theSequence,ldp);
                ldp.nl()
                   .append(convert(null,theEquation)).nl()
                   .append("\\end{equation}").nl();
            }
            else {
                // Unnumbered equation
                ldp.append("\\begin{equation*}").nl()
                   .append(convert(null,theEquation)).nl()
                   .append("\\end{equation*}").nl();
            }
            return true;
        }
        else {
            return false;
        }
    }
	
    private boolean parseDisplayEquation(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            Node equation = getFormula(child);
            if (equation!=null) {
                if (theEquation==null) {
                    theEquation = (Element) equation;
                }
                else { // two or more equations -> not a display
                    return false;
                }
            }
            else if (Misc.isElement(child)) {
                String sName = child.getNodeName();
                if (XMLString.TEXT_SEQUENCE.equals(sName)) {
                    if (theSequence==null) {
                        theSequence = (Element) child;
                    }
                    else { // two sequence numbers -> not a display
                        return false;
                    }
                }
                else if (XMLString.TEXT_SPAN.equals(sName)) {
                    if (!parseDisplayEquation(child)) {
                        return false;
                    }
                }
                else if (XMLString.TEXT_S.equals(sName)) {
                    // Spaces are allowed
                }
                else if (XMLString.TEXT_TAB.equals(sName)) {
                    // Tab stops are allowed
                }
                else if (XMLString.TEXT_TAB_STOP.equals(sName)) { // old
                    // Tab stops are allowed
                }
                else {
                    // Other elements -> not a display
                    return false;
                }
            }
            else if (Misc.isText(child)) {
                String s = child.getNodeValue();
                int nLen = s.length();
                for (int i=0; i<nLen; i++) {
                    char c = s.charAt(i);
                    if (c!='(' && c!=')' && c!='[' && c!=']' && c!='{' && c!='}' && c!=' ' && c!='\u00A0') {
                        // Characters except brackets and whitespace -> not a display
                        return false;
                    }
                }
            }
            child = child.getNextSibling();
        }
        return true;
    }
	
    // TODO: Extend OfficeReader to handle frames
    private Node getFormula(Node node) {
        if (Misc.isElement(node,XMLString.DRAW_FRAME)) {
            node=Misc.getFirstChildElement(node);
        }

        String sHref = Misc.getAttribute(node,XMLString.XLINK_HREF);
		
        if (sHref!=null) { // Embedded object in package or linked object
            if (ofr.isInPackage(sHref)) { // Embedded object in package
                if (sHref.startsWith("#")) { sHref=sHref.substring(1); }
                if (sHref.startsWith("./")) { sHref=sHref.substring(2); }
                EmbeddedObject object = palette.getEmbeddedObject(sHref); 
                if (object!=null) {
                    if (MIMETypes.MATH.equals(object.getType()) || MIMETypes.ODF.equals(object.getType())) { // Formula!
                        try {
                            Document formuladoc = ((EmbeddedXMLObject) object).getContentDOM();
                            Element formula = Misc.getChildByTagName(formuladoc,XMLString.MATH); // Since OOo 3.2
                            if (formula==null) {
                            	formula = Misc.getChildByTagName(formuladoc,XMLString.MATH_MATH);
                            }
                            return formula;
                        }
                        catch (org.xml.sax.SAXException e) {
                            e.printStackTrace();
                        }
                        catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
	                }
                }
            }
        }
        else { // flat xml, object is contained in node
            Element formula = Misc.getChildByTagName(node,XMLString.MATH); // Since OOo 3.2
            if (formula==null) {
            	formula = Misc.getChildByTagName(node,XMLString.MATH_MATH);
            }
            return formula;
        }
        return null;
    }
	


}