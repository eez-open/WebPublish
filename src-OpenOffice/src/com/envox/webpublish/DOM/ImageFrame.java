/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
class ImageFrame extends Block {
    private String m_src;
    private String m_title;
    private Style m_styleDiv;
    private Style m_styleImg;

    private String m_hyperLinkURL;
    private String m_hyperLinkName;
    private String m_hyperLinkTarget;

    ImageFrame(Document doc, Object xObject) {
        super(doc, xObject);

        String graphicURL = (String) getProperty("GraphicURL");

        String name = (String) getProperty("LinkDisplayName");
        m_src = m_doc.registerImageURL(graphicURL, name);

        m_title = (String) getProperty("Title");
        if (StringUtils.isBlank(m_title)) {
            m_title = name;
        }

        m_styleDiv = m_doc.createStyle(xObject);
        m_styleDiv.loadImageDivProperties();

        m_styleImg = m_doc.createStyle(xObject);
        m_styleImg.loadImageProperties();

        m_hyperLinkURL = (String) getProperty("HyperLinkURL");
        m_hyperLinkName = (String) getProperty("HyperLinkName");
        m_hyperLinkTarget = (String) getProperty("HyperLinkTarget");
    }

    boolean isHyperlink() {
        return StringUtils.isNotBlank(m_hyperLinkURL) || StringUtils.isNotBlank(m_hyperLinkName);
    }

    int getNumPageBreaks() {
        return 0;
    }

    PageBreak breakPageAt(int pos) {
        throw new UnsupportedOperationException();
    }

    public void emit(HTMLBuilder builder) {
        builder.beginTag("div");

        String cssStyleDiv = m_styleDiv.getCssStyle();
        if (StringUtils.isNotBlank(cssStyleDiv)) {
            builder.attr("style", cssStyleDiv);
        }

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
        }

        builder.beginTag("img");

        builder.attr("src", m_doc.getImageURL(m_src));
        builder.attr("alt", m_title);

        String cssStyleImg = m_styleImg.getCssStyle();
        if (StringUtils.isNotBlank(cssStyleImg)) {
            builder.attr("style", cssStyleImg);
        }

        builder.endTag("img");

        if (isHyperlink()) {
            builder.endTag("a");
        }

        builder.endTag("div");
    }

    void emitInsideFootnote(HTMLBuilder builder) {
        // do nothing
    }

    public void collectFootnotes(List<FootnoteTextPortion> footnotes) {
        // nothing to do
    }
}
