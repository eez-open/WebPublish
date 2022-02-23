/************************************************************************
 *
 *  ClassicI18n.java
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
 *  Version 1.0 (2009-09-07) 
 * 
 */

package writer2latex.latex.i18n;

import java.io.*;
import java.util.Hashtable;
import java.util.Stack;
//import java.util.Vector;
//import java.util.Enumeration;
import java.util.Iterator;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import writer2latex.util.CSVList;
import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;

/** This class (and the helpers in the same package) takes care of i18n in
 *  Writer2LaTeX.  In classic LaTeX, i18n is a mixture of inputencodings, fontencodings
 *  and babel languages. The class ClassicI18n thus manages these, and in particular
 *  implements a Unicode->LaTeX translation that can handle different
 *  inputencodings and fontencodings.
 *  The translation is table driven, using symbols.xml (embedded in the jar)
 *  Various sections of symbols.xml handles different cases:
 *  <ul>
 *    <li>common symbols in various font encodings such as T1, T2A, LGR etc.</li>
 *    <li>input encodings such as ISO-8859-1 (latin-1), ISO-8859-7 (latin/greek) etc.</li>
 *    <li>additional symbol fonts such as wasysym, dingbats etc.</li>
 *    <li>font-specific symbols, eg. for 8-bit fonts/private use area</li>
 *  </ul>
 *  The class uses the packages inputenc, fontenc, babel, tipa, bbding, 
 *  ifsym, pifont, eurosym, amsmath, wasysym, amssymb, amsfonts and textcomp
 *  in various combinations depending on the configuration.
 */
public class ClassicI18n extends I18n {
    // **** Static data and methods: Inputencodings ****
    public static final int ASCII = 0;
    public static final int LATIN1 = 1; // ISO Latin 1 (ISO-8859-1)
    public static final int LATIN2 = 2; // ISO Latin 1 (ISO-8859-1)
    public static final int ISO_8859_7 = 3; // ISO latin/greek
    public static final int CP1250 = 4; // Microsoft Windows Eastern European
    public static final int CP1251 = 5; // Microsoft Windows Cyrillic
    public static final int KOI8_R = 6; // Latin/russian
    public static final int UTF8 = 7; // UTF-8

    // Read an inputencoding from a string
    public static final int readInputenc(String sInputenc) {
        if ("ascii".equals(sInputenc)) return ASCII;
        else if ("latin1".equals(sInputenc)) return LATIN1;
        else if ("latin2".equals(sInputenc)) return LATIN2;
        else if ("iso-8859-7".equals(sInputenc)) return ISO_8859_7;
        else if ("cp1250".equals(sInputenc)) return CP1250;
        else if ("cp1251".equals(sInputenc)) return CP1251;
        else if ("koi8-r".equals(sInputenc)) return KOI8_R;
        else if ("utf8".equals(sInputenc)) return UTF8;
        else return ASCII; // unknown = ascii
    }

    // Return the LaTeX name of an inputencoding
    public static final String writeInputenc(int nInputenc) {
        switch (nInputenc) {
            case ASCII : return "ascii";
            case LATIN1 : return "latin1";
            case LATIN2 : return "latin2";
            case ISO_8859_7 : return "iso-8859-7";
            case CP1250 : return "cp1250";
            case CP1251 : return "cp1251";
            case KOI8_R : return "koi8-r";
            case UTF8 : return "utf8";
            default : return "???";
        }
    }
	
    // Return the java i18n name of an inputencoding
    public static final String writeJavaEncoding(int nInputenc) {
        switch (nInputenc) {
            case ASCII : return "ASCII";
            case LATIN1 : return "ISO8859_1";
            case LATIN2 : return "ISO8859_2";
            case ISO_8859_7 : return "ISO8859_7";
            case CP1250 : return "Cp1250";
            case CP1251 : return "Cp1251";
            case KOI8_R : return "KOI8_R";
            case UTF8 : return "UTF-8";
            default : return "???";
        }
    }
	
    // **** Static data and methods: Fontencodings ****
    private static final int T1_ENC = 1;
    private static final int T2A_ENC = 2;
    private static final int T3_ENC = 4;
    private static final int LGR_ENC = 8;
    private static final int ANY_ENC = 15;

