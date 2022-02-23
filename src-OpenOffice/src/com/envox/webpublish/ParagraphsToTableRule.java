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
public class ParagraphsToTableRule {
    private String m_paragraphs = "";
    private String m_tableClass = "";
    private String m_tableCSS = "";

    public ParagraphsToTableRule() {
    }

    public String getParagraphs() { return m_paragraphs; }
    public void setParagraphs(String paragraphs) { m_paragraphs = paragraphs; }

    public String getTableClass() { return m_tableClass; }
    public void setTableClass(String tableClass) { m_tableClass = tableClass; }

    public String getTableCSS() { return m_tableCSS; }
    public void setTableCSS(String tableCSS) { m_tableCSS = tableCSS; }

    public static List<ParagraphsToTableRule> clone(List<ParagraphsToTableRule> rules) {
        List<ParagraphsToTableRule> newRules = new ArrayList<ParagraphsToTableRule>();

        for (ParagraphsToTableRule rule:rules) {
            ParagraphsToTableRule newRule = new ParagraphsToTableRule();

            newRule.setParagraphs(rule.getParagraphs());
            newRule.setTableClass(rule.getTableClass());
            newRule.setTableCSS(rule.getTableCSS());

            newRules.add(newRule);
        }

        return newRules;
    }

    public void loadFromXML(Element parentElement) {
        m_paragraphs = Util.getTagValue("Paragraphs", parentElement);
        m_tableClass = Util.getTagValue("TableClass", parentElement);
        m_tableCSS = Util.getTagValue("TableCSS", parentElement);
    }

    public void saveToXML(Element parentElement, Document doc) {
        Util.setTagValue("Paragraphs", m_paragraphs, parentElement);
        Util.setTagValue("TableClass", m_tableClass, parentElement);
        Util.setTagValue("TableCSS", m_tableCSS, parentElement);
    }
}
