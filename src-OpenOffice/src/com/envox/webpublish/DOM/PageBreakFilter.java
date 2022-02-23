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
public class PageBreakFilter extends Filter {
    private int m_freq;
    private String m_html;
    private int m_counter;

    PageBreakFilter(Document doc, int freq, String html) {
        super(doc);

        m_freq = freq;
        m_html = html;
    }

    public List<Block> process(BlockContainer parent, List<Block> blocks) {
        if (!(parent instanceof MainBlock)) {
            return blocks;
        }

        List<Block> result = new ArrayList<Block>();

        m_counter = m_freq;

        m_doc.filterStatus("Page break filter ...");
        int i = 0;
        for (Block block:blocks) {
            while (m_counter - block.getNumPageBreaks() <= 0) {
                Block.PageBreak pageBreak = block.breakPageAt(m_counter);

                m_counter = m_freq;

                if (pageBreak.getBlockBefore() != null) {
                    result.add(pageBreak.getBlockBefore());
                }

                PageBreak pageBreakBlock = m_doc.createPageBreak();
                pageBreakBlock.setHtml(m_html);
                result.add(pageBreakBlock);
                
                block = pageBreak.getBlockAfter();
                if (block == null) {
                    break;
                }
                m_doc.filterProgress(++i, blocks.size());
            }

            if (block != null) {
                m_counter -= block.getNumPageBreaks();

                if (block != null) {
                    result.add(block);
                }
            }
        }

        // if the last block is PageBreak than remove it
        if (result.size() > 0 && result.get(result.size()-1) instanceof PageBreak) {
            result.remove(result.size()-1);
        }

        return result;
    }
}
