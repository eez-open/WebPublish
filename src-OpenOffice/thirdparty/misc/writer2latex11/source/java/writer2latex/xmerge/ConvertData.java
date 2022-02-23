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
 
// This version is adapted for Writer2LaTeX
// Change: The first document added will become the "master document" 2006/10/05
// Version 1.0 (2008-11-24)

package writer2latex.xmerge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Iterator;

import writer2latex.api.ConverterResult;
import writer2latex.api.OutputFile;

/**
 *  <p><code>ConvertData</code> is used as a container for passing
 *  <code>OutputFile</code> objects in and out of the <code>Convert</code>
 *  class.  The <code>ConvertData</code> contains a <code>String</code>
 *  name and a <code>Vector</code> of <code>OutputFile</code> objects.</p>
 *
 *  @author  Martin Maher 
 */
public class ConvertData implements ConverterResult {

    /**
     *  Vector of <code>OutputFile</code> objects.
     */
	private Vector v = new Vector();
	
    /** Master doc */
    private OutputFile masterDoc = null;

    /**
     *  Name of the <code>ConvertData</code> object.
     */
	private String name;
	

    /**
     *  Resets ConvertData.  This empties all <code>OutputFile</code>
     *  objects from this class.  This allows reuse of a
     *  <code>ConvertData</code>.
     */
    public void reset() {
		name = null;
                v.removeAllElements();
	}
        
    /**
     *  Returns the <code>OutputFile</code> name.
     *
     *  @return  The <code>OutputFile</code> name.
     */
    public String getName() {
		return name;
	}


    /**
     *  Sets the <code>OutputFile</code> name.
     *
     *  @param  docName  The name of the <code>OutputFile</code>.
     */
    public void setName(String docName) {
		name = docName;
	}

    /**
     *  Adds a <code>OutputFile</code> to the vector.
     *
     *  @param  doc  The <code>OutputFile</code> to add.
     */
    public void addDocument(OutputFile doc) {
        if (v.size()==0) { masterDoc = doc; }
        v.add(doc);
	}
	
    /** Get the master document
     *  @return <code>OutputFile</code> the master document
     */
    public OutputFile getMasterDocument() {
        return masterDoc;
    }
	
    /** Check if a given document is the master document
     *  @param doc  The <code>OutputFile</code> to check
     *  @return true if this is the master document
     */
    public boolean isMasterDocument(OutputFile doc) {
        return doc == masterDoc;
    }

	
    /**
     *  Gets an <code>Iterator</code> to access the <code>Vector</code>
     *  of <code>OutputFile</code> objects
     *
     *  @return  The <code>Iterator</code> to access the
     *           <code>Vector</code> of <code>OutputFile</code> objects.
     */
    public Iterator iterator() {
        return v.iterator();
	}


    /**
     *  Gets the number of <code>OutputFile</code> objects currently stored 
     *
     *  @return  The number of <code>OutputFile</code> objects currently
     *           stored.
     */
    public int getNumDocuments() {
		return (v.size());
	}
    
    public void write(File dir) throws IOException {
        if (dir!=null && !dir.exists()) throw new IOException("Directory does not exist");
        Iterator docEnum = iterator();
        while (docEnum.hasNext()) {
            OutputFile docOut = (OutputFile) docEnum.next();
            String sDirName = "";
            String sFileName = docOut.getFileName();
            File subdir = dir;
            int nSlash = sFileName.indexOf("/");
            if (nSlash>-1) {
                sDirName = sFileName.substring(0,nSlash);
                sFileName = sFileName.substring(nSlash+1);
                subdir = new File(dir,sDirName);
                if (!subdir.exists()) { subdir.mkdir(); }
            }
            File outfile = new File (subdir,sFileName);
            FileOutputStream fos = new FileOutputStream(outfile);
            docOut.write(fos);
            fos.flush();
            fos.close();
        }

    }
}

