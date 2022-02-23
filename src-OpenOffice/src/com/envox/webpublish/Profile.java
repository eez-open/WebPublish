/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author martin
 */
public class Profile {
    public enum Type { 
        HTML,
        EPUB,
        WordPress,
        Joomla
    };

    private Type m_type;

    private String m_profileName = "";
    private String m_blogAddress = "";
    private String m_blogUsername = "";
    private String m_blogPassword = "";
    private String m_blogID = "";
    private boolean m_shortenWebLinks = true;
    private String m_documentTitleStyle = "";
    private String m_htmlCodeStyle = "";
    private int m_scaling = 100;
    private boolean m_convertToPixels = true;
    private boolean m_pageBreakEnabled = false;
    private int m_pageBreakFreq = 1;
    private String m_pageBreakHTML = "";
    private List<ParagraphsToTableRule> m_paragraphsToTableRules = new ArrayList<ParagraphsToTableRule>();
    private String m_CSSSelector = "";
    private String m_customCSS = "";
    private String m_cssImagesFolder = "";
    private String m_cssFontsFolder = "";
    private List<FontAlternative> m_fontAlternatives = new ArrayList<FontAlternative>();
    private String m_htmlImagesOutputFolder = "";

    public Profile() {
    }

    public Profile(Type type) {
        m_type = type;
        init();
    }

    private void init() {
        if (m_type == Type.EPUB) {
            setProfileName("EPUB");
            setDocumentTitleStyle("Heading 1");
            setHtmlCodeStyle("html-code");
            setScaling(100);
            setConvertToPixels(true);
            setPageBreakEnabled(false);
            setCSSSelector("");
            setCustomCSS("");
        } else if (m_type == Type.HTML) {
            setProfileName("HTML");
            setDocumentTitleStyle("Heading 1");
            setHtmlCodeStyle("html-code");
            setScaling(100);
            setConvertToPixels(true);
            setPageBreakEnabled(false);
            setCSSSelector("");
            setCustomCSS("");
        } else if (m_type == Type.Joomla) {
            setProfileName("Joomla");
            setBlogAddress("https://<<address>>/xmlrpc/index.php");
            setShortenWebLinks(true);
            setDocumentTitleStyle("Heading 1");
            setHtmlCodeStyle("html-code");
            setScaling(100);
            setConvertToPixels(true);
            setPageBreakEnabled(true);
            setPageBreakFreq(2);
            setPageBreakHTML("<hr class=\"system-pagebreak\" />");
            setCSSSelector("#content");
            setCustomCSS("");
        } else if (m_type == Type.WordPress) {
            setProfileName("WordPress");
            setBlogAddress("https://<<address>>/xmlrpc.php");
            setShortenWebLinks(true);
            setDocumentTitleStyle("Heading 1");
            setHtmlCodeStyle("html-code");
            setScaling(100);
            setConvertToPixels(true);
            setPageBreakEnabled(false);
            setPageBreakFreq(2);
            setPageBreakHTML("");
            setCSSSelector("");
            setCustomCSS("");
        }
    }

    public Type getType() { return m_type; }

    public boolean isPublishProfile() { return m_type != Type.HTML && m_type != Type.EPUB; }

    public String getProfileName() { return m_profileName; }
    public void setProfileName(String profileName) { m_profileName = profileName; }

    public String getBlogAddress() { return m_blogAddress; }
    public void setBlogAddress(String blogAddress) { m_blogAddress = blogAddress; }

    public String getBlogUsername() { return m_blogUsername; }
    public void setBlogUsername(String blogUsername) { m_blogUsername = blogUsername; }

    public String getBlogPassword() {
        if (m_blogPassword == null) {
            m_blogPassword = Util.getPassword(m_profileName);
        }
        return m_blogPassword != null ? m_blogPassword : "";
    }
    public void setBlogPassword(String blogPassword) { m_blogPassword = blogPassword; }

    public String getBlogID() { return m_blogID; }
    public void setBlogID(String blogID) { m_blogID = blogID; }

    public boolean getShortenWebLinks() { return m_shortenWebLinks; }
    public void setShortenWebLinks(boolean shortenWebLinks) { m_shortenWebLinks = shortenWebLinks; }

    public String getDocumentTitleStyle() { return m_documentTitleStyle; }
    public void setDocumentTitleStyle(String documentTitleStyle) { m_documentTitleStyle = documentTitleStyle; }

    public String getHtmlCodeStyle() { return m_htmlCodeStyle; }
    public void setHtmlCodeStyle(String htmlCodeStyle) { m_htmlCodeStyle = htmlCodeStyle; }

