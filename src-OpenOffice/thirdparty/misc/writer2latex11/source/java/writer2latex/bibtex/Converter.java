/************************************************************************
 *
 *  Converter.java
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
 *  Copyright: 2001-2008 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  version 1.0 (2008-11-22)
 *
 */

package writer2latex.bibtex;

import writer2latex.api.Config;
//import writer2latex.api.ConverterResult;
import writer2latex.base.ConverterBase;
import writer2latex.latex.LaTeXConfig;
import writer2latex.office.BibMark;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

//import writer2latex.xmerge.ConvertData;
//import writer2latex.xmerge.OfficeDocument;

import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <p>BibTeX export</p>
 *
 * <p>This class extracts bibliographic information from an OpenDocument text file to a BibTeX data file.</p>
 *
 */
public final class Converter extends ConverterBase {
                        
    // Configuration - TODO: Doesn't really use it - should use some fake config
    private LaTeXConfig config;
	
    public Config getConfig() { return config; }
	
    // Constructor
    public Converter() {
        super();
        config = new LaTeXConfig();
    }
	
    /**
     *  <p>Convert the data passed into the <code>InputStream</code>
     *  into BibTeX format.</p>
     *
     *  @throws  IOException       If any I/O error occurs.
     */
    public void convertInner() throws IOException {      
        sTargetFileName = Misc.trimDocumentName(sTargetFileName,".bib");

        BibTeXDocument bibDoc = new BibTeXDocument(sTargetFileName);

        // Collect all text:bibliography-mark elements from the content
        Element doc = ofr.getContent();
        NodeList list;
        list = doc.getElementsByTagName(XMLString.TEXT_BIBLIOGRAPHY_MARK);
        int nLen = list.getLength();
        for (int i=0; i<nLen; i++) {
            String sIdentifier = Misc.getAttribute(list.item(i),XMLString.TEXT_IDENTIFIER);
            if (sIdentifier!=null && !bibDoc.containsKey(sIdentifier)) {
                bibDoc.put(new BibMark(list.item(i)));
            }            
        }
      
        // Add result
        convertData.addDocument(bibDoc);
    }

}