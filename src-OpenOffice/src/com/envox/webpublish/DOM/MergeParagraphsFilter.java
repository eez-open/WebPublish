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
public class MergeParagraphsFilter extends Filter {

    MergeParagraphsFilter(Document doc) {
        super(doc);
    }

    public List<Block> process(BlockContainer parent, List<Block> blocks) {
        List<Block> result = new ArrayList<Block>();

        DivBlock divBlock = null;
        Paragraph firstParagraphInDivBlock = null;
        List<Block> images = new ArrayList<Block>();

        if (parent instanceof MainBlock) {
            m_doc.filterStatus("Merge paragraphs filter ...");
        }
        int i = 0;
        for (Block block:blocks) {
            if (block instanceof Paragraph) {
                Paragraph paragraph = (Paragraph) block;
                if (paragraph.hasProperty("ParaIsConnectBorder") && (Boolean)paragraph.getProperty("ParaIsConnectBorder")) {
                    if (divBlock != null) {
                        String[] properties = new String[] { "margin-left", "margin-right", "border-left", "border-right", "border-top", "border-bottom" };
                        if (paragraph.getStyle().getCssStyle(properties).equals(firstParagraphInDivBlock.getStyle().getCssStyle(properties))) {
                            // add all anchored images and paragraph to div block
                            for (Block imageBlock:images) {
                                divBlock.addBlock(imageBlock);
                            }
                            images.clear();
                            divBlock.addBlock(block);
                            continue;
                        }

                        addDivBlock(result, divBlock);
                        divBlock = null;
                    }

                    if (!paragraph.getStyle().getStyleProperty("border-left").equals("0") ||
                            !paragraph.getStyle().getStyleProperty("border-right").equals("0") ||
                            !paragraph.getStyle().getStyleProperty("border-top").equals("0") ||
                            !paragraph.getStyle().getStyleProperty("border-bottom").equals("0")) {
                        divBlock = m_doc.createDivBlock();
                        firstParagraphInDivBlock = paragraph;
                        // add all anchored images and paragraph to div block
                        for (Block imageBlock:images) {
                            divBlock.addBlock(imageBlock);
                        }
                        images.clear();
                        divBlock.addBlock(paragraph);
                        continue;
                    }
                }

                if (divBlock != null) {
                    addDivBlock(result, divBlock);
                    divBlock = null;
                }
            } else if (block instanceof ImageFrame || block instanceof EmbeddedObjectFrame || block instanceof TextFrame) {
                images.add(block);
                continue;
            }

            // add all anchored images and paragraph
            for (Block imageBlock:images) {
                result.add(imageBlock);
            }
            images.clear();
            result.add(block);

            if (parent instanceof MainBlock) {
                m_doc.filterProgress(++i, blocks.size());
            }
        }

        if (divBlock != null) {
            addDivBlock(result, divBlock);
            divBlock = null;
        }

        for (Block imageBlock:images) {
            result.add(imageBlock);
        }
        images.clear();

        return result;
    }

    void addDivBlock(List<Block> result, DivBlock divBlock) {
        if (divBlock.getBlocks().size() >= 2) {
            Style style = m_doc.createStyle();
            
            boolean fistParagraph = true;

            for (int i = 0; i < divBlock.getBlocks().size(); ++i) {
                Block block = divBlock.getBlocks().get(i);
                if (block instanceof Paragraph) {
                    Paragraph paragraph = (Paragraph) divBlock.getBlocks().get(i);

                    paragraph.getStyle().setStyleProperty("margin-left", "0");
                    paragraph.getStyle().setStyleProperty("margin-right", "0");
                    paragraph.getStyle().setStyleProperty("border", "0");

                    if (fistParagraph) {
                        fistParagraph = false;

                        style.setStyleProperty("margin-left", paragraph.getStyle().getStyleProperty("margin-left"));
                        style.setStyleProperty("margin-right", paragraph.getStyle().getStyleProperty("margin-right"));

                        style.setStyleProperty("border-left", paragraph.getStyle().getStyleProperty("border-left"));
                        style.setStyleProperty("border-right", paragraph.getStyle().getStyleProperty("border-right"));
                        style.setStyleProperty("border-top", paragraph.getStyle().getStyleProperty("border-top"));
                        style.setStyleProperty("border-bottom", paragraph.getStyle().getStyleProperty("border-bottom"));

                        style.setStyleProperty("margin-top", paragraph.getStyle().getStyleProperty("margin-top"));
                        paragraph.getStyle().setStyleProperty("margin-top", "0");
                    } else if (i == divBlock.getBlocks().size() - 1) {
                        style.setStyleProperty("margin-bottom", paragraph.getStyle().getStyleProperty("margin-bottom"));
                        paragraph.getStyle().setStyleProperty("margin-bottom", "0");
                    }
                }
            }

            divBlock.setStyle(style);

            divBlock.filter();

            result.add(divBlock);
        } else {
            result.add(divBlock.getBlocks().get(0));
        }
    }
}
