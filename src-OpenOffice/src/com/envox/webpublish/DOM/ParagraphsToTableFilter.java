/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.envox.webpublish.ParagraphsToTableRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
class ParagraphsToTableFilter extends Filter {
    List<ParagraphsToTableRule> m_rules;
    Set<ParagraphsToTableRule> m_usedRules = new HashSet<ParagraphsToTableRule>();

    ParagraphsToTableFilter(Document doc, List<ParagraphsToTableRule> rules) {
        super(doc);
        m_rules = rules;
    }

    public List<Block> process(BlockContainer parent, List<Block> blocks) {
        List<Block> result = new ArrayList<Block>();

        List<Paragraph> tableParagraphs = new ArrayList<Paragraph>();
        ParagraphsToTableRule tableRule = null;

        if (parent instanceof MainBlock) {
            m_doc.filterStatus("Paragraphs to table filter ...");
        }
        int i = 0;
        for (Block block:blocks) {
            Paragraph paragraph = null;
            String paragraphStyleName = null;
            ParagraphsToTableRule rule = null;
            if (block instanceof Paragraph) {
                paragraph = (Paragraph) block;
                paragraphStyleName = paragraph.getStyleName();
                rule = findRule(paragraphStyleName);
            }

            if (block instanceof BlockContainer) {
                Logger.getLogger(BlockContainer.class.getName()).log(Level.INFO, paragraphStyleName, paragraphStyleName);
            }

            if (tableRule != null && (paragraph == null || rule != tableRule)) {
                result.add(paragraphsToTable(tableParagraphs, tableRule));
                tableParagraphs.clear();
                tableRule = null;
            }

            if (rule != null) {
                tableParagraphs.add(paragraph);
                tableRule = rule;
            } else {
                result.add(block);
            }

            if (parent instanceof MainBlock) {
                m_doc.filterProgress(++i, blocks.size());
            }
        }

        if (tableRule != null) {
            result.add(paragraphsToTable(tableParagraphs, tableRule));
        }

        return result;
    }

    private ParagraphsToTableRule findRule(String paragraphStyleName) {
        for (ParagraphsToTableRule rule:m_rules) {
            if (ArrayUtils.contains(StringUtils.stripAll(StringUtils.split(rule.getParagraphs(), ",")), paragraphStyleName)) {
                return rule;
            }
        }
        return null;
    }

    private Table paragraphsToTable(List<Paragraph> paragraphs, ParagraphsToTableRule rule) {

        if (!m_usedRules.contains(rule)) {
            m_usedRules.add(rule);
            if (StringUtils.isNotBlank(rule.getTableCSS())) {
                m_doc.addCSS(rule.getTableCSS());
            }
        }

        Table table = m_doc.createTable();

        if (StringUtils.isNotBlank(rule.getTableClass())) {
            table.setClass(rule.getTableClass());
        }

        for (Paragraph paragraph:paragraphs) {
            TableRow row = m_doc.createTableRow();

            Paragraph cellParagraph = paragraph.cloneStyle();

            List<TextPortion> textPortions = paragraph.getTextPortions();
            for (TextPortion textPortion:textPortions) {
                if (textPortion instanceof TextTextPortion) {
                    TextTextPortion textTextPortion = (TextTextPortion) textPortion;
                    String text = textTextPortion.getText();
                    if (!textTextPortion.isHyperlink() && text.contains("\t")) {
                        String[] columns = StringUtils.splitPreserveAllTokens(text, "\t");
                        if (columns.length > 1) {
                            addTextPortion(cellParagraph, textTextPortion, columns[0]);
                            for (int i = 1; i < columns.length; ++i) {
                                addCell(row, cellParagraph);
                                cellParagraph = paragraph.cloneStyle();
                                addTextPortion(cellParagraph, textTextPortion, columns[i]);
                            }
                        } else {
                            cellParagraph.addTextPortion(textPortion);
                            addCell(row, cellParagraph);
                            cellParagraph = paragraph.cloneStyle();
                        }
                    } else {
                        cellParagraph.addTextPortion(textPortion);
                    }
                } else {
                    cellParagraph.addTextPortion(textPortion);
                }
            }

            if (cellParagraph.getTextPortions().size() > 0) {
                addCell(row, cellParagraph);
            }

            table.addRow(row);
        }

        return table;
    }

    private void addTextPortion(Paragraph paragraph, TextTextPortion textTextPortion, String text) {
        TextTextPortion temp = textTextPortion.cloneStyle();
        temp.setText(text);
        paragraph.addTextPortion(temp);
    }

    private void addCell(TableRow row, Paragraph paragraph) {
        TableCell cell = m_doc.createTableCell();
        cell.addBlock(paragraph);
        row.addCell(cell);
    }
}
