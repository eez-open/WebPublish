/************************************************************************
 *
 *  InlineConverter.java
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
 *  Version 1.0 (2008-11-22)
 *
 */

package writer2latex.latex;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2latex.util.Misc;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.HeadingMap;

/**
 *  <p>This class handles basic inline text.</p>
 */
public class InlineConverter extends ConverterHelper {

    private String sTabstop = "\\ \\ ";
    private boolean bHasPdfannotation = false;
	
    public InlineConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        // Get custom code for tab stops
        if (config.getTabstop().length()>0) {
            sTabstop = config.getTabstop();
        }
    }
	
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bHasPdfannotation) {
            decl.append("\\newcommand\\pdfannotation[1]")
                .append("{\\ifx\\pdfoutput\\undefined\\marginpar{#1}\\else")
                 .append("\\pdfstringdef\\tempstring{#1}\\marginpar{")
                .append("\\pdfannot width 5cm height 12pt depth 4cm ")
                .append("{ /Subtype /Text /Open false /Contents(\\tempstring) /Color [1 0 0]}")
                .append("}\\fi}").nl();
        }
    }
	
    /** Handle a text:span element
     */     
    public void handleTextSpan(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (oc.isMathMode()) { handleTextSpanMath(node, ldp, oc); }
        else { handleTextSpanText(node, ldp, oc); }
    }
	
    private void handleTextSpanMath(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // TODO: Handle a selection of formatting attributes: color, supscript...
        String sStyleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);
        StyleWithProperties style = ofr.getTextStyle(sStyleName);

        // Always push the font used
        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(style));
		
        // Convert formatting
        BeforeAfter ba = new BeforeAfter();
        if (style!=null) {
            String sPos = style.getProperty(XMLString.STYLE_TEXT_POSITION, true);
            if (sPos!=null) {
                if (sPos.startsWith("sub") || sPos.startsWith("-")) {
                    ba.add("_{", "}");
                }
                else if (sPos.startsWith("super") || !sPos.startsWith("0%")) {
                    ba.add("^{", "}");
                }
            }
        }

        ldp.append(ba.getBefore());
        traverseInlineMath(node, ldp, oc);
        ldp.append(ba.getAfter());

        // finally pop the font table
        palette.getI18n().popSpecialTable();
    }
	
    private void handleTextSpanText(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String styleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);
		
        // Check for strict handling of styles
        String sDisplayName = ofr.getTextStyles().getDisplayName(styleName);
        if (config.otherStyles()!=LaTeXConfig.ACCEPT && !config.getTextStyleMap().contains(sDisplayName)) {
            if (config.otherStyles()==LaTeXConfig.WARNING) {
                System.err.println("Warning: Text with style "+sDisplayName+" was ignored");
            }
            else if (config.otherStyles()==LaTeXConfig.ERROR) {
                ldp.append("% Error in source document: Text with style ")
                   .append(palette.getI18n().convert(sDisplayName,false,oc.getLang()))
                   .append(" was ignored").nl();
            }
            // Ignore this text:
            return;
        }
			
        boolean styled = true;
		
        // don't style it if a {foot|end}note is the only content
        if (onlyNote(node) || OfficeReader.getCharacterCount(node)==0) { styled = false; }
		
        // Also don't style it if we're already within a verbatim environment
        if (oc.isVerbatim()) { styled = false; }        
		
        boolean bNoFootnotes = false;
		
        // Always push the font used
        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(ofr.getTextStyle(styleName)));
		
        // Apply the style
        BeforeAfter ba = new BeforeAfter();
        Context ic = (Context) oc.clone();
        if (styled) { palette.getCharSc().applyTextStyle(styleName,ba,ic); }
		
        // Footnote problems:
        // No footnotes in sub/superscript (will disappear)
        // No multiparagraph footnotes embedded in text command (eg. \textbf{..})
        // Simple solution: styled text element is forbidden footnote area
        if (styled && !ic.isInFootnote()) { bNoFootnotes = true; }

        // Temp solution: Ignore hard formatting in header/footer (name clash problem)
        // only in package format.
        StyleWithProperties style = ofr.getTextStyle(styleName);
        if (ofr.isPackageFormat() && (style!=null && style.isAutomatic()) && ic.isInHeaderFooter()) {
            styled = false;
        }

        if (styled) {    
            if (bNoFootnotes) { ic.setNoFootnotes(true); }
            ldp.append(ba.getBefore());
        }
                              
        traverseInlineText(node,ldp,ic);
        
        if (styled) {    
            ldp.append(ba.getAfter());
            ic.setNoFootnotes(false);
            if (!ic.isInFootnote()) { palette.getNoteCv().flushFootnotes(ldp,oc); }
        }
		
        // Flush any pending index marks and reference marks
        palette.getFieldCv().flushReferenceMarks(ldp,oc);
        palette.getIndexCv().flushIndexMarks(ldp,oc);
		
        // finally pop the special table
        palette.getI18n().popSpecialTable();
    }
	
    public void traverseInlineText(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (oc.isVerbatim()) {
            traverseVerbatimInlineText(node,ldp,oc);
        }
        else if (oc.isMathMode()) {
            traverseInlineMath(node,ldp,oc);
        }
        else {
            traverseOrdinaryInlineText(node,ldp,oc);
        }
    }
	
    // Traverse ordinary inline text in text mode (temporarily changing to math
    // mode for a sequence of text:span with style "OOoLaTeX")
    private void traverseOrdinaryInlineText(Element node,LaTeXDocumentPortion ldp, Context oc) {
        Node childNode = node.getFirstChild();
        while (childNode!=null) {     
            short nodeType = childNode.getNodeType();
               
            switch (nodeType) {
                case Node.TEXT_NODE:
                    String s = childNode.getNodeValue();
                    if (s.length() > 0) {
                        ldp.append(palette.getI18n().convert(s, false, oc.getLang()));
                    }
                    break;
                        
                case Node.ELEMENT_NODE:
                    Element child = (Element)childNode;
                    String sName = child.getTagName();
                    if (sName.equals(XMLString.TEXT_SPAN)) {
                        String sStyleName = child.getAttribute(XMLString.TEXT_STYLE_NAME);
                        boolean bIsMathSpan = "OOoLaTeX".equals(ofr.getTextStyles().getDisplayName(sStyleName));
                        if (bIsMathSpan) {
                            // Temporarily change to math mode
                            Context ic = (Context) oc.clone();
                            ic.setMathMode(true);
 
                            ldp.append("$");

                            Node remember;
                            boolean bContinue = false;

                            do {
                                handleTextSpanMath((Element)childNode, ldp, ic);
                                remember = childNode;
                                childNode = childNode.getNextSibling();
                                bContinue = false;
                                if (childNode!=null && childNode.getNodeType()==Node.ELEMENT_NODE &&
                                    childNode.getNodeName().equals(XMLString.TEXT_SPAN)) {
                                    sStyleName = Misc.getAttribute(childNode,XMLString.TEXT_STYLE_NAME);
                                    if ("OOoLaTeX".equals(ofr.getTextStyles().getDisplayName(sStyleName))) 
                                        //child = (Element) childNode;
                                        bContinue = true;
                                    }
                            } while(bContinue);
                            childNode = remember;

                            ldp.append("$");
                        }
                        else {                        
                            handleTextSpan(child,ldp,oc);
                        }
                    }
                    else if (child.getNodeName().startsWith("draw:")) {
                            palette.getDrawCv().handleDrawElement(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_S)) {
                            if (config.ignoreDoubleSpaces()) {
                                ldp.append(" ");
                            }
                            else {
                                int count= Misc.getPosInteger(child.getAttribute(XMLString.TEXT_C),1);
                                //String sSpace = config.ignoreDoubleSpaces() ? " " : "\\ ";
                                for ( ; count > 0; count--) { ldp.append("\\ "); }
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_TAB_STOP) || sName.equals(XMLString.TEXT_TAB)) { // text:tab in oasis
                            // tab stops are not supported by the converter, but the special usage
                            // of tab stops in header and footer can be emulated with \hfill
                            // TODO: Sometimes extra \hfill should be added at end of line
                            if (oc.isInHeaderFooter()) { ldp.append("\\hfill "); }
                            else { ldp.append(sTabstop); }
                        }
                        else if (sName.equals(XMLString.TEXT_LINE_BREAK)) {
                            if (!oc.isInHeaderFooter() && !config.ignoreHardLineBreaks()) {
                                ldp.append("\\newline").nl();
                            }
                            else { ldp.append(" "); }
                        }
                        else if (sName.equals(XMLString.TEXT_A)) {
                            palette.getFieldCv().handleAnchor(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.OFFICE_ANNOTATION)) {
                            handleOfficeAnnotation(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_PAGE_NUMBER)) {
                            palette.getFieldCv().handlePageNumber(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_PAGE_COUNT)) {
                            palette.getFieldCv().handlePageCount(child,ldp,oc);
                        }
                        else if (oc.isInHeaderFooter()) {
                            if (sName.equals(XMLString.TEXT_CHAPTER)) {
                                handleChapterField(child,ldp,oc);
                            }
                            else if (sName.startsWith("text:")) {
                                traverseInlineText(child,ldp,oc);
                            }
                        }
                        else {
	                        // These tags are ignored in header and footer
                            if (sName.equals(XMLString.TEXT_FOOTNOTE)) {
                                palette.getNoteCv().handleFootnote(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_ENDNOTE)) {
                                palette.getNoteCv().handleEndnote(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_NOTE)) {
                                if ("endnote".equals(child.getAttribute(XMLString.TEXT_NOTE_CLASS))) {
                                    palette.getNoteCv().handleEndnote(child,ldp,oc);
                                }
                                else {
                                    palette.getNoteCv().handleFootnote(child,ldp,oc);
                                }
                            }
                            else if (sName.equals(XMLString.TEXT_SEQUENCE)) {
	                            palette.getFieldCv().handleSequence(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_SEQUENCE_REF)) {
	                            palette.getFieldCv().handleSequenceRef(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_FOOTNOTE_REF)) {
	                            palette.getNoteCv().handleFootnoteRef(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_ENDNOTE_REF)) {
                                palette.getNoteCv().handleEndnoteRef(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_NOTE_REF)) { // oasis
                                palette.getNoteCv().handleNoteRef(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_REFERENCE_MARK)) {
	                            palette.getFieldCv().handleReferenceMark(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_REFERENCE_MARK_START)) {
                                palette.getFieldCv().handleReferenceMark(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_REFERENCE_REF)) {
	                            palette.getFieldCv().handleReferenceRef(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_BOOKMARK)) {
                                palette.getFieldCv().handleBookmark(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_BOOKMARK_START)) {
	                            palette.getFieldCv().handleBookmark(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_BOOKMARK_REF)) {
                                palette.getFieldCv().handleBookmarkRef(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_BIBLIOGRAPHY_MARK)) {
                                palette.getBibCv().handleBibliographyMark(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK)) {
                                palette.getIndexCv().handleAlphabeticalIndexMark(child,ldp,oc);
                            }
                            else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK_START)) {
                                palette.getIndexCv().handleAlphabeticalIndexMark(child,ldp,oc);
                            }
                            else if (sName.startsWith("text:")) {
                                traverseInlineText(child,ldp,oc);
                            }
                        }
                        break;
                    default:
                        // Do nothing
            }

            childNode = childNode.getNextSibling();
        }
		
    }

    /* traverse inline text, ignoring any draw objects, footnotes, formatting and hyperlinks */
    public void traversePlainInlineText(Element node,LaTeXDocumentPortion ldp, Context oc) {
        String styleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);
		
        // Always push the font used
        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(ofr.getTextStyle(styleName)));
        
        Node childNode = node.getFirstChild();
        while (childNode!=null) {
            short nodeType = childNode.getNodeType();
               
            switch (nodeType) {
                case Node.TEXT_NODE:
                    String s = childNode.getNodeValue();
                    if (s.length() > 0) {
                        // Need to protect ]
                        for (int j=0; j<s.length(); j++) {
                            if (s.charAt(j)!=']') {
                                ldp.append(palette.getI18n().convert(Character.toString(s.charAt(j)),false,oc.getLang()));
                            }
                            else {
                                ldp.append("{]}");
                            }
                        }
                    }
                    break;
                        
                case Node.ELEMENT_NODE:
                    Element child = (Element)childNode;
                    String sName = child.getTagName();
                    if (sName.equals(XMLString.TEXT_SPAN)) {
                        traversePlainInlineText(child,ldp,oc);
                    }
                    else if (sName.equals(XMLString.TEXT_S)) {
                        int count= Misc.getPosInteger(child.getAttribute(XMLString.TEXT_C),1);
                        for ( ; count > 0; count--) {
                           ldp.append("\\ ");
                        }
                    }
                    else if (sName.equals(XMLString.TEXT_TAB_STOP) || sName.equals(XMLString.TEXT_TAB)) { // text:tab in oasis
                        // tab stops are not supported by the converter
                        ldp.append(sTabstop);
                    }
                    else if (OfficeReader.isNoteElement(child)) {
                        // ignore
                    }
                    else if (OfficeReader.isTextElement(child)) {
                        traversePlainInlineText(child,ldp,oc);
                    }
                    break;
                default:
                    // Do nothing
            }
            childNode = childNode.getNextSibling();
        }
        // finally pop the special table
        palette.getI18n().popSpecialTable();
    }

    /* traverse inline math, ignoring any draw objects, footnotes, formatting and hyperlinks */
    public void traverseInlineMath(Element node,LaTeXDocumentPortion ldp, Context oc) {
        String styleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);
		
        // Always push the font used
        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(ofr.getTextStyle(styleName)));
        
        Node childNode = node.getFirstChild();
        while (childNode!=null) {
            short nodeType = childNode.getNodeType();
               
            switch (nodeType) {
                case Node.TEXT_NODE:
                    String s = childNode.getNodeValue();
                    ldp.append(palette.getI18n().convert(s,true,oc.getLang()));
                    break;
                        
                case Node.ELEMENT_NODE:
                    Element child = (Element)childNode;
                    String sName = child.getTagName();
                    if (sName.equals(XMLString.TEXT_S)) {
                        int count= Misc.getPosInteger(child.getAttribute(XMLString.TEXT_C),1);
                        for ( ; count > 0; count--) {
                           ldp.append("\\ ");
                        }
                    }
                    else if (sName.equals(XMLString.TEXT_TAB_STOP) || sName.equals(XMLString.TEXT_TAB)) { // text:tab in oasis
                        // tab stops are not supported by the converter
                        ldp.append(" ");
                    }
                    else if (OfficeReader.isNoteElement(child)) {
                        // ignore
                    }
                    else if (OfficeReader.isTextElement(child)) {
                        traversePlainInlineText(child,ldp,oc);
                    }
                    break;
                default:
                    // Do nothing
            }
            childNode = childNode.getNextSibling();
        }
        // finally pop the special table
        palette.getI18n().popSpecialTable();
    }

    /* traverse verbatim inline text, ignoring any draw objects, footnotes, formatting and hyperlinks */
    private void traverseVerbatimInlineText(Element node,LaTeXDocumentPortion ldp, Context oc) {        
        if (node.hasChildNodes()) {
		
            NodeList nList = node.getChildNodes();
            int len = nList.getLength();
                       
            for (int i = 0; i < len; i++) {
                
                Node childNode = nList.item(i);
                short nodeType = childNode.getNodeType();
               
                switch (nodeType) {
                    case Node.TEXT_NODE:
                        String s = childNode.getNodeValue();
                        if (s.length() > 0) {
                             // text is copied verbatim! (Will be replaced by
                             // question marks if outside inputenc)
                            ldp.append(s);
                        }
                        break;
                        
                    case Node.ELEMENT_NODE:
                        Element child = (Element)childNode;
                        String sName = child.getTagName();
                        if (sName.equals(XMLString.TEXT_S)) {
                            int count= Misc.getPosInteger(child.getAttribute(XMLString.TEXT_C),1);
                            for ( ; count > 0; count--) {
                                ldp.append(" ");
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_TAB_STOP) || sName.equals(XMLString.TEXT_TAB)) { // text:tab in oasis
                            // tab stops are not supported by the onverter
                            ldp.append(sTabstop);
                        }
                        else if (sName.equals(XMLString.TEXT_LINE_BREAK)) {
                            if (!oc.isNoLineBreaks()) { ldp.nl(); }
                        }
                        else if (sName.equals(XMLString.TEXT_NOTE)) {
                            // oasis; ignore
                        }
                        else if (sName.equals(XMLString.TEXT_FOOTNOTE)) {
                            // ignore
                        }
                        else if (sName.equals(XMLString.TEXT_ENDNOTE)) {
                            // ignore
                        }
                        // The respective handlers know how to postpone these marks in verbatim context:
                        else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK)) {
                            palette.getIndexCv().handleAlphabeticalIndexMark(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK_START)) {
                            palette.getIndexCv().handleAlphabeticalIndexMark(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK)) {
                            palette.getFieldCv().handleReferenceMark(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK_START)) {
                            palette.getFieldCv().handleReferenceMark(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK)) {
                            palette.getFieldCv().handleBookmark(child,ldp,oc);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK_START)) {
                            palette.getFieldCv().handleBookmark(child,ldp,oc);
                        }

                        else if (sName.startsWith("text:")) {
                            traverseVerbatimInlineText(child,ldp,oc);
                        }
                        break;
                    default:
                        // Do nothing
                }
            }
        }
    }

    public void traversePCDATA(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (node.hasChildNodes()) {
            NodeList nl = node.getChildNodes();
            int nLen = nl.getLength();
            for (int i=0; i<nLen; i++) {
                if (nl.item(i).getNodeType()==Node.TEXT_NODE) {
                    ldp.append(palette.getI18n().convert(nl.item(i).getNodeValue(),false,oc.getLang()));
                }
            }
        }
    }
	
    private void handleChapterField(Element node, LaTeXDocumentPortion ldp, Context oc) {
        HeadingMap hm = config.getHeadingMap();
        int nLevel = Misc.getPosInteger(node.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
        if (nLevel<=hm.getMaxLevel()) {
            int nLaTeXLevel = hm.getLevel(nLevel);
            if (nLaTeXLevel==1) {
		        palette.getPageSc().setChapterField1(node.getAttribute(XMLString.TEXT_DISPLAY));
                ldp.append("{\\leftmark}");
            }
		    else if (nLaTeXLevel==2) {
		        palette.getPageSc().setChapterField2(node.getAttribute(XMLString.TEXT_DISPLAY));
                ldp.append("{\\rightmark}");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Annotations	
	
    private void handleOfficeAnnotation(Element node, LaTeXDocumentPortion ldp, Context oc) {
    	String sCommand = null;
        switch (config.notes()) {
        case LaTeXConfig.IGNORE: return;
        case LaTeXConfig.COMMENT:
        	// Get the unformatted text of all paragraphs and insert each paragraph as a single comment
			ldp.append("%").nl();
        	Node child = node.getFirstChild();
        	while (child!=null) {
        		if (Misc.isElement(child, XMLString.TEXT_P)) {
        			ldp.append("%");
        			traversePlainInlineText((Element)child, ldp, oc); 
        			ldp.nl();
        		}
                child = child.getNextSibling();        		
        	}
        	return;
        case LaTeXConfig.PDFANNOTATION:
            bHasPdfannotation = true;
            sCommand = "\\pdfannotation";
            break;
        case LaTeXConfig.MARGINPAR:
            sCommand = "\\marginpar";
            break;
        case LaTeXConfig.CUSTOM:
            sCommand = config.getNotesCommand();
            break;
        }
    	
    	// Get the unformatted text of all paragraphs, separated by spaces
        ldp.append(sCommand).append("{");
        boolean bFirst = true;
    	Node child = node.getFirstChild();
    	while (child!=null) {
    		if (Misc.isElement(child, XMLString.TEXT_P)) {
    			if (!bFirst) ldp.append(" ");
    			traversePlainInlineText((Element)child, ldp, oc);
    			bFirst = false;
    		}
            child = child.getNextSibling();
    		
    	}
    	ldp.append("}");
    }

    /* Check to see if this node has a footnote or endnote as the only subnode */
    private boolean onlyNote(Node node) {
        if (!node.hasChildNodes()) { return false; }
        NodeList nList = node.getChildNodes();
        int nLen = nList.getLength();
                   
        for (int i = 0; i < nLen; i++) {
            
            Node child = nList.item(i);
            short nType = child.getNodeType();
           
            switch (nType) {
                case Node.TEXT_NODE: return false;
                case Node.ELEMENT_NODE:
                    if (!OfficeReader.isNoteElement(child)) { return false; }
            }
        }
        return true;
    }
  
}