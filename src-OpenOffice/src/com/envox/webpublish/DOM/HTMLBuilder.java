/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class HTMLBuilder {
    StringBuilder m_stringBuilder = new StringBuilder();
    String m_indent;
    String m_beginTagName;
    boolean m_bXHTML;

    public HTMLBuilder() {
        m_indent = "";
    }

    public HTMLBuilder(String indent) {
        m_indent = indent;
    }

    public void setXHTML(boolean bXHTML) {
        m_bXHTML = bXHTML;
    }

    // <editor-fold desc="HTML builder">

    public void beginTag(String tagName) {
        closeBeginTag();

        if (!isInlineElement(tagName)) {
            m_stringBuilder.append(m_indent);
        }

        m_stringBuilder.append("<");
        m_stringBuilder.append(tagName);

        m_beginTagName = tagName;
    }

    public void attr(String attrName, String attrValue) {
        if (StringUtils.isNotBlank(attrName)) {
            m_stringBuilder.append(" ");
            m_stringBuilder.append(attrName);
            m_stringBuilder.append("=\"");
            m_stringBuilder.append(htmlEscape(attrValue));
            m_stringBuilder.append("\"");
        }
    }

    private void closeBeginTag() {
        if (m_beginTagName == null) {
            return;
        }

        if (isEmptyClosedElement(m_beginTagName)) {
            m_stringBuilder.append("/>\n");
        } else if (isInlineElement(m_beginTagName) || isTextBlockElement(m_beginTagName)) {
            m_stringBuilder.append(">");
        } else {
            m_stringBuilder.append(">\n");
            m_indent += "\t";
        }

        m_beginTagName = null;
    }

    public void append(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }

        closeBeginTag();

        content = htmlEscape(content)
                .replace("\u00a0", "&#160;") // &nbsp;
                .replace("\u00ad", "&#173;") // &shy;
                .replace("\u2011", "&#45;")  // hard hyphen
                ;

        if (content.contains("\n")) {
            content = content.replace("\r\n", m_bXHTML ? "<br/>" : "<br>");
            content = content.replace("\n", m_bXHTML ? "<br/>" : "<br>");
        }

        m_stringBuilder.append(content);
    }

    public void appendHtml(String content) {
        if (StringUtils.isEmpty(content)) {
            return;
        }

        closeBeginTag();

        m_stringBuilder.append(m_indent);
        m_stringBuilder.append(content);
        m_stringBuilder.append("\n");
    }

    public void endTag(String tagName) {
        closeBeginTag();

        if (isEmptyClosedElement(tagName)) {
            return;
        }

        if (isInlineElement(tagName)) {
            emitEndTag(tagName);
        } else if (isTextBlockElement(tagName)) {
            emitEndTag(tagName);
            m_stringBuilder.append("\n");
        } else {
            m_indent = m_indent.substring(0, m_indent.length()-1);
            m_stringBuilder.append(m_indent);
            emitEndTag(tagName);
            m_stringBuilder.append("\n");
        }
    }

    private void emitEndTag(String tagName) {
        m_stringBuilder.append("</");
        m_stringBuilder.append(tagName);
        m_stringBuilder.append(">");
    }

    private String htmlEscape(String content) {
        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private boolean isInlineElement(String tagName) {
        return tagName.equals("span") || tagName.equals("a");
    }

    private boolean isEmptyClosedElement(String tagName) {
        return tagName.equals("meta") || tagName.equals("img") || tagName.equals("link");
    }

    private boolean isTextBlockElement(String tagName) {
        return tagName.equals("p") || tagName.equals("li")
            || tagName.equals("dt") || tagName.equals("dd")
            || tagName.equals("title");
    }

    // </editor-fold>

    // <editor-fold desc="CSS builder">

    void cssOpen(String selector) {
        closeBeginTag();
        m_stringBuilder.append(m_indent);
        m_stringBuilder.append(selector);
        m_stringBuilder.append(" {");
    }

    void cssAttr(String attrName, String attrValue) {
        if (m_stringBuilder.length() > 0) {
            m_stringBuilder.append(" ");
        }
        m_stringBuilder.append(attrName);
        m_stringBuilder.append(": ");
        m_stringBuilder.append(attrValue);
        m_stringBuilder.append(";");
    }

    void cssStyle(String style) {
        if (m_stringBuilder.length() > 0) {
            m_stringBuilder.append(" ");
        }
        m_stringBuilder.append(style);
    }

    void appendCss(String css) {
        closeBeginTag();
        m_stringBuilder.append(m_indent);
        m_stringBuilder.append(css);
        m_stringBuilder.append("\n");
    }

    void cssClose() {
        m_stringBuilder.append(" }\n");
    }

    // </editor-fold>

    @Override
    public String toString() {
        return m_stringBuilder.toString();
    }
}
