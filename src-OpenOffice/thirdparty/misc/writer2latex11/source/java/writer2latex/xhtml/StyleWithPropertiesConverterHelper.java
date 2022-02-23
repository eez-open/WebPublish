/************************************************************************
 *
 *  StyleWithPropertiesConverterHelper.java
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
//import java.util.Hashtable;

//import writer2latex.latex.util.Info;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.util.CSVList;
//import writer2latex.util.ExportNameCollection;

/**
 * <p>This is an abstract class to convert an OpenDocument style family
 * represented by <code>StyleWithProperties</code> to CSS2 styles.</p>
 */
public abstract class StyleWithPropertiesConverterHelper
    extends StyleConverterHelper {

    /** Create a new <code>StyleWithPropertiesConverterHelper</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public StyleWithPropertiesConverterHelper(OfficeReader ofr, XhtmlConfig config,
        Converter converter, int nType) {
        super(ofr,config,converter,nType);
    }

    /** Apply a style, either by converting the style or by applying the
     *  style map from the configuarion
     *  @param sStyleName name of the OpenDocument style
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    public void applyStyle(String sStyleName, StyleInfo info) {
        StyleWithProperties style = (StyleWithProperties) getStyles().getStyle(sStyleName);
        info.sTagName = getDefaultTagName(style);
        if (style!=null) {
            applyLang(style,info);
            applyDirection(style,info);
            if (style.isAutomatic()) {
                // Apply parent style + hard formatting
                applyStyle(style.getParentName(),info);
                if (bConvertHard) { applyProperties(style,info.props,false); }
            }
            else {
                String sDisplayName = style.getDisplayName();
                if (styleMap.contains(sDisplayName)) {
                    // Apply attributes as specified in style map from user
                    info.sTagName = styleMap.getElement(sDisplayName);
                    if (!"(none)".equals(styleMap.getCss(sDisplayName))) {
                        info.sClass = styleMap.getCss(sDisplayName);
                    }
                }
                else {
                    // Generate class name from display name
                    info.sClass = getClassNamePrefix()
                                  + styleNames.getExportName(sDisplayName);
                }
            }
        }
    }
	
    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        if (bConvertStyles) {
            StringBuffer buf = new StringBuffer();
            Enumeration names = styleNames.keys();
            while (names.hasMoreElements()) {
                String sDisplayName = (String) names.nextElement();
                StyleWithProperties style = (StyleWithProperties)
                    getStyles().getStyleByDisplayName(sDisplayName);
                if (!style.isAutomatic()) {
                    CSVList props = new CSVList(";");
                    applyProperties(style,props,true);
                    buf.append(sIndent);
                    buf.append(getDefaultTagName(null));
                    buf.append(".");
                    buf.append(getClassNamePrefix());
                    buf.append(styleNames.getExportName(sDisplayName));
                    buf.append(" {");
                    buf.append(props.toString());
                    buf.append("}\n");
                    // TODO: Create a method "getStyleDeclarationsInner"
                    // to be used by eg. FrameStyleConverter
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
    public String getClassNamePrefix() { return ""; }

    /** Create default tag name to represent a specific style, e.g.
     *  <code>span</code> (text style) or <code>ul</code> (unordered list)
     *  @param style to use
     *  @return the tag name. If the style is null, a default result should be
     *  returned.
     */
    public abstract String getDefaultTagName(StyleWithProperties style);
	
    /** Convert formatting properties for a specific style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public abstract void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit);
	

}
