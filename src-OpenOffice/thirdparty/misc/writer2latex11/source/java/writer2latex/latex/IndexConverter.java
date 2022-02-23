/************************************************************************
 *
 *  IndexConverter.java
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

import java.util.Vector;

import org.w3c.dom.Element;
//import org.w3c.dom.Node;

import writer2latex.util.Misc;

import writer2latex.office.IndexMark;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;

import writer2latex.latex.util.Context;

/**
 *  <p>This class handles indexes (table of contents, list of tables, list of
 *  illustrations, object index, user index, alphabetical index)
 *  as well as their associated index marks.</p>
 */
public class IndexConverter extends ConverterHelper {

    private boolean bContainsAlphabeticalIndex = false;
	
    private Vector postponedIndexMarks = new Vector();
	
    /** <p>Construct a new <code>IndexConverter</code>.
     * @param config the configuration to use 
     * @param palette the <code>ConverterPalette</code> to link to
     * if such a document is created by the <code>IndexConverter</code>
     */
    public IndexConverter(OfficeReader ofr,LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
    }

    /** <p>Append declarations needed by the <code>IndexConverter</code> to
     * the preamble.
     * @param pack the <code>LaTeXDocumentPortion</code> to which
     * declarations of packages should be added (<code>\\usepackage</code>).
     * @param decl the <code>LaTeXDocumentPortion</code> to which
     * other declarations should be added.
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
       if (bContainsAlphabeticalIndex) {
            pack.append("\\usepackage{makeidx}").nl();
            decl.append("\\makeindex").nl();
        }
    }
	
    /** Process Table of Contents (text:table-of-content tag)
     * @param node The element containing the Table of Contents
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleTOC (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.noIndex()) { return; }
        /* TODO: Apply more formatting by modfification of \l@section etc.
        Something like this:
        \newcommand\l@section[2]{\@dottedtocline{1}{1.5em}{2.3em}{\textbf{#1}}{\textit{#2}}
        Textformatting is trivial; see article.cls for examples of more complicated
        formatting. Note: The section number can't be formatted indivdually.*/

        Element source = Misc.getChildByTagName(node,XMLString.TEXT_TABLE_OF_CONTENT_SOURCE);
        if (source!=null) {
            if ("chapter".equals(source.getAttribute(XMLString.TEXT_INDEX_SOURCE))) {
                 ldp.append("[Warning: Table of content (for this chapter) ignored!]").nl().nl();
            }
            else {
                int nLevel = Misc.getPosInteger(source.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
                ldp.append("\\setcounter{tocdepth}{"+nLevel+"}").nl();
                Element title = Misc.getChildByTagName(source,XMLString.TEXT_INDEX_TITLE_TEMPLATE);
                if (title!=null) {
                    ldp.append("\\renewcommand\\contentsname{");
                    palette.getInlineCv().traversePCDATA(title,ldp,oc);
                    ldp.append("}").nl();
                }
            }
        }
        ldp.append("\\tableofcontents").nl();
    }

    /** Process List of Illustrations (text:list-of-illustrations tag)
     * @param node The element containing the List of Illustrations
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleLOF (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.noIndex()) { return; }
        ldp.append("\\listoffigures").nl();
    }

    /** Process List of Tables (text:list-of-tables tag)
     * @param node The element containing the List of Tables
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleLOT (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.noIndex()) { return; }
        ldp.append("\\listoftables").nl();
    }

    /** Process Object Index (text:object index tag)
     * @param node The element containing the Object Index
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleObjectIndex (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.noIndex()) { return; }
        ldp.append("[Warning: Object index ignored]").nl().nl();
    }

    /** Process User Index (text:user-index tag)
     * @param node The element containing the User Index
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleUserIndex (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.noIndex()) { return; }
        ldp.append("[Warning: User index ignored]").nl().nl();
    }


    /** Process Alphabetical Index (text:alphabetical-index tag)
     * @param node The element containing the Alphabetical Index
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleAlphabeticalIndex (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.noIndex()) { return; }
        ldp.append("\\printindex").nl();
        bContainsAlphabeticalIndex = true;
    }


    /** Process an Alphabetical Index Mark (text:alphabetical-index-mark{-start} tag)
     * @param node The element containing the Mark
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleAlphabeticalIndexMark(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            String sValue = IndexMark.getIndexValue(node);
            if (sValue!=null) {
                ldp.append("\\index{");
                String sKey1 = IndexMark.getKey1(node);
                if (sKey1!=null) {
                    writeIndexText(sKey1.trim(),ldp,oc);
                    ldp.append("!");
                }
                String sKey2 = IndexMark.getKey2(node);
                if (sKey2!=null) {
                    writeIndexText(sKey2.trim(),ldp,oc);
                    ldp.append("!");
                }
                writeIndexText(sValue.trim(),ldp,oc);
                ldp.append("}");
            }
        }
        else {
            // Index marks should not appear within \section or \caption
            postponedIndexMarks.add(node);
        }
    }
	
    public void flushIndexMarks(LaTeXDocumentPortion ldp, Context oc) {
        // We may still be in a context with no index marks
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            // Type out all postponed index marks
            int n = postponedIndexMarks.size();
            for (int i=0; i<n; i++) {
                handleAlphabeticalIndexMark((Element) postponedIndexMarks.get(i),ldp,oc);
            }
            postponedIndexMarks.clear();
        }
    }


    // Helper: Write the text of an index mark, escaping special characters
    private void writeIndexText(String sText, LaTeXDocumentPortion ldp, Context oc) {
        String sTextOut = palette.getI18n().convert(sText,false,oc.getLang());
        //  need to escape !, @, | and ":
        int nLen = sTextOut.length();
        boolean bBackslash = false;
        for (int i=0; i<nLen; i++) {
            if (bBackslash) {
                ldp.append(sTextOut.substring(i,i+1));
                bBackslash = false;
            }
            else {
                switch (sTextOut.charAt(i)) {
                    case '\\' : bBackslash = true;
                                ldp.append("\\");
                                break;
                    case '~' :
                    case '\u00A0' :
                        // Non-breaking space causes trouble in index:
                        ldp.append(" ");
                        break;
                    case '!' :
                    case '@' :
                    case '|' :
                    case '"' : ldp.append("\"");
                    default : ldp.append(sTextOut.substring(i,i+1)); 
                }
            }
        }
    }
    
}