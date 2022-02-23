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
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2009-03-02)
 *
 */

package writer2latex.xhtml;

import java.io.File;
import java.io.FileInputStream;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;

import java.io.InputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import writer2latex.api.Config;
import writer2latex.api.ConverterFactory;
//import writer2latex.api.ConverterResult;
import writer2latex.base.ConverterBase;
//import writer2latex.latex.LaTeXDocumentPortion;
//import writer2latex.latex.util.Context;
import writer2latex.office.MIMETypes;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.ExportNameCollection;
import writer2latex.util.Misc;

/**
 * <p>This class converts an OpenDocument file to an XHTML(+MathML) document<.</p>
 *
 */
public class Converter extends ConverterBase {
    // Config
    private XhtmlConfig config;
	
    public Config getConfig() { return config; }

    // The locale
    private L10n l10n;
                        
    // The helpers
    private StyleConverter styleCv;
    private TextConverter textCv;
    private TableConverter tableCv;
    private DrawConverter drawCv;
    private MathConverter mathCv;
	
    // The template
    private XhtmlDocument template = null;

    // The xhtml output file(s)
    protected int nType = XhtmlDocument.XHTML10; // the doctype
    Vector outFiles;
    private int nOutFileIndex;
    private XhtmlDocument htmlDoc; // current outfile
    private Document htmlDOM; // current DOM, usually within htmlDoc
    private boolean bNeedHeaderFooter = false;

    // Hyperlinks
    Hashtable targets = new Hashtable();
    LinkedList links = new LinkedList();
    // Strip illegal characters from internal hyperlink targets
    private ExportNameCollection targetNames = new ExportNameCollection(true);
    
    // Constructor setting the DOCTYPE
    public Converter(int nType) {
        super();
        config = new XhtmlConfig();
        this.nType = nType;
    }

    // override
    public void readTemplate(InputStream is) throws IOException {
        template = new XhtmlDocument("Template",nType);
        template.read(is);
    }
	
    public void readTemplate(File file) throws IOException {
        readTemplate(new FileInputStream(file));
    }

    protected StyleConverter getStyleCv() { return styleCv; }
	
    protected TextConverter getTextCv() { return textCv; }
	
    protected TableConverter getTableCv() { return tableCv; }

    protected DrawConverter getDrawCv() { return drawCv; }

    protected MathConverter getMathCv() { return mathCv; }
	
    protected int getType() { return nType; }
	
    protected int getOutFileIndex() { return nOutFileIndex; }
	
    protected Element createElement(String s) { return htmlDOM.createElement(s); }
	
    protected Text createTextNode(String s) { return htmlDOM.createTextNode(s); }
	
    protected Node importNode(Node node, boolean bDeep) { return htmlDOM.importNode(node,bDeep); }
	
    protected L10n getL10n() { return l10n; }
	
