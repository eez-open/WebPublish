/************************************************************************
 *
 *  TextStyleConverter.java
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

import java.util.Enumeration;
import java.util.Hashtable;

import writer2latex.office.FontDeclaration;
import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.ExportNameCollection;
import writer2latex.util.Misc;

/**
 * This class converts OpenDocument text styles to CSS2 styles.
 * This includes conversion of text properties in other styles
 * (paragraph, cell, graphic and presentation styles).
 * <ul><li>TODO: Support CJK and CTL</li> 
 * <li>TODO: Support style:use-window-font-color ("automatic color")</li>
 * <li>TODO: Support style:font-charset (other encoding)</li>
 * <li>TODO: Support style:font-size-rel</li>
 * <li>TODO: Support text:display and text:condition</li></ul>

 */
public class TextStyleConverter extends StyleWithPropertiesConverterHelper {

    // OpenDocument does *not* define the style for links without style name,
    // but OOo uses these styles, and so do we if they are available
    // (Caveat: OOo does not export "Visited Internet Link" until a link is actually clicked)
    private static final String DEFAULT_LINK_STYLE = "Internet link"; // Not "Link"!
    private static final String DEFAULT_VISITED_LINK_STYLE = "Visited Internet Link";

    // Bookkeeping for anchors
    private ExportNameCollection anchorStyleNames = new ExportNameCollection(true);
    private ExportNameCollection anchorVisitedStyleNames = new ExportNameCollection(true);
    private Hashtable anchorCombinedStyleNames = new Hashtable();
    private Hashtable orgAnchorStyleNames = new Hashtable();
    private Hashtable orgAnchorVisitedStyleNames = new Hashtable();

