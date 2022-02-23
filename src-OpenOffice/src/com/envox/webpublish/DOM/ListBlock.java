/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.style.NumberingType;

/**
 *
 * @author martin
 */
public class ListBlock extends BlockContainer {
    private ListBlock m_parent;
    private short m_numberingLevel;
    private short m_numberingType;
    private String m_tag;
    private int m_startListNum = -1;

    ListBlock(Document doc, ListBlock parent, short numberingType) {
        super(doc);

        m_parent = parent;

        m_numberingLevel = (short) (parent == null ? 0 : parent.getNumberingLevel() + 1);
        m_numberingType = numberingType;
        m_tag = m_numberingType == NumberingType.CHAR_SPECIAL ? "ul" : "ol";
    }

    ListBlock getParent() {
        return m_parent;
    }

    short getNumberingLevel() {
        return m_numberingLevel;
    }

    short getNumberingType() {
        return m_numberingType;
    }

    @Override
    void addBlock(Block block) {
        if (block instanceof Paragraph) {
            Paragraph paragraph = (Paragraph) block;
            if (paragraph.getCreatedFromParagraph() != null) {
                paragraph.setListNum(paragraph.getCreatedFromParagraph().getListNum());
            }
            if (m_startListNum == -1)
                m_startListNum = paragraph.getListNum();
        }
        super.addBlock(block);
    }

    @Override
    public void emit(HTMLBuilder builder) {
        builder.beginTag(m_tag);

        if (m_tag.equals("ol") && m_startListNum > 1) {
            builder.attr("start", Integer.toString(m_startListNum));
        }

        super.emit(builder);

        builder.endTag(m_tag);
    }
}
