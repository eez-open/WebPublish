/************************************************************************
 *
 *  ConverterFactory.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0.2 (2010-05-31)
 *
 */
 
package writer2latex.api;

/** This is a factory class which provides static methods to create converters
 *  for documents in OpenDocument (or OpenOffice.org 1.x) format into a specific MIME type
 */
public class ConverterFactory {

    // Version information
    private static final String VERSION = "1.0.2";
    private static final String DATE = "2010-05-31";
	
    /** Return version information
     *  @return the Writer2LaTeX version in the form
     *  (major version).(minor version).(patch level)
     *  an uneven minor version indicates a development release
     */
    public static String getVersion() { return VERSION; }

    /** Return date information
     *  @return the release date for this Writer2LaTeX version
     */
    public static String getDate() { return DATE; }

    /** <p>Create a <code>Converter</code> implementation which supports
     *  conversion into the specified MIME type.</p>
     *  <p>Currently supported MIME types are:</p>
     *  <ul>
     *    <li><code>application/x-latex</code> for LaTeX format</li>
     *    <li><code>application/x-bibtex</code> for BibTeX format</li>
     *    <li><code>text/html</code> for XHTML 1.0 strict format</li>
     *    <li><code>application/xhtml+xml</code> for XHTML+MathML</li>
     *    <li><code>application/xml</code> for XHTML+MathML using stylesheets from w3c's
     *        math working group</li>
     *  </ul>
     *  
     *  @param sMIME the MIME type of the target format
     *  @return the required <code>Converter</code> or null if a converter for
     *  the requested MIME type could not be created
     */
    public static Converter createConverter(String sMIME) {
        Object converter = null;
        if (MIMETypes.LATEX.equals(sMIME)) {
            converter = createInstance("writer2latex.latex.ConverterPalette");
        }
        else if (MIMETypes.BIBTEX.equals(sMIME)) {
            converter = createInstance("writer2latex.bibtex.Converter");
        }
        else if (MIMETypes.XHTML.equals(sMIME)) {
            converter = createInstance("writer2latex.xhtml.Xhtml10Converter");
        }
        else if (MIMETypes.XHTML_MATHML.equals(sMIME)) {
            converter = createInstance("writer2latex.xhtml.XhtmlMathMLConverter");
        }
        else if (MIMETypes.XHTML_MATHML_XSL.equals(sMIME)) {
            converter = createInstance("writer2latex.xhtml.XhtmlMathMLXSLConverter");
        }
        return converter instanceof Converter ? (Converter) converter : null;
    }
	
    /** <p>Create a <code>BatchConverter</code> implementation which supports
     *  conversion into the specified MIME type</p>
     *  <p>The only currently supported MIME type is <code>text/html</code>
     *  (XHTML 1.0 strict)</p>
     *
     *  @param sMIME the MIME type of the target format
     *  @return the required <code>BatchConverter</code> or null if a converter
     *  for the requested MIME type could not be created
     */
    public static BatchConverter createBatchConverter(String sMIME) {
        Object converter = null;
        if (MIMETypes.XHTML.equals(sMIME)) {
            converter = createInstance("writer2latex.xhtml.BatchConverterImpl");
        }
        return converter instanceof BatchConverter ? (BatchConverter) converter : null;
    }
	
    /** Create a <code>StarMathConverter</code> implementation
     *
     *  @return the converter
     */
    public static StarMathConverter createStarMathConverter() {
        Object converter = createInstance("writer2latex.latex.StarMathConverter");
        return converter instanceof StarMathConverter ? (StarMathConverter) converter : null;
    }
	
    private static Object createInstance(String sClassName) {
        try {
		        return Class.forName(sClassName).newInstance();
        }
        catch (java.lang.ClassNotFoundException e) {
            return null;
        } 
        catch (java.lang.InstantiationException e) {
            return null;
        }
        catch (java.lang.IllegalAccessException e) {
            return null;
        } 
    }

}
