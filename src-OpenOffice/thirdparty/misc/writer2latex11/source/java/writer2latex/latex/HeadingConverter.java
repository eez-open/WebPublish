/************************************************************************
 *
 *  HeadingConverter.java
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

//import java.util.Hashtable;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.HeadingMap;
//import writer2latex.latex.util.StyleMap;

/* This class converts OpenDocument headings (<code>text:h</code>) and
 * paragraph styles/formatting into LaTeX
 * Export of formatting depends on the option "formatting":
 * <ul>
 * <li><code>ignore_all</code>
 * <li><code>ignore_most</code>
 * <li><code>convert_basic</code>
 * <li><code>convert_most</code>
 * <li><code>convert_all</code>
 * </ul> 
 */
public class HeadingConverter extends ConverterHelper {
    private String[] sHeadingStyles = new String[11];

    /** Constructs a new <code>HeadingConverter</code>.
     */
    public HeadingConverter(OfficeReader ofr, LaTeXConfig config,
        ConverterPalette palette) {
        super(ofr,config,palette);
    }
	
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        appendHeadingStyles(decl);
    }
	
    /** Process a heading
     *  @param node The text:h element node containing the heading
     *  @param ldp The LaTeXDocumentPortion to add LaTeX code to
     *  @param oc The current context
     */
    public void handleHeading(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // Get the level, the heading map and the style name
        int nLevel = ofr.isOpenDocument() ?
            Misc.getPosInteger(Misc.getAttribute(node, XMLString.TEXT_OUTLINE_LEVEL),1) :
            Misc.getPosInteger(Misc.getAttribute(node, XMLString.TEXT_LEVEL),1);
        HeadingMap hm = config.getHeadingMap();
        String sStyleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);

        if (nLevel<=hm.getMaxLevel()) {
            // Always push the font used
            palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(ofr.getParStyle(sStyleName)));

            Context ic = (Context) oc.clone();
            ic.setInSection(true);
            // Footnotes with more than one paragraph are not allowed within
            // sections. To be safe, we disallow all footnotes
            ic.setNoFootnotes(true);

            // Apply style
            BeforeAfter baHardPage = new BeforeAfter();
            BeforeAfter baHardChar = new BeforeAfter();
            applyHardHeadingStyle(nLevel, sStyleName,
                baHardPage, baHardChar, ic);

            // Export the heading
            ldp.append(baHardPage.getBefore());
            ldp.append("\\"+hm.getName(nLevel));
            // If this heading contains formatting, add optional argument:
            if (baHardChar.getBefore().length()>0 || containsElements(node)) {
                ldp.append("[");
                palette.getInlineCv().traversePlainInlineText(node,ldp,ic);
                ldp.append("]");
            }
            ldp.append("{").append(baHardChar.getBefore());
            palette.getInlineCv().traverseInlineText(node,ldp,ic);
            ldp.append(baHardChar.getAfter()).append("}").nl();
            ldp.append(baHardPage.getAfter());
			
            // Include pending index marks, labels, footnotes & floating frames
            palette.getFieldCv().flushReferenceMarks(ldp,oc);
            palette.getIndexCv().flushIndexMarks(ldp,oc);
            palette.getNoteCv().flushFootnotes(ldp,oc);
            palette.getDrawCv().flushFloatingFrames(ldp,ic);

            // Pop the font name
            palette.getI18n().popSpecialTable();
        }
        else { // beyond supported headings - export as ordinary paragraph
            palette.getParCv().handleParagraph(node,ldp,oc,false);
        }
    }


    /** Use a paragraph style on a heading. If hard paragraph formatting
     *  is applied to a heading, page break and font is converted - other
     *  hard formatting is ignored.
     *  This method also collects name of heading style
     *  @param <code>nLevel</code> The level of this heading
     *  @param <code>sStyleName</code> the name of the paragraph style to use
     *  @param <code>baPage</code> a <code>BeforeAfter</code> to put page break code into
     *  @param <code>baText</code> a <code>BeforeAfter</code> to put character formatting code into
     *  @param <code>context</code> the current context. This method will use and update the formatting context  
     */
    private void applyHardHeadingStyle(int nLevel, String sStyleName,
        BeforeAfter baPage, BeforeAfter baText, Context context) {

        // Get the style
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style==null) { return; }

        // Register heading style
        if (sHeadingStyles[nLevel]==null) {
            sHeadingStyles[nLevel] = style.isAutomatic() ? style.getParentName() : sStyleName;
        }

        // Do conversion
        if (style.isAutomatic()) {
            palette.getPageSc().applyPageBreak(style,false,baPage);
            palette.getCharSc().applyHardCharFormatting(style,baText);
        }
		
        // Update context
        context.updateFormattingFromStyle(style);
    }


    /** Convert heading styles and outline numbering to LaTeX.
     *  An array of stylenames to use is required: The OOo writer file format
     *  allows different paragraph styles to be applied to individual headings,
     *  so this is not included in the styles.
     *  LaTeX (and OOo Writer!) usually uses the same format for all headings.
     *  @param ldp    the LaTeXDocumentPortion to add definitions to.
     */
    // TODO: use method from ListStyleConverter to create labels
    private void appendHeadingStyles(LaTeXDocumentPortion ldp) {
        // The user may not want to convert the formatting of headings
        if (config.formatting()<=LaTeXConfig.IGNORE_MOST) { return; }

        HeadingMap hm = config.getHeadingMap();

        // OK, we are going to convert. First find the max level for headings
        int nMaxLevel = 0;
        for (int i=1; i<=5; i++) { if (sHeadingStyles[i]!=null) { nMaxLevel=i; } }
        if (nMaxLevel==0) { return; } // no headings, nothing to do!
        if (nMaxLevel>hm.getMaxLevel()) { nMaxLevel = hm.getMaxLevel(); }

        boolean bOnlyNum = config.formatting()==LaTeXConfig.CONVERT_BASIC;
        if (bOnlyNum) {
            ldp.append("% Outline numbering").nl();
        }
        else {
            ldp.append("% Headings and outline numbering").nl()
               .append("\\makeatletter").nl();
        }

        // Paragraph style for headings:
        if (!bOnlyNum) {
            for (int i=1; i<=nMaxLevel; i++) {
                if (sHeadingStyles[i]!=null) {
                    StyleWithProperties style = ofr.getParStyle(sHeadingStyles[i]);
                    if (style!=null) {
                        BeforeAfter decl = new BeforeAfter();
                        BeforeAfter comm = new BeforeAfter();
					
                        palette.getPageSc().applyPageBreak(style,true,decl);

                        palette.getCharSc().applyNormalFont(decl);
                        palette.getCharSc().applyFont(style,true,true,decl,new Context());
                        palette.getParCv().applyAlignment(style,false,true,decl);
		    			
                        palette.getI18n().applyLanguage(style,false,true,comm);
                        palette.getCharSc().applyFontEffects(style,true,comm);
										
                        String sMarginTop = style.getAbsoluteLength(XMLString.FO_MARGIN_TOP);
                        String sMarginBottom = style.getAbsoluteLength(XMLString.FO_MARGIN_BOTTOM);
                        String sMarginLeft = style.getAbsoluteLength(XMLString.FO_MARGIN_LEFT);
    
                        String sSecName = hm.getName(i);
                        if (!comm.isEmpty()) { // have to create a cs for this heading
                            ldp.append("\\newcommand\\cs").append(sSecName).append("[1]{")
                               .append(comm.getBefore()).append("#1").append(comm.getAfter())
                               .append("}").nl();
                        }
                        ldp.append("\\renewcommand\\").append(sSecName)
                           .append("{\\@startsection{").append(sSecName).append("}{"+hm.getLevel(i))
                           .append("}{"+sMarginLeft+"}{");
                        // Suppress indentation after heading? currently not..
                        // ldp.append("-"); 
                        ldp.append(sMarginTop)
                           .append("}{").append(sMarginBottom).append("}{");
                        // Note: decl.getAfter() may include a page break after, which we ignore
	                    ldp.append(decl.getBefore());
                        if (!comm.isEmpty()) {
                            ldp.append("\\cs").append(sSecName);
                        }
                        ldp.append("}}").nl();
                    }
                }
            }
        }

        // redefine formatting of section counters
        // simplified if the user wants to ignore formatting
        if (!bOnlyNum) {
            ldp.append("\\renewcommand\\@seccntformat[1]{")
               .append("\\csname @textstyle#1\\endcsname{\\csname the#1\\endcsname}")
               .append("\\csname @distance#1\\endcsname}").nl();
        }

        // Collect numbering styles and set secnumdepth
        int nSecnumdepth = nMaxLevel;
        ListStyle outline = ofr.getOutlineStyle();
        String[] sNumFormat = new String[6];
        for (int i=nMaxLevel; i>=1; i--) {
            sNumFormat[i] = ListStyleConverter.numFormat(outline.getLevelProperty(i,
                               XMLString.STYLE_NUM_FORMAT));
            if (sNumFormat[i]==null || "".equals(sNumFormat[i])) {
                nSecnumdepth = i-1;
            }
        }
        ldp.append("\\setcounter{secnumdepth}{"+nSecnumdepth+"}").nl();

        for (int i=1; i<=nMaxLevel; i++) {
            if (sNumFormat[i]==null || "".equals(sNumFormat[i])) {
                // no numbering at this level
                if (!bOnlyNum) {
                    ldp.append("\\newcommand\\@distance")
                       .append(hm.getName(i)).append("{}").nl()
                       .append("\\newcommand\\@textstyle")
                       .append(hm.getName(i)).append("[1]{#1}").nl();
                }
            }
            else {
                if (!bOnlyNum) {
                    // Distance between label and text:
                    String sDistance = outline.getLevelStyleProperty(i,XMLString.TEXT_MIN_LABEL_DISTANCE);
                    ldp.append("\\newcommand\\@distance")
                       .append(hm.getName(i)).append("{");
                    if (sDistance!=null) { 
                        ldp.append("\\hspace{").append(sDistance).append("}");
                    }
                    ldp.append("}").nl();
                    // Label width and alignment
                    String sLabelWidth = outline.getLevelStyleProperty(i,XMLString.TEXT_MIN_LABEL_WIDTH);
                    String sTextAlign = outline.getLevelStyleProperty(i,XMLString.FO_TEXT_ALIGN);
                    String sAlignmentChar = "l"; // start (or left) is default
                    if (sTextAlign!=null) {
                        if ("end".equals(sTextAlign)) { sAlignmentChar="r"; }
                        else if ("right".equals(sTextAlign)) { sAlignmentChar="r"; }
                        else if ("center".equals(sTextAlign)) { sAlignmentChar="c"; }
                    }
                    // Textstyle to use for label:
                    String sStyleName = outline.getLevelProperty(i,XMLString.TEXT_STYLE_NAME);
                    // Prefix and suffix text to decorate the label
                    String sPrefix = outline.getLevelProperty(i,XMLString.STYLE_NUM_PREFIX);
                    String sSuffix = outline.getLevelProperty(i,XMLString.STYLE_NUM_SUFFIX);
                    // TODO is this correct?? todo: space-before??? start value???
                    BeforeAfter baText = new BeforeAfter();
                    if (!bOnlyNum) {palette.getCharSc().applyTextStyle(sStyleName,baText,new Context()); }
                    ldp.append("\\newcommand\\@textstyle")
                       .append(hm.getName(i)).append("[1]{");
                    if (!bOnlyNum && sLabelWidth!=null) {
                        ldp.append("\\makebox[").append(sLabelWidth).append("][").append(sAlignmentChar).append("]{");
                    }
			        ldp.append(baText.getBefore())
                       .append(sPrefix!=null ? sPrefix : "")
                       .append("#1")
                       .append(sSuffix!=null ? sSuffix : "")
                       .append(baText.getAfter());
                    if (!bOnlyNum && sLabelWidth!=null) {
                        ldp.append("}");
                    }
                    ldp.append("}").nl();
                }

                // The label:
                int nLevels = Misc.getPosInteger(outline.getLevelProperty(i,
                                      XMLString.TEXT_DISPLAY_LEVELS),1);
                ldp.append("\\renewcommand\\the")
                   .append(hm.getName(i))
                   .append("{");
                for (int j=i-nLevels+1; j<i; j++) {
                    ldp.append(sNumFormat[j])
                       .append("{").append(sectionName(j)).append("}")
                       .append(".");
                }
                ldp.append(sNumFormat[i])
                   .append("{").append(hm.getName(i)).append("}")
                   .append("}").nl();
            }
            
        }

        if (!bOnlyNum) {
            ldp.append("\\makeatother").nl();
        }
    }
	
    /* Check to see if this node contains any element nodes, except reference marks */
    public boolean containsElements(Node node) {
        if (!node.hasChildNodes()) { return false; }
        NodeList list = node.getChildNodes();
        int nLen = list.getLength();
        for (int i = 0; i < nLen; i++) {
            Node child = list.item(i);
            if (child.getNodeType()==Node.ELEMENT_NODE && 
                !child.getNodeName().startsWith(XMLString.TEXT_REFERENCE_MARK)) {
                return true;
            }
        }
        return false;
    }
	
    static final String sectionName(int nLevel){
        switch (nLevel) {
            case 1: return "section";
            case 2: return "subsection";
            case 3: return "subsubsection";
            case 4: return "paragraph";
            case 5: return "subparagraph";
            default: return null;
        }
    }


}
