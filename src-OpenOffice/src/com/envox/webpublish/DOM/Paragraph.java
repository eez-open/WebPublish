/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexReplace;
import com.sun.star.style.BreakType;
import com.sun.star.style.NumberingType;
import com.sun.star.uno.UnoRuntime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class Paragraph extends Block {
    private List<TextPortion> m_textPortions = new ArrayList<TextPortion>();
    private List<Integer> m_pageBreaks = new ArrayList<Integer>();
    private String m_id;
    private String m_paraStyleName;
    private Style m_style;
    private Paragraph m_createdFromParagraph;
    private int m_listNum;
    
    private boolean m_outlineNumbering = false;
    private short m_outlineNumberingLevel;
    private short m_outlineNumberingStartWith;
    private short m_outlineNumberingParentNumbering;
    private String m_outlineNumberingPrefix;
    private String m_outlineNumberingSuffix;

    Paragraph(Document doc) {
        super(doc);
    }

    Paragraph(Document doc, Object xObject, boolean firstParagraph) {
        super(doc, xObject);

        // Map<String, Object> numberingRule = new TreeMap<String, Object>();
        Short numberingLevel = (Short) getProperty("NumberingLevel");
        if (numberingLevel != null) {
            Short numberingType = null;
            Short startWith = null;
            Short parentNumbering = null;
            String prefix = null;
            String suffix = null;
        
            XIndexReplace xNumberingRules = (XIndexReplace) UnoRuntime.queryInterface(XIndexReplace.class, getProperty("NumberingRules"));
            if (xNumberingRules != null) {
                try {
                    PropertyValue[] properties = (PropertyValue[]) xNumberingRules.getByIndex(numberingLevel);
                    for (PropertyValue p: properties) {
                        if (p.Name.equals("NumberingType")) {
                            numberingType = (Short) p.Value;
                        } else if (p.Name.equals("StartWith")) {
                            startWith = (Short) p.Value;
                        } else if (p.Name.equals("ParentNumbering")) {
                            parentNumbering = (Short) p.Value;
                        } else if (p.Name.equals("Prefix")) {
                            prefix = (String) p.Value;
                        } else if (p.Name.equals("Sufix")) {
                            suffix = (String) p.Value;
                        }
                        // numberingRule.put(p.Name, p.Value);
                    }
                } catch (com.sun.star.uno.Exception ex) {
                }
            }
            
            if (numberingType != null && numberingType.shortValue() == NumberingType.ARABIC) {
                if (startWith == null) startWith = 1;
                if (parentNumbering == null) parentNumbering = 1;
                if (prefix == null) prefix = "";
                if (suffix == null) suffix = ".";
                
                m_outlineNumbering = true;
                m_outlineNumberingLevel = numberingLevel.shortValue();
                m_outlineNumberingStartWith = startWith.shortValue();
                m_outlineNumberingParentNumbering = parentNumbering.shortValue();
                m_outlineNumberingPrefix = prefix;
                m_outlineNumberingSuffix = suffix;
            }
        }
        
        m_paraStyleName = (String) getProperty("ParaStyleName");
        m_style = m_doc.createParagraphStyle(xObject, m_paraStyleName);

        BreakType breakType = BreakType.NONE;
        try {
            breakType = (BreakType) getProperty("BreakType");
        } catch (Exception ex) {
        }

        if (!firstParagraph && (breakType == BreakType.PAGE_BEFORE || breakType == BreakType.PAGE_BOTH)) {
            m_pageBreaks.add(m_textPortions.size());
        }

        load();

        if (breakType == BreakType.PAGE_AFTER || breakType == BreakType.PAGE_BOTH) {
            m_pageBreaks.add(m_textPortions.size());
        }
    }

    private void load() {
        XEnumerationAccess xTextAccess = (XEnumerationAccess) queryInterface(XEnumerationAccess.class);
        XEnumeration xTextEnum = xTextAccess.createEnumeration();
        while (xTextEnum.hasMoreElements()) {
            Object xObject;
            try {
                xObject = xTextEnum.nextElement();
            } catch (Exception ex) {
                break;
            }

            XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xObject);
            String textPortionType;
            try {
                textPortionType = (String) xPropertySet.getPropertyValue("TextPortionType");
            } catch (Exception ex) {
                continue;
            }

            if (textPortionType.equals("SoftPageBreak")) {
                m_pageBreaks.add(m_textPortions.size());
            } else  if (textPortionType.equals("Text")) {
                m_textPortions.add(m_doc.createTextTextPortion(this, xObject));
            } else if (textPortionType.equals("Footnote")) {
                m_textPortions.add(m_doc.createFootnoteTextPortion(this, xObject));
            } else if (textPortionType.equals("TextField")) {
                m_textPortions.add(m_doc.createTextTextPortion(this, xObject));
            } else if (textPortionType.equals("Bookmark")) {
                m_textPortions.add(m_doc.createBookmarkTextPortion(this, xObject));
            }
        }
    }

    Paragraph cloneStyle() {
        Paragraph paragraph = m_doc.createParagraph();
        paragraph.m_paraStyleName = m_paraStyleName;
        paragraph.m_style = m_style.cloneStyle();
        return paragraph;
    }

    public void setID(String id) {
        m_id = id;
    }

    public String getID() {
        return m_id;
    }

    Style getStyle() {
        return m_style;
    }
    
    String getStyleName() {
        return m_style.getParentParagraphStyle().getName();
    }

    boolean isListItem() {
        return m_style.getNumberingLevel() != -1;
    }

    List<TextPortion> getTextPortions() {
        return m_textPortions;
    }

    void addTextPortion(TextPortion textPortion) {
        m_textPortions.add(textPortion);
        textPortion.setParagraph(this);
    }

    public String getText() {
        return m_xTextRange.getString();
    }

    public short getOutlineLevel() {
        return m_style.getOutlineLevel();
    }

    short getNumberingLevel() {
        return m_style.getNumberingLevel();
    }

    short getNumberingType() {
        return m_style.getNumberingType();
    }

    int getListNum() {
        return m_listNum;
    }

    void setListNum(int listNum) {
        m_listNum = listNum;
    }

    Paragraph getCreatedFromParagraph() {
        return m_createdFromParagraph;
    }

    int getNumPageBreaks() {
        return m_pageBreaks.size();
    }

    PageBreak breakPageAt(int pos) {
        int i = m_pageBreaks.get(pos-1);

        if (i == 0) {
            m_pageBreaks.remove(0);
            return new PageBreak(null, this);
        }

        return new PageBreak(this, null);
    }

    public void emit(HTMLBuilder builder) {
        if (StringUtils.equals(m_paraStyleName, m_doc.getHtmlCodeStyle())) {
            StringBuilder html = new StringBuilder();
            for (TextPortion textPortion:m_textPortions) {
                html.append(textPortion.getText());
            }
            builder.appendHtml(html.toString());
        } else {
            String tagName = isListItem() ? "li" : m_doc.getParagraphTagName(m_paraStyleName);
            builder.beginTag(tagName);

            if (m_id != null) {
                builder.attr("id", m_id);
            }

            String classAttr = m_paraStyleName == null || m_doc.isDeafaultParagraph(m_paraStyleName) ? "" :
                m_style.getParentParagraphStyle().getCssClassName();
            if (StringUtils.isNotBlank(classAttr)) {
                builder.attr("class", classAttr);
            }

            String styleAttr = m_style.getCssStyle();
            if (StringUtils.isNotBlank(styleAttr)) {
                builder.attr("style", styleAttr);
            }

            if (m_outlineNumbering) {
                m_doc.emitOutlineNumber(builder, m_outlineNumberingLevel, m_outlineNumberingParentNumbering, m_outlineNumberingStartWith, m_outlineNumberingPrefix, m_outlineNumberingSuffix);
            }
            
            if (m_textPortions.isEmpty() || m_textPortions.size() == 1 && StringUtils.isBlank(m_textPortions.get(0).getText())) {
                builder.append("\u00a0"); // nbsp
            } else {
                for (TextPortion textPortion:m_textPortions) {
                    textPortion.emit(builder);
                }
            }

            builder.endTag(tagName);
        }
    }

    void emitInsideFootnote(HTMLBuilder builder) {
        for (TextPortion textPortion:m_textPortions) {
            textPortion.emit(builder);
        }
    }

    public void collectFootnotes(List<FootnoteTextPortion> footnotes) {
        for (TextPortion textPortion:m_textPortions) {
            if (textPortion instanceof FootnoteTextPortion) {
                footnotes.add((FootnoteTextPortion) textPortion);
            }
        }
    }
}
