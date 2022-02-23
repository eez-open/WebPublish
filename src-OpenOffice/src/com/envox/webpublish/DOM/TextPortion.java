/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.text.XTextRange;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
abstract class TextPortion extends Node {
    protected Paragraph m_paragraph;
    protected String m_text;
    protected Style m_style;

    TextPortion(Document doc) {
        super(doc);
    }

    TextPortion(Paragraph paragraph, Document doc, Object xObject) {
        super(doc, xObject);

        m_paragraph = paragraph;

        XTextRange xTextRange = (XTextRange) queryInterface(XTextRange.class);
        m_text = xTextRange.getString();

        String charStyleName = (String) getProperty("CharStyleName");
        m_style = m_doc.createCharacterStyle(xObject, charStyleName, paragraph.getStyle());
    }

    abstract void emit(HTMLBuilder builder);

    Paragraph getParagraph() {
        return m_paragraph;
    }

    void setParagraph(Paragraph paragraph) {
        m_paragraph = paragraph;
    }

    String getText() {
        return m_text;
    }

    void setText(String text) {
        m_text = text;
    }

    protected void emitText(HTMLBuilder builder, String text, String cssClassName, String cssStyle) {
        if (StringUtils.isNotBlank(cssClassName) || StringUtils.isNotBlank(cssStyle)) {
            builder.beginTag("span");

            if (StringUtils.isNotBlank(cssClassName)) {
                builder.attr("class", cssClassName);
            }

            if (StringUtils.isNotBlank(cssStyle)) {
                builder.attr("style", cssStyle);
            }

            builder.append(text);

            builder.endTag("span");
        } else {
            builder.append(text);
        }
    }
}
