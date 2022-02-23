/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.awt.FontSlant;
import com.sun.star.awt.FontStrikeout;
import com.sun.star.awt.FontUnderline;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XIndexReplace;
import com.sun.star.style.NumberingType;
import com.sun.star.table.BorderLine;
import com.sun.star.text.WrapTextMode;
import com.sun.star.uno.UnoRuntime;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
class Style extends Node {
    private String m_name;
    private Style m_parentParagraphStyle;
    private Style m_parentCharacterStyle;
    private Map<String, String> m_styleProperties = new LinkedHashMap<String, String>();

    private short m_outlineLevel = -1;
    private short m_numberingLevel = -1;
    private short m_numberingType;
    private Map<String, Object> m_numberingRule = new TreeMap<String, Object>();

    Style(Document doc) {
        super(doc);
    }

    Style(Document doc, Object xObject) {
        super(doc, xObject);
    }

    Style(Document doc, Object xObject, Style paragraphStyleParent) {
        super(doc, xObject);
        m_parentParagraphStyle = paragraphStyleParent;
    }

    Style(Document doc, Object xObject, Style characterStyleParent, Style paragraphStyleParent) {
        super(doc, xObject);
        m_parentParagraphStyle = paragraphStyleParent;
        m_parentCharacterStyle = characterStyleParent;
    }

    Style cloneStyle() {
        Style style = m_doc.createStyle();

        style.m_name = m_name;
        style.m_parentParagraphStyle = m_parentParagraphStyle;
        style.m_parentCharacterStyle = m_parentCharacterStyle;

        style.m_styleProperties = new TreeMap<String, String>();
        for (Entry<String, String> property:m_styleProperties.entrySet()) {
            style.m_styleProperties.put(property.getKey(), property.getValue());
        }

        style.m_outlineLevel = m_outlineLevel;
        style.m_numberingLevel = m_numberingLevel;
        style.m_numberingType = m_numberingType;
        style.m_numberingRule = m_numberingRule;

        return style;
    }

    String getName() {
        return m_name;
    }

    void setName(String name) {
        m_name = name;
    }

    String getCssClassName() {
        String name = m_name;
        if (name.contains(" ")) {
            name = name.replace(' ', '_');
        }
        if (name.contains(".")) {
            name = name.replace('.', '_');
        }
        return name;
    }

    String getCssStyle() {
        HTMLBuilder builder = new HTMLBuilder();
        emit(builder);
        return builder.toString();
    }

    void emit(HTMLBuilder builder) {
        for (Entry<String, String> property:m_styleProperties.entrySet()) {
            builder.cssAttr(property.getKey(), property.getValue());
        }
    }

    String getCssStyle(String[] properties) {
        HTMLBuilder builder = new HTMLBuilder();
        emit(builder, properties);
        return builder.toString();
    }

    void emit(HTMLBuilder builder, String[] properties) {
        for (String property:properties) {
            String propertyValue = getStyleProperty(property);
            if (StringUtils.isNotBlank(propertyValue)) {
                builder.cssAttr(property, propertyValue);
            }
        }
    }

    Style getParentParagraphStyle() {
        return m_parentParagraphStyle;
    }

    Style getParentCharacterStyle() {
        return m_parentCharacterStyle;
    }

    short getOutlineLevel() {
        return m_outlineLevel;
    }

    short getNumberingLevel() {
        return m_numberingLevel;
    }

    short getNumberingType() {
        return (Short)m_numberingRule.get("NumberingType");
    }

