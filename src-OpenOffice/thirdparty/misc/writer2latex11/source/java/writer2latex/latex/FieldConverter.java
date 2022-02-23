/************************************************************************
 *
 *  FieldConverter.java
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

//import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.latex.util.Context; 
import writer2latex.latex.util.HeadingMap;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.ExportNameCollection;
import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/**
 *  This class handles text fields and links in the document.
 *  Packages: lastpage, hyperref, titleref, oooref (all optional)
 *  TODO: Need proper treatment of "caption" and "text" for sequence
 *  references not to figures and tables (should be fairly rare, though)

 */
public class FieldConverter extends ConverterHelper {

    // Links & references
    private ExportNameCollection targets = new ExportNameCollection(true);
    private ExportNameCollection refnames = new ExportNameCollection(true);
    private ExportNameCollection bookmarknames = new ExportNameCollection(true);
    private ExportNameCollection seqnames = new ExportNameCollection(true);
    private ExportNameCollection seqrefnames = new ExportNameCollection(true);
	
    // sequence declarations (maps name->text:sequence-decl element)
    private Hashtable seqDecl = new Hashtable();
    // first usage of sequence (maps name->text:sequence element)
    private Hashtable seqFirst = new Hashtable();
	
    private Vector postponedReferenceMarks = new Vector();
    private Vector postponedBookmarks = new Vector();

    private boolean bUseHyperref = false;
    private boolean bUsesPageCount = false;
    private boolean bUsesTitleref = false;
    private boolean bUsesOooref = false;
	
