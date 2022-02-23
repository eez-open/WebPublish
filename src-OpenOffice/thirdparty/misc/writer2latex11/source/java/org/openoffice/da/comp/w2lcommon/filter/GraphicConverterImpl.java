/************************************************************************
 *
 *  GraphicConverterImpl.java
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
 
package org.openoffice.da.comp.w2lcommon.filter;

import com.sun.star.uno.XComponentContext;

import writer2latex.api.GraphicConverter;

public class GraphicConverterImpl implements GraphicConverter {

    private GraphicConverter graphicConverter1;
    private GraphicConverter graphicConverter2;

    public GraphicConverterImpl(XComponentContext xComponentContext) {
        graphicConverter1 = new GraphicConverterImpl1(xComponentContext);
        graphicConverter2 = new GraphicConverterImpl2(xComponentContext);
    }
	
    public boolean supportsConversion(String sSourceMime, String sTargetMime, boolean bCrop, boolean bResize) {
        return graphicConverter1.supportsConversion(sSourceMime, sTargetMime, bCrop, bResize) ||
               graphicConverter2.supportsConversion(sSourceMime, sTargetMime, bCrop, bResize);
    }
	
    public byte[] convert(byte[] source, String sSourceMime, String sTargetMime) {
        byte[] result = null;

        // Prefer the simple implementation (GraphicProvider)
        if (graphicConverter1.supportsConversion(sSourceMime, sTargetMime, false, false)) {
            result = graphicConverter1.convert(source, sSourceMime, sTargetMime);
        }

        // If this is not possible or fails, try the complex implementation
        if (result==null) {
            result = graphicConverter2.convert(source, sSourceMime, sTargetMime);
        }
		
        return result;
	
    }


}

