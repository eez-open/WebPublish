/************************************************************************
 *
 *  XeTeXI18n.java
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
 *  Version 1.0 (2009-02-17) 
 * 
 */

package writer2latex.latex.i18n;

import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;

/** This class takes care of i18n in XeLaTeX
 */
public class XeTeXI18n extends I18n {

    // **** Constructors ****

    /** Construct a new XeTeXI18n as ConverterHelper
     *  @param ofr the OfficeReader to get language information from
     *  @param config the configuration which determines the symbols to use
     *  @param palette the ConverterPalette (unused)
     */
    public XeTeXI18n(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
    	super(ofr,config,palette);
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
    	pack.append("\\usepackage{fontspec}").nl()
    		.append("\\usepackage{xunicode}").nl()
    		.append("\\usepackage{xltxtra}").nl()
    		.append("\\usepackage{amsmath,amssymb,amsfonts}").nl();
    }
	
    /** Apply a language language
     *  @param style the OOo style to read attributesfrom
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba) {
    	// TODO (polyglossia)
    }

    /** Push a font to the font stack
     *  @param sName the name of the font
     */
    public void pushSpecialTable(String sName) {
    	// TODO
    }
	
    /** Pop a font from the font stack
     */
    public void popSpecialTable() {
    	// TODO
    }
	
    /** Convert a string of characters into LaTeX
     *  @param s the source string
     *  @param bMathMode true if the string should be rendered in math mode
     *  @param sLang the iso language of the string
     *  @return the LaTeX string
     */
    public String convert(String s, boolean bMathMode, String sLang){
    	StringBuffer buf = new StringBuffer();
    	int nLen = s.length();
    	char c;
    	for (int i=0; i<nLen; i++) {
    		c = s.charAt(i);
    		switch (c) {
    			case '"' : buf.append("\\textquotedbl{}"); break;
    			case '#' : buf.append("\\#"); break;
    			case '$' : buf.append("\\$"); break;
    			case '%' : buf.append("\\%"); break;
    			case '&' : buf.append("\\&"); break;
    			case '\'' : buf.append("\\textbackslash{}"); break;
    			case '<' : buf.append("\\textless{}"); break;
    			case '>' : buf.append("\\textgreater{}"); break;
    			case '\\' : buf.append("\\textbackslash{}"); break;
    			case '\u005e' : buf.append("\\^{}"); break;
    			case '_' : buf.append("\\_"); break;
    			case '\u0060' : buf.append("\\textasciigrave{}"); break;
    			case '{' : buf.append("\\{"); break;
    			case '|' : buf.append("\\textbar{}"); break;
    			case '}' : buf.append("\\}"); break;
    			case '~' : buf.append("\\~{}"); break;
    			default: buf.append(c);
    		}
    	}
    	return buf.toString();
    }
	

}
