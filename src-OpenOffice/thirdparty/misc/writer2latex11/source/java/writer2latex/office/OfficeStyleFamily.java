/************************************************************************
 *
 *  OfficeStyleFamily.java
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

package writer2latex.office;

import org.w3c.dom.Node;
import java.util.Hashtable;
import java.util.Enumeration;
import writer2latex.util.Misc;

/** Container class representing a style family in OOo */
public class OfficeStyleFamily {
    private Hashtable styles = new Hashtable();
    private Class styleClass;
	
    private Hashtable displayNames = new Hashtable();
	
    private OfficeStyle defaultStyle = null;

    /** Create a new OfficeStyleFamily based on a class
     *  @param styleClass the subclass of OfficeStyle used to represent styles
     *  in this family
     */
    public OfficeStyleFamily(Class styleClass) {
        this.styleClass = styleClass;
    }
	
    /** Define the default style for this family, ie. an unnamed style providing
     *  defaults for some style properties. This style cannot be found using
     *  getStyle or getStyleByDisplayName.
     *  @param style the new default style
     */
    public void setDefaultStyle(OfficeStyle style) {
        defaultStyle = style;
    }
	
    /** Get the default style for this family
     *  @return the default style, or null if none is defined
     */
    public OfficeStyle getDefaultStyle() {
        return defaultStyle;
    }
	
    /** Get a style by name
     *  @param sName the name of the style
     *  @return the style, or null if such a style does not exist
     */
    public OfficeStyle getStyle(String sName) {
        if (sName==null) { return null; }
        else { return (OfficeStyle) styles.get(sName); }
    }
	
    /** Get a style by display name. Automatic styles does not have a display
     *  name, so only common styles can be retrieved with this method
     *  @param sDisplayName the display name of the style
     *  @return the style, or null if such a style does not exist
     */
    public OfficeStyle getStyleByDisplayName(String sDisplayName) {
        if (sDisplayName==null) { return null; }
        else { return getStyle((String) displayNames.get(sDisplayName)); }
    }
	
    /** Get the display name for the style with the specified name.
     *  If this is an automatic style, the parent style is used
     *  @param sName the style name
     *  @return the display name, or null if the style does not exist
     */ 
    public String getDisplayName(String sName) {
        OfficeStyle style = getStyle(sName);
        if (style==null) { return null; }
        if (style.isAutomatic()) {
            style = getStyle(style.getParentName());
            if (style==null) { return null; }
        }
        return style.getDisplayName();
    }

    /** Get all named styles in the family (ie. excluding the default style)
     *  @return an enumeration of all styles represented by OfficeStyle objects
     */
    public Enumeration getStylesEnumeration(){
        return styles.elements();
    }
	
    /** Load a style from a DOM representation
     *  @param node the style:... node representing the style
     *  @param bAutomatic if true, the style is an automatic style
     */
    public void loadStyleFromDOM(Node node, boolean bAutomatic) {
        String sName = Misc.getAttribute(node,XMLString.STYLE_NAME);
        if (sName!=null) {
            try {
                OfficeStyle style = (OfficeStyle) styleClass.newInstance();
                style.sName=sName;
                style.family=this;
                style.bAutomatic=bAutomatic;
                style.loadStyleFromDOM(node);
                styles.put(sName,style);
                if (!bAutomatic) {
                    // Create backlink from display name to name
                    displayNames.put(style.getDisplayName(),sName);
                }
            }
            catch (InstantiationException e) {
                // Will not happen if a proper class is passed in the constructor
            }
            catch (IllegalAccessException e) {
                // Should also not happen
            }
        }
    }
	
	
}