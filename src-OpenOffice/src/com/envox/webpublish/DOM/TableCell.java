/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.table.XCell;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
class TableCell extends BlockContainer {
    private Style m_style;
    private int m_colspan = 1;
    private int m_rowspan = 1;

    TableCell(Document doc) {
        super(doc);
    }

    TableCell(Document doc, XCell xCell) {
        super(doc, (XText) UnoRuntime.queryInterface(XText.class, xCell));

        m_style = m_doc.createStyle(xCell);
        m_style.loadTableCellProperties();
    }

    void setColspan(int colspan) {
        m_colspan = colspan;
    }

    void setRowspan(int rowspan) {
        m_rowspan = rowspan;
    }

    void setWidth(int width) {
        m_style.setStyleProperty("width", String.format("%d%%", width));
    }

    @Override
    public void emit(HTMLBuilder builder) {
        builder.beginTag("td");

        if (m_colspan > 1) {
            builder.attr("colspan", Integer.toString(m_colspan));
        }

        if (m_rowspan > 1) {
            builder.attr("rowspan", Integer.toString(m_rowspan));
        }

        if (m_style != null) {
            String cssStyle = m_style.getCssStyle();
            if (StringUtils.isNotBlank(cssStyle)) {
                builder.attr("style", cssStyle);
            }
        }

        super.emit(builder);

        builder.endTag("td");
    }

}
