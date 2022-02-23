/************************************************************************
 *
 *  BatchConverter.java
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
 *  Version 1.0 (2009-02-16)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToOutputStreamAdapter;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentInfoSupplier;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XTextDocument;
import com.sun.star.ucb.CommandAbortedException;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

//import writer2latex.api.BatchConverter;
//import writer2latex.api.BatchHandler;
//import writer2latex.api.Converter;
import writer2latex.api.ConverterFactory;
import writer2latex.api.IndexPageEntry;
import writer2latex.api.MIMETypes;
import writer2latex.api.OutputFile;

// Import interfaces as defined in uno idl
import org.openoffice.da.writer2xhtml.XBatchConverter;
import org.openoffice.da.writer2xhtml.XBatchHandler;
import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;

/** This class provides a uno component which implements the interface
 *  org.openoffice.da.writer2xhtml.XBatchConverter
 */
public class BatchConverter implements
        XBatchConverter,						     
        XServiceName,
        XServiceInfo,
        XTypeProvider {
    
    /** The component will be registered under this name.
     */
    public static final String __serviceName = "org.openoffice.da.writer2xhtml.BatchConverter";
	
    public static final String __implementationName = "org.openoffice.da.comp.writer2xhtml.BatchConverter";

    private XComponentContext xComponentContext = null;
    
    private XSimpleFileAccess2 sfa2 = null;
    private writer2latex.api.BatchConverter batchConverter = null;
    
    private XBatchHandler handler;
    
    // Based on convert arguments
    private boolean bRecurse = true;
    private String sWriterFilterName = "org.openoffice.da.writer2xhtml";
    private Object writerFilterData = null;
    private String sCalcFilterName = "org.openoffice.da.calc2xhtml";
    private Object calcFilterData = null;
    private boolean bIncludePdf = true;
    private boolean bIncludeOriginal = false;
    private boolean bUseTitle = true;
    private boolean bUseDescription = true;
    private String sUplink = "";
    private String sDirectoryIcon = "";
    private String sDocumentIcon = "";
    private String sTemplateURL = null;

    public BatchConverter(XComponentContext xComponentContext) {
        this.xComponentContext = xComponentContext;
        // Get the SimpleFileAccess service
        try {
            Object sfaObject = xComponentContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.ucb.SimpleFileAccess", xComponentContext);
            sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get SimpleFileAccess service (should not happen)
        }
    }
    
    // Helper: Get a string from an any
    private String getValue(Object any) {
    	if (AnyConverter.isString(any)) {
    		try {
    			return AnyConverter.toString(any);
    		}
    		catch (com.sun.star.lang.IllegalArgumentException e) {
    			return "";
    		}
    	}
    	else {
    		return "";
    	}
    }
        
    // Implementation of XBatchConverter:
    public void convert(String sSourceURL, String sTargetURL, PropertyValue[] lArguments, XBatchHandler handler) {
        // Create batch converter (currently we don't need to set a converter)
        batchConverter = ConverterFactory.createBatchConverter(MIMETypes.XHTML);
        
        this.handler = handler;
        
        // Read the arguments
        int nSize = lArguments.length;
        for (int i=0; i<nSize; i++) {
        	String sName = lArguments[i].Name;
        	Object value = lArguments[i].Value;
        	if ("Recurse".equals(sName)) {
        		bRecurse = !"false".equals(getValue(value));
        	}
        	else if ("IncludePdf".equals(sName)) {
        		bIncludePdf = !"false".equals(getValue(value));
        	}
        	else if ("IncludeOriginal".equals(sName)) {
        		bIncludeOriginal = "true".equals(getValue(value));
        		
        	}
        	else if ("UseTitle".equals(sName)) {
        		bUseTitle = !"false".equals(getValue(value));        		
        	}
        	else if ("UseDescription".equals(sName)) {
        		bUseDescription = !"false".equals(getValue(value));
        	}
        	else if ("Uplink".equals(sName)) {
        		sUplink = getValue(value);
        	}
        	else if ("DirectoryIcon".equals(sName)) {
        		sDirectoryIcon = getValue(value);
        	}
        	else if ("DocumentIcon".equals(sName)) {
        		sDocumentIcon = getValue(value);
        	}
        	else if ("TemplateURL".equals(sName)) {
        		sTemplateURL = getValue(value);
        	}
        	else if ("WriterFilterName".equals(sName)) {
        		sWriterFilterName = getValue(value);
        	}
        	else if ("WriterFilterData".equals(sName)) {
        		writerFilterData = lArguments[i].Value;
        	}
        	else if ("CalcFilterName".equals(sName)) {
        		sCalcFilterName = getValue(value);        		
        	}
        	else if ("CalcFilterData".equals(sName)) {
        		calcFilterData = lArguments[i].Value;        		
        	}
        }
        
        // Set arguments on batch converter
        batchConverter.getConfig().setOption("uplink", sUplink);
        batchConverter.getConfig().setOption("directory_icon", sDirectoryIcon);
        batchConverter.getConfig().setOption("document_icon", sDocumentIcon);
        if (sTemplateURL!=null) {
        	try {
        		XInputStream xis = sfa2.openFileRead(sTemplateURL);
        		XInputStreamToInputStreamAdapter isa = new XInputStreamToInputStreamAdapter(xis);
            	batchConverter.readTemplate(isa);
        	}
        	catch (IOException e) {
        		// The batchconverter failed to read the template
        	}
        	catch (CommandAbortedException e) {
        		// The sfa could not execute the command
        	}
        	catch (com.sun.star.uno.Exception e) {
        		// Unspecified uno exception
        	}
        }
        
        // Convert the directory
        handler.startConversion();
        convertDirectory(sSourceURL, sTargetURL, getName(sSourceURL));
        handler.endConversion();
    }
    
    // Convert a directory - return true if not cancelled
    private boolean convertDirectory(String sSourceURL, String sTargetURL, String sHeading) {
        handler.startDirectory(sSourceURL);

        // Step 1: Get the directory
        String[] contents;
        try {
            contents = sfa2.getFolderContents(sSourceURL, true);
        }
        catch (CommandAbortedException e) {
            handler.endDirectory(sSourceURL,false);
            return true;
        }
        catch (com.sun.star.uno.Exception e) {
            handler.endDirectory(sSourceURL,false);
            return true;
        }
        int nLen = contents.length;
        IndexPageEntry[] entries = new IndexPageEntry[nLen];
		
        // Step 2: Traverse subdirectories, if allowed
        if (bRecurse) {
            String sUplink = batchConverter.getConfig().getOption("uplink");
            for (int i=0; i<nLen; i++) {
                boolean bIsDirectory = false;
                try {
                    bIsDirectory = sfa2.isFolder(contents[i]);
                }
                catch (CommandAbortedException e) {
                    // Considered non critical, ignore
                }
                catch (com.sun.star.uno.Exception e) {
                    // Considered non critical, ignore
                }
                if (bIsDirectory) {
                    batchConverter.getConfig().setOption("uplink","../index.html");
                    String sNewTargetURL = ensureSlash(sTargetURL) + getName(contents[i]);
                    String sNewHeading = sHeading + " - " + decodeURL(getName(contents[i]));
                    boolean bResult = convertDirectory(contents[i],sNewTargetURL,sNewHeading);
                    batchConverter.getConfig().setOption("uplink", sUplink);
                    if (!bResult) { return false; }
                    // Create entry for this subdirectory
                    IndexPageEntry entry = new IndexPageEntry(ensureSlash(sNewTargetURL)+"index.html",true);
                    entry.setDisplayName(decodeURL(getName(contents[i])));
                    entries[i]=entry;
                }
            }
        }

        // Step 3: Traverse documents
        for (int i=0; i<nLen; i++) {
            boolean bIsFile = false;
            try {
                bIsFile = sfa2.exists(contents[i]) && !sfa2.isFolder(contents[i]);
            }
            catch (CommandAbortedException e) {
                // Considered non critical, ignore
            }
            catch (com.sun.star.uno.Exception e) {
                // Considered non critical, ignore
            }
            if (bIsFile) {
                IndexPageEntry entry = convertFile(contents[i],sTargetURL);
                if (entry!=null) { entries[i]=entry; }
                if (handler.cancel()) { return false; }
            }
        }

        // Step 4: Create and write out the index file
        OutputFile indexFile = batchConverter.createIndexFile(sHeading, entries);

        try {
            if (!sfa2.exists(sTargetURL)) { sfa2.createFolder(sTargetURL); }
        }
        catch (CommandAbortedException e) {
            handler.endDirectory(sSourceURL,false);
            return true;
        }
        catch (com.sun.star.uno.Exception e) {
            handler.endDirectory(sSourceURL,false);
            return true;
        }

        try {
            // writeFile demands an InputStream, so we need a pipe
            Object xPipeObj=xComponentContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.io.Pipe",xComponentContext);
            XInputStream xInStream
                = (XInputStream) UnoRuntime.queryInterface(XInputStream.class, xPipeObj );
            XOutputStream xOutStream
                = (XOutputStream) UnoRuntime.queryInterface(XOutputStream.class, xPipeObj );
            OutputStream outStream = new XOutputStreamToOutputStreamAdapter(xOutStream);
            // Feed the pipe with content...
            indexFile.write(outStream);
            outStream.flush();
            outStream.close();
            xOutStream.closeOutput();
            // ...and then write the content to the url
            sfa2.writeFile(ensureSlash(sTargetURL)+"index.html",xInStream);
        }
        catch (IOException e) {
            handler.endDirectory(sSourceURL,false);
            return true;
        }
        catch (NotConnectedException e) {
            handler.endDirectory(sSourceURL,false);
            return true;
        }
        catch (com.sun.star.uno.Exception e) {
            handler.endDirectory(sSourceURL,false);
            return true;
        }

        handler.endDirectory(sSourceURL, true);

        return !handler.cancel();
    }
    
    private IndexPageEntry convertFile(String sSourceFileURL, String sTargetURL) {
        handler.startFile(sSourceFileURL);

        String sTargetFileURL = ensureSlash(sTargetURL) + getBaseName(sSourceFileURL) + ".html";
        
        IndexPageEntry entry = new IndexPageEntry(getName(sTargetFileURL),false);
        entry.setDisplayName(decodeURL(getBaseName(sTargetFileURL)));

        // Load component
        XComponent xDocument;
        try {
            Object desktop = xComponentContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.frame.Desktop", xComponentContext);

            XComponentLoader xComponentLoader = (XComponentLoader)
                UnoRuntime.queryInterface(XComponentLoader.class, desktop);

            PropertyValue[] fileProps = new PropertyValue[1];
            fileProps[0] = new PropertyValue();
            fileProps[0].Name = "Hidden";
            fileProps[0].Value = new Boolean(true);

            xDocument = xComponentLoader.loadComponentFromURL(sSourceFileURL, "_blank", 0, fileProps);
        }
        catch (com.sun.star.io.IOException e) {
            handler.endFile(sSourceFileURL,false);
            return null;
        }
        catch (com.sun.star.uno.Exception e) {
            handler.endFile(sSourceFileURL,false);
            return null;
        }
        
        // Get the title and the description of the document
        XDocumentInfoSupplier docInfo = (XDocumentInfoSupplier) UnoRuntime.queryInterface(XDocumentInfoSupplier.class, xDocument);
        XPropertySet infoProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, docInfo.getDocumentInfo());
        if (infoProps!=null) {
            try {
            	Object loadedTitle = infoProps.getPropertyValue("Title");
            	if (AnyConverter.isString(loadedTitle)) {
            		String sLoadedTitle = AnyConverter.toString(loadedTitle);
            		if (bUseTitle && sLoadedTitle.length()>0) {
            			entry.setDisplayName(sLoadedTitle);
            		}
            	}

            	Object loadedDescription = infoProps.getPropertyValue("Description");
            	if (AnyConverter.isString(loadedDescription)) {
            		String sLoadedDescription = AnyConverter.toString(loadedDescription);
            		if (bUseDescription && sLoadedDescription.length()>0) {
            			entry.setDescription(sLoadedDescription);
            		}
            	}
            }
            catch (UnknownPropertyException e) {
            }
            catch (WrappedTargetException e) {
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
            }
        }

        // Determine the type of the component
        boolean bText = false;
        boolean bSpreadsheet = false;
        if (UnoRuntime.queryInterface(XTextDocument.class, xDocument)!=null) { bText=true; }
        else if (UnoRuntime.queryInterface(XSpreadsheetDocument.class, xDocument)!=null) { bSpreadsheet=true; }
        if (!bText && !bSpreadsheet) {
            handler.endFile(sSourceFileURL,false);
            xDocument.dispose();
            return null;
        }
        
        // Convert using requested filter
        boolean bHtmlSuccess = true;
        
        PropertyHelper exportProps = new PropertyHelper();
        exportProps.put("FilterName", bText ? sWriterFilterName : sCalcFilterName);
        exportProps.put("Overwrite", new Boolean(true));
        if (bText && writerFilterData!=null) { exportProps.put("FilterData", writerFilterData); }
        if (bSpreadsheet && calcFilterData!=null) { exportProps.put("FilterData", calcFilterData); }
			
        try {
            XStorable xStore = (XStorable) UnoRuntime.queryInterface (XStorable.class, xDocument);
            xStore.storeToURL (sTargetFileURL, exportProps.toArray());
        }
        catch (com.sun.star.io.IOException e) {
        	// Failed to convert; continue anyway, but don't link to the file name
        	entry.setFile(null);
            bHtmlSuccess = false;
        }
        
        // Convet to pdf if requested
        boolean bPdfSuccess = true;
        
        if (bIncludePdf) {
            PropertyValue[] pdfProps = new PropertyValue[2];
            pdfProps[0] = new PropertyValue();
            pdfProps[0].Name = "FilterName";
            pdfProps[0].Value = bText ? "writer_pdf_Export" : "calc_pdf_Export";
            pdfProps[1] = new PropertyValue();
            pdfProps[1].Name = "Overwrite";
            pdfProps[1].Value = new Boolean(true);
            
            String sPdfFileURL = ensureSlash(sTargetURL) + getBaseName(sSourceFileURL) + ".pdf";
			
            try {
                XStorable xStore = (XStorable) UnoRuntime.queryInterface (XStorable.class, xDocument);
                xStore.storeToURL (sPdfFileURL, pdfProps);
                entry.setPdfFile(sPdfFileURL);
            }
            catch (com.sun.star.io.IOException e) {
                // Not critical, continue without pdf
            	bPdfSuccess = false;
            }
        }

        xDocument.dispose();
        
        // Include original document if required
        if (bIncludeOriginal) {
        	// TODO
        }
        
    	// Report a failure to the user if either of the exports failed
        handler.endFile(sSourceFileURL,bHtmlSuccess && bPdfSuccess);
        // But return the index entry even if only one succeded
        if (bHtmlSuccess || bPdfSuccess) { return entry; }
        else { return null; }
    }
    
    private String getName(String sURL) {
        int n = sURL.lastIndexOf("/");
        return n>-1 ? sURL.substring(n+1) : sURL;
    }
    
    private String getBaseName(String sURL) {
        String sName = getName(sURL);
        int n = sName.lastIndexOf(".");
        return n>-1 ? sName.substring(0,n) : sName;
    }
    
    private String ensureSlash(String sURL) {
        return sURL.endsWith("/") ? sURL : sURL+"/";
    }
    
    private String decodeURL(String sURL) {
        try {
            return new URI(sURL).getPath();
        }
        catch (URISyntaxException e) {
            return sURL;
        }
    }
	
    // Implement methods from interface XTypeProvider

    public com.sun.star.uno.Type[] getTypes() {
        Type[] typeReturn = {};
        try {
            typeReturn = new Type[] {
            new Type( XBatchConverter.class ),
            new Type( XTypeProvider.class ),
            new Type( XServiceName.class ),
            new Type( XServiceInfo.class ) };
        }
        catch( Exception exception ) {
		
        }

        return( typeReturn );
    }


    public byte[] getImplementationId() {
        byte[] byteReturn = {};

        byteReturn = new String( "" + this.hashCode() ).getBytes();

        return( byteReturn );
    }

    // Implement method from interface XServiceName
    public String getServiceName() {
        return( __serviceName );
    }
    
    // Implement methods from interface XServiceInfo
    public boolean supportsService(String stringServiceName) {
        return( stringServiceName.equals( __serviceName ) );
    }
    
    public String getImplementationName() {
        return( __implementationName );
    }
    
    public String[] getSupportedServiceNames() {
        String[] stringSupportedServiceNames = { __serviceName };
        return( stringSupportedServiceNames );
    }

		
}



