/************************************************************************
 *
 *  BatchHandler.java
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

/** This is a call back interface to handle user interaction during a
 *  batch conversion with a {@link BatchConverter}
 */
public interface BatchHandler {
	
    /** Notification that the conversion is started */
    public void startConversion();
	
    /** Notification that the conversion has finished */
    public void endConversion();
	
    /** Notification that a directory conversion starts
     * 
     *  @param sName the name of the directory to convert
     */
    public void startDirectory(String sName);
	
    /** Notification that a directory conversion has finished
     * 
     *  @param sName the name of the directory
     *  @param bSuccess true if the conversion was successful (this only means
     *  that the index page was created with success, not that the conversion
     *  of files and subdirectories was successful)
     */
    public void endDirectory(String sName, boolean bSuccess);
	
    /** Notification that a file conversion starts
     * 
     *  @param sName the name of the file to convert
     */
    public void startFile(String sName);
	
    /** Notification that a file conversion has finished
     * 
     *  @param sName the name of the file
     *  @param bSuccess true if the conversion of this file was successful
     */
    public void endFile(String sName, boolean bSuccess);
	
    /** Notification that the conversion may be cancelled. The
     *  {@link BatchConverter} fires this event once per document.
     *  Cancelling the conversion does not delete files that was already
     *  converted
     *  
     *  @return true if the handler wants to cancel the conversion
     */
    public boolean cancel();

}
