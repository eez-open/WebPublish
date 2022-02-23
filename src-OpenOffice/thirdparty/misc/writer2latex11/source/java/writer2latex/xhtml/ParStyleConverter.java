/************************************************************************
 *
 *  ParStyleConverter.java
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
 *  Version 1.0 (2008-09-08)
 *
 */

package writer2latex.xhtml;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;

    /*
    TODO: drop caps (contained in a child of the style:properties element)
    The CSS attributes should be applied to the :first-letter
    pseudo-element or to an additional inline element.
    */


/**
 * This class converts OpenDocument paragraph styles to CSS2 styles for
 * use in paragraphs and headings.
 * This also includes conversion of paragraph properties in other styles
 * (cell styles).
 */
public class ParStyleConverter extends StyleWithPropertiesConverterHelper {

    // Some bookkeeping for headings
    private String[] sHeadingStyles = new String[7];

    /** Create a new <code>ParStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public ParStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.styleMap = config.getXParStyleMap();
        this.bConvertStyles = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES;
    }
	
    // TODO: Remove me, OfficeReader takes care of this
    public void setHeadingStyle(int nLevel, String sStyleName) {
        if (sHeadingStyles[nLevel]==null) {
		    sHeadingStyles[nLevel] = sStyleName;
        }
    }

    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        StringBuffer buf = new StringBuffer();
        buf.append(super.getStyleDeclarations(sIndent));
        if (bConvertStyles) {
            // Styles for headings
            for (int i=1; i<=6; i++) {
                if (sHeadingStyles[i]!=null) {
                    StyleWithProperties style = ofr.getParStyle(sHeadingStyles[i]);
                    if (style!=null) {
                        CSVList props = new CSVList(";");
                        applyProperties(style,props,true);
                        props.addValue("clear","left");
                        buf.append(sIndent).append("h").append(i)
                           .append(" {").append(props.toString()).append("}\n");
                    }
                }
            }
        }
        return buf.toString();
    }

    /** Get the family of paragraph styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getParStyles();
    }
	
    /** Create default tag name to represent a paragraph
     *  @param style to use
     *  @return the tag name. 
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "p";
    }
	
    /** Convert formatting properties for a specific Par style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        getFrameSc().cssMargins(style,props,bInherit);
        getFrameSc().cssBorder(style,props,bInherit);
        getFrameSc().cssPadding(style,props,bInherit);
        getFrameSc().cssBackgroundCommon(style,props,bInherit);
        cssPar(style,props,bInherit);
        getTextSc().cssTextCommon(style,props,bInherit);
    }
	
    public String getTextBackground(String sStyleName) {
        CSVList props = new CSVList(";");
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style!=null) {
            getTextSc().cssTextBackground(style,props,true);
        }
        return props.toString();
    }
	
    // TODO: get rid of this
    public String getRealParStyleName(String sStyleName) {
        if (sStyleName==null) { return sStyleName; }
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style==null || !style.isAutomatic()) { return sStyleName; }
        return style.getParentName();
    }
	
    public void cssPar(StyleWithProperties style, CSVList props, boolean bInherit){
        String s;

        // translates paragraph style properties.
        // The following properties are not supported by CSS2:
        // style:justify-single-word and style:text-align-last

/* problem: 120% times normal makes no sense...
        s = style.getProperty(XMLString.FO_LINE_HEIGHT);
	    if (s!=null && s.equals("normal")) {
            props.addValue("line-height:normal;"; 
        }
        else { // length or percentage
            s = style.getAbsoluteProperty(XMLString.FO_LINE_HEIGHT);
            if (s!=null) { props.addValue("line-height",s); }
        }
		*/
        // TODO: style:line-height-at-least and stype:line-spacing
		
        // Background color fits with css (note: Paragraph property!)
        s = style.getParProperty(XMLString.FO_BACKGROUND_COLOR,bInherit);
        if (s!=null) { props.addValue("background-color",s); }

        // Indentation: Absolute values of this property fit with css...
        if (bInherit || style.getProperty(XMLString.FO_TEXT_INDENT,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_TEXT_INDENT);
	        if (s!=null) { 
                props.addValue("text-indent",scale(s));
            }
            else { // ... but css doesn't have this one
                s = style.getProperty(XMLString.STYLE_AUTO_TEXT_INDENT);
                if ("true".equals(s)) { props.addValue("text-indent","2em"); }
            }
        }
		
        // Alignment: This property fit with css, but two values have different names		
        s = style.getProperty(XMLString.FO_TEXT_ALIGN,bInherit);
        if (s!=null) { // rename two property values:
            if (s.equals("start")) { s="left"; }
            else if (s.equals("end")) { s="right"; }
            props.addValue("text-align",s);
        }
		
        // Wrap (only in table cells, only in spreadsheets):
        if (ofr.isSpreadsheet()) {
            s = style.getProperty(XMLString.FO_WRAP_OPTION,bInherit);
            if ("no-wrap".equals(s)) props.addValue("white-space","nowrap");
            else if ("wrap".equals(s)) props.addValue("white-space","normal");
        }
    }
		

}
