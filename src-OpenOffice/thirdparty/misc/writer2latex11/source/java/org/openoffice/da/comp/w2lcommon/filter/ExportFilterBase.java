/************************************************************************
 *
 *  ExportFilterBase.java
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
 *  Version 1.0 (2009-04-14)
 *  
 */
 
// This file was originally based on OOo's XMergeBridge, which is (c) by Sun Microsystems

package org.openoffice.da.comp.w2lcommon.filter;

import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToOutputStreamAdapter;

//import com.sun.star.beans.PropertyValue;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.uno.AnyConverter;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
//import com.sun.star.xml.sax.InputSource;
//import com.sun.star.xml.sax.XParser;
import com.sun.star.xml.sax.XDocumentHandler;
import com.sun.star.xml.XExportFilter;

import org.openoffice.da.comp.w2lcommon.helper.MessageBox;
//import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import writer2latex.api.Converter;
import writer2latex.api.ConverterFactory;
import writer2latex.api.ConverterResult;
import writer2latex.api.OutputFile;

import java.util.Iterator;
//import java.util.Enumeration;
//import java.util.Vector;
import java.io.*;
//import javax.xml.parsers.*;
//import org.xml.sax.SAXException;
//import java.net.URI;


/** This class provides an abstract uno component which implements an XExportFilter.
 *  The filter is actually generic and only then constructor and 3 strings needs
 *   to changed by the subclass.
 */
