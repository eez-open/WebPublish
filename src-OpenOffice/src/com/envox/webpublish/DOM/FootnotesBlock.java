/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author martin
 */
public class FootnotesBlock extends Block {
    List<FootnoteTextPortion> m_footnotes = new ArrayList<FootnoteTextPortion>();
    List<FootnoteTextPortion> m_endnotes = new ArrayList<FootnoteTextPortion>();

    FootnotesBlock(Document doc, List<FootnoteTextPortion> footnotes) {
        super(doc);

        for (FootnoteTextPortion footnote:footnotes) {
            if (footnote.isFootnoteOrEndnote()) {
                m_footnotes.add(footnote);
            } else {
                m_endnotes.add(footnote);
            }
        }
    }

    int getNumPageBreaks() {
        return 0;
    }

    PageBreak breakPageAt(int pos) {
        throw new UnsupportedOperationException();
    }

    public void emit(HTMLBuilder builder) {
        emitNotes(builder, m_footnotes, Document.FOOTNOTES_DIVIDER_ID,
                Document.FOOTNOTES_ID);

        emitNotes(builder, m_endnotes, Document.ENDNOTES_DIVIDER_ID,
                Document.ENDNOTES_ID);
    }

    void emitInsideFootnote(HTMLBuilder builder) {

    }

    void emitNotes(HTMLBuilder builder, List<FootnoteTextPortion> notes,
            String dividerID, String notesID) {
        if (notes.isEmpty()) {
            return;
        }

        builder.beginTag("div");
        builder.attr("id", dividerID);
        builder.endTag("div");

        builder.beginTag("table");
        builder.attr("id", notesID);

        for (FootnoteTextPortion note:notes) {
            note.emitBody(builder);
        }
        builder.endTag("table");

        notes.clear();
    }

    public void collectFootnotes(List<FootnoteTextPortion> footnotes) {
        // nothing to do
    }
}