    // read set of font encodings from a string
    public static final int readFontencs(String sFontencs) {
        sFontencs = sFontencs.toUpperCase();
        if ("ANY".equals(sFontencs)) return ANY_ENC;
        int nFontencs = 0;
        if (sFontencs.indexOf("T1")>=0)  nFontencs+=T1_ENC; 
        if (sFontencs.indexOf("T2A")>=0) nFontencs+=T2A_ENC; 
        if (sFontencs.indexOf("T3")>=0) nFontencs+=T3_ENC; 
        if (sFontencs.indexOf("LGR")>=0) nFontencs+=LGR_ENC;
        return nFontencs;
    }
	
    // return string representation of a single font encoding
    /*private static final String writeFontenc(int nFontenc) {
        switch (nFontenc) {
            case T1_ENC: return "T1";
            case T2A_ENC: return "T2A";
            case T3_ENC: return "T3";
            case LGR_ENC: return "LGR";
        }
        return null;
    }*/
	
    // check that a given set of font encodings contains a specific font encoding
    private static final boolean supportsFontenc(int nFontencs, int nFontenc) {
        return (nFontencs & nFontenc) != 0;
    }

    // get one fontencoding from a set of fontencodings
    private static final int getFontenc(int nFontencs) {
        if (supportsFontenc(nFontencs,T1_ENC)) return T1_ENC;
        if (supportsFontenc(nFontencs,T2A_ENC)) return T2A_ENC;
        if (supportsFontenc(nFontencs,T3_ENC)) return T3_ENC;
        if (supportsFontenc(nFontencs,LGR_ENC)) return LGR_ENC;
        return 0;
    }
	
    // get the font encoding for a specific iso language
    private static final int getFontenc(String sLang) {
        // Greek uses "local greek" encoding
        if ("el".equals(sLang)) return LGR_ENC;
        // Russian, ukrainian, bulgarian and serbian uses T2A encoding
        else if ("ru".equals(sLang)) return T2A_ENC;
        else if ("uk".equals(sLang)) return T2A_ENC;
        else if ("bg".equals(sLang)) return T2A_ENC;
        else if ("sr".equals(sLang)) return T2A_ENC;
        // Other languages uses T1 encoding
        else return T1_ENC;
    }

    // return cs for a fontencoding
    private static final String getFontencCs(int nFontenc) {
        switch (nFontenc) {
            case T1_ENC: return "\\textlatin"; // requires babel
            case T2A_ENC: return "\\textcyrillic"; // requires babel with russian, bulgarian or ukrainian option
            case T3_ENC: return "\\textipa"; // requires tipa.sty
            case LGR_ENC: return "\\textgreek"; // requires babel with greek option
            default: return null;
        }
    }
		
    // End of static part of I18n!
	 
    // **** Global variables ****
    private Hashtable babelLanguages; // mappings iso->babel language

    // Unicode translation
    private Hashtable tableSet; // all tables
    private UnicodeTable table; // currently active table (top of stack)
    private Stack tableStack; // stack of active tables
    private UnicodeStringParser ucparser; // Unicode string parser

    // Collected data
    private int nDefaultFontenc; // Fontenc for the default language
    private boolean bT2A = false; // Do we use cyrillic letters?
    private boolean bGreek = false; // Do we use greek letters?
    private boolean bPolytonicGreek = false; // Do we use polytonic greek letters?

    // **** Constructors ****

    /** Construct a new ClassicI18n as ConverterHelper
     *  @param ofr the OfficeReader to get language information from
     *  @param config the configuration which determines the symbols to use
     *  @param palette the ConverterPalette (unused)
     */
    public ClassicI18n(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
    	super(ofr,config,palette);
        // We don't need the palette and the office reader is only used to
        // identify the default language

        // Set up table for iso->babel translation
        prepareBabelLanguages();
        
        nDefaultFontenc = getFontenc(sDefaultLanguage);
		
        // Unicode stuff
        ucparser = new UnicodeStringParser();

        String sSymbols="ascii"; // always load common symbols
        if (config.getInputencoding()!=ASCII) {
            sSymbols+="|"+writeInputenc(config.getInputencoding());
        }

        if (config.useWasysym()) sSymbols+="|wasysym";
        if (config.useBbding()) sSymbols+="|bbding";
        if (config.useIfsym()) sSymbols+="|ifsym";
        if (config.usePifont()) sSymbols+="|dingbats";
        if (config.useEurosym()) sSymbols+="|eurosym";
        if (config.useTipa()) sSymbols+="|tipa";

        tableSet = new Hashtable();
        UnicodeTableHandler handler=new UnicodeTableHandler(tableSet, sSymbols);
        SAXParserFactory factory=SAXParserFactory.newInstance();
        InputStream is = this.getClass().getResourceAsStream("symbols.xml");
        try {
            SAXParser saxParser=factory.newSAXParser();
            saxParser.parse(is,handler);
        }
        catch (Throwable t){
		    System.err.println("Oops - Unable to read symbols.xml");
            t.printStackTrace();
        }
        // put root table at top of stack
        tableStack = new Stack();
        tableStack.push((UnicodeTable) tableSet.get("root"));
        table = (UnicodeTable) tableSet.get("root");
    }
	
