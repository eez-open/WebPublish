/************************************************************************
 *
 *  StarMathConverter.java
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

//import java.io.InputStream;
//import java.io.IOException;

/** This is an interface for a converter, which offers conversion of
 *  a StarMath formula into LaTeX
 *  Instances of this interface are created using the
 *  {@link ConverterFactory}
 */
public interface StarMathConverter {
    
    /** Get the configuration used when converting.
     *
     *  @return the configuration used by this converter
     */
    public Config getConfig();

    /** Convert a StarMath formula
     *
     *  @param sStarMathFormula is a string containing the StarMath formula
     *  @return a string containing the converted LaTeX formula
     */
    public String convert(String sStarMathFormula);
	
    /** Create a suitable LaTeX preamble to process the formulas converted so far
     * 
     *  @return a string containg the entire LaTeX preamble
     */
    public String getPreamble();

}



