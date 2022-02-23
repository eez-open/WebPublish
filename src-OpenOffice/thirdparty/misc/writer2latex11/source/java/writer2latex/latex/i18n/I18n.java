/************************************************************************
 *
 *  I18n.java
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
 *  Version 1.0 (2009-02-16) 
 * 
 */

package writer2latex.latex.i18n;

import java.util.HashSet;

import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;

/** This abstract class takes care of i18n in the LaTeX export.
 *  Since i18n is handled quite differently in LaTeX "Classic"
 *  and XeTeX, we use two different classes
 */
public abstract class I18n {
    // **** Global variables ****

    // Configuration items
    protected LaTeXConfig config;
    protected ReplacementTrie stringReplace;
    protected boolean bGreekMath; // Use math mode for greek letters
    protected boolean bAlwaysUseDefaultLang; // Ignore sLang parameter to convert()

    // Collected data
    protected String sDefaultLanguage; // The default iso language to use
    protected HashSet languages = new HashSet(); // All languages used

    // **** Constructors ****

    /** Construct a new I18n as ConverterHelper
     *  @param ofr the OfficeReader to get language information from
     *  @param config the configuration which determines the symbols to use
     *  @param palette the ConverterPalette (unused)
     */
    public I18n(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        // We don't need the palette and the office reader is only used to
        // identify the default language

        // Set up config items
        this.config = config;
        stringReplace = config.getStringReplace();
        bGreekMath = config.greekMath();
        bAlwaysUseDefaultLang = !config.multilingual();
        
        // Default language
        if (ofr!=null) {
            if (config.multilingual()) {
                // Read the default language from the default paragraph style
                StyleWithProperties style = ofr.getDefaultParStyle();
                if (style!=null) { 
                    sDefaultLanguage = style.getProperty(XMLString.FO_LANGUAGE);
                }
            }
            else {
                // the most common language is the only language
                sDefaultLanguage = ofr.getMajorityLanguage();
            }
        }
        if (sDefaultLanguage==null) { sDefaultLanguage="en"; }
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public abstract void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl);
	
    /** Apply a language language
     *  @param style the OOo style to read attributesfrom
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public abstract void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba);

    /** Push a font to the font stack
     *  @param sName the name of the font
     */
    public abstract void pushSpecialTable(String sName);
	
    /** Pop a font from the font stack
     */
    public abstract void popSpecialTable();
	
    /** Convert a string of characters into LaTeX
     *  @param s the source string
     *  @param bMathMode true if the string should be rendered in math mode
     *  @param sLang the iso language of the string
     *  @return the LaTeX string
     */
    public abstract String convert(String s, boolean bMathMode, String sLang);
}
