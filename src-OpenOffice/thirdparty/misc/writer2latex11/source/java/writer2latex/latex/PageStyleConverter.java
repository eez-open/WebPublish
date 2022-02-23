/************************************************************************
 *
 *  PageStyleConverter.java
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

import java.util.Enumeration;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.util.CSVList;
import writer2latex.util.Misc;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

// TODO: chngpage.sty??

/* This class creates LaTeX code from OOo page layouts/master pages
 */
public class PageStyleConverter extends StyleConverter {

    // Value of attribute text:display of most recent text:chapter field
    // This is used to handle chaptermarks in headings
    private String sChapterField1 = null;
    private String sChapterField2 = null;
	
    // The page layout used for the page geometry
    // (LaTeX only supports one page geometry per page)
    private PageLayout mainPageLayout;

    /** <p>Constructs a new <code>PageStyleConverter</code>.</p>
     */ 
    public PageStyleConverter(OfficeReader ofr, LaTeXConfig config,
        ConverterPalette palette) {
        super(ofr,config,palette);
        // Determine the main page master
        MasterPage firstMasterPage = ofr.getFirstMasterPage();
        String sPageLayoutName = null;
        if (firstMasterPage!=null) {
            MasterPage nextMasterPage = ofr.getMasterPage(
                firstMasterPage.getProperty(XMLString.STYLE_NEXT_STYLE_NAME));
            if (nextMasterPage!=null) {
                sPageLayoutName = nextMasterPage.getPageLayoutName();
            }
            else {
                sPageLayoutName = firstMasterPage.getPageLayoutName();
            }
        }
        mainPageLayout = ofr.getPageLayout(sPageLayoutName);
    }

    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (config.useFancyhdr()) { pack.append("\\usepackage{fancyhdr}").nl(); }
        // The first master page must be known
        MasterPage firstMasterPage = ofr.getFirstMasterPage();
        if (firstMasterPage!=null) {
            styleNames.addName(getDisplayName(firstMasterPage.getName()));
        }
        // Convert page geometry
        convertPageMasterGeometry(pack,decl);
        // Convert master pages
        convertMasterPages(decl);
        if (firstMasterPage!=null) {
            BeforeAfter ba = new BeforeAfter();
            applyMasterPage(firstMasterPage.getName(),ba);
            decl.append(ba.getBefore());
        }

    }
	
    public void setChapterField1(String s) { sChapterField1 = s; }
	
    public void setChapterField2(String s) { sChapterField2 = s; }
	
    public boolean isTwocolumn() {
        return mainPageLayout!=null && mainPageLayout.getColCount()>1;
    }
	
    /** <p>Apply page break properties from a style.</p>
     *  @param style the style to use
     *  @param bInherit true if inheritance from parent style should be used
     *  @param ba a <code>BeforeAfter</code> to put code into
     */
    public void applyPageBreak(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style==null) { return; }
        if (style.isAutomatic() && config.ignoreHardPageBreaks()) { return; }
        // A page break can be a simple page break before or after...
        String s = style.getProperty(XMLString.FO_BREAK_BEFORE,bInherit);
        if ("page".equals(s)) { ba.add("\\clearpage",""); }
        s = style.getProperty(XMLString.FO_BREAK_AFTER,bInherit);
        if ("page".equals(s)) { ba.add("","\\clearpage"); }
        // ...or it can be a new master page
        String sMasterPage = style.getMasterPageName();
        if (sMasterPage==null || sMasterPage.length()==0) { return; }
        ba.add("\\clearpage","");
        String sPageNumber=style.getProperty(XMLString.STYLE_PAGE_NUMBER);
        if (sPageNumber!=null) {
            int nPageNumber = Misc.getPosInteger(sPageNumber,1);
            ba.add("\\setcounter{page}{"+nPageNumber+"}","");
        }
        applyMasterPage(sMasterPage,ba);
    }
		
    /** <p>Use a Master Page (pagestyle in LaTeX)</p>
     *  @param sName    name of the master page to use
     *  @param ba      the <code>BeforeAfter</code> to add code to.
     */
    private void applyMasterPage(String sName, BeforeAfter ba) {
        if (config.pageFormatting()==LaTeXConfig.IGNORE_ALL) return;
        MasterPage style = ofr.getMasterPage(sName);
        if (style==null) { return; }
        String sNextName = style.getProperty(XMLString.STYLE_NEXT_STYLE_NAME);
        MasterPage nextStyle = ofr.getMasterPage(sNextName);
        if (style==nextStyle || nextStyle==null) {
            ba.add("\\pagestyle{"+styleNames.getExportName(getDisplayName(sName))+"}\n", "");
        }
        else {
            ba.add("\\pagestyle{"+styleNames.getExportName(getDisplayName(sNextName))+"}\n"+
               "\\thispagestyle{"+styleNames.getExportName(getDisplayName(sName))+"}\n","");
        }
        // todo: should warn the user if next master also contains a next-style-name;
        // LaTeX's page style mechanism cannot handle that
    }
	
    /*
     * Process header or footer contents
     */
    private void convertMasterPages(LaTeXDocumentPortion ldp) {
        if (config.pageFormatting()==LaTeXConfig.IGNORE_ALL) { return; }

        Context context = new Context();
        context.resetFormattingFromStyle(ofr.getDefaultParStyle());
        context.setInHeaderFooter(true);
		

        Enumeration styles = ofr.getMasterPages().getStylesEnumeration();
        ldp.append("% Pages styles").nl();
        if (!config.useFancyhdr()) {
            ldp.append("\\makeatletter").nl();
        }
        while (styles.hasMoreElements()) {
            MasterPage style = (MasterPage) styles.nextElement();
            String sName = style.getName();
            if (styleNames.containsName(getDisplayName(sName))) {
                sChapterField1 = null;
                sChapterField2 = null;

                String sPageLayout = style.getPageLayoutName();
                PageLayout pageLayout = ofr.getPageLayout(sPageLayout);

                if (config.useFancyhdr()) {
                    ldp.append("\\fancypagestyle{")
                       .append(styleNames.getExportName(getDisplayName(sName)))
                       .append("}{\\fancyhf{}").nl();
                    // Header - odd or both
                    ldp.append("  \\fancyhead[")
                       .append(getParAlignment(style.getHeader()))
                       .append(style.getHeaderLeft()!=null ? "O" : "")
                       .append("]{");
                    traverseHeaderFooter((Element)style.getHeader(),ldp,context);
                    ldp.append("}").nl();
                    // Header - even
                    if (style.getHeaderLeft()!=null) {
                        ldp.append("  \\fancyhead[")
                           .append(getParAlignment(style.getHeaderLeft()))
                           .append("E]{");
                        traverseHeaderFooter((Element)style.getHeaderLeft(),ldp,context);
                        ldp.append("}").nl();
                    }
                    // Footer - odd or both
                    ldp.append("  \\fancyfoot[")
                       .append(getParAlignment(style.getFooter()))
                       .append(style.getFooterLeft()!=null ? "O" : "")
                       .append("]{");
                    traverseHeaderFooter((Element)style.getFooter(),ldp,context);
                    ldp.append("}").nl();
                    // Footer - even
                    if (style.getFooterLeft()!=null) {
                        ldp.append("  \\fancyfoot[")
                           .append(getParAlignment(style.getFooterLeft()))
                           .append("E]{");
                        traverseHeaderFooter((Element)style.getFooterLeft(),ldp,context);
                        ldp.append("}").nl();
                    }
                    // Rules
                    ldp.append("  \\renewcommand\\headrulewidth{")
                       .append(getBorderWidth(pageLayout,true))
                       .append("}").nl()
                       .append("  \\renewcommand\\footrulewidth{")
                       .append(getBorderWidth(pageLayout,false))
                       .append("}").nl();
                }
                else { // use low-level page styles
                    ldp.append("\\newcommand\\ps@")
                       .append(styleNames.getExportName(getDisplayName(sName)))
                       .append("{").nl();
                    // Header
                    ldp.append("  \\renewcommand\\@oddhead{");
                    traverseHeaderFooter((Element)style.getHeader(),ldp,context);
                    ldp.append("}").nl();
                    ldp.append("  \\renewcommand\\@evenhead{");
                    if (style.getHeaderLeft()!=null) {
                        traverseHeaderFooter((Element)style.getHeaderLeft(),ldp,context);
                    }
                    else if (style.getHeader()!=null) {
                        ldp.append("\\@oddhead");
                    }
                    ldp.append("}").nl();
                    // Footer
                    ldp.append("  \\renewcommand\\@oddfoot{");
                    traverseHeaderFooter((Element)style.getFooter(),ldp,context);
                    ldp.append("}").nl();
                    ldp.append("  \\renewcommand\\@evenfoot{");
                    if (style.getFooterLeft()!=null) {
                        traverseHeaderFooter((Element)style.getFooterLeft(),ldp,context);
                    }
                    else if (style.getFooter()!=null) {
                        ldp.append("\\@oddfoot");
                    }
                    ldp.append("}").nl();
                }
				
                // Sectionmark and subsectionmark
                if (sChapterField1!=null) {
                    ldp.append("  \\def\\sectionmark##1{\\markboth{");
                    if ("name".equals(sChapterField1)) { ldp.append("##1"); }
                    else if ("number".equals(sChapterField1) || "plain-number".equals(sChapterField1)) {
                        ldp.append("\\thesection");
                    }
                    else { ldp.append("\\thesection\\ ##1"); }
                    ldp.append("}{}}").nl();
                }
                if (sChapterField2!=null) {
                    if (sChapterField1==null) {
                        ldp.append("  \\def\\sectionmark##1{\\markboth{}{}}").nl();
                    }
                    ldp.append("  \\def\\subsectionmark##1{\\markright{");
                    if ("name".equals(sChapterField2)) { ldp.append("##1"); }
                    else if ("number".equals(sChapterField2) || "plain-number".equals(sChapterField1)) {
                        ldp.append("\\thesubsection");
                    }
                    else { ldp.append("\\thesubsection\\ ##1"); }
                    ldp.append("}{}}").nl();
                }
                // Page number (this is the only part of the page master used in each page style)
                if (pageLayout!=null) {
                    String sNumFormat = pageLayout.getProperty(XMLString.STYLE_NUM_FORMAT);
                    if (sNumFormat!=null) {
                    ldp.append("  \\renewcommand\\thepage{")
                       .append(ListStyleConverter.numFormat(sNumFormat))
                       .append("{page}}").nl();
                    }
                    String sPageNumber = pageLayout.getProperty(XMLString.STYLE_FIRST_PAGE_NUMBER);
                    if (sPageNumber!=null && !sPageNumber.equals("continue")) {
                    ldp.append("  \\setcounter{page}{")
                       .append(Integer.toString(Misc.getPosInteger(sPageNumber,0)))
                       .append("}").nl();
                    }
                }

                ldp.append("}").nl();
            }
        }
        if (!config.useFancyhdr()) {
            ldp.append("\\makeatother").nl();
        }
    }
	
    // Get alignment of first paragraph in node
    private String getParAlignment(Node node) {
        String sAlign = "L";
        if (node!=null) {
            Element par = Misc.getChildByTagName(node,XMLString.TEXT_P);
            if (par!=null) {
                String sStyleName = Misc.getAttribute(par,XMLString.TEXT_STYLE_NAME);
                StyleWithProperties style = ofr.getParStyle(sStyleName);
                if (style!=null) {
                    String s = style.getProperty(XMLString.FO_TEXT_ALIGN);
                    if ("center".equals(s)) { sAlign = "C"; }
                    else if ("end".equals(s)) { sAlign = "R"; }
                }
            }
        }
        return sAlign;
    }
	
    // Get border width from header/footer style
    private String getBorderWidth(PageLayout style, boolean bHeader) {
        if (style==null) { return "0pt"; }
        String sBorder;
        if (bHeader) {
            sBorder = style.getHeaderProperty(XMLString.FO_BORDER_BOTTOM);
            if (sBorder==null) {
                sBorder = style.getHeaderProperty(XMLString.FO_BORDER);
            }
        }
        else {
            sBorder = style.getFooterProperty(XMLString.FO_BORDER_TOP);
            if (sBorder==null) {
                sBorder = style.getFooterProperty(XMLString.FO_BORDER);
            }
        }
        if (sBorder!=null && !sBorder.equals("none")) {
            return sBorder.substring(0,sBorder.indexOf(' '));
        }
        else {
            return "0pt";
        }
    }

    private void traverseHeaderFooter(Element node, LaTeXDocumentPortion ldp, Context context) {
        if (node==null) { return; }
        // get first paragraph; all other content is ignored
        Element par = Misc.getChildByTagName(node,XMLString.TEXT_P);
        if (par==null) { return; }
		
        String sStyleName = par.getAttribute(XMLString.TEXT_STYLE_NAME);
        BeforeAfter ba = new BeforeAfter();
        // Temp solution: Ignore hard formatting in header/footer (name clash problem)
        // only in package format. TODO: Find a better solution!
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style!=null && (!ofr.isPackageFormat() || !style.isAutomatic())) {
            palette.getCharSc().applyHardCharFormatting(style,ba);
        }

        if (par.hasChildNodes()) {
            ldp.append(ba.getBefore());
            palette.getInlineCv().traverseInlineText(par,ldp,context);
            ldp.append(ba.getAfter());
        }
        
    }

    // TODO: Reenable several geometries per document??
    private void convertPageMasterGeometry(LaTeXDocumentPortion pack, LaTeXDocumentPortion ldp) {
        if (config.pageFormatting()!=LaTeXConfig.CONVERT_ALL) { return; }
        if (mainPageLayout==null) { return; }

        // Set global document options
        if ("mirrored".equals(mainPageLayout.getPageUsage())) {
            palette.addGlobalOption("twoside");
        }
        if (isTwocolumn()) {
            palette.addGlobalOption("twocolumn");
        }

        // Collect all page geometry
        // 1. Page size
        String sPaperHeight = mainPageLayout.getAbsoluteProperty(XMLString.FO_PAGE_HEIGHT);
        String sPaperWidth = mainPageLayout.getAbsoluteProperty(XMLString.FO_PAGE_WIDTH);
        // 2. Margins
        String sMarginTop = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_TOP);
        String sMarginBottom = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_BOTTOM);
        String sMarginLeft = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_LEFT);
        String sMarginRight = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_RIGHT);
        // 3. Header+footer dimensions
        String sHeadHeight = "0cm";
        String sHeadSep = "0cm";
        String sFootHeight = "0cm";
        String sFootSep = "0cm";
        boolean bIncludeHead = false;
        boolean bIncludeFoot = false;
        // Look through all applied page layouts and use largest heights
        Enumeration masters = ofr.getMasterPages().getStylesEnumeration();
        while (masters.hasMoreElements()) {
            MasterPage master = (MasterPage) masters.nextElement();
            if (styleNames.containsName(getDisplayName(master.getName()))) {
                PageLayout layout = ofr.getPageLayout(master.getPageLayoutName());
                if (layout!=null) {
                    if (layout.hasHeaderStyle()) {
                        String sThisHeadHeight = layout.getHeaderProperty(XMLString.FO_MIN_HEIGHT);
                        if (sThisHeadHeight!=null && Misc.isLessThan(sHeadHeight,sThisHeadHeight)) {
                            sHeadHeight = sThisHeadHeight;
                        }
                        String sThisHeadSep = layout.getHeaderProperty(XMLString.FO_MARGIN_BOTTOM);
                        if (sThisHeadSep!=null && Misc.isLessThan(sHeadSep,sThisHeadSep)) {
                            sHeadSep = sThisHeadSep;
                        }
                        bIncludeHead = true;
                    }
                    if (layout.hasFooterStyle()) {
                        String sThisFootHeight = layout.getFooterProperty(XMLString.FO_MIN_HEIGHT);
                        if (sThisFootHeight!=null && Misc.isLessThan(sFootHeight,sThisFootHeight)) {
                            sFootHeight = sThisFootHeight;
                        }
                        String sThisFootSep = layout.getFooterProperty(XMLString.FO_MARGIN_TOP);
                        if (sThisFootSep!=null && Misc.isLessThan(sFootSep,sThisFootSep)) {
                            sFootSep = sThisFootSep;
                        }
                        bIncludeFoot = true;
                    }
                }
            }
        }
        // Define 12pt as minimum height (the source may specify 0pt..)
        if (bIncludeHead && Misc.isLessThan(sHeadHeight,"12pt")) {
            sHeadHeight = "12pt";
        }
        if (bIncludeFoot && Misc.isLessThan(sFootHeight,"12pt")) {
            sFootHeight = "12pt";
        }
           
        String sFootSkip = Misc.add(sFootHeight,sFootSep);
		
        if (config.useGeometry()) {
            // Set up options for geometry.sty
		    CSVList props = new CSVList(",");
            if (!standardPaperSize(sPaperWidth,sPaperHeight)) {
                props.addValue("paperwidth="+sPaperWidth);
                props.addValue("paperheight="+sPaperHeight);
            }
            props.addValue("top="+sMarginTop);
            props.addValue("bottom="+sMarginBottom);
            props.addValue("left="+sMarginLeft);
            props.addValue("right="+sMarginRight);
            if (bIncludeHead) {
                props.addValue("includehead");
                props.addValue("head="+sHeadHeight);
                props.addValue("headsep="+sHeadSep);
            }
            else {
                props.addValue("nohead");
            }
            if (bIncludeFoot) {
                props.addValue("includefoot");
                props.addValue("foot="+sFootHeight);
                props.addValue("footskip="+sFootSkip);
            }
            else {
                props.addValue("nofoot");
            }
            // Use the package
            pack.append("\\usepackage[").append(props.toString()).append("]{geometry}").nl();
            
        }
        else {
            // Calculate text height and text width
            String sTextHeight = Misc.sub(sPaperHeight,sMarginTop);
            sTextHeight = Misc.sub(sTextHeight,sHeadHeight);
            sTextHeight = Misc.sub(sTextHeight,sHeadSep);
            sTextHeight = Misc.sub(sTextHeight,sFootSkip);
            sTextHeight = Misc.sub(sTextHeight,sMarginBottom);
            String sTextWidth = Misc.sub(sPaperWidth,sMarginLeft);
            sTextWidth = Misc.sub(sTextWidth,sMarginRight);

            ldp.append("% Page layout (geometry)").nl();

            // Page dimensions
            if (!standardPaperSize(sPaperWidth,sPaperHeight)) {
                ldp.append("\\setlength\\paperwidth{").append(sPaperWidth).append("}").nl()
                   .append("\\setlength\\paperheight{").append(sPaperHeight).append("}").nl();
            }

            // PDF page dimensions, only if hyperref.sty is not loaded
            if (config.getBackend()==LaTeXConfig.PDFTEX && !config.useHyperref()) {
                ldp.append("\\setlength\\pdfpagewidth{").append(sPaperWidth).append("}").nl()
                   .append("\\setlength\\pdfpageheight{").append(sPaperHeight).append("}").nl();
            }

            // Page starts in upper left corner of paper!!
            ldp.append("\\setlength\\voffset{-1in}").nl()
               .append("\\setlength\\hoffset{-1in}").nl();

            // Margins
            ldp.append("\\setlength\\topmargin{").append(sMarginTop).append("}").nl()
               .append("\\setlength\\oddsidemargin{").append(sMarginLeft).append("}").nl();
            // Left margin for even (left) pages; only for mirrored page master
            if ("mirrored".equals(mainPageLayout.getPageUsage())) {
                ldp.append("\\setlength\\evensidemargin{").append(sMarginRight).append("}").nl();
            }

            // Text size (sets bottom and right margins indirectly)
            ldp.append("\\setlength\\textheight{").append(sTextHeight).append("}").nl();
            ldp.append("\\setlength\\textwidth{").append(sTextWidth).append("}").nl();

            // Header and footer
            ldp.append("\\setlength\\footskip{").append(sFootSkip).append("}").nl();
            ldp.append("\\setlength\\headheight{").append(sHeadHeight).append("}").nl();
            ldp.append("\\setlength\\headsep{").append(sHeadSep).append("}").nl();
        }

        // Footnote rule
        // TODO: Support alignment.
        String sAdjustment = mainPageLayout.getFootnoteProperty(XMLString.STYLE_ADJUSTMENT);
        String sBefore = mainPageLayout.getFootnoteProperty(XMLString.STYLE_DISTANCE_BEFORE_SEP);
        if (sBefore==null) { sBefore = "1mm"; }
        String sAfter = mainPageLayout.getFootnoteProperty(XMLString.STYLE_DISTANCE_AFTER_SEP);
        if (sAfter==null) { sAfter = "1mm"; }
        String sHeight = mainPageLayout.getFootnoteProperty(XMLString.STYLE_WIDTH);
        if (sHeight==null) { sHeight = "0.2mm"; }
        String sWidth = mainPageLayout.getFootnoteProperty(XMLString.STYLE_REL_WIDTH);
        if (sWidth==null) { sWidth = "25%"; }
        sWidth=Float.toString(Misc.getFloat(sWidth.substring(0,sWidth.length()-1),1)/100);
        BeforeAfter baColor = new BeforeAfter();
        String sColor = mainPageLayout.getFootnoteProperty(XMLString.STYLE_COLOR);
        palette.getColorCv().applyColor(sColor,false,baColor,new Context());
		
        String sSkipFootins = Misc.add(sBefore,sHeight);
 
        ldp.append("% Footnote rule").nl()
           .append("\\setlength{\\skip\\footins}{").append(sSkipFootins).append("}").nl()
           .append("\\renewcommand\\footnoterule{\\vspace*{-").append(sHeight)
           .append("}");
        if ("right".equals(sAdjustment)) {
            ldp.append("\\setlength\\leftskip{0pt plus 1fil}\\setlength\\rightskip{0pt}");
        }
        else if ("center".equals(sAdjustment)) {
            ldp.append("\\setlength\\leftskip{0pt plus 1fil}\\setlength\\rightskip{0pt plus 1fil}");
        }
        else { // default left
            ldp.append("\\setlength\\leftskip{0pt}\\setlength\\rightskip{0pt plus 1fil}");
        }
        ldp.append("\\noindent")
           .append(baColor.getBefore()).append("\\rule{").append(sWidth)
           .append("\\columnwidth}{").append(sHeight).append("}")
           .append(baColor.getAfter())
           .append("\\vspace*{").append(sAfter).append("}}").nl();
    }
    
    private boolean standardPaperSize(String sWidth, String sHeight) {
        if (standardPaperSize1(sWidth,sHeight)) {
            return true;
        }
        else if (standardPaperSize1(sHeight,sWidth)) {
            palette.addGlobalOption("landscape");
            return true;
        }
        return false;
    }
    
    private boolean standardPaperSize1(String sWidth, String sHeight) {
        // The list of known paper sizes in LaTeX's standard classes is rather short
        if (compare(sWidth, "210mm", "0.5mm") && compare(sHeight, "297mm", "0.5mm")) {
            palette.addGlobalOption("a4paper");
            return true;
        }
        else if (compare(sWidth, "148mm", "0.5mm") && compare(sHeight, "210mm", "0.5mm")) {
            palette.addGlobalOption("a5paper");
            return true;
        }
        else if (compare(sWidth, "176mm", "0.5mm") && compare(sHeight, "250mm", "0.5mm")) {
            palette.addGlobalOption("b5paper");
            return true;
        }
        else if (compare(sWidth, "8.5in", "0.02in") && compare(sHeight, "11in", "0.02in")) {
            palette.addGlobalOption("letterpaper");
            return true;
        }
        else if (compare(sWidth, "8.5in", "0.02in") && compare(sHeight, "14in", "0.02in")) {
            palette.addGlobalOption("legalpaper");
            return true;
        }
        else if (compare(sWidth, "7.25in", "0.02in") && compare(sHeight, "10.5in", "0.02in")) {
            palette.addGlobalOption("executivepaper");
            return true;
        }
        return false;
    }
	
    private boolean compare(String sLength1, String sLength2, String sTolerance) {
        return Misc.isLessThan(Misc.abs(Misc.sub(sLength1,sLength2)),sTolerance);
    }

    /* Helper: Get display name, or original name if it doesn't exist */
    private String getDisplayName(String sName) {
        String sDisplayName = ofr.getMasterPages().getDisplayName(sName);
        return sDisplayName!=null ? sDisplayName : sName;
    }


	
}
