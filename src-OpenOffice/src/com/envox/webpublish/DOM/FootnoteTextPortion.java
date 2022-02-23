/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;

/**
 *
 * @author martin
 */
public class FootnoteTextPortion extends TextPortion {
    private boolean m_isFootnoteOrEndnote;
    private int m_index;
    private String m_href;
    private String m_id;
    private String m_class;
    private BlockContainer m_body;
    private String m_hrefBack;
    private String m_idBack;
    private String m_classBack;

    FootnoteTextPortion(Paragraph paragraph, Document doc, Object xObject) {
        super(paragraph, doc, xObject);

        Object xFootnote = getProperty("Footnote");
        m_isFootnoteOrEndnote = m_doc.isFootnoteOrEndnote(xFootnote);

        if (m_isFootnoteOrEndnote) {
            m_class = Document.FOOTNOTE_A_CLASS;
            m_classBack = Document.FOOTNOTE_BACK_A_CLASS;
        } else {
            m_class = Document.ENDNOTE_A_CLASS;
            m_classBack = Document.FOOTNOTE_BACK_A_CLASS;
        }

        m_body = m_doc.createBlockContainer((XText) UnoRuntime.queryInterface(XText.class, xFootnote));
    }

    boolean isFootnoteOrEndnote() {
        return m_isFootnoteOrEndnote;
    }

    void setIndex(int index) {
        m_index = index;

        m_id = String.format("%s%d", m_isFootnoteOrEndnote ?
            Document.FOOTNOTE_A_PREFIX : Document.ENDNOTE_A_PREFIX, m_index);
        m_href = "#" + m_id;

        m_idBack = String.format("%s%d", m_isFootnoteOrEndnote ?
            Document.FOOTNOTE_BACK_A_PREFIX : Document.ENDNOTE_BACK_A_PREFIX, m_index);
        m_hrefBack = "#" + m_idBack;
    }

    void emit(HTMLBuilder builder) {
        builder.beginTag("a");
        builder.attr("id", m_idBack);
        builder.attr("href", m_href);

        builder.attr("class", m_class);

        String cssClassName = m_style.getParentCharacterStyle() != null ?
            m_style.getParentCharacterStyle().getCssClassName() : "";
        String cssStyle = m_style.getCssStyle();
        emitText(builder, m_text, cssClassName, cssStyle);

        builder.endTag("a");
    }

    void emitBody(HTMLBuilder builder) {
        builder.beginTag("tr");

        builder.beginTag("td");
        builder.attr("id", m_id);
        builder.attr("class", "label");
        builder.append(m_text);
        builder.endTag("td");

        builder.beginTag("td");

        builder.beginTag("a");
        builder.attr("href", m_hrefBack);
        builder.attr("class", m_classBack + " content");
        builder.append("^");
        builder.endTag("a");

        m_body.emitInsideFootnote(builder);

        builder.endTag("td");

        builder.endTag("tr");
    }

    String getBodyCSSStyle() {
        for (Block block:m_body.getBlocks()) {
            if (block instanceof Paragraph) {
                return ((Paragraph) block).getStyle().getCssStyle(new String[] { 
                    "text-align", "font-family", "font-size" 
                });
            }
        }
        return null;
    }
}