    void loadParagraphProperties() {
        m_outlineLevel = (Short) getProperty("OutlineLevel");
        if (m_outlineLevel == 0) {
            Short numberingLevel = (Short) getProperty("NumberingLevel");
            if (numberingLevel != null) {
                XIndexReplace xNumberingRules = (XIndexReplace) UnoRuntime.queryInterface(XIndexReplace.class, getProperty("NumberingRules"));
                if (xNumberingRules != null) {
                    try {
                        PropertyValue[] properties = (PropertyValue[]) xNumberingRules.getByIndex(numberingLevel);
                        for (PropertyValue p: properties) {
                            m_numberingRule.put(p.Name, p.Value);
                        }
                        m_numberingLevel = numberingLevel;
                    } catch (com.sun.star.uno.Exception ex) {
                    }
                }
            }
        }

        // text-align
        Short paraAdjust = (Short) getProperty("ParaAdjust");
        if (paraAdjust != null) {
            if (paraAdjust == 0) {
                setStyleProperty("text-align", "left");
            } else if (paraAdjust == 1) {
                setStyleProperty("text-align", "right");
            } else if (paraAdjust == 3) {
                setStyleProperty("text-align", "center");
            } else if (paraAdjust == 2 || paraAdjust.shortValue() == 4) {
                setStyleProperty("text-align", "justify");
            }
        }

        // margin
        if (m_numberingRule.containsKey("IndentAt")) {
            setStyleProperty("margin-left", formatSize((Integer)m_numberingRule.get("IndentAt")));
        } else {
            addSizeProperty("margin-left", "ParaLeftMargin");
        }
        addSizeProperty("margin-right", "ParaRightMargin");
        addSizeProperty("margin-top", "ParaTopMargin");
        addSizeProperty("margin-bottom", "ParaBottomMargin");

        // border
        addBorderLineProperty("border-left", "LeftBorder");
        addBorderLineProperty("border-right", "RightBorder");
        addBorderLineProperty("border-top", "TopBorder");
        addBorderLineProperty("border-bottom", "BottomBorder");

        // padding
        addSizeProperty("padding-left", "LeftBorderDistance");
        addSizeProperty("padding-right", "RightBorderDistance");
        addSizeProperty("padding-top", "TopBorderDistance");
        addSizeProperty("padding-bottom", "BottomBorderDistance");

        // text-indent
        if (m_numberingRule.containsKey("FirstLineIndent")) {
            setStyleProperty("text-indent", "0");
        } else {
            addSizeProperty("text-indent", "ParaFirstLineIndent");
        }

        // list-style-type
        if (m_numberingRule.containsKey("NumberingType")) {
            short numberingType = (Short)m_numberingRule.get("NumberingType");
            if (numberingType == NumberingType.CHARS_UPPER_LETTER) {
                setStyleProperty("list-style-type", "upper-alpha");
            } else if (numberingType == NumberingType.CHARS_LOWER_LETTER) {
                setStyleProperty("list-style-type", "lower-alpha");
            } else if (numberingType == NumberingType.ROMAN_UPPER) {
                setStyleProperty("list-style-type", "upper-roman");
            } else if (numberingType == NumberingType.ROMAN_LOWER) {
                setStyleProperty("list-style-type", "lower-roman");
            } else if (numberingType == NumberingType.ARABIC) {
                // setStyleProperty("list-style-type", "decimal");
            } else if (numberingType == NumberingType.NUMBER_NONE) {
                setStyleProperty("list-style-type", "none");
            } else if (numberingType == NumberingType.CHAR_SPECIAL) {
                String bulletChar = (String)m_numberingRule.get("BulletChar");
                if (bulletChar.equals("\u2022")) {
                    setStyleProperty("list-style-type", "disc");
                } else if (bulletChar.equals("\u25E6")) {
                    setStyleProperty("list-style-type", "circle");
                } else if (bulletChar.equals("\u25AA")) {
                    setStyleProperty("list-style-type", "square");
                } else if (bulletChar.equals("\u0020")) {
                    setStyleProperty("list-style-type", "none");
                }
            }
        }

        // background & background-color
        Boolean paraBackTransparent = (Boolean) getProperty("ParaBackTransparent");
        if (paraBackTransparent != null) {
            if (paraBackTransparent) {
                setStyleProperty("background", "transparent");
            } else {
                addColorProperty("background-color", "ParaBackColor");
            }
        }

        loadCharacterProperties(true);
    }

    void loadCharacterProperties() {
        loadCharacterProperties(false);
    }

