/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
class EmbeddedObjectFrame extends Block {
    private String m_src;
    private String m_title;
    private Style m_style;

    EmbeddedObjectFrame(Document doc, Object xObject) {
        super(doc, xObject);

        String name = (String) getProperty("LinkDisplayName");
        m_src = m_doc.registerEmbeddedObjectImageURL(name);

        m_title = (String) getProperty("Title");
        if (StringUtils.isBlank(m_title)) {
            m_title = name;
        }

        m_style = m_doc.createStyle(xObject);
        m_style.loadImageProperties();
    }

    int getNumPageBreaks() {
        return 0;
    }

    PageBreak breakPageAt(int pos) {
        throw new UnsupportedOperationException();
    }

    public void emit(HTMLBuilder builder) {
        builder.beginTag("img");

        builder.attr("src", m_doc.getImageURL(m_src));
        builder.attr("alt", m_title);

        String cssStyle = m_style.getCssStyle();
        if (StringUtils.isNotBlank(cssStyle)) {
            builder.attr("style", cssStyle);
        }

        builder.endTag("img");
    }

    void emitInsideFootnote(HTMLBuilder builder) {
        // do nothing
    }

    public void collectFootnotes(List<FootnoteTextPortion> footnotes) {
        // nothing to do
    }
}
