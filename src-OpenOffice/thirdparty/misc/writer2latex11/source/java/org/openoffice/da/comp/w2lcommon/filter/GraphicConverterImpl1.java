/************************************************************************
 *
 *  GraphicConverterImpl1.java
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
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 *  
 *  Version 1.0 (2009-03-08)
 */

// 
 
package org.openoffice.da.comp.w2lcommon.filter;

// Java uno helper class
import com.sun.star.lib.uno.adapter.ByteArrayToXInputStreamAdapter;

// UNO classes
import com.sun.star.beans.PropertyValue;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
//import com.sun.star.io.XInputStream;
//import com.sun.star.io.XOutputStream;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.UnoRuntime;

//import java.io.InputStream;
//import java.io.OutputStream;

import writer2latex.api.GraphicConverter;
import writer2latex.api.MIMETypes;

/** A GraphicConverter implementation which uses the GraphicProvider service
 *  to convert the graphic. This service does only support simple format
 *  conversion using the "internal" graphics filters in Draw. Advanced features
 *  like pdf, crop and resize thus cannot be handled.
 */
public class GraphicConverterImpl1 implements GraphicConverter {

    private XGraphicProvider xGraphicProvider;
    
    private EPSCleaner epsCleaner;
	
    public GraphicConverterImpl1(XComponentContext xComponentContext) {
        try {
            // Get the XGraphicProvider interface of the GraphicProvider service
            XMultiComponentFactory xMCF = xComponentContext.getServiceManager();
            Object graphicProviderObject = xMCF.createInstanceWithContext("com.sun.star.graphic.GraphicProvider", xComponentContext);
            xGraphicProvider = (XGraphicProvider) UnoRuntime.queryInterface(XGraphicProvider.class, graphicProviderObject);
        }
        catch (com.sun.star.uno.Exception ex) {
            System.err.println("Failed to get XGraphicProvider object");
            xGraphicProvider = null;
        }
        
        epsCleaner = new EPSCleaner();
            
    }
	
    public boolean supportsConversion(String sSourceMime, String sTargetMime, boolean bCrop, boolean bResize) {
        // We don't support cropping and resizing
        if (bCrop || bResize) { return false; }

        // We can convert vector formats to eps:
        if (MIMETypes.EPS.equals(sTargetMime) && (MIMETypes.WMF.equals(sSourceMime) && MIMETypes.SVM.equals(sSourceMime))) {
            return true;
        }
		
        // And we can convert all formats to bitmaps
        boolean bSupportsSource =
           MIMETypes.PNG.equals(sSourceMime) || MIMETypes.JPEG.equals(sSourceMime) ||
           MIMETypes.GIF.equals(sSourceMime) || MIMETypes.TIFF.equals(sSourceMime) ||
           MIMETypes.BMP.equals(sSourceMime) || MIMETypes.WMF.equals(sSourceMime) ||
           MIMETypes.SVM.equals(sSourceMime);
        boolean bSupportsTarget =
           MIMETypes.PNG.equals(sTargetMime) || MIMETypes.JPEG.equals(sTargetMime) ||
           MIMETypes.GIF.equals(sTargetMime) || MIMETypes.TIFF.equals(sTargetMime) ||
           MIMETypes.BMP.equals(sTargetMime);
        return bSupportsSource && bSupportsTarget;
    }
	
    public byte[] convert(byte[] source, String sSourceMime, String sTargetMime) {

        // It seems that the GraphicProvider can only create proper eps if
        // the source is a vector format, hence
        if (MIMETypes.EPS.equals(sTargetMime)) {
            if (!MIMETypes.WMF.equals(sSourceMime) && !MIMETypes.SVM.equals(sSourceMime)) {
                return null;
            }
        }

        ByteArrayToXInputStreamAdapter xSource = new ByteArrayToXInputStreamAdapter(source);
        ByteArrayXStream xTarget = new ByteArrayXStream();
        try {
            // Read the source
            PropertyValue[] sourceProps = new PropertyValue[1];
            sourceProps[0]       = new PropertyValue();
            sourceProps[0].Name  = "InputStream";
            sourceProps[0].Value = xSource;
            XGraphic result = xGraphicProvider.queryGraphic(sourceProps);

            // Store as new type
            PropertyValue[] targetProps = new PropertyValue[2];
            targetProps[0]       = new PropertyValue();
            targetProps[0].Name  = "MimeType";
            targetProps[0].Value = sTargetMime;
            targetProps[1]       = new PropertyValue();
            targetProps[1].Name  = "OutputStream";
            targetProps[1].Value = xTarget; 
            xGraphicProvider.storeGraphic(result,targetProps);


            // Close the output and return the result
            xTarget.closeOutput();
            xTarget.flush();
            if (MIMETypes.EPS.equals(sTargetMime)) {
                return epsCleaner.cleanEps(xTarget.getBuffer());
            }
            else {
                return xTarget.getBuffer();
            }
        }
        catch (com.sun.star.io.IOException e) {
            return null;
        }
        catch (com.sun.star.lang.IllegalArgumentException e) {
            return null;
        }
        catch (com.sun.star.lang.WrappedTargetException e) {
            return null;
        }
        catch (Throwable e) {
            return null;
        }
    } 
	
}

