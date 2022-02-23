/************************************************************************
 *
 *  Context.java
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
 *  Copyright: 2002-2007 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2007-11-23) 
 *
 */

package writer2latex.latex.util;

import writer2latex.office.XMLString;
import writer2latex.office.StyleWithProperties;

/** <p>LaTeX code is in general very context dependent. This class tracks the
 *  current context, which is the used by the converter to create valid and
 *  optimal LaTeX code.</p> 
 */
public class Context {

    // *** Formatting Info (current values in the source OOo document) ***
	
    // Current list style
    private String sListStyleName = null;
	
    // Current background color
    private String sBgColor = null;
	
    // Current character formatting attributes
    private String sFontName = null;
    private String sFontStyle = null;
    private String sFontVariant = null;
    private String sFontWeight = null;
    private String sFontSize = null;
    private String sFontColor = null;
    private String sLang = null;
    private String sCountry = null;
	
    // *** Structural Info (identifies contructions in the LaTeX document) ***
	
    // within the header or footer of a pagestyle
    private boolean bInHeaderFooter = false;
	
    // within a table cell
    private boolean bInTable = false; // any column
    private boolean bInLastTableColumn = false; // last column
    private boolean bInSimpleTable = false; // l, c or r-column
	
    // within a multicols environment
    private boolean bInMulticols = false;
	
    // within a list of this level
    private int nListLevel = 0;
	
    // within a section command
    private boolean bInSection = false;
	
    // within a caption
    private boolean bInCaption = false;
	
    // within a floating figure (figure environment)
    private boolean bInFigureFloat = false;
	
    // within a floating table (table envrionment)
    private boolean bInTableFloat = false;
	
    // within a minipage environment
    private boolean bInFrame = false;

    // within a \footnote or \endnote
    private boolean bInFootnote = false;
	
    // in verbatim mode
    private boolean bVerbatim = false;
	
    // in math mode
    private boolean bMathMode = false;
	
    // *** Special Info ***
	
    // Inside (inline) verbatim text, where line breaks are disallowed
    private boolean bNoLineBreaks = false;

    // Inside a construction, where footnotes are disallowed
    private boolean bNoFootnotes = false;
	
    // Inside an area, where lists are ignored
    private boolean bIgnoreLists = false;
	
    // *** Accessor Methods ***
	
    public void setBgColor(String sBgColor) { this.sBgColor = sBgColor; }
	
    public String getBgColor() { return sBgColor; }

    public void setListStyleName(String sListStyleName) { this.sListStyleName = sListStyleName; }	

    public String getListStyleName() { return sListStyleName; }
	
    public void setFontName(String sFontName) { this.sFontName = sFontName; }	

    public String getFontName() { return sFontName; }
	
    public void setFontStyle(String sFontStyle) { this.sFontStyle = sFontStyle; }	

    public String getFontStyle() { return sFontStyle; }
	
    public void setFontVariant(String sFontVariant) { this.sFontVariant = sFontVariant; }	

    public String getFontVariant() { return sFontVariant; }
	
    public void setFontWeight(String sFontWeight) { this.sFontWeight = sFontWeight; }	

    public String getFontWeight() { return sFontWeight; }
	
    public void setFontSize(String sFontSize) { this.sFontSize = sFontSize; }	

    public String getFontSize() { return sFontSize; }
	
    public void setFontColor(String sFontColor) { this.sFontColor = sFontColor; }	

    public String getFontColor() { return sFontColor; }
	
    public void setLang(String sLang) { this.sLang = sLang; }
	
    public String getLang() { return sLang; }
	
    public void setCountry(String sCountry) { this.sCountry = sCountry; }	

    public String getCountry() { return sCountry; }
	
    public void setInHeaderFooter(boolean bInHeaderFooter) {
        this.bInHeaderFooter = bInHeaderFooter;
    }
	
    public boolean isInHeaderFooter() { return bInHeaderFooter; }
	
    public void setInTable(boolean bInTable) { this.bInTable = bInTable; }
	
    public boolean isInTable() { return bInTable; }

    public void setInLastTableColumn(boolean bInLastTableColumn) { this.bInLastTableColumn = bInLastTableColumn; }
	
    public boolean isInLastTableColumn() { return bInLastTableColumn; }

    public void setInSimpleTable(boolean bInSimpleTable) { this.bInSimpleTable = bInSimpleTable; }
	
    public boolean isInSimpleTable() { return bInSimpleTable; }

    public void setInMulticols(boolean bInMulticols) {
        this.bInMulticols = bInMulticols;
    }
	
    public boolean isInMulticols() { return bInMulticols; }

    public void setListLevel(int nListLevel) { this.nListLevel = nListLevel; }
	
    public void incListLevel() { nListLevel++; }

    public int getListLevel() { return nListLevel; }

