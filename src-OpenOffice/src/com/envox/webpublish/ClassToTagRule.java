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
public class ClassToTagRule {
    private String m_className = "";
    private String m_tagName = "";

    public ClassToTagRule() {
    }

    public String getClassName() { return m_className; }
    public void setClassName(String className) { m_className = className; }

    public String getTagName() { return m_tagName; }
    public void setTagName(String tagName) { m_tagName = tagName; }

    public static List<ClassToTagRule> clone(List<ClassToTagRule> rules) {
        List<ClassToTagRule> newRules = new ArrayList<ClassToTagRule>();

        for (ClassToTagRule rule:rules) {
            ClassToTagRule newRule = new ClassToTagRule();

            newRule.setClassName(rule.getClassName());
            newRule.setTagName(rule.getTagName());

            newRules.add(newRule);
        }

        return newRules;
    }

    public void loadFromXML(Element parentElement) {
        m_className = Util.getTagValue("ClassName", parentElement);
        m_tagName = Util.getTagValue("TagName", parentElement);
    }

    public void saveToXML(Element parentElement, Document doc) {
        Util.setTagValue("ClassName", m_className, parentElement);
        Util.setTagValue("TagName", m_tagName, parentElement);
    }
    
    public String getDescription() {
        return m_className + " -> " + m_tagName;
    }
}
