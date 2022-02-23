/************************************************************************
 *
 *  FrameStyleConverter.java
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

package writer2latex.xhtml;

import java.util.Enumeration;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
//import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/**
 * This class converts OpenDocument graphic (frame) styles to CSS2 styles.
 * This includes conversion of frame properties in other styles (paragraph,
 * cell, section, page and presentation styles).
 */
public class FrameStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>FrameStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public FrameStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.styleMap = config.getXFrameStyleMap();
        this.bConvertStyles = config.xhtmlFrameFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFrameFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlFrameFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFrameFormatting()==XhtmlConfig.IGNORE_STYLES;
    }
	
    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        if (bConvertStyles) {
            StringBuffer buf = new StringBuffer();
            buf.append(super.getStyleDeclarations(sIndent));
            Enumeration names = styleNames.keys();
            while (names.hasMoreElements()) {
                String sDisplayName = (String) names.nextElement();
                StyleWithProperties style = (StyleWithProperties)
                    getStyles().getStyleByDisplayName(sDisplayName);
                if (!style.isAutomatic()) {
                    // Apply style to paragraphs contained in this frame
                    CSVList props = new CSVList(";");
                    getFrameSc().cssMargins(style,props,true);
                    getParSc().cssPar(style,props,true);
                    getTextSc().cssTextCommon(style,props,true);
                    if (!props.isEmpty()) {
                        buf.append(sIndent)
                           .append(getDefaultTagName(null))
                           .append(".").append(getClassNamePrefix())
                           .append(styleNames.getExportName(sDisplayName))
                           .append(" p {").append(props.toString()).append("}\n");
                    }
                }
            }
            return buf.toString();
        }
        else {
            return "";
        }
    }

    /** Return a prefix to be used in generated css class names
     *  @return the prefix
     */
    public String getClassNamePrefix() { return "frame"; }

    /** Get the family of frame styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getFrameStyles();
    }
	
    /** Create default tag name to represent a frame
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "";
    }
	
    /** Convert formatting properties for a specific frame style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssBox(style,props,bInherit);
        getTextSc().cssTextCommon(style,props,bInherit); // only in presentations
    }
	
    ////////////////////////////////////////////////////////////////////////////
    // OpenDocument frame properties

    public void cssBox(StyleWithProperties style, CSVList props, boolean bInherit){
        // translates "box" style properties.
        // these can be applied to paragraph styles, frame styles and page styles.
        // The following properties are not supported by CSS2:
        // style:border-line-width and style:border-line-width-*
        // TODO: What about shadow?
		cssMargins(style,props,bInherit);
        cssBorder(style,props,bInherit);
        cssPadding(style,props,bInherit);
        cssBackground(style,props,bInherit);
    }
	
    public void cssMargins(StyleWithProperties style, CSVList props, boolean bInherit){
        // *Absolute* values fit with css
        String s;
        if (bInherit || style.getProperty(XMLString.FO_MARGIN_LEFT,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_MARGIN_LEFT);
            if (s!=null) { props.addValue("margin-left",scale(s)); }
            else { props.addValue("margin-left","0"); }
        }
        if (bInherit || style.getProperty(XMLString.FO_MARGIN_RIGHT,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_MARGIN_RIGHT);
	        if (s!=null) { props.addValue("margin-right",scale(s)); }
            else { props.addValue("margin-right","0"); }
        }
        if (bInherit || style.getProperty(XMLString.FO_MARGIN_TOP,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_MARGIN_TOP);
	        if (s!=null) { props.addValue("margin-top",scale(s)); }
            else { props.addValue("margin-top","0"); }
        }
        if (bInherit || style.getProperty(XMLString.FO_MARGIN_BOTTOM,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_MARGIN_BOTTOM);
	        if (s!=null) { props.addValue("margin-bottom",scale(s)); }
            else { props.addValue("margin-bottom","0"); }
        }
    }
	
    public void cssBorder(StyleWithProperties style, CSVList props, boolean bInherit){
        // Same as in css
        boolean bHasBorder = false;
        String s=null;
        if (bInherit || style.getProperty(XMLString.FO_BORDER,false)!=null) {
            s = style.getProperty(XMLString.FO_BORDER);
        }
        if (s!=null) {
            props.addValue("border",borderScale(s)); bHasBorder = true;
        }
        else { // apply individual borders
            if (bInherit || style.getProperty(XMLString.FO_BORDER_TOP,false)!=null) {
                s = style.getProperty(XMLString.FO_BORDER_TOP);
                if (s!=null) { props.addValue("border-top",borderScale(s)); bHasBorder=true; }
            }
            if (bInherit || style.getProperty(XMLString.FO_BORDER_BOTTOM,false)!=null) {
                s = style.getProperty(XMLString.FO_BORDER_BOTTOM);
                if (s!=null) { props.addValue("border-bottom",borderScale(s)); bHasBorder=true; }
            }
            if (bInherit || style.getProperty(XMLString.FO_BORDER_LEFT,false)!=null) {
                s = style.getProperty(XMLString.FO_BORDER_LEFT);
                if (s!=null) { props.addValue("border-left",borderScale(s)); bHasBorder=true; }
            }
            if (bInherit || style.getProperty(XMLString.FO_BORDER_RIGHT,false)!=null) {
                s = style.getProperty(XMLString.FO_BORDER_RIGHT);
                if (s!=null) { props.addValue("border-right",borderScale(s)); bHasBorder=true; }
            }
        }
        // Default to no border:
        if (bInherit && !bHasBorder) { props.addValue("border","none"); }
    }
	
    public void cssPadding(StyleWithProperties style, CSVList props, boolean bInherit){
        // *Absolute* values fit with css
        String s=null;
        if (bInherit || style.getProperty(XMLString.FO_PADDING,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_PADDING);
        }
        if (s!=null) {
            props.addValue("padding",scale(s));
        }
        else { // apply individual paddings
            boolean bTop = false;
            boolean bBottom = false;
            boolean bLeft = false;
            boolean bRight = false;
            if (bInherit || style.getProperty(XMLString.FO_PADDING_TOP,false)!=null) {
                s = style.getAbsoluteProperty(XMLString.FO_PADDING_TOP);
                if (s!=null) { props.addValue("padding-top",scale(s)); bTop=true; }
            }
            if (bInherit || style.getProperty(XMLString.FO_PADDING_BOTTOM,false)!=null) {
                s = style.getAbsoluteProperty(XMLString.FO_PADDING_BOTTOM);
                if (s!=null) { props.addValue("padding-bottom",scale(s)); bBottom=true; }
            }
            if (bInherit || style.getProperty(XMLString.FO_PADDING_LEFT,false)!=null) {
                s = style.getAbsoluteProperty(XMLString.FO_PADDING_LEFT);
                if (s!=null) { props.addValue("padding-left",scale(s)); bLeft=true; }
            }
            if (bInherit || style.getProperty(XMLString.FO_PADDING_RIGHT,false)!=null) {
                s = style.getAbsoluteProperty(XMLString.FO_PADDING_RIGHT);
                if (s!=null) { props.addValue("padding-right",scale(s)); bRight=true; }
            }
            if (bInherit) { // must specify padding
                if (!bTop && !bBottom && !bLeft && !bRight) {
                    props.addValue("padding","0");
                }
                else {
                    if (!bTop) { props.addValue("padding-top","0"); }
                    if (!bBottom) { props.addValue("padding-bottom","0"); }
                    if (!bLeft) { props.addValue("padding-left","0"); }
                    if (!bRight) { props.addValue("padding-right","0"); }
                }
            }
        }
    }
	
    // parapgrah styles need this for special treatment of background color
    public void cssBackgroundCommon(StyleWithProperties style, CSVList props, boolean bInherit){
        // Background image:
        String sUrl = style.getBackgroundImageProperty(XMLString.XLINK_HREF);
        if (sUrl!=null) { // currently only support for linked image
            props.addValue("background-image","url("+escapeUrl(sUrl)+")");

            String sRepeat = style.getBackgroundImageProperty(XMLString.STYLE_REPEAT);
            if ("no-repeat".equals(sRepeat) || "stretch".equals(sRepeat)) {
                props.addValue("background-repeat","no-repeat");
            }
            else {
                props.addValue("background-repeat","repeat");
            }

            String sPosition = style.getBackgroundImageProperty(XMLString.STYLE_POSITION);
            if (sPosition!=null) { props.addValue("background-position",sPosition); }
        }
    }

    public void cssBackground(StyleWithProperties style, CSVList props, boolean bInherit){
        // Background color: Same as in css
        String s = style.getProperty(XMLString.FO_BACKGROUND_COLOR,bInherit);
        if (s!=null) { props.addValue("background-color",s); }
        cssBackgroundCommon(style,props,bInherit);
    }
	
    // Scale the border with while preserving the rest of the attribute
    public String borderScale(String sBorder) {
        SimpleInputBuffer in = new SimpleInputBuffer(sBorder);
        StringBuffer out = new StringBuffer();
        while (in.peekChar()!='\0') {
            // Skip spaces
            while(in.peekChar()==' ') { out.append(" "); in.getChar(); }
            // If it's a number it must be a unit -> convert it
            if ('0'<=in.peekChar() && in.peekChar()<='9') {
                out.append(scale(in.getNumber()+in.getIdentifier()));
            }
            // skip other characters
            while (in.peekChar()!=' ' && in.peekChar()!='\0') {
                out.append(in.getChar());
            } 
        }
        return out.toString();
    }
	
    // Must escape certain characters in the url property	
    private String escapeUrl(String sUrl) {
        StringBuffer buf = new StringBuffer();
        int nLen = sUrl.length();
        for (int i=0; i<nLen; i++) {
            char c = sUrl.charAt(i);
            if (c=='\'' || c=='"' || c=='(' || c==')' || c==',' || c==' ') {
                buf.append("\\");
            }
            buf.append(c);
        }
        return buf.toString();
    }

}
