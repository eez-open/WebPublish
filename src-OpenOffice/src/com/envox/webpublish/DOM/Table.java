/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.table.XTableRows;
import com.sun.star.text.XTextTable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
class Table extends BlockContainer {
    private XTextTable m_xTextTable;
    private boolean m_parseFailed;
    private Style m_style;
    private String m_class;

    Table(Document doc, Object xObject) {
        super(doc, xObject);

        m_xTextTable = (XTextTable) queryInterface(XTextTable.class);

        TableParser tableParser;
        try {
            tableParser = new TableParser(m_xTextTable);
        } catch (Exception ex) {
            m_parseFailed = true;
            return;
        }

        m_style = m_doc.createStyle(xObject);
        m_style.loadTableProperties();

        int width = Math.min(m_doc.getBodyWidth(), tableParser.getTableWidth()); // we don't want table bigger than 100%
        m_style.addSizeProperty("width", width);

        XTableRows xTableRows = m_xTextTable.getRows();

        for (int i = 0; i < tableParser.getRows(); ++i) {

            Object xTableRow = null;
            try {
                xTableRow = xTableRows.getByIndex(i);
            } catch (Exception ex) {
                Logger.getLogger(Table.class.getName()).log(Level.SEVERE, null, ex);
            }

            TableRow row = xTableRow != null ? m_doc.createTableRow(xTableRow) : m_doc.createTableRow();


            for (int j = 0; j < tableParser.getCols(); ++j) {
                String name = tableParser.getCell(i, j);
                if (name != null) {
                    TableCell cell = m_doc.createTableCell(m_xTextTable.getCellByName(name));

                    cell.setColspan(tableParser.getColSpan(i, j));
                    cell.setRowspan(tableParser.getRowSpan(i, j));
                    cell.setWidth(tableParser.getCellWidthAsPercent(i, j));

                    row.addCell(cell);
                }
            }
            addRow(row);
        }
    }

    Table(Document doc) {
        super(doc);
    }

    Table cloneStyle() {
        Table table = m_doc.createTable();
        table.m_style = m_style;
        table.m_class = m_class;
        return table;
    }
    
    void setStyle(Style style) {
        m_style = style;
    }
    
    void setClass(String class_) {
        m_class = class_;
    }

    final void addRow(TableRow row) {
        addBlock(row);
    }

    boolean isStartOfNewPage(int iRow) {
        return iRow > 0 && iRow % 36 == 0;
    }

    @Override
    int getNumPageBreaks() {
        int numPageBreaks = 0;
        for (int iRow = 0; iRow < m_blocks.size(); ++iRow) {
            if (isStartOfNewPage(iRow)) {
                ++numPageBreaks;
            }
        }
        return numPageBreaks;
    }

    @Override
    PageBreak breakPageAt(int pos) {
        for (int iRow = 0; iRow < m_blocks.size(); ++iRow) {
            if (isStartOfNewPage(iRow)) {
                if (--pos == 0) {
                    if (iRow == 0) {
                        return new Block.PageBreak(null, this);
                    }

                    Table table = cloneStyle();
                    table.m_blocks = m_blocks.subList(0, iRow);

                    m_blocks = m_blocks.subList(iRow, m_blocks.size());

                    return new Block.PageBreak(table, this);
                }
            }
        }
        return null;
    }

    @Override
    public void emit(HTMLBuilder builder) {
        builder.beginTag("table");

        if (m_style != null) {
            builder.attr("style", m_style.getCssStyle());
        }

        if (m_class != null) {
            builder.attr("class", m_class);
        }

        super.emit(builder);

        builder.endTag("table");
    }
}
