/************************************************************************
 *
 *  Option.java
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

package writer2latex.base;

// The mother of all options; reads and writes string values
public class Option {
    protected String sValue;
    private String sName;
	
    public void setString(String sValue) { this.sValue = sValue; }

    public String getString() { return sValue; }
	
    public String getName() { return sName; }
	
    public Option(String sName, String sDefaultValue) {
       this.sName = sName;
       setString(sDefaultValue);
    }	
} 

