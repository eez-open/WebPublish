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
 *  Copyright: 2002-2008 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2008-11-23)
 *
 */
 
package writer2latex.api;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

/** This is an interface for a converter, which offers conversion of
 *  all OpenDocument (or OpenOffice.org 1.x) documents in a directory
 *  (and optionally subdirectories), creating index pages in a specific format.
 *  Instances of this interface are created using the
 *  {@link ConverterFactory}
 */
public interface BatchConverter {

    /** Get the configuration interface for this batch converter
     *
     *  @return the configuration
     */
    public Config getConfig();
	
    /** Define a <code>Converter</code> implementation to use for
     *  conversion of the individual documents.
     *  If no converter is given, the <code>convert</code> method cannot
     *  convert documents (but can still create index pages).
     *
     *  @param converter the <code>Converter</code> to use
     */
    public void setConverter(Converter converter);

    /** Read a template to use as a base for the index pages.
     *  The format of the template depends on the <code>BatchConverter</code>
     *  implementation.
     *
     *  @param is an <code>InputStream</code> from which to read the template
     *  @throws IOException if some exception occurs while reading the template
     */
    public void readTemplate(InputStream is) throws IOException;
	
    /** Read a template to use as a base for the index pages.
     *  The format of the template depends on the <code>BatchConverter</code>
     *  implementation.
     *
     *  @param file the file from which to read the template
     *  @throws IOException if the file does not exist or some exception occurs
     *  while reading the template
     */
    public void readTemplate(File file) throws IOException;
	
    /** Create an index page with specific entries
     *  
     *  @param sHeading a heading describing the index page
     *  @param entries an array of <code>IndexPageEntry</code> objects (null entries
     *  are allowed, and will be ignored) describing the individual directories
     *  and documents
     */
    public OutputFile createIndexFile(String sHeading, IndexPageEntry[] entries);

    /** Convert a directory using the given <code>Converter</code> (if none is given,
     *  all files will be ignored).
     *  This method fails silently if you haven't set a converter.
     *
     *  @param source a <code>File</code> representing the directory to convert
     *  @param target a <code>File</code> representing the directory to contain
     *  the converted documents
     *  @param bRecurse determines wether or not to recurse into subdirectories
     *  @param handler a </code>BatchHandler</code>
     */
    public void convert(File source, File target, boolean bRecurse, BatchHandler handler);

}
