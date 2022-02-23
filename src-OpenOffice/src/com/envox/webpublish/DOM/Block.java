/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.text.XTextRange;
import java.util.List;

/**
 *
 * @author martin
 */
abstract public class Block extends Node {
    protected XTextRange m_xTextRange;

    Block(Document doc) {
        super(doc);
    }

    Block(Document doc, Object xObject) {
        super(doc, xObject);
        m_xTextRange = (XTextRange) queryInterface(XTextRange.class);
    }

    XTextRange getTextRange() {
        return m_xTextRange;
    }

    abstract int getNumPageBreaks();

    class PageBreak {
        Block m_blockBefore;
        Block m_blockAfter;

        PageBreak(Block blockBefore, Block blockAfter) {
            m_blockBefore = blockBefore;
            m_blockAfter = blockAfter;
        }

        Block getBlockBefore() {
            return m_blockBefore;
        }

        Block getBlockAfter() {
            return m_blockAfter;
        }
    }

    abstract PageBreak breakPageAt(int pos);

    public abstract void emit(HTMLBuilder builder);
    abstract void emitInsideFootnote(HTMLBuilder builder);
    public abstract void collectFootnotes(List<FootnoteTextPortion> footnotes);
}
