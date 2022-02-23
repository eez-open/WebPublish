/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.style.NumberingType;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author martin
 */
public class ListFilter extends Filter {

    ListFilter(Document doc) {
        super(doc);
    }

    public List<Block> process(BlockContainer parent, List<Block> blocks) {
        List<Block> result = new ArrayList<Block>();

        ListBlock listBlock = null;

        int[] listNum = new int[10];

        if (parent instanceof MainBlock) {
            m_doc.filterStatus("List filter ...");
        }
        int i = 0;
        for (Block block:blocks) {
            if (block instanceof Paragraph) {
                Paragraph paragraph = (Paragraph) block;
                if (paragraph.getNumberingLevel() >= 0) {
                    for (int j = paragraph.getNumberingLevel() + 1; j < listNum.length; ++j) {
                        listNum[j] = 0;
                    }
                    if (paragraph.hasProperty("ParaIsNumberingRestart") && (Boolean)paragraph.getProperty("ParaIsNumberingRestart")) {
                        listNum[paragraph.getNumberingLevel()] = (Short)paragraph.getProperty("NumberingStartValue");
                    } else {
                        if (paragraph.getNumberingType() != NumberingType.CHAR_SPECIAL && paragraph.getCreatedFromParagraph() == null) {
                            listNum[paragraph.getNumberingLevel()]++;
                        }
                    }
                    paragraph.setListNum(listNum[paragraph.getNumberingLevel()]);

                    while (listBlock != null && (listBlock.getNumberingLevel() > paragraph.getNumberingLevel() ||
                            (listBlock.getNumberingLevel() == paragraph.getNumberingLevel() &&
                            listBlock.getNumberingType() != paragraph.getNumberingType()))) {
                        listBlock = listBlock.getParent();
                    }

                    while (listBlock == null || listBlock.getNumberingLevel() < paragraph.getNumberingLevel()) {
                        short numberingLevel = (short) (listBlock == null ? 0 : listBlock.getNumberingLevel() + 1);
                        short numberingType = numberingLevel == paragraph.getNumberingLevel() ?
                                paragraph.getNumberingType() : NumberingType.CHAR_SPECIAL;

                        ListBlock temp = m_doc.createListBlock(listBlock, numberingType);

                        if (listBlock != null) {
                            listBlock.addBlock(temp);
                        } else {
                            result.add(temp);
                        }

                        listBlock = temp;
                    }

                    listBlock.addBlock(paragraph);

                    continue;
                }
            }

            listBlock = null;
            result.add(block);
            if (parent instanceof MainBlock) {
                m_doc.filterProgress(++i, blocks.size());
            }
        }

        return result;
    }
}
