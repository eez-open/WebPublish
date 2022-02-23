/************************************************************************
 *
 *  GraphicConverterImpl2.java
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
 *
 */
 
 
package org.openoffice.da.comp.w2lcommon.filter;

import java.util.Hashtable;

import com.sun.star.awt.Point;
import com.sun.star.awt.Size;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.drawing.XDrawPages;
import com.sun.star.drawing.XDrawPagesSupplier;
import com.sun.star.drawing.XShape;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XRefreshable;

import com.sun.star.lib.uno.adapter.ByteArrayToXInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToByteArrayAdapter;

import writer2latex.api.GraphicConverter;
import writer2latex.api.MIMETypes;

/** A GraphicConverter implementation which uses a hidden Draw document to
 *  store the graphic, providing more control over the image than the
 *  simple GraphicProvider implementation.
 */
public class GraphicConverterImpl2 implements GraphicConverter {

    private XComponentContext xComponentContext;
    private Hashtable importFilter;
    private Hashtable exportFilter;
    private EPSCleaner epsCleaner;

    public GraphicConverterImpl2(XComponentContext xComponentContext) {
        this.xComponentContext = xComponentContext;

        importFilter = new Hashtable();
        importFilter.put(MIMETypes.BMP, "BMP - MS Windows");
        //importFilter.put(MIMETypes.EMF, "EMF - MS Windows Metafile");
        importFilter.put(MIMETypes.EPS, "EPS - Encapsulated PostScript");
        importFilter.put(MIMETypes.GIF, "GIF - Graphics Interchange Format");
        importFilter.put(MIMETypes.JPEG, "JPG - JPEG");
        importFilter.put(MIMETypes.PNG, "PNG - Portable Network Graphic");
        importFilter.put(MIMETypes.SVM, "SVM - StarView Metafile");
        importFilter.put(MIMETypes.TIFF, "TIF - Tag Image File");
        importFilter.put(MIMETypes.WMF, "WMF - MS Windows Metafile");
		
        exportFilter = new Hashtable();
        exportFilter.put(MIMETypes.BMP,"draw_bmp_Export");
        //exportFilter.put(MIMETypes.EMF,"draw_emf_Export");
        exportFilter.put(MIMETypes.EPS,"draw_eps_Export");
        exportFilter.put(MIMETypes.GIF,"draw_gif_Export");
        exportFilter.put(MIMETypes.JPEG,"draw_jpg_Export");
        exportFilter.put(MIMETypes.PNG,"draw_png_Export");
        //exportFilter.put(MIMETypes.SVG,"draw_svg_Export");
        exportFilter.put(MIMETypes.SVM,"draw_svm_Export");
        exportFilter.put(MIMETypes.TIFF,"draw_tif_Export");
        exportFilter.put(MIMETypes.WMF,"draw_wmf_Export");
        exportFilter.put(MIMETypes.PDF,"draw_pdf_Export");
        
        epsCleaner = new EPSCleaner();
    }
	
    public boolean supportsConversion(String sSourceMime, String sTargetMime, boolean bCrop, boolean bResize) {
        // We don't support cropping and resizing
        if (bCrop || bResize) { return false; }

        // We currently support conversion of bitmaps and svm into pdf and eps
        // Trying wmf causes an IllegalArgumentException "URL seems to be an unsupported one"
        // Seems to be an OOo bug; workaround: Use temporary files..??
        boolean bSupportsSource = MIMETypes.SVM.equals(sSourceMime) ||
           MIMETypes.PNG.equals(sSourceMime) || MIMETypes.JPEG.equals(sSourceMime) ||
           MIMETypes.GIF.equals(sSourceMime) || MIMETypes.TIFF.equals(sSourceMime) ||
           MIMETypes.BMP.equals(sSourceMime);
           //  || MIMETypes.WMF.equals(sSourceMime)
        boolean bSupportsTarget = MIMETypes.PDF.equals(sTargetMime) || MIMETypes.EPS.equals(sTargetMime);
        return bSupportsSource && bSupportsTarget;
    }
	
