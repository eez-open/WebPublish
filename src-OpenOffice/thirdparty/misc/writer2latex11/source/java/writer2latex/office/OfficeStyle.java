/************************************************************************
 *
 *  OfficeStyle.java
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
 *  Copyright: 2002-2006 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 0.5 (2006-11-23)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;
import writer2latex.util.Misc;

/** <p> Abstract class representing a style in OOo </p> */
public abstract class OfficeStyle {
    // These attributes are defined by OfficeStyleFamily upon collection of styles
    protected String sName;
    protected OfficeStyleFamily family;
    protected boolean bAutomatic;

    private String sDisplayName;
    private String sParentName;
    private String sListStyleName; 
    private String sMasterPageName;

    public String getName() { return sName; }

    public OfficeStyleFamily getFamily() { return family; }

    public boolean isAutomatic() { return bAutomatic; }
	
    public String getDisplayName() { return sDisplayName; }

    public String getParentName() { return sParentName; }
	
    public String getListStyleName() { return sListStyleName; }

    public String getMasterPageName() { return sMasterPageName; }
	
    public void loadStyleFromDOM(Node node){
        sDisplayName = Misc.getAttribute(node,XMLString.STYLE_DISPLAY_NAME);
        if (sDisplayName==null) { sDisplayName = sName; }
        sParentName = Misc.getAttribute(node,XMLString.STYLE_PARENT_STYLE_NAME);
        sListStyleName = Misc.getAttribute(node,XMLString.STYLE_LIST_STYLE_NAME);
        sMasterPageName = Misc.getAttribute(node,XMLString.STYLE_MASTER_PAGE_NAME);
    }
	
}