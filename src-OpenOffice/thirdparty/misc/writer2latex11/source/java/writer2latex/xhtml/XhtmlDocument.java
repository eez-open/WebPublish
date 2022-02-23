/************************************************************************
 *
 *  XhtmlDocument.java
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
 *  Version 1.0 (2009-02-19)
 *
 */
 
 //TODO: Remove redundant lang and dir attributes
 //TODO: Add named entities outside ISO-latin 1

package writer2latex.xhtml;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
//import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.DOMImplementation;
//import org.xml.sax.SAXException;
//import org.xml.sax.SAXParseException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.ParserConfigurationException;

import writer2latex.xmerge.DOMDocument;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;

/**
 *  An implementation of <code>Document</code> for
 *  XHTML documents.
 */
public class XhtmlDocument extends DOMDocument {

    /** Constant to identify XHTML 1.0 strict documents */
    public static final int XHTML10 = 0;
	
    /** Constant to identify XHTML 1.1 documents (not used currently) */
    public static final int XHTML11 = 1;
    
    /** Constant to identify XHTML + MathML documents */
    public static final int XHTML_MATHML = 2;

    /** Constant to identify XHTML + MathML documents using the xsl transformations
     *  from w3c's math working group (http://www.w3.org/Math/XSL/)
     */
    public static final int XHTML_MATHML_XSL = 3;
	
    private static final String[] sExtension = { ".html", ".html", ".xhtml", ".xml" };

    private static final String[] sEmpty = { "base", "meta", "link", "hr", "br", "param", "img", "area", "input", "col" };
	
    private static String[] entities; // Not convenient to define directly due to a lot of null values

    // Type of document
    private int nType;

    // Configuration
    private String sEncoding = "UTF-8";	
    private boolean bUseNamedEntities = false;
    private char cLimit = 65535;
    private boolean bNoDoctype = false;
    private boolean bAddBOM = false;
    private String sXsltPath = "";
	
    // Content
    private Element headNode = null;
    private Element bodyNode = null;
    private Element titleNode = null;
    private Element contentNode = null;
    private Element panelNode = null;
    private Element headerNode = null;
    private Element footerNode = null;
    
    public static final String getExtension(int nType) {
        return sExtension[nType];
    } 

    /**
     *  Constructor. This constructor also creates the DOM (minimal: root, head,
     *  title and body node only) - unlike the constructors in
     *  writer2latex.xmerge.DOMDocument.
     *  @param  name  <code>Document</code> name.
     *  @param  nType the type of document
     */
    public XhtmlDocument(String name, int nType) {
        super(name,sExtension[nType]);
        this.nType = nType;
        // Define publicId and systemId
        String sPublicId = null;
        String sSystemId = null;		
        switch (nType) {
            case XHTML10 :
                sPublicId = "-//W3C//DTD XHTML 1.0 Strict//EN";
                sSystemId = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";
                break; 
            case XHTML11 :
                sPublicId = "-//W3C//DTD XHTML 1.1//EN";
                sSystemId = "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd";
                break; 
            case XHTML_MATHML :
            case XHTML_MATHML_XSL : 
                sPublicId = "-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN";
                sSystemId = "http://www.w3.org/Math/DTD/mathml2/xhtml-math11-f.dtd";
                //sSystemId = "http://www.w3.org/TR/MathML2/dtd/xhtml-math11-f.dtd"; (old version)
                /* An alternative is to use XHTML + MathML + SVG:
                sPublicId = "-//W3C//DTD XHTML 1.1 plus MathML 2.0 plus SVG 1.1//EN",
                sSystemId = "http://www.w3.org/2002/04/xhtml-math-svg/xhtml-math-svg.dtd"); */
        }

        // create DOM
        Document contentDOM = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            DocumentType doctype = domImpl.createDocumentType("html", sPublicId, sSystemId); 
            contentDOM = domImpl.createDocument("http://www.w3.org/1999/xhtml","html",doctype);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        contentDOM.getDocumentElement().setAttribute("xmlns","http://www.w3.org/1999/xhtml");
        // add head, title and body
        headNode = contentDOM.createElement("head");
        titleNode = contentDOM.createElement("title");
        bodyNode = contentDOM.createElement("body");
        contentDOM.getDocumentElement().appendChild(headNode);
        headNode.appendChild(titleNode);
        contentDOM.getDocumentElement().appendChild(bodyNode);
        contentNode = bodyNode;
        setContentDOM(contentDOM);
        
    }
	
