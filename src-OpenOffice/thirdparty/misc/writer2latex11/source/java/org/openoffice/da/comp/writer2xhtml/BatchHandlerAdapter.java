/************************************************************************
 *
 *  BatchHandlerAdapter.java
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
 *  Version 1.0 (2008-10-05) 
 *
 */
 
package org.openoffice.da.comp.writer2xhtml;

import writer2latex.api.BatchHandler;
import org.openoffice.da.writer2xhtml.XBatchHandler;

/** The uno interface provides an XBatchHandler implementation, the java
 *  interface requires a BatchHandler implementation. This simple class
 *  implements the latter using an instance of the former.
 */
public class BatchHandlerAdapter implements BatchHandler {

    private XBatchHandler unoHandler;

    public BatchHandlerAdapter(XBatchHandler unoHandler) {
        this.unoHandler = unoHandler;
    }
    
    public void startConversion() {
        unoHandler.startConversion();
    }
	
    public void endConversion() {
        unoHandler.endConversion();
    }
	
    public void startDirectory(String sName) {
        unoHandler.startDirectory(sName);
    }
	
    public void endDirectory(String sName, boolean bSuccess) {
        unoHandler.endDirectory(sName, bSuccess);
    }
	
    public void startFile(String sName) {
        unoHandler.startFile(sName);
    }
	
    public void endFile(String sName, boolean bSuccess) {
        unoHandler.endFile(sName, bSuccess);
    }
	
    public boolean cancel() {
        return unoHandler.cancel();
    }

}
