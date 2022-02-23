/************************************************************************
 *
 *  RowStyleConverter.java
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
 *  Version 1.0 (2008-11-22)
 *
 */

package writer2latex.xhtml;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
//import writer2latex.office.XMLString;
import writer2latex.util.CSVList;

/**
 * This class converts OpenDocument row styles to CSS2 styles.
 * Rows formatting includes <em>background</em>, and also <em>height</em>,
 * which is considered elsewhere.
 */
public class RowStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>RowStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public RowStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        // Style maps for rows are currently not supported.
        this.styleMap = new XhtmlStyleMap();
        this.bConvertStyles = config.xhtmlTableFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlTableFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlTableFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlTableFormatting()==XhtmlConfig.IGNORE_STYLES;
    }

    /** Get the family of row styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getRowStyles();
    }
	
    /** Create default tag name to represent a row object
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "tr";
    }
	
    /** Convert formatting properties for a specific Row style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        getFrameSc().cssBackground(style,props,bInherit);
    }
	
}
