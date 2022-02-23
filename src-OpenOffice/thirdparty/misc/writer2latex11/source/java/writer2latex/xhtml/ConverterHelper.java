/************************************************************************
 *
 *  ConverterHelper.java
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

import org.w3c.dom.Element;

import writer2latex.office.OfficeReader;

public class ConverterHelper {
    protected OfficeReader ofr;
    protected XhtmlConfig config;
    protected Converter converter;
	
    protected StyleConverter getStyleCv() { return converter.getStyleCv(); }

    protected TextStyleConverter getTextSc() { return converter.getStyleCv().getTextSc(); }
	
    protected ParStyleConverter getParSc() { return converter.getStyleCv().getParSc(); }
	
    protected ListStyleConverter getListSc() { return converter.getStyleCv().getListSc(); }
	
    protected SectionStyleConverter getSectionSc() { return converter.getStyleCv().getSectionSc(); }
	
    protected TableStyleConverter getTableSc() { return converter.getStyleCv().getTableSc(); }
	
    protected RowStyleConverter getRowSc() { return converter.getStyleCv().getRowSc(); }
	
    protected CellStyleConverter getCellSc() { return converter.getStyleCv().getCellSc(); }
	
    protected FrameStyleConverter getFrameSc() { return converter.getStyleCv().getFrameSc(); }
	
    protected PresentationStyleConverter getPresentationSc() { return converter.getStyleCv().getPresentationSc(); }
	
    protected PageStyleConverter getPageSc() { return converter.getStyleCv().getPageSc(); }
	
    protected TextConverter getTextCv() { return converter.getTextCv(); }
	
    protected TableConverter getTableCv() { return converter.getTableCv(); }

    protected DrawConverter getDrawCv() { return converter.getDrawCv(); }

    protected MathConverter getMathCv() { return converter.getMathCv(); }
	
    // TODO: Move to StyleInfo!
    protected void applyStyle(StyleInfo info, Element hnode) {
        if (info.sClass!=null) {
            hnode.setAttribute("class",info.sClass);
        }
        if (!info.props.isEmpty()) {
            hnode.setAttribute("style",info.props.toString());
        }
        if (info.sLang!=null) {
            hnode.setAttribute("xml:lang",info.sLang);
            if (converter.getType()==XhtmlDocument.XHTML10) {
                hnode.setAttribute("lang",info.sLang); // HTML4 compatibility
            }
        }
        if (info.sDir!=null) {
            hnode.setAttribute("dir",info.sDir);
        }
    }

    public ConverterHelper(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        this.ofr = ofr;
        this.config = config;
        this.converter = converter;
    }
}
