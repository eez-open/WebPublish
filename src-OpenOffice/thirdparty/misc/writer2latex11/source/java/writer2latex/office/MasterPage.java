/************************************************************************
 *
 *  MasterPage.java
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
import writer2latex.util.Misc;

/** <p> Class representing a master page in OOo Writer </p> */
public class MasterPage extends OfficeStyle {
    private PropertySet properties = new PropertySet();
    private Node header = null;
    private Node headerLeft = null;
    private Node footer = null;
    private Node footerLeft = null;
	
    public String getProperty(String sPropName) {
        return properties.getProperty(sPropName);
    }
	
    public String getPageLayoutName() {
        String sName = properties.getProperty(XMLString.STYLE_PAGE_MASTER_NAME);
        if (sName==null) { // try oasis format
            sName = properties.getProperty(XMLString.STYLE_PAGE_LAYOUT_NAME);
        }
        return sName;
    }
    
    public Node getHeader() { return header; }
    public Node getHeaderLeft() { return headerLeft; }
    public Node getFooter() { return footer; }
    public Node getFooterLeft() { return footerLeft; }
	
    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        properties.loadFromDOM(node);
        header = Misc.getChildByTagName(node,XMLString.STYLE_HEADER);
        headerLeft = Misc.getChildByTagName(node,XMLString.STYLE_HEADER_LEFT);
        footer = Misc.getChildByTagName(node,XMLString.STYLE_FOOTER);
        footerLeft = Misc.getChildByTagName(node,XMLString.STYLE_FOOTER_LEFT);
    }
	
}