/************************************************************************
 *
 *  SectionConverter.java
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

package writer2latex.latex;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

/** <p>This class creates LaTeX code from OOo sections.
 *  <p>Sections are converted to multicols environments using <code>multicol.sty</code>
 */
public class SectionConverter extends ConverterHelper {

    // Do we need multicols.sty?
    private boolean bNeedMulticol = false;
	
    // Filenames for external sections
    private ExportNameCollection fileNames = new ExportNameCollection(true);

    /** <p>Constructs a new <code>SectionStyleConverter</code>.</p>
     */
    public SectionConverter(OfficeReader ofr, LaTeXConfig config,
        ConverterPalette palette) {
        super(ofr,config,palette);
    }
	
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bNeedMulticol) { pack.append("\\usepackage{multicol}").nl(); }
    }
	
    /** <p> Process a section (text:section tag)</p>
     * @param node The element containing the section
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleSection(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // We may need a hyperlink target, add this first
        palette.getFieldCv().addTarget(node,"|region",ldp);

        // Create new document, if desired
        String sFileName = null;
        Element source = Misc.getChildByTagName(node,XMLString.TEXT_SECTION_SOURCE);
        if (config.splitLinkedSections() && source!=null) {
            sFileName = fileNames.getExportName(Misc.removeExtension(Misc.urlDecode(source.getAttribute(XMLString.XLINK_HREF))));
        }
        else if (config.splitToplevelSections() && isToplevel(node)) {
            //sFileName = fileNames.getExportName(palette.getOutFileName()+node.getAttribute(XMLString.TEXT_NAME));
            sFileName = fileNames.getExportName(node.getAttribute(XMLString.TEXT_NAME));
        }

        LaTeXDocumentPortion sectionLdp = ldp;
        if (sFileName!=null) {
            LaTeXDocument newDoc = new LaTeXDocument(sFileName,config.getWrapLinesAfter());
            if (config.getBackend()!=LaTeXConfig.XETEX) {
                newDoc.setEncoding(ClassicI18n.writeJavaEncoding(config.getInputencoding()));            	
            }
            else {
                newDoc.setEncoding("UTF-8");            	            	
            }
            palette.addDocument(newDoc);
            sectionLdp = newDoc.getContents();
        }

        // Apply the style
        String sStyleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);
        BeforeAfter ba = new BeforeAfter();
        Context ic = (Context) oc.clone();
        applySectionStyle(sStyleName,ba,ic);
		
        // Do conversion
        ldp.append(ba.getBefore());
        if (sFileName!=null) {
            ldp.append("\\input{").append(sFileName).append("}").nl();
        }
        palette.getBlockCv().traverseBlockText(node,sectionLdp,ic);
        if (sectionLdp!=ldp) { sectionLdp.append("\\endinput").nl(); }
        ldp.append(ba.getAfter());
    }

    // Create multicols environment as needed
    private void applySectionStyle(String sStyleName, BeforeAfter ba, Context context) {
        StyleWithProperties style = ofr.getSectionStyle(sStyleName);
        // Don't nest multicols and require at least 2 columns
        if (context.isInMulticols() || style==null || style.getColCount()<2) { return; }
        int nCols = style.getColCount(); 
        bNeedMulticol = true;
        context.setInMulticols(true);
        ba.add("\\begin{multicols}{"+(nCols>10 ? 10 : nCols)+"}\n", "\\end{multicols}\n");
    }
	
    // return true if this node is *not* contained in a text:section element
    private boolean isToplevel(Node node) {
        Node parent = node.getParentNode();
        if (XMLString.TEXT_SECTION.equals(parent.getNodeName())) {
            return false;
        }
        else if (XMLString.OFFICE_BODY.equals(parent.getNodeName())) {
            return true;
        }
        return isToplevel(parent);
    }


    
}