    /** Construct a new I18n for general use
     *  @param config the configuration which determines the symbols to use
     */
    public ClassicI18n(LaTeXConfig config) {
        this (null, config, null);
    }

    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
   		pack.append("\\usepackage[")
   			.append(writeInputenc(config.getInputencoding()))
            .append("]{inputenc}").nl();

        // usepackage fontenc
        CSVList fontencs = new CSVList(',');
        if (bT2A) { fontencs.addValue("T2A"); }
        if (bGreek) { fontencs.addValue("LGR"); }
        if (config.useTipa()) { fontencs.addValue("T3"); }
        fontencs.addValue("T1");
        pack.append("\\usepackage[").append(fontencs.toString())
            .append("]{fontenc}").nl();

        // usepackage babel
		
        // If the document contains "anonymous" greek letters we need greek in any case
        // If the document contains "anonymous cyrillic letters we need one of the
        // languages russian, ukrainian or bulgarian
        if (greek() && !languages.contains("el")) languages.add("el");
        if (cyrillic() && !(languages.contains("ru") || languages.contains("uk") || languages.contains("bg"))) {
            languages.add("ru");
        } 

        // Load babel with the used languages
        CSVList babelopt = new CSVList(",");		
        Iterator langiter = languages.iterator();
        while (langiter.hasNext()) {
            String sLang = (String) langiter.next();
            if (!sLang.equals(sDefaultLanguage)) {
                if ("el".equals(sLang) && this.polytonicGreek()) {
                    babelopt.addValue("polutonikogreek");
                }
                else {
                    babelopt.addValue(getBabelLanguage(sLang));
                }
            }
        }

        // The default language must be the last one
        if (sDefaultLanguage!=null) {
            if ("el".equals(sDefaultLanguage) && this.polytonicGreek()) {
                babelopt.addValue("polutonikogreek");
            }
            else {
                babelopt.addValue(getBabelLanguage(sDefaultLanguage));
            }
        }

        if (!babelopt.isEmpty()) {
            pack.append("\\usepackage[")
                .append(babelopt.toString())
                .append("]{babel}").nl();
            // For Polish we must undefine \lll which is later defined by ams
            if (languages.contains("pl")) { 
            	pack.append("\\let\\lll\\undefined").nl();
            }
        }
			
        // usepackage tipa
        if (config.useTipa()) {
            pack.append("\\usepackage[noenc]{tipa}").nl()
                .append("\\usepackage{tipx}").nl();
        }

        // usepackage bbding (Has to avoid some nameclashes.)
        if (config.useBbding()) {
            pack.append("\\usepackage{bbding}").nl()
                .append("\\let\\bbCross\\Cross\\let\\Cross\\undefined").nl()
                .append("\\let\\bbSquare\\Square\\let\\Square\\undefined").nl()
                .append("\\let\\bbTrianbleUp\\TriangleUp\\let\\TriangleUp\\undefined").nl()
                .append("\\let\\bbTrianlgeDown\\TriangleDown\\let\\TriangleDown\\undefined").nl();
        }
		
        // usepackage ifsym
        if (config.useIfsym()) {
            pack.append("\\usepackage[geometry,weather,misc,clock]{ifsym}").nl();
        }

        // usepackage pifont
        if (config.usePifont()) { pack.append("\\usepackage{pifont}").nl(); }

        // usepackage eurosym
        if (config.useEurosym()) { pack.append("\\usepackage{eurosym}").nl(); }