    public int getScaling() { return m_scaling; }
    public void setScaling(int scaling) { m_scaling = scaling; }

    public boolean getConvertToPixels() { return m_convertToPixels; }
    public void setConvertToPixels(boolean convertToPixels) { m_convertToPixels = convertToPixels; }

    public boolean getPageBreakEnabled() { return m_pageBreakEnabled; }
    public void setPageBreakEnabled(boolean pageBreakEnabled) { m_pageBreakEnabled = pageBreakEnabled; }

    public int getPageBreakFreq() { return m_pageBreakFreq; }
    public void setPageBreakFreq(int pageBreakFreq) { m_pageBreakFreq = pageBreakFreq; }

    public String getPageBreakHTML() { return m_pageBreakHTML; }
    public void setPageBreakHTML(String pageBreakHTML) { m_pageBreakHTML = pageBreakHTML; }

    public List<ParagraphsToTableRule> getParagraphsToTableRules() { return m_paragraphsToTableRules; }
    public void setParagraphsToTableRules(List<ParagraphsToTableRule> paragraphsToTableRules) { m_paragraphsToTableRules = paragraphsToTableRules; }

    public String getCSSSelector() { return m_CSSSelector; }
    public void setCSSSelector(String CSSSelector) { m_CSSSelector = CSSSelector; }

    public String getCustomCSS() { return m_customCSS; }
    public void setCustomCSS(String customCSS) { m_customCSS = customCSS; }

    public String getCSSImagesFolder() { return m_cssImagesFolder; }
    public void setCSSImagesFolder(String cssImagesFolder) { m_cssImagesFolder = cssImagesFolder; }

    public String getCSSFontsFolder() { return m_cssFontsFolder; }
    public void setCSSFontsFolder(String cssFontsFolder) { m_cssFontsFolder = cssFontsFolder; }

    public List<FontAlternative> getFontAlternatives() { return m_fontAlternatives; }
    public void setFontAlternatives(List<FontAlternative> fontAlternatives) { m_fontAlternatives = fontAlternatives; }

    public String getHTMLImagesOutputFolder() { return m_htmlImagesOutputFolder; }
    public void setHTMLImagesOutputFolder(String htmlImagesOutputFolder) { m_htmlImagesOutputFolder = htmlImagesOutputFolder; }
    
    public void loadFromXML(Element parentElement) {
        m_profileName = Util.getTagValue("ProfileName", parentElement);

        String type = Util.getTagValue("Type", parentElement);
        if (StringUtils.isBlank(type)) {
            if (m_profileName.equals("HTML")) {
                m_type = Type.HTML;
            } else if (m_profileName.equals("EPUB")) {
                m_type = Type.EPUB;
            } else if (m_profileName.contains("WordPress")) {
                m_type = Type.WordPress;
            } else {
                m_type = Type.Joomla;
            }
        } else {
            if (type.equals("HTML")) {
                m_type = Type.HTML;
            } else if (type.equals("EPUB")) {
                m_type = Type.EPUB;
            } else if (type.equals("WordPress")) {
                m_type = Type.WordPress;
            } else if (type.equals("Joomla")) {
                m_type = Type.Joomla;
            } else {
                if (m_profileName.contains("WordPress")) {
                    m_type = Type.WordPress;
                } else {
                    m_type = Type.Joomla;
                }
            }
        }

        m_blogAddress = Util.getTagValue("BlogAddress", parentElement);
        m_blogUsername = Util.getTagValue("BlogUsername", parentElement);
        m_blogPassword = Util.getTagValue("BlogPassword", parentElement);
        if (StringUtils.isBlank(m_blogPassword)) {
            m_blogPassword = null;
        }
        m_blogID = Util.getTagValue("BlogID", parentElement);

        String strShortenWebLinks = Util.getTagValue("ShortenWebLinks", parentElement);
        if (StringUtils.isBlank(strShortenWebLinks)) {
            m_shortenWebLinks = true;
        } else {
            m_shortenWebLinks = Boolean.parseBoolean(strShortenWebLinks);
        }

        m_documentTitleStyle = Util.getTagValue("DocumentTitleStyle", parentElement);
        m_htmlCodeStyle = Util.getTagValue("HtmlCodeStyle", parentElement);
        if (StringUtils.isBlank(m_htmlCodeStyle)) {
            m_htmlCodeStyle = "html-code";
        }

        m_scaling = Integer.parseInt(Util.getTagValue("Scaling", parentElement));
        m_convertToPixels = Boolean.parseBoolean(Util.getTagValue("ConvertToPixels", parentElement));

        m_pageBreakEnabled = Boolean.parseBoolean(Util.getTagValue("PageBreakEnabled", parentElement));
        m_pageBreakFreq = Integer.parseInt(Util.getTagValue("PageBreakFreq", parentElement));
        m_pageBreakHTML = Util.getTagValue("PageBreakHTML", parentElement);

        NodeList nodeList = parentElement.getElementsByTagName("ParagraphsToTableRule");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Element ruleElement = (Element) nodeList.item(i);
            ParagraphsToTableRule rule = new ParagraphsToTableRule();
            rule.loadFromXML(ruleElement);
            m_paragraphsToTableRules.add(rule);
        }

