/************************************************************************
 *
 *  ContentHandlingOption.java
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

package writer2latex.latex;

import writer2latex.base.IntegerOption;

public class ContentHandlingOption extends IntegerOption {
    public ContentHandlingOption(String sName, String sDefaultValue) {
        super(sName,sDefaultValue);
    }	

    public void setString(String sValue) {
        super.setString(sValue);
        if ("accept".equals(sValue)) nValue = LaTeXConfig.ACCEPT;
        else if ("ignore".equals(sValue)) nValue = LaTeXConfig.IGNORE;
        else if ("warning".equals(sValue)) nValue = LaTeXConfig.WARNING;
        else if ("error".equals(sValue)) nValue = LaTeXConfig.ERROR;
    }
}