        // usepackage amsmath (always!)
        pack.append("\\usepackage{amsmath}").nl();

        // usepackage wasysym (*must* be loaded between amsmath and amsfonts!)
        if (config.useWasysym()) { 
            pack.append("\\usepackage{wasysym}").nl();
        }
		
        // usepackage amssymb, amsfonts, textcomp (always!)
        pack.append("\\usepackage{amssymb,amsfonts,textcomp}").nl();
		
    }
	
    /** Apply a language language
     *  @param style the OOo style to read attributesfrom
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba) {
        if (!bAlwaysUseDefaultLang && style!=null) {
            String sISOLang = style.getProperty(XMLString.FO_LANGUAGE,bInherit);
            if (sISOLang!=null) {
                languages.add(sISOLang);
                String sLang = getBabelLanguage(sISOLang);
                if (sLang!=null) {
                    if (bDecl) {
                        ba.add("\\selectlanguage{"+sLang+"}","");
                        //ba.add("\\begin{otherlanguage}{"+sLang+"}","\\end{otherlanguage}");
                    } 
                    else {
                        ba.add("\\foreignlanguage{"+sLang+"}{","}");
                    }
                }
            }
        }
    }

    /** Push a font to the font stack
     *  @param sName the name of the font
     */
    public void pushSpecialTable(String sName) {
        // If no name is specified we should keep the current table
        // Otherwise try to find the table, and use root if it's not available
        if (sName!=null) {
            table = (UnicodeTable) tableSet.get(sName);
            if (table==null) { table = (UnicodeTable) tableSet.get("root"); }
        }
        tableStack.push(table);
    }
	
    /** Pop a font from the font stack
     */
    public void popSpecialTable() {
        tableStack.pop();
        table = (UnicodeTable) tableStack.peek();
    }

    /** Get the number of characters defined in the current table
     *  (for informational purposes only)
     *  @return the number of characters
     */
    public int getCharCount() { return table.getCharCount(); }
	
    /** Convert a string of characters into LaTeX
     *  @param s the source string
     *  @param bMathMode true if the string should be rendered in math mode
     *  @param sLang the iso language of the string
     *  @return the LaTeX string
     */
    public String convert(String s, boolean bMathMode, String sLang){
        if (!bAlwaysUseDefaultLang && sLang!=null) { languages.add(sLang); }
        StringBuffer buf=new StringBuffer();
        int nFontenc = bAlwaysUseDefaultLang ? nDefaultFontenc : getFontenc(sLang);
        int nLen = s.length();
        int i = 0;
        int nStart = i;
        while (i<nLen) {
            ReplacementTrieNode node = stringReplace.get(s,i,nLen);
            if (node!=null) {
                if (i>nStart) {
                    convert(s,nStart,i,bMathMode,sLang,buf,nFontenc);
                }
                boolean bOtherFontenc = !supportsFontenc(node.getFontencs(),nFontenc);
                if (bOtherFontenc) {
                    buf.append(getFontencCs(getFontenc(node.getFontencs()))).append("{");
                }
                buf.append(node.getLaTeXCode());
                if (bOtherFontenc) {
                    buf.append("}");
                }
                i += node.getInputLength();
                nStart = i;
            }
            else {
                i++;
            }
        }
        if (nStart<nLen) {
            convert(s,nStart,nLen,bMathMode,sLang,buf,nFontenc);
        }
        return buf.toString();
    }
	
    private void convert(String s, int nStart, int nEnd, boolean bMathMode, String sLang, StringBuffer buf, int nFontenc) {
        int nCurFontenc = nFontenc;
        ucparser.reset(table,s,nStart,nEnd);
        boolean bProtectDashes = true;
        boolean bTempMathMode = false;
        while (ucparser.next()) {
            char c = ucparser.getChar();
            if (bMathMode) {
                buf.append(convertMathChar(c,nFontenc));
            }
            else if (greekMath(c,nFontenc) || (table.hasMathChar(c) && !table.hasTextChar(c))) {
                if (!bTempMathMode) { // switch to math mode
                    buf.append("$");
                    bTempMathMode = true;
                }
                buf.append(convertMathChar(c,nFontenc));
                bProtectDashes = false;
            }
            else if (table.hasTextChar(c)) {
                if (bTempMathMode) { // switch to text mode
                    buf.append("$");
                    bTempMathMode = false;
                }
                int nFontencs = table.getFontencs(c);
                if (supportsFontenc(nFontencs,nCurFontenc)) {
                    // The text character is valid in the current font encoding
                    // Note: Change of font encoding is greedy - change?

                    // Prevent unwanted --- ligatures
                    if (table.isDashes(c)) {
                        if (bProtectDashes) { buf.append("{}"); }
                        bProtectDashes = true;
                    }
                    else {
                        bProtectDashes = false;
                    }

                    setFlags(c,nCurFontenc);
                    if (ucparser.hasCombiningChar()) {
                        char cc = ucparser.getCombiningChar();
                        if (supportsFontenc(table.getFontencs(cc),nCurFontenc)) {
                            buf.append(table.getTextChar(cc)).append("{")
                               .append(table.getTextChar(c)).append("}");
                        }
                        else { // ignore combining char if not valid in this font encoding
                            buf.append(table.getTextChar(c));
                        }
                    }
                    else {
                        buf.append(table.getTextChar(c));
                    }
                }
                else {
                    // The text character is valid in another font encoding
					
                    bProtectDashes = table.isDashes(c);

                    int nFontenc1 = getFontenc(nFontencs);
                    setFlags(c,nFontenc1);
                    if (nCurFontenc!=nFontenc) { // end "other font encoding"
                        buf.append("}");
                    }
                    if (nFontenc1!=nFontenc) { // start "other font encoding"
                        buf.append(getFontencCs(nFontenc1)).append("{");
                    }

                    if (ucparser.hasCombiningChar()) {
                        char cc = ucparser.getCombiningChar();
                        if (supportsFontenc(table.getFontencs(cc),nCurFontenc)) {
                            buf.append(table.getTextChar(cc)).append("{")
                               .append(table.getTextChar(c)).append("}");
                        }
                        else { // ignore combining char if not valid in this font encoding
                            buf.append(table.getTextChar(c));
                        }
                    }
                    else {
                        buf.append(table.getTextChar(c));
                    }
                    nCurFontenc = nFontenc1;
                }
            }
            else {
                buf.append(notFound(c,nCurFontenc));
            }
        }
		
        if (bTempMathMode) { // turn of math mode
            buf.append("$");
        }

        if (nCurFontenc!=nFontenc) { // end unfinished "other font encoding"
            buf.append("}");
        }

    }

    // convert a single math character
    private String convertMathChar(char c, int nFontenc) {
        if (table.hasMathChar(c)) {
            return table.getMathChar(c);
        }
        else if (table.hasTextChar(c)) { // use text mode as a fallback
            int nFontencs = table.getFontencs(c);
            if (supportsFontenc(nFontencs,nFontenc)) {
                // The text character is valid in the current font encoding
                setFlags(c,nFontenc);
                if (table.getCharType(c)==UnicodeCharacter.COMBINING) {
                    return "\\text{" + table.getTextChar(c) +"{}}";
                }
                else {
                    return "\\text{" + table.getTextChar(c) +"}";
                }
            }
            else {
                // The text character is valid in another font encoding
                int nFontenc1 = getFontenc(nFontencs);
                setFlags(c,nFontenc1);
                if (table.getCharType(c)==UnicodeCharacter.COMBINING) {
                    return "\\text{" + getFontencCs(nFontenc1) + "{" + table.getTextChar(c) +"{}}}";
                }
                else {
                    return "\\text{" + getFontencCs(nFontenc1) + "{" + table.getTextChar(c) +"}}";
                }
            }
        }
        else {
            return "\\text{" + notFound(c,nFontenc) + "}";
        }
    }
	
    // Missing symbol
    private String notFound(char c,int nFontenc) {
        //String sErrorMsg = "[Warning: Missing symbol " + Integer.toHexString(c).toUpperCase() +"]";
        String sErrorMsg = "["+Integer.toHexString(c).toUpperCase() +"?]";		
        if (nFontenc==T1_ENC) return sErrorMsg;
        else return "\\textlatin{"+sErrorMsg+"}"; 
    }

	
    // Convert a single character	
    /*private String convert(char c, boolean bMathMode, String sLang){
        int nFontenc = bAlwaysUseDefaultLang ? nDefaultFontenc : getFontenc(sLang);
        if (bMathMode) {
            return convertMathChar(c,nFontenc);
        }
        else if (greekMath(c,nFontenc) || (table.hasMathChar(c) && !table.hasTextChar(c))) {
            return "$" + convertMathChar(c,nFontenc) + "$";
        }
        else if (table.hasTextChar(c)) {
            int nFontencs = table.getFontencs(c);
            if (supportsFontenc(nFontencs,nFontenc)) {
                // The text character is valid in the current font encoding
                setFlags(c,nFontenc);
                if (table.getCharType(c)==UnicodeCharacter.COMBINING) {
                    return table.getTextChar(c)+"{}";
                }
                else {
                    return table.getTextChar(c);
                }
            }
            else {
                // The text character is valid in another font encoding
                int nFontenc1 = getFontenc(nFontencs);
                setFlags(c,nFontenc1);
                if (table.getCharType(c)==UnicodeCharacter.COMBINING) {
                    return getFontencCs(nFontenc1) + "{" + table.getTextChar(c) +"{}}";
                }
                else {
                    return getFontencCs(nFontenc1) + "{" + table.getTextChar(c) +"}";
                }
            }
        }
        else {
            return notFound(c,nFontenc);
        }
    }*/

	
	
    // **** Languages ****
	
    // Convert iso language to babel language
    // todo: include iso country
    // todo: support automatic choice of inputenc (see comments)?
    private String getBabelLanguage(String sLang) {
        if (babelLanguages.containsKey(sLang)) {
            return (String) babelLanguages.get(sLang);
        }
        else {
            return "english"; // interpret unknown languages as English
        }
    }
	
    private void prepareBabelLanguages() {
        babelLanguages = new Hashtable();
        babelLanguages.put("en", "english"); // latin1
        babelLanguages.put("bg", "bulgarian"); // cp1251?
        babelLanguages.put("cs", "czech"); // latin2
        babelLanguages.put("da", "danish"); // latin1
        babelLanguages.put("de", "ngerman"); // latin1
        babelLanguages.put("el", "greek"); // iso-8859-7
        babelLanguages.put("es", "spanish"); // latin1
        babelLanguages.put("fi", "finnish"); // latin1 (latin9?)
        babelLanguages.put("fr", "french"); // latin1 (latin9?)
        babelLanguages.put("ga", "irish"); // latin1
        babelLanguages.put("hr", "croatian"); // latin2
        babelLanguages.put("hu", "magyar"); // latin2
        babelLanguages.put("la", "latin"); // ascii
        babelLanguages.put("is", "icelandic"); // latin1
        babelLanguages.put("it", "italian"); // latin1
        babelLanguages.put("nl", "dutch"); // latin1
        babelLanguages.put("no", "norsk"); // latin1
        babelLanguages.put("pl", "polish"); // latin2
        babelLanguages.put("pt", "portuges"); // latin1
        babelLanguages.put("ro", "romanian"); // latin2
        babelLanguages.put("ru", "russian"); // cp1251?
        babelLanguages.put("sk", "slovak"); // latin2
        babelLanguages.put("sl", "slovene"); // latin2
        babelLanguages.put("sr", "serbian"); // cp1251?
        babelLanguages.put("sv", "swedish"); // latin1
        babelLanguages.put("tr", "turkish");
        babelLanguages.put("uk", "ukrainian"); // cp1251?
    }
	
    // **** Helpers to collect various information ****

    // Did we use cyrillic?
    private boolean cyrillic() { return bT2A; }
	
    // Did we use greek?
    private boolean greek() { return bGreek; }
	
    // Did we use polytonic greek?
    private boolean polytonicGreek() { return bPolytonicGreek; }

    // Outside greek text, greek letters may be rendered in math mode,
    // if the user requires that in the configuration.
    private boolean greekMath(char c, int nFontenc) {
        return bGreekMath && nFontenc!=LGR_ENC && table.getFontencs(c)==LGR_ENC;
    }
	
    // Set cyrillic and greek flags
    private void setFlags(char c, int nFontenc) {
        if ((c>='\u1F00') && (c<='\u1FFF')) bPolytonicGreek = true;
        if (nFontenc==LGR_ENC) bGreek = true;
        if (nFontenc==T2A_ENC) bT2A = true;
    }

}
