/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class DocumentProperties {

    XTextDocument m_xTextDocument;
    XDocumentProperties m_xProperties;
    XPropertyContainer m_xUserPropertyContainer;
    XPropertySet m_xUserPropertySet;

    public DocumentProperties(XTextDocument xTextDocument) {
        m_xTextDocument = xTextDocument;

        XDocumentPropertiesSupplier xSupplier = (XDocumentPropertiesSupplier) UnoRuntime.queryInterface(
                XDocumentPropertiesSupplier.class, m_xTextDocument);
        m_xProperties = xSupplier.getDocumentProperties();
        m_xUserPropertyContainer = m_xProperties.getUserDefinedProperties();
        m_xUserPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_xUserPropertyContainer);
    }

    public String getTitle() {
        return m_xProperties.getTitle();
    }

    public void setTitle(String title) {
        m_xProperties.setTitle(title);
    }

    public String getAuthor() {
        return m_xProperties.getAuthor();
    }

    public void setAuthor(String title) {
        m_xProperties.setAuthor(title);
    }

    public String getDescription() {
        return m_xProperties.getDescription();
    }

    public void setDescription(String title) {
        m_xProperties.setDescription(title);
    }

    public String[] getKeywords() {
        return m_xProperties.getKeywords();
    }

    public void setKeywords(String[] keywords) {
        m_xProperties.setKeywords(keywords);
    }

    public Object readCustomProperty(String name) {
        try {
            return m_xUserPropertySet.getPropertyValue(name);
        } catch (Exception ex) {
            return null;
        }
    }

    public String readCustomProperty(String name, String defaultValue) {
        try {
            return (String) m_xUserPropertySet.getPropertyValue(name);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public boolean readCustomProperty(String name, boolean defaultValue) {
        try {
            return Integer.parseInt((String) m_xUserPropertySet.getPropertyValue(name)) == 1 ? true : false;
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public int readCustomProperty(String name, int defaultValue) {
        try {
            return Integer.parseInt((String) m_xUserPropertySet.getPropertyValue(name));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public void writeCustomProperty(String name, Object defaultValue, Object value) {
        try {
            if (value != null && !value.equals(defaultValue)) {
                try {
                    m_xUserPropertyContainer.addProperty(name, (short) (PropertyAttribute.OPTIONAL | PropertyAttribute.REMOVEABLE), defaultValue);
                } catch (Exception ex) {
                }
                m_xUserPropertySet.setPropertyValue(name, value);
            } else {
                try {
                    m_xUserPropertyContainer.removeProperty(name);
                } catch (Exception ex) {
                }
            }
        } catch (Exception ex) {
        }
    }
}
