/************************************************************************
 *
 *  TocReader.java
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

package writer2latex.office;

import java.util.Hashtable;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.util.Misc;

/**
 *  <p>The class reads a <code>text:table-of-content</code> element.</p>
 */
public class TocReader {

    Element tocSource = null;
    Element indexBody = null;
	 
    String sName=null;                       // (section) name for this toc
    String sStyleName=null;                  // section style name

    int nOutlineLevel = 10;              // max level to include
    boolean bUseOutlineLevel = true;         // use headings
    boolean bUseIndexSourceStyles = false;   // use additional styles
    boolean bUseIndexMarks = true;           // use toc marks
    boolean bIsByChapter = false;            // default is document
	
    Element indexTitleTemplate = null;
    Element[] tocEntryTemplate = new Element[11];
    Hashtable indexSourceStyles = new Hashtable();
    


    /** <p>Initialize the TocReader with a table of content node.
     *  @param onode a <code>text:table-of-content</code>
     */
    public TocReader(Element onode) {
        sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);

        Element tocSource = Misc.getChildByTagName(onode,XMLString.TEXT_TABLE_OF_CONTENT_SOURCE);
        //Element indexBody = Misc.getChildByTagName(onode,XMLString.TEXT_INDEX_BODY);

        if (tocSource!=null) {
            nOutlineLevel = Misc.getPosInteger(tocSource.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
            bUseOutlineLevel = !"false".equals(tocSource.getAttribute(XMLString.TEXT_USE_OUTLINE_LEVEL));
            bUseIndexSourceStyles = "true".equals(tocSource.getAttribute(XMLString.TEXT_USE_INDEX_SOURCE_STYLES));
            bUseIndexMarks = !"false".equals(tocSource.getAttribute(XMLString.TEXT_USE_INDEX_MARKS));
            bIsByChapter = "chapter".equals(tocSource.getAttribute(XMLString.TEXT_INDEX_SCOPE));
            
            // traverse the source to collect templates
            Node child = tocSource.getFirstChild();
            while (child!=null) {
                if (child.getNodeType()==Node.ELEMENT_NODE) {
                    Element elm = (Element) child;
                    if (XMLString.TEXT_INDEX_TITLE_TEMPLATE.equals(elm.getTagName())) {
                        indexTitleTemplate = elm;
                    }
                    if (XMLString.TEXT_TABLE_OF_CONTENT_ENTRY_TEMPLATE.equals(elm.getTagName())) {
                        int nLevel = Misc.getPosInteger(elm.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
                        if (1<=nLevel && nLevel<=10) { tocEntryTemplate[nLevel] = elm; }
                    }
                    if (XMLString.TEXT_INDEX_SOURCE_STYLES.equals(elm.getTagName())) {
                        int nLevel = Misc.getPosInteger(elm.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
                        if (1<=nLevel && nLevel<=10) {					
                            // traverse to collect index source styles for this level
                            Node child1 = elm.getFirstChild();
                            while (child1!=null) {
                                if (child1.getNodeType()==Node.ELEMENT_NODE) {
                                    Element elm1 = (Element) child1;
                                    if (XMLString.TEXT_INDEX_SOURCE_STYLE.equals(elm1.getTagName())) {
                                        String sIndexSourceStyle = Misc.getAttribute(elm1,XMLString.TEXT_STYLE_NAME);
                                        if (sIndexSourceStyle!=null) {
                                            indexSourceStyles.put(sIndexSourceStyle,new Integer(nLevel));
                                        } 
                                    }
                                }
                                child1 = child1.getNextSibling();
                            }
                        }
                    }
                }
                child = child.getNextSibling();
            }
        }
    }
	
    /** <p>Get the (section) name for this toc </p>
     *  @return the name of the toc
     */
    public String getName() { return sName; }
	
    /** <p>Get the (section) style name for this toc </p>
     *  @return name of the section style to use for this toc
     */
    public String getStyleName() { return sStyleName; }
	
    /** <p>Get max outline level for this toc </p>
     *  @return max outline level
     */
    public int getOutlineLevel() { return nOutlineLevel; }
	
    /** <p>Do we use outline (headings) in this toc? </p>
     *  @return true if headings should be used
     */
    public boolean useOutlineLevel() { return bUseOutlineLevel; }
	
    /** <p>Do we use additional styles in this toc? </p>
     *  @return true if additional styles should be used
     */
    public boolean useIndexSourceStyles() { return bUseIndexSourceStyles; }
	
    /** <p>Do we use toc marks in this toc? </p>
     *  @return true if toc marks should be used
     */
    public boolean useIndexMarks() { return bUseIndexMarks; }
	
    /** <p>Is this toc by chapter? </p>
     *  @return true if the scope is a chapter only
     */
    public boolean isByChapter() { return bIsByChapter; }
	
    /** <p>Get the index title template for this toc</p>
     *  @return the <code>text:index-title-template</code> element, or null
     */
    public Element getIndexTitleTemplate() { return indexTitleTemplate; }
	
    /** <p>Get the entry template for this toc at a specific level</p>
     *  @param nLevel the outline level
     *  @return the <code>text:table-of-content-entry-template</code> element, or null
     */
    public Element getTocEntryTemplate(int nLevel) {
        if (1<=nLevel && nLevel<=10) { return tocEntryTemplate[nLevel]; }
        else { return null; }
    }
	
    /** <p>Get a set view of all index source styles</p>
     *  @return a set of all index source style names
     */
    public Set getIndexSourceStyles() { return indexSourceStyles.keySet(); }
	
    /** <p>Get the level associated with a specific index source style</p>
     *  @param sStyleName the style name of the index source style
     *  @return the level or -1 if the style is not used in this toc
     */
    public int getIndexSourceStyleLevel(String sStyleName) {
        if (indexSourceStyles.containsKey(sStyleName)) {
            return ((Integer) indexSourceStyles.get(sStyleName)).intValue();
        }
        else {
            return -1;
        }
    }

    /** <p>Return the generated content of this toc, if available</p>
     *  @return the <code>text:index-body</code> element
     */
    public Element getIndexBody() { return indexBody; }

	
}