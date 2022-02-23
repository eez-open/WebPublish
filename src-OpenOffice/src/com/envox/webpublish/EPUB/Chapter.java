/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.EPUB;

import com.envox.webpublish.DOM.Block;
import com.envox.webpublish.DOM.FootnoteTextPortion;
import com.envox.webpublish.DOM.HTMLBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.codec.binary.StringUtils;

/**
 *
 * @author martin
 */
public class Chapter {
    Book m_book;
    String m_title;
    Chapter m_parent;
    int m_id;
    int m_outlineLevel;
    List<Chapter> m_chapters = new ArrayList<Chapter>();
    List<Block> m_blocks = new ArrayList<Block>();
    List<FootnoteTextPortion> m_footnotes = new ArrayList<FootnoteTextPortion>();

    Chapter(Book book) {
        m_book = book;
        m_id = 0;
        m_outlineLevel = 0;
        m_parent = null;
    }

    Chapter(Book book, String title, Chapter parent, int id) {
        m_book = book;
        m_title = title;
        m_id = id;
        m_parent = parent;
        m_parent.addChapter(this);
        m_outlineLevel = parent.getOutlineLevel() + 1;
    }

    int getOutlineLevel() {
        return m_outlineLevel;
    }

    Chapter getParent() {
        return m_parent;
    }

    void addChapter(Chapter chapter) {
        m_chapters.add(chapter);
    }

    void addBlock(Block block) {
        m_blocks.add(block);
        block.collectFootnotes(m_footnotes);
    }

    public void emitManifest(StringBuilder stringBuilder) {
        if (m_parent != null) {
            stringBuilder
                    .append("        <item id=\"").append(Book.CHAPTER_ID_PREFIX).append(m_id).append("\" href=\"").append(Book.CHAPTERS_FOLDER).append("/").append(String.format(Book.CHAPTER_XML_FILE_NAME_FORMAT, m_id)).append("\" media-type=\"application/xhtml+xml\"/>\n");
        }

        for (Chapter chapter:m_chapters) {
            chapter.emitManifest(stringBuilder);
        }
    }

    public void emitSpine(StringBuilder stringBuilder) {
        if (m_parent != null) {
            stringBuilder
                    .append("        <itemref idref=\"").append(Book.CHAPTER_ID_PREFIX).append(m_id).append("\" linear=\"yes\"/>\n");
        }

        for (Chapter chapter:m_chapters) {
            chapter.emitSpine(stringBuilder);
        }
    }
    
    public void emitNavPoint(StringBuilder stringBuilder, String indent) {
        if (m_parent != null) {
            stringBuilder
                    .append(indent).append("<navPoint class=\"part\" id=\"").append(Book.CHAPTER_ID_PREFIX).append(m_id).append("\" playOrder=\"").append(m_id).append("\">\n")
                    .append(indent).append("  <navLabel><text>").append(m_title).append("</text></navLabel>\n")
                    .append(indent).append("  <content src=\"").append(Book.CHAPTERS_FOLDER).append("/").append(String.format(Book.CHAPTER_XML_FILE_NAME_FORMAT, m_id)).append("\"/>\n");
        }

        for (Chapter chapter:m_chapters) {
            chapter.emitNavPoint(stringBuilder, indent + "  ");
        }

        if (m_parent != null) {
            stringBuilder
                    .append(indent).append("</navPoint>\n");
        }
    }

    public void save(ZipOutputStream out) throws IOException {
        if (m_parent != null) {
            HTMLBuilder builder = new HTMLBuilder();

            builder.setXHTML(true);

            builder.appendHtml("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            builder.appendHtml("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");

            builder.beginTag("html");

            builder.attr("xmlns", "http://www.w3.org/1999/xhtml");

            builder.beginTag("head");

            builder.beginTag("meta");
            builder.attr("http-equiv", "Content-Type");
            builder.attr("content", "text/html; charset=utf-8");
            builder.endTag("meta");

            builder.beginTag("title");
            builder.append(m_title);
            builder.endTag("title");

            builder.beginTag("link");
            builder.attr("rel", "stylesheet");
            builder.attr("type", "text/css");
            builder.attr("href", "../" + Book.CHAPTER_CSS_FILE_NAME);
            builder.endTag("link");

            builder.endTag("head");

            builder.beginTag("body");

            if (m_footnotes.size() > 0) {
                m_blocks.add(m_book.getDocument().createFootnotesBlock(m_footnotes));
            }

            for (Block block:m_blocks) {
                block.emit(builder);
            }

            if (m_blocks.size() == 1) {
                emitTOC(builder, 0);
            }

            builder.endTag("body");

            builder.endTag("html");

            saveFile(out,
                    Book.CHAPTERS_PATH + "/" + String.format(Book.CHAPTER_XML_FILE_NAME_FORMAT, m_id),
                    StringUtils.getBytesUtf8(builder.toString()));
        }

        m_book.progressChapter();

        for (Chapter chapter:m_chapters) {
            chapter.save(out);
        }
    }

    private void saveFile(ZipOutputStream out, String fileName, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        out.putNextEntry(entry);
        out.write(data, 0, data.length);
    }

    private void emitTOC(HTMLBuilder builder, int level) {
        if (!m_chapters.isEmpty()) {
            builder.beginTag("ul");
            builder.attr("style", String.format("list-style-type: none; margin-left: %dem; text-indent: 0;", level));
            for (Chapter chapter:m_chapters) {
                builder.beginTag("li");
                builder.beginTag("a");
                builder.attr("href", String.format(Book.CHAPTER_XML_FILE_NAME_FORMAT, chapter.m_id));
                builder.append(chapter.m_title);
                builder.endTag("a");
                builder.endTag("li");
                chapter.emitTOC(builder, ++level);
            }
            builder.endTag("ul");
        }
    }
}
