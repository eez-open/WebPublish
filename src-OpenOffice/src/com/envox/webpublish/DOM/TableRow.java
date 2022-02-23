/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
class TableRow extends BlockContainer {
    private Style m_style;

    TableRow(Document doc) {
        super(doc);
    }

    TableRow(Document doc, Object xTableRow) {
        super(doc, xTableRow);

        m_style = m_doc.createStyle(xTableRow);
        m_style.loadTableRowProperties();
    }

    void addCell(TableCell cell) {
        addBlock(cell);
    }
    
    @Override
    public void emit(HTMLBuilder builder) {
        builder.beginTag("tr");

        if (m_style != null) {
            String cssStyle = m_style.getCssStyle();
            if (StringUtils.isNotBlank(cssStyle)) {
                builder.attr("style", cssStyle);
            }
        }

        super.emit(builder);

        builder.endTag("tr");
    }
}
