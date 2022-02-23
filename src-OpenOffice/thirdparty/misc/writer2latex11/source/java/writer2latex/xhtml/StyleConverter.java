/************************************************************************
 *
 *  StyleConverter.java
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

//import java.util.Enumeration;
//import java.util.Hashtable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.office.*;
import writer2latex.util.*;

/**
 * <p>This class converts OpenDocument styles to CSS2 styles.</p>
 * <p>Note that some elements in OpenDocument has attributes that also maps
 * to CSS2 properties. Example: the width of a text box.</p>
 * <p>Also note, that some OpenDocument style properties cannot be mapped to
 * CSS2 without creating an additional inline element.</p>
 * <p>The class uses one helper class per OpenDocument style family
 * (paragraph, frame etc.)</p>
 */
class StyleConverter extends ConverterHelper {

    // Helpers for text styles
    private TextStyleConverter textSc;
    private ParStyleConverter parSc;
    private ListStyleConverter listSc;
    private SectionStyleConverter sectionSc;

    // Helpers for table styles
    private TableStyleConverter tableSc;
    private RowStyleConverter rowSc;
    private CellStyleConverter cellSc;

    // Helpers for drawing styles
    private FrameStyleConverter frameSc;
    private PresentationStyleConverter presentationSc;
	
    // Helper for page styles
    private PageStyleConverter pageSc;

    /** <p>Create a new <code>StyleConverter</code></p>
     */
    public StyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter);
        // Create the helpers
        textSc = new TextStyleConverter(ofr,config,converter,nType);
        parSc = new ParStyleConverter(ofr,config,converter,nType);
        listSc = new ListStyleConverter(ofr,config,converter,nType);
        sectionSc = new SectionStyleConverter(ofr,config,converter,nType);
        tableSc = new TableStyleConverter(ofr,config,converter,nType);
        rowSc = new RowStyleConverter(ofr,config,converter,nType);
        cellSc = new CellStyleConverter(ofr,config,converter,nType);
        frameSc = new FrameStyleConverter(ofr,config,converter,nType);
        presentationSc = new PresentationStyleConverter(ofr,config,converter,nType);
        pageSc = new PageStyleConverter(ofr,config,converter,nType);
    }
	
    // Accessor methods for helpers
    protected TextStyleConverter getTextSc() { return textSc; }

    protected ParStyleConverter getParSc() { return parSc; }

    protected ListStyleConverter getListSc() { return listSc; }

    protected SectionStyleConverter getSectionSc() { return sectionSc; }

    protected TableStyleConverter getTableSc() { return tableSc; }

    protected RowStyleConverter getRowSc() { return rowSc; }

    protected CellStyleConverter getCellSc() { return cellSc; }

    protected FrameStyleConverter getFrameSc() { return frameSc; }

    protected PresentationStyleConverter getPresentationSc() { return presentationSc; }

    protected PageStyleConverter getPageSc() { return pageSc; }
	
    private StyleWithProperties getDefaultStyle() {
        if (ofr.isSpreadsheet()) return ofr.getDefaultCellStyle();
        else if (ofr.isPresentation()) return ofr.getDefaultFrameStyle();
        else return ofr.getDefaultParStyle();
    }
	
    // Apply the default language
    public void applyDefaultLanguage(Element node) {
        StyleWithProperties style = getDefaultStyle();
        if (style!=null) {
            StyleInfo info = new StyleInfo();
            StyleConverterHelper.applyLang(style,info);
            applyStyle(info,node);
        }
    }
	
    // Export used styles to CSS
    public Node exportStyles(Document htmlDOM) {
        String sIndent = "      ";
		
        StringBuffer buf = new StringBuffer();
        
        // Export default style
        if (config.xhtmlCustomStylesheet().length()==0 &&
            (config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL ||
            config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD)) {
            // Default paragraph/cell/frame style is applied to the body element
            StyleWithProperties defaultStyle = getDefaultStyle();
            if (defaultStyle!=null) {
                CSVList props = new CSVList(";");
                // text properties only!
                getTextSc().cssTextCommon(defaultStyle,props,true);
                buf.append(sIndent)
                   .append("body {").append(props.toString()).append("}\n");
            }

        }
		
        // Export declarations from helpers
        // For OpenDocument documents created with OOo only some will generate content:
        //   Text documents: text, par, list, frame
        //   Spreadsheet documents: cell
        //   Presentation documents: frame, presentation, page
        buf.append(getTextSc().getStyleDeclarations(sIndent));
        buf.append(getParSc().getStyleDeclarations(sIndent));
        buf.append(getListSc().getStyleDeclarations(sIndent));
        buf.append(getSectionSc().getStyleDeclarations(sIndent));
        buf.append(getCellSc().getStyleDeclarations(sIndent));
        buf.append(getTableSc().getStyleDeclarations(sIndent));
        buf.append(getRowSc().getStyleDeclarations(sIndent));
        buf.append(getFrameSc().getStyleDeclarations(sIndent));
        buf.append(getPresentationSc().getStyleDeclarations(sIndent));
        buf.append(getPageSc().getStyleDeclarations(sIndent));
		
        // Create node
        if (buf.length()>0) {
            Element htmlStyle = htmlDOM.createElement("style");
            htmlStyle.setAttribute("media","all");
            htmlStyle.setAttribute("type","text/css");
            htmlStyle.appendChild(htmlDOM.createTextNode("\n"));
            htmlStyle.appendChild(htmlDOM.createTextNode(buf.toString()));
            htmlStyle.appendChild(htmlDOM.createTextNode("    "));
            return htmlStyle;
        }
        else {
            return null;
        }

    }
		
}