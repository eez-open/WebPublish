/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.DOM.HTMLBuilder;
import com.envox.webpublish.DOM.TableParser;
import com.sun.star.awt.FontSlant;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XIndexReplace;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.document.XStorageBasedDocument;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XStream;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.style.NumberingType;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.table.BorderLine;
import com.sun.star.table.XCell;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.WrapTextMode;
import com.sun.star.text.XEndnotesSupplier;
import com.sun.star.text.XFootnotesSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextGraphicObjectsSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextTable;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;

/**
 *
 * @author martin
 */
public class HtmlGenerator {
    private XComponentContext m_xContext;

    private HTMLBuilder m_builder;

    private XTextDocument m_xTextDocument;

    // collection of all page styles
    private XNameContainer m_pageStyles;

    // collection of all paragraph styles
    private XNameContainer m_paraStyles;

    // collection of all character styles
    private XNameContainer m_charStyles;

    // collection of names of all used paragraph styles
    private Set<String> m_paraStyleNames = new HashSet<String>();
    private Set<String> m_liStyleNames = new HashSet<String>();

    // current page style properties
    private XPropertySet m_xPageStyle;
    private int m_pageWidth;
    private int m_leftMargin;
    private int m_rightMargin;

    // current paragraph style properties
    private XPropertySet m_xParaStyle;

    // current paragraph properties
    private XPropertySet m_xParaPropertySet;

    // current character style properties
    private XPropertySet m_xCharStyle;

    // current text portion properties
    private XPropertySet m_xTextPropertySet;

    // default paragraph style name
    private String m_defaultParagraphStyleName = "Standard";

    // default paragraph style properties
    private XPropertySet m_xDefaultParagraphStyle;

    private List<XTextContent> m_xFrames;

    private XSimpleFileAccess2 m_xFileWriter;
    private XNameAccess m_xPicturesNameAccess;

    private String m_imagesBaseURL;
    private Map<String, String> m_images = new TreeMap<String, String>();

    private boolean m_skipCharBackgroundColor;

    ParagraphsToTable m_paragraphsToTable;