    void loadCharacterProperties(boolean skipCharBackgroundColor) {
        // font-family
        String charFontName = (String) getProperty("CharFontName");
        if (charFontName != null) {
            charFontName = "\"" + charFontName + "\"";
            setStyleProperty("font-family", m_doc.getFontName(charFontName));
        }

        // font-size
        Float charHeight = (Float) getProperty("CharHeight");
        if (charHeight != null) {
            setStyleProperty("font-size", String.format("%spt", charHeight * m_doc.getScaling() / 100.0));
        }

        // font-weight
        Float charWeight = (Float) getProperty("CharWeight");
        if (charWeight != null) {
            setStyleProperty("font-weight", getCssCharWeight(charWeight));
        }

        // font-style
        FontSlant charPosture = (FontSlant) getProperty("CharPosture");
        if (charPosture != null) {
            if (charPosture == FontSlant.ITALIC) {
                setStyleProperty("font-style", "italic");
            } else if (charPosture == FontSlant.OBLIQUE) {
                setStyleProperty("font-style", "oblique");
            } else {
                setStyleProperty("font-style", "normal");
            }
        }

        // text-transform
        Short caseMap = (Short) getProperty("CharCaseMap");
        if (caseMap != null) {
            if (caseMap == 1) {
                setStyleProperty("text-transform", "uppercase");
            } else if (caseMap == 2) {
                setStyleProperty("text-transform", "lowercase");
            } else if (caseMap == 3) {
                setStyleProperty("text-transform", "capitalize");
            } else if (caseMap == 4) {
                setStyleProperty("font-variant", "small-caps");
            }
        }

        // background & background-color
        if (!skipCharBackgroundColor) {
            Boolean paraBackTransparent = (Boolean) getProperty("CharBackTransparent");
            if (paraBackTransparent != null) {
                if (paraBackTransparent) {
                    setStyleProperty("background", "transparent");
                } else {
                    addColorProperty("background-color", "CharBackColor");
                }
            }
        }

        // color
        Integer oColor = (Integer) getProperty("CharColor");
        if (oColor != null) {
            if (oColor != -1) {
                Color color = new Color(oColor);
                setStyleProperty("color", String.format("#%02x%02x%02x",
                        color.getRed(), color.getGreen(), color.getBlue()));
            }
        }

        // vertical-align
        Short charEscapement = (Short) getProperty("CharEscapement");
        if (charEscapement != null) {
            if (charEscapement > 0) {
                setStyleProperty("vertical-align", "super");
            }  else if (charEscapement < 0) {
                setStyleProperty("vertical-align", "sub");
            }
        }

        // text decoration
        Short charUnderline = (Short) getProperty("CharUnderline");
        Short charStrikeout = (Short) getProperty("CharStrikeout");
        Short charOverline = (Short) getProperty("CharOverline");
        if (charUnderline != null && charUnderline != FontUnderline.NONE) {
            setStyleProperty("text-decoration", "underline");

            if (charUnderline == FontUnderline.DOUBLE) {
                setStyleProperty("text-decoration-style", "double");
                setStyleProperty("-moz-text-decoration-style", "double");
                setStyleProperty("-webkit-text-decoration-style", "double");
                setStyleProperty("-o-text-decoration-style", "double");
                setStyleProperty("-ms-text-decoration-style", "double");
            } else if (charUnderline == FontUnderline.DOTTED) {
                setStyleProperty("text-decoration-style", "dotted");
                setStyleProperty("-moz-text-decoration-style", "dotted");
                setStyleProperty("-webkit-text-decoration-style", "dotted");
                setStyleProperty("-o-text-decoration-style", "dotted");
                setStyleProperty("-ms-text-decoration-style", "dotted");
            } else if (charUnderline == FontUnderline.DASH) {
                setStyleProperty("text-decoration-style", "dashed");
                setStyleProperty("-moz-text-decoration-style", "dashed");
                setStyleProperty("-webkit-text-decoration-style", "dashed");
                setStyleProperty("-o-text-decoration-style", "dashed");
                setStyleProperty("-ms-text-decoration-style", "dashed");
            } else if (charUnderline == FontUnderline.WAVE) {
                setStyleProperty("text-decoration-style", "wavy");
                setStyleProperty("-moz-text-decoration-style", "wavy");
                setStyleProperty("-webkit-text-decoration-style", "wavy");
                setStyleProperty("-o-text-decoration-style", "wavy");
                setStyleProperty("-ms-text-decoration-style", "wavy");
            }

            Boolean hasColor = (Boolean) getProperty("CharUnderlineHasColor");
            if (hasColor != null && hasColor) {
                Integer oLineColor = (Integer) getProperty("CharUnderlineColor");
                if (oLineColor != null && oLineColor != -1) {
                    Color lineColor = new Color(oLineColor);
                    String strLineColor = String.format("#%02x%02x%02x", lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue());
                    setStyleProperty("text-decoration-color", strLineColor);
                    setStyleProperty("-moz-text-decoration-color", strLineColor);
                    setStyleProperty("-webkit-text-decoration-color", strLineColor);
                    setStyleProperty("-o-text-decoration-color", strLineColor);
                    setStyleProperty("-ms-text-decoration-color", strLineColor);
                }
            }
        } else if (charStrikeout != null && charStrikeout != FontStrikeout.NONE) {
            setStyleProperty("text-decoration", "line-through");

            if (charStrikeout == FontStrikeout.DOUBLE) {
                setStyleProperty("text-decoration-style", "double");
                setStyleProperty("-moz-text-decoration-style", "double");
                setStyleProperty("-webkit-text-decoration-style", "double");
                setStyleProperty("-o-text-decoration-style", "double");
                setStyleProperty("-ms-text-decoration-style", "double");
            }
        } else if (charOverline != null && charOverline != FontUnderline.NONE) {
            setStyleProperty("text-decoration", "overline");

            if (charOverline == FontUnderline.DOUBLE) {
                setStyleProperty("text-decoration-style", "double");
                setStyleProperty("-moz-text-decoration-style", "double");
                setStyleProperty("-webkit-text-decoration-style", "double");
                setStyleProperty("-o-text-decoration-style", "double");
                setStyleProperty("-ms-text-decoration-style", "double");
            } else if (charOverline == FontUnderline.DOTTED) {
                setStyleProperty("text-decoration-style", "dotted");
                setStyleProperty("-moz-text-decoration-style", "dotted");
                setStyleProperty("-webkit-text-decoration-style", "dotted");
                setStyleProperty("-o-text-decoration-style", "dotted");
                setStyleProperty("-ms-text-decoration-style", "dotted");
            } else if (charOverline == FontUnderline.DASH) {
                setStyleProperty("text-decoration-style", "dashed");
                setStyleProperty("-moz-text-decoration-style", "dashed");
                setStyleProperty("-webkit-text-decoration-style", "dashed");
                setStyleProperty("-o-text-decoration-style", "dashed");
                setStyleProperty("-ms-text-decoration-style", "dashed");
            } else if (charOverline == FontUnderline.WAVE) {
                setStyleProperty("text-decoration-style", "wavy");
                setStyleProperty("-moz-text-decoration-style", "wavy");
                setStyleProperty("-webkit-text-decoration-style", "wavy");
                setStyleProperty("-o-text-decoration-style", "wavy");
                setStyleProperty("-ms-text-decoration-style", "wavy");
            }

            Boolean hasColor = (Boolean) getProperty("CharOverlineHasColor");
            if (hasColor != null && hasColor) {
                Integer oLineColor = (Integer) getProperty("CharOverlineColor");
                if (oLineColor != null && oLineColor != -1) {
                    Color lineColor = new Color(oLineColor);
                    String strLineColor = String.format("#%02x%02x%02x", lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue());
                    setStyleProperty("text-decoration-color", strLineColor);
                    setStyleProperty("-moz-text-decoration-color", strLineColor);
                    setStyleProperty("-webkit-text-decoration-color", strLineColor);
                    setStyleProperty("-o-text-decoration-color", strLineColor);
                    setStyleProperty("-ms-text-decoration-color", strLineColor);
                }
            }
        }
    }