    public byte[] convert(byte[] source, String sSourceMime, String sTargetMime) {
        // Open a hidden sdraw document
        XMultiComponentFactory xMCF = xComponentContext.getServiceManager();

        try {
            // Load the graphic into a new draw document as xDocument
            // using a named filter
            Object desktop = xMCF.createInstanceWithContext(
                "com.sun.star.frame.Desktop", xComponentContext);

            XComponentLoader xComponentLoader = (XComponentLoader)
                UnoRuntime.queryInterface(XComponentLoader.class, desktop);

            PropertyValue[] fileProps = new PropertyValue[3];
            fileProps[0] = new PropertyValue();
            fileProps[0].Name = "FilterName";
            fileProps[0].Value = (String) importFilter.get(sSourceMime);
            fileProps[1] = new PropertyValue();
            fileProps[1].Name = "InputStream";
            fileProps[1].Value = new ByteArrayToXInputStreamAdapter(source);
            fileProps[2] = new PropertyValue();
            fileProps[2].Name = "Hidden";
            fileProps[2].Value = new Boolean(true);

            XComponent xDocument = xComponentLoader.loadComponentFromURL(
                "private:stream", "_blank", 0, fileProps);
				
            // Get the first draw page as xDrawPage
            XDrawPagesSupplier xDrawPagesSupplier = (XDrawPagesSupplier)
                UnoRuntime.queryInterface(XDrawPagesSupplier.class, xDocument);
            XDrawPages xDrawPages = xDrawPagesSupplier.getDrawPages();
            Object drawPage =  xDrawPages.getByIndex(0);
            XDrawPage xDrawPage = (XDrawPage) UnoRuntime.queryInterface(
                XDrawPage.class, drawPage);

            // Get the shape as xShape
            Object shape =  xDrawPage.getByIndex(0);
            XShape xShape = (XShape) UnoRuntime.queryInterface(XShape.class, shape);

            // Move the shape to upper left corner of the page
            Point position = new Point();
            position.X = 0;
            position.Y = 0;
            xShape.setPosition(position);
			
            // Adjust the page size and margin to the size of the graphic
            XPropertySet xPageProps = (XPropertySet)  UnoRuntime.queryInterface(
                XPropertySet.class, xDrawPage);
            Size size = xShape.getSize();
            xPageProps.setPropertyValue("Width", new Integer(size.Width));
            xPageProps.setPropertyValue("Height", new Integer(size.Height));
            xPageProps.setPropertyValue("BorderTop", new Integer(0));
            xPageProps.setPropertyValue("BorderBottom", new Integer(0));
            xPageProps.setPropertyValue("BorderLeft", new Integer(0));
            xPageProps.setPropertyValue("BorderRight", new Integer(0));
			
            // Export the draw document (xDocument)
            refreshDocument(xDocument);
			
            XOutputStreamToByteArrayAdapter outputStream = new XOutputStreamToByteArrayAdapter();
			
            PropertyValue[] exportProps = new PropertyValue[3];
            exportProps[0] = new PropertyValue();
            exportProps[0].Name = "FilterName";
            exportProps[0].Value = (String) exportFilter.get(sTargetMime);
            exportProps[1] = new PropertyValue();
            exportProps[1].Name = "OutputStream";
            exportProps[1].Value = outputStream;
            exportProps[2] = new PropertyValue();
            exportProps[2].Name = "Overwrite";
            exportProps[2].Value = new Boolean(true);
			
            XStorable xStore = (XStorable) UnoRuntime.queryInterface (
                XStorable.class, xDocument);
            xStore.storeToURL ("private:stream", exportProps);
            outputStream.closeOutput();

            byte[] result = outputStream.getBuffer();
            xDocument.dispose(); 
            
            if (MIMETypes.EPS.equals(sTargetMime)) {
            	return epsCleaner.cleanEps(result);
            }
            else {
            	return result;
            }
 
        }
        catch (com.sun.star.beans.PropertyVetoException e) {
        }
        catch (com.sun.star.beans.UnknownPropertyException e) {
        }
        catch (com.sun.star.io.IOException e) {
        }
        catch (com.sun.star.lang.IllegalArgumentException e) {
        }
        catch (com.sun.star.lang.IndexOutOfBoundsException e) {
        }
        catch (com.sun.star.lang.WrappedTargetException e) {
        }
        catch (com.sun.star.uno.Exception e) {
        }

        // Conversion failed, for whatever reason
        return null;

    } 
	
    protected void refreshDocument(XComponent document) {
        XRefreshable refreshable = (XRefreshable) UnoRuntime.queryInterface(XRefreshable.class, document);
        if (refreshable != null) {
            refreshable.refresh();
        }
    }




/*	Dim SelSize As New com.sun.star.awt.Size
	SelSize = oGraphic.Size
	oDrawGraphic.GraphicURL = oGraphic.GraphicURL
	oDrawGraphic.Size = SelSize
	oDrawPage.add(oDrawGraphic)
	oDrawGraphic.GraphicCrop = oGraphic.GraphicCrop
	oDrawPage.Width = oGraphic.Size.Width
	oDrawPage.Height =  oGraphic.Size.Height
	Dim aFilterData (1) As new com.sun.star.beans.PropertyValue
	aFilterData(0).Name  = "PixelWidth"        '
	aFilterData(0).Value = oDrawPage.Width/100 * iPixels / 25.40
	aFilterData(1).Name  = "PixelHeight"
	aFilterData(1).Value = oDrawPage.Height/100 * iPixels / 25.40
	Export( oDrawPage, sURLImageResized , aFilterData() )
	On error resume Next
	oDrawDoc.Close(True)
	On error goto 0
	
	SUB Export( xObject, sFileUrl As String, aFilterData )
	Dim xExporter As Object
	xExporter = createUnoService( "com.sun.star.drawing.GraphicExportFilter" )
	xExporter.SetSourceDocument( xObject )
	Dim aArgs (2) As new com.sun.star.beans.PropertyValue
	'sFileURL = ConvertToURL(sFileURL)
	aArgs(0).Name  = "FilterName"
	aArgs(0).Value = "jpg"
	aArgs(1).Name  = "URL"
	aArgs(1).Value = sFileURL
	'print sFileURL
	aArgs(2).Name  = "FilterData"
	aArgs(2).Value = aFilterData
	xExporter.filter( aArgs() )
END SUB*/

        
	
}

