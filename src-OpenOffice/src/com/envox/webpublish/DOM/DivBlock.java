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
class DivBlock extends BlockContainer {
    Style m_style;

    DivBlock(Document doc) {
        super(doc);
    }

    void filter() {
        m_blocks = m_doc.filterBlocksWithoutMergeParagraphsFilter(this, m_blocks);
    }

    void setStyle(Style style) {
        m_style = style;
    }

    @Override
    public void emit(HTMLBuilder builder) {
        builder.beginTag("div");

        if (m_style != null) {
            String styleAttr = m_style.getCssStyle();
            if (StringUtils.isNotBlank(styleAttr)) {
                builder.attr("style", styleAttr);
            }
        }

        super.emit(builder);

        builder.endTag("div");
    }
}
