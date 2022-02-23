/************************************************************************
 *
 *  W2XExportFilter.java
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
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.filter.ExportFilterBase;


/** This class implements the xhtml export filter component
 */
public class W2XExportFilter extends ExportFilterBase {
    
    /** Service name for the component */
    public static final String __serviceName = "org.openoffice.da.comp.writer2xhtml.W2XExportFilter";
	
    /** Implementation name for the component */
    public static final String __implementationName = "org.openoffice.da.comp.writer2xhtml.W2XExportFilter";
	
    /** Filter name to include in error messages */
	public static final String __displayName = "Writer2xhtml";

    public W2XExportFilter(XComponentContext xComponentContext1) {
        super(xComponentContext1);
        xMSF = W2XRegistration.xMultiServiceFactory;
    }

		
}