    // override
    public void convertInner() throws IOException {      
        sTargetFileName = Misc.trimDocumentName(sTargetFileName,XhtmlDocument.getExtension(nType));
		
        outFiles = new Vector();
        nOutFileIndex = -1;

        bNeedHeaderFooter = ofr.isSpreadsheet() || ofr.isPresentation() || config.getXhtmlSplitLevel()>0 || config.getXhtmlUplink().length()>0;

        l10n = new L10n();
        
        imageLoader.setUseSubdir(config.saveImagesInSubdir());

        imageLoader.setDefaultFormat(MIMETypes.PNG);
        imageLoader.addAcceptedFormat(MIMETypes.JPEG);
        imageLoader.addAcceptedFormat(MIMETypes.GIF);

        styleCv = new StyleConverter(ofr,config,this,nType);
        textCv = new TextConverter(ofr,config,this);
        tableCv = new TableConverter(ofr,config,this);
        drawCv = new DrawConverter(ofr,config,this);
        mathCv = new MathConverter(ofr,config,this,nType!=XhtmlDocument.XHTML10);

        // Set locale to document language
        StyleWithProperties style = ofr.isSpreadsheet() ? ofr.getDefaultCellStyle() : ofr.getDefaultParStyle();
        if (style!=null) {
            String sLang = style.getProperty(XMLString.FO_LANGUAGE);
            String sCountry = style.getProperty(XMLString.FO_COUNTRY);
            if (sLang!=null) {
                if (sCountry==null) { l10n.setLocale(sLang); }
                else { l10n.setLocale(sLang+"-"+sCountry); }
            }
        }

		//NodeList list;
        // Traverse the body
        Element body = ofr.getContent();
        if (ofr.isSpreadsheet()) { tableCv.convertTableContent(body); }
        else if (ofr.isPresentation()) { drawCv.convertDrawContent(body); }
        else { textCv.convertTextContent(body); }
		
        // Add footnotes and endnotes
        textCv.insertFootnotes(htmlDoc.getContentNode());
        textCv.insertEndnotes(htmlDoc.getContentNode());

        // Resolve links
        ListIterator iter = links.listIterator();
        while (iter.hasNext()) {
            LinkDescriptor ld = (LinkDescriptor) iter.next();
            Integer targetIndex = (Integer) targets.get(ld.sId);
            if (targetIndex!=null) {
                int nTargetIndex = targetIndex.intValue();
                if (nTargetIndex == ld.nIndex) { // same file
                    ld.element.setAttribute("href","#"+targetNames.getExportName(ld.sId));
                }
                else {
                    ld.element.setAttribute("href",getOutFileName(nTargetIndex,true)
                                            +"#"+targetNames.getExportName(ld.sId));
                }
            }
        }

        // Export styles (temp.)
        for (int i=0; i<=nOutFileIndex; i++) {
            Document dom = ((XhtmlDocument) outFiles.get(i)).getContentDOM();
            NodeList hlist = dom.getElementsByTagName("head");
            Node styles = styleCv.exportStyles(dom);
            if (styles!=null) {
                hlist.item(0).appendChild(styles);
            }
        }
        
        // Create headers & footers (if nodes are available)
        if (ofr.isSpreadsheet()) {
            for (int i=0; i<=nOutFileIndex; i++) {

                XhtmlDocument doc = (XhtmlDocument) outFiles.get(i);
                Document dom = doc.getContentDOM();
                Element header = doc.getHeaderNode();
                Element footer = doc.getFooterNode();
                Element headerPar = dom.createElement("p");
                Element footerPar = dom.createElement("p");
                footerPar.setAttribute("style","clear:both"); // no floats may pass!

                // Add uplink
                if (config.getXhtmlUplink().length()>0) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    headerPar.appendChild(a);
                    headerPar.appendChild(dom.createTextNode(" "));
                    a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    footerPar.appendChild(a);
                    footerPar.appendChild(dom.createTextNode(" "));
                }
                // Add links to all sheets:
                int nSheets = tableCv.sheetNames.size();
                for (int j=0; j<nSheets; j++) {
                    if (config.xhtmlCalcSplit()) {
                        addNavigationLink(dom,headerPar,(String) tableCv.sheetNames.get(j),j);
                        addNavigationLink(dom,footerPar,(String) tableCv.sheetNames.get(j),j);
                    }
                    else {
                        addInternalNavigationLink(dom,headerPar,(String) tableCv.sheetNames.get(j),"tableheading"+j);
                        addInternalNavigationLink(dom,footerPar,(String) tableCv.sheetNames.get(j),"tableheading"+j);
	                }
                }
                
                if (header!=null) { header.appendChild(headerPar); }
                if (footer!=null) { footer.appendChild(footerPar); }
            }
        }
        else if (ofr.isPresentation() || config.getXhtmlSplitLevel()>0) {
            for (int i=0; i<=nOutFileIndex; i++) {
                XhtmlDocument doc = (XhtmlDocument) outFiles.get(i);
                Document dom = doc.getContentDOM();
                //Element content = doc.getContentNode();

                // Header links
                Element header = doc.getHeaderNode();
                if (header!=null) {
                    if (ofr.isPresentation()) {
                        // Absolute placement in presentations (quick and dirty solution)
                        header.setAttribute("style","position:absolute;top:0;left:0");
                    }
                    if (config.getXhtmlUplink().length()>0) {
                        Element a = dom.createElement("a");
                        a.setAttribute("href",config.getXhtmlUplink());
                        a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                        header.appendChild(a);
                        header.appendChild(dom.createTextNode(" "));
                    }
                    addNavigationLink(dom,header,l10n.get(L10n.FIRST),0);
                    addNavigationLink(dom,header,l10n.get(L10n.PREVIOUS),i-1);
                    addNavigationLink(dom,header,l10n.get(L10n.NEXT),i+1);
                    addNavigationLink(dom,header,l10n.get(L10n.LAST),nOutFileIndex);
                    if (textCv.getTocIndex()>=0) {
                        addNavigationLink(dom,header,l10n.get(L10n.CONTENTS),textCv.getTocIndex());
                    }
                    if (textCv.getAlphabeticalIndex()>=0) {
                        addNavigationLink(dom,header,l10n.get(L10n.INDEX),textCv.getAlphabeticalIndex());
                    }
                }

                // Footer links
                Element footer = doc.getFooterNode();
                if (footer!=null && !ofr.isPresentation()) {
                    // No footer in presentations
                    if (config.getXhtmlUplink().length()>0) {
                        Element a = dom.createElement("a");
                        a.setAttribute("href",config.getXhtmlUplink());
                        a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                        footer.appendChild(a);
                        footer.appendChild(dom.createTextNode(" "));
                    }
                    addNavigationLink(dom,footer,l10n.get(L10n.FIRST),0);
                    addNavigationLink(dom,footer,l10n.get(L10n.PREVIOUS),i-1);
                    addNavigationLink(dom,footer,l10n.get(L10n.NEXT),i+1);
                    addNavigationLink(dom,footer,l10n.get(L10n.LAST),nOutFileIndex);
                    if (textCv.getTocIndex()>=0) {
                        addNavigationLink(dom,footer,l10n.get(L10n.CONTENTS),textCv.getTocIndex());
                    }
                    if (textCv.getAlphabeticalIndex()>=0) {
                        addNavigationLink(dom,footer,l10n.get(L10n.INDEX),textCv.getAlphabeticalIndex());
                    }
                }
            }
        }
        else if (config.getXhtmlUplink().length()>0) {
            for (int i=0; i<=nOutFileIndex; i++) {
                XhtmlDocument doc = (XhtmlDocument) outFiles.get(i);
                Document dom = doc.getContentDOM();
                //Element content = doc.getContentNode();

                Element header = doc.getHeaderNode();
                if (header!=null) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    header.appendChild(a);
                    header.appendChild(dom.createTextNode(" "));
                }

                Element footer = doc.getFooterNode();
                if (footer!=null) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    footer.appendChild(a);
                    footer.appendChild(dom.createTextNode(" "));
                }
            }
        }

    }
	
    private void addNavigationLink(Document dom, Node node, String s, int nIndex) {
        if (nIndex>=0 && nIndex<=nOutFileIndex) {
            Element a = dom.createElement("a");
            a.setAttribute("href",Misc.makeHref(getOutFileName(nIndex,true)));
            a.appendChild(dom.createTextNode(s));
            //node.appendChild(dom.createTextNode("["));
            node.appendChild(a);
            node.appendChild(dom.createTextNode(" "));
            //node.appendChild(dom.createTextNode("] "));
        }
        else {
            Element span = dom.createElement("span");
            span.setAttribute("class","nolink");
            node.appendChild(span);
            span.appendChild(dom.createTextNode(s));
            node.appendChild(dom.createTextNode(" "));
            //node.appendChild(dom.createTextNode("["+s+"] "));
        }
    }
	
    private void addInternalNavigationLink(Document dom, Node node, String s, String sLink) {
        Element a = dom.createElement("a");
        a.setAttribute("href","#"+sLink);
        a.appendChild(dom.createTextNode(s));
        //node.appendChild(dom.createTextNode("["));
        node.appendChild(a);
        node.appendChild(dom.createTextNode(" "));
        //node.appendChild(dom.createTextNode("] "));
    }
    
    /* get inline text, ignoring any draw objects, footnotes, formatting and hyperlinks */
    private String getPlainInlineText(Node node) {
    	StringBuffer buf = new StringBuffer();
        Node child = node.getFirstChild();
        while (child!=null) {
            short nodeType = child.getNodeType();
               
            switch (nodeType) {
                case Node.TEXT_NODE:
                    buf.append(child.getNodeValue());
                    break;
                        
                case Node.ELEMENT_NODE:
                    String sName = child.getNodeName();
                    if (sName.equals(XMLString.TEXT_S)) {
                           buf.append(" ");
                    }
                    else if (sName.equals(XMLString.TEXT_TAB_STOP) || sName.equals(XMLString.TEXT_TAB)) { // text:tab in oasis
                        buf.append(" ");
                    }
                    else if (OfficeReader.isNoteElement(child)) {
                        // ignore
                    }
                    else if (OfficeReader.isTextElement(child)) {
                    	buf.append(getPlainInlineText(child));
                    }
                    break;
                default:
                    // Do nothing
            }
            child = child.getNextSibling();
        }
        return buf.toString();
    }


    public void handleOfficeAnnotation(Node onode, Node hnode) {
        if (config.xhtmlNotes()) {
            // Extract the text from the paragraphs, seperate paragraphs with newline
        	StringBuffer buf = new StringBuffer();
        	Node child = onode.getFirstChild();
        	while (child!=null) {
        		if (Misc.isElement(child, XMLString.TEXT_P)) {
        			if (buf.length()>0) { buf.append('\n'); }
        			buf.append(getPlainInlineText(child));
        		}
        		child = child.getNextSibling();
        	}
            Node commentNode = htmlDOM.createComment(buf.toString());
            hnode.appendChild(commentNode);
        }
    }
	
    /////////////////////////////////////////////////////////////////////////
    // UTILITY METHODS
	
    // Create output file name (docname.html, docname1.html, docname2.html etc.)
    public String getOutFileName(int nIndex, boolean bWithExt) {
        return sTargetFileName + (nIndex>0 ? Integer.toString(nIndex) : "") 
                               + (bWithExt ? htmlDoc.getFileExtension() : "");
    }
	
    // Return true if the current outfile has a non-empty body
    public boolean outFileHasContent() {
        return htmlDoc.getContentNode().hasChildNodes();
    }
	
    // Use another document. TODO: This is very ugly; clean it up!!!
    public void changeOutFile(int nIndex) {
        nOutFileIndex = nIndex;
        htmlDoc = (XhtmlDocument) outFiles.get(nIndex);
        htmlDOM = htmlDoc.getContentDOM();
    }
	
    public Element getPanelNode() {
        return htmlDoc.getPanelNode();
    }
	
    // Prepare next output file
    public Element nextOutFile() {
        if (nOutFileIndex>=0) { textCv.insertFootnotes(htmlDoc.getContentNode()); }
        htmlDoc = new XhtmlDocument(getOutFileName(++nOutFileIndex,false),nType);
        htmlDoc.setEncoding(config.xhtmlEncoding());
        htmlDoc.setNoDoctype(config.xhtmlNoDoctype());
        htmlDoc.setAddBOM(config.xhtmlAddBOM());
        htmlDoc.setUseNamedEntities(config.useNamedEntities());
        htmlDoc.setXsltPath(config.getXsltPath());
        if (template!=null) { htmlDoc.readFromTemplate(template); }
        else if (bNeedHeaderFooter) { htmlDoc.createHeaderFooter(); }
        outFiles.add(nOutFileIndex,htmlDoc);
        convertData.addDocument(htmlDoc);
        
        // Create head + body
        htmlDOM = htmlDoc.getContentDOM();
        Element rootElement = htmlDOM.getDocumentElement();
        styleCv.applyDefaultLanguage(rootElement);
        rootElement.insertBefore(htmlDOM.createComment(
             "This file was converted to xhtml by "
             + (ofr.isText() ? "Writer" : (ofr.isSpreadsheet() ? "Calc" : "Impress"))
             + "2xhtml ver. " + ConverterFactory.getVersion() +
             ". See http://writer2latex.sourceforge.net for more info."),
             rootElement.getFirstChild());
		
        // Apply page formatting (using first master page)
        if (ofr.getFirstMasterPage()!=null && !ofr.isPresentation()) {
            StyleInfo pageInfo = new StyleInfo();
            styleCv.getPageSc().applyStyle(ofr.getFirstMasterPage().getName(),pageInfo);
            styleCv.getPageSc().applyStyle(pageInfo,htmlDoc.getContentNode());
        }

        // Add title (required by xhtml)
        String sTitle = metaData.getTitle();
        if (sTitle==null) { // use filename as fallback
            sTitle = htmlDoc.getFileName();
        }
        htmlDoc.getTitleNode().appendChild( htmlDOM.createTextNode(sTitle) );

        // Declare charset (we need this for xhtml because we have no <?xml ... ?>)
        if (nType==XhtmlDocument.XHTML10) {
            Element meta = htmlDOM.createElement("meta");
            meta.setAttribute("http-equiv","Content-Type");
            meta.setAttribute("content","text/html; charset="+htmlDoc.getEncoding().toLowerCase());
            htmlDoc.getHeadNode().appendChild(meta);
        }
		
        // "Traditional" meta data
        //createMeta("generator","Writer2LaTeX "+Misc.VERSION);
        createMeta("description",metaData.getDescription());
        createMeta("keywords",metaData.getKeywords());

        // Dublin core meta data (optional)
        // Format as recommended on dublincore.org
        // Declare meta data profile
        if (config.xhtmlUseDublinCore()) {
            htmlDoc.getHeadNode().setAttribute("profile","http://dublincore.org/documents/dcq-html/");
            // Add link to declare namespace
            Element dclink = htmlDOM.createElement("link");
            dclink.setAttribute("rel","schema.DC");
            dclink.setAttribute("href","http://purl.org/dc/elements/1.1/");
            htmlDoc.getHeadNode().appendChild(dclink);
            // Insert the actual meta data
            createMeta("DC.title",metaData.getTitle());
            // DC.subject actually contains subject+keywords, so we merge them
            String sDCSubject = metaData.getSubject();
            if (metaData.getSubject().length()>0 && metaData.getKeywords().length()>0) {
                sDCSubject+=", ";
            }
            sDCSubject+=metaData.getKeywords();
            createMeta("DC.subject",sDCSubject);
            createMeta("DC.description",metaData.getDescription());
            createMeta("DC.creator",metaData.getCreator());
            createMeta("DC.date",metaData.getDate());
            createMeta("DC.language",metaData.getLanguage());
        }
        
        // Add link to stylesheet
        if (config.xhtmlCustomStylesheet().length()>0) {
            Element htmlStyle = htmlDOM.createElement("link");
            htmlStyle.setAttribute("rel","stylesheet");
            htmlStyle.setAttribute("type","text/css");
            htmlStyle.setAttribute("media","all");
            htmlStyle.setAttribute("href",config.xhtmlCustomStylesheet());
            htmlDoc.getHeadNode().appendChild(htmlStyle);
        }
        /* later....
        if (nSplit>0 && !config.xhtmlIgnoreStyles()) {
            Element htmlStyle = htmlDOM.createElement("link");
            htmlStyle.setAttribute("rel","stylesheet");
            htmlStyle.setAttribute("type","text/css");
            htmlStyle.setAttribute("media","all");
            htmlStyle.setAttribute("href",oooDoc.getName()+"-styles.css");
            htmlHead.appendChild(htmlStyle);
        }*/
        // Note: For single output file, styles are exported to the doc at the end.

        // Recreate nested sections, if any
        if (!textCv.sections.isEmpty()) {
            Iterator iter = textCv.sections.iterator();
            while (iter.hasNext()) {
                Element section = (Element) iter.next();
                String sStyleName = Misc.getAttribute(section,XMLString.TEXT_STYLE_NAME);
                Element div = htmlDOM.createElement("div");
                htmlDoc.getContentNode().appendChild(div);
                htmlDoc.setContentNode(div);
                StyleInfo sectionInfo = new StyleInfo();
                styleCv.getSectionSc().applyStyle(sStyleName,sectionInfo);
                styleCv.getSectionSc().applyStyle(sectionInfo,div);
            }
        }
        
        return htmlDoc.getContentNode();
    }
	
    // create a target
    public Element createTarget(String sId) {
        Element a = htmlDOM.createElement("a");
        a.setAttribute("id",targetNames.getExportName(sId));
        targets.put(sId, new Integer(nOutFileIndex));
        return a;
    }
	
    // put a target id on an existing element
    public void addTarget(Element node,String sId) {
        node.setAttribute("id",targetNames.getExportName(sId));
        targets.put(sId, new Integer(nOutFileIndex));
    }

    // create an internal link
    public Element createLink(String sId) {
        Element a = htmlDOM.createElement("a");
        LinkDescriptor ld = new LinkDescriptor();
        ld.element = a; ld.sId = sId; ld.nIndex = nOutFileIndex;
        links.add(ld);
        return a;
    }

    // create a link
    public Element createLink(Element onode) {
        // First create the anchor
        String sHref = onode.getAttribute(XMLString.XLINK_HREF);
        Element anchor;
        if (sHref.startsWith("#")) { // internal link
            anchor = createLink(sHref.substring(1));
        }
        else { // external link
            anchor = htmlDOM.createElement("a");
	
            // Workaround for an OOo problem:
            if (sHref.indexOf("?")==-1) { // No question mark
                int n3F=sHref.indexOf("%3F");
                if (n3F>0) { // encoded question mark
                    sHref = sHref.substring(0,n3F)+"?"+sHref.substring(n3F+3);
                }
            }
            anchor.setAttribute("href",sHref);
            String sName = Misc.getAttribute(onode,XMLString.OFFICE_NAME);
            if (sName!=null) {
                anchor.setAttribute("name",sName);
                anchor.setAttribute("title",sName); // OOo does not have title...
            }
            // TODO: target has been deprecated in xhtml 1.0 strict
            String sTarget = Misc.getAttribute(onode,XMLString.OFFICE_TARGET_FRAME_NAME);
            if (sTarget!=null) { anchor.setAttribute("target",sTarget); }
        }

        // Then style it
        String sStyleName = onode.getAttribute(XMLString.TEXT_STYLE_NAME);
        String sVisitedStyleName = onode.getAttribute(XMLString.TEXT_VISITED_STYLE_NAME);
        StyleInfo anchorInfo = new StyleInfo();
        styleCv.getTextSc().applyAnchorStyle(sStyleName,sVisitedStyleName,anchorInfo);
        styleCv.getTextSc().applyStyle(anchorInfo,anchor);

        return anchor;
    }

	
    private void createMeta(String sName, String sValue) {
        if (sValue==null) { return; }
        Element meta = htmlDOM.createElement("meta");
        meta.setAttribute("name",sName);
        meta.setAttribute("content",sValue);
        htmlDoc.getHeadNode().appendChild(meta);
    }
	
		
}