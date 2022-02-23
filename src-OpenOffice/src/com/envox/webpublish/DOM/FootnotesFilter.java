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
public class FootnotesFilter extends Filter {
    FootnotesFilter(Document doc) {
        super(doc);
    }

    public List<Block> process(BlockContainer parent, List<Block> blocks) {
        if (!(parent instanceof MainBlock)) {
            return blocks;
        }

        List<Block> result = new ArrayList<Block>();

        List<FootnoteTextPortion> footnotes = new ArrayList<FootnoteTextPortion>();

        m_doc.filterStatus("Footnotes filter ...");
        int i = 0;
        for (Block block:blocks) {
            if (block instanceof PageBreak) {
                if (!footnotes.isEmpty()) {
                    result.add(m_doc.createFootnotesBlock(footnotes));
                    footnotes.clear();
                }
            }
            block.collectFootnotes(footnotes);
            result.add(block);
            m_doc.filterProgress(++i, blocks.size());
        }

        if (!footnotes.isEmpty()) {
            result.add(m_doc.createFootnotesBlock(footnotes));
        }

        return result;
    }
}
