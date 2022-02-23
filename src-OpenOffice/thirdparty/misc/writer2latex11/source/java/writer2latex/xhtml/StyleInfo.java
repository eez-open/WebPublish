/************************************************************************
 *
 *  StyleInfo.java
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
 *  Copyright: 2002-2007 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 0.5 (2007-02-27)
 *
 */

package writer2latex.xhtml;

import writer2latex.util.CSVList;

public class StyleInfo {
    public String sTagName = null;
    public String sClass = null;
    public CSVList props = new CSVList(";");
    public String sLang = null;
    public String sDir = null;
	
    public boolean hasAttributes() {
        return !props.isEmpty() || sClass!=null || sLang!=null || sDir!=null;
    }
}
