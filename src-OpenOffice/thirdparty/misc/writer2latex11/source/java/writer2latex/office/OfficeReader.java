/************************************************************************
 *
 *  OfficeReader.java
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
 *  Version 1.0 (2008-04-02)
 *
 */

package writer2latex.office;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import writer2latex.xmerge.OfficeDocument;
import writer2latex.util.Misc;

/** <p> This class reads and collects global information about an OOo document.
  * This includes styles, forms, information about indexes and references etc.
  * </p> 
  */
public class OfficeReader {

    ///////////////////////////////////////////////////////////////////////////
    // Static methods
	
    /** Checks, if a node is an element in the text namespace
     *  @param node the node to check
     *  @return true if this is a text element
     */
    public static boolean isTextElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               node.getNodeName().startsWith(XMLString.TEXT_);
    }

    /** Checks, if a node is an element in the table namespace
     *  @param node the node to check
     *  @return true if this is a table element
     */
    public static boolean isTableElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               node.getNodeName().startsWith(XMLString.TABLE_);
    }

    /** Checks, if a node is an element in the draw namespace
     *  @param node the node to check
     *  @return true if this is a draw element
     */
    public static boolean isDrawElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               node.getNodeName().startsWith(XMLString.DRAW_);
    }
	
    /** Checks, if a node is an element representing a note (footnote/endnote)
     *  @param node the node to check
     *  @return true if this is a note element
     */
    public static boolean isNoteElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               ( node.getNodeName().equals(XMLString.TEXT_NOTE)     ||
                 node.getNodeName().equals(XMLString.TEXT_FOOTNOTE) ||
                 node.getNodeName().equals(XMLString.TEXT_ENDNOTE)  );
    }
	
    /** Checks, if this node contains at most one element, and that this is a
     *  paragraph.
     *  @param node the node to check
     *  @return true if the node contains a single paragraph or nothing
     */
    public static boolean isSingleParagraph(Node node) {
        boolean bFoundPar = false;
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                if (child.getNodeName().equals(XMLString.TEXT_P)) {
                    if (bFoundPar) { return false; }
                    else { bFoundPar = true; }
                }
                else {
                    return false;
                }
            }
            child = child.getNextSibling();
        }
        return bFoundPar;
    }
	
    /** <p>Checks, if the only text content of this node is whitespace</p>
     *  @param node the node to check (should be a paragraph node or a child
     *  of a paragraph node)
     *  @return true if the node contains whitespace only
     */
    public static boolean isWhitespaceContent(Node node) {
	    Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
            	if (isTextElement(child)) {
                    if (!isWhitespaceContent(child)) { return false; }
                }
                else {
                    return false; // found non-text content!
                }
            }
            else if (child.getNodeType()==Node.TEXT_NODE) {
                if (!isWhitespace(child.getNodeValue())) { return false; }
            }
            child = child.getNextSibling();
        }
        return true; // found nothing!
    }
	
    /** <p>Checks, if this text is whitespace</p>
     *  @param s the String to check
     *  @return true if the String contains whitespace only
     */
    public static boolean isWhitespace(String s) {
        int nLen = s.length();
        for (int i=0; i<nLen; i++) {
            if (!Character.isWhitespace(s.charAt(i))) { return false; }
        }
        return true;
    }
	
    /** Counts the number of characters (text nodes) in this element
     *  excluding footnotes etc.
     *  @param node the node to count in
     *  @return the number of characters
     */
    public static int getCharacterCount(Node node) {
        Node child = node.getFirstChild();
        int nCount = 0;
        while (child!=null) {
            short nodeType = child.getNodeType();
               
            switch (nodeType) {
                case Node.TEXT_NODE:
                    nCount += child.getNodeValue().length();
                    break;
                        
                case Node.ELEMENT_NODE:
                    String sName = child.getNodeName();
                    if (sName.equals(XMLString.TEXT_S)) {
                        nCount += Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_C),1);
                    }
                    else if (sName.equals(XMLString.TEXT_TAB_STOP)) {
                        nCount++; // treat as single space
                    }
                    else if (sName.equals(XMLString.TEXT_TAB)) { // oasis
                        nCount++; // treat as single space
                    }
                    else if (isNoteElement(child)) {
                        // ignore
                    }
                    else if (isTextElement(child)) {
                        nCount += getCharacterCount(child);
                    }
            }
            child = child.getNextSibling();
        }
        return nCount;
    }

    public String getTextContent(Node node) {
        String s = "";
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                s += getTextContent(child);
            }
            else if (child.getNodeType()==Node.TEXT_NODE) {
                s += child.getNodeValue();
            }
            child = child.getNextSibling();
        }
        return s;
    }
	
    /** Return the next character in logical order
     */
    public static char getNextChar(Node node) {
        Node next = node;
        do {
            next = getNextNode(next);
            if (next!=null && next.getNodeType()==Node.TEXT_NODE &&
                next.getNodeValue().length()>0) {
                // Found the next character!
                return next.getNodeValue().charAt(0);
            }
            //else if (next!=null && next.getNodeType()==Node.ELEMENT_NODE &&
                //XMLString.TEXT_S.equals(next.getNodeName())) {
                // Next character is a space (first of several)
                //return ' ';
            //}
        } while (next!=null);
        // No more text in this paragraph!
        return '\u0000';
    }

    // Return the next node of *this paragraph* in logical order
    // (Parents before children, siblings from left to right)
    // Do not descend into draw elements and footnotes/endnotes
    private static Node getNextNode(Node node) {
        // If element node: Next node is first child
        if (node.getNodeType()==Node.ELEMENT_NODE && node.hasChildNodes() &&
            !isDrawElement(node) && !isNoteElement(node)) {
            return node.getFirstChild();
        }
        // else iterate for next node, but don't leave this paragraph
        Node next = node;
        do {
            // First look for next sibling
            if (next.getNextSibling()!=null) { return next.getNextSibling(); }
            // Then move to parent, if this is the text:p node, we are done
            next = next.getParentNode();
            if (next.getNodeType()==Node.ELEMENT_NODE &&
                next.getNodeName().equals(XMLString.TEXT_P)) {
                return null;
            }
        } while (next!=null);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Data
	
    // The Document
    private OfficeDocument oooDoc = null;

    // Font declarations
    private OfficeStyleFamily font = new OfficeStyleFamily(FontDeclaration.class);

    // Styles
    private OfficeStyleFamily text = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily par = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily section = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily table = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily column = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily row = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily cell = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily frame = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily presentation = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily drawingPage = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily list = new OfficeStyleFamily(ListStyle.class);
    private OfficeStyleFamily pageLayout = new OfficeStyleFamily(PageLayout.class);
    private OfficeStyleFamily masterPage = new OfficeStyleFamily(MasterPage.class);

    // Document-wide styles
    private ListStyle outline = new ListStyle();
    private PropertySet footnotes = null;
    private PropertySet endnotes = null;
	
    // Special styles
    private StyleWithProperties[] heading = new StyleWithProperties[11];
    private MasterPage firstMasterPage = null;
    //private String sFirstMasterPageName = null;
	
    // All indexes
    private Hashtable indexes = new Hashtable();
    private HashSet indexSourceStyles = new HashSet();
    private HashSet figureSequenceNames = new HashSet();
    private HashSet tableSequenceNames = new HashSet();
    private String sAutoFigureSequenceName = null;
    private String sAutoTableSequenceName = null;
	
    // Map paragraphs to sequence names (caption helper)
    private Hashtable sequenceNames = new Hashtable();
	
    // Map sequence reference names to sequence names
    private Hashtable seqrefNames = new Hashtable();
	
    // All references
    private HashSet footnoteRef = new HashSet();
    private HashSet endnoteRef = new HashSet();
    private HashSet referenceRef = new HashSet();
    private HashSet bookmarkRef = new HashSet();
    private HashSet sequenceRef = new HashSet();
	
    // Reference marks and bookmarks contained in headings
    private HashSet referenceHeading = new HashSet();
    private HashSet bookmarkHeading = new HashSet();
	
    // All internal hyperlinks
    private HashSet links = new HashSet();
	
    // Forms
    private FormsReader forms = new FormsReader();
	
    // The main content element
    private Element content = null;
	
    // Identify OASIS OpenDocument format
    private boolean bOpenDocument = false;

    // Identify individual genres
    private boolean bText = false;
    private boolean bSpreadsheet = false;
    private boolean bPresentation = false;	
	
    ///////////////////////////////////////////////////////////////////////////
    // Various methods
	
    /** Checks whether or not this document is in package format
     *  @return true if it's in package format
     */
    public boolean isPackageFormat() { return oooDoc.isPackageFormat(); }
	
    /** Checks whether this url is internal to the package
     *  @param sUrl the url to check
     *  @return true if the url is internal to the package
     */
    public boolean isInPackage(String sUrl) {
        if (!bOpenDocument && sUrl.startsWith("#")) { return true; } // old format
        if (sUrl.startsWith("./")) { sUrl=sUrl.substring(2); }
        return oooDoc.getEmbeddedObject(sUrl)!=null; 
    } 
	
    ///////////////////////////////////////////////////////////////////////////
    // Accessor methods

    /** <p>Get the collection of all font declarations.</p>
     *  @return the <code>OfficeStyleFamily</code> of font declarations   
     */
    public OfficeStyleFamily getFontDeclarations() { return font; }
	
    /** <p>Get a specific font declaration</p>
     * @param sName the name of the font declaration
     * @return a <code>FontDeclaration</code> representing the font
     */
    public FontDeclaration getFontDeclaration(String sName) {
        return (FontDeclaration) font.getStyle(sName);
    }

    // Accessor methods for styles
    public OfficeStyleFamily getTextStyles() { return text; }
    public StyleWithProperties getTextStyle(String sName) {
        return (StyleWithProperties) text.getStyle(sName);
    }

    public OfficeStyleFamily getParStyles() { return par; }
    public StyleWithProperties getParStyle(String sName) {
        return (StyleWithProperties) par.getStyle(sName);
    }
    public StyleWithProperties getDefaultParStyle() {
        return (StyleWithProperties) par.getDefaultStyle();
    }

    public OfficeStyleFamily getSectionStyles() { return section; }
    public StyleWithProperties getSectionStyle(String sName) {
        return (StyleWithProperties) section.getStyle(sName);
    }

    public OfficeStyleFamily getTableStyles() { return table; }
    public StyleWithProperties getTableStyle(String sName) {
        return (StyleWithProperties) table.getStyle(sName);
    }
    public OfficeStyleFamily getColumnStyles() { return column; }
    public StyleWithProperties getColumnStyle(String sName) {
        return (StyleWithProperties) column.getStyle(sName);
    }

    public OfficeStyleFamily getRowStyles() { return row; }
    public StyleWithProperties getRowStyle(String sName) {
        return (StyleWithProperties) row.getStyle(sName);
    }

    public OfficeStyleFamily getCellStyles() { return cell; }
    public StyleWithProperties getCellStyle(String sName) {
        return (StyleWithProperties) cell.getStyle(sName);
    }
    public StyleWithProperties getDefaultCellStyle() {
        return (StyleWithProperties) cell.getDefaultStyle();
    }

    public OfficeStyleFamily getFrameStyles() { return frame; }
    public StyleWithProperties getFrameStyle(String sName) {
        return (StyleWithProperties) frame.getStyle(sName);
    }
    public StyleWithProperties getDefaultFrameStyle() {
        return (StyleWithProperties) frame.getDefaultStyle();
    }

    public OfficeStyleFamily getPresentationStyles() { return presentation; }
    public StyleWithProperties getPresentationStyle(String sName) {
        return (StyleWithProperties) presentation.getStyle(sName);
    }
    public StyleWithProperties getDefaultPresentationStyle() {
        return (StyleWithProperties) presentation.getDefaultStyle();
    }

    public OfficeStyleFamily getDrawingPageStyles() { return drawingPage; }
    public StyleWithProperties getDrawingPageStyle(String sName) {
        return (StyleWithProperties) drawingPage.getStyle(sName);
    }
    public StyleWithProperties getDefaultDrawingPageStyle() {
        return (StyleWithProperties) drawingPage.getDefaultStyle();
    }

    public OfficeStyleFamily getListStyles() { return list; }
    public ListStyle getListStyle(String sName) {
        return (ListStyle) list.getStyle(sName);
    }
	
    public OfficeStyleFamily getPageLayouts() { return pageLayout; }
    public PageLayout getPageLayout(String sName) {
        return (PageLayout) pageLayout.getStyle(sName);
    }
	
    public OfficeStyleFamily getMasterPages() { return masterPage; }
    public MasterPage getMasterPage(String sName) {
        return (MasterPage) masterPage.getStyle(sName);
    }
	
    public ListStyle getOutlineStyle() { return outline; }
	
    public PropertySet getFootnotesConfiguration() { return footnotes; }
	
    public PropertySet getEndnotesConfiguration() { return endnotes; }
	
    /** <p>Returns the paragraph style associated with headings of a specific
     *  level. Returns <code>null</code> if no such style is known.
     *  <p>In principle, different styles can be used for each heading, in
     *  practice the same (soft) style is used for all headings of a specific
     *  level.
     *  @param nLevel the level of the heading
     *  @return a <code>StyleWithProperties</code> object representing the style
     */
    public StyleWithProperties getHeadingStyle(int nLevel) {
        return 1<=nLevel && nLevel<=10 ? heading[nLevel] : null;
    }
	
    /** <p>Returns the first master page used in the document. If no master
     *  page is used explicitly, the first master page found in the styles is
     *  returned. Returns null if no master pages exists.
     *  @return a <code>MasterPage</code> object representing the master page
     */
    public MasterPage getFirstMasterPage() { return firstMasterPage; }
	
    /** Return the iso language used in most paragaph styles (in a well-structured
     * document this will be the default language)
     * TODO: Base on content rather than style 
     * @return the iso language
     */ 
    public String getMajorityLanguage() {
        Hashtable langs = new Hashtable();

        // Read the default language from the default paragraph style
        String sDefaultLang = null;
        StyleWithProperties style = getDefaultParStyle();
        if (style!=null) { 
            sDefaultLang = style.getProperty(XMLString.FO_LANGUAGE);
        }

        // Collect languages from paragraph styles
        Enumeration enumeration = getParStyles().getStylesEnumeration();
        while (enumeration.hasMoreElements()) {
            style = (StyleWithProperties) enumeration.nextElement();
            String sLang = style.getProperty(XMLString.FO_LANGUAGE);
            if (sLang==null) { sLang = sDefaultLang; }
            if (sLang!=null) {
                int nCount = 1;
                if (langs.containsKey(sLang)) {
                    nCount = ((Integer) langs.get(sLang)).intValue()+1;
                }
                langs.put(sLang,new Integer(nCount));
            }
        }
		
        // Find the most common language
        int nMaxCount = 0;
        String sMajorityLanguage = null;
        enumeration = langs.keys();
        while (enumeration.hasMoreElements()) {
            String sLang = (String) enumeration.nextElement();
            int nCount = ((Integer) langs.get(sLang)).intValue();
            if (nCount>nMaxCount) {
                nMaxCount = nCount;
                sMajorityLanguage = sLang;
            }
        }
        return sMajorityLanguage;        
    }

	
    /** <p>Returns a reader for a specific toc
     *  @param onode the <code>text:table-of-content-node</code>
     *  @return the reader, or null
     */
    public TocReader getTocReader(Element onode) {
        if (indexes.containsKey(onode)) { return (TocReader) indexes.get(onode); }
        else { return null; }
    } 
	
    /** <p>Is this style used in some toc as an index source style?</p>
     *  @param sStyleName the name of the style
     *  @return true if this is an index source style
     */
    public boolean isIndexSourceStyle(String sStyleName) {
        return indexSourceStyles.contains(sStyleName);
    }
	
    /** <p>Does this sequence name belong to a lof?</p>
     *  @param sName the name of the sequence
     *  @return true if it belongs to an index
     */
    public boolean isFigureSequenceName(String sName) {
        return figureSequenceNames.contains(sName);
    }
	
    /** <p>Does this sequence name belong to a lot?</p>
     *  @param sName the name of the sequence
     *  @return true if it belongs to an index
     */
    public boolean isTableSequenceName(String sName) {
        return tableSequenceNames.contains(sName);
    }
	
    /** <p>Add a sequence name for table captions.</p>
     *  <p>OpenDocument has a very weak notion of table captions: A caption is a
     *  paragraph containing a text:sequence element. Moreover, the only source
     *  to identify which sequence number to use is the list(s) of tables.
     *  If there's no list of tables, captions cannot be identified.
     *  Thus this method lets the user add a sequence name to identify the
     *  table captions.
     *  @param sName the name to add
     */
    public void addTableSequenceName(String sName) {
        tableSequenceNames.add(sName);
    }
	
    /** <p>Add a sequence name for figure captions.</p>
     *  <p>OpenDocument has a very weak notion of figure captions: A caption is a
     *  paragraph containing a text:sequence element. Moreover, the only source
     *  to identify which sequence number to use is the list(s) of figures.
     *  If there's no list of figures, captions cannot be identified.
     *  Thus this method lets the user add a sequence name to identify the
     *  figure captions.
     *  @param sName the name to add
     */
    public void addFigureSequenceName(String sName) {
        figureSequenceNames.add(sName);
    }
    /** <p>Get the sequence name associated with a paragraph</p>
     *  @param par the paragraph to look up
     *  @return the sequence name or null
     */
    public String getSequenceName(Element par) {
        return sequenceNames.containsKey(par) ? (String) sequenceNames.get(par) : null;
    }
	
    /** <p>Get the sequence name associated with a reference name</p>
     *  @param sRefName the reference name to use
     *  @return the sequence name or null
     */
    public String getSequenceFromRef(String sRefName) {
        return (String) seqrefNames.get(sRefName);
    }
	
	
    /** <p>Is there a reference to this footnote id?
     *  @param sId the id of the footnote
     *  @return true if there is a reference
     */
    public boolean hasFootnoteRefTo(String sId) {
        return footnoteRef.contains(sId);
    }

    /** <p>Is there a reference to this endnote?
     *  @param sId the id of the endnote
     *  @return true if there is a reference
     */
    public boolean hasEndnoteRefTo(String sId) {
        return endnoteRef.contains(sId);
    }

    /** Is this reference mark contained in a heading?
     *  @param sName the name of the reference mark 
     *  @return true if so
     */
    public boolean referenceMarkInHeading(String sName) {
        return referenceHeading.contains(sName);
    }

    /** Is there a reference to this reference mark?
     *  @param sName the name of the reference mark 
     *  @return true if there is a reference
     */
    public boolean hasReferenceRefTo(String sName) {
        return referenceRef.contains(sName);
    }

    /** Is this bookmark contained in a heading?
     *  @param sName the name of the bookmark 
     *  @return true if so
     */
    public boolean bookmarkInHeading(String sName) {
        return bookmarkHeading.contains(sName);
    }

    /** <p>Is there a reference to this bookmark?
     *  @param sName the name of the bookmark
     *  @return true if there is a reference
     */
    public boolean hasBookmarkRefTo(String sName) {
        return bookmarkRef.contains(sName);
    }

    /** <p>Is there a reference to this sequence field?
     *  @param sId the id of the sequence field
     *  @return true if there is a reference
     */
    public boolean hasSequenceRefTo(String sId) {
        return sequenceRef.contains(sId);
    }

    /** <p>Is there a link to this sequence anchor name?
     *  @param sName the name of the anchor
     *  @return true if there is a link
     */
    public boolean hasLinkTo(String sName) {
        return links.contains(sName);
    }
	
    /** <p>Is this an OASIS OpenDocument or an OOo 1.0 document?
     *  @return true if it's an OASIS OpenDocument
     */
    public boolean isOpenDocument() { return bOpenDocument; }
	
    /** <p>Is this an text document?
     *  @return true if it's a text document
     */
    public boolean isText() { return bText; }

    /** <p>Is this a spreadsheet document?
     *  @return true if it's a spreadsheet document
     */
    public boolean isSpreadsheet() { return bSpreadsheet; }

    /** <p>Is this a presentation document?
     *  @return true if it's a presentation document
     */
    public boolean isPresentation() { return bPresentation; }

    /** <p>Get the content element</p>
     *  <p>In the old file format this means the <code>office:body</code> element
     *  <p>In the OpenDocument format this means a <code>office:text</code>,
     *     <code>office:spreadsheet</code> or <code>office:presentation</code>
     *     element.</p>
     *  @return the content <code>Element</code>
     */
    public Element getContent() {
        return content;
    }
	
    /** <p>Get the forms belonging to this document.</p>
     *  @return a <code>FormsReader</code> representing the forms
     */
    public FormsReader getForms() { return forms; }
	
    /** <p>Read a table from a table:table node</p>
     *  @param node the table:table Element node
     *  @return a <code>TableReader</code> object representing the table
     */
    public TableReader getTableReader(Element node) {
        return new TableReader(this,node);
    }

    /** Constructor; read a document */
    public OfficeReader(OfficeDocument oooDoc, boolean bAllParagraphsAreSoft) {
        this.oooDoc = oooDoc;
        loadStylesFromDOM(oooDoc.getStyleDOM(),oooDoc.getContentDOM(),bAllParagraphsAreSoft);
        loadContentFromDOM(oooDoc.getContentDOM());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers
	
    /*private void collectMasterPage(StyleWithProperties style) {
        if (style==null || firstMasterPage!=null) { return; }
        String s = style.getMasterPageName();
        if (s!=null && s.length()>0) {
            firstMasterPage = getMasterPage(s);
        }
    }*/

    private void loadStylesFromDOM(Node node, boolean bAllParagraphsAreSoft) {
        // node should be office:master-styles, office:styles or office:automatic-styles
        boolean bAutomatic = XMLString.OFFICE_AUTOMATIC_STYLES.equals(node.getNodeName());
        if (node.hasChildNodes()){
            NodeList nl = node.getChildNodes();
            int nLen = nl.getLength();
            for (int i = 0; i < nLen; i++ ) {                                   
                Node child=nl.item(i);
                if (child.getNodeType()==Node.ELEMENT_NODE){
                    if (child.getNodeName().equals(XMLString.STYLE_STYLE)){
                        String sFamily = Misc.getAttribute(child,XMLString.STYLE_FAMILY);
                        if ("text".equals(sFamily)){ 
                            text.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("paragraph".equals(sFamily)){ 
                            par.loadStyleFromDOM(child,bAutomatic && !bAllParagraphsAreSoft); 
                        }
                        else if ("section".equals(sFamily)){ 
                            section.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table".equals(sFamily)){ 
                            table.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table-column".equals(sFamily)){
                            column.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table-row".equals(sFamily)){
                            row.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table-cell".equals(sFamily)){
                            cell.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("graphics".equals(sFamily)){
                            frame.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("graphic".equals(sFamily)){ // oasis
                            frame.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("presentation".equals(sFamily)){
                            presentation.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("drawing-page".equals(sFamily)){
                            // Bug in OOo 1.x: The same name may be used for a real and an automatic style...
                            if (drawingPage.getStyle(Misc.getAttribute(child,XMLString.STYLE_NAME))==null) {
                                drawingPage.loadStyleFromDOM(child,bAutomatic);
                            }
                        }
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_PAGE_MASTER)) { // old
                        pageLayout.loadStyleFromDOM(child,bAutomatic);
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_PAGE_LAYOUT)) { // oasis
                        pageLayout.loadStyleFromDOM(child,bAutomatic);
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_MASTER_PAGE)) {
                        masterPage.loadStyleFromDOM(child,bAutomatic);
                        if (firstMasterPage==null) {
                            firstMasterPage = (MasterPage) masterPage.getStyle(Misc.getAttribute(child,XMLString.STYLE_NAME));
                        }
                    }
                    else if (child.getNodeName().equals(XMLString.TEXT_LIST_STYLE)) {
                        list.loadStyleFromDOM(child,bAutomatic);
                    }
                    else if (child.getNodeName().equals(XMLString.TEXT_OUTLINE_STYLE)) {
                        outline.loadStyleFromDOM(child);
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_DEFAULT_STYLE)){
                        String sFamily = Misc.getAttribute(child,XMLString.STYLE_FAMILY);
                        if ("paragraph".equals(sFamily)) {
                            StyleWithProperties defaultPar = new StyleWithProperties();
                            defaultPar.loadStyleFromDOM(child);
                            par.setDefaultStyle(defaultPar);
                        }
                        else if ("graphics".equals(sFamily) || "graphic".equals(sFamily)) { // oasis: no s
                            StyleWithProperties defaultFrame = new StyleWithProperties();
                            defaultFrame.loadStyleFromDOM(child);
                            frame.setDefaultStyle(defaultFrame);
                        }
                        else if ("table-cell".equals(sFamily)) {
                            StyleWithProperties defaultCell = new StyleWithProperties();
                            defaultCell.loadStyleFromDOM(child);
                            cell.setDefaultStyle(defaultCell);
                        }
                    }
                }
            }
        }                                            
    }
	
    private void loadStylesFromDOM(Document stylesDOM, Document contentDOM, boolean bAllParagraphsAreSoft){
        // Flat xml: stylesDOM will be null and contentDOM contain everything
        // This is only the case for old versions of xmerge; newer versions
        // creates DOM for styles, content, meta and settings.
        NodeList list;

        // font declarations: Try old format first
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.OFFICE_FONT_DECLS);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_FONT_DECLS);
        }
        // If that fails, try oasis format
        if (list.getLength()==0) {
            if (stylesDOM==null) {
                list = contentDOM.getElementsByTagName(XMLString.OFFICE_FONT_FACE_DECLS);
            }
            else {
                list = stylesDOM.getElementsByTagName(XMLString.OFFICE_FONT_FACE_DECLS);
            }
        }
		
        if (list.getLength()!=0) {
            Node node = list.item(0);
            if (node.hasChildNodes()){
                NodeList nl = node.getChildNodes();
                int nLen = nl.getLength();
                for (int i = 0; i < nLen; i++ ) {                                   
                    Node child = nl.item(i);
                    if (child.getNodeType()==Node.ELEMENT_NODE){
                        if (child.getNodeName().equals(XMLString.STYLE_FONT_DECL)){
                            font.loadStyleFromDOM(child,false);
                        }
                        else if (child.getNodeName().equals(XMLString.STYLE_FONT_FACE)){
                            font.loadStyleFromDOM(child,false);
                        }
                    }
                } 
            }
        }

        // soft formatting:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.OFFICE_STYLES);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_STYLES);
        }
        if (list.getLength()!=0) {
            loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
        }
		
        // master styles:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.OFFICE_MASTER_STYLES);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_MASTER_STYLES);
        }
        if (list.getLength()!=0) {
            loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
        }
    
        // hard formatting:
        // Load from styles.xml first. Problem: There may be name clashes
        // with automatic styles from content.xml
        if (stylesDOM!=null) {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_AUTOMATIC_STYLES);
            if (list.getLength()!=0) {
                loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
            }
        }	
        list = contentDOM.getElementsByTagName(XMLString.OFFICE_AUTOMATIC_STYLES);
        if (list.getLength()!=0) {
            loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
        }

        // footnotes configuration:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.TEXT_FOOTNOTES_CONFIGURATION);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.TEXT_FOOTNOTES_CONFIGURATION);
        }
        if (list.getLength()!=0) {
            footnotes = new PropertySet();
            footnotes.loadFromDOM(list.item(0));
        }
		
        // endnotes configuration:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.TEXT_ENDNOTES_CONFIGURATION);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.TEXT_ENDNOTES_CONFIGURATION);
        }
        if (list.getLength()!=0) {
            endnotes = new PropertySet();
            endnotes.loadFromDOM(list.item(0));
        }
		
        // if it failed, try oasis format
        if (footnotes==null || endnotes==null) {
            if (stylesDOM==null) {
                list = contentDOM.getElementsByTagName(XMLString.TEXT_NOTES_CONFIGURATION);
            }
            else {
                list = stylesDOM.getElementsByTagName(XMLString.TEXT_NOTES_CONFIGURATION);
            }
            int nLen = list.getLength();
            for (int i=0; i<nLen; i++) {
                String sClass = Misc.getAttribute(list.item(i),XMLString.TEXT_NOTE_CLASS);
                if ("endnote".equals(sClass)) {
                    endnotes = new PropertySet();
                    endnotes.loadFromDOM(list.item(i));
                }
                else {
                    footnotes = new PropertySet();
                    footnotes.loadFromDOM(list.item(i));
                }
            }
       }
		

    }
	
    private void loadContentFromDOM(Document contentDOM) {
     // Get the office:body element
        NodeList list = contentDOM.getElementsByTagName(XMLString.OFFICE_BODY);
        if (list.getLength()>0) {
            // There may be several bodies, but the first one is the main body
            Element body = (Element) list.item(0);

            // Now get the content and identify the type of document
            content = Misc.getChildByTagName(body,XMLString.OFFICE_TEXT);
            if (content!=null) { // OpenDocument Text
                bOpenDocument = true; bText = true;
            }
            else {
                content = Misc.getChildByTagName(body,XMLString.OFFICE_SPREADSHEET);
                if (content!=null) { // OpenDocument Spreadsheet
                    bOpenDocument = true; bSpreadsheet = true;
                }
                else {
                    content = Misc.getChildByTagName(body,XMLString.OFFICE_PRESENTATION);
                    if (content!=null) { // OpenDocument Presentation
                        bOpenDocument = true; bPresentation = true;
                    }
                    else {
                        content = body;
                        // OOo 1.x file format - look through content to determine genre
                        bSpreadsheet = true;
                        bPresentation = false;
                        Node child = body.getFirstChild();
                        while (child!=null) {
                            if (child.getNodeType()==Node.ELEMENT_NODE) {
                                String sName = child.getNodeName();
                                if (XMLString.TEXT_P.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_H.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_ORDERED_LIST.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_ORDERED_LIST.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_SECTION.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.DRAW_PAGE.equals(sName)) {
                                    bPresentation = true; bSpreadsheet = false;
                                }
                                else if (XMLString.DRAW_PAGE.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                            }
                            child = child.getNextSibling();
                        }
                        bText = !bSpreadsheet && !bPresentation;
                    }
                }
            }                

            traverseContent(body);

            if (sAutoFigureSequenceName!=null) {
                addFigureSequenceName(sAutoFigureSequenceName);
            }
            if (sAutoTableSequenceName!=null) {
                addTableSequenceName(sAutoTableSequenceName);
            }
        }

        /*if (firstMasterPage==null) {
            firstMasterPage = getMasterPage(sFirstMasterPageName);
        }*/
    }
	
    private Element getParagraph(Element node) {
        Element parent = (Element) node.getParentNode();
        if (parent.getTagName().equals(XMLString.TEXT_P) || parent.getTagName().equals(XMLString.TEXT_H)) {
            return parent;
        } 
        return getParagraph(parent);
    }
	
    private void traverseContent(Element node) {
        // Handle this node first
        String sName = node.getTagName();
        if (sName.equals(XMLString.TEXT_P)) {
            //collectMasterPage(getParStyle(node.getAttribute(XMLString.TEXT_STYLE_NAME)));
        }
        else if (sName.equals(XMLString.TEXT_H)) {
            int nLevel = Misc.getPosInteger(node.getAttribute(XMLString.TEXT_LEVEL),1);
            StyleWithProperties style = getParStyle(node.getAttribute(XMLString.TEXT_STYLE_NAME));
            //collectMasterPage(style);
            if (1<=nLevel && nLevel<=10 && heading[nLevel]==null) {
                if (style!=null && style.isAutomatic()) {
                    heading[nLevel] = getParStyle(style.getParentName());
                }
                else {
                    heading[nLevel] = null;
                }
            }
        }
        else if (sName.equals(XMLString.TEXT_SEQUENCE)) {
            String sSeqName = Misc.getAttribute(node,XMLString.TEXT_NAME);
            String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
            if (sSeqName!=null) {
                Element par = getParagraph(node);
                if (!sequenceNames.containsKey(par)) {
                    // Only the first text:seqence should be registered as possible caption sequence
                    sequenceNames.put(par,sSeqName);
                }
                if (sRefName!=null) {
                    seqrefNames.put(sRefName,sSeqName);
                }
            }
        }
        else if (sName.equals(XMLString.TEXT_FOOTNOTE_REF)) {
            collectRefName(footnoteRef,node);
        }
        else if (sName.equals(XMLString.TEXT_ENDNOTE_REF)) {
            collectRefName(endnoteRef,node);
        }
        else if (sName.equals(XMLString.TEXT_NOTE_REF)) { // oasis
            String sClass = Misc.getAttribute(node,XMLString.TEXT_NOTE_CLASS);
            if ("footnote".equals(sClass)) { collectRefName(footnoteRef,node); }
            else if ("endnote".equals(sClass)) { collectRefName(endnoteRef,node); }
        }
        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK)) {
            collectMarkInHeading(referenceHeading,node);
        }
        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK_START)) {
            collectMarkInHeading(referenceHeading,node);
        }
        else if (sName.equals(XMLString.TEXT_REFERENCE_REF)) {
            collectRefName(referenceRef,node);
        }
        else if (sName.equals(XMLString.TEXT_BOOKMARK)) {
            collectMarkInHeading(bookmarkHeading,node);
        }
        else if (sName.equals(XMLString.TEXT_BOOKMARK_START)) {
            collectMarkInHeading(bookmarkHeading,node);
        }
        else if (sName.equals(XMLString.TEXT_BOOKMARK_REF)) {
            collectRefName(bookmarkRef,node);
        }
        else if (sName.equals(XMLString.TEXT_SEQUENCE_REF)) {
            collectRefName(sequenceRef,node);
        }
        else if (sName.equals(XMLString.TEXT_A)) {
            String sHref = node.getAttribute(XMLString.XLINK_HREF);
            if (sHref!=null && sHref.startsWith("#")) {
                links.add(sHref.substring(1));
            }
        }
        else if (sName.equals(XMLString.OFFICE_FORMS)) {
            forms.read(node);
        }
        else if (sName.equals(XMLString.TEXT_TABLE_OF_CONTENT)) {
            TocReader tocReader = new TocReader(node);
            indexes.put(node,tocReader);
            indexSourceStyles.addAll(tocReader.getIndexSourceStyles());
        }
        else if (sName.equals(XMLString.TEXT_TABLE_INDEX) ||
                 sName.equals(XMLString.TEXT_ILLUSTRATION_INDEX)) {
            LoftReader loftReader = new LoftReader(node);
            indexes.put(node,loftReader);
            if (loftReader.useCaption()) {
                if (loftReader.isTableIndex()) {
                  tableSequenceNames.add(loftReader.getCaptionSequenceName());
                }
                else {
                  figureSequenceNames.add(loftReader.getCaptionSequenceName());
                }
            }
        }
        // todo: other indexes
		
        // Traverse the children
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                traverseContent((Element) child);
            }
            child = child.getNextSibling();
        }

        // Collect automatic captions sequences
        // Use OOo defaults: Captions have style names Illustration and Table resp.
        if ((sAutoFigureSequenceName==null || sAutoTableSequenceName==null) && sName.equals(XMLString.TEXT_P)) {
            String sStyleName = getParStyles().getDisplayName(node.getAttribute(XMLString.TEXT_STYLE_NAME)); 
            if (sAutoFigureSequenceName==null) {
                if ("Illustration".equals(sStyleName)) {
                    sAutoFigureSequenceName = getSequenceName(node);
                }
            }
            if (sAutoTableSequenceName==null) {
                if ("Table".equals(sStyleName)) {
                    sAutoTableSequenceName = getSequenceName(node);
                }
            }
        }
       
    }
	
    private void collectRefName(HashSet ref, Element node) {
        String sRefName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (sRefName!=null && sRefName.length()>0) {
            ref.add(sRefName);
        }
    }
	
    private void collectMarkInHeading(HashSet marks, Element node) {
        String sName = node.getAttribute(XMLString.TEXT_NAME);
        if (sName!=null && sName.length()>0) {
            Element par = getParagraph(node);
            if (XMLString.TEXT_H.equals(par.getTagName())) {
                marks.add(sName);
            }
        }
    }
	
}