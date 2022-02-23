/************************************************************************
 *
 *  LaTeXDocument.java
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
 *  Version 1.0 (2008-11-23)
 *
 */

package writer2latex.latex;

import writer2latex.xmerge.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * <p>Class representing a LaTeX document.</p>
 *
 */
public class LaTeXDocument implements Document {
    private static final String FILE_EXTENSION = ".tex";
	
    private String sName;
	
    private String sEncoding = "ASCII";
	
    private int nWrap;
    
    private LaTeXDocumentPortion contents;

    /**
     * <p>Constructs a new LaTeX Document.</p>
     *
     * <p>This new document is empty.  Document data must added to the preamble and
     *    the body using appropriate methods.</p>
     *
     * @param   sName   The name of the <code>LaTeXDocument</code>.
     * @param   nWrap   Lines should be wrapped after this position
     */
    public LaTeXDocument(String sName,int nWrap) {
        this.nWrap = nWrap;
        this.sName = trimDocumentName(sName);
        contents = new LaTeXDocumentPortion(true);
    }
    
    /**
     * <p>This method is supposed to read <code>byte</code> data from the InputStream.
     * Currently it does nothing, since we don't need it.</p>
     * 
     * @param   is      InputStream containing a LaTeX data file.
     *
     * @throws  IOException     In case of any I/O errors.
     */
    public void read(InputStream is) throws IOException {
        // Do nothing.
    }
  
    
    /**
     * <p>Returns the <code>Document</code> name with no file extension.</p>
     *
     * @return  The <code>Document</code> name with no file extension.
     */
    public String getName() {
        return sName;
    }
    
    
    /**
     * <p>Returns the <code>Document</code> name with file extension.</p>
     *
     * @return  The <code>Document</code> name with file extension.
     */
    public String getFileName() {
        return new String(sName + FILE_EXTENSION);
    }
    
    
    /**
     * <p>Writes out the <code>Document</code> content to the specified
     * <code>OutputStream</code>.</p>
     *
     * <p>This method may not be thread-safe.
     * Implementations may or may not synchronize this
     * method.  User code (i.e. caller) must make sure that
     * calls to this method are thread-safe.</p>
     *
     * @param  os  <code>OutputStream</code> to write out the
     *             <code>Document</code> content.
     *
     * @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(os,sEncoding);
        contents.write(osw,nWrap,"\n");
        osw.flush();
        osw.close();
    }
    
    /**
     * <p> Set the output encoding to use when writing the document.</p>
     */
    public void setEncoding(String sEncoding) { this.sEncoding = sEncoding; }

    /**
     * <p>Returns the <code>LaTeXDocumentPortion</code>, that contains the
     * contents of the document.</p>
     *
     * @return  The content <code>LaTeXDocumentPortion</code>.
     */
    public LaTeXDocumentPortion getContents(){
        return contents;
    }

    /*
     * Utility method to make sure the document name is stripped of any file
     * extensions before use.
     */
    private String trimDocumentName(String name) {
        String temp = name.toLowerCase();
        
        if (temp.endsWith(FILE_EXTENSION)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - FILE_EXTENSION.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }
}
    