    public Element getHeadNode() { return headNode; }
	
    public Element getBodyNode() { return bodyNode; }
	
    public Element getTitleNode() { return titleNode; }
	
    public Element getContentNode() { return contentNode; }
	
    public void setContentNode(Element contentNode) { this.contentNode = contentNode; }
	
    public Element getPanelNode() { return panelNode; }
	
    public Element getHeaderNode() { return headerNode; }
	
    public Element getFooterNode() { return footerNode; }
	
    public void createHeaderFooter() {
        headerNode = getContentDOM().createElement("div");
        headerNode.setAttribute("id","header");
        bodyNode.appendChild(headerNode);
        contentNode = getContentDOM().createElement("div");
        contentNode.setAttribute("id","content");
        bodyNode.appendChild(contentNode);
        footerNode = getContentDOM().createElement("div");
        footerNode.setAttribute("id","footer");
        bodyNode.appendChild(footerNode);
    }
	
    public void setContentDOM(Document doc) {
        super.setContentDOM(doc);
        collectNodes();
    }
	
    public void read(InputStream is) throws IOException {
        super.read(is);
        collectNodes();
    }
	
    public void readFromTemplate(XhtmlDocument template) {
        // Remove all current child nodes
        Element root = getContentDOM().getDocumentElement();
        Node child = root.getFirstChild();
        while (child!=null) {
            root.removeChild(child);
            child = root.getFirstChild();
        }

        // Import all child nodes from template
        Element templateRoot = template.getContentDOM().getDocumentElement();
        NodeList children = templateRoot.getChildNodes();
        int nLen = children.getLength();
        for (int i=0; i<nLen; i++) {
            root.appendChild(getContentDOM().importNode(children.item(i),true));
        }

        // get the entry point nodes
        collectNodes();
    }
	
    private void collectNodes(Element elm) {
        String sTagName = elm.getTagName();
        if ("head".equals(sTagName)) {
            headNode = elm;
        }
        else if ("body".equals(sTagName)) {
            bodyNode = elm;
        }
        else if ("title".equals(sTagName)) {
            titleNode = elm;
        }
        else if ("div".equals(sTagName)) {
            String sId = elm.getAttribute("id");
            if ("content".equals(sId)) { contentNode = elm; }
            else if ("header".equals(sId)) { headerNode = elm; }
            else if ("footer".equals(sId)) { footerNode = elm; }
            else if ("panel".equals(sId)) { panelNode = elm; }
        }
		
        Node child = elm.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                collectNodes((Element)child);
            }
            child = child.getNextSibling();
        }
    }
	
    private void collectNodes() {
        headNode = null;
        bodyNode = null;
        titleNode = null;
        contentNode = null;
        headerNode = null;
        footerNode = null;
        panelNode = null;

        Element elm = getContentDOM().getDocumentElement();
        collectNodes(elm);
        if (contentNode==null) { contentNode = bodyNode; }
        if (titleNode==null) {
            titleNode = getContentDOM().createElement("title");
            headNode.appendChild(titleNode);
        }
    }
	
    public void setEncoding(String s) {
        s = s.toUpperCase();
        if ("UTF-16".equals(s)) {
            sEncoding = s;
            cLimit = 65535;
        }
        else if ("ISO-8859-1".equals(s)) {
            sEncoding = s;
            cLimit = 255;
        }
        else if ("US-ASCII".equals(s)) {
            sEncoding = s;
            cLimit = 127;
        }
        else {
            sEncoding = "UTF-8";
            cLimit = 65535;
        }
    }
	
    public String getEncoding() { return sEncoding; }
	
    public void setNoDoctype(boolean b) { bNoDoctype = b; }
	
    public void setAddBOM(boolean b) { bAddBOM = b; }
    
    public void setUseNamedEntities(boolean b) {
        bUseNamedEntities = b;
    }
	
    public void setXsltPath(String s) { sXsltPath = s; }

    public String getFileExtension() { return super.getFileExtension(); }

    /**
     *  Write out content to the supplied <code>OutputStream</code>.
     *  (with pretty printing)
     *  @param  os  XML <code>OutputStream</code>.
     *  @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {
        // Prepare named entities
        prepareEntities();

        OutputStreamWriter osw = new OutputStreamWriter(os,sEncoding);
        // Add a BOM if the user desires so
        if (bAddBOM) { osw.write("\uFEFF"); }

        // Omit xml prolog for pure xhtml documents (to be browser safe)
        if (nType==XHTML_MATHML || nType==XHTML_MATHML_XSL) {
            osw.write("<?xml version=\"1.0\" encoding=\""+sEncoding+"\" ?>\n");
        }
        // Either specify doctype or xsl transformation (the user may require
        // that no doctype is used; this may be desirable for further transformations)
        if (nType==XHTML_MATHML_XSL) {
            // Original url: http://www.w3.org/Math/XSL/pmathml.xsl
            // Add trailing slash if needed
            String sSlash = sXsltPath.length()>0 && !sXsltPath.endsWith("/") ? "/" : "";
            osw.write("<?xml-stylesheet type=\"text/xsl\" href=\""+sXsltPath+sSlash+"pmathml.xsl\"?>\n");
        }
        else if (!bNoDoctype) {
            osw.write("<!DOCTYPE html PUBLIC \"");
            osw.write(getContentDOM().getDoctype().getPublicId());
            osw.write("\" \"");
            osw.write(getContentDOM().getDoctype().getSystemId());
            osw.write("\">\n");
        }
        write(getContentDOM().getDocumentElement(),0,osw);
        osw.flush();
        osw.close();
    }

    private boolean isEmpty(String sTagName) {
        for (int i=0; i<sEmpty.length; i++) {
            if (sEmpty[i].equals(sTagName)) { return true; }
        }
        return false;
    }
	
    // Write nodes; we only need element, text and comment nodes
    private void write(Node node, int nLevel, OutputStreamWriter osw) throws IOException {
        short nType = node.getNodeType();
        switch (nType) {
            case Node.ELEMENT_NODE:
                if (isEmpty(node.getNodeName())) {
                    // This node must be empty, we ignore childnodes
                    if (nLevel>=0) { writeSpaces(nLevel,osw); }
                    osw.write("<"+node.getNodeName());
                    writeAttributes(node,osw);
                    osw.write(" />");
                    if (nLevel>=0) { osw.write("\n"); }
                }
                else if (node.hasChildNodes()) {
                    // Block pretty print from this node?
                    NodeList list = node.getChildNodes();
                    int nLen = list.getLength();
                    boolean bBlockPrettyPrint = false;
                    if (nLevel>=0) {
                        for (int i = 0; i < nLen; i++) {
                            bBlockPrettyPrint |= list.item(i).getNodeType()==Node.TEXT_NODE;
                        }
                    }
                    // Print start tag
                    if (nLevel>=0) { writeSpaces(nLevel,osw); }
                    osw.write("<"+node.getNodeName());
                    writeAttributes(node,osw);
                    osw.write(">");
                    if (nLevel>=0 && !bBlockPrettyPrint) { osw.write("\n"); }
                    // Print children
                    for (int i = 0; i < nLen; i++) {
                        int nNextLevel;
                        if (bBlockPrettyPrint || nLevel<0) { nNextLevel=-1; }
                        else { nNextLevel=nLevel+1; }
                        write(list.item(i),nNextLevel,osw);
                    }
                    // Print end tag
                    if (nLevel>=0 && !bBlockPrettyPrint) { writeSpaces(nLevel,osw); }
                    osw.write("</"+node.getNodeName()+">");
                    if (nLevel>=0) { osw.write("\n"); }
                }
                else { // empty element
                    if (nLevel>=0) { writeSpaces(nLevel,osw); }
                    osw.write("<"+node.getNodeName());
                    writeAttributes(node,osw);
                    // HTML compatibility: use end-tag even if empty
                    if (nType<=XHTML11) {
                        osw.write("></"+node.getNodeName()+">");
                    }
                    else {
                        osw.write(" />");
                    }
                    if (nLevel>=0) { osw.write("\n"); }
                }
                break;
            case Node.TEXT_NODE:
                write(node.getNodeValue(),osw);
                break;
            case Node.COMMENT_NODE:
                if (nLevel>=0) { writeSpaces(nLevel,osw); }
                osw.write("<!-- ");
                write(node.getNodeValue(),osw);
                osw.write(" -->");
                if (nLevel>=0) { osw.write("\n"); }
        }
    }
	
    private void writeAttributes(Node node, OutputStreamWriter osw) throws IOException {
        NamedNodeMap attr = node.getAttributes();
        int nLen = attr.getLength();
        for (int i=0; i<nLen; i++) {
            Node item = attr.item(i);
            osw.write(" ");
            write(item.getNodeName(),osw);
            osw.write("=\"");
            writeAttribute(item.getNodeValue(),osw);
            osw.write("\"");
        }
    }

    private void writeSpaces(int nCount, OutputStreamWriter osw) throws IOException {
        for (int i=0; i<nCount; i++) { osw.write("  "); }
    }
	
    private void write(String s, OutputStreamWriter osw) throws IOException {
        // Allow null strings, though this means there is a bug somewhere...
        if (s==null) { osw.write("null"); return; }
        int nLen = s.length();
        char c;
        for (int i=0; i<nLen; i++) {
            c = s.charAt(i);
            switch (c) {
                case ('<'): osw.write("&lt;"); break;
                case ('>'): osw.write("&gt;"); break;
                case ('&'): osw.write("&amp;"); break;
                default: 
                    write(c,osw);
            }
        }
    }

    private void writeAttribute(String s, OutputStreamWriter osw) throws IOException {
        int nLen = s.length();
        char c;
        for (int i=0; i<nLen; i++) {
            c = s.charAt(i);
            switch (c) {
                case ('<'): osw.write("&lt;"); break;
                case ('>'): osw.write("&gt;"); break;
                case ('&'): osw.write("&amp;"); break;
                case ('"'): osw.write("&quot;"); break;
                case ('\''): osw.write( nType == XHTML10 ? "&#39;" : "&apos;"); break;
                default:
                    write(c,osw);
            }
        }
    }
    
    private void write(char c, OutputStreamWriter osw) throws IOException {
    	if (bUseNamedEntities) {
    		if (c<256 && entities[c]!=null) {
    			// XHTML has a named entity here
    			osw.write(entities[c]);
    			return;
    		}
    		String s=getMathMLEntity(c);
    		if (s!=null && (nType==XHTML_MATHML || nType==XHTML_MATHML_XSL)) {
    			// There's a MathML entity to use
    			osw.write(s);
    			return;
    		}
    	}
        if (c>cLimit) {
            osw.write("&#x"+Integer.toHexString(c).toUpperCase()+";");
        }
        else {
            osw.write(c);
        }
    }
    
    private static void prepareEntities() {
        if (entities==null) {
            entities = new String[256];
            // Latin 1 symbols
            entities[160]="&nbsp;";
            entities[161]="&iexcl;";
            entities[162]="&cent;";
            entities[163]="&pound;";
            entities[164]="&curren;";
            entities[165]="&yen;";
            entities[166]="&brvbar;";
            entities[167]="&sect;";
            entities[168]="&uml;";
            entities[169]="&copy;";
            entities[170]="&ordf;";
            entities[171]="&laquo;";
            entities[172]="&not;";
            entities[173]="&shy;";
            entities[174]="&reg;";
            entities[175]="&macr;";
            entities[176]="&deg;";
            entities[177]="&plusmn;";
            entities[178]="&sup2;";
            entities[179]="&sup3;";
            entities[180]="&acute;";
            entities[181]="&micro;";
            entities[182]="&para;";
            entities[183]="&middot;";
            entities[184]="&cedil;";
            entities[185]="&sup1;";
            entities[186]="&ordm;";
            entities[187]="&raquo;";
            entities[188]="&frac14;";
            entities[189]="&frac12;";
            entities[190]="&frac34;";
            entities[191]="&iquest;";
            entities[215]="&times;";
            entities[247]="&divide;";
            // Latin 1 characters
            entities[192]="&Agrave;";
            entities[193]="&Aacute;";
            entities[194]="&Acirc;";
            entities[195]="&Atilde;";
            entities[196]="&Auml;";
            entities[197]="&Aring;";
            entities[198]="&AElig;";
            entities[199]="&Ccedil;";
            entities[200]="&Egrave;";
            entities[201]="&Eacute;";
            entities[202]="&Ecirc;";
            entities[203]="&Euml;";
            entities[204]="&Igrave;";
            entities[205]="&Iacute;";
            entities[206]="&Icirc;";
            entities[207]="&Iuml;";
            entities[208]="&ETH;";
            entities[209]="&Ntilde;";
            entities[210]="&Ograve;";
            entities[211]="&Oacute;";
            entities[212]="&Ocirc;";
            entities[213]="&Otilde;";
            entities[214]="&Ouml;";
            entities[216]="&Oslash;";
            entities[217]="&Ugrave;";
            entities[218]="&Uacute;";
            entities[219]="&Ucirc;";
            entities[220]="&Uuml;";
            entities[221]="&Yacute;";
            entities[222]="&THORN;";
            entities[223]="&szlig;";
            entities[224]="&agrave;";
            entities[225]="&aacute;";
            entities[226]="&acirc;";
            entities[227]="&atilde;";
            entities[228]="&auml;";
            entities[229]="&aring;";
            entities[230]="&aelig;";
            entities[231]="&ccedil;";
            entities[232]="&egrave;";
            entities[233]="&eacute;";
            entities[234]="&ecirc;";
            entities[235]="&euml;";
            entities[236]="&igrave;";
            entities[237]="&iacute;";
            entities[238]="&icirc;";
            entities[239]="&iuml;";
            entities[240]="&eth;";
            entities[241]="&ntilde;";
            entities[242]="&ograve;";
            entities[243]="&oacute;";
            entities[244]="&ocirc;";
            entities[245]="&otilde;";
            entities[246]="&ouml;";
            entities[248]="&oslash;";
            entities[249]="&ugrave;";
            entities[250]="&uacute;";
            entities[251]="&ucirc;";
            entities[252]="&uuml;";
            entities[253]="&yacute;";
            entities[254]="&thorn;";
            entities[255]="&yuml;";
        }
    }
    
    // Translate character to MathML entity (contributed by Bruno Mascret)
    private String getMathMLEntity(char c) {
    	switch (c) {
    	case '\u0192': return "&fnof;";// lettre minuscule latine f hameon
		case '\u0391': return "&Alpha;";// lettre majuscule grecque alpha
		case '\u0392': return "&Beta;";// lettre majuscule grecque beta
		case '\u0393': return "&Gamma;";// lettre majuscule grecque gamma
		case '\u0394': return "&Delta;";// lettre majuscule grecque delta
		case '\u0395': return "&Epsilon;";// lettre majuscule grecque epsilon
		case '\u0396': return "&Zeta;";// lettre majuscule grecque zeta
		case '\u0397': return "&Eta;";// lettre majuscule grecque eta
		case '\u0398': return "&Theta;";// lettre majuscule grecque theta
		case '\u0399': return "&Iota;";// lettre majuscule grecque iota
		case '\u039A': return "&Kappa;";// lettre majuscule grecque kappa
		case '\u039B': return "&Lambda;";// lettre majuscule grecque lambda
		case '\u039C': return "&Mu;";// lettre majuscule grecque mu
		case '\u039D': return "&Nu;";// lettre majuscule grecque nu
		case '\u039E': return "&Xi;";// lettre majuscule grecque xi
		case '\u039F': return "&Omicron;";// lettre majuscule grecque omicron
		case '\u03A0': return "&Pi;";// lettre majuscule grecque pi
		case '\u03A1': return "&Rho;";// lettre majuscule grecque rho
		case '\u03A3': return "&Sigma;";// lettre majuscule grecque sigma (Il n'y pas de caractere Sigmaf ni U+03A2 non plus)
		case '\u03A4': return "&Tau;";// lettre majuscule grecque tau
		case '\u03A5': return "&Upsilon;";// lettre majuscule grecque upsilon
		case '\u03A6': return "&Phi;";// lettre majuscule grecque phi
		case '\u03A7': return "&Chi;";// lettre majuscule grecque chi
		case '\u03A8': return "&Psi;";// lettre majuscule grecque psi
		case '\u03A9': return "&Omega;";// lettre majuscule grecque omega
		case '\u03B1': return "&alpha;";// lettre minuscule grecque alpha
		case '\u03B2': return "&beta;";// lettre minuscule grecque beta
		case '\u03B3': return "&gamma;";// lettre minuscule grecque gamma
		case '\u03B4': return "&delta;";// lettre minuscule grecque delta
		//case '\u03B4': return "&delta;";// lettre minuscule grecque delta			
		case '\u03B5': return "&epsilon;";// lettre minuscule grecque epsilon
		case '\u03B6': return "&zeta;";// lettre minuscule grecque zeta
		case '\u03B7': return "&eta;";// lettre minuscule grecque eta
		case '\u03B8': return "&theta;";// lettre minuscule grecque theta
		case '\u03B9': return "&iota;";// lettre minuscule grecque iota
		case '\u03BA': return "&kappa;";// lettre minuscule grecque kappa
		case '\u03BB': return "&lambda;";// lettre minuscule grecque lambda
		case '\u03BC': return "&mu;";// lettre minuscule grecque mu
		case '\u03BD': return "&nu;";// lettre minuscule grecque nu
		case '\u03BE': return "&xi;";// lettre minuscule grecque xi
		case '\u03BF': return "&omicron;";// lettre minuscule grecque omicron
		case '\u03C0': return "&pi;";// lettre minuscule grecque pi
		case '\u03C1': return "&rho;";// lettre minuscule grecque rho
		case '\u03C2': return "&sigmaf;";// lettre minuscule grecque final sigma
		case '\u03C3': return "&sigma;";// lettre minuscule grecque sigma
		case '\u03C4': return "&tau;";// lettre minuscule grecque tau
		case '\u03C5': return "&upsilon;";// lettre minuscule grecque upsilon
		case '\u03C6': return "&phi;";// lettre minuscule grecque phi
		case '\u03C7': return "&chi;";// lettre minuscule grecque chi
		case '\u03C8': return "&psi;";// lettre minuscule grecque psi
		case '\u03C9': return "&omega;";// lettre minuscule grecque omega
		case '\u03D1': return "&thetasym;";// lettre minuscule grecque theta symbol
		case '\u03D2': return "&upsih;";// symbole grec upsilon crochet
		case '\u03D6': return "&piv;";// symbole grec pi
		case '\u2022': return "&bull;";// puce (Ce N'EST PAS la meme chose que l'operateur puce, U+2219)
		case '\u2026': return "&hellip;";// points de suspension
		case '\u2032': return "&prime;";// prime
		case '\u2033': return "&Prime;";// double prime
		case '\u203E': return "&oline;";// tiret en chef
		case '\u2044': return "&frasl;";// barre de fraction
		case '\u2118': return "&weierp;";// fonction elliptique de Weierstrass
		case '\u2111': return "&image;";// majuscule I gothique = partie imaginaire
		case '\u211C': return "&real;";// majuscule R gothique = partie reelle
		case '\u2122': return "&trade;";// symbole marque commerciale
		case '\u2135': return "&alefsym;";// symbole alef = premier nombre transfini (Le symbole alef N'EST PAS pareil a la lettre hebreue alef, U+05D0 meme si on pourrait utiliser le meme glyphe pour representer les deux caracteres)
		case '\u2190': return "&larr;";// fleche vers la gauche
		case '\u2191': return "&uarr;";// fleche vers le haut
		case '\u2192': return "&rarr;";// fleche vers la droite
		case '\u2193': return "&darr;";// fleche vers le bas
		case '\u2194': return "&harr;";// fleche bilaterale
		case '\u21B5': return "&crarr;";// fleche vers le bas avec coin vers la gauche = retour de chariot
		case '\u21D0': return "&lArr;";// double fleche vers la gauche (ISO 10646 ne dit pas que lArr est la meme chose que la fleche 'est implique par' et n'a pas non plus d'autre caractere pour cette fonction. Alors ? On peut utiliser lArr pour 'est implique par' comme le suggere)
		case '\u21D1': return "&uArr;";// double fleche vers le haut
		case '\u21D2': return "&rArr;";// double fleche vers la droite (ISO 10646 ne dit pas qu'il s'agit du caractere 'implique' et n'a pas non plus d'autre caractere avec cette fonction. Alors ? On peut utiliser rArr pour 'implique' comme le suggere)
		case '\u21D3': return "&dArr;";// double fleche vers le bas
		case '\u21D4': return "&hArr;";// double fleche bilaterale
		case '\u2200': return "&forall;";// pour tous
		case '\u2202': return "&part;";// derivee partielle
		case '\u2203': return "&exist;";// il existe
		case '\u2205': return "&empty;";// ensemble vide = symbole diametre
		case '\u2207': return "&nabla;";// nabla
		case '\u2208': return "&isin;";// appartient
		case '\u2209': return "&notin;";// n'appartient pas
		case '\u220B': return "&ni;";// contient comme element (Est-ce qu'il ne pourrait pas y avoir un nom plus parlant que 'ni' ?)
		case '\u220F': return "&prod;";// produit de la famille = signe produit (prod N'EST PAS le meme caractere que U+03A0 'lettre capitale grecque pi' meme si le meme glyphe peut s'utiliser pour les deux)
		case '\u2211': return "&sum;";// sommation de la famille (sum N'EST PAS le meme caractere que U+03A3 'ettre capitale grecque sigma' meme si le meme glyphe peut s'utiliser pour les deux)
		case '\u2212': return "&minus;";// signe moins
		case '\u2217': return "&lowast;";// operateur asterisque
		case '\u221A': return "&radic;";// racine carree = signe radical
		case '\u221D': return "&prop;";// proportionnel
		case '\u221E': return "&infin;";// infini
		case '\u2220': return "&ang;";// angle
		case '\u2227': return "&and;";// ET logique
		case '\u2228': return "&or;";// OU logique
		case '\u2229': return "&cap;";// intersection = cap
		case '\u222A': return "&cup;";// union = cup
		case '\u222B': return "&int;";// integrale
		case '\u2234': return "&there4;";// par consequent
		case '\u223C': return "&sim;";// operateur tilde = varie avec = similaire (L'operateur tilde N'EST PAS le meme caractere que le tilde U+007E, meme si le meme glyphe peut s'utiliser pour les deux)
		case '\u2245': return "&cong;";// approximativement egal
		case '\u2248': return "&asymp;";// presque egal = asymptotique
		case '\u2260': return "&ne;";// pas egal
		case '\u2261': return "&equiv;";// identique
		//case '\u2261': return "&equiv;";// identique			
		case '\u2264': return "&le;";// plus petit ou egal
		case '\u2265': return "&ge;";// plus grand ou egal
		case '\u2282': return "&sub;";// sous-ensemble de
		case '\u2283': return "&sup;";// sur-ensemble de (Remarquez que nsup 'pas un sur-ensemble de' 2285, n'est pas couvert par le codage de la police Symbol. Devrait-il l'etre par symetrie ? Il est dans)
		case '\u2284': return "&nsub;";// pas un sous-ensemble de
		case '\u2286': return "&sube;";// sous-ensemble ou egal
		case '\u2287': return "&supe;";// sur-ensemble de ou egal
		case '\u2295': return "&oplus;";// plus cercle = somme directe
		case '\u2297': return "&otimes;";// multiplie par cercle = produit vectoriel
		case '\u22A5': return "&perp;";// taquet vers le haut = orthogonal = perpendiculaire
		case '\u22C5': return "&sdot;";// operateur point (L'operateur point N'EST PAS le meme caractere que le 'point median', U+00B7)
		case '\u2308': return "&lceil;";// plafond gauche = anglet gauche
		case '\u2309': return "&rceil;";// plafond droite
		case '\u230A': return "&lfloor;";// plancher gauche
		case '\u230B': return "&rfloor;";// plancher droite
		case '\u2329': return "&lang;";// chevron vers la gauche (lang N'EST PAS le meme caractere que U+003C 'inferieur' ou U+2039 'guillemet simple vers la gauche')
		case '\u232A': return "&rang;";// chevron vers la droite (rang iN'EST PAS le meme caractere que U+003E 'superieur' ou U+203A 'guillemet simple vers la droite')
		case '\u25CA': return "&loz;";// losange
		case '\u2660': return "&spades;";// pique noir (Noir semble dire ici rempli par opposition ajoure)
		case '\u2663': return "&clubs;";// trefle noir
		case '\u2665': return "&hearts;";// coeur noir
		case '\u2666': return "&diams;";// carreau noir
		// truc pas prevus
		case '\u2102': return "&Copf;";// ensemble C des complexes
		case '\u2115': return "&Nopf;";// ensemble N des entiers
		case '\u211A': return "&Qopf;";// ensemble Q des rationnels
		case '\u211D': return "&Ropf;";// ensemble R des reels
		case '\u2124': return "&Zopf;";// ensemble R des entiers relatifs
		case '\u2223': return "&mid;";// divise
		case '\u2224': return "&nmid;";// ne divise pas
		case '\u2243': return "&simeq;";// asymptotiquement egal
		case '\u2244': return "&nsimeq;";// asymptotiquement egal
		case '\u2225': return "&par;";// parallele
		case '\u00B1': return "&PlusMinus;";// plus ou moins
		case '\u2213': return "&MinusPlus;"; // moins ou plus (different de plus ou moins)
		case '\u2494': return "&leqslant;"; // inferieur ou egal incline
		case '\u2270': return "&nle;"; //non inferieur ou egal incline
		case '\u00AC': return "&not;";// signe not
		case '\u00B0': return "&circ;";// petit cercle, operateur concatenation, normalement &deg; mais on va le considere comme circ
		case '\u224A': return "&ape;";// approxivativement egal 
		case '\u002B': return "&plus;"; // signe plus
		case '\u00D7': return "&times;"; // signe multiplication (croix)
		case '\u003D': return "&equals;"; // signe egal
		case '\u226E': return "&nlt;"; // non inferieur
		case '\u2A7D': return "&les;"; // inferieur incline = leqslant
		case '\u220A': return "&isin;";// appartient
		case '\u2216': return "&setminus;";// difference d'ensemble
		case '\u2288': return "&nsube;";// ni un sous-ensemble ni egal
		case '\u2289': return "&nsupe;";// ni un surensemble ni egal
		case '\u2285': return "&nsup;";// non un surensemble de
		case '\u301A': return "&lobrk;";// crochet gauche avec barre 
		case '\u301B': return "&robrk;";// crochet droit avec barre
		case '\u2210': return "&coprod;";// coproduit (Pi l'envers)
		case '\u222C': return "&Int;";// integrale double
		case '\u222D': return "&tint;";// integrale triple
		case '\u222E': return "&conint;";// integrale de contour
		case '\u222F': return "&Conint;";// integrale de surface
		case '\u2230': return "&Cconint;";// integrale de volume
		case '\u210F': return "&hbar;";// const de Planck sur 2Pi
		case '\u2253': return "&;";// BUG points suspensions diagonale descendant droite
		case '\u22EE': return "&vellip;";// points suspensions verticaux
		case '\u22EF': return "&ctdot;";// points suspensions horizontaux medians
		case '\u22F0': return "&utdot;";// points suspensions diagonale montant droite
		case '\u22F1': return "&dtdot;";// points suspensions diagonale descendant droite
		case '\u02DA': return "&ring;"; //rond en chef
		case '\u00A8':	return "&Dot;"; // double point en chef(trema)
		case '\u02D9':	return "&dot;"; // point en chef
		case '\u2015': return "&horbar;"; // barre horizonthale
		case '\u00AF': return "&macr;"; // barre horizonthale en chef
		case '\u0332': return "&UnderBar;"; // souligne
		case '\u2222': return "&angsph;"; // angle spherique
		case '\u03F1': return "&rhov;"; // symbole grec rho final
		case '\u226B': return "&Gt;"; // tres superieur
		case '\u226A': return "&Lt;"; // tres inferieur
	    default: return null;
    	}
    }


}



        




