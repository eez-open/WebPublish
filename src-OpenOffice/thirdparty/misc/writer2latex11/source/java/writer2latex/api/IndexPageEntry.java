/************************************************************************
 *
 *  IndexPageEntry.java
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
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2009-02-08)
 *
 */
 
package writer2latex.api;

/** This class represents a single entry on an index page created by a batch converter
 */
public class IndexPageEntry {

    private String sFile;
    private String sDisplayName;
    private String sDescription = null;
    private String sPdfFile = null;
    private String sOriginalFile = null;
    private boolean bIsDirectory;
	
    /** Construct a new <code>IndexPageEntry</code> based on a file name.
     *  The file name is also used as display name.
     * 
     * @param sFile the file name for this entry
     * @param bIsDirectory true if this is a directory, false if it is a file
     */
    public IndexPageEntry(String sFile, boolean bIsDirectory) {
        this.sFile = sFile;
        this.sDisplayName = sFile;
        this.bIsDirectory = bIsDirectory;
    }
	
    /** Set the file name
     * 
     * @param sFile the file name
     */
    public void setFile(String sFile) {
        this.sFile = sFile;
    }

    /** Set the display name for this entry. The display name is the
     *  name presented on the index page.
     * 
     * @param sDisplayName the display name
     */
    public void setDisplayName(String sDisplayName) {
        this.sDisplayName = sDisplayName;
    }

    /** Set the description of this file (additional information about the file)
     * 
     * @param sDescription the description
     */
    public void setDescription(String sDescription) {
        this.sDescription = sDescription;
    }

    /** Set the file name for a pdf file associated with this file
     * 
     * @param sPdfFile the file name
     */
    public void setPdfFile(String sPdfFile) {
        this.sPdfFile = sPdfFile;
    }

    /** Set the file name for the original file
     * 
     * @param sOriginalFile the origianl file name
     */
    public void setOriginalFile(String sOriginalFile) {
        this.sOriginalFile = sOriginalFile;
    }
	
    /** Get the file name
     * 
     * @return the file name
     */
    public String getFile() {
        return sFile;
    }

    /** Get the display name
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return sDisplayName;
    }

    /** Get the description
     * 
     * @return the description, or null if there is no description
     */
    public String getDescription() {
        return sDescription;
    }

    /** Get the pdf file name
     * 
     * @return the file name or null if there is none
     */
    public String getPdfFile() {
        return sPdfFile;
    }

    /** Get the original file name
     * 
     * @return the file name or null if there is none
     */
    public String getOriginalFile() {
        return sOriginalFile;
    }
	
    /** Check whether this is a file or a directory
     * 
     * @return true for a directory, false for a file
     */
    public boolean isDirectory() {
        return bIsDirectory;
    }

}
