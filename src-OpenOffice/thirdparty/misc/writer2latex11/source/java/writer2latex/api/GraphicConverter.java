/************************************************************************
 *
 *  GraphicConverter.java
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

/** A simple interface for a graphic converter which converts between various
 *  graphics formats
 */
public interface GraphicConverter {
    
    /** Check whether a certain conversion is supported by the converter
     * 
     *  @param sSourceMime a string containing the source Mime type
     *  @param sTargetMime a string containing the target Mime type
     *  @param bCrop true if the target graphic should be cropped
     *  @param bResize true if the target graphic should be resized
     *  (the last two parameters are for future use)
     *  @return true if the conversion is supported 
     */
    public boolean supportsConversion(String sSourceMime, String sTargetMime, boolean bCrop, boolean bResize);
	
    /** Convert a graphics file from one format to another
     * 
     *  @param source a byte array containing the source graphic
     *  @param sSourceMime a string containing the Mime type of the source
     *  @param sTargetMime a string containing the desired Mime type of the target
     *  @return a byte array containing the converted graphic. Returns null
     *  if the conversion failed. 
     */
    public byte[] convert(byte[] source, String sSourceMime, String sTargetMime);

}



