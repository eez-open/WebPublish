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
class TextTextPortion extends TextPortion {
    private String m_hyperLinkURL;
    private String m_hyperLinkName;
    private String m_hyperLinkTarget;

    TextTextPortion(Document doc) {
        super(doc);
    }

    TextTextPortion(Paragraph paragraph, Document doc, Object xObject) {
        super(paragraph, doc, xObject);

        m_hyperLinkURL = (String) getProperty("HyperLinkURL");
        m_hyperLinkName = (String) getProperty("HyperLinkName");
        m_hyperLinkTarget = (String) getProperty("HyperLinkTarget");

        if (isHyperlink()) {
            m_style.removeProperty("color");
        }
    }

    TextTextPortion cloneStyle() {
        TextTextPortion textTextPortion = m_doc.createTextTextPortion();
        textTextPortion.m_style = m_style;
        return textTextPortion;
    }

    boolean isHyperlink() {
        return StringUtils.isNotBlank(m_hyperLinkURL) || StringUtils.isNotBlank(m_hyperLinkName);
    }

    void emit(HTMLBuilder builder) {
        String cssClassName = m_style.getParentCharacterStyle() != null ?
            m_style.getParentCharacterStyle().getCssClassName() : "";
        String cssStyle = m_style.getCssStyle();

        if (isHyperlink()) {
            builder.beginTag("a");

            if (StringUtils.isNotBlank(m_hyperLinkURL)) {
                builder.attr("href", m_doc.shortenURL(m_hyperLinkURL));

                if (StringUtils.isNotBlank(m_hyperLinkTarget)) {
                    builder.attr("target", m_hyperLinkTarget);
                }
            }

            if (StringUtils.isNotBlank(m_hyperLinkName)) {
                builder.attr("name", m_hyperLinkName);
            }

            emitText(builder, m_text, cssClassName, cssStyle);

            builder.endTag("a");
        } else {
            emitText(builder, m_text, cssClassName, cssStyle);
        }
    }
}
