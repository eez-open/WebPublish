/************************************************************************
 *
 *  XhtmlFormatOption.java
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

import writer2latex.base.IntegerOption;

class XhtmlFormatOption extends IntegerOption {
    public XhtmlFormatOption(String sName, String sDefaultValue) {
        super(sName,sDefaultValue);
    }	

    public void setString(String sValue) {
        super.setString(sValue);
        if ("ignore_styles".equals(sValue)) nValue = XhtmlConfig.IGNORE_STYLES;
        else if ("ignore_hard".equals(sValue)) nValue = XhtmlConfig.IGNORE_HARD;
        else if ("ignore_all".equals(sValue)) nValue = XhtmlConfig.IGNORE_ALL;
        else nValue = XhtmlConfig.CONVERT_ALL;
    }
}
