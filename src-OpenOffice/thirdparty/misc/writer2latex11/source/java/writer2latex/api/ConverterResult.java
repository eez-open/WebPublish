/************************************************************************
 *
 *  ConverterResult.java
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
 *  Version 1.0 (2008-11-24)
 *
 */
 
package writer2latex.api;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/** A <code>ConverterResult</code> represent a document, which is the result
 *  of a conversion performed by a <code>Converter</code>implementation.
 */
public interface ConverterResult {
    
    /** Get the master document
     *  @return <code>OutputFile</code> the master document
     */
    public OutputFile getMasterDocument();

    /** Gets an <code>Iterator</code> to access all files in the
     *  <code>ConverterResult</code>. This <em>includes</em> the master document.
     *  @return  an <code>Iterator</code> of all files
     */
    public Iterator iterator();
    
    /** Write all files of the <code>ConverterResult</code> to a directory.
     *  Subdirectories are created as required by the individual
     *  <code>OutputFile</code>s.
     *  @param dir the directory to write to (this directory must exist).
               If the parameter is null, the default directory is used
     *  @throws IOException if the directory does not exist or one or more files
     *  		could not be written
     */
    public void write(File dir) throws IOException;

}