    public void setInSection(boolean bInSection) { this.bInSection = bInSection; }
	
    public boolean isInSection() { return bInSection; }

    public void setInCaption(boolean bInCaption) { this.bInCaption = bInCaption; }
	
    public boolean isInCaption() { return bInCaption; }

    public void setInFigureFloat(boolean bInFigureFloat) { this.bInFigureFloat = bInFigureFloat; }
	
    public boolean isInFigureFloat() { return bInFigureFloat; }

    public void setInTableFloat(boolean bInTableFloat) { this.bInTableFloat = bInTableFloat; }
	
    public boolean isInTableFloat() { return bInTableFloat; }

    public void setInFrame(boolean bInFrame) { this.bInFrame = bInFrame; }
	
    public boolean isInFrame() { return bInFrame; }

    public void setInFootnote(boolean bInFootnote) {
        this.bInFootnote = bInFootnote;
    }
	
    public boolean isInFootnote() { return bInFootnote; }

    public void setNoFootnotes(boolean bNoFootnotes) {
        this.bNoFootnotes = bNoFootnotes;
    }
	
    public boolean isNoFootnotes() { return bNoFootnotes; }

    public void setIgnoreLists(boolean bIgnoreLists) {
        this.bIgnoreLists = bIgnoreLists;
    }
	
    public boolean isIgnoreLists() { return bIgnoreLists; }
	
    public void setNoLineBreaks(boolean bNoLineBreaks) {
        this.bNoLineBreaks = bNoLineBreaks;
    }
    public boolean isNoLineBreaks() { return bNoLineBreaks; }

    public boolean isVerbatim() { return bVerbatim; }
	
    public void setVerbatim(boolean bVerbatim) { this.bVerbatim = bVerbatim; }
	
    public boolean isMathMode() { return bMathMode; }
	
    public void setMathMode(boolean bMathMode) { this.bMathMode = bMathMode; }
	
    // update context
	
    public void updateFormattingFromStyle(StyleWithProperties style) {
        String s;

        if (style==null) { return; }	
	
        s = style.getProperty(XMLString.STYLE_FONT_NAME);
        if (s!=null) { setFontName(s); }

        s = style.getProperty(XMLString.FO_FONT_STYLE);
        if (s!=null) { setFontStyle(s); }

        s = style.getProperty(XMLString.FO_FONT_VARIANT);
        if (s!=null) { setFontVariant(s); }

        s = style.getProperty(XMLString.FO_FONT_WEIGHT);
        if (s!=null) { setFontWeight(s); }

        s = style.getProperty(XMLString.FO_FONT_SIZE);
        if (s!=null) { setFontSize(s); }

        s = style.getProperty(XMLString.FO_COLOR);
        if (s!=null) { setFontColor(s); }

        s = style.getProperty(XMLString.FO_LANGUAGE);
        if (s!=null) { setLang(s); }

        s = style.getProperty(XMLString.FO_COUNTRY);
        if (s!=null) { setCountry(s); }
    }
	
    public void resetFormattingFromStyle(StyleWithProperties style) {
        setFontName(null);
        setFontStyle(null);
        setFontVariant(null);
        setFontWeight(null);
        setFontSize(null);
        setFontColor(null);
        setLang(null);
        setCountry(null);
        updateFormattingFromStyle(style);
    }


    // clone this Context
    public Object clone() {
        Context newContext = new Context();

        newContext.setListStyleName(sListStyleName);
        newContext.setBgColor(sBgColor);
        newContext.setFontName(sFontName);
        newContext.setFontStyle(sFontStyle);
        newContext.setFontVariant(sFontVariant);
        newContext.setFontWeight(sFontWeight);
        newContext.setFontSize(sFontSize);
        newContext.setFontColor(sFontColor);
        newContext.setLang(sLang);
        newContext.setCountry(sCountry);
        newContext.setInHeaderFooter(bInHeaderFooter);
        newContext.setInTable(bInTable);
        newContext.setInLastTableColumn(bInLastTableColumn);
        newContext.setInSimpleTable(bInSimpleTable);
        newContext.setInMulticols(bInMulticols);
        newContext.setListLevel(nListLevel);
        newContext.setInSection(bInSection);
        newContext.setInCaption(bInCaption);
        newContext.setInFigureFloat(bInFigureFloat);
        newContext.setInTableFloat(bInTableFloat);
        newContext.setInFrame(bInFrame);
        newContext.setInFootnote(bInFootnote);
        newContext.setVerbatim(bVerbatim);
        newContext.setMathMode(bMathMode);
        newContext.setNoFootnotes(bNoFootnotes);
        newContext.setIgnoreLists(bIgnoreLists);
        newContext.setNoLineBreaks(bNoLineBreaks);
		
        return newContext;
    }

}
