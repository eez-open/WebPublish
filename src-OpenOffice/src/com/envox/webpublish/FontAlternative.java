/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author martin
 */
public class FontAlternative {
    private String m_font = "";
    private String m_alternative = "";

    public FontAlternative() {
    }

    public String getFont() { return m_font; }
    public void setFont(String font) { m_font = font; }

    public String getAlternative() { return m_alternative; }
    public void setAlternative(String alternative) { m_alternative = alternative; }

    public static List<FontAlternative> clone(List<FontAlternative> alternatives) {
        List<FontAlternative> newAlternatives = new ArrayList<FontAlternative>();

        for (FontAlternative alternative:alternatives) {
            FontAlternative newAlternative = new FontAlternative();

            newAlternative.setFont(alternative.getFont());
            newAlternative.setAlternative(alternative.getAlternative());

            newAlternatives.add(newAlternative);
        }

        return newAlternatives;
    }

    public void loadFromXML(Element parentElement) {
        m_font = Util.getTagValue("Font", parentElement);
        m_alternative = Util.getTagValue("Alternative", parentElement);
    }

    public void saveToXML(Element parentElement, Document doc) {
        Util.setTagValue("Font", m_font, parentElement);
        Util.setTagValue("Alternative", m_alternative, parentElement);
    }
}
