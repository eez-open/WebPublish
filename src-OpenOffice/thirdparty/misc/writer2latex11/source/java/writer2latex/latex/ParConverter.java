/************************************************************************
 *
 *  ParConverter.java
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

//import java.util.Hashtable;

import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;

import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
//import writer2latex.latex.util.HeadingMap;
import writer2latex.latex.util.StyleMap;

/* <p>This class converts OpenDocument paragraphs (<code>text:p</code>) and
 * paragraph styles/formatting into LaTeX</p>
 * <p>Export of formatting depends on the option "formatting":</p>
 * <ul>
 * <li><code>ignore_all</code>
 * <li><code>ignore_most</code>
 * <li><code>convert_basic</code>
 * <li><code>convert_most</code>
 * <li><code>convert_all</code>
 * </ul> 
 * <p>TODO: Captions and {foot|end}notes should also use this class
 */
public class ParConverter extends StyleConverter {

    private boolean bNeedArrayBslash = false;

    /** <p>Constructs a new <code>ParConverter</code>.</p>
     */
    public ParConverter(OfficeReader ofr, LaTeXConfig config,
        ConverterPalette palette) {
        super(ofr,config,palette);
    }
	
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bNeedArrayBslash) {
            // centering and raggedright redefines \\, fix this
            // Note: aviods nameclash with tabularx (arraybackslash) 
            // TODO: Should perhaps choose to load tabularx instead?
            decl.append("\\makeatletter").nl()
                .append("\\newcommand\\arraybslash{\\let\\\\\\@arraycr}").nl()
                .append("\\makeatother").nl();
        }
	

        if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
            // We typeset with \raggedbottom since OOo doesn't use rubber lengths
            // TODO: Maybe turn vertical spacing from OOo into rubber lengths?
            decl.append("\\raggedbottom").nl();
        }

        if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
            decl.append("% Paragraph styles").nl();
            // First default paragraph style
            palette.getCharSc().applyDefaultFont(ofr.getDefaultParStyle(),decl);
            super.appendDeclarations(pack,decl);
        }         
    }
	
    /**
     * <p> Process a text:p tag</p>
     *  @param node The text:h element node containing the heading
     *  @param ldp The <code>LaTeXDocumentPortion</code> to add LaTeX code to
     *  @param oc The current context
     *  @param bLastInBlock If this is true, the paragraph is the
     *  last one in a block, and we need no trailing blank line (eg. right before
     *  \end{enumerate}).
     */
    public void handleParagraph(Element node, LaTeXDocumentPortion ldp, Context oc, boolean bLastInBlock) {
        if (palette.getMathmlCv().handleDisplayEquation(node,ldp)) { return; }
		
        // Get the style name for this paragraph
        String sStyleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);
        String sDisplayName = ofr.getParStyles().getDisplayName(sStyleName);

		
        // Check for strict handling of styles
        if (config.otherStyles()!=LaTeXConfig.ACCEPT && !config.getParStyleMap().contains(sDisplayName)) {
            if (config.otherStyles()==LaTeXConfig.WARNING) {
                System.err.println("Warning: A paragraph with style "+sDisplayName+" was ignored");
            }
            else if (config.otherStyles()==LaTeXConfig.ERROR) {
                ldp.append("% Error in source document: A paragraph with style ")
                   .append(palette.getI18n().convert(sDisplayName,false,oc.getLang()))
                   .append(" was ignored").nl();
            }
            // Ignore this paragraph:
            return;        
        }
		
        // Empty paragraphs are often (mis)used to achieve vertical spacing in WYSIWYG
        // word processors. Hence we translate an empty paragraph to \bigskip.
        // This also solves the problem that LaTeX ignores empty paragraphs, Writer doesn't.
        // In a well-structured document, an empty paragraph is probably a mistake,
        // hence the configuration can specify that it should be ignored.
        // Note: Don't use \bigskip in tables (this can lead to strange results)
        if (OfficeReader.isWhitespaceContent(node)) {
            // Always add page break; other formatting is ignored
            BeforeAfter baPage = new BeforeAfter();
            StyleWithProperties style = ofr.getParStyle(sStyleName);
            palette.getPageSc().applyPageBreak(style,true,baPage);
            if (!oc.isInTable()) { ldp.append(baPage.getBefore()); }
            if (!config.ignoreEmptyParagraphs()) {
                if (!oc.isInTable()) {
                    ldp.nl().append("\\bigskip").nl();
                }
                else {
                    ldp.append("~").nl();
                }
                if (!bLastInBlock) { ldp.nl(); }
            }
            if (!oc.isInTable()) { ldp.append(baPage.getAfter()); }
            return;
        }
		
        Context ic = (Context) oc.clone();

        // Always push the font used
        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(ofr.getParStyle(sStyleName)));
		
        // Apply the style
        BeforeAfter ba = new BeforeAfter();
        if (oc.isInTable()) {
            applyCellParStyle(sStyleName,ba,ic,OfficeReader.getCharacterCount(node)==0,bLastInBlock);
        }
        else {
            applyParStyle(sStyleName,ba,ic,OfficeReader.getCharacterCount(node)==0);
        }
		
        // Do conversion
        ldp.append(ba.getBefore());
        palette.getInlineCv().traverseInlineText(node,ldp,ic);
        ldp.append(ba.getAfter());
        // Add a blank line except within verbatim and last in a block:
        if (!bLastInBlock && !ic.isVerbatim() && !ic.isInSimpleTable()) { ldp.nl(); }
		
        // Flush any pending index marks, reference marks and floating frames
        palette.getFieldCv().flushReferenceMarks(ldp,oc);
        palette.getIndexCv().flushIndexMarks(ldp,oc);
        palette.getDrawCv().flushFloatingFrames(ldp,oc);

        // pop the font name
        palette.getI18n().popSpecialTable();
    }

    private void applyCellParStyle(String sName, BeforeAfter ba, Context context, boolean bNoTextPar, boolean bLastInBlock) {
        // Paragraph formatting for paragraphs within table cells
        // We always use simple par styles here
		
        // Add newline if *between* paragraphs
        if (!bLastInBlock) { ba.add("","\n"); }

        if (context.isInSimpleTable()) {
            if (config.formatting()!=LaTeXConfig.IGNORE_ALL) {
                // only character formatting!
                StyleWithProperties style = ofr.getParStyle(sName);
                if (style!=null) {
                    palette.getI18n().applyLanguage(style,true,true,ba);
                    palette.getCharSc().applyFont(style,true,true,ba,context);
                    if (ba.getBefore().length()>0) { ba.add(" ",""); }
                }
            }
        }
        else if (bNoTextPar && (config.formatting()==LaTeXConfig.CONVERT_BASIC || config.formatting()==LaTeXConfig.IGNORE_MOST) ) {
            // only alignment!
            StyleWithProperties style = ofr.getParStyle(sName);
            if (style!=null) {
                // Apply hard formatting attributes
                // Note: Left justified text is exported as full justified text!
                palette.getPageSc().applyPageBreak(style,false,ba);
                String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
                if (bLastInBlock && context.isInLastTableColumn()) { // no grouping needed, but need to fix problem with \\
                    if ("center".equals(sTextAlign)) { ba.add("\\centering\\arraybslash ",""); }
                    else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft\\arraybslash ",""); }
                    bNeedArrayBslash = true;
                }
                else if (bLastInBlock) { // no grouping needed
                    if ("center".equals(sTextAlign)) { ba.add("\\centering ",""); }
                    else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft ",""); }
                }
                else {
                    if ("center".equals(sTextAlign)) { ba.add("{\\centering ","\\par}"); }
                    else if ("end".equals(sTextAlign)) { ba.add("{\\raggedleft ","\\par}"); }
                }
            }
        }
        else {
            // Export character formatting + alignment only
            BeforeAfter baText = new BeforeAfter();

            // Apply hard formatting attributes
            // Note: Left justified text is exported as full justified text!
            StyleWithProperties style = ofr.getParStyle(sName);
            if (style!=null) {
                String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
                if (bLastInBlock && context.isInLastTableColumn()) { // no grouping needed, but need to fix problem with \\
                    if ("center".equals(sTextAlign)) { ba.add("\\centering\\arraybslash ",""); }
                    else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft\\arraybslash ",""); }
                    bNeedArrayBslash = true;
                }
                else if (bLastInBlock) { // no \par needed
                    if ("center".equals(sTextAlign)) { ba.add("\\centering ",""); }
                    else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft ",""); }
                }
                else {
                    if ("center".equals(sTextAlign)) { ba.add("\\centering ","\\par"); }
                    else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft ","\\par"); }
                }
                palette.getI18n().applyLanguage(style,true,true,baText);
                palette.getCharSc().applyFont(style,true,true,baText,context);
            }

            // Assemble the bits. If there is any hard character formatting
            // or alignment we must group the contents.
            if (!baText.isEmpty() && !bLastInBlock) { ba.add("{","}"); }
            ba.add(baText.getBefore(),baText.getAfter());
            if (baText.getBefore().length()>0) { ba.add(" ",""); }
        } 
		
        // Update context
        StyleWithProperties style = ofr.getParStyle(sName);
        if (style==null) { return; }
        context.updateFormattingFromStyle(style);
        context.setVerbatim(styleMap.getVerbatim(sName));
    }

	
    /** <p>Use a paragraph style in LaTeX.</p>
     *  @param <code>sName</code> the name of the text style
     *  @param <code>ba</code> a <code>BeforeAfter</code> to put code into
     *  @param <code>context</code> the current context. This method will use and update the formatting context
     *  @param <code>bNoTextPar</code> true if this paragraph has no text content (hence character formatting is not needed)  
     */
    private void applyParStyle(String sName, BeforeAfter ba, Context context, boolean bNoTextPar) {
        applyParStyle(sName,ba,context,bNoTextPar,true);
    }
	
    private void applyParStyle(String sName, BeforeAfter ba, Context context, boolean bNoTextPar, boolean bBreakInside) {
        // No style specified?
        if (sName==null) { return; }
        
        if (context.isInSimpleTable()) {
            if (config.formatting()!=LaTeXConfig.IGNORE_ALL) {
                // only character formatting!
                StyleWithProperties style = ofr.getParStyle(sName);
                if (style!=null) {
                    palette.getI18n().applyLanguage(style,true,true,ba);
                    palette.getCharSc().applyFont(style,true,true,ba,context);
                    if (ba.getBefore().length()>0) { ba.add(" ",""); }
                }
            }
        }
        else if (bNoTextPar && (config.formatting()==LaTeXConfig.CONVERT_BASIC || config.formatting()==LaTeXConfig.IGNORE_MOST) ) {
            // Always end with a line break
            ba.add("","\n");
            // only alignment!
            StyleWithProperties style = ofr.getParStyle(sName);
            if (style!=null) {
                // Apply hard formatting attributes
                // Note: Left justified text is exported as full justified text!
                palette.getPageSc().applyPageBreak(style,false,ba);
                String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
                if ("center".equals(sTextAlign)) { ba.add("{\\centering ","\\par}"); }
                else if ("end".equals(sTextAlign)) { ba.add("{\\raggedleft ","\\par}"); }
            }
        }
        else {
            // Always end with a line break
            ba.add("","\n");
            // Apply the style
            if (!styleMap.contains(sName)) { createParStyle(sName); }
            String sBefore = styleMap.getBefore(sName); 
            String sAfter = styleMap.getAfter(sName);
            ba.add(sBefore,sAfter);
            // Add line breaks inside?
            if (bBreakInside && styleMap.getLineBreak(sName)) {
                if (sBefore.length()>0) { ba.add("\n",""); }
                if (sAfter.length()>0 && !"}".equals(sAfter)) { ba.add("","\n"); }
            }
        } 
		
        // Update context
        StyleWithProperties style = ofr.getParStyle(sName);
        if (style==null) { return; }
        context.updateFormattingFromStyle(style);
        context.setVerbatim(styleMap.getVerbatim(sName));
    }
	
    /** <p>Convert a paragraph style to LaTeX. </p> 
     *  <p>A soft style is declared in <code>styleDeclarations</code> as
     *  <code>\newenvironment...</code></p>
     *  <p>A hard style is used by applying LaTeX code directly</p>
     *  @param <code>sName</code> the OOo name of the style
     */
    private void createParStyle(String sName) {
        // A paragraph style should always be created relative to main context
        Context context = (Context) palette.getMainContext().clone();
        // The style may already be declared in the configuration:
        String sDisplayName = ofr.getParStyles().getDisplayName(sName);
        StyleMap sm = config.getParStyleMap();
        if (sm.contains(sDisplayName)) {
            styleMap.put(sName,sm.getBefore(sDisplayName),sm.getAfter(sDisplayName),
                               sm.getLineBreak(sDisplayName),sm.getVerbatim(sDisplayName));
            return;
        }
        // Does the style exist?
        StyleWithProperties style = ofr.getParStyle(sName);
        if (style==null) {
            styleMap.put(sName,"","");
            return;
        }
        // Convert the style!
        switch (config.formatting()) {
            case LaTeXConfig.CONVERT_MOST:
                if (style.isAutomatic()) {
                    createAutomaticParStyle(style,context);
                    return;
                }
            case LaTeXConfig.CONVERT_ALL:
                createSoftParStyle(style,context);
                return;
            case LaTeXConfig.CONVERT_BASIC:
            case LaTeXConfig.IGNORE_MOST:
                createSimpleParStyle(style,context);
                return;
            case LaTeXConfig.IGNORE_ALL:
            default:
                styleMap.put(sName,"","");
        }
    }

    private void createAutomaticParStyle(StyleWithProperties style, Context context) {
        // Hard paragraph formatting from this style should be ignored
        // (because the user wants to ignore hard paragraph formatting
        // or there is a style map for the parent.)
        BeforeAfter ba = new BeforeAfter();
        BeforeAfter baPar = new BeforeAfter();
        BeforeAfter baText = new BeforeAfter();

        // Apply paragraph formatting from parent
        // If parent is verbatim, this is all
        String sParentName = style.getParentName();
        if (styleMap.getVerbatim(sParentName)) {
            styleMap.put(style.getName(),styleMap.getBefore(sParentName),styleMap.getAfter(sParentName),
                         styleMap.getLineBreak(sParentName),styleMap.getVerbatim(sParentName));
            return;
        }
        applyParStyle(sParentName,baPar,context,false,false);
		
        // Apply hard formatting properties:
        palette.getPageSc().applyPageBreak(style,false,ba);
        palette.getI18n().applyLanguage(style,true,false,baText);
        palette.getCharSc().applyFont(style,true,false,baText,context);

        // Assemble the bits. If there is any hard character formatting
        // we must group the contents.
        if (baPar.isEmpty() && !baText.isEmpty()) { ba.add("{","}"); }
        else { ba.add(baPar.getBefore(),baPar.getAfter()); }
        ba.add(baText.getBefore(),baText.getAfter());
        boolean bLineBreak = styleMap.getLineBreak(sParentName);
        if (!bLineBreak && !baText.isEmpty()) { ba.add(" ",""); }
        styleMap.put(style.getName(),ba.getBefore(),ba.getAfter(),bLineBreak,false);
    }
	
    private void createSimpleParStyle(StyleWithProperties style, Context context) {
        // Export character formatting + alignment only
        if (style.isAutomatic() && config.getParStyleMap().contains(ofr.getParStyles().getDisplayName(style.getParentName()))) {
            createAutomaticParStyle(style,context);
            return;
        }

        BeforeAfter ba = new BeforeAfter();
        BeforeAfter baText = new BeforeAfter();

        // Apply hard formatting attributes
        // Note: Left justified text is exported as full justified text!
        palette.getPageSc().applyPageBreak(style,false,ba);
        String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
        if ("center".equals(sTextAlign)) { baText.add("\\centering","\\par"); }
        else if ("end".equals(sTextAlign)) { baText.add("\\raggedleft","\\par"); }
        palette.getI18n().applyLanguage(style,true,true,baText);
        palette.getCharSc().applyFont(style,true,true,baText,context);

        // Assemble the bits. If there is any hard character formatting
        // or alignment we must group the contents.
        if (!baText.isEmpty()) { ba.add("{","}"); }
        ba.add(baText.getBefore(),baText.getAfter());
        styleMap.put(style.getName(),ba.getBefore(),ba.getAfter());
    }

    private void createSoftParStyle(StyleWithProperties style, Context context) {
        // This style should be converted to an enviroment, except if
        // it's automatic and there is a config style map for the parent
        if (style.isAutomatic() && config.getParStyleMap().contains(ofr.getParStyles().getDisplayName(style.getParentName()))) {
            createAutomaticParStyle(style,context);
        }

        BeforeAfter ba = new BeforeAfter();
        applyParProperties(style,ba);
        ba.add("\\writerlistparindent\\writerlistleftskip","");
        palette.getI18n().applyLanguage(style,true,true,ba);
        ba.add("\\leavevmode","");
        palette.getCharSc().applyNormalFont(ba);
        palette.getCharSc().applyFont(style,true,true,ba,context);
        ba.add("\\writerlistlabel","");
        ba.add("\\ignorespaces","");
        // Declare the paragraph style (\newenvironment)
        String sTeXName = "style" + styleNames.getExportName(style.getDisplayName());
        styleMap.put(style.getName(),"\\begin{"+sTeXName+"}","\\end{"+sTeXName+"}");
        declarations.append("\\newenvironment{").append(sTeXName)
                    .append("}{").append(ba.getBefore()).append("}{")
                    .append(ba.getAfter()).append("}").nl();
    }

    // Remaining methods are private helpers

    /** <p>Apply line spacing from a style.</p>
     *  @param <code>style</code> the paragraph style to use
     *  @param <code>ba</code> a <code>BeforeAfter</code> to put code into
     */
    private void applyLineSpacing(StyleWithProperties style, BeforeAfter ba) {
        if (style==null) { return; }
        String sLineHeight = style.getProperty(XMLString.FO_LINE_HEIGHT);
        if (sLineHeight==null || !sLineHeight.endsWith("%")) { return; }
        float fPercent=Misc.getFloat(sLineHeight.substring(0,sLineHeight.length()-1),100);
        // Do not allow less that 120% (LaTeX default)
        if (fPercent<120) { fPercent = 120; }
        ba.add("\\renewcommand\\baselinestretch{"+fPercent/120+"}","");
    }

    /** <p>Helper: Create a horizontal border. Currently unused</p>
     */
    /*private String createBorder(String sLeft, String sRight, String sTop,
                                String sHeight, String sColor) {
        BeforeAfter baColor = new BeforeAfter();
        palette.getColorCv().applyColor(sColor,false,baColor, new Context());
        return "{\\setlength\\parindent{0pt}\\setlength\\leftskip{" + sLeft + "}"
               + "\\setlength\\baselineskip{0pt}\\setlength\\parskip{" + sHeight + "}"
               + baColor.getBefore()
               + "\\rule{\\textwidth-" + sLeft + "-" + sRight + "}{" + sHeight + "}"
               + baColor.getAfter()
               + "\\par}";
    }*/	

    /** <p>Apply margin+alignment properties from a style.</p>
     *  @param <code>style</code> the paragraph style to use
     *  @param <code>ba</code> a <code>BeforeAfter</code> to put code into
     */
    private void applyMargins(StyleWithProperties style, BeforeAfter ba) {
        // Read padding/margin/indentation properties:
        //String sPaddingTop = style.getAbsoluteLength(XMLString.FO_PADDING_TOP);
        //String sPaddingBottom = style.getAbsoluteLength(XMLString.FO_PADDING_BOTTOM);
        //String sPaddingLeft = style.getAbsoluteLength(XMLString.FO_PADDING_LEFT);
        //String sPaddingRight = style.getAbsoluteLength(XMLString.FO_PADDING_RIGHT);
        String sMarginTop = style.getAbsoluteLength(XMLString.FO_MARGIN_TOP);
        String sMarginBottom = style.getAbsoluteLength(XMLString.FO_MARGIN_BOTTOM);
        String sMarginLeft = style.getAbsoluteLength(XMLString.FO_MARGIN_LEFT);
        String sMarginRight = style.getAbsoluteLength(XMLString.FO_MARGIN_RIGHT);
        String sTextIndent;
        if ("true".equals(style.getProperty(XMLString.STYLE_AUTO_TEXT_INDENT))) {
            sTextIndent = "2em";
        }
        else {
            sTextIndent = style.getAbsoluteLength(XMLString.FO_TEXT_INDENT);
        }
        // Read alignment properties:
        boolean bRaggedLeft = false; // add 1fil to \leftskip
        boolean bRaggedRight = false; // add 1fil to \rightskip
        boolean bParFill = false; // add 1fil to \parfillskip
        String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN);
        if ("center".equals(sTextAlign)) {
            bRaggedLeft = true; bRaggedRight = true; // centered paragraph
        }
        else if ("start".equals(sTextAlign)) {
            bRaggedRight = true; bParFill = true; // left aligned paragraph
        }
        else if ("end".equals(sTextAlign)) {
            bRaggedLeft = true; // right aligned paragraph
        }
        else if (!"justify".equals(style.getProperty(XMLString.FO_TEXT_ALIGN_LAST))) {
            bParFill = true; // justified paragraph with ragged last line
        }
        // Create formatting:
        String sRubberMarginTop = Misc.multiply("10%",sMarginTop);
        if (Misc.length2px(sRubberMarginTop).equals("0")) { sRubberMarginTop="1pt"; }
        String sRubberMarginBottom = Misc.multiply("10%",sMarginBottom);
        if (Misc.length2px(sRubberMarginBottom).equals("0")) { sRubberMarginBottom="1pt"; }
        ba.add("\\setlength\\leftskip{"+sMarginLeft+(bRaggedLeft?" plus 1fil":"")+"}","");
        ba.add("\\setlength\\rightskip{"+sMarginRight+(bRaggedRight?" plus 1fil":"")+"}","");
        ba.add("\\setlength\\parindent{"+sTextIndent+"}","");
        ba.add("\\setlength\\parfillskip{"+(bParFill?"0pt plus 1fil":"0pt")+"}","");
        ba.add("\\setlength\\parskip{"+sMarginTop+" plus "+sRubberMarginTop+"}",
               "\\unskip\\vspace{"+sMarginBottom+" plus "+sRubberMarginBottom+"}");
    }
	
    public void applyAlignment(StyleWithProperties style, boolean bIsSimple, boolean bInherit, BeforeAfter ba) {
        if (bIsSimple || style==null) { return; }
        String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,bInherit);
        if ("center".equals(sTextAlign)) { ba.add("\\centering",""); }
        else if ("start".equals(sTextAlign)) { ba.add("\\raggedright",""); }
        else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft",""); }
    }


    /** <p>Apply all paragraph properties.</p>
     *  @param <code>style</code> the paragraph style to use
     *  @param <code>ba</code> a <code>BeforeAfter</code> to put code into
     */
    private void applyParProperties(StyleWithProperties style, BeforeAfter ba) {
        palette.getPageSc().applyPageBreak(style,true,ba);
        ba.add("","\\par");
        applyLineSpacing(style,ba);
        applyMargins(style,ba);
    }

}
