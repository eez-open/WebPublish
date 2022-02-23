/************************************************************************
 *
 *  PageStyleConverter.java
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
 *  Version 1.0 (2008-09-08)
 *
 */

package writer2latex.xhtml;

import java.util.Enumeration;

import writer2latex.office.MasterPage;
import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.PageLayout;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;

/**
 * This class converts OpenDocument page styles to CSS2 styles.
 * A page style in a presentation is represented through the master page,
 * which links to a page layout defining the geometry and optionally a drawing
 * page defining the drawing background.
 * In a presentation document we export the full page style, in a text
 * document we only export the background.
 */
public class PageStyleConverter extends StyleConverterHelper {

    /** Create a new <code>PageStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public PageStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
    }

    public void applyStyle(String sStyleName, StyleInfo info) {
        MasterPage masterPage = ofr.getMasterPage(sStyleName);
        String sDisplayName = masterPage.getDisplayName();
        if (masterPage!=null) {
            if (ofr.isPresentation()) {
                // Always generates class name
                info.sClass="masterpage"+styleNames.getExportName(sDisplayName);
            }
            else {
                // For text documents only writing direction and background
                String sPageLayout = masterPage.getPageLayoutName();
                PageLayout pageLayout = ofr.getPageLayout(sPageLayout);
                if (pageLayout!=null) {
                    applyDirection(pageLayout,info);
                    if (bConvertStyles) {
                        getFrameSc().cssBackground(pageLayout,info.props,true);
                    }
                }
            }
        }
    }

    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        StringBuffer buf = new StringBuffer();
        Enumeration names = styleNames.keys();
        while (names.hasMoreElements()) {
            // This will be master pages for presentations only
            String sDisplayName = (String) names.nextElement();
            MasterPage style = (MasterPage)
                getStyles().getStyleByDisplayName(sDisplayName);
            StyleInfo info = new StyleInfo();
            // First apply page layout (size)
            PageLayout pageLayout = ofr.getPageLayout(style.getPageLayoutName());
            if (pageLayout!=null) {
                applyDirection(pageLayout,info);
                cssPageSize(pageLayout,info.props);
                getFrameSc().cssBackground(pageLayout,info.props,true);
            }
            // Next apply drawing-page style (draw background)
            StyleWithProperties drawingPage = ofr.getDrawingPageStyle(style.getProperty(XMLString.DRAW_STYLE_NAME));
            if (drawingPage!=null) {
                cssDrawBackground(drawingPage,info.props,true);
            }
            // The export the results
            buf.append(sIndent)
               .append(".masterpage").append(styleNames.getExportName(sDisplayName))
               .append(" {").append(info.props.toString()).append("}\n");
        }
        return buf.toString();
    }
	
    /** Get the family of page styles (master pages)
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getMasterPages();
    }

    // Background properties in draw: Color, gradient, hatching or bitmap
    private void cssDrawBackground(StyleWithProperties style, CSVList props, boolean bInherit){
        // Fill color: Same as in css
        String s = style.getProperty(XMLString.DRAW_FILL_COLOR,bInherit);
        if (s!=null) { props.addValue("background-color",s); }
    }

	
    private void cssPageSize(PageLayout style, CSVList props) {
        String sWidth = style.getProperty(XMLString.FO_PAGE_WIDTH);
        if (sWidth!=null) { props.addValue("width",scale(sWidth)); }
        String sHeight = style.getProperty(XMLString.FO_PAGE_HEIGHT);
        if (sHeight!=null) { props.addValue("height",scale(sHeight)); }
    }

	
	
}
