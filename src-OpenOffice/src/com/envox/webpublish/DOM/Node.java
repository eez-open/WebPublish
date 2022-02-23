/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.uno.UnoRuntime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
class Node {
    protected Document m_doc;
    protected Object m_xObject;
    protected XPropertySet m_xPropertySet;
    protected XPropertySetInfo m_xPropertySetInfo;

    Node(Document doc) {
        m_doc = doc;
    }

    Node(Document doc, Object xObject) {
        m_doc = doc;
        m_xObject = xObject;
        m_xPropertySet = (XPropertySet) queryInterface(XPropertySet.class);
        if (m_xPropertySet != null) {
            m_xPropertySetInfo = m_xPropertySet.getPropertySetInfo();
        }
    }

    final Object queryInterface(Class zInterface) {
        if (m_xObject == null) {
            return null;
        }
        return UnoRuntime.queryInterface(zInterface, m_xObject);
    }

    boolean hasProperty(String name) {
        return m_xPropertySetInfo != null && m_xPropertySet != null && m_xPropertySetInfo.hasPropertyByName(name);
    }

    Object getProperty(String name) {
        if (hasProperty(name)) {
            try {
                return m_xPropertySet.getPropertyValue(name);
            } catch (Exception ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