    /** Create a new <code>TextStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public TextStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.styleMap = config.getXTextStyleMap();
        this.bConvertStyles = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES;
    }

    /** Apply a link style, using a combination of two text styles
     *  @param sStyleName name of the OpenDocument style
     *  @param sVisitedStyleName name of the OpenDocument style for visited links
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    public void applyAnchorStyle(String sStyleName, String sVisitedStyleName,
        StyleInfo info) {
        if (sStyleName==null || sVisitedStyleName==null) { return; }
        if (sStyleName.length()==0 || sVisitedStyleName.length()==0) { return; }
        // Look for a style map
        String sDisplayName = ofr.getTextStyles().getDisplayName(sStyleName);
        if (styleMap.contains(sDisplayName)) { // class name from config
            if (!"(none)".equals(styleMap.getCss(sDisplayName))) {
                info.sClass = styleMap.getCss(sDisplayName);
            }
            return;
        }

        String sName = sStyleName+sVisitedStyleName;
        if (!anchorCombinedStyleNames.containsKey(sName)) {
            String sExportName;
            // This combination is not seen before, but the base style may be known
            // In that case, use the visited style name as well
            if (anchorStyleNames.containsName(sStyleName)) {
                sExportName = anchorStyleNames.getExportName(sStyleName)
                              +anchorVisitedStyleNames.getExportName(sVisitedStyleName);
            }
            else {
                sExportName = anchorStyleNames.getExportName(sStyleName);
            }
            anchorCombinedStyleNames.put(sName,sExportName);
            orgAnchorStyleNames.put(sExportName,sStyleName);
            orgAnchorVisitedStyleNames.put(sExportName,sVisitedStyleName);
        }
        info.sClass = (String)anchorCombinedStyleNames.get(sName);
    }
	
    /** <p>Convert style information for used styles</p>
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        StringBuffer buf = new StringBuffer();
        buf.append(super.getStyleDeclarations(sIndent));
        if (bConvertStyles) {
            // Export anchor styles
            // Default is always the styles "Internet link" and "Visited Internet Link"(?) 
            StyleWithProperties defaultLinkStyle = (StyleWithProperties)
                getStyles().getStyleByDisplayName(DEFAULT_LINK_STYLE);
            if (defaultLinkStyle!=null) {
                CSVList props = new CSVList(";");
                cssText(defaultLinkStyle,props,true);
                cssHyperlink(defaultLinkStyle,props);
                buf.append(sIndent)
                   .append("a:link {").append(props.toString()).append("}\n");
            }
		
            defaultLinkStyle = (StyleWithProperties)
                getStyles().getStyleByDisplayName(DEFAULT_VISITED_LINK_STYLE);
            if (defaultLinkStyle!=null) {
                CSVList props = new CSVList(";");
                cssText(defaultLinkStyle,props,true);
                cssHyperlink(defaultLinkStyle,props);
                buf.append(sIndent)
                   .append("a:visited {").append(props.toString()).append("}\n");
            }

            // Remaining link styles...
            Enumeration enumer = anchorCombinedStyleNames.elements();
            while (enumer.hasMoreElements()) {
                String sExportName = (String) enumer.nextElement();
                String sStyleName = (String) orgAnchorStyleNames.get(sExportName);
                String sVisitedStyleName = (String) orgAnchorVisitedStyleNames.get(sExportName);

                StyleWithProperties style = ofr.getTextStyle(sStyleName);

                if (style!=null) {
                    CSVList props = new CSVList(";");
                    cssText(style,props,true);
                    cssHyperlink(style,props);
                    buf.append(sIndent).append("a.").append(sExportName)
                       .append(":link {").append(props.toString()).append("}\n");
                }
			
                style = ofr.getTextStyle(sVisitedStyleName);
                if (style!=null) {
                    CSVList props = new CSVList(";");
                    cssText(style,props,true);
                    cssHyperlink(style,props);
                    buf.append(sIndent).append("a.").append(sExportName)
                       .append(":visited {").append(props.toString()).append("}\n");
                }
            }
        }
        return buf.toString();

    }

    /** Get the family of text (character) styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getTextStyles();
    }
	
    /** Create default tag name to represent a text
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "span";
    }
	
    /** Convert formatting properties for a specific text style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssText(style,props,bInherit);
    }
	
    // Methods to query individual formatting properties (no inheritance)
	
    // Does this style contain the bold attribute?
    public boolean isBold(StyleWithProperties style) {
        String s = style.getProperty(XMLString.FO_FONT_WEIGHT,false);
        return s!=null && "bold".equals(s);
    }

    // Does this style contain the italics/oblique attribute?
    public boolean isItalics(StyleWithProperties style) {
        String s = style.getProperty(XMLString.FO_FONT_STYLE,false);
        return s!=null && !"normal".equals(s);
    }
	
    // Does this style contain a fixed pitch font?
    public boolean isFixed(StyleWithProperties style) {
        String s = style.getProperty(XMLString.STYLE_FONT_NAME,false);
        String s2 = null;
        String s3 = null;
        if (s!=null) {
            FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(s);
            if (fd!=null) {
                s2 = fd.getFontFamilyGeneric();
                s3 = fd.getFontPitch();
            }
        }
        else {            
            s = style.getProperty(XMLString.FO_FONT_FAMILY,false);
            s2 = style.getProperty(XMLString.STYLE_FONT_FAMILY_GENERIC,false);
            s3 = style.getProperty(XMLString.STYLE_FONT_PITCH,false);
        }
        if ("fixed".equals(s3)) { return true; }
        if ("modern".equals(s2)) { return true; }
        return false;
    }

    // Does this style specify superscript?
    public boolean isSuperscript(StyleWithProperties style) {
        String sPos = style.getProperty(XMLString.STYLE_TEXT_POSITION,false);
        if (sPos==null) return false;
        if (sPos.startsWith("sub")) return false;
        if (sPos.startsWith("-")) return false;
        return true;
    }

    // Does this style specify subscript?
    public boolean isSubscript(StyleWithProperties style) {
        String sPos = style.getProperty(XMLString.STYLE_TEXT_POSITION,false);
        if (sPos==null) return false;
        if (sPos.startsWith("sub")) return true;
        if (sPos.startsWith("-")) return true;
        return false;
    }
	
    ////////////////////////////////////////////////////////////////////////////
    // OpenDocument text properties
    // Text properties can be applied to text, paragraph, cell, graphic and
    // presentation styles.
    // Language and country attributes are handled elsewhere
    // The following attributes are currently not supported:
    //   - style:use-window-font-color ("automatic color")
    //   - style:font-charset (other encoding)
    //   - style:font-size-rel
    //   - text:display
    //   - text:condition
    // Also all attributes for CJK and CTL text are currently ignored:
    //   style:font-name-*, style:font-family-*, style:font-family-generic-*,
    //   style:font-style-name-*, style:font-pitch-*, style:font-charset-*,
    //   style:font-size-*, style:font-size-rel-*, style:script-type 
    // The following attributes cannot be supported using CSS2:
    //   - style:text-outline
    //   - style:font-relief
    //   - style:text-line-trough-* (formatting of line through)
    //   - style:text-underline-* (formatting of underline)
    //   - style:letter-kerning 
    //   - style:text-combine-*
    //   - style:text-emphasis
    //   - style:text-scale
    //   - style:text-rotation-*
    //   - fo:hyphenate
    //   - fo:hyphenation-*
    //   

    public void cssText(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssTextCommon(style,props,bInherit);
        cssTextBackground(style,props,bInherit);
    }
	
    public void cssTextCommon(StyleWithProperties style, CSVList props, boolean bInherit) {
        String s=null,s2=null,s3=null,s4=null;
        CSVList val;
		
        // Font family
        if (bInherit || style.getProperty(XMLString.STYLE_FONT_NAME,false)!=null) {
            val = new CSVList(","); // multivalue property!
            // Get font family information from font declaration or from style
            s = style.getProperty(XMLString.STYLE_FONT_NAME);
            if (s!=null) {
                FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(s);
                if (fd!=null) {
                    s = fd.getFontFamily();
                    s2 = fd.getFontFamilyGeneric();
                    s3 = fd.getFontPitch();
                }
            }
            else {            
                s = style.getProperty(XMLString.FO_FONT_FAMILY);
                s2 = style.getProperty(XMLString.STYLE_FONT_FAMILY_GENERIC);
                s3 = style.getProperty(XMLString.STYLE_FONT_PITCH);
            }
   		
            // Add the western font family (CJK and CTL is more complicated)
            if (s!=null) { val.addValue(s); }
            // Add generic font family
            if ("fixed".equals(s3)) { val.addValue("monospace"); }
            else if ("roman".equals(s2)) { val.addValue("serif"); }
            else if ("swiss".equals(s2)) { val.addValue("sans-serif"); }
            else if ("modern".equals(s2)) { val.addValue("monospace"); }
            else if ("decorative".equals(s2)) { val.addValue("fantasy"); }
            else if ("script".equals(s2)) { val.addValue("cursive"); }
            else if ("system".equals(s2)) { val.addValue("serif"); } // System default font
            if (!val.isEmpty()) { props.addValue("font-family",val.toString()); }
        }
		
        // Font style (italics): This property fit with css2
        s = style.getProperty(XMLString.FO_FONT_STYLE,bInherit);
	    if (s!=null) { props.addValue("font-style",s); }
	  
        // Font variant (small caps): This property fit with css2
        s = style.getProperty(XMLString.FO_FONT_VARIANT,bInherit);
        if (s!=null) { props.addValue("font-variant",s); }
	    
        // Font weight (bold): This property fit with css2
        s = style.getProperty(XMLString.FO_FONT_WEIGHT,bInherit);
        if (s!=null) { props.addValue("font-weight",s); }
 
        // Font size: Absolute values of this property fit with css2
        // this is handled together with sub- and superscripts (style:text-position)
        // First value: sub, super or percentage (raise/lower relative to font height)
        // Second value (optional): percentage (relative size);
        if (bInherit || style.getProperty(XMLString.FO_FONT_SIZE,false)!=null
                     || style.getProperty(XMLString.STYLE_TEXT_POSITION,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_FONT_SIZE);
            s2 = style.getProperty(XMLString.STYLE_TEXT_POSITION);
	        if (s2!=null) {
                s2 = s2.trim();
                int i = s2.indexOf(" ");
                if (i>0) { // two values
                    s3 = s2.substring(0,i);
                    s4 = s2.substring(i+1);
                } 		
                else { // one value
                    s3 = s2; s4="100%";
                }
                if (s!=null) { props.addValue("font-size",Misc.multiply(s4,scale(s))); }
                else { props.addValue("font-size",s4); }
                props.addValue("vertical-align",s3);
            }
            else if (s!=null) {
                props.addValue("font-size",scale(s));
            }
        }

        // Color: This attribute fit with css2
        s = style.getProperty(XMLString.FO_COLOR,bInherit);
	    if (s!=null) { props.addValue("color",s); }
	  
        // Shadow: This attribute fit with css2
        // (Currently OOo has only one shadow style, which is saved as 1pt 1pt)
        s = style.getProperty(XMLString.FO_TEXT_SHADOW,bInherit);
        if (s!=null) { props.addValue("text-shadow",s); }
	  
        // Text decoration. Here OOo is more flexible that CSS2.
        if (ofr.isOpenDocument()) {
            s = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE,bInherit);
            s2 = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,bInherit);
        }
        else {
            s = style.getProperty(XMLString.STYLE_TEXT_CROSSING_OUT,bInherit);
            s2 = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE,bInherit);
        }
        s3 = style.getProperty(XMLString.STYLE_TEXT_BLINKING,bInherit);
        // Issue: Since these three properties all maps to the single CSS property
        // text-decoration, there is no way to turn on one kind of decoration and 
        // turn another one off (without creating another inline element).
        // If one decoration is turned of, we turn them all off:
        if ("none".equals(s) || "none".equals(s2) || "false".equals(s3)) {
            props.addValue("text-decoration","none");
        }
        else { // set the required properties
            val = new CSVList(" "); // multivalue property!
            if (s!=null && !"none".equals(s)) { val.addValue("line-through"); }
            if (s2!=null && !"none".equals(s2)) { val.addValue("underline"); }
            if (s3!=null && "true".equals(s3)) { val.addValue("blink"); }
            if (!val.isEmpty()) { props.addValue("text-decoration",val.toString()); }  
        }
  
        // Letter spacing: This property fit with css
        s = style.getProperty(XMLString.FO_LETTER_SPACING,bInherit);
	    if (s!=null) { props.addValue("letter-spacing",scale(s)); }
  
        // Capitalization: This property fit with css
        s = style.getProperty(XMLString.FO_TEXT_TRANSFORM,bInherit);
	    if (s!=null) { props.addValue("text-transform",s); }
    }
	
    public void cssTextBackground(StyleWithProperties style, CSVList props, boolean bInherit) {
        // Background color: This attribute fit with css when applied to inline text
        String s =ofr.isOpenDocument() ?
            style.getTextProperty(XMLString.FO_BACKGROUND_COLOR,bInherit) :
            style.getTextProperty(XMLString.STYLE_TEXT_BACKGROUND_COLOR,bInherit);
	    if (s!=null) { props.addValue("background-color",s); }
    }
	
    private void cssHyperlink(StyleWithProperties style, CSVList props) {
        String s1,s2;
        // For hyperlinks, export text-decoration:none even if nothing is defined in source
        if (ofr.isOpenDocument()) {
            s1 = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE,true);
            s2 = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,true);
        }
        else {
            s1 = style.getProperty(XMLString.STYLE_TEXT_CROSSING_OUT,true);
            s2 = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE,true);
        }
        String s3 = style.getProperty(XMLString.STYLE_TEXT_BLINKING,true);
        if (s1==null && s2==null && s3==null) {
            props.addValue("text-decoration","none");
        }
    }

}
