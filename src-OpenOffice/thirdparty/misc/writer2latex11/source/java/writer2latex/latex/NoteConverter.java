/************************************************************************
 *
 *  NoteConverter.java
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
 *  Copyright: 2002-2008 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2008-11-23)
 *
 */

package writer2latex.latex;

import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2latex.util.Misc;
import writer2latex.util.ExportNameCollection;
import writer2latex.office.OfficeReader;
import writer2latex.office.PropertySet;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

/**
 *  <p>This class handles conversion of footnotes and endnotes, including
 *  references. It takes advantage of the packages <code>endnotes.sty</code>
 *  and <code>perpage.sty</code> if allowed in the configuration.</p>
 */
public class NoteConverter extends ConverterHelper {

    private ExportNameCollection footnotenames = new ExportNameCollection(true);
    private ExportNameCollection endnotenames = new ExportNameCollection(true);
    private boolean bContainsEndnotes = false;
    private boolean bContainsFootnotes = false;
    // Keep track of footnotes (inside minipage etc.), that should be typeset later
    private LinkedList postponedFootnotes = new LinkedList();

    public NoteConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
    }

    /** <p>Append declarations needed by the <code>NoteConverter</code> to
     * the preamble.
     * @param pack the <code>LaTeXDocumentPortion</code> to which
     * declarations of packages should be added (<code>\\usepackage</code>).
     * @param decl the <code>LaTeXDocumentPortion</code> to which
     * other declarations should be added.
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bContainsEndnotes) { pack.append("\\usepackage{endnotes}").nl(); }
        if (bContainsFootnotes) convertFootnotesConfiguration(decl);
        if (bContainsEndnotes) convertEndnotesConfiguration(decl);
    }
	
    /** <p>Process a footnote (text:footnote tag)
     * @param node The element containing the footnote
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleFootnote(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();
        ic.setInFootnote(true);

        String sId = node.getAttribute(XMLString.TEXT_ID);
        Element fntbody = Misc.getChildByTagName(node,XMLString.TEXT_FOOTNOTE_BODY);
        if (fntbody==null) { // try oasis
            fntbody = Misc.getChildByTagName(node,XMLString.TEXT_NOTE_BODY);
        }
        if (fntbody != null) {
            bContainsFootnotes = true;
            if (ic.isNoFootnotes()) {
                ldp.append("\\footnotemark{}");
                postponedFootnotes.add(fntbody);
            }
            else {
                ldp.append("\\footnote");
	    		ldp.append("{");
		        if (sId != null && ofr.hasFootnoteRefTo(sId)) {
                    ldp.append("\\label{fnt:"+footnotenames.getExportName(sId)+"}");
                }
                traverseNoteBody(fntbody,ldp,ic);
                ldp.append("}");
            }
        }
	} 
	
    /** Flush the queue of postponed footnotes */
    public void flushFootnotes(LaTeXDocumentPortion ldp, Context oc) {
        // We may still be in a context with no footnotes
        if (oc.isNoFootnotes()) { return; }
        // Type out all postponed footnotes:
        Context ic = (Context) oc.clone();
        ic.setInFootnote(true);
        int n = postponedFootnotes.size();
        if (n==1) {
            ldp.append("\\footnotetext{");
            traverseNoteBody((Element) postponedFootnotes.get(0),ldp,ic);
            ldp.append("}").nl();
            postponedFootnotes.clear();
        }
        else if (n>1) {
            // Several footnotes; have to adjust the footnote counter
            ldp.append("\\addtocounter{footnote}{-"+n+"}").nl();
            for (int i=0; i<n; i++) {
                ldp.append("\\stepcounter{footnote}\\footnotetext{");
                traverseNoteBody((Element) postponedFootnotes.get(i),ldp,ic);
                ldp.append("}").nl();
            }
            postponedFootnotes.clear();
        }
    }
	
    /** <p>Process an endnote (text:endnote tag)
     * @param node The element containing the endnote
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleEndnote(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();
        ic.setInFootnote(true);

        String sId = node.getAttribute(XMLString.TEXT_ID);
        Element entbody = Misc.getChildByTagName(node,XMLString.TEXT_ENDNOTE_BODY);
        if (entbody==null) { // try oasis
            entbody = Misc.getChildByTagName(node,XMLString.TEXT_NOTE_BODY);
        }
        if (entbody != null) {
            if (ic.isNoFootnotes() && !config.useEndnotes()) {
                ldp.append("\\footnotemark()");
                postponedFootnotes.add(entbody);
            }
            else {
                if (config.useEndnotes()) {
                    ldp.append("\\endnote");
                    bContainsEndnotes = true;
                }
                else {
                    ldp.append("\\footnote");
                    bContainsFootnotes = true;
                }
	            ldp.append("{");
                if (sId != null && ofr.hasEndnoteRefTo(sId)) {
			        ldp.append("\\label{ent:"+endnotenames.getExportName(sId)+"}");
                }
                traverseNoteBody(entbody,ldp,ic);
                ldp.append("}");
            }
		}
	} 

    /** <p>Insert the endnotes into the documents.
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * the endnotes should be added.
     */
    public void insertEndnotes(LaTeXDocumentPortion ldp) {
        if (bContainsEndnotes) {
            ldp.append("\\clearpage").nl()
               .append("\\theendnotes").nl();
        }
    }
	

	
    /** <p>Process a note reference (text:note-ref tag, oasis)
     * @param node The element containing the note reference
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleNoteRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sClass=node.getAttribute(XMLString.TEXT_NOTE_CLASS);
        if (sClass.equals("footnote")) { handleFootnoteRef(node,ldp,oc); }
        else if (sClass.equals("endnote")) { handleEndnoteRef(node,ldp,oc); }
    } 
	
    /** <p>Process a footnote reference (text:footnote-ref tag)
     * @param node The element containing the footnote reference
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleFootnoteRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFormat = node.getAttribute(XMLString.TEXT_REFERENCE_FORMAT);
        String sName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (("page".equals(sFormat) || "".equals(sFormat)) && sName!=null) {
            ldp.append("\\pageref{fnt:"+footnotenames.getExportName(sName)+"}");
        }
        else if ("text".equals(sFormat) && sName!=null) {
            ldp.append("\\ref{fnt:"+footnotenames.getExportName(sName)+"}");
        }
        else { // use current value
            palette.getInlineCv().traversePCDATA(node,ldp,oc);
        }
    } 
        
    /** <p>Process an endnote reference (text:endnote-ref tag)
     * @param node The element containing the endnote reference
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleEndnoteRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFormat = node.getAttribute(XMLString.TEXT_REFERENCE_FORMAT);
        String sName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (("page".equals(sFormat) || "".equals(sFormat)) && sName!=null) {
            ldp.append("\\pageref{ent:"+endnotenames.getExportName(sName)+"}");
        }
        else if ("text".equals(sFormat) && sName!=null) {
            ldp.append("\\ref{ent:"+endnotenames.getExportName(sName)+"}");
        }
        else { // use current value
            palette.getInlineCv().traversePCDATA(node,ldp,oc);
        }
    }
	
    /** <p>Add a footnote name. The method <code>handleFootnote</code> includes
     * a <code>\label</code> only if the footnote name is already known to the
     * <code>NoteConverter</code>. Hence this method is invoked by the prepass
     * for each footnote reference. The end result is, that only necessary
     * labels will be included.  
     * @param sName the name (id) of the footnote 
     */
    public void addFootnoteName(String sName) { footnotenames.addName(sName); } 

    /** <p>Add an endnote name. The method <code>handleEndnote</code> includes
     * a <code>\label</code> only if the endnote name is already known to the
     * <code>NoteConverter</code>. Hence this method is invoked by the prepass
     * for each endnote reference. The end result is, that only necessary
     * labels will be included.  
     * @param sName the name (id) of the endnote 
     */
    public void addEndnoteName(String sName) { endnotenames.addName(sName); }
	
	/*
     * Process the contents of a footnote or endnote
     * TODO: Merge with BlockConverter.traverseBlockText?
     */
    private void traverseNoteBody (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (node.hasChildNodes()) {
            NodeList nList = node.getChildNodes();
            int len = nList.getLength();
            
            for (int i = 0; i < len; i++) {
                Node childNode = nList.item(i);
                
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element)childNode;
                    String nodeName = child.getTagName();

                    palette.getInfo().addDebugInfo(child,ldp);
                    
                    if (nodeName.equals(XMLString.TEXT_H)) {
                        palette.getHeadingCv().handleHeading(child,ldp,oc);
                    }

                    if (nodeName.equals(XMLString.TEXT_P)) {
                        palette.getInlineCv().traverseInlineText(child,ldp,oc);
                        if (i<len-1) {
                            if (nList.item(i+1).getNodeName().startsWith(XMLString.TEXT_)) {
                                ldp.append("\\par ");
                            }
                            else {
                                ldp.nl();
                            }
                        }
                    }
					
                    else if (nodeName.equals(XMLString.TEXT_LIST)) { // oasis
                        palette.getBlockCv().handleList(child,ldp,oc);
                    }

                    if (nodeName.equals(XMLString.TEXT_ORDERED_LIST)) {
                        palette.getBlockCv().handleList(child,ldp,oc);
                    }
                    
                    if (nodeName.equals(XMLString.TEXT_UNORDERED_LIST)) {
                        palette.getBlockCv().handleList(child,ldp,oc);
                    }
                }
            }
        }
    }
	
    // Convert footnotes configuration.
    private void convertFootnotesConfiguration(LaTeXDocumentPortion ldp) {
        // Note: Continuation notices are not supported in LaTeX
        // TODO: Support text:footnotes-postion="document" (footnotes as endnotes)
        // TODO: Support text:start-numbering-at="page" (footnpag.sty/footmisc.sty)
        convertFootEndnotesConfiguration(ofr.getFootnotesConfiguration(),"foot",ldp);
    }

    // Convert endnotes configuration.
    private void convertEndnotesConfiguration(LaTeXDocumentPortion ldp) {
        // Note: Continuation notices are not supported in LaTeX
        convertFootEndnotesConfiguration(ofr.getEndnotesConfiguration(),"end",ldp);
    }
	
    /* Convert {foot|end}notes configuration.
     * Note: All {foot|end}notes are formatted with the default style for {foot|end}footnotes.
     * (This doesn't conform with the file format specification, but in LaTeX
     * all {foot|end}notes are usually formatted in a fixed style.)
     */
    private void convertFootEndnotesConfiguration(PropertySet notes, String sType, LaTeXDocumentPortion ldp) {
        if (config.formatting()<LaTeXConfig.CONVERT_BASIC) { return; }
        String sTypeShort = sType.equals("foot") ? "fn" : "en";
        if (notes==null) { return; }
        ldp.append("% ").append(sType).append("notes configuration").nl()
           .append("\\makeatletter").nl();

        // The numbering style is controlled by \the{foot|end}note
		String sFormat = notes.getProperty(XMLString.STYLE_NUM_FORMAT);
        if (sFormat!=null) {
            ldp.append("\\renewcommand\\the").append(sType).append("note{")
               .append(ListStyleConverter.numFormat(sFormat))
               .append("{").append(sType).append("note}}").nl();
        }
        
        // Number {foot|end}notes by sections
        if ("chapter".equals(notes.getProperty(XMLString.TEXT_START_NUMBERING_AT))) {
            ldp.append("\\@addtoreset{").append(sType).append("note}{section}").nl();
        }
		
        // Set start value offset (default 0)
        int nStartValue = Misc.getPosInteger(notes.getProperty(XMLString.TEXT_START_VALUE),0);
        if (nStartValue!=0) {
            ldp.append("\\setcounter{").append(sType).append("note}{"+nStartValue+"}").nl();
        }
        
        if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
            // The formatting of the {foot|end}note citation is controlled by \@make{fn|en}mark
            String sCitBodyStyle = notes.getProperty(XMLString.TEXT_CITATION_BODY_STYLE_NAME);
            if (sCitBodyStyle!=null && ofr.getTextStyle(sCitBodyStyle)!=null) {
                BeforeAfter baText = new BeforeAfter();
                palette.getCharSc().applyTextStyle(sCitBodyStyle,baText,new Context());
                ldp.append("\\renewcommand\\@make").append(sTypeShort).append("mark{\\mbox{")
                   .append(baText.getBefore())
                   .append("\\@the").append(sTypeShort).append("mark")
                   .append(baText.getAfter())
                   .append("}}").nl();
            }
		
            // The layout and formatting of the {foot|end}note is controlled by \@make{fn|en}text
            String sCitStyle = notes.getProperty(XMLString.TEXT_CITATION_STYLE_NAME);
            String sStyleName = notes.getProperty(XMLString.TEXT_DEFAULT_STYLE_NAME);
            if (sStyleName!=null) {
                BeforeAfter baText = new BeforeAfter();
                palette.getCharSc().applyTextStyle(sCitStyle,baText,new Context());
                StyleWithProperties style = ofr.getParStyle(sStyleName);
                if (style!=null) {
                    BeforeAfter baPar = new BeforeAfter();
                    palette.getCharSc().applyHardCharFormatting(style,baPar);
                    ldp.append("\\renewcommand\\@make").append(sTypeShort)
                       .append("text[1]{\\noindent")
                       .append(baText.getBefore())
                       .append("\\@the").append(sTypeShort).append("mark\\ ")
                       .append(baText.getAfter())
                       .append(baPar.getBefore())
                       .append("#1")
                       .append(baPar.getAfter());
                    ldp.append("}").nl();
                }	 
            }
        }
        
        ldp.append("\\makeatother").nl();		
    }

		 
}