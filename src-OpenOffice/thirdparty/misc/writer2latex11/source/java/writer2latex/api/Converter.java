/************************************************************************
 *
 *  Converter.java
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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

/** This is an interface for a converter, which offers conversion of
 *  OpenDocument (or OpenOffice.org 1.x) documents into a specific format.
 *  Instances of this interface are created using the
 *  <code>ConverterFactory</code>
 */
public interface Converter {

    /** Get the interface for the configuration of this converter
     *
     *  @return the configuration
     */
    public Config getConfig();
	
    /** Define a <code>GraphicConverter</code> implementation to use for
     *  conversion of graphic files. If no converter is specified, graphic
     *  files will not be converted into other formats.
     *
     *  @param gc the <code>GraphicConverter</code> to use
     */
    public void setGraphicConverter(GraphicConverter gc);

    /** Read a template to use as a base for the converted document.
     *  The format of the template depends on the <code>Converter</code>
     *  implementation.
     *
     *  @param is an <code>InputStream</code> from which to read the template
     *  @throws IOException if some exception occurs while reading the template
     */
    public void readTemplate(InputStream is) throws IOException;

    /** Read a template to use as a base for the converted document.
     *  The format of the template depends on the <code>Converter</code>
     *  implementation.
     *
     *  @param file a file from which to read the template
     *  @throws IOException if the file does not exist or some exception occurs
     *  while reading the template
     */
    public void readTemplate(File file) throws IOException;

    /** Convert a document
     *
     *  @param is an <code>InputStream</code> from which to read the source document.
     *  @param sTargetFileName the file name to use for the converted document
     *  (if the converted document is a compound document consisting consisting
     *  of several files, this name will be used for the master document)
     *  @return a <code>ConverterResult</code> containing the converted document
     *  @throws IOException if some exception occurs while reading the document
     */
    public ConverterResult convert(InputStream is, String sTargetFileName)
        throws IOException;

    /** Convert a document
     *
     *  @param source a <code>File</code> from which to read the source document.
     *  @param sTargetFileName the file name to use for the converted document
     *  (if the converted document is a compound document consisting consisting
     *  of several files, this name will be used for the master document)
     *  @return a <code>ConverterResult</code> containing the converted document
     *  @throws FileNotFoundException if the file does not exist
     *  @throws IOException if some exception occurs while reading the document
     */
    public ConverterResult convert(File source, String sTargetFileName)
        throws FileNotFoundException, IOException;

}
