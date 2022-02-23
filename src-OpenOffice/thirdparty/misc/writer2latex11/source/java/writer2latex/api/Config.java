/************************************************************************
 *
 *  Config.java
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
import java.io.OutputStream;
import java.lang.IllegalArgumentException;

/** This is an interface for configuration of a {@link Converter}.
 *  A configuration always supports simple name/value options.
 *  In addition, you can read and write configurations using streams
 *  or abstract file names. The format depends on the {@link Converter}
 *  implementation, cf. the user's manual.
 */
public interface Config {
    
    /** Read a default configuration: The available configurations depend on the
     *  {@link Converter} implementation
     *
     * @param sName the name of the configuration
     * @throws IllegalArgumentException if the configuration does not exist
     */
	public void readDefaultConfig(String sName) throws IllegalArgumentException;
	 
    /** Read a configuration (stream based version) 
     * 
     * @param is the <code>InputStream</code> to read from
     * @throws IOException if an error occurs reading the stream, or the data
     * is not in the right format
     */
	public void read(InputStream is) throws IOException;
	
	/** Read a configuration (file based version) 
	 * 
	 * @param file the <code>File</code> to read from
	 * @throws IOException if the file does not exist, an error occurs reading
	 * the file, or the data is not in the right format
	 */
	public void read(File file) throws IOException;
    
	/** Write the configuration (stream based version)
	 * 
	 * @param os the <code>OutputStream</code> to write to
	 * @throws IOException if an error occurs writing to the stream
	 */
	public void write(OutputStream os) throws IOException;
    
	/** Write the configuration (file based version)
	 * 
	 * @param file the <code>File</code> to write to
	 * @throws IOException if an error occurs writing to the file
	 */
	public void write(File file) throws IOException;
	
	/** Set a name/value option. Options that are not defined by the
	 * {@link Converter} implementation as well as null values are
	 * silently ignored
	 * 
	 * @param sName the name of the option
	 * @param sValue the value of the option
	 */
	public void setOption(String sName, String sValue);
	
	/** Get a named option
	 * 
	 * @param sName the name of the option
	 * @return the value of the option, or <code>null</code> if the option does
	 * not exist or the given name is null
	 */
	public String getOption(String sName);

}

