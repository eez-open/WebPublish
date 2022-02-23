/************************************************************************
 *
 *  PropertyHelper.java
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
 *  Version 1.0 (2008-07-21)
 *
 */
 
package org.openoffice.da.comp.w2lcommon.helper;

import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.star.beans.PropertyValue; 

/** This class provides access by name to a <code>PropertyValue</code> array
 */
public class PropertyHelper {

    private Hashtable data;
	
    public PropertyHelper() {
        data = new Hashtable();
    }

    public PropertyHelper(PropertyValue[] props) {
        data = new Hashtable();
        int nLen = props.length;
        for (int i=0; i<nLen; i++) {
            data.put(props[i].Name,props[i].Value);
        }
    }
	
    public void put(String sName, Object value) {
        data.put(sName,value);
    }
	
    public Object get(String sName) {
        return data.get(sName);
    }
	
    public Enumeration keys() {
        return data.keys();
    }
	
    public PropertyValue[] toArray() {
        int nSize = data.size();
        PropertyValue[] props = new PropertyValue[nSize];
        int i=0;
        Enumeration keys = keys();
        while (keys.hasMoreElements()) {
            String sKey = (String) keys.nextElement();
            props[i] = new PropertyValue();
            props[i].Name = sKey;
            props[i++].Value = get(sKey);
        }
        return props;
    }
	
}