    public FieldConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        // hyperref.sty is not compatible with titleref.sty and oooref.sty:
        bUseHyperref = config.useHyperref() && !config.useTitleref() && !config.useOooref();
    }
	
    /** <p>Append declarations needed by the <code>FieldConverter</code> to
     * the preamble.</p>
     * @param pack the <code>LaTeXDocumentPortion</code> to which
     * declarations of packages should be added (<code>\\usepackage</code>).
     * @param decl the <code>LaTeXDocumentPortion</code> to which
     * other declarations should be added.
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        // use lastpage.sty
        if (bUsesPageCount) {
            pack.append("\\usepackage{lastpage}").nl();
        }
		
        // use titleref.sty
        if (bUsesTitleref) {
            pack.append("\\usepackage{titleref}").nl();
        } 

        // use oooref.sty
        if (bUsesOooref) {
            pack.append("\\usepackage[");
            HeadingMap hm = config.getHeadingMap();
            CSVList opt = new CSVList(",");
            for (int i=0; i<=hm.getMaxLevel(); i++) { opt.addValue(hm.getName(i)); }
            pack.append(opt.toString()).append("]{oooref}").nl();
        } 

        // use hyperref.sty
        if (bUseHyperref){
            pack.append("\\usepackage{hyperref}").nl();
            pack.append("\\hypersetup{");
            if (config.getBackend()==LaTeXConfig.PDFTEX) pack.append("pdftex, ");
            else if (config.getBackend()==LaTeXConfig.DVIPS) pack.append("dvips, ");
            //else pack.append("hypertex");
            pack.append("colorlinks=true, linkcolor=blue, citecolor=blue, filecolor=blue, urlcolor=blue");
            if (config.getBackend()==LaTeXConfig.PDFTEX) {
                pack.append(createPdfMeta("pdftitle",palette.getMetaData().getTitle()));
                if (config.metadata()) {
                    pack.append(createPdfMeta("pdfauthor",palette.getMetaData().getCreator()))
                        .append(createPdfMeta("pdfsubject",palette.getMetaData().getSubject()))
                        .append(createPdfMeta("pdfkeywords",palette.getMetaData().getKeywords()));
                }
            }
            pack.append("}").nl();
        }		
		
        // Export sequence declarations
        // The number format is fetched from the first occurence of the
        // sequence in the text, while the outline level and the separation
        // character are fetched from the declaration
        Enumeration names = seqFirst.keys();
        while (names.hasMoreElements()) {
            // Get first text:sequence element
            String sName = (String) names.nextElement();
            Element first = (Element) seqFirst.get(sName);
            // Collect data
            String sNumFormat = Misc.getAttribute(first,XMLString.STYLE_NUM_FORMAT);
            if (sNumFormat==null) { sNumFormat="1"; }
            int nLevel = 0;
            String sSepChar = ".";
            if (seqDecl.containsKey(sName)) {
                Element sdecl = (Element) seqDecl.get(sName);
                nLevel = Misc.getPosInteger(sdecl.getAttribute(XMLString.TEXT_DISPLAY_OUTLINE_LEVEL),0);
                if (sdecl.hasAttribute(XMLString.TEXT_SEPARATION_CHARACTER)) {
                    sSepChar = palette.getI18n().convert(
                        sdecl.getAttribute(XMLString.TEXT_SEPARATION_CHARACTER),
                        false,palette.getMainContext().getLang());
                }
            }
            // Create counter
            decl.append("\\newcounter{")
                .append(seqnames.getExportName(sName))
                .append("}");
            String sPrefix = "";
            if (nLevel>0) {
                HeadingMap hm = config.getHeadingMap();
                int nUsedLevel = nLevel<=hm.getMaxLevel() ? nLevel : hm.getMaxLevel();
                if (nUsedLevel>0) {
                    decl.append("[").append(hm.getName(nUsedLevel)).append("]");
                    sPrefix = "\\the"+hm.getName(nUsedLevel)+sSepChar;
                }
            }
            decl.nl()
                .append("\\renewcommand\\the")
                .append(seqnames.getExportName(sName))
                .append("{").append(sPrefix)
                .append(ListStyleConverter.numFormat(sNumFormat))
                .append("{").append(seqnames.getExportName(sName))
                .append("}}").nl();
        }
    }
	
    /** <p>Process sequence declarations</p>
     *  @param node the text:sequence-decls node
     */
    public void handleSequenceDecls(Element node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (Misc.isElement(child,XMLString.TEXT_SEQUENCE_DECL)) {
                // Don't process the declaration, but store a reference
                seqDecl.put(((Element)child).getAttribute(XMLString.TEXT_NAME),child);
            }
            child = child.getNextSibling();
        }
    }
	
    /** <p>Process a sequence field (text:sequence tag)</p>
     * @param node The element containing the sequence field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleSequence(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sName = Misc.getAttribute(node,XMLString.TEXT_NAME);
        String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
        String sFormula = Misc.getAttribute(node,XMLString.TEXT_FORMULA);
        if (sFormula==null) {
            // If there's no formula, we must use the content as formula
            // The parser below requires a namespace, so we add that..
            sFormula = "ooow:"+Misc.getPCDATA(node);
        }
        if (sName!=null) {
            if (ofr.isFigureSequenceName(sName) || ofr.isTableSequenceName(sName)) {
                // Export \label only, assuming the number is generated by \caption
                if (sRefName!=null && ofr.hasSequenceRefTo(sRefName)) {
                    ldp.append("\\label{seq:")
                       .append(seqrefnames.getExportName(sRefName))
                       .append("}");
                }
            }
            else {
                // General purpose sequence -> export as counter
                if (!seqFirst.containsKey(sName)) {
                    // Save first occurence -> used to determine number format
                    seqFirst.put(sName,node);
                }
                if (sRefName!=null && ofr.hasSequenceRefTo(sRefName)) {
                    // Export as {\refstepcounter{name}\thename\label{refname}}
                    ldp.append("{").append(changeCounter(sName,sFormula,true))
                       .append("\\the").append(seqnames.getExportName(sName))
                       .append("\\label{seq:")
                       .append(seqrefnames.getExportName(sRefName))
                       .append("}}");
                }
                else {
                    // Export as \stepcounter{name}{\thename}
                    ldp.append(changeCounter(sName,sFormula,false))
                       .append("{\\the")
                       .append(seqnames.getExportName(sName))
                       .append("}");
                }
            }
        }
    }
	
    /** <p>Create label for a sequence field (text:sequence tag)</p>
     * @param node The element containing the sequence field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     */
    public void handleSequenceLabel(Element node, LaTeXDocumentPortion ldp) {
        String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
        if (sRefName!=null && ofr.hasSequenceRefTo(sRefName)) {
            ldp.append("\\label{seq:")
               .append(seqrefnames.getExportName(sRefName))
               .append("}");
        }
    }

    // According to the spec for OpenDocument, the formula is application
    // specific, prefixed with a namespace. OOo uses the namespace ooow, and
    // we accept the formulas ooow:<number>, ooow:<name>, ooow:<name>+<number>
    // and ooow:<name>-<number>
    // Note: In OOo a counter is a 16 bit unsigned integer, whereas a (La)TeX
    // counter can be negative - thus there will be a slight deviation in the
    // (rare) case of a negative number
    private String changeCounter(String sName, String sFormula, boolean bRef) {
        if (sFormula!=null) { 
            sFormula = sFormula.trim();
            if (sFormula.startsWith("ooow:")) {
                SimpleInputBuffer input = new SimpleInputBuffer(sFormula.substring(5));
                if (input.peekChar()>='0' && input.peekChar()<='9') {
                    // Value is <number>
                    String sNumber = input.getInteger();
                    if (input.atEnd()) {
                        return setCounter(sName, Misc.getPosInteger(sNumber,0), bRef);
                    }
                }
                else if (input.peekChar()=='-') {
                    // Value is a negative <number>
                    input.getChar();
                    if (input.peekChar()>='0' && input.peekChar()<='9') {
                        String sNumber = input.getInteger();
                        if (input.atEnd()) {
                            return setCounter(sName, -Misc.getPosInteger(sNumber,0), bRef);
                        }
                    }
                }
                else {
                    // Value starts with <name>
                    String sToken = input.getIdentifier();
                    if (sToken.equals(sName)) {
                        input.skipSpaces();
                        if (input.peekChar()=='+') {
                            // Value is <name>+<number>
                            input.getChar();
                            input.skipSpaces();
                            String sNumber = input.getInteger();
                            if (input.atEnd()) {
                                return addtoCounter(sName, Misc.getPosInteger(sNumber,0), bRef);
                            }
                        }
                        else if (input.peekChar()=='-') {
                            // Value is <name>-<number>
                            input.getChar();
                            input.skipSpaces();
                            String sNumber = input.getInteger();
                            if (input.atEnd()) {
                                return addtoCounter(sName, -Misc.getPosInteger(sNumber,0), bRef);
                            }
                        }
                        else if (input.atEnd()) {
                            // Value is <name>
                            return addtoCounter(sName, 0, bRef);
                        }
                    }
                }
            }
        }
        // No formula, or a formula we don't understand -> use default behavior
        return stepCounter(sName, bRef);
    }
	
    private String stepCounter(String sName, boolean bRef) {
        if (bRef) {
            return "\\refstepcounter{" + seqnames.getExportName(sName) + "}";
        }
        else {
            return "\\stepcounter{" + seqnames.getExportName(sName) + "}";
        }
    }
	
    private String addtoCounter(String sName, int nValue, boolean bRef) {
        if (nValue==1) {
            return stepCounter(sName, bRef);
        }
        else if (bRef) {
            return "\\addtocounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue-1) + "}"
                 + "\\refstepcounter{" + seqnames.getExportName(sName) + "}";
        }
        else if (nValue!=0) {
            return "\\addtocounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue) + "}";
        }
        else {
            return "";
        }
    }
	
    private String setCounter(String sName, int nValue, boolean bRef) {
        if (bRef) {
            return "\\setcounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue-1) + "}"
                 + "\\refstepcounter{" + seqnames.getExportName(sName) + "}";
        }
        else {
            return "\\setcounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue) + "}";
        }
    }
	
    /** <p>Process a sequence reference (text:sequence-ref tag)</p>
     * @param node The element containing the sequence reference 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleSequenceRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
        String sFormat = Misc.getAttribute(node,XMLString.TEXT_REFERENCE_FORMAT);
        String sName = ofr.getSequenceFromRef(sRefName);
        if (sRefName!=null) {
            if (sFormat==null || "page".equals(sFormat)) {
                ldp.append("\\pageref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
            }
            else if ("value".equals(sFormat)) {
                ldp.append("\\ref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
            }
            else if ("category-and-value".equals(sFormat)) {
                // Export as Name~\\ref{refname}
                if (sName!=null) {
                    if (ofr.isFigureSequenceName(sName)) {
                        ldp.append("\\figurename~");
                    }
                    else if (ofr.isTableSequenceName(sName)) {
                        ldp.append("\\tablename~");
                    }
                    else {
                        ldp.append(sName).append("~");
                    }
                }
                ldp.append("\\ref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
            }
            else if ("chapter".equals(sFormat) && config.useOooref()) {
                ldp.append("\\chapterref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
                bUsesOooref = true;
            }
            else if ("caption".equals(sFormat) && config.useTitleref() &&
                    (ofr.isFigureSequenceName(sName) || ofr.isTableSequenceName(sName))) {
                ldp.append("\\titleref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
                bUsesTitleref = true;
            }
            else if ("text".equals(sFormat) && config.useTitleref() &&
                    (ofr.isFigureSequenceName(sName) || ofr.isTableSequenceName(sName))) {
                // This is a combination of "category-and-value" and "caption"
                // Export as \\figurename~\ref{refname}:~\titleref{refname}
                if (ofr.isFigureSequenceName(sName)) {
                    ldp.append("\\figurename");
                }
                else if (ofr.isTableSequenceName(sName)) {
                    ldp.append("\\tablename");
                }
                ldp.append("~\\ref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}:~\\titleref{")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
                bUsesTitleref = true;
            }
            else { // use current value
                palette.getInlineCv().traversePCDATA(node,ldp,oc);
            }
        }
    } 


    /** <p>Process a reference mark (text:reference-mark tag)</p>
     * @param node The element containing the reference mark 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleReferenceMark(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            // Note: Always include \label here, even when it's not used
            String sName = node.getAttribute(XMLString.TEXT_NAME);
            if (sName!=null) {
                ldp.append("\\label{ref:"+refnames.getExportName(sName)+"}");
            }
        }
        else {
            // Reference marks should not appear within \section or \caption
            postponedReferenceMarks.add(node);
        }
    }
	
    /** <p>Process a reference (text:reference-ref tag)</p>
     * @param node The element containing the reference 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleReferenceRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFormat = node.getAttribute(XMLString.TEXT_REFERENCE_FORMAT);
        String sName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (("page".equals(sFormat) || "".equals(sFormat)) && sName!=null) {
            ldp.append("\\pageref{ref:"+refnames.getExportName(sName)+"}");
        }
        else if ("chapter".equals(sFormat) && ofr.referenceMarkInHeading(sName)) {
            // This is safe if the reference mark is contained in a heading
            ldp.append("\\ref{ref:"+refnames.getExportName(sName)+"}");
        }
        else { // use current value
            palette.getInlineCv().traversePCDATA(node,ldp,oc);
        }
    } 
	
    /** <p>Process a bookmark (text:bookmark tag)</p>
     * <p>A bookmark may be the target for either a hyperlink or a reference,
     * so this will generate a <code>\\hyperref</code> and/or a <code>\\label</code></p>
     * @param node The element containing the bookmark 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleBookmark(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            String sName = node.getAttribute(XMLString.TEXT_NAME);
            if (sName!=null) {
                // A bookmark may be used as a target for a hyperlink as well as
                // for a reference. We export whatever is actually used:
                addTarget(node,"",ldp);
                if (ofr.hasBookmarkRefTo(sName)) {
                    ldp.append("\\label{bkm:"+bookmarknames.getExportName(sName)+"}");
                }
            }
        }
        else {
            // Bookmarks should not appear within \section or \caption
            postponedBookmarks.add(node);
        }
    }
	
    /** <p>Process a bookmark reference (text:bookmark-ref tag).</p>
     * @param node The element containing the bookmark reference 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleBookmarkRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFormat = node.getAttribute(XMLString.TEXT_REFERENCE_FORMAT);
        String sName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (("page".equals(sFormat) || "".equals(sFormat)) && sName!=null) {
            ldp.append("\\pageref{bkm:"+bookmarknames.getExportName(sName)+"}");
        }
        else if ("chapter".equals(sFormat) && ofr.bookmarkInHeading(sName)) {
            // This is safe if the bookmark is contained in a heading
            ldp.append("\\ref{bkm:"+bookmarknames.getExportName(sName)+"}");
        }
        else { // use current value
            palette.getInlineCv().traversePCDATA(node,ldp,oc);
        }
    }
	
    /** <p>Process pending reference marks and bookmarks (which may have been
     * postponed within sections, captions or verbatim text.</p>
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void flushReferenceMarks(LaTeXDocumentPortion ldp, Context oc) {
        // We may still be in a context with no reference marks
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            // Type out all postponed reference marks
            int n = postponedReferenceMarks.size();
            for (int i=0; i<n; i++) {
                handleReferenceMark((Element) postponedReferenceMarks.get(i),ldp,oc);
            }
            postponedReferenceMarks.clear();
            // Type out all postponed bookmarks
            n = postponedBookmarks.size();
            for (int i=0; i<n; i++) {
                handleBookmark((Element) postponedBookmarks.get(i),ldp,oc);
            }
            postponedBookmarks.clear();
        }
    }
	
    /** <p>Process a hyperlink (text:a tag)</p>
     * @param node The element containing the hyperlink 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleAnchor(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sHref = node.getAttribute(XMLString.XLINK_HREF);
        if (sHref!=null) {
            if (sHref.startsWith("#")) {
                // TODO: hyperlinks to headings (?) and objects
                if (bUseHyperref) {
                    ldp.append("\\hyperlink{")
                       .append(targets.getExportName(Misc.urlDecode(sHref.substring(1))))
                       .append("}{");
                    // ignore text style (let hyperref.sty handle the decoration):
                    palette.getInlineCv().traverseInlineText(node,ldp,oc);
                    ldp.append("}");
                }
                else { // user don't want to include hyperlinks
                    palette.getInlineCv().handleTextSpan(node,ldp,oc);
                }
            }
            else {
			    if (bUseHyperref) {
                    if (ofr.getTextContent(node).trim().equals(sHref)) {
                        // The link text equals the url
                        ldp.append("\\url{")
                           .append(oc.isInFootnote() ? escapeHref(Misc.urlDecode(sHref)) : Misc.urlDecode(sHref))
                           .append("}");
                    }
                    else {
                        ldp.append("\\href{")
                           .append(oc.isInFootnote() ? escapeHref(Misc.urlDecode(sHref)) : Misc.urlDecode(sHref))
                           .append("}{");
                        // ignore text style (let hyperref.sty handle the decoration):
                        palette.getInlineCv().traverseInlineText(node,ldp,oc);
                        ldp.append("}");
                    }
                }
                else { // user don't want to include hyperlinks
                    palette.getInlineCv().handleTextSpan(node,ldp,oc);
                }
            }
        }
        else {
            palette.getInlineCv().handleTextSpan(node,ldp,oc);
        }
    }
	
    /** <p>Add a <code>\\hypertarget</code></p>
     * @param node The element containing the name of the target
     * @param sSuffix A suffix to be added to the target,
     * e.g. "|table" for a reference to a table.
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     */
    public void addTarget(Element node, String sSuffix, LaTeXDocumentPortion ldp) {
        // TODO: Remove this and use addTarget by name only
        String sName = node.getAttribute(XMLString.TEXT_NAME);
        if (sName == null) { sName = node.getAttribute(XMLString.TABLE_NAME); }
        if (sName == null || !bUseHyperref) { return; }
        if (!ofr.hasLinkTo(sName+sSuffix)) { return; }
        ldp.append("\\hypertarget{")
           .append(targets.getExportName(sName+sSuffix))
           .append("}{}");
    }
    
    /** <p>Add a <code>\\hypertarget</code></p>
     * @param sName The name of the target
     * @param sSuffix A suffix to be added to the target,
     * e.g. "|table" for a reference to a table.
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     */
    public void addTarget(String sName, String sSuffix, LaTeXDocumentPortion ldp) {
        if (sName!=null && bUseHyperref && ofr.hasLinkTo(sName+sSuffix)) {
            ldp.append("\\hypertarget{")
               .append(targets.getExportName(sName+sSuffix))
               .append("}{}");
        }
    }

    /** <p>Process a page number field (text:page-number tag)</p>
     * @param node The element containing the page number field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handlePageNumber(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // TODO: Obey attributes!
        ldp.append("\\thepage{}");
    }
    
    /** <p>Process a page count field (text:page-count tag)</p>
     * @param node The element containing the page count field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handlePageCount(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // TODO: Obey attributes!
        // Note: Actually LastPage refers to the page number of the last page, not the number of pages
        if (config.useLastpage()) {
            bUsesPageCount = true;
            ldp.append("\\pageref{LastPage}");
        }
        else {
            ldp.append("?");
        }
    }

    // Helpers:
	
    private String createPdfMeta(String sName, String sValue) {
        if (sValue==null) { return ""; }
        // Replace commas with semicolons (the keyval package doesn't like commas):
        sValue = sValue.replace(',', ';');
        // Meta data is assumed to be in the default language:
        return ", "+sName+"="+palette.getI18n().convert(sValue,false,palette.getMainContext().getLang());
    }

    // For href within footnote, we have to escape the #
    private String escapeHref(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); i++) {
            if (s.charAt(i)=='#') { buf.append("\\#"); }
            else { buf.append(s.charAt(i)); }
        }
        return buf.toString();
    }
 
}