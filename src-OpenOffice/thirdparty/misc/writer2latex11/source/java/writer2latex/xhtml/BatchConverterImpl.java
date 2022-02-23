/************************************************************************
 *
 *  BatchConverterImpl.java
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
 
package writer2latex.xhtml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import java.text.Collator;
import org.w3c.dom.Element;

import writer2latex.api.IndexPageEntry;
import writer2latex.api.OutputFile;
import writer2latex.base.BatchConverterBase;

/**
 * Implementation of <code>writer2latex.api.BatchConverter</code> for
 * xhtml 1.0 strict
 */
public class BatchConverterImpl extends BatchConverterBase {
	
    private XhtmlConfig config;
    private XhtmlDocument template;
	
    private String sDefaultLang;
    private String sDefaultCountry;
    private L10n l10n;

    public BatchConverterImpl() {
        super();
        config = new XhtmlConfig();
        template = null;

        l10n = new L10n();
        sDefaultLang = System.getProperty("user.language");
        sDefaultCountry = System.getProperty("user.country");
        l10n.setLocale(sDefaultLang, sDefaultCountry);
    }
	
    // Implementation of the remaining (xhtml specific) parts of the interface

    public writer2latex.api.Config getConfig() {
        return config;
    }

    public void readTemplate(InputStream is) throws IOException {
        template = new XhtmlDocument("Template",XhtmlDocument.XHTML10);
        try {
            template.read(is);
        }
        catch (IOException e) {
            template = null;
            throw e;
        }
    }

    public void readTemplate(File file) throws IOException {
    	readTemplate(new FileInputStream(file));
    }

    protected String getIndexFileName() {
        return "index.html";
    }
	
    public OutputFile createIndexFile(String sHeading, IndexPageEntry[] entries) {
        // Create the index page (with header/footer or from template)
        XhtmlDocument htmlDoc = new XhtmlDocument("index",XhtmlDocument.XHTML10);
        htmlDoc.setEncoding(config.xhtmlEncoding());
        htmlDoc.setNoDoctype(config.xhtmlNoDoctype());
        htmlDoc.setAddBOM(config.xhtmlAddBOM());
        htmlDoc.setUseNamedEntities(config.useNamedEntities());
        if (template!=null) { htmlDoc.readFromTemplate(template); }
        else { htmlDoc.createHeaderFooter(); }

        org.w3c.dom.Document htmlDOM = htmlDoc.getContentDOM();

        // Declare charset (we need this for xhtml because we have no <?xml ... ?>)
        Element meta = htmlDOM.createElement("meta");
        meta.setAttribute("http-equiv","Content-Type");
        meta.setAttribute("content","text/html; charset="+htmlDoc.getEncoding().toLowerCase());
        htmlDoc.getHeadNode().appendChild(meta);
		
        // Add link to stylesheet
        if (config.xhtmlCustomStylesheet().length()>0) {
            Element htmlStyle = htmlDOM.createElement("link");
            htmlStyle.setAttribute("rel","stylesheet");
            htmlStyle.setAttribute("type","text/css");
            htmlStyle.setAttribute("media","all");
            htmlStyle.setAttribute("href",config.xhtmlCustomStylesheet());
            htmlDoc.getHeadNode().appendChild(htmlStyle);
        }

        // Add uplink to header and footer
        Element header = htmlDoc.getHeaderNode();
        if (header!=null) {
            if (config.getXhtmlUplink().length()>0) {
                Element a = htmlDOM.createElement("a");
                a.setAttribute("href",config.getXhtmlUplink());
                a.appendChild(htmlDOM.createTextNode(l10n.get(L10n.UP)));
                header.appendChild(a);
            }
            else {
                header.appendChild(htmlDOM.createTextNode(l10n.get(L10n.UP)));
            }
        }
		
        Element footer = htmlDoc.getFooterNode();
        if (footer!=null) {
            if (config.getXhtmlUplink().length()>0) {
                Element a = htmlDOM.createElement("a");
                a.setAttribute("href",config.getXhtmlUplink());
                a.appendChild(htmlDOM.createTextNode(l10n.get(L10n.UP)));
                footer.appendChild(a);
            }
            else {
                footer.appendChild(htmlDOM.createTextNode(l10n.get(L10n.UP)));
            }
        }
		
        // Add title and heading
        htmlDoc.getTitleNode().appendChild(htmlDOM.createTextNode(sHeading));
        Element h1 = htmlDOM.createElement("h1");
        htmlDoc.getContentNode().appendChild(h1);
        h1.appendChild(htmlDOM.createTextNode(sHeading));
		
        // Sort the entries
        int nLen = entries.length;
        Collator collator = Collator.getInstance(new Locale(sDefaultLang,sDefaultCountry));
        for (int i = 0; i<nLen; i++) {
            if (entries[i]!=null) {
                for (int j = i+1; j<nLen ; j++) {
                    if (entries[j]!=null) {
                        IndexPageEntry entryi = entries[i];
                        IndexPageEntry entryj = entries[j];
                        if (collator.compare(entryi.getDisplayName(), entryj.getDisplayName()) > 0) {
                            entries[i] = entryj;
                            entries[j] = entryi;
                        }
                    }
                }
            }
        }
		
        // Insert directory entries
        boolean bUseIcon = config.getXhtmlDirectoryIcon().length()>0;
        for (int i=0; i<nLen; i++) {
            if (entries[i]!=null && entries[i].isDirectory()) {
                Element p = htmlDOM.createElement("p");
                htmlDoc.getContentNode().appendChild(p);
                if (bUseIcon) {
                    Element img = htmlDOM.createElement("img");
                    p.appendChild(img);
                    img.setAttribute("src",config.getXhtmlDirectoryIcon());
                    img.setAttribute("alt",l10n.get(L10n.DIRECTORY));
                    p.appendChild(htmlDOM.createTextNode(" "));
                }
                Element a = htmlDOM.createElement("a");
                p.appendChild(a);
                a.setAttribute("href",entries[i].getFile());
                a.appendChild(htmlDOM.createTextNode(entries[i].getDisplayName()));
            }
        }
		
        // Insert document entries
        bUseIcon = config.getXhtmlDocumentIcon().length()>0;
        for (int i=0; i<nLen; i++) {
            if (entries[i]!=null && !entries[i].isDirectory()) {
                Element p = htmlDOM.createElement("p");
                htmlDoc.getContentNode().appendChild(p);
                if (bUseIcon) {
                    Element img = htmlDOM.createElement("img");
                    p.appendChild(img);
                    img.setAttribute("src",config.getXhtmlDocumentIcon());
                    img.setAttribute("alt",l10n.get(L10n.DOCUMENT));
                    p.appendChild(htmlDOM.createTextNode(" "));
                }
                // Add link to html file
                if (entries[i].getFile()!=null) {
                	Element a = htmlDOM.createElement("a");
                	p.appendChild(a);
                	a.setAttribute("href",entries[i].getFile());
                	a.appendChild(htmlDOM.createTextNode(entries[i].getDisplayName()));
                }
                else {
                	p.appendChild(htmlDOM.createTextNode(entries[i].getDisplayName()));
                }
                // Add link to pdf file
                if (entries[i].getPdfFile()!=null) {
                    p.appendChild(htmlDOM.createTextNode(" "));
                    Element pdfa = htmlDOM.createElement("a");
                    p.appendChild(pdfa);
                    pdfa.setAttribute("href",entries[i].getPdfFile());
                    pdfa.appendChild(htmlDOM.createTextNode("pdf"));
                }
                // TODO: Add link to original file if defined
                // Add description if available
                if (entries[i].getDescription()!=null) {
                	p.appendChild(htmlDOM.createTextNode(": "+entries[i].getDescription()));
                }
            }
        }
		
        return htmlDoc;
    }

}