public abstract class ExportFilterBase implements
        XExportFilter,						     
        XServiceName,
        XServiceInfo,
        XDocumentHandler,  
        XTypeProvider {
    
    /** Service name for the component */
    public static final String __serviceName = "";
	
    /** Implementation name for the component */
    public static final String __implementationName = "";
	
    /** Filter name to include in error messages */
	public static final String __displayName = "";

    private static XComponentContext xComponentContext = null;
    protected static XMultiServiceFactory xMSF;
    private static XInputStream xInStream =null;
    private static XOutputStream xOutStream=null;
    private static XOutputStream xos = null;
    private static String sdMime=null;
    private static String sURL="";
	
    private Object filterData;
    private XSimpleFileAccess2 sfa2;
	

    /** We need to get the Service Manager from the Component context to
     *  instantiate certain services, hence this constructor.
     *  The subclass must override this to set xMSF properly from the reigstration class
     */
    public ExportFilterBase(XComponentContext xComponentContext1) {
        xComponentContext = xComponentContext1;
        xMSF = null;
    }
        
        
        // Some utility methods:
		
        String getFileName(String origName) {
            String name=null;
	        if (origName !=null) {
                if(origName.equalsIgnoreCase(""))
                    name = "OutFile"; 
                else {
                    if (origName.lastIndexOf("/")>=0) {
                        origName=origName.substring(origName.lastIndexOf("/")+1,origName.length());
                    }
                    if (origName.lastIndexOf(".")>=0) {
                        name = origName.substring(0,(origName.lastIndexOf(".")));
                    }
                    else {
                        name=origName;
                   }
               }
            }
            else{   
                name = "OutFile"; 
            }

            return name;
        }
		
        public String replace(String origString, String origChar, String replaceChar){
	        String tmp="";	
	        int index=origString.indexOf(origChar);
	        if(index !=-1){
                while (index !=-1){
		            String first =origString.substring(0,index);
		            first=first.concat(replaceChar);
                    tmp=tmp.concat(first);
                    origString=origString.substring(index+1,origString.length());
 		            index=origString.indexOf(origChar);
         	        if(index==-1) {
                        tmp=tmp.concat(origString);
                    }
        	    }
	        }
	        return tmp;
        }
	   
        public String needsMask(String origString) {
            if (origString.indexOf("&")!=-1){
                origString=replace(origString,"&","&amp;");
     	    }    
	        if (origString.indexOf("\"")!=-1){
        		origString=replace(origString,"\"","&quot;");
    	    }    
	        if (origString.indexOf("<")!=-1){
        		origString=replace(origString,"<","&lt;");
    	    }  
    	    if (origString.indexOf(">")!=-1){
	        	origString=replace(origString,">","&gt;");
    	    }  
  	        return origString;
	  
        }


        // Implementation of XExportFilter:

        public boolean exporter(com.sun.star.beans.PropertyValue[] aSourceData, 
	 		       java.lang.String[] msUserData) throws com.sun.star.uno.RuntimeException{
		 		  
            sURL=null;
            filterData = null;
			
            // Get user data from configuration (type detection)
            //String udConvertClass=msUserData[0];
            //String udImport =msUserData[2];
            //String udExport =msUserData[3];
            sdMime = msUserData[5];
		
            // Get source data (only the OutputStream and the URL are actually used)
            com.sun.star.beans.PropertyValue[] pValue = aSourceData;
            for  (int  i = 0 ; i < pValue.length; i++) {
                try{
                    if (pValue[i].Name.compareTo("OutputStream")==0){
                        xos=(com.sun.star.io.XOutputStream)AnyConverter.toObject(new Type(com.sun.star.io.XOutputStream.class), pValue[i].Value);
                    }
                    //if (pValue[i].Name.compareTo("FileName")==0){
                    //    sFileName=(String)AnyConverter.toObject(new Type(java.lang.String.class), pValue[i].Value);
                    //}  
                    if (pValue[i].Name.compareTo("URL")==0){
                        sURL=(String)AnyConverter.toObject(new Type(java.lang.String.class), pValue[i].Value);
                    }
                    //if (pValue[i].Name.compareTo("Title")==0){
                    //    title=(String)AnyConverter.toObject(new Type(java.lang.String.class), pValue[i].Value);
                    //}
                    if (pValue[i].Name.compareTo("FilterData")==0) {
                        filterData = pValue[i].Value;
                    }
                } 
                catch(com.sun.star.lang.IllegalArgumentException AnyExec){
                    System.err.println("\nIllegalArgumentException "+AnyExec);
                }
            }
	     
					 
            if (sURL==null){
                sURL="";
            }
	     
            // Create a pipe to be used by the XDocumentHandler implementation:
            try {
                Object xPipeObj=xMSF.createInstance("com.sun.star.io.Pipe");
                xInStream = (XInputStream) UnoRuntime.queryInterface(
                            XInputStream.class , xPipeObj );
                xOutStream = (XOutputStream) UnoRuntime.queryInterface(
                            XOutputStream.class , xPipeObj );
            }
            catch (Exception e){
                System.err.println("Exception "+e);
                return false;
            }
	      
            return true;
        }

		
	
        // Implementation of XDocumentHandler:
        // Flat xml is created by the sax events and passed through the pipe
        // created by exporter()

        public void  startDocument () {
 	        //Do nothing
        }
	
    public void endDocument()throws com.sun.star.uno.RuntimeException {	   
        try{
            xOutStream.closeOutput();	
		    convert(xInStream,xos);
	    }
	    catch (IOException e){
            MessageBox msgBox = new MessageBox(xComponentContext);
            msgBox.showMessage(__displayName+": IO error in conversion",
                e.toString()+" at "+e.getStackTrace()[0].toString());
            throw new com.sun.star.uno.RuntimeException(e.getMessage());
	    }
	     catch (Exception e){
            MessageBox msgBox = new MessageBox(xComponentContext);
            msgBox.showMessage(__displayName+": Internal error in conversion",
                e.toString()+" at "+e.getStackTrace()[0].toString());
            throw new com.sun.star.uno.RuntimeException(__displayName+" Exception");
	    }
	}
	


	public void startElement (String str, com.sun.star.xml.sax.XAttributeList xattribs)
	{
	   
	    str="<".concat(str);
	    if (xattribs !=null)
	    {
		str= str.concat(" ");
		int len=xattribs.getLength();
		for (short i=0;i<len;i++)
		    {
			str=str.concat(xattribs.getNameByIndex(i));
			str=str.concat("=\"");
			str=str.concat(needsMask(xattribs.getValueByIndex(i)));
			str=str.concat("\" ");
		    }
	    }
	    str=str.concat(">");
	    try{
		 xOutStream.writeBytes(str.getBytes("UTF-8"));
	    }
	    catch (Exception e){
		System.err.println("\n"+e);
	    }
	    
	}

	public void endElement(String str){
	   
	    str="</".concat(str);
	    str=str.concat(">");
	    try{
		 xOutStream.writeBytes(str.getBytes("UTF-8"));
		
	    }
	    catch (Exception e){
		System.err.println("\n"+e);
	    }
    
	   
	}
	public void characters(String str){
	    str=needsMask(str);

	    try{
		 xOutStream.writeBytes(str.getBytes("UTF-8"));
	    }
	   catch (Exception e){
	       System.err.println("\n"+e);
	   }
	     
	    
	}
	
	public void ignorableWhitespace(String str){
	   
	   
	}
       public void processingInstruction(String aTarget, String aData){
	  
       }
	
	public void setDocumentLocator(com.sun.star.xml.sax.XLocator xLocator){
	  
	}

     
        
        // This is the actual conversion method, using Writer2LaTeX to convert
        // the flat xml recieved from the XInputStream, and writing the result
        // to the XOutputStream. The XMLExporter does not support export to
        // compound documents with multiple output files; so the main file
        // is written to the XOutStream and other files are written using ucb.

        public void convert (com.sun.star.io.XInputStream xml,com.sun.star.io.XOutputStream exportStream)
            throws com.sun.star.uno.RuntimeException, IOException {
			
            // Initialise the file access
            sfa2 = null;
            try {
                Object sfaObject = xComponentContext.getServiceManager().createInstanceWithContext(
                    "com.sun.star.ucb.SimpleFileAccess", xComponentContext);
                sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
            }
            catch (com.sun.star.uno.Exception e) {
                // failed to get SimpleFileAccess service (should not happen)
            }
			
            // Get base name from the url provided by OOo
            String sName= getFileName(sURL);
	  
            // Adapter for input stream (OpenDocument flat xml)
            XInputStreamToInputStreamAdapter xis =new XInputStreamToInputStreamAdapter(xml);
			
            // Adapter for output stream (Main output file)
            XOutputStreamToOutputStreamAdapter newxos =new XOutputStreamToOutputStreamAdapter(exportStream);

            // Create converter
            Converter converter = ConverterFactory.createConverter(sdMime);
            if (converter==null) {
                throw new com.sun.star.uno.RuntimeException("Failed to create converter to "+sdMime);
            }

            // Apply the FilterData to the converter
            if (filterData!=null) {
                FilterDataParser fdp = new FilterDataParser(xComponentContext);
                fdp.applyFilterData(filterData,converter);
            }
			
            // Do conversion 
            converter.setGraphicConverter(new GraphicConverterImpl(xComponentContext));
            
            ConverterResult dataOut = null;
            //try {
                dataOut = converter.convert(xis,sName);
            //}
            //catch (IOException e) {
                // Fail silently
            //}

            // Write out files
            Iterator docEnum = dataOut.iterator();
			
            // Remove the file name part of the url
            String sNewURL = null;
            if (sURL.lastIndexOf("/")>-1) {
                // Take the url up to and including the last slash
                sNewURL = sURL.substring(0,sURL.lastIndexOf("/")+1);
            }
            else {
                // The url does not include a path; this should not really happen,
                // but in this case we will write to the current default directory
                sNewURL = "";
            }
			
            while (docEnum.hasNext() && sURL.startsWith("file:")) {
                OutputFile docOut      = (OutputFile)docEnum.next();

                if (dataOut.getMasterDocument()==docOut) {
                    // The master document is written to the XOutStream supplied
                    // by the XMLFilterAdaptor
                    docOut.write(newxos);
                    newxos.flush();
                    newxos.close();
                }
                else {				
                    // Additional documents are written directly using ucb

                    // Get the file name and the (optional) directory name
                    String sFullFileName = docOut.getFileName();
                    String sDirName = "";
                    String sFileName = sFullFileName;
                    int nSlash = sFileName.indexOf("/");
                    if (nSlash>-1) {
                        sDirName = sFileName.substring(0,nSlash);
                        sFileName = sFileName.substring(nSlash+1);
                    }

                    try{
                        // Create subdirectory if required
                        if (sDirName.length()>0 && !sfa2.exists(sNewURL+sDirName)) {
                            sfa2.createFolder(sNewURL+sDirName);
                        }

                        // writeFile demands an InputStream, so we need a pipe
                        Object xPipeObj=xMSF.createInstance("com.sun.star.io.Pipe");
                        XInputStream xInStream
                          = (XInputStream) UnoRuntime.queryInterface(XInputStream.class, xPipeObj );
                        XOutputStream xOutStream
                          = (XOutputStream) UnoRuntime.queryInterface(XOutputStream.class, xPipeObj );
                        OutputStream outStream = new XOutputStreamToOutputStreamAdapter(xOutStream);
                        // Feed the pipe with content...
                        docOut.write(outStream);
                        outStream.flush();
                        outStream.close();
                        xOutStream.closeOutput();
                        // ...and then write the content to the url
                        sfa2.writeFile(sNewURL+sFullFileName,xInStream);
                    }
                    catch (Throwable e){
                        MessageBox msgBox = new MessageBox(xComponentContext);
                        msgBox.showMessage(__displayName+": Error writing files",
                            e.toString()+" at "+e.getStackTrace()[0].toString());
                    }
                }
					 
			}
			
        }


        // Implement methods from interface XTypeProvider
        // Implementation of XTypeProvider
		
        public com.sun.star.uno.Type[] getTypes() {
            Type[] typeReturn = {};

            try {
                typeReturn = new Type[] {
                new Type( XTypeProvider.class ),
                new Type( XExportFilter.class ),
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
            return __implementationName;
            //return( W2LExportFilter.class.getName() );
        }
    
        public String[] getSupportedServiceNames() {
            String[] stringSupportedServiceNames = { __serviceName };
            return( stringSupportedServiceNames );
        }

		
}