    void loadTableProperties() {
        // border-collapse
        Boolean collapsingBorders = (Boolean) getProperty("CollapsingBorders");
        if (collapsingBorders != null) {
            if (collapsingBorders) {
                setStyleProperty("border-collapse", "collapse");
            } else {
                setStyleProperty("border-collapse", "separate");
            }
        }

        // background & background-color
        Boolean backTransparent = (Boolean) getProperty("BackTransparent");
        if (backTransparent != null) {
            if (backTransparent) {
                setStyleProperty("background", "transparent");
            } else {
                addColorProperty("background-color", "BackColor");
            }
        }
    }

    void loadTableRowProperties() {
        // height
        Boolean IsAutoHeight = (Boolean)getProperty("IsAutoHeight");
        if (IsAutoHeight == null || !IsAutoHeight) {
            addSizeProperty("height", "Height");
        }

        // background & background-color
        Boolean backTransparent = (Boolean) getProperty("BackTransparent");
        if (backTransparent != null) {
            if (backTransparent) {
                setStyleProperty("background", "transparent");
            } else {
                addColorProperty("background-color", "BackColor");
            }
        }
    }

    void loadTableCellProperties() {
        // border
        addBorderLineProperty("border-left", "LeftBorder");
        addBorderLineProperty("border-right", "RightBorder");
        addBorderLineProperty("border-top", "TopBorder");
        addBorderLineProperty("border-bottom", "BottomBorder");

        // vertical-align
        Short vertOrient = (Short) getProperty("VertOrient");
        if (vertOrient != null) {
            if (vertOrient == 0) {
                setStyleProperty("vertical-align", "top");
            } else if (vertOrient == 2) {
                setStyleProperty("vertical-align", "middle");
            } else if (vertOrient == 3) {
                setStyleProperty("vertical-align", "bottom");
            }
        }

        // background & background-color
        Boolean backTransparent = (Boolean) getProperty("BackTransparent");
        if (backTransparent != null) {
            if (backTransparent) {
                setStyleProperty("background", "transparent");
            } else {
                addColorProperty("background-color", "BackColor");
            }
        }
    }

