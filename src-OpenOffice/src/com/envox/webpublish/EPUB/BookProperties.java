/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.EPUB;

import com.envox.webpublish.DocumentProperties;
import com.sun.star.lang.Locale;
import com.sun.star.text.XTextDocument;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class BookProperties {
    DocumentProperties m_documentProperties;

    private String m_id;
    private String m_title;
    private String m_language;
    private String m_author;
    private String m_ISBN;
    private String m_publisher;
    private String m_subject;
    private String m_description;

    public BookProperties(XTextDocument xTextDocument) {
        m_documentProperties = new DocumentProperties(xTextDocument);
    }

    public String getID() { return m_id; }
    public void setID(String id) { m_id = id; }

    public String getTitle() { return m_title; }
    public void setTitle(String title) { m_title = title; }

    public String getLanguage() { return m_language; }
    public void setLanguage(String language) { m_language = language; }

    public String getAuthor() { return m_author; }
    public void setAuthor(String author) { m_author = author; }

    public String getISBN() { return m_ISBN; }
    public void setISBN(String ISBN) { m_ISBN = ISBN; }

    public String getPublisher() { return m_publisher; }
    public void setPublisher(String publisher) { m_publisher = publisher; }

    public String getSubject() { return m_subject; }
    public void setSubject(String subject) { m_subject = subject; }

    public String getDescription() { return m_description; }
    public void setDescription(String description) { m_description = description; }

    public void read() {
        m_id = m_documentProperties.readCustomProperty("WebPublish_EPUB_ID", UUID.randomUUID().toString());
        m_title = m_documentProperties.readCustomProperty("WebPublish_EPUB_Title", m_documentProperties.getTitle());

        try {
            Locale locale = (Locale) m_documentProperties.readCustomProperty("CharLocale");
            m_language = locale.Language + "-" + locale.Country;
        } catch (Exception ex) {
            Logger.getLogger(BookProperties.class.getName()).log(Level.SEVERE, null, ex);
            m_language = "en-US"; // TODO ponuditi da se editira
        }

        m_author = m_documentProperties.readCustomProperty("WebPublish_EPUB_Author", m_documentProperties.getAuthor());
        m_ISBN = m_documentProperties.readCustomProperty("WebPublish_EPUB_ISBN", "");
        m_publisher = m_documentProperties.readCustomProperty("WebPublish_EPUB_Publisher", "");
        m_subject = m_documentProperties.readCustomProperty("WebPublish_EPUB_Subject", "");
        m_description = m_documentProperties.readCustomProperty("WebPublish_EPUB_Description", m_documentProperties.getDescription());
    }

    public void write() {
        m_documentProperties.writeCustomProperty("WebPublish_EPUB_Title", "", m_title);
        m_documentProperties.writeCustomProperty("WebPublish_EPUB_Author", "", m_author);
        m_documentProperties.writeCustomProperty("WebPublish_EPUB_ISBN", "", m_ISBN);
        m_documentProperties.writeCustomProperty("WebPublish_EPUB_Publisher", "", m_publisher);
        m_documentProperties.writeCustomProperty("WebPublish_EPUB_Subject", "", m_subject);
        m_documentProperties.writeCustomProperty("WebPublish_EPUB_Description", "", m_description);
    }
}
