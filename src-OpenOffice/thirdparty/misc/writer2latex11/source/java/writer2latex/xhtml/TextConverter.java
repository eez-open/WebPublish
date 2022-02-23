/************************************************************************
 *
 *  TextConverter.java
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
 *  Version 1.0 (2009-03-10)
 *
 */

package writer2latex.xhtml;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.LinkedList;
import java.util.Locale;

import java.text.Collator;

//import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import writer2latex.util.Misc;
import writer2latex.office.XMLString;
import writer2latex.office.IndexMark;
import writer2latex.office.ListCounter;
import writer2latex.office.ListStyle;
import writer2latex.office.PropertySet;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.OfficeReader;
import writer2latex.office.TocReader;

// Helper class (a struct) to contain information about an alphabetical
// index entry.
final class AlphabeticalEntry {
    String sWord; // the word for the index
    int nIndex; // the original index of this entry
}

// Helper class (a struct) to contain information about a toc entry
// (ie. a heading, other paragraph or toc-mark)
final class TocEntry {
    Element onode; // the original node
    String sLabel = null; // generated label for the entry
    int nFileIndex; // the file index for the generated content
    int nOutlineLevel; // the outline level for this heading
    int[] nOutlineNumber; // the natural outline number for this heading
}

// Helper class (a struct) to point back to indexes that should be processed
final class IndexData {
    int nOutFileIndex; // the index of the out file containing the index
    Element onode; // the original node
    Element chapter; // the chapter containing this toc
    Element hnode; // a div node where the index should be added
}

public class TextConverter extends ConverterHelper {

    // Data used to handle splitting over several files
    // TODO: Accessor methods for sections
    int nSplit = 0;  // The outline level at which to split files (0=no split)
    int nRepeatLevels = 5; // The number of levels to repeat when splitting (0=no repeat)
    private int nLastSplitLevel = 1; // The outline level at which the last split occured
    private int nDontSplitLevel = 0; // if > 0 splitting is forbidden
    boolean bAfterHeading=false; // last element was a top level heading
    protected Stack sections = new Stack(); // stack of nested sections
    Element[] currentHeading = new Element[7]; // Last headings (repeated when splitting)

    // Counters for generated numbers
    private ListCounter outlineNumbering;
    private Hashtable listCounters = new Hashtable();
    private String sCurrentListLabel = null;
    
    // Mode used to handle floats (depends on source doc type and config)
    private int nFloatMode; 
	
    // Data used for index bookkeeping
    private Vector indexes = new Vector();

    // Data used to handle Alphabetical Index
    Vector index = new Vector(); // All words for the index
    private int nIndexIndex = -1; // Current index used for id's (of form idxN) 
    private int nAlphabeticalIndex = -1; // File containing alphabetical index

    // Data used to handle Table of Contents
    private Vector tocEntries = new Vector(); // All potential(!) toc items
    private int nTocFileIndex = -1; // file index for main toc
    private Element currentChapter = null; // Node for the current chapter (level 1) heading
    private int nTocIndex = -1; // Current index for id's (of form tocN)
    private ListCounter naturalOutline = new ListCounter(); // Current "natural" outline number 

    // Style names for foot- and endnotes
    private String sFntCitBodyStyle = null;
    private String sFntCitStyle = null;
    private String sEntCitBodyStyle = null;
    private String sEntCitStyle = null;

    // Gather the footnotes and endnotes
    private LinkedList footnotes = new LinkedList();
    private LinkedList endnotes = new LinkedList();

    // Sometimes we have to create an inlinenode in a block context
    // (labels for footnotes and endnotes)
    // We put it here and insert it in the first paragraph/heading to come:
    private Node asapNode = null;

    // When generating toc, a few things should be done differently
    private boolean bInToc = false;

