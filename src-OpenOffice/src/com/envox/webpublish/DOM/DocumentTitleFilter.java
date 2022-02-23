/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class DocumentTitleFilter extends Filter {
    private String[] m_documentTitleStyles;

    DocumentTitleFilter(Document doc, String documentTitleStyle) {
        super(doc);

        m_documentTitleStyles = StringUtils.stripAll(StringUtils.split(documentTitleStyle, ","));
    }

    public List<Block> process(BlockContainer parent, List<Block> blocks) {
        if (!(parent instanceof MainBlock)) {
            return blocks;
        }

        List<Block> result = new ArrayList<Block>();

        m_doc.filterStatus("Document title filter ...");
        int i = 0;
        for (Block block:blocks) {
            if (block instanceof Paragraph) {
                Paragraph paragraph = (Paragraph) block;
                if (ArrayUtils.contains(m_documentTitleStyles, paragraph.getStyleName())) {
                    continue;
                }
            }
            result.add(block);
            m_doc.filterProgress(++i, blocks.size());
        }

        return result;
    }
}
