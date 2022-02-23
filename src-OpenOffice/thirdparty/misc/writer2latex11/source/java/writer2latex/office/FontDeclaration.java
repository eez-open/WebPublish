/************************************************************************
 *
 *  FontDeclaration.java
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
 *  Copyright: 2002-2005 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 0.5 (2005-10-10)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;

/** <p> Class representing a font declaration in OOo</p> 
  */
public class FontDeclaration extends OfficeStyle {
    private PropertySet properties = new PropertySet();
	
    private String sFontFamily = null;
    private String sFontFamilyGeneric = null;
    private String sFontPitch = null;

    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        properties.loadFromDOM(node);
        sFontFamily = properties.getProperty(XMLString.FO_FONT_FAMILY);
        if (sFontFamily==null) { // oasis
            sFontFamily = properties.getProperty(XMLString.SVG_FONT_FAMILY);
        }
        sFontFamilyGeneric = properties.getProperty(XMLString.STYLE_FONT_FAMILY_GENERIC);
        sFontPitch = properties.getProperty(XMLString.STYLE_FONT_PITCH);
    }
	
    public String getProperty(String sProperty){ 
        return properties.getProperty(sProperty);
    }    
	
    public String getFontFamily() { return sFontFamily; }

    public String getFontFamilyGeneric() { return sFontFamilyGeneric; }

    public String getFontPitch() { return sFontPitch; }

}