    void loadImageDivProperties() {
        WrapTextMode wrapTextMode = (WrapTextMode) getProperty("TextWrap");
        if (wrapTextMode == WrapTextMode.RIGHT || wrapTextMode == WrapTextMode.LEFT) {
            setStyleProperty("float", wrapTextMode == WrapTextMode.RIGHT ? "left" : "right");

            // margin
            addSizeProperty("margin-left", "LeftMargin");
            addSizeProperty("margin-right", "RightMargin");
            addSizeProperty("margin-top", "TopMargin");
            addSizeProperty("margin-bottom", "BottomMargin");
        } else {
            short horiOrient = (Short) getProperty("HoriOrient");
            if (horiOrient == 2) {
                // center
                setStyleProperty("text-align", "center");
            } else if (horiOrient == 0) {
                setStyleProperty("position", "relative");
                addSizeProperty("left", "HoriOrientPosition");
                addSizeProperty("top", "VertOrientPosition");
            }
        }

        // padding
        addSizeProperty("padding-left", "LeftBorderDistance");
        addSizeProperty("padding-right", "RightBorderDistance");
        addSizeProperty("padding-top", "TopBorderDistance");
        addSizeProperty("padding-bottom", "BottomBorderDistance");
    }

    void loadImageProperties() {
        // border
        addBorderLineProperty("border-left", "LeftBorder");
        addBorderLineProperty("border-right", "RightBorder");
        addBorderLineProperty("border-top", "TopBorder");
        addBorderLineProperty("border-bottom", "BottomBorder");

        addSizeProperty("width", "Width");
        addSizeProperty("height", "Height");
    }

    String getStyleProperty(String name) {
        String value = m_styleProperties.get(name);
        if (value == null && m_parentCharacterStyle != null) {
            value = m_parentCharacterStyle.getStyleProperty(name);
        }
        if (value == null && m_parentParagraphStyle != null) {
            value = m_parentParagraphStyle.getStyleProperty(name);
        }
        return value;
    }

    void setStyleProperty(String name, String value) {
        if (m_styleProperties.containsKey(name) ||
                (m_parentCharacterStyle == null || !value.equals(m_parentCharacterStyle.getStyleProperty(name))) &&
                (m_parentParagraphStyle == null || !value.equals(m_parentParagraphStyle.getStyleProperty(name)))) {
            m_styleProperties.put(name, value);
        }
    }

    void addSizeProperty(String cssName, int value) {
        setStyleProperty(cssName, formatSize(value));
    }

    void addSizeProperty(String cssName, String ooName) {
        Integer value = (Integer) getProperty(ooName);
        if (value != null) {
            addSizeProperty(cssName, value);
        }
    }

    void addBorderLineProperty(String cssName, String ooName) {
        String value = getBorderLineProperty(ooName);
        if (value != null) {
            setStyleProperty(cssName, value);
        }
    }

    void addColorProperty(String cssName, String ooName) {
        Integer oColor = (Integer) getProperty(ooName);
        if (oColor != null) {
            Color color = new Color(oColor);
            setStyleProperty(cssName, String.format("#%02x%02x%02x",
                    color.getRed(), color.getGreen(), color.getBlue()));
        }
    }

    void removeProperty(String name) {
        m_styleProperties.remove(name);
    }

    String getCssCharWeight(Float charWeight) {
        if (charWeight.floatValue() == 150) {
            return "bold";
        } else {
            return "normal";
        }
    }

    String formatSize(int size) {
        if (size == 0) {
            return "0";
        }

        // size is in 1/100 mm
        double dSize = size / 100.0;

        String unit;
        if (m_doc.getConvertToPixels()) {
            // 1 inch = 25.4 mm = 96 pixels
            dSize = dSize * 96.0 / 25.4;
            unit = "px";
        } else {
            unit = "mm";
        }

        dSize = dSize * m_doc.getScaling() / 100.0;

        DecimalFormat df = new DecimalFormat("#.#####");
        return String.format("%s%s", df.format(dSize), unit);
    }

    String getBorderLineProperty(String name) {
        BorderLine oValue = (BorderLine) getProperty(name);
        if (oValue == null)
            return null;
        return getBorderStyle(oValue);
    }

    static String getBorderStyle(BorderLine border) {
        if (border.OuterLineWidth == 0) {
            return "0";
        }

        int width = (int)Math.max(1, Math.round(Math.max(
                border.OuterLineWidth, border.InnerLineWidth) / 35.0));
        String style = (border.OuterLineWidth > 0 && border.InnerLineWidth > 0)
                ? "double" : "solid";
        Color color = new Color(border.Color);

        return String.format("%dpx %s #%02x%02x%02x",
                width, style, color.getRed(), color.getGreen(), color.getBlue());
    }
}
