/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.text.XTextFrame;
import com.sun.star.uno.UnoRuntime;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
class TextFrame extends BlockContainer {

    private XTextFrame m_xTextFrame;

    private String m_src;
    private String m_title;
    private Style m_styleDiv;
    private Style m_styleImg;

    TextFrame(Document doc, Object xObject) {
        super(doc, ((XTextFrame) UnoRuntime.queryInterface(XTextFrame.class, xObject)).getText());

        String backGraphicURL = (String) getProperty("BackGraphicURL");
        if (StringUtils.isNotEmpty(backGraphicURL)) {
            String name = (String) getProperty("LinkDisplayName");
            m_src = m_doc.registerImageURL(backGraphicURL, name);
            m_title = (String) getProperty("Title");
            if (StringUtils.isBlank(m_title)) {
                m_title = name;
            }

            m_styleDiv = m_doc.createStyle(xObject);
            m_styleDiv.loadImageDivProperties();

            m_styleImg = m_doc.createStyle(xObject);
            m_styleImg.loadImageProperties();
        }
        
    }

    @Override
    public void emit(HTMLBuilder builder) {
        builder.beginTag("div");

        if (StringUtils.isNotEmpty(m_src)) {
            builder.beginTag("div");

            String cssStyleDiv = m_styleDiv.getCssStyle();
            if (StringUtils.isNotBlank(cssStyleDiv)) {
                builder.attr("style", cssStyleDiv);
            }

            builder.beginTag("img");

            builder.attr("src", m_doc.getImageURL(m_src));
            builder.attr("alt", m_title);

            String cssStyleImg = m_styleImg.getCssStyle();
            if (StringUtils.isNotBlank(cssStyleImg)) {
                builder.attr("style", cssStyleImg);
            }

            builder.endTag("img");

            builder.endTag("div");
        }

        super.emit(builder);

        builder.endTag("div");
    }
}