        m_CSSSelector = Util.getTagValue("CSSSelector", parentElement);
        m_customCSS = Util.getTagValue("CustomCSS", parentElement);
        m_cssImagesFolder = Util.getTagValue("CSSImagesFolder", parentElement);
        m_cssFontsFolder = Util.getTagValue("CSSFontsFolder", parentElement);

        nodeList = parentElement.getElementsByTagName("FontAlternative");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Element alternativeElement = (Element) nodeList.item(i);
            FontAlternative alternative = new FontAlternative();
            alternative.loadFromXML(alternativeElement);
            m_fontAlternatives.add(alternative);
        }
        
        m_htmlImagesOutputFolder = Util.getTagValue("HTMLImagesOutputFolder", parentElement);
    }

    public void saveToXML(Element parentElement, Document doc) {
        if (m_type == Type.HTML) {
            Util.setTagValue("Type", "HTML", parentElement);
        } else if (m_type == Type.EPUB) {
            Util.setTagValue("Type", "EPUB", parentElement);
        } else if (m_type == Type.WordPress) {
            Util.setTagValue("Type", "WordPress", parentElement);
        } else if (m_type == Type.Joomla) {
            Util.setTagValue("Type", "Joomla", parentElement);
        }

        Util.setTagValue("ProfileName", m_profileName, parentElement);

        Util.setTagValue("BlogAddress", m_blogAddress, parentElement);
        Util.setTagValue("BlogUsername", m_blogUsername, parentElement);
        if (!StringUtils.isBlank(m_blogPassword)) {
            Util.storePassword(m_profileName, m_blogPassword);
        }
        Util.setTagValue("BlogID", m_blogID, parentElement);

        Util.setTagValue("ShortenWebLinks", Boolean.toString(m_shortenWebLinks), parentElement);

        Util.setTagValue("DocumentTitleStyle", m_documentTitleStyle, parentElement);
        Util.setTagValue("HtmlCodeStyle", m_htmlCodeStyle, parentElement);

        Util.setTagValue("Scaling", Integer.toString(m_scaling), parentElement);
        Util.setTagValue("ConvertToPixels", Boolean.toString(m_convertToPixels), parentElement);

        Util.setTagValue("PageBreakEnabled", Boolean.toString(m_pageBreakEnabled), parentElement);
        Util.setTagValue("PageBreakFreq", Integer.toString(m_pageBreakFreq), parentElement);
        Util.setTagValue("PageBreakHTML", m_pageBreakHTML, parentElement);

        Element childElement = doc.createElement("ParagraphsToTableRules");
        for (ParagraphsToTableRule rule:m_paragraphsToTableRules) {
            Element ruleElement = doc.createElement("ParagraphsToTableRule");
            rule.saveToXML(ruleElement, doc);
            childElement.appendChild(ruleElement);
        }
        parentElement.appendChild(childElement);

        Util.setTagValue("CSSSelector", m_CSSSelector, parentElement);
        Util.setTagValue("CustomCSS", m_customCSS, parentElement);
        Util.setTagValue("CSSImagesFolder", m_cssImagesFolder, parentElement);
        Util.setTagValue("CSSFontsFolder", m_cssFontsFolder, parentElement);

        childElement = doc.createElement("FontAlternatives");
        for (FontAlternative alternative:m_fontAlternatives) {
            Element alternativeElement = doc.createElement("FontAlternative");
            alternative.saveToXML(alternativeElement, doc);
            childElement.appendChild(alternativeElement);
        }
        parentElement.appendChild(childElement);
        
        Util.setTagValue("HTMLImagesOutputFolder", m_htmlImagesOutputFolder, parentElement);
    }

    public MoveableTypeAPIClient getBlogClient() throws MalformedURLException, XmlRpcException {
        return new MoveableTypeAPIClient(getBlogAddress(),
                getBlogUsername(), getBlogPassword(), getBlogID());
    }
}
