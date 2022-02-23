/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.container.XNamed;
import com.sun.star.uno.UnoRuntime;

/**
 *
 * @author martin
 */
class BookmarkTextPortion extends TextPortion {
    private String m_hyperLinkName;

    BookmarkTextPortion(Paragraph paragraph, Document doc, Object xObject) {
        super(paragraph, doc, xObject);

        Object xBookmark = getProperty("Bookmark");
        XNamed xBookmarkNamed = (XNamed) UnoRuntime.queryInterface(XNamed.class, xBookmark);
        m_hyperLinkName = xBookmarkNamed.getName();
    }

    void emit(HTMLBuilder builder) {
        builder.beginTag("a");
        builder.attr("name", m_hyperLinkName);
        builder.endTag("a");
    }
}