    public HtmlGenerator(XTextDocument xTextDocument, XComponentContext xContext) throws NoSuchElementException, WrappedTargetException, Exception
    {
        m_xTextDocument = xTextDocument;
        m_xContext = xContext;
        m_bStyleAdjusted = false;
        m_skipCharBackgroundColor = false;

        XStyleFamiliesSupplier styleFamiliesSupplier = (XStyleFamiliesSupplier) UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, m_xTextDocument);
        XNameAccess styleFamilies = styleFamiliesSupplier.getStyleFamilies();
        m_pageStyles = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, styleFamilies.getByName("PageStyles"));
        m_paraStyles = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, styleFamilies.getByName("ParagraphStyles"));
        m_charStyles = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, styleFamilies.getByName("CharacterStyles"));

        m_xDefaultParagraphStyle = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_paraStyles.getByName(m_defaultParagraphStyleName));

        {
            m_xFileWriter = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class,
                    m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.ucb.SimpleFileAccess", m_xContext));

            XStorageBasedDocument xStorageBasedDocument = (XStorageBasedDocument) UnoRuntime.queryInterface(
                XStorageBasedDocument.class, m_xTextDocument);

            XNameAccess xDocStorageNameAccess = (XNameAccess) UnoRuntime.queryInterface(
                XNameAccess.class, xStorageBasedDocument.getDocumentStorage());

            if (xDocStorageNameAccess.hasByName("Pictures")) {
                m_xPicturesNameAccess = (XNameAccess) UnoRuntime.queryInterface(
                   XNameAccess.class, xDocStorageNameAccess.getByName("Pictures"));
            } else {
                m_xPicturesNameAccess = null;
            }
        }

        m_imagesBaseURL = Util.getStringConf("BlogAccountPage", "txtImagesBaseURL", "");

        initFootnoteHandling();

        loadDocumentTitleConf();
        loadPageBreakConf();
    }

    public String getBody(String indent) throws NoSuchElementException, WrappedTargetException, Exception {
        m_xFrames = new ArrayList<XTextContent>();
        findTextFrames(m_xTextDocument);
        findGraphicObjects(m_xTextDocument);

        initPageBreak();

        m_builder = new HTMLBuilder(indent);
        genText(m_xTextDocument.getText(), true);

        return m_builder.toString();
    }

    public String getCss(String indent) {
        m_builder = new HTMLBuilder(indent);

        for (String paraStyleName : m_paraStyleNames) {
            try {
                XPropertySet xParaStyle = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_paraStyles.getByName(paraStyleName));
                if (paraStyleName.equals(m_defaultParagraphStyleName)) {
                    m_builder.cssOpen("p");
                    genParaStyle(xParaStyle, null, false);
                    m_builder.cssClose();
                } else {
                    m_builder.cssOpen("." + mapParaStyleToClass(paraStyleName));
                    genParaStyle(xParaStyle, new XPropertySet[] { m_xDefaultParagraphStyle }, false);
                    m_builder.cssClose();
                }
            } catch (com.sun.star.uno.Exception ex) {
            }
        }

        for (String liStyleName : m_liStyleNames) {
            try {
                XPropertySet xParaStyle = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_paraStyles.getByName(liStyleName));
                m_builder.cssOpen("." + mapParaStyleToClass(liStyleName));
                genParaStyle(xParaStyle, new XPropertySet[] { m_xDefaultParagraphStyle }, true);
                m_builder.cssClose();
            } catch (com.sun.star.uno.Exception ex) {
            }
        }

        genNotesCss();

        return m_builder.toString();
    }

    private void genText(XText xText, boolean main) throws NoSuchElementException, WrappedTargetException {
        ListHandler listHandler = new ListHandler();

        ParagraphsToTable savedParagraphToTable = m_paragraphsToTable;
        m_paragraphsToTable = new ParagraphsToTable();

        XEnumerationAccess xParaAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xText);
        XEnumeration xParaEnum = xParaAccess.createEnumeration();
        while (xParaEnum.hasMoreElements()) {
            XServiceInfo xParaInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xParaEnum.nextElement());

            processFrames(xText, xParaInfo);

            if (!xParaInfo.supportsService("com.sun.star.text.TextTable")) {
                if (isDocumentTitleParagraph(xParaInfo)) {
                    continue;
                }

                m_xParaPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaInfo);

                Object oPageStyleName = getProperty("PageStyleName", m_xParaPropertySet, null);
                if (oPageStyleName != null) {
                    m_xPageStyle = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_pageStyles.getByName((String)oPageStyleName));
                    m_pageWidth = (Integer)getProperty("Width", m_xPageStyle, null);
                    m_leftMargin = (Integer)getProperty("LeftMargin", m_xPageStyle, null);
                    m_rightMargin = (Integer)getProperty("RightMargin", m_xPageStyle, null);
                }

                String paraStyleName = (String) getProperty("ParaStyleName", m_xParaPropertySet, null);
                m_xParaStyle = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_paraStyles.getByName(paraStyleName));

                String classAttr = !paraStyleName.equals(m_defaultParagraphStyleName)
                        ? mapCharStyleToClass(paraStyleName) : "";
                String styleAttr  = getParaStyle(m_xParaPropertySet, new XPropertySet[] {
                    m_xParaStyle, m_xDefaultParagraphStyle }, listHandler.isListItem());

                listHandler.visitParagraph(xParaInfo, m_builder);

                if (!listHandler.isListItem()) {
                    m_paragraphsToTable.visitParagraph(xParaInfo, classAttr, styleAttr, m_builder);
                }

                if (listHandler.isListItem()) {
                    if (!m_paraStyleNames.contains(paraStyleName))
                        m_liStyleNames.add(paraStyleName);
                } else {
                    if (!m_liStyleNames.contains(paraStyleName))
                        m_paraStyleNames.add(paraStyleName);
                }

                m_builder.beginTagNewLine(listHandler.isListItem() ? "li" : "p");

                if (!classAttr.isEmpty()) {
                    m_builder.attr("class", classAttr);
                }

                if (!styleAttr.isEmpty()) {
                    m_builder.attr("style", styleAttr);
                }

                genParaContent(xParaInfo);

                m_builder.endTag(listHandler.isListItem() ? "li" : "p");
            } else {
                try {
                    XTextTable xTextTable = (XTextTable) UnoRuntime.queryInterface(XTextTable.class, xParaInfo);

                    TableParser tableParser = new TableParser(xTextTable);

                    m_builder.beginTagNewLine("table");

                    m_builder.attr("width", formatSize(tableParser.getTableWidth()));
                    //m_builder.attr("style", tableParser.getTableStyle());

                    for (int i = 0; i < tableParser.getRows(); ++i) {
                        m_builder.beginTagNewLine("tr");
                        for (int j = 0; j < tableParser.getCols(); ++j) {
                            String name = tableParser.getCell(i, j);
                            if (name != null) {
                                m_builder.beginTagNewLine("td");

                                int colspan = tableParser.getColSpan(i, j);
                                if (colspan != 1) {
                                    m_builder.attr("colspan", Integer.toString(colspan));
                                }

                                int rowspan = tableParser.getRowSpan(i, j);
                                if (rowspan != 1) {
                                    m_builder.attr("rowspan", Integer.toString(rowspan));
                                }

                                m_builder.attr("width", String.format("%d%%", tableParser.getCellWidthAsPercent(i, j)));

                                /*
                                String cellStyle = tableParser.getCellStyle(i, j);
                                if (!cellStyle.isEmpty()) {
                                    m_builder.attr("style", cellStyle);
                                }
                                */

                                XCell xCell = xTextTable.getCellByName(name);
                                XText xCellText = (XText) UnoRuntime.queryInterface(XText.class, xCell);
                                genText(xCellText, false);

                                m_builder.endTag("td");
                            }
                        }
                        m_builder.endTag("tr");
                    }
                    m_builder.endTag("table");
                } catch (UnknownPropertyException ex) {
                    Logger.getLogger(HtmlGenerator.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IndexOutOfBoundsException ex) {
                    Logger.getLogger(HtmlGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (main) {
                doPageBreak();
            }
        }

        listHandler.flush(m_builder);
        
        m_paragraphsToTable.flush(m_builder);
        m_paragraphsToTable = savedParagraphToTable;

        if (main) {
            genFootnotes();
            genEndnotes();
        }
    }

    private void genParaContent(XServiceInfo xParaInfo) throws NoSuchElementException, WrappedTargetException {
        XEnumerationAccess xTextAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xParaInfo);
        XEnumeration xTextEnum = xTextAccess.createEnumeration();
        while (xTextEnum.hasMoreElements()) {
            XServiceInfo xTextInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xTextEnum.nextElement());
            genTextContent(xTextInfo);
        }
    }

    private void genTextContent(XServiceInfo xTextInfo) throws NoSuchElementException, WrappedTargetException {
        m_xTextPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextInfo);

        String textPortionType = (String) getProperty("TextPortionType", m_xTextPropertySet, null);
        if (textPortionType.equals("Text")) {
            genTextPortion(xTextInfo);
        } else if (textPortionType.equals("Footnote")) {
            genFootnoteTextPortion(xTextInfo);
        } else if (textPortionType.equals("SoftPageBreak")) {
            incPageBreakCounter();
        }
    }

    private void genTextPortion(XServiceInfo xTextInfo) throws NoSuchElementException, WrappedTargetException {
        XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, xTextInfo);

        String text = xTextRange.getString();

        String charStyleName = (String) getProperty("CharStyleName", m_xTextPropertySet, null);
        m_xCharStyle = charStyleName.isEmpty() ? null :
            (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_charStyles.getByName(charStyleName));

        String charStyle = getCharStyle(m_xTextPropertySet, new XPropertySet[] { m_xCharStyle, m_xParaStyle } );

        String hyperLinkURL = (String) getProperty("HyperLinkURL", m_xTextPropertySet, null);
        String hyperLinkName = (String) getProperty("HyperLinkName", m_xTextPropertySet, null);
        if (!hyperLinkURL.isEmpty() || !hyperLinkName.isEmpty()) {
            m_builder.beginTag("a");

            if (!hyperLinkURL.isEmpty()) {
                m_builder.attr("href", hyperLinkURL);

                String hyperLinkTarget = (String) getProperty("HyperLinkTarget", m_xTextPropertySet, null);
                if (!hyperLinkTarget.isEmpty()) {
                    m_builder.attr("target", hyperLinkTarget);
                }
            } 

            if (!hyperLinkName.isEmpty()) {
                m_builder.attr("name", hyperLinkName);
            }

            genTextPortionText(text, charStyleName, charStyle);

            m_builder.endTag("a");
        } else {
            if (m_paragraphsToTable != null && m_paragraphsToTable.isInTable() && text.contains("\t")) {
                String[] columns = StringUtils.split(text, "\t");
                if (columns.length > 1) {
                    genTextPortionText(columns[0], charStyleName, charStyle);
                    for (int i = 1; i < columns.length; ++i) {
                        m_paragraphsToTable.breakParagraph(m_builder);
                        genTextPortionText(columns[i], charStyleName, charStyle);
                    }
                } else {
                    genTextPortionText(columns[0], charStyleName, charStyle);
                    m_paragraphsToTable.breakParagraph(m_builder);
                }
            } else {
                genTextPortionText(text, charStyleName, charStyle);
            }
        }
    }

    public void genTextPortionText(String text, String charStyleName, String charStyle) {
        if (text.contains("\n")) {
            text = text.replace("\r\n", "<br/>");
            text = text.replace("\n", "<br/>");
        }

        if (!charStyleName.isEmpty() || !charStyle.isEmpty()) {
            m_builder.beginTag("span");

            if (!charStyleName.isEmpty()) {
                m_builder.attr("class", mapCharStyleToClass(charStyleName));
            }

            if (!charStyle.isEmpty()) {
                m_builder.attr("style", charStyle);
            }

            m_builder.append(text);

            m_builder.endTag("span");
        } else {
            m_builder.append(text);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Style handling">

    private boolean m_bStyleAdjusted;

    public boolean getStyleAdjusted() { 
        return m_bStyleAdjusted;
    }

    private String mapParaStyleToClass(String styleName) {
        if (styleName.contains(" ")) {
            styleName = styleName.replace(' ', '_');
            m_bStyleAdjusted = true;
        }
        return styleName;
    }

    private String mapCharStyleToClass(String styleName) {
        if (styleName.contains(" ")) {
            styleName = styleName.replace(' ', '_');
            m_bStyleAdjusted = true;
        }
        return styleName;
    }

    private String getCssCharWeight(Float charWeight) {
        if (charWeight.floatValue() == 150) {
            return "bold";
        } else {
            return "normal";
        }
    }

    private String getParaStyle(XPropertySet set, XPropertySet[] parentSets, boolean liStyle) {
        HTMLBuilder builder = new HTMLBuilder("");
        HTMLBuilder saved = m_builder;
        m_builder = builder;
        genParaStyle(set, parentSets, liStyle);
        m_builder = saved;
        return builder.toString();
    }

    private void genParaStyle(XPropertySet set, XPropertySet[] parentSets, boolean liStyle) {
        // paragraph adjust
        Short paraAdjust = (Short) getProperty("ParaAdjust", set, parentSets);
        if (paraAdjust != null) {
            if (paraAdjust.shortValue() == 0) {
                m_builder.cssAttr("text-align", "left");
            } else if (paraAdjust.shortValue() == 1) {
                m_builder.cssAttr("text-align", "right");
            } else if (paraAdjust.shortValue() == 3) {
                m_builder.cssAttr("text-align", "center");
            } else if (paraAdjust.shortValue() == 2 || paraAdjust.shortValue() == 4) {
                m_builder.cssAttr("text-align", "justify");
            }
        }

        if (!liStyle) {
            // left margin
            Integer leftMargin = (Integer) getProperty("ParaLeftMargin", set, parentSets);
            if (leftMargin != null) {
                m_builder.cssAttr("margin-left", formatSize(leftMargin));
            }

            // right margin
            Integer rightMargin = (Integer) getProperty("ParaRightMargin", set, parentSets);
            if (rightMargin != null) {
                m_builder.cssAttr("margin-right", formatSize(rightMargin));
            }

            // top margin
            Integer topMargin = (Integer) getProperty("ParaTopMargin", set, parentSets);
            if (topMargin != null) {
                m_builder.cssAttr("margin-top", formatSize(topMargin));
            }

            // bottom margin
            Integer bottomMargin = (Integer) getProperty("ParaBottomMargin", set, parentSets);
            if (bottomMargin != null) {
                m_builder.cssAttr("margin-bottom", formatSize(bottomMargin));
            }

            // left border
            String leftBorder = getBorderLineProperty("LeftBorder", set, parentSets);
            if (leftBorder != null) {
                m_builder.cssAttr("border-left", leftBorder);
            }

            // right border
            String rightBorder = getBorderLineProperty("RightBorder", set, parentSets);
            if (rightBorder != null) {
                m_builder.cssAttr("border-right", rightBorder);
            }

            // top border
            String topBorder = getBorderLineProperty("TopBorder", set, parentSets);
            if (topBorder != null) {
                m_builder.cssAttr("border-top", topBorder);
            }

            // bottom border
            String bottomBorder = getBorderLineProperty("BottomBorder", set, parentSets);
            if (bottomBorder != null) {
                m_builder.cssAttr("border-bottom", bottomBorder);
            }

            // left padding
            Integer leftPadding = (Integer) getProperty("LeftBorderDistance", set, parentSets);
            if (leftPadding != null) {
                m_builder.cssAttr("padding-left", formatSize(leftPadding));
            }

            // right padding
            Integer rightPadding = (Integer) getProperty("RightBorderDistance", set, parentSets);
            if (rightPadding != null) {
                m_builder.cssAttr("padding-right", formatSize(rightPadding));
            }

            // top padding
            Integer topPadding = (Integer) getProperty("TopBorderDistance", set, parentSets);
            if (topPadding != null) {
                m_builder.cssAttr("padding-top", formatSize(topPadding));
            }

            // bottom padding
            Integer bottomPadding = (Integer) getProperty("BottomBorderDistance", set, parentSets);
            if (bottomPadding != null) {
                m_builder.cssAttr("padding-bottom", formatSize(bottomPadding));
            }

            // first line indent
            Integer firstLineIndent = (Integer) getProperty("ParaFirstLineIndent", set, parentSets);
            if (firstLineIndent != null) {
                m_builder.cssAttr("text-indent", formatSize(firstLineIndent));
            }
        }

        // background color
        Object oParaBackTransparent = getProperty("ParaBackTransparent", set, parentSets);
        if (oParaBackTransparent != null) {
            if ((Boolean)oParaBackTransparent) {
                m_builder.cssAttr("background", "transparent");
            } else {
                Integer oParaBackColor = (Integer) getProperty("ParaBackColor", set, parentSets);
                if (oParaBackColor != null) {
                    Color paraBackColor = new Color(oParaBackColor.intValue());
                    m_builder.cssAttr("background-color", String.format("#%02x%02x%02x", paraBackColor.getRed(), paraBackColor.getGreen(), paraBackColor.getBlue()));
                }
            }
        }

        m_skipCharBackgroundColor = true;
        genCharStyle(set, parentSets);
        m_skipCharBackgroundColor = false;
    }

    private String getCharStyle(XPropertySet set, XPropertySet[] parentSets) {
        HTMLBuilder builder = new HTMLBuilder("");
        HTMLBuilder saved = m_builder;
        m_builder = builder;
        genCharStyle(set, parentSets);
        m_builder = saved;
        return builder.toString();
    }

    private void genCharStyle(XPropertySet set, XPropertySet[] parentSets) {
        // char font name
        String charFontName = (String) getProperty("CharFontName", set, parentSets);
        if (charFontName != null) {
            m_builder.cssAttr("font-family", String.format("%s", charFontName));
        }

        // char height
        Float charHeight = (Float) getProperty("CharHeight", set, parentSets);
        if (charHeight != null) {
            m_builder.cssAttr("font-size", String.format("%spt", charHeight.toString()));
        }

        // char weight
        Float charWeight = (Float) getProperty("CharWeight", set, parentSets);
        if (charWeight != null) {
            m_builder.cssAttr("font-weight", getCssCharWeight(charWeight));
        }

        // char style
        FontSlant charPosture = (FontSlant) getProperty("CharPosture", set, parentSets);
        if (charPosture != null) {
            if (charPosture == FontSlant.ITALIC) {
                m_builder.cssAttr("font-style", "italic");
            } else if (charPosture == FontSlant.OBLIQUE) {
                m_builder.cssAttr("font-style", "oblique");
            } else {
                m_builder.cssAttr("font-style", "normal");
            }
        }

        // char background color
        if (!m_skipCharBackgroundColor) {
            Object oParaBackTransparent = getProperty("CharBackTransparent", set, parentSets);
            if (oParaBackTransparent != null) {
                if ((Boolean)oParaBackTransparent) {
                    m_builder.cssAttr("background", "transparent");
                } else {
                    Integer oParaBackColor = (Integer) getProperty("CharBackColor", set, parentSets);
                    if (oParaBackColor != null) {
                        Color paraBackColor = new Color(oParaBackColor.intValue());
                        m_builder.cssAttr("background-color", String.format("#%02x%02x%02x", paraBackColor.getRed(), paraBackColor.getGreen(), paraBackColor.getBlue()));
                    }
                }
            }
        }

        // text color
        Integer oTextColor = (Integer) getProperty("CharColor", set, parentSets);
        if (oTextColor != null) {
            Color textColor = null;
            if (oTextColor != -1) {
                textColor = new Color(oTextColor.intValue());
                m_builder.cssAttr("color", String.format("#%02x%02x%02x", textColor.getRed(), textColor.getGreen(), textColor.getBlue()));
            }
        }
    }

    private Object getProperty(String propName, XPropertySet[] sets) {
        try {
            if (sets != null) {
                for (XPropertySet set: sets) {
                    if (set != null) {
                        Object oValue = set.getPropertyValue(propName);
                        if (oValue != null) {
                            return oValue;
                        }
                    }
                }
            }
        } catch (com.sun.star.uno.Exception ex) {
        }
        return null;
    }

    private Object getProperty(String propName, XPropertySet set, XPropertySet[] parentSets) {
        try {
            Object oValue = set.getPropertyValue(propName);
            Object oParentValue = getProperty(propName, parentSets);
            if (oParentValue == null || !oValue.equals(oParentValue)) {
                return oValue;
            }
        } catch (com.sun.star.uno.Exception ex) {
        }
        return null;
    }

    private String getBorderLineProperty(String propName, XPropertySet set, XPropertySet[] parentSets) {
        try {
            BorderLine oValue = (BorderLine) set.getPropertyValue(propName);
            String value = null;
            if (oValue != null)
                value = ""; // Util.getBorderStyle(oValue);

            BorderLine oParentValue = (BorderLine) getProperty(propName, parentSets);
            String parentValue = null;
            if (oParentValue != null)
                parentValue = ""; // Util.getBorderStyle(oParentValue);
            
            if (parentValue == null || !value.equals(parentValue)) {
                return value;
            }
        } catch (com.sun.star.uno.Exception ex) {
        }
        return null;
    }

    String formatSize(int size) {
        double dSize;
        String unit;

        int width = m_pageWidth - m_leftMargin - m_rightMargin;
        if (width == 0) {
            dSize = size / 100.0;
            unit = "mm";
        } else {
            dSize = size * 100.0 / width;
            unit = "%";
        }

        if (dSize == 0) {
            return "0";
        }

        if (dSize < 1.0) {
            return String.format("%.4f%s", dSize, unit);
        } if (dSize < 10.0) {
            return String.format("%.3f%s", dSize, unit);
        } else if (dSize < 50.0) {
            return String.format("%.2f%s", dSize, unit);
        } else {
            return String.format("%d%s", (int)Math.round(dSize), unit);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Frames processing">

    void findTextFrames(XTextDocument xTextDocument) throws NoSuchElementException, WrappedTargetException {
        XTextFramesSupplier xSupplier = (XTextFramesSupplier)
                UnoRuntime.queryInterface(XTextFramesSupplier.class, xTextDocument);
        XNameAccess xObjects = xSupplier.getTextFrames();
        String[] objectNames =  xObjects.getElementNames();
        for (String name:objectNames) {
            XTextContent xTextContent = (XTextContent)
                UnoRuntime.queryInterface(XTextContent.class, xObjects.getByName(name));
            XPropertySet xPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextContent);
            TextContentAnchorType anchorType = (TextContentAnchorType) getProperty("AnchorType", xPS, null);
            if (anchorType == TextContentAnchorType.AT_PARAGRAPH) {
                m_xFrames.add(xTextContent);
            }
        }
    }

    void findGraphicObjects(XTextDocument xTextDocument) throws NoSuchElementException, WrappedTargetException {
        XTextGraphicObjectsSupplier xSupplier = (XTextGraphicObjectsSupplier)
            UnoRuntime.queryInterface(XTextGraphicObjectsSupplier.class, xTextDocument);
        XNameAccess xObjects = xSupplier.getGraphicObjects();
        String[] objectNames =  xObjects.getElementNames();
        for (String name:objectNames) {
            XTextContent xTextContent = (XTextContent)
                UnoRuntime.queryInterface(XTextContent.class, xObjects.getByName(name));
            XPropertySet xPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextContent);
            TextContentAnchorType anchorType = (TextContentAnchorType) getProperty("AnchorType", xPS, null);
            if (anchorType == TextContentAnchorType.AT_PARAGRAPH) {
                m_xFrames.add(xTextContent);
            }
        }
    }

    void processFrames(XText xText, XServiceInfo xParaInfo) throws NoSuchElementException, WrappedTargetException {
        XTextRangeCompare xTextRangeCompare = (XTextRangeCompare)
                UnoRuntime.queryInterface(XTextRangeCompare.class, xText);
        XTextRange xParaTextRange = (XTextRange)
                UnoRuntime.queryInterface(XTextRange.class, xParaInfo);

        Iterator<XTextContent> iterator = m_xFrames.iterator();
        while (iterator.hasNext()) {
            XTextContent xTextContent = iterator.next();
            XTextRange xTextRange = xTextContent.getAnchor();
            try {
                if (xTextRangeCompare.compareRegionStarts(xTextRange, xParaTextRange) == 0) {
                    XServiceInfo xInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xTextContent);
                    if (xInfo.supportsService("com.sun.star.text.TextFrame")) {
                        XTextFrame xTextFrame = (XTextFrame) UnoRuntime.queryInterface(XTextFrame.class, xInfo);
                        m_builder.beginTagNewLine("div");
                        genText(xTextFrame.getText(), false);
                        m_builder.endTag("div");
                    } else if (xInfo.supportsService("com.sun.star.text.TextGraphicObject")) {
                        XPropertySet xPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextContent);

                        String graphicURL = (String) getProperty("GraphicURL", xPS, null);

                        String name = (String) getProperty("Name", xPS, null);
                        if (name == null) // TODO zašto je ovo null, pa moram gledati i LinkDisplayName???
                            name = (String) getProperty("LinkDisplayName", xPS, null);

                        if (graphicURL.startsWith("vnd.sun.star.GraphicObject:")) {
                            graphicURL = graphicURL.substring("vnd.sun.star.GraphicObject:".length());
                            if (m_xPicturesNameAccess != null) {
                                String[] pictureNames = m_xPicturesNameAccess.getElementNames();
                                for (String pictureName: pictureNames) {
                                    if (pictureName.startsWith(graphicURL)) {
                                        name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                                        name = FilenameUtils.removeExtension(name) + "." + FilenameUtils.getExtension(pictureName);
                                        m_images.put(name, pictureName);
                                    }
                                }
                            }
                        }

                        String title = (String) getProperty("Title", xPS, null);

                        WrapTextMode wrapTextMode = (WrapTextMode) getProperty("TextWrap", xPS, null);

                        m_builder.beginTag("img");
                        m_builder.attr("src", Util.urlCombine(m_imagesBaseURL, name));
                        m_builder.attr("alt", title);
                        if (wrapTextMode == WrapTextMode.RIGHT) {
                            m_builder.attr("style", "float: left; padding: 0 1em 1em 0;");
                        } else if (wrapTextMode == WrapTextMode.LEFT) {
                            m_builder.attr("style", "float: right; padding: 0 0 1em 1em;");
                        }
                        m_builder.endTagNoTag("img");
                    }
                }
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(HtmlGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Images handling">
    void publishImages(MoveableTypeAPIClient blogClient) throws CommandErrorException, XmlRpcException {
        for (Map.Entry<String, String> entry:m_images.entrySet()) {
            String imageName = entry.getKey();
            String fileName = entry.getValue();
            if (!blogClient.isImagePublished(imageName)) {
                try {
                    XStream xStream = (XStream) UnoRuntime.queryInterface(
                        XStream.class, m_xPicturesNameAccess.getByName(fileName));

                    XInputStream xInputStream = (XInputStream) UnoRuntime.queryInterface(
                        XInputStream.class, xStream.getInputStream());

                    File temp = File.createTempFile("wpi", FilenameUtils.getExtension(fileName));
                    temp.deleteOnExit();

                    m_xFileWriter.writeFile(temp.getPath(), xInputStream);

                    blogClient.publishImage(imageName, temp.getPath());
                } catch (IOException ex) {
                    throw new CommandErrorException(CommandError.ERROR_GRAPHIC_EXTRACT);
                } catch (Exception ex) {
                    throw new CommandErrorException(CommandError.ERROR_GRAPHIC_EXTRACT);
                }
            }
        }
    }
    // </editor-fold>

    // <editor-fold  defaultstate="collapsed" desc="Document title handling">
    private String m_documentTitleStyle;
    private String m_documentTitle;

    public String getDocumentTitle() throws NoSuchElementException, WrappedTargetException {
        if (m_documentTitle == null && !m_documentTitleStyle.isEmpty()) {
            XText xText = m_xTextDocument.getText();
            XEnumerationAccess xParaAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xText);
            XEnumeration xParaEnum = xParaAccess.createEnumeration();
            while (xParaEnum.hasMoreElements()) {
                XServiceInfo xParaInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xParaEnum.nextElement());
                if (!xParaInfo.supportsService("com.sun.star.text.TextTable")) {
                    m_xParaPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaInfo);
                    String paraStyleName = (String) getProperty("ParaStyleName", m_xParaPropertySet, null);
                    if (paraStyleName.equals(m_documentTitleStyle)) {
                        XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, xParaInfo);
                        m_documentTitle = xTextRange.getString();
                        break;
                    }
                }
            }
        }
        return m_documentTitle;
    }

    private void loadDocumentTitleConf() {
        m_documentTitleStyle = Util.getStringConf("HTMLOptionsPage", "txtDocumentTitleStyle", "");
    }

    private boolean isDocumentTitleParagraph(XServiceInfo xParaInfo) {
        if (m_documentTitleStyle.isEmpty()) {
            return false;
        }

        XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaInfo);
        String paraStyleName = (String) getProperty("ParaStyleName", xPropertySet, null);
        if (!paraStyleName.equals(m_documentTitleStyle)) {
            return false;
        }

        if (m_documentTitle == null) {
            XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, xParaInfo);
            m_documentTitle = xTextRange.getString();
        }

        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Page break handling">
    private boolean m_pageBreakEnabled;
    private int m_pageBreakFreq;
    private String m_pageBreakHtml = "<hr title=\"\" alt=\"\" class=\"system-pagebreak\" />";
    private int m_pageBreakCounter;

    private void loadPageBreakConf() {
        m_pageBreakEnabled = Util.getShortConf("HTMLOptionsPage", "chkPageBreakEnabled", (short)0) == 1 ? true : false;
        if (m_pageBreakEnabled) {
            m_pageBreakFreq = Util.getStringConfAsInt("HTMLOptionsPage", "txtPageBreakFreq", 0);
            m_pageBreakHtml = Util.getStringConf("HTMLOptionsPage", "txtPageBreakHtml", "");
            if (m_pageBreakFreq <= 0 || m_pageBreakHtml.isEmpty()) {
                m_pageBreakEnabled = false;
            }
        }
    }

    private void initPageBreak() {
        if (m_pageBreakEnabled) {
            m_pageBreakCounter = m_pageBreakFreq;
        } else {
            m_pageBreakCounter = 0;
        }
    }

    private void incPageBreakCounter() {
        if (m_pageBreakCounter > 0) {
            --m_pageBreakCounter;
        }
    }

    private void doPageBreak() throws NoSuchElementException, WrappedTargetException {
        if (m_pageBreakEnabled) {
            if (m_pageBreakCounter == 0) {
                genFootnotes();
                genEndnotes();
                genPagebreak();
                m_pageBreakCounter = m_pageBreakFreq;
            }
        }
    }

    private void genPagebreak() {
        m_builder.appendHtml(m_pageBreakHtml);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Footnote/endnote handling">

    private XIndexAccess m_xFootnotes;
    private XIndexAccess m_xEndnotes;

    private Map<Integer, XServiceInfo> m_footnotes;
    private Map<Integer, XServiceInfo> m_endnotes;

    private int m_footnoteCounter;
    private int m_endnoteCounter;

    private static final String FOOTNOTE_A_PREFIX = "webpub_footnote";
    private static final String ENDNOTE_A_PREFIX = "webpub_endnote";
    private static final String FOOTNOTE_A_CLASS = "webpub_footnote";
    private static final String ENDNOTE_A_CLASS = "webpub_endnote";

    private static final String FOOTNOTES_DIVIDER_ID = "webpub_footnotes_divider";
    private static final String ENDNOTES_DIVIDER_ID = "webpub_endnotes_divider";

    private static final String FOOTNOTES_ID = "webpub_footnotes";
    private static final String ENDNOTES_ID = "webpub_endnotes";

    private void initFootnoteHandling() {
        XFootnotesSupplier xFootnoteSupplier = (XFootnotesSupplier) UnoRuntime.queryInterface(XFootnotesSupplier.class, m_xTextDocument);
        m_xFootnotes = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, xFootnoteSupplier.getFootnotes());

        XEndnotesSupplier xEndnoteSupplier = (XEndnotesSupplier) UnoRuntime.queryInterface(XEndnotesSupplier.class, m_xTextDocument);
        m_xEndnotes = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, xEndnoteSupplier.getEndnotes());

        m_footnotes = new TreeMap();
        m_endnotes = new TreeMap();

        m_footnoteCounter = 0;
        m_endnoteCounter = 0;
    }

    private void genFootnoteTextPortion(XServiceInfo xTextInfo) throws NoSuchElementException, WrappedTargetException {
        String href;
        String cls;

        if (isFootnote(xTextInfo)) {
            ++m_footnoteCounter;
            m_footnotes.put(m_footnoteCounter, xTextInfo);
            href = String.format("#%s%d", FOOTNOTE_A_PREFIX, m_footnoteCounter);
            cls = FOOTNOTE_A_CLASS;
        } else {
            ++m_endnoteCounter;
            m_endnotes.put(m_endnoteCounter, xTextInfo);
            href = String.format("#%s%d", ENDNOTE_A_PREFIX, m_endnoteCounter);
            cls = ENDNOTE_A_CLASS;
        }

        XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, xTextInfo);
        String text = xTextRange.getString();

        m_builder.beginTag("a");
        m_builder.attr("href", href);
        m_builder.attr("class", cls);
        m_builder.append(text);
        m_builder.endTag("a");
    }

    private boolean isFootnote(XServiceInfo xTextInfo) {
        XPropertySet textInfoPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextInfo);
        Object oFootnote = getProperty("Footnote", textInfoPropertySet, null);

        XPropertySet footnotePropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, oFootnote);
        Short referenceId = (Short) getProperty("ReferenceId", footnotePropertySet, null);

        for (int i = 0; i < m_xEndnotes.getCount(); ++i) {
            try {
                Object o = m_xEndnotes.getByIndex(i);
                XPropertySet propertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, o);
                Short rid = (Short) getProperty("ReferenceId", propertySet, null);
                if (referenceId.equals(rid)) {
                    return false;
                }
            } catch (com.sun.star.uno.Exception ex) {
            }
        }

        return true;
    }

    private void genFootnotes() throws NoSuchElementException, WrappedTargetException {
        genNotes(m_footnotes, FOOTNOTES_DIVIDER_ID, FOOTNOTES_ID, FOOTNOTE_A_PREFIX);
    }

    private void genEndnotes() throws NoSuchElementException, WrappedTargetException {
        genNotes(m_endnotes, ENDNOTES_DIVIDER_ID,  ENDNOTES_ID, ENDNOTE_A_PREFIX);
    }

    private void genNotes(Map<Integer, XServiceInfo> notes, String dividerID, String notesID, String anchorPrefix) throws NoSuchElementException, WrappedTargetException {
        if (!notes.isEmpty()) {
            m_builder.beginTagNewLine("div");
            m_builder.attr("id", dividerID);
            m_builder.endTag("div");

            m_builder.beginTagNewLine("div");
            m_builder.attr("id", notesID);

            for (Entry<Integer, XServiceInfo> entry:notes.entrySet()) {
                XServiceInfo xTextInfo = entry.getValue();
                XPropertySet textInfoPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextInfo);
                Object oFootnote = getProperty("Footnote", textInfoPropertySet, null);

                m_builder.beginTagNewLine("dl");

                m_builder.beginTagNewLine("dt");
                XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, xTextInfo);
                String text = xTextRange.getString();
                m_builder.beginTag("a");
                m_builder.attr("name", String.format("%s%d", anchorPrefix, entry.getKey()));
                m_builder.append(text);
                m_builder.endTag("a");
                m_builder.endTag("dt");

                m_builder.beginTag("dd");
                genNoteText(oFootnote);
                m_builder.endTag("dd");

                m_builder.endTag("dl");
            }

            m_builder.endTag("div");

            notes.clear();
        }
    }

    private void genNoteText(Object oFootnote) throws NoSuchElementException, WrappedTargetException {
        XText xText = (XText) UnoRuntime.queryInterface(XText.class, oFootnote);
        XEnumerationAccess xParaAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xText);
        XEnumeration xParaEnum = xParaAccess.createEnumeration();
        if (xParaEnum.hasMoreElements()) {
            XServiceInfo xParaInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xParaEnum.nextElement());
            if (!xParaInfo.supportsService("com.sun.star.text.TextTable")) {
                m_xParaPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaInfo);

                String paraStyleName = (String) getProperty("ParaStyleName", m_xParaPropertySet, null);
                m_xParaStyle = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_paraStyles.getByName(paraStyleName));

                genParaContent(xParaInfo);
            }
        }
    }

    private void genNotesCss(String dividerID, String noteID, String border, String footnoteAClass) {
        m_builder.cssOpen("#" + dividerID);
        m_builder.cssAttr("margin-top", "2em");
        m_builder.cssAttr("width", "25%");
        m_builder.cssAttr("border-top", border);
        m_builder.cssClose();

        m_builder.cssOpen("#" + noteID);
        m_builder.cssClose();

        m_builder.cssOpen("#" + noteID + " dt");
        m_builder.cssAttr("clear", "both");
        m_builder.cssAttr("float", "left");
        m_builder.cssAttr("text-align", "right");
        m_builder.cssAttr("width", "1em");
        m_builder.cssClose();

        m_builder.cssOpen("#" + noteID + " dd");
        m_builder.cssAttr("margin-left", "1.5em");
        m_builder.cssClose();

        m_builder.cssOpen("a." + footnoteAClass);
        m_builder.cssAttr("vertical-align", "super");
        m_builder.cssAttr("font-size", "80%");
        m_builder.cssClose();
    }

    private void genNotesCss() {
        if (m_footnoteCounter > 0) {
            genNotesCss(FOOTNOTES_DIVIDER_ID, FOOTNOTES_ID, "1px solid black", FOOTNOTE_A_CLASS);
        }

        if (m_endnoteCounter > 0) {
            genNotesCss(ENDNOTES_DIVIDER_ID, ENDNOTES_ID, "3px double black", ENDNOTE_A_CLASS);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="List handling">

    class ListHandler {
        Stack m_lists = new Stack();

        short m_numberingLevel;
        short m_numberingType;

        public void visitParagraph(XServiceInfo xParaInfo, HTMLBuilder builder) {
            XPropertySet xParagraphPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaInfo);

            m_numberingLevel = -1;
            m_numberingType = -1;

            Short outlineLevel = (Short) getProperty("OutlineLevel", xParagraphPS, null);
            if (outlineLevel.shortValue() == 0) {
                Short oNumberingLevel = (Short) getProperty("NumberingLevel", xParagraphPS, null);
                XIndexReplace xNumberingRules = (XIndexReplace) UnoRuntime.queryInterface(XIndexReplace.class, getProperty("NumberingRules", xParagraphPS, null));
                if (xNumberingRules != null) {
                    try {
                        PropertyValue[] properties = (PropertyValue[]) xNumberingRules.getByIndex(oNumberingLevel.shortValue());
                        for (PropertyValue p: properties) {
                            if (p.Name.equals("NumberingType")) {
                                Short oNumberingType = (Short)p.Value;
                                m_numberingType = oNumberingType.shortValue();
                                m_numberingLevel = oNumberingLevel.shortValue();
                                break;
                            }
                        }
                    } catch (com.sun.star.uno.Exception ex) {
                    }
                }
            }
            
            emitTags(builder);
        }
        public boolean isListItem() {
            return m_numberingLevel != -1;
        }

        public void flush(HTMLBuilder builder) {
            m_numberingLevel = -1;
            emitTags(builder);
        }

        private void emitTags(HTMLBuilder builder) {
            if (m_numberingLevel != m_lists.size() - 1 || (!m_lists.isEmpty() && m_numberingType != ((Short)m_lists.lastElement()).shortValue())) {
                while (!m_lists.empty() && m_numberingLevel < m_lists.size()) {
                    if ((Short)m_lists.pop() == NumberingType.CHAR_SPECIAL) {
                        builder.endTag("ul");
                    } else {
                        builder.endTag("ol");
                    }
                }

                while (m_numberingLevel >= m_lists.size()) {
                    m_lists.push(new Short(m_numberingType));
                    if (m_numberingType == NumberingType.CHAR_SPECIAL) {
                        builder.beginTagNewLine("ul");
                    } else {
                        builder.beginTagNewLine("ol");
                    }
                }
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Paragraphs to table handling">

    class ParagraphsToTable {
        String[] m_tableParagraphs;
        boolean m_inTable;
        String m_classAttr;
        String m_styleAttr;

        public ParagraphsToTable() {
            String temp = Util.getStringConf("HTMLOptionsPage", "txtTableParagraphs", "");
            m_tableParagraphs = StringUtils.stripAll(StringUtils.split(temp, ","));
        }

        public void visitParagraph(XServiceInfo xParaInfo, String classAttr, String styleAttr, HTMLBuilder builder) {
            if (m_tableParagraphs.length == 0) {
                return;
            }

            XPropertySet xParagraphPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaInfo);
            String paraStyleName = (String) getProperty("ParaStyleName", xParagraphPS, null);
            if (ArrayUtils.contains(m_tableParagraphs, paraStyleName)) {
                if (!m_inTable) {
                    builder.beginTagNewLine("table");
                    if (!classAttr.isEmpty()) {
                        builder.attr("class", "table_" + classAttr);
                    }
                    m_inTable = true;
                } else {
                    builder.endTag("td");
                    builder.endTag("tr");
                }
                builder.beginTagNewLine("tr");
                builder.beginTag("td");
            } else {
                if (m_inTable) {
                    builder.endTag("td");
                    builder.endTag("tr");
                    builder.endTag("table");

                    m_inTable = false;
                }
            }

            m_classAttr = classAttr;
            m_styleAttr = styleAttr;
        }

        public void breakParagraph(HTMLBuilder builder) {
            if (m_inTable) {
                builder.endTag("p");
                builder.endTag("td");

                builder.beginTag("td");

                builder.beginTag("p");

                if (!m_classAttr.isEmpty()) {
                    builder.attr("class", m_classAttr);
                }

                if (!m_styleAttr.isEmpty()) {
                    builder.attr("style", m_styleAttr);
                }
            }
        }

        public void flush(HTMLBuilder builder) {
            if (m_inTable) {
                builder.endTag("td");
                builder.endTag("tr");
                builder.endTag("table");
            }
        }

        public boolean isInTable() {
            return m_inTable;
        }
    }

    // </editor-fold>
}
