/************************************************************************
 *
 *  The Contents of this file are made available subject to the terms of
 *
 *         - GNU Lesser General Public License Version 2.1
 *
 *  Sun Microsystems Inc., October, 2000
 *
 *  GNU Lesser General Public License Version 2.1
 *  =============================================
 *  Copyright 2000 by Sun Microsystems, Inc.
 *  901 San Antonio Road, Palo Alto, CA 94303, USA
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
 *  The Initial Developer of the Original Code is: Sun Microsystems, Inc.
 *
 *  Copyright: 2000 by Sun Microsystems, Inc.
 *
 *  All Rights Reserved.
 *
 *  Contributor(s): _______________________________________
 *
 *
 ************************************************************************/
 
// This version is adapted for writer2latex
// Version 1.0 (2008-11-23)

package writer2latex.xmerge;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
//import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;


import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
//import org.xml.sax.SAXParseException;

/**
 *  An implementation of <code>Document</code> for
 *  StarOffice documents.
 */
public class DOMDocument
    implements writer2latex.xmerge.Document {

    /** Factory for <code>DocumentBuilder</code> objects. */
    private static DocumentBuilderFactory factory =
       DocumentBuilderFactory.newInstance();

    /** DOM <code>Document</code> of content.xml. */
    private Document contentDoc = null;

    /** DOM <code>Document</code> of styles.xml. */
    //private Document styleDoc = null;

    private String documentName = null;
    private String fileName = null;
    private String fileExt = null;

    /** Resources object. */
    //private Resources res = null;


    /**
     *  Default constructor.
     *
     *  @param  name  <code>Document</code> name.
     *  @param  ext   <code>Document</code> extension.
     */
    public DOMDocument(String name,String ext)
    {
	this(name,ext,true, false);
    }

     /**
     *  Returns the file extension of the <code>Document</code> 
     *  represented.
     *
     *  @return  file extension of the <code>Document</code>.
     */
    protected String getFileExtension() {
        return fileExt;
    }


    /**
     *  Constructor with arguments to set <code>namespaceAware</code>
     *  and <code>validating</code> flags.
     *
     *  @param  name            <code>Document</code> name (may or may not
     *                          contain extension).
     *  @param  ext             <code>Document</code> extension.
     *  @param  namespaceAware  Value for <code>namespaceAware</code> flag.
     *  @param  validating      Value for <code>validating</code> flag.
     */
    public DOMDocument(String name, String ext,boolean namespaceAware, boolean validating) {

        //res = Resources.getInstance();
        factory.setValidating(validating);
        factory.setNamespaceAware(namespaceAware);
        this.fileExt = ext;
	this.documentName = trimDocumentName(name);
        this.fileName = documentName + getFileExtension();
    }


    /**
     *  Removes the file extension from the <code>Document</code>
     *  name.
     *
     *  @param  name  Full <code>Document</code> name with extension.
     *
     *  @return  Name of <code>Document</code> without the extension.
     */
    private String trimDocumentName(String name) {
        String temp = name.toLowerCase();
        String ext = getFileExtension();

        if (temp.endsWith(ext)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - ext.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }


    /**
     *  Return a DOM <code>Document</code> object of the document content
     *  file.  Note that a content DOM is not created when the constructor
     *  is called.  So, either the <code>read</code> method or the
     *  <code>initContentDOM</code> method will need to be called ahead
     *  on this object before calling this method.
     *
     *  @return  DOM <code>Document</code> object.
     */
    public Document getContentDOM() {

        return contentDoc;
    }

    /**
     *  Sets the Content of the <code>Document</code> to the contents of the 
     *  supplied <code>Node</code> list. 
     *
     *  @param newDom  DOM <code>Document</code> object.
     */
    public void setContentDOM( Node newDom) {
	contentDoc=(Document)newDom;
    }


    /**
     *  Return the name of the <code>Document</code>.
     *
     *  @return  The name of <code>Document</code>.
     */
    public String getName() {

        return documentName;
    }


    /**
     *  Return the file name of the <code>Document</code>, possibly
     *  with the standard extension.
     *
     *  @return  The file name of <code>Document</code>.
     */
    public String getFileName() {

        return fileName;
    }


    /**
     *  Read the Office <code>Document</code> from the specified
     *  <code>InputStream</code>.
     *
     *  @param  is  Office document <code>InputStream</code>.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    public void read(InputStream is) throws IOException {
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {  
            throw new IOException(ex.getMessage());
        }
        try {
            contentDoc=  builder.parse(is);
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }
    
  
    /**
     *  Write out content to the supplied <code>OutputStream</code>.
     *
     *  @param  os  XML <code>OutputStream</code>.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {

        // set bytes for writing to output stream
        byte contentBytes[] = docToBytes(contentDoc);
       
        os.write(contentBytes);
    }


    /**
     *  <p>Write out a <code>org.w3c.dom.Document</code> object into a
     *  <code>byte</code> array.</p>
     *
     *  <p>TODO: remove dependency on com.sun.xml.tree.XmlDocument
     *  package!</p>
     *
     *  @param  Document  DOM <code>Document</code> object.
     *
     *  @return  <code>byte</code> array of DOM <code>Document</code>
     *           object.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    private byte[] docToBytes(Document doc)
        throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        java.lang.reflect.Constructor con;
        java.lang.reflect.Method meth;
        
        String domImpl = doc.getClass().getName();
        
        /*
         * We may have multiple XML parsers in the Classpath.
         * Depending on which one is first, the actual type of
         * doc may vary.  Need a way to find out which API is being
         * used and use an appropriate serialization method.
         */
        try {
            // First of all try for JAXP 1.0
            if (domImpl.equals("com.sun.xml.tree.XmlDocument")) {
                Class jaxpDoc = Class.forName("com.sun.xml.tree.XmlDocument");
            
                // The method is in the XMLDocument class itself, not a helper
                meth = jaxpDoc.getMethod("write", 
                            new Class[] { Class.forName("java.io.OutputStream") } );
                                     
                meth.invoke(doc, new Object [] { baos } );
            }
	    else if (domImpl.equals("org.apache.crimson.tree.XmlDocument"))
	    {
		 Class crimsonDoc = Class.forName("org.apache.crimson.tree.XmlDocument");
		 // The method is in the XMLDocument class itself, not a helper
                meth = crimsonDoc.getMethod("write", 
                            new Class[] { Class.forName("java.io.OutputStream") } );
                                     
                meth.invoke(doc, new Object [] { baos } );  
	    }
            else if (domImpl.equals("org.apache.xerces.dom.DocumentImpl") 
            || domImpl.equals("org.apache.xerces.dom.DeferredDocumentImpl")) {
                // Try for Xerces
                Class xercesSer = 
                        Class.forName("org.apache.xml.serialize.XMLSerializer");
                
                // Get the OutputStream constructor
                // May want to use the OutputFormat parameter at some stage too
                con = xercesSer.getConstructor(new Class [] 
                        { Class.forName("java.io.OutputStream"),
                          Class.forName("org.apache.xml.serialize.OutputFormat") } );
                              
                
                // Get the serialize method
                meth = xercesSer.getMethod("serialize", 
                            new Class [] { Class.forName("org.w3c.dom.Document") } );                                           
                                           
                          
                // Get an instance
                Object serializer = con.newInstance(new Object [] { baos, null } );
                
                
                // Now call serialize to write the document
                meth.invoke(serializer, new Object [] { doc } );
            }
            else if (domImpl.equals("gnu.xml.dom.DomDocument")) {

                Class gnuSer = Class.forName("gnu.xml.dom.ls.DomLSSerializer");

                // Get the serialize method
                meth = gnuSer.getMethod("serialize",
                            new Class [] { Class.forName("org.w3c.dom.Node"),
                            Class.forName("java.io.OutputStream") } );

                // Get an instance
                Object serializer = gnuSer.newInstance();

                // Now call serialize to write the document
                meth.invoke(serializer, new Object [] { doc, baos } );
            }
            else {
                // We dont have another parser  
                throw new IOException("No appropriate API (JAXP/Xerces) to serialize XML document: " + domImpl);
            }
        }
        catch (ClassNotFoundException cnfe) {
            throw new IOException(cnfe.toString());
        }
        catch (Exception e) {
            // We may get some other errors, but the bottom line is that
            // the steps being executed no longer work
            throw new IOException(e.toString());
        }

        byte bytes[] = baos.toByteArray();

        return bytes;
    }


    /**
     *  Initializes a new DOM <code>Document</code> with the content
     *  containing minimum XML tags.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    public final void initContentDOM() throws IOException {	
        contentDoc = createDOM("");
	
    }

    /**
     *  <p>Creates a new DOM <code>Document</code> containing minimum
     *  OpenOffice XML tags.</p>
     *
     *  <p>This method uses the subclass
     *  <code>getOfficeClassAttribute</code> method to get the
     *  attribute for <i>office:class</i>.</p>
     *
     *  @param  rootName  root name of <code>Document</code>.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    private final Document createDOM(String rootName) throws IOException {

        Document doc = null;

        try {

            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();

        } catch (ParserConfigurationException ex) {
             System.err.println("Error:"+ ex);
	   

        }

        Element root = (Element) doc.createElement(rootName);
        doc.appendChild(root);

       
        return doc;
    }

}




