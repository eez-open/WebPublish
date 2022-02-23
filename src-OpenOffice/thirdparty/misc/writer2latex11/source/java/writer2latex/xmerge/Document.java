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
 *  This version is adapted for Writer2LaTeX
 *  version 1.0 (2008-11-22)
 *
 ************************************************************************/

package writer2latex.xmerge;

//import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

import writer2latex.api.OutputFile;

/**
 *  <p>A <code>Document</code> represents any <code>Document</code>
 *  to be converted and the resulting <code>Document</code> from any
 *  conversion.</p>
 *
 *
 *  @author  Herbie Ong
 */
public interface Document extends OutputFile {


	/**
	 *  <p>Reads the content from the <code>InputStream</code> into
     *  the <code>Document</code>.</p>
     *
     *  <p>This method may not be thread-safe.
	 *  Implementations may or may not synchronize this
	 *  method.  User code (i.e. caller) must make sure that
	 *  calls to this method are thread-safe.</p>
	 *
	 *  @param  is  <code>InputStream</code> to read in the
     *              <code>Document</code> content.
	 *
	 *  @throws  IOException  If any I/O error occurs.
	 */
	 public void read(InputStream is) throws IOException;


    /**
     *  Returns the <code>Document</code> name with no file extension.
     *
     *  @return  The <code>Document</code> name with no file extension.
     */
    public String getName();


}