    public TextConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        nSplit = config.getXhtmlSplitLevel();
        nRepeatLevels = config.getXhtmlRepeatLevels();
        nFloatMode = ofr.isText() && config.xhtmlFloatObjects() ? 
            DrawConverter.FLOATING : DrawConverter.ABSOLUTE;
        outlineNumbering = new ListCounter(ofr.getOutlineStyle());
        // Styles for footnotes and endnotes
        PropertySet notes = ofr.getFootnotesConfiguration();
        if (notes!=null) {
            sFntCitBodyStyle = notes.getProperty(XMLString.TEXT_CITATION_BODY_STYLE_NAME);
            sFntCitStyle = notes.getProperty(XMLString.TEXT_CITATION_STYLE_NAME);
        }
        notes = ofr.getEndnotesConfiguration();
        if (notes!=null) {
            sEntCitBodyStyle = notes.getProperty(XMLString.TEXT_CITATION_BODY_STYLE_NAME);
            sEntCitStyle = notes.getProperty(XMLString.TEXT_CITATION_STYLE_NAME);
        }
    }
	
    /** Converts an office node as a complete text document
     *
     *  @param onode the Office node containing the content to convert
     */
    public void convertTextContent(Element onode) {
        Element hnode = converter.nextOutFile();

        // Create form
        if (nSplit==0) {
            Element form = getDrawCv().createForm();
            if (form!=null) {
                hnode.appendChild(form);
                hnode = form;
            }
        }

        // Convert content
        traverseBlockText(onode,hnode);

        // Generate all indexes
        int nIndexCount = indexes.size();
        for (int i=0; i<nIndexCount; i++) {
            generateToc((IndexData) indexes.get(i));
        }
		
        // Generate navigation links
        generateHeaders();
        generateFooters();
        generatePanels();
    }
	
    protected int getTocIndex() { return nTocFileIndex; }
	
    protected int getAlphabeticalIndex() { return nAlphabeticalIndex; }
	
    ////////////////////////////////////////////////////////////////////////
    // NAVIGATION (fill header, footer and panel with navigation links)
    ////////////////////////////////////////////////////////////////////////

    // The header is populated with prev/next navigation
    private void generateHeaders() { }

    // The footer is populated with prev/next navigation
    private void generateFooters() { }

    // The panel is populated with a minitoc
    // TODO: Include link to toc and index in appropriate places..
    private void generatePanels() {
        int nLastIndex = converter.getOutFileIndex();

        bInToc = true;
		
        boolean bHasFrontMatter = false;

        TocEntry fakeEntry = new TocEntry();
        fakeEntry.nOutlineLevel = 0;
        fakeEntry.nOutlineNumber = new int[11];

        int nLen = tocEntries.size();

        for (int nIndex=0; nIndex<=nLastIndex; nIndex++) {
            converter.changeOutFile(nIndex);
            Element panel = converter.getPanelNode();
            if (panel!=null) {
                // Get the last heading of level <= split level for this file
                TocEntry entryCurrent = null;				
                for (int i=nLen-1; i>=0; i--) {
                    TocEntry entry = (TocEntry) tocEntries.get(i);
                    if (XMLString.TEXT_H.equals(entry.onode.getTagName()) && entry.nFileIndex==nIndex && entry.nOutlineLevel<=nSplit) {
                        entryCurrent = entry; break;
                    }
                }
				
                if (entryCurrent==null) {
                    entryCurrent = fakeEntry;
                    if (nIndex==0) { bHasFrontMatter=true; }
                }
				
                // Determine the maximum outline level to include
                int nMaxLevel = entryCurrent.nOutlineLevel;
                if (nMaxLevel<nSplit) { nMaxLevel++; }

                // Create minitoc with relevant entries
                if (bHasFrontMatter) {
                    Element inline = createPanelLink(panel, nIndex, 0, 1);
                    inline.appendChild(converter.createTextNode(converter.getL10n().get(L10n.HOME)));
                }
				
                int nPrevFileIndex = 0;
                for (int i=0; i<nLen; i++) {
                    TocEntry entry = (TocEntry) tocEntries.get(i);

                    if (entry.nFileIndex>nPrevFileIndex+1) {
                        // Skipping a file index means we have passed an index
                        for (int k=nPrevFileIndex+1; k<entry.nFileIndex; k++) {
                            createIndexLink(panel,nIndex,k);
                        }
                    }
                    nPrevFileIndex = entry.nFileIndex;
					
                    String sNodeName = entry.onode.getTagName();
                    if (XMLString.TEXT_H.equals(sNodeName)) {

                        // Determine wether or not to include this heading
                        // Note that this condition misses the case where
                        // a heading of level n is followed by a heading of
                        // level n+2. This is considered a bug in the document!
                        boolean bInclude = entry.nOutlineLevel<=nMaxLevel;
                        if (bInclude) {
                            // Check that this heading matches the current
                            int nCompareLevels = entry.nOutlineLevel;
                            for (int j=1; j<nCompareLevels; j++) {
                                if (entry.nOutlineNumber[j]!=entryCurrent.nOutlineNumber[j]) {
                                    bInclude = false;
                                }
                            }
                        }
                        
                        if (bInclude) {
                            Element inline = createPanelLink(panel, nIndex, entry.nFileIndex, entry.nOutlineLevel);

                            // Add content of heading
                            if (entry.sLabel!=null && entry.sLabel.length()>0) {
                                inline.appendChild(converter.createTextNode(entry.sLabel));
                                if (!entry.sLabel.endsWith(" ")) {
                                    inline.appendChild(converter.createTextNode(" "));
                                }
                            }
                            traverseInlineText(entry.onode,inline);
                        }
                    }
                }
                if (nPrevFileIndex<nLastIndex) {
                    // Trailing index
                    for (int k=nPrevFileIndex+1; k<=nLastIndex; k++) {
                        createIndexLink(panel,nIndex,k);
                    }
                }
            }
        }
        
        bInToc = false;
		
        converter.changeOutFile(nLastIndex);

    }
	
    private void createIndexLink(Element panel, int nIndex, int nFileIndex) {
        if (nFileIndex==nTocFileIndex) {
            Element inline = createPanelLink(panel, nIndex, nTocFileIndex, 1);
            inline.appendChild(converter.createTextNode(converter.getL10n().get(L10n.CONTENTS)));
        }
        else if (nFileIndex==nAlphabeticalIndex) {
            Element inline = createPanelLink(panel, nIndex, nAlphabeticalIndex, 1);
            inline.appendChild(converter.createTextNode(converter.getL10n().get(L10n.INDEX)));
        }
    }

    private Element createPanelLink(Element panel, int nCurrentFile, int nLinkFile, int nOutlineLevel) {
        // Create a link
        Element p = converter.createElement("p");
        p.setAttribute("class","level"+nOutlineLevel);
        panel.appendChild(p);
        Element inline;
        if (nCurrentFile!=nLinkFile) {
            inline = converter.createElement("a");
            inline.setAttribute("href",converter.getOutFileName(nLinkFile,true));
        }
        else {
            inline = converter.createElement("span");
            inline.setAttribute("class","nolink");
        }
        p.appendChild(inline);
        return inline;
    }
	
    ////////////////////////////////////////////////////////////////////////
    // BLOCK TEXT (returns current html node at end of block)
    ////////////////////////////////////////////////////////////////////////

    public Node traverseBlockText(Node onode, Node hnode) {
        return traverseBlockText(onode,0,null,hnode);
    } 
	
    private Node traverseBlockText(Node onode, int nLevel, String styleName, Node hnode) {
        if (!onode.hasChildNodes()) { return hnode; }
        bAfterHeading = false;
        NodeList nList = onode.getChildNodes();
        int nLen = nList.getLength();
        int i = 0;
        while (i < nLen) {
            Node child = nList.item(i);
            
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                // Block splitting
                nDontSplitLevel++;
                
                if (OfficeReader.isDrawElement(child)) {
                    getDrawCv().handleDrawElement((Element)child,(Element)hnode,null,nFloatMode);
                }
                else if (nodeName.equals(XMLString.TEXT_P)) {
                    // is there a block element, we should use?
                    XhtmlStyleMap xpar = config.getXParStyleMap();
                    String sDisplayName = ofr.getParStyles().getDisplayName(Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME));
					
                    if (sDisplayName!=null && xpar.contains(sDisplayName)) {
                        Node curHnode = hnode;
                        String sBlockElement = xpar.getBlockElement(sDisplayName);
                        String sBlockCss = xpar.getBlockCss(sDisplayName);
                        if (xpar.getBlockElement(sDisplayName).length()>0) {
                            Element block = converter.createElement(xpar.getBlockElement(sDisplayName));
                            if (!"(none)".equals(xpar.getBlockCss(sDisplayName))) {
                                block.setAttribute("class",xpar.getBlockCss(sDisplayName));
                            }
                            hnode.appendChild(block);
                            curHnode = block;
                        }
                        boolean bMoreParagraphs = true;
                        do {
                            handleParagraph(child,curHnode);
                            bMoreParagraphs = false;
                            if (++i<nLen) {
                                child = nList.item(i);
                                String cnodeName = child.getNodeName();
                                if (cnodeName.equals(XMLString.TEXT_P)) {
                                    String sCurDisplayName = ofr.getParStyles().getDisplayName(Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME));
                                    if (sCurDisplayName!=null && xpar.contains(sCurDisplayName)) {
                                        if (sBlockElement.equals(xpar.getBlockElement(sCurDisplayName)) &&
	                                        sBlockCss.equals(xpar.getBlockCss(sCurDisplayName))) {
                                            bMoreParagraphs = true;
                                         }
                                    }
                                }
                            }
                        } while (bMoreParagraphs);
                        i--;
                    }
                    else {
                        handleParagraph(child,hnode);
                    }
                }
                else if(nodeName.equals(XMLString.TEXT_H)) {
                    int nOutlineLevel = getOutlineLevel((Element)child);
                    Node rememberNode = hnode;
                    hnode = maybeSplit(hnode,nOutlineLevel,bAfterHeading);
                    handleHeading(child,hnode,rememberNode!=hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_LIST) || // oasis
                         nodeName.equals(XMLString.TEXT_UNORDERED_LIST) || // old
                         nodeName.equals(XMLString.TEXT_ORDERED_LIST)) // old
                    {
                    if (listIsOnlyHeadings(child)) {
                        nDontSplitLevel--;
                        hnode = handleFakeList(child,nLevel+1,styleName,hnode);
                        nDontSplitLevel++;
                    }
                    else {
                        handleList(child,nLevel+1,styleName,hnode);
                    }
                }
                else if (nodeName.equals(XMLString.TABLE_TABLE)) {
                    getTableCv().handleTable(child,hnode);
                }
                else if (nodeName.equals(XMLString.TABLE_SUB_TABLE)) {
                    getTableCv().handleTable(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_SECTION)) {
                    nDontSplitLevel--;
                    hnode = handleSection(child,hnode);
                    nDontSplitLevel++;
                }
                else if (nodeName.equals(XMLString.TEXT_TABLE_OF_CONTENT)) {
                    if (!ofr.getTocReader((Element)child).isByChapter()) {
                        hnode = maybeSplit(hnode,1,bAfterHeading);
                    }
                    handleTOC(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_ILLUSTRATION_INDEX)) {
                    handleLOF(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_TABLE_INDEX)) {
                    handleLOT(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_OBJECT_INDEX)) {
                    handleObjectIndex(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_USER_INDEX)) {
                    handleUserIndex(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_ALPHABETICAL_INDEX)) {
                    hnode = maybeSplit(hnode,1,bAfterHeading);
                    handleAlphabeticalIndex(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_BIBLIOGRAPHY)) {
                    hnode = maybeSplit(hnode,1,bAfterHeading);
                    handleBibliography(child,hnode);
                }
                else if (nodeName.equals(XMLString.OFFICE_ANNOTATION)) {
                    converter.handleOfficeAnnotation(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_SEQUENCE_DECLS)) {
                    //handleSeqeuenceDecls(child);
                }
                // Reenable splitting
                nDontSplitLevel--;
                // Remember if this was a heading
                if (nDontSplitLevel==0) {
                    bAfterHeading = nodeName.equals(XMLString.TEXT_H);
                }
            }
            i++;
        }
        return hnode;
    }
    
    private Node maybeSplit(Node node, int nLevel, boolean bAfterHeading) {
        if (nDontSplitLevel>1) { // we cannot split due to a nested structure
            return node;
        }
        if (bAfterHeading && nLevel-nLastSplitLevel<=nRepeatLevels) {
            // we cannot split because we are right after a heading and the
            // maximum number of parent headings on the page is not reached  
            return node;
        }
        if (nSplit>=nLevel && converter.outFileHasContent()) {
            // No objections, this is a level that causes splitting
            return converter.nextOutFile();
        }
        return node;
    }

    /* Process a text:section tag (returns current html node) */
    private Node handleSection(Node onode, Node hnode) {
        String sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
        Element div = converter.createElement("div");
        hnode.appendChild(div);
        converter.addTarget(div,sName+"|region");
        StyleInfo sectionInfo = new StyleInfo();
        getSectionSc().applyStyle(sStyleName,sectionInfo);
        applyStyle(sectionInfo,div);
        sections.push(onode);
        Node newhnode = traverseBlockText(onode, div);
        sections.pop();
        return newhnode.getParentNode();
    }
	
    private void handleHeading(Node onode, Node hnode, boolean bAfterSplit) {
        int nListLevel = getOutlineLevel((Element)onode);
        boolean bUnNumbered = "true".equals(Misc.getAttribute(onode,XMLString.TEXT_IS_LIST_HEADER));
        boolean bRestart = "true".equals(Misc.getAttribute(onode,XMLString.TEXT_RESTART_NUMBERING));
        int nStartValue = Misc.getPosInteger(Misc.getAttribute(onode,XMLString.TEXT_START_VALUE),1)-1;
        handleHeading(onode, hnode, bAfterSplit, ofr.getOutlineStyle(),
            nListLevel, bUnNumbered, bRestart, nStartValue);        
    }

    /*
     * Process a text:h tag
     */
    private void handleHeading(Node onode, Node hnode, boolean bAfterSplit,
        ListStyle listStyle, int nListLevel, boolean bUnNumbered,
        boolean bRestart, int nStartValue) {
        // Note: nListLevel may in theory be different from the outline level,
        // though the ui in OOo does not allow this

        // Numbering: It is possible to define outline numbering in CSS2
        // using counters; but this is not supported by Mozilla 1.0.
        // TODO: Offer CSS2 solution as an alternative later.

        // Note: Conditional styles are not supported
        int nLevel = getOutlineLevel((Element)onode);
        if (nLevel<=6) {
            if (nLevel==1) { currentChapter = (Element) onode; }
            // If split output, add headings of higher levels
            if (bAfterSplit && nSplit>0) {
                int nFirst = nLevel-nRepeatLevels;
                if (nFirst<0) { nFirst=0; }                
                for (int i=nFirst; i<nLevel; i++) {
                    if (currentHeading[i]!=null) {
                        hnode.appendChild(converter.importNode(currentHeading[i],true));
                    }
                }
            }		

			// add Hx element
            Element heading = converter.createElement("h"+nLevel);
            traverseFloats(onode,hnode,heading);
            hnode.appendChild(heading);
            // Apply writing direction
            String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
            StyleWithProperties style = ofr.getParStyle(sStyleName);
            if (style!=null) {
                StyleInfo headInfo = new StyleInfo(); 
                StyleConverterHelper.applyDirection(style,headInfo);
                getParSc().applyStyle(headInfo,heading);
            }

            getParSc().setHeadingStyle(nLevel,sStyleName);
			
            // Prepend asapNode
            prependAsapNode(heading);
			
            // Prepend numbering
            ListCounter counter = getListCounter(listStyle); 
            if (bRestart) { counter.restart(nListLevel,nStartValue); }
            String sLabel = counter.step(nListLevel).getLabel();
            if (!bUnNumbered && sLabel.length()>0) {
                Element span = converter.createElement("span");
                StyleInfo info = new StyleInfo();
                info.sClass = "SectionNumber";
                if (listStyle!=null) {
                    String sTextStyleName = listStyle.getLevelProperty(
                        nListLevel,XMLString.TEXT_STYLE_NAME);
                    getTextSc().applyStyle(sTextStyleName, info);
                }
                getTextSc().applyStyle(info, span);
                heading.appendChild(span);
                span.appendChild( converter.createTextNode(sLabel) );
            }
			
            // Add to toc
            if (!bInToc) {
                converter.addTarget(heading,"toc"+(++nTocIndex));
                TocEntry entry = new TocEntry();
                entry.onode = (Element) onode;
                entry.sLabel = sLabel;
                entry.nFileIndex = converter.getOutFileIndex();
                entry.nOutlineLevel = nLevel; 
                entry.nOutlineNumber = naturalOutline.step(nLevel).getValues();
                tocEntries.add(entry);
            }

            traverseInlineText(onode,heading);
			
            // Keep track of current headings for split output
            currentHeading[nLevel] = heading;
            for (int i=nLevel+1; i<=6; i++) {
                currentHeading[i] = null;
            }
        }
        else { // beyond h6 - export as ordinary paragraph
            handleParagraph(onode,hnode);
        }
    }

    /*
     * Process a text:p tag
     */
    private void handleParagraph(Node onode, Node hnode) {
        boolean bIsEmpty = OfficeReader.isWhitespaceContent(onode);
        if (config.ignoreEmptyParagraphs() && bIsEmpty) { return; }
        String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);

        Element par;
        if (ofr.isSpreadsheet()) { // attach inline text directly to parent (always a table cell)
            par = (Element) hnode;
        }
        else {
            // Hack because createParagraph doesn't work the way we need here :-(
            Element temp = converter.createElement("temp");
            par = createParagraph(temp, sStyleName);
            prependAsapNode(par);
            traverseFloats(onode,hnode,par);
            hnode.appendChild(temp.getFirstChild());
        }

        // Maybe add to toc
        if (ofr.isIndexSourceStyle(getParSc().getRealParStyleName(sStyleName))) {
            converter.addTarget(par,"toc"+(++nTocIndex));
            TocEntry entry = new TocEntry();
            entry.onode = (Element) onode;
            entry.sLabel = sCurrentListLabel;  
            entry.nFileIndex = converter.getOutFileIndex();
            tocEntries.add(entry);
        }
        sCurrentListLabel = null;
		
        if (!bIsEmpty) {
            par = createTextBackground(par, sStyleName);
            traverseInlineText(onode,par);
        }
        else {
            // An empty paragraph (this includes paragraphs that only contains
            // whitespace) is ignored by the browser, hence we add &nbsp;
            par.appendChild( converter.createTextNode("\u00A0") );
        }        
    }
    
    private void prependAsapNode(Node node) {
        if (asapNode!=null) {
            // May float past a split; check this first
            if (asapNode.getOwnerDocument()!=node.getOwnerDocument()) {
                asapNode = converter.importNode(asapNode,true);
            }
            node.appendChild(asapNode); asapNode = null;
        }
    }
	
	
    ///////////////////////////////////////////////////////////////////////////
    // LISTS
    ///////////////////////////////////////////////////////////////////////////
	
    // Helper: Get a list counter for a list style
    private ListCounter getListCounter(ListStyle style) {
        if (style==ofr.getOutlineStyle()) {
            // Outline numbering has a special counter
            return outlineNumbering;
        }
        else if (style!=null) {
            // Get existing or create new counter
            if (listCounters.containsKey(style.getName())) {
                return (ListCounter) listCounters.get(style.getName());
            }
            else {
                ListCounter counter = new ListCounter(style);
                listCounters.put(style.getName(),counter);
                return counter;
            }
        }
        else {
            // No style, return a dummy
            return new ListCounter();
        }
    }
	
    // Helper: Check if a list contains any items
    private boolean hasItems(Node onode) {
        Node child = onode.getFirstChild();
        while (child!=null) {
            if (Misc.isElement(child,XMLString.TEXT_LIST_ITEM) ||
                Misc.isElement(child,XMLString.TEXT_LIST_HEADER)) {
                return true;
            }
            child = child.getNextSibling();
        }
        return false;
    }

    // TODO: Merge these three methods
	
    /*
     * Process a text:ordered-list tag.
     */
    private void handleOL (Node onode, int nLevel, String sStyleName, Node hnode) {
        if (hasItems(onode)) {
            // add an OL element
            Element list = converter.createElement("ol");
            StyleInfo listInfo = new StyleInfo();
            getListSc().applyStyle(nLevel,sStyleName,listInfo);
            applyStyle(listInfo,list);
            hnode.appendChild(list);
            traverseList(onode,nLevel,sStyleName,list);
        }
    }

    /*
     * Process a text:unordered-list tag.  
     */
    private void handleUL (Node onode, int nLevel, String sStyleName, Node hnode) {
        if (hasItems(onode)) {
            // add an UL element
            Element list = converter.createElement("ul");
            StyleInfo listInfo = new StyleInfo();
            getListSc().applyStyle(nLevel,sStyleName,listInfo);
            applyStyle(listInfo,list);
            hnode.appendChild(list);
            traverseList(onode,nLevel,sStyleName,list);
        }
    }
	
    private void handleList(Node onode, int nLevel, String sStyleName, Node hnode) {
        // In OpenDocument, we should use the style to determine the type of list
        String sStyleName1 = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
        if (sStyleName1!=null) { sStyleName = sStyleName1; }
        ListStyle style = ofr.getListStyle(sStyleName);
        if (style!=null && style.isNumber(nLevel)) {
            handleOL(onode,nLevel,sStyleName,hnode);
        }
        else {
            handleUL(onode,nLevel,sStyleName,hnode);
        }
    }

    /*
     * Process the contents of a list (Changed as suggested by Nick Bower)
     * The option xhtml_use_list_hack triggers some *invalid* code:
     * - the attribute start on ol (is valid in html 4 transitional)
     * - the attribute value on li (is valid in html 4 transitional)
     * (these attributes are supposed to be replaced by css, but browsers
     * generally don't support that)
     * - generates <ol><ol><li>...</li></ol></ol> instead of
     *   <ol><li><ol><li>...</li></ol></li></ol> in case the first child of
     *   a list item is a new list. This occurs when a list is *continued* at
     *   level 2 or higher. This hack seems to be the only solution that
     *   actually produces correct results in browsers :-(
     */
    private void traverseList (Node onode, int nLevel, String styleName, Element hnode) {
        ListCounter counter = getListCounter(ofr.getListStyle(styleName));

        // Restart numbering, if required
        if (counter!=null) {
            boolean bContinueNumbering = "true".equals(Misc.getAttribute(onode,XMLString.TEXT_CONTINUE_NUMBERING));
            if (bContinueNumbering) {
                if (config.xhtmlUseListHack()) {
                    hnode.setAttribute("start",Integer.toString(counter.getValue(nLevel)+1));
                }
            }
            else if (counter!=null) {
                counter.restart(nLevel);
            }
        }

        if (onode.hasChildNodes()) {
            NodeList nList = onode.getChildNodes();
            int len = nList.getLength();
            
            for (int i = 0; i < len; i++) {
                Node child = nList.item(i);
                
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String nodeName = child.getNodeName();
                    
                    if (nodeName.equals(XMLString.TEXT_LIST_ITEM)) {
                        // Check to see if first child is a new list
                        boolean bIsImmediateNestedList = false;
                        Element child1 = Misc.getFirstChildElement(child);
                        if (child1.getTagName().equals(XMLString.TEXT_ORDERED_LIST) || // old
                            child1.getTagName().equals(XMLString.TEXT_UNORDERED_LIST) || // old
                            child1.getTagName().equals(XMLString.TEXT_LIST)) { // oasis
                            bIsImmediateNestedList = true;
                        }

                        if (config.xhtmlUseListHack() && bIsImmediateNestedList) {
                            traverseListItem(child,nLevel,styleName,hnode);
                        }
                        else {
                            // add an li element
                            sCurrentListLabel = counter.step(nLevel).getLabel();
                            Element item = converter.createElement("li");
                            StyleInfo info = new StyleInfo();
                            getPresentationSc().applyOutlineStyle(nLevel,info);
                            applyStyle(info,item);
                            hnode.appendChild(item);
                            if (config.xhtmlUseListHack()) {
                                boolean bRestart = "true".equals(Misc.getAttribute(child,
                                    XMLString.TEXT_RESTART_NUMBERING));
                                int nStartValue = Misc.getPosInteger(Misc.getAttribute(child,
                                    XMLString.TEXT_START_VALUE),1);
                                if (bRestart) {
                                    item.setAttribute("value",Integer.toString(nStartValue));
                                    if (counter!=null) {
                                        sCurrentListLabel = counter.restart(nLevel,nStartValue).getLabel();
                                    }
                                }
                            }
                            traverseListItem(child,nLevel,styleName,item);
                        }
                    }
                    if (nodeName.equals(XMLString.TEXT_LIST_HEADER)) {
                        // add an li element
                        Element item = converter.createElement("li");
                        hnode.appendChild(item);
                        item.setAttribute("style","list-style-type:none");
                        traverseListItem(child,nLevel,styleName,item);
                    }
                }
            }
        }
    }
    
    
    /*
     * Process the contents of a list item
     * (a list header should only contain paragraphs, but we don't care)
     */
    private void traverseListItem (Node onode, int nLevel, String styleName, Node hnode) { 
        // First check if we have a single paragraph to be omitted
        // This should happen if we ignore styles and have no style-map
        // for the paragraph style used        
        if (config.xhtmlFormatting()!=XhtmlConfig.CONVERT_ALL && onode.hasChildNodes()) {
            NodeList list = onode.getChildNodes();
            int nLen = list.getLength();
            int nParCount = 0;
            boolean bNoPTag = true;
            for (int i=0; i<nLen; i++) {
                if (list.item(i).getNodeType()==Node.ELEMENT_NODE) {
                    if (list.item(i).getNodeName().equals(XMLString.TEXT_P)) {
                        nParCount++;
                        if (bNoPTag) {
                            String sDisplayName = ofr.getParStyles().getDisplayName(Misc.getAttribute(list.item(0),XMLString.TEXT_STYLE_NAME));
                            if (config.getXParStyleMap().contains(sDisplayName)) {
                                bNoPTag = false;
                            }
                        }
                    }
                    else { // found non-text:p element
                        bNoPTag=false;
                    }
                }
            }
            if (bNoPTag && nParCount<=1) {
                // traverse the list
                for (int i = 0; i < nLen; i++) {
                    Node child = list.item(i);
          
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String nodeName = child.getNodeName();
                    
                        if (nodeName.equals(XMLString.TEXT_P)) {
                            traverseInlineText(child,hnode);
                        }
                        if (nodeName.equals(XMLString.TEXT_LIST)) { // oasis
                            handleList(child,nLevel+1,styleName,hnode);
                        }
                        if (nodeName.equals(XMLString.TEXT_ORDERED_LIST)) { // old
                            handleOL(child,nLevel+1,styleName,hnode);
                        }
                        if (nodeName.equals(XMLString.TEXT_UNORDERED_LIST)) { // old
                            handleUL(child,nLevel+1,styleName,hnode);
                        }
                    }
                }
                return;
            }
        }
        // Still here? - traverse block text as usual!
        traverseBlockText(onode,nLevel,styleName,hnode);
    }
	
    ///////////////////////////////////////////////////////////////////////////
    // FAKE LISTS
    ///////////////////////////////////////////////////////////////////////////
	
    // A fake list is a list which is converted into a sequence of numbered
    // paragraphs rather than into a list.
    // Currently this is done for list which only containsheadings
	
    // Helper: Check to see, if this list contains only headings
    // (If so, we will ignore the list and apply the numbering to the headings)   
    private boolean listIsOnlyHeadings(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                if (nodeName.equals(XMLString.TEXT_LIST_ITEM)) {
                    if (!itemIsOnlyHeadings(child)) return false;
                }
                else if (nodeName.equals(XMLString.TEXT_LIST_HEADER)) {
                    if (!itemIsOnlyHeadings(child)) return false;
                }
            }
            child = child.getNextSibling();
        }
        return true;
    }
    
    private boolean itemIsOnlyHeadings(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                if (nodeName.equals(XMLString.TEXT_LIST)) {
                    if (!listIsOnlyHeadings(child)) return false;
                }
                else if (nodeName.equals(XMLString.TEXT_ORDERED_LIST)) {
                    if (!listIsOnlyHeadings(child)) return false;
                }
                else if (nodeName.equals(XMLString.TEXT_UNORDERED_LIST)) {
                    if (!listIsOnlyHeadings(child)) return false;
                }
                else if(!nodeName.equals(XMLString.TEXT_H)) {
                    return false;
                }
            }
            child = child.getNextSibling();
        }
        return true;
    }
	
    // Splitting may occur inside a fake list, so we return the (new) hnode 
    private Node handleFakeList(Node onode, int nLevel, String sStyleName, Node hnode) {
        String sStyleName1 = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
        if (sStyleName1!=null) { sStyleName = sStyleName1; }
        return traverseFakeList(onode,hnode,nLevel,sStyleName);
    }

    // Traverse a list which is not exported as a list but as a sequence of
    // numbered headings/paragraphs
    private Node traverseFakeList (Node onode, Node hnode, int nLevel, String sStyleName) {
        // Restart numbering?
        boolean bContinueNumbering ="true".equals(
            Misc.getAttribute(onode,XMLString.TEXT_CONTINUE_NUMBERING));
        if (!bContinueNumbering) {
            getListCounter(ofr.getListStyle(sStyleName)).restart(nLevel);
        }

        Node child = onode.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sNodeName = child.getNodeName();
                
                if (sNodeName.equals(XMLString.TEXT_LIST_ITEM)) {
                    boolean bRestart = "true".equals(Misc.getAttribute(child,
                        XMLString.TEXT_RESTART_NUMBERING));
                    int nStartValue = Misc.getPosInteger(Misc.getAttribute(child,
                        XMLString.TEXT_START_VALUE),1);
                    hnode = traverseFakeListItem(child, hnode, nLevel, sStyleName, false, bRestart, nStartValue);
                }
                else if (sNodeName.equals(XMLString.TEXT_LIST_HEADER)) {
                    hnode = traverseFakeListItem(child, hnode, nLevel, sStyleName, true, false, 0);
                }
            }
            child = child.getNextSibling();
        }
        return hnode;
    }
    
    
    // Process the contents of a fake list item
    private Node traverseFakeListItem (Node onode, Node hnode, int nLevel,
        String sStyleName, boolean bUnNumbered, boolean bRestart, int nStartValue) { 
        Node child = onode.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sNodeName = child.getNodeName();
            
                if (sNodeName.equals(XMLString.TEXT_H)) {
                    nDontSplitLevel++;
                    int nOutlineLevel = getOutlineLevel((Element)onode);
                    Node rememberNode = hnode;
                    hnode = maybeSplit(hnode,nOutlineLevel,bAfterHeading);
                    handleHeading(child, hnode, rememberNode!=hnode,
                        ofr.getListStyle(sStyleName), nLevel,
                        bUnNumbered, bRestart, nStartValue);
                    nDontSplitLevel--;
                    if (nDontSplitLevel==0) { bAfterHeading=true; }
                }
                else if (sNodeName.equals(XMLString.TEXT_P)) {
                     // Currently we only handle fakes lists containing headings
                }
                else if (sNodeName.equals(XMLString.TEXT_LIST)) { // oasis
                     return traverseFakeList(child, hnode, nLevel+1, sStyleName);
                }
                else if (sNodeName.equals(XMLString.TEXT_ORDERED_LIST)) { // old
                     return traverseFakeList(child, hnode, nLevel+1, sStyleName);
                }
                else if (sNodeName.equals(XMLString.TEXT_UNORDERED_LIST)) { // old
                     return traverseFakeList(child, hnode, nLevel+1, sStyleName);
                }
            }
            child = child.getNextSibling();
        }
        return hnode;
    }

	
    //////////////////////////////////////////////////////////////////////////
    // INDEXES
    //////////////////////////////////////////////////////////////////////////
		    
    /* Process table of contents
     */
    private void handleTOC(Node onode, Node hnode) {
        if (!ofr.getTocReader((Element)onode).isByChapter()) { 
            nTocFileIndex = converter.getOutFileIndex(); 
        }

        Element div = converter.createElement("div");
        hnode.appendChild(div);

        IndexData data = new IndexData();
        data.nOutFileIndex = converter.getOutFileIndex();
        data.onode = (Element) onode;
        data.chapter = currentChapter;
        data.hnode = (Element) div;
        indexes.add(data); // to be processed later with generateTOC
    }

    private void generateToc(IndexData data) {
        Element onode = data.onode;
        Element chapter = data.chapter;
        Element div = data.hnode;

        int nSaveOutFileIndex = converter.getOutFileIndex();
        converter.changeOutFile(data.nOutFileIndex);
 
        bInToc = true;
        TocReader tocReader = ofr.getTocReader(onode);

        StyleInfo sectionInfo = new StyleInfo();
        getSectionSc().applyStyle(tocReader.getStyleName(),sectionInfo);
        applyStyle(sectionInfo,div);

        if (tocReader.getName()!=null) { converter.addTarget(div,tocReader.getName()); }
        // Generate title
        Element title = tocReader.getIndexTitleTemplate();
        if (title!=null) {
            String sStyleName = Misc.getAttribute(title,XMLString.TEXT_STYLE_NAME);
            Element p = createParagraph(div,sStyleName);
            traversePCDATA(title,p);
        }
			
        // TODO: Read the entire content of the entry templates!
        String[] sEntryStyleName = new String[11];
        for (int i=1; i<=10; i++) {
            Element entryTemplate = tocReader.getTocEntryTemplate(i);
            if (entryTemplate!=null) {
                sEntryStyleName[i] = Misc.getAttribute(entryTemplate,XMLString.TEXT_STYLE_NAME);
            }
        }

        int nStart = 0;
        int nLen = tocEntries.size();

        // Find the chapter
        if (tocReader.isByChapter() && chapter!=null) {
            for (int i=0; i<nLen; i++) {
                TocEntry entry = (TocEntry) tocEntries.get(i);
                if (entry.onode==chapter) { nStart=i; break; }
            }
            
        }

        // Generate entries
        for (int i=nStart; i<nLen; i++) {
            TocEntry entry = (TocEntry) tocEntries.get(i);
            String sNodeName = entry.onode.getTagName();
            if (XMLString.TEXT_H.equals(sNodeName)) {
                int nLevel = getOutlineLevel(entry.onode);

                if (nLevel==1 && tocReader.isByChapter() && entry.onode!=chapter) { break; }
                if (tocReader.useOutlineLevel() && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createParagraph(div,sEntryStyleName[nLevel]);
                    if (entry.sLabel!=null) {
                        Element span = converter.createElement("span");
                        p.appendChild(span);
                        span.setAttribute("class","SectionNumber");
                        span.appendChild(converter.createTextNode(entry.sLabel));
                    }
                    Element a = converter.createLink("toc"+i);
                    p.appendChild(a);
                    traverseInlineText(entry.onode,a);
                }
                else {
                    String sStyleName = getParSc().getRealParStyleName(entry.onode.getAttribute(XMLString.TEXT_STYLE_NAME));
                    nLevel = tocReader.getIndexSourceStyleLevel(sStyleName);
                    if (tocReader.useIndexSourceStyles() && 1<=nLevel && nLevel<=tocReader.getOutlineLevel()) {
                        Element p = createParagraph(div,sEntryStyleName[nLevel]);
                        if (entry.sLabel!=null) {
                            p.appendChild(converter.createTextNode(entry.sLabel));
                        }
                        Element a = converter.createLink("toc"+i);
                        p.appendChild(a);
                        traverseInlineText(entry.onode,a);
                    }
                }
            }
            else if (XMLString.TEXT_P.equals(sNodeName)) {
                String sStyleName = getParSc().getRealParStyleName(entry.onode.getAttribute(XMLString.TEXT_STYLE_NAME));
                int nLevel = tocReader.getIndexSourceStyleLevel(sStyleName);
                if (tocReader.useIndexSourceStyles() && 1<=nLevel && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createParagraph(div,sEntryStyleName[nLevel]);
                    if (entry.sLabel!=null) {
                        p.appendChild(converter.createTextNode(entry.sLabel));
                    }
                    Element a = converter.createLink("toc"+i);
                    p.appendChild(a);
                    traverseInlineText(entry.onode,a);
                }
            }
            else if (XMLString.TEXT_TOC_MARK.equals(sNodeName)) {
                int nLevel = Misc.getPosInteger(entry.onode.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
                if (tocReader.useIndexMarks() && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createParagraph(div,sEntryStyleName[nLevel]);
                    Element a = converter.createLink("toc"+i);
                    p.appendChild(a);
                    a.appendChild(converter.createTextNode(IndexMark.getIndexValue(entry.onode)));
                }
            }
            else if (XMLString.TEXT_TOC_MARK_START.equals(sNodeName)) {
                int nLevel = Misc.getPosInteger(entry.onode.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
                if (tocReader.useIndexMarks() && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createParagraph(div,sEntryStyleName[nLevel]);
                    Element a = converter.createLink("toc"+i);
                    p.appendChild(a);
                    a.appendChild(converter.createTextNode(IndexMark.getIndexValue(entry.onode)));
                }
            }
        }
        bInToc = false;
		
        converter.changeOutFile(nSaveOutFileIndex);
    }

    /*
     * Process list of illustrations
     */
    private void handleLOF (Node onode, Node hnode) {
        // later
    }

    /*
     * Process list of tables
     */
    private void handleLOT (Node onode, Node hnode) {
        // later
    }

    /*
     * Process Object index
     */
    private void handleObjectIndex (Node onode, Node hnode) {
        // later
    }

    /*
     * Process User index
     */
    private void handleUserIndex (Node onode, Node hnode) {
        // later
    }

    /*
     * Process Alphabetical index
     */
    private void handleAlphabeticalIndex (Node onode, Node hnode) {
        nAlphabeticalIndex = converter.getOutFileIndex();
        Node source = Misc.getChildByTagName(onode,XMLString.TEXT_ALPHABETICAL_INDEX_SOURCE);
        if (source!=null) {
            Element div = converter.createElement("div");
            converter.addTarget(div,"alphabeticalindex");
            hnode.appendChild(div);
            // Generate title
            Node title = Misc.getChildByTagName(source,XMLString.TEXT_INDEX_TITLE_TEMPLATE);
            if (title!=null) {
                String sStyleName = Misc.getAttribute(title,XMLString.TEXT_STYLE_NAME);
                Element p = createParagraph(div,sStyleName);
                traversePCDATA(title,p);
            }
            // Collect style name for entries
            // TODO: Should read the entire template
            String sEntryStyleName = null;
            if (source.hasChildNodes()) {
                NodeList nl = source.getChildNodes();
                int nLen = nl.getLength();
                for (int i = 0; i < nLen; i++) {
                    Node child = nl.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE
                        && child.getNodeName().equals(XMLString.TEXT_ALPHABETICAL_INDEX_ENTRY_TEMPLATE)) {
		                // Note: There are actually three outline-levels: separator, 1, 2 and 3
                        int nLevel = Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_OUTLINE_LEVEL),1);
                        if (nLevel==1) {
                            sEntryStyleName = Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME);
                        }
                    }	                        
                }
            }
            // Sort the index entries
            Collator collator;
            String sLanguage = Misc.getAttribute(source,XMLString.FO_LANGUAGE);
            if (sLanguage==null) { // use default locale
                collator = Collator.getInstance();
            }
            else {
                String sCountry = Misc.getAttribute(source,XMLString.FO_COUNTRY);
                if (sCountry==null) { sCountry=""; }
                collator = Collator.getInstance(new Locale(sLanguage,sCountry));
            }
            for (int i = 0; i<=nIndexIndex; i++) {
                for (int j = i+1; j<=nIndexIndex ; j++) {
                    AlphabeticalEntry entryi = (AlphabeticalEntry) index.get(i);
                    AlphabeticalEntry entryj = (AlphabeticalEntry) index.get(j);
                    if (collator.compare(entryi.sWord, entryj.sWord) > 0) {
                        index.set(i,entryj);
                        index.set(j,entryi);
                    }
                }
            }
            // Generate the index
            Element table = converter.createElement("table");
            table.setAttribute("style","width:100%");
            div.appendChild(table);
            Element tr = converter.createElement("tr");
            table.appendChild(tr);
            Element[] td = new Element[4];
            for (int i=0; i<4; i++) {
                td[i] = converter.createElement("td");
                td[i].setAttribute("style","vertical-align:top");
                tr.appendChild(td[i]);
            }
            int nColEntries = nIndexIndex/4+1;
            int nColIndex = -1;
            for (int i=0; i<=nIndexIndex; i++) {
                if (i%nColEntries==0) { nColIndex++; } 
                AlphabeticalEntry entry = (AlphabeticalEntry) index.get(i);
                Element p = createParagraph(td[nColIndex],sEntryStyleName);
                Element a = converter.createLink("idx"+entry.nIndex);
                p.appendChild(a);
                a.appendChild(converter.createTextNode(entry.sWord));
            }
        }
        
    }

    /*
     * Process Bibliography
     */
    private void handleBibliography (Node onode, Node hnode) {
        // Use the content, not the template
        // This is a temp. solution. Later we want to be able to create
        // hyperlinks from the bib-item to the actual entry in the bibliography,
        // so we have to recreate the bibliography from the template.
        Node body = Misc.getChildByTagName(onode,XMLString.TEXT_INDEX_BODY);
        if (body!=null) {
            Element div = converter.createElement("div");
            converter.addTarget(div,"bibliography");
            hnode.appendChild(div);
            //asapNode = converter.createTarget("bibliography");
            Node title = Misc.getChildByTagName(body,XMLString.TEXT_INDEX_TITLE);
            if (title!=null) { traverseBlockText(title,div); }
            traverseBlockText(body,div);
        }     
    }

    ////////////////////////////////////////////////////////////////////////
    // INLINE TEXT
    ////////////////////////////////////////////////////////////////////////
	
    /* Process floating frames bound to this inline text (ie. paragraph) */
    private void traverseFloats(Node onode, Node hnodeBlock, Node hnodeInline) {
        Node child = onode.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                Element elm = (Element) child;
                String sTag = elm.getTagName();
                if (OfficeReader.isDrawElement(elm)) {
                    elm = getDrawCv().getRealDrawElement(elm);
                    if (elm!=null) {
                        String sAnchor = elm.getAttribute(XMLString.TEXT_ANCHOR_TYPE);
                        // Convert only floating frames; text-boxes must always float
                        if (!"as-char".equals(sAnchor)) {
                            getDrawCv().handleDrawElement(elm,(Element)hnodeBlock,
                                (Element)hnodeInline,nFloatMode);
                        }
                        else if (XMLString.DRAW_TEXT_BOX.equals(sTag)) {
                            getDrawCv().handleDrawElement(elm,(Element)hnodeBlock,
                                (Element)hnodeInline,DrawConverter.INLINE);
                        }
                    }
                }
                else if (OfficeReader.isTextElement(elm)) {
                    // Do not descend into {foot|end}notes
                    if (!OfficeReader.isNoteElement(elm)) {
                        traverseFloats(elm,hnodeBlock,hnodeInline);
                    }
                }
            }
            child = child.getNextSibling();
        }
    }

    /*
     * Process inline text
     */
    private void traverseInlineText (Node onode,Node hnode) {        
        //String styleName = Misc.getAttribute(onode, XMLString.TEXT_STYLE_NAME);
                              
        if (onode.hasChildNodes()) {
            NodeList nList = onode.getChildNodes();
            int nLen = nList.getLength();
                       
            for (int i = 0; i < nLen; i++) {
                
                Node child = nList.item(i);
                short nodeType = child.getNodeType();
               
                switch (nodeType) {
                    case Node.TEXT_NODE:
                        String s = child.getNodeValue();
                        if (s.length() > 0) {
                            hnode.appendChild( converter.createTextNode(s) );
                        }
                        break;
                        
                    case Node.ELEMENT_NODE:
                        String sName = child.getNodeName();
                        if (OfficeReader.isDrawElement(child)) {
                            Element elm = getDrawCv().getRealDrawElement((Element)child);
                            if (elm!=null) {
                                String sAnchor = (elm.getAttribute(XMLString.TEXT_ANCHOR_TYPE));
                                if ("as-char".equals(sAnchor)) {
                                    getDrawCv().handleDrawElement(elm,null,(Element)hnode,DrawConverter.INLINE);
                                }
                            }
                        }
                        else if (child.getNodeName().equals(XMLString.TEXT_S)) {
                            if (config.ignoreDoubleSpaces()) {
                                hnode.appendChild( converter.createTextNode(" ") );
                            }
                            else {
                                int count= Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_C),1);
                                for ( ; count > 0; count--) {
                                    hnode.appendChild( converter.createTextNode("\u00A0") );
                                }
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_TAB_STOP)) {
                            handleTabStop(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_TAB)) { // oasis
                            handleTabStop(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_LINE_BREAK)) {
                            if (!config.ignoreHardLineBreaks()) {
                                hnode.appendChild( converter.createElement("br") );
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_SPAN)) {
                            handleSpan(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_A)) {
                            handleAnchor(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_FOOTNOTE)) {
                            handleFootnote(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_ENDNOTE)) {
                            handleEndnote(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_NOTE)) { // oasis
                            if ("endnote".equals(Misc.getAttribute(child,XMLString.TEXT_NOTE_CLASS))) {
                                handleEndnote(child,hnode);
                            }
                            else {
                                handleFootnote(child,hnode);
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_SEQUENCE)) {
	                        handleSequence(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_PAGE_NUMBER)) {
	                        handlePageNumber(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_PAGE_COUNT)) {
	                        handlePageCount(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_SEQUENCE_REF)) {
	                        handleSequenceRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_FOOTNOTE_REF)) {
	                        handleNoteRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_ENDNOTE_REF)) {
	                        handleNoteRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_NOTE_REF)) { // oasis
	                        handleNoteRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK)) {
	                        handleReferenceMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK_START)) {
	                        handleReferenceMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_REF)) {
	                        handleReferenceRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK)) {
	                        handleBookmark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK_START)) {
	                        handleBookmark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK_REF)) {
	                        handleBookmarkRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK)) {
	                        handleAlphabeticalIndexMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK_START)) {
	                        handleAlphabeticalIndexMarkStart(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_TOC_MARK)) {
	                        handleTocMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_TOC_MARK_START)) {
	                        handleTocMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_BIBLIOGRAPHY_MARK)) {
	                        handleBibliographyMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.OFFICE_ANNOTATION)) {
                            converter.handleOfficeAnnotation(child,hnode);
                        }
						else if (sName.startsWith("text:")) {
							 traverseInlineText(child,hnode);
						}
                        // other tags are ignored;
                        break;
                    default:
                        // Do nothing
                }
            }
        }
    }
	
    private void handleTabStop(Node onode, Node hnode) {
        // xhtml does not have tab stops, we export a space, which the
        // user may choose to format
        if (config.getXhtmlTabstopStyle().length()>0) {
            Element span = converter.createElement("span");
            hnode.appendChild(span);
            span.setAttribute("class",config.getXhtmlTabstopStyle());
            span.appendChild(converter.createTextNode(" "));
        }
        else {
            hnode.appendChild(converter.createTextNode(" "));
        }
    }
	
    private void handleSpan(Node onode, Node hnode) {
    	if (!bInToc) {
    		String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
    		Element span = createInline((Element) hnode,sStyleName);
    		traverseInlineText(onode,span);
    	}
    	else {
    		traverseInlineText(onode,hnode);
    	}
    }

    private void traversePCDATA(Node onode, Node hnode) {
        if (onode.hasChildNodes()) {
            NodeList nl = onode.getChildNodes();
            int nLen = nl.getLength();
            for (int i=0; i<nLen; i++) {
                if (nl.item(i).getNodeType()==Node.TEXT_NODE) {
                    hnode.appendChild( converter.createTextNode(nl.item(i).getNodeValue()) );
                }
            }
        }
    }
    
    protected void handleAnchor(Node onode, Node hnode) {
        Element anchor = converter.createLink((Element)onode);
        hnode.appendChild(anchor);
        traverseInlineText(onode,anchor);
    }

    /* Process a footnote */
    private void handleFootnote(Node onode, Node hnode) {
        String sId = Misc.getAttribute(onode,XMLString.TEXT_ID);
		Element span = createInline((Element) hnode,sFntCitBodyStyle);
        // Create target and back-link
        Element link = converter.createLink(sId);
        converter.addTarget(link,"body"+sId);
		span.appendChild(link);
        Node citation = Misc.getChildByTagName(onode,XMLString.TEXT_FOOTNOTE_CITATION);
        if (citation==null) { // try oasis
            citation = Misc.getChildByTagName(onode,XMLString.TEXT_NOTE_CITATION);
        }
        traversePCDATA(citation,link);
        footnotes.add(onode);
	} 
	
    public void insertFootnotes(Node hnode) {
        int n = footnotes.size();
        for (int i=0; i<n; i++) {
            Node footnote = (Node) footnotes.get(i);
            String sId = Misc.getAttribute(footnote,XMLString.TEXT_ID); 
            Node citation = Misc.getChildByTagName(footnote,XMLString.TEXT_FOOTNOTE_CITATION);
            if (citation==null) { // try oasis
                citation = Misc.getChildByTagName(footnote,XMLString.TEXT_NOTE_CITATION);
            }
            Node body = Misc.getChildByTagName(footnote,XMLString.TEXT_FOOTNOTE_BODY);
            if (body==null) { // try oasis
                body = Misc.getChildByTagName(footnote,XMLString.TEXT_NOTE_BODY);
            }
            traverseNoteBody(sId,sFntCitStyle,citation,body,hnode);
        }
        footnotes.clear();
    }

    /* Process an endnote */
    private void handleEndnote(Node onode, Node hnode) {
        String sId = Misc.getAttribute(onode,XMLString.TEXT_ID);
		Element span = createInline((Element) hnode,sEntCitBodyStyle);
        // Create target and back-link
        Element link = converter.createLink(sId);
        converter.addTarget(link,"body"+sId);
		span.appendChild(link);
        Node citation = Misc.getChildByTagName(onode,XMLString.TEXT_ENDNOTE_CITATION);
        if (citation==null) { // try oasis
            citation = Misc.getChildByTagName(onode,XMLString.TEXT_NOTE_CITATION);
        }
        traversePCDATA(citation,link);
        endnotes.add(onode);
	} 

    public void insertEndnotes(Node hnode) {
        int n = endnotes.size();
        if (nSplit>0 && n>0) { hnode = converter.nextOutFile(); }
        for (int i=0; i<n; i++) {
            Node endnote = (Node) endnotes.get(i);
            String sId = Misc.getAttribute(endnote,XMLString.TEXT_ID); 
            Node citation = Misc.getChildByTagName(endnote,XMLString.TEXT_ENDNOTE_CITATION);
            if (citation==null) { // try oasis
                citation = Misc.getChildByTagName(endnote,XMLString.TEXT_NOTE_CITATION);
            }
            Node body = Misc.getChildByTagName(endnote,XMLString.TEXT_ENDNOTE_BODY);
            if (body==null) { // try oasis
                body = Misc.getChildByTagName(endnote,XMLString.TEXT_NOTE_BODY);
            }
            traverseNoteBody(sId,sEntCitStyle,citation,body,hnode);
        }
    }

	/*
     * Process the contents of a footnote or endnote
     */
    private void traverseNoteBody (String sId, String sCitStyle, Node citation,Node onode, Node hnode) {
        // Create the anchor/footnote symbol:
        // Create target and link
        Element link = converter.createLink("body"+sId);
        converter.addTarget(link,sId);
        StyleInfo linkInfo = new StyleInfo();
        getTextSc().applyStyle(sCitStyle,linkInfo);
        applyStyle(linkInfo,link);
        traversePCDATA(citation,link);
        // Add a space and save it for later insertion:
        Element span = converter.createElement("span");
        span.appendChild(link);
        span.appendChild(converter.createTextNode(" "));
        asapNode = span;
		
        traverseBlockText(onode,hnode);

        /*if (onode.hasChildNodes()) {
            NodeList nList = onode.getChildNodes();
            int len = nList.getLength();
            
            for (int i = 0; i < len; i++) {
                Node child = nList.item(i);
                
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String nodeName = child.getNodeName();
                    
                    if (nodeName.equals(XMLString.TEXT_H)) {
                        handleHeading(child,hnode);
                    }

                    if (nodeName.equals(XMLString.TEXT_P)) {
                        handleParagraph(child,hnode);
                    }
                    
                    if (nodeName.equals(XMLString.TEXT_ORDERED_LIST)) {
                        handleOL(child,0,null,hnode);
                    }
                    
                    if (nodeName.equals(XMLString.TEXT_UNORDERED_LIST)) {
                        handleUL(child,0,null,hnode);
                    }
                }
            }
        }*/
    }

    private void handlePageNumber(Node onode, Node hnode) {
        // doesn't make any sense...
        hnode.appendChild( converter.createTextNode("(Page number)") );
    }
    
    private void handlePageCount(Node onode, Node hnode) {
       // also no sense
        hnode.appendChild( converter.createTextNode("(Page count)") );
    }

    private void handleSequence(Node onode, Node hnode) {
        // Use current value, but turn references into hyperlinks
        String sName = Misc.getAttribute(onode,XMLString.TEXT_REF_NAME);
        if (sName!=null && !bInToc) {
            Element anchor = converter.createTarget("seq"+sName);
            hnode.appendChild(anchor);
            traversePCDATA(onode,anchor);
        }
        else {
            traversePCDATA(onode,hnode);
        }        
    }
	
    private void createReference(Node onode, Node hnode, String sPrefix) {
        // Turn reference into hyperlink
        String sFormat = Misc.getAttribute(onode,XMLString.TEXT_REFERENCE_FORMAT);
        String sName = Misc.getAttribute(onode,XMLString.TEXT_REF_NAME);
        Element anchor = converter.createLink(sPrefix+sName);
        hnode.appendChild(anchor);
        if ("page".equals(sFormat)) { // all page numbers are 1 :-)
            anchor.appendChild( converter.createTextNode("1") );
        }
        else { // in other cases use current value
            traversePCDATA(onode,anchor);
        }
    }
		
    private void handleSequenceRef(Node onode, Node hnode) {
        createReference(onode,hnode,"seq");
    } 

    private void handleNoteRef(Node onode, Node hnode) {
        createReference(onode,hnode,"");
    } 
        
    private void handleReferenceMark(Node onode, Node hnode) {
        String sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        if (sName!=null && !bInToc) {
            hnode.appendChild(converter.createTarget("ref"+sName));
        }
    }
	
    private void handleReferenceRef(Node onode, Node hnode) {
        createReference(onode,hnode,"ref");
    } 

    private void handleBookmark(Node onode, Node hnode) {
        // Note: Two targets (may be the target of a hyperlink or a reference)
        String sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        if (sName!=null && !bInToc) {
            hnode.appendChild(converter.createTarget(sName));
            hnode.appendChild(converter.createTarget("bkm"+sName));
        }
    }
	
    private void handleBookmarkRef(Node onode, Node hnode) {
        createReference(onode,hnode,"bkm");
    } 
	
    private void handleAlphabeticalIndexMark(Node onode, Node hnode) {
        if (bInToc) { return; }
        String sWord = Misc.getAttribute(onode,XMLString.TEXT_STRING_VALUE);
        if (sWord==null) { return; }
        AlphabeticalEntry entry = new AlphabeticalEntry();
        entry.sWord = sWord; entry.nIndex = ++nIndexIndex; 
        index.add(entry);
        hnode.appendChild(converter.createTarget("idx"+nIndexIndex));
    }

    private void handleAlphabeticalIndexMarkStart(Node onode, Node hnode) {
        if (bInToc) { return; }
        String sWord = IndexMark.getIndexValue(onode);
        if (sWord==null) { return; }
        AlphabeticalEntry entry = new AlphabeticalEntry();
        entry.sWord = sWord; entry.nIndex = ++nIndexIndex; 
        index.add(entry);
        hnode.appendChild(converter.createTarget("idx"+nIndexIndex));
    }
	
    private void handleTocMark(Node onode, Node hnode) {
        hnode.appendChild(converter.createTarget("toc"+(++nTocIndex)));
        TocEntry entry = new TocEntry();
        entry.onode = (Element) onode;
        entry.nFileIndex = converter.getOutFileIndex();
        tocEntries.add(entry);
    }
	
    private void handleBibliographyMark(Node onode, Node hnode) {
        if (bInToc) {
            traversePCDATA(onode,hnode);
        }
        else {
            Element anchor = converter.createLink("bibliography");
            hnode.appendChild(anchor);
            traversePCDATA(onode,anchor);
        }
    }
	
    ///////////////////////////////////////////////////////////////////////////
    // UTILITY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /* apply hard formatting attribute style maps */
    private Element applyAttributes(Element node, StyleWithProperties style) {
        // Do nothing if we convert hard formatting
        if (config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES) { return node; }
        // Do nothing if this is not an automatic style
        if (style==null) { return node; }
        if (!style.isAutomatic()) { return node; }
        node = applyAttribute(node,"bold",getTextSc().isBold(style));
        node = applyAttribute(node,"italics",getTextSc().isItalics(style));
        node = applyAttribute(node,"fixed",getTextSc().isFixed(style));
        node = applyAttribute(node,"superscript",getTextSc().isSuperscript(style));
        node = applyAttribute(node,"subscript",getTextSc().isSubscript(style));
        return node;
    }
	
    /* apply hard formatting attribute style maps */
    private Element applyAttribute(Element node, String sAttr, boolean bApply) {
        if (!bApply) { return node; }
        XhtmlStyleMap xattr = config.getXAttrStyleMap();
        if (!xattr.contains(sAttr)) { return node; }
        Element attr = converter.createElement(xattr.getElement(sAttr));
        if (!"(none)".equals(xattr.getCss(sAttr))) {
            attr.setAttribute("class",xattr.getCss(sAttr));
        }
        node.appendChild(attr);
        return attr;
    }
	
    /* Create a styled paragraph node */
    private Element createParagraph(Element node, String sStyleName) {
        StyleInfo info = new StyleInfo();
        getParSc().applyStyle(sStyleName,info);
        Element par = converter.createElement(info.sTagName);
        node.appendChild(par);
        applyStyle(info,par);
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style!=null && style.isAutomatic()) {
            return applyAttributes(par,style);
        }
        else {
            return par;
        }
    }
	
    /* Create an inline node with background style from paragraph style */
    private Element createTextBackground(Element node, String sStyleName) {
        if (config.xhtmlFormatting()==XhtmlConfig.IGNORE_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD) {
            return node;
        } 
        String sBack = getParSc().getTextBackground(sStyleName);
        if (sBack.length()>0) {
            Element span = converter.createElement("span");
            span.setAttribute("style",sBack);
            node.appendChild(span);
            return span;
        }
        else {
            return node;
        }
    }
		
    /* Create a styled inline node */
    private Element createInline(Element node, String sStyleName) {
        StyleInfo info = new StyleInfo();
        getTextSc().applyStyle(sStyleName,info);
        Element newNode = node;
        if (info.hasAttributes() || !"span".equals(info.sTagName)) {
            // We need to create a new element
            newNode = converter.createElement(info.sTagName);
            node.appendChild(newNode);
            applyStyle(info,newNode);
       }
       return applyAttributes(newNode,ofr.getTextStyle(sStyleName));
    }

    private int getOutlineLevel(Element node) {
        return ofr.isOpenDocument() ?
            Misc.getPosInteger(node.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1):
            Misc.getPosInteger(node.getAttribute(XMLString.TEXT_LEVEL),1);
    }


	
}


