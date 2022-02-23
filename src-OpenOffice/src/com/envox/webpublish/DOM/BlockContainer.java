/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class BlockContainer extends Block {
    protected List<Block> m_blocks = new ArrayList<Block>();

    BlockContainer(Document doc) {
        super(doc);
    }

    BlockContainer(Document doc, Object xObject) {
        super(doc, xObject);
    }

    BlockContainer(Document doc, Block block) {
        super(doc);
        m_blocks.add(block);
    }

    BlockContainer(Document doc, XText xText) {
        super(doc, xText);

        XEnumerationAccess xParaAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xText);
        XEnumeration xParaEnum = xParaAccess.createEnumeration();
        boolean firstParagraph = true;
        while (xParaEnum.hasMoreElements()) {
            XServiceInfo xParaInfo;
            try {
                xParaInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xParaEnum.nextElement());
            } catch (Exception ex) {
                Logger.getLogger(BlockContainer.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }

            Block block;
            if (xParaInfo.supportsService("com.sun.star.text.TextTable")) {
                block = m_doc.createTable(xParaInfo);
            } else {
                block = m_doc.createParagraph(xParaInfo, firstParagraph);
            }

            m_blocks.addAll(m_doc.getFramesAtBlock(block));

            m_blocks.add(block);

            m_doc.progressParagraph();

            firstParagraph = false;
        }

        m_blocks = m_doc.filterBlocks(this, m_blocks);
    }

    List<Block> getBlocks() {
        return m_blocks;
    }

    void addBlock(Block block) {
        m_blocks.add(block);
    }

    int getNumPageBreaks() {
        return 0;
    }

    PageBreak breakPageAt(int pos) {
        throw new UnsupportedOperationException();
    }

    public void emit(HTMLBuilder builder) {
        for (Block block:m_blocks) {
            block.emit(builder);
        }
    }

    void emitInsideFootnote(HTMLBuilder builder) {
        for (Block block:m_blocks) {
            block.emitInsideFootnote(builder);
        }
    }

    public void collectFootnotes(List<FootnoteTextPortion> footnotes) {
        for (Block block:m_blocks) {
            block.collectFootnotes(footnotes);
        }
    }
}
