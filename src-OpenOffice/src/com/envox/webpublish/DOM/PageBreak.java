/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import java.util.List;

/**
 *
 * @author martin
 */
class PageBreak extends Block {
    String m_html;

    PageBreak(Document doc) {
        super(doc, null);
    }

    void setHtml(String html) {
        m_html = html;
    }

    int getNumPageBreaks() {
        return 0;
    }

    PageBreak breakPageAt(int pos) {
        throw new UnsupportedOperationException();
    }

    public void emit(HTMLBuilder builder) {
        builder.appendHtml(m_html);
    }

    void emitInsideFootnote(HTMLBuilder builder) {
    }

    public void collectFootnotes(List<FootnoteTextPortion> footnotes) {
        // nothing to do
    }
}
