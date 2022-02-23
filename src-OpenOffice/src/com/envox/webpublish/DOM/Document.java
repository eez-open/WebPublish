/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.envox.webpublish.CommandProgress;
import com.envox.webpublish.FontAlternative;
import com.envox.webpublish.Helper.ByteArrayXStream;
import com.envox.webpublish.Helper.ReplaceCallback;
import com.envox.webpublish.ParagraphsToTableRule;
import com.envox.webpublish.Profile;
import com.envox.webpublish.Util;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.document.XEmbeddedObjectSupplier2;
import com.sun.star.document.XStorageBasedDocument;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XStream;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lib.uno.adapter.XOutputStreamToByteArrayAdapter;
import com.sun.star.style.NumberingType;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.table.XCell;
import com.sun.star.text.SectionFileLink;
import com.sun.star.text.XEndnotesSupplier;
import com.sun.star.text.XFootnotesSupplier;
import com.sun.star.text.XNumberingRulesSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextEmbeddedObjectsSupplier;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextGraphicObjectsSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class Document {
    // <editor-fold desc="Constants">

    static final String FOOTNOTE_A_PREFIX = "webpub_footnote";
    static final String ENDNOTE_A_PREFIX = "webpub_endnote";
    static final String FOOTNOTE_A_CLASS = "webpub_footnote";
    static final String ENDNOTE_A_CLASS = "webpub_endnote";
    static final String FOOTNOTE_BACK_A_PREFIX = "webpub_footnote_back";
    static final String ENDNOTE_BACK_A_PREFIX = "webpub_endnote_back";
    static final String FOOTNOTE_BACK_A_CLASS = "webpub_footnote_back";
    static final String ENDNOTE_BACK_A_CLASS = "webpub_endnote_back";
    static final String FOOTNOTES_DIVIDER_ID = "webpub_footnotes_divider";
    static final String ENDNOTES_DIVIDER_ID = "webpub_endnotes_divider";
    static final String FOOTNOTES_ID = "webpub_footnotes";
    static final String ENDNOTES_ID = "webpub_endnotes";

    // </editor-fold>

    // <editor-fold desc="Private fields">

    XTextDocument m_xTextDocument;

    Profile m_profile;

    private BlockContainer m_blocks;

    private final List<Filter> m_filters = new ArrayList<Filter>();

    private final List<Table> m_tables = new ArrayList<Table>();
    private final List<XTextContent> m_xTextFrames = new ArrayList<XTextContent>();
    private final List<XTextContent> m_xImageFrames = new ArrayList<XTextContent>();
    private final List<XTextContent> m_xEmbeddedObjectFrames = new ArrayList<XTextContent>();
    private List<TextFrame> m_textFrames = new ArrayList<TextFrame>();
    private final List<ImageFrame> m_imageFrames = new ArrayList<ImageFrame>();
    private final List<EmbeddedObjectFrame> m_embeddedObjectFrames = new ArrayList<EmbeddedObjectFrame>();

    private XNameContainer m_xPageStyles;
    private XNameContainer m_xParagraphStyles;
    private XNameContainer m_xCharacterStyles;
    private final Map<String, Style> m_paragraphStyles = new TreeMap<String, Style>();
    private final Map<String, Style> m_characterStyles = new TreeMap<String, Style>();
    private final String m_defaultParagraphStyleName = "Standard";
    private Style m_defaultParagraphStyle;

    private int m_bodyWidth;

    private XIndexAccess m_xFootnotes;
    private XIndexAccess m_xEndnotes;
    private final Map<Integer, FootnoteTextPortion> m_footnotes = new TreeMap<Integer, FootnoteTextPortion>();
    private final Map<Integer, FootnoteTextPortion> m_endnotes = new TreeMap<Integer, FootnoteTextPortion>();

    private ImageHandler m_imageHandler;
    private final Map<String, byte[]> m_imagesData = new TreeMap<String, byte[]>();

    private FontHandler m_fontHandler;

    private CommandProgress m_progress;
    private int m_numParagraphs;
    private int m_paragraphCounter;

    private final StringBuilder m_css = new StringBuilder();

    private List<ParagraphsToTableRule> m_rules;

    // </editor-fold>

    // <editor-fold desc="Public methods">

    public Document(XTextDocument xTextDocument, Profile profile) throws NoSuchElementException, WrappedTargetException, Exception {
        m_xTextDocument = xTextDocument;
        m_profile = profile;
        init();
    }

    public void load(CommandProgress progress) throws NoSuchElementException, WrappedTargetException, Exception {
        m_progress = progress;
        m_paragraphCounter = 0;

        loadTextFrames();
        loadImages();
        loadEmbeddedObjects();
        loadNumberingRules();
        m_blocks = createMainBlock(m_xTextDocument.getText());
    }

    public void addFilter(Filter filter) {
        m_filters.add(filter);
    }

    public void setImageHandler(ImageHandler imageHandler) {
        m_imageHandler = imageHandler;
    }

    public void setFontHandler(FontHandler fontHandler) {
        m_fontHandler = fontHandler;
    }

    public List<Block> getBlocks() {
        return m_blocks.getBlocks();
    }

    public String emitBody(CommandProgress progress) {
        HTMLBuilder builder = new HTMLBuilder();
        
        String selector = m_profile.getCSSSelector();
        if (StringUtils.isEmpty(selector)) {
            emitBody(builder);
        } else {
            builder.beginTag("div");
            builder.attr("id", selector.substring(1));
            emitBody(builder);
            builder.endTag("div");
        }
        return builder.toString();
    }

    private void emitBody(HTMLBuilder builder) {
        m_blocks.emit(builder);
    }

    public void addCSS(String css) {
        m_css.append(css);
    }

    public String emitCSS(CommandProgress progress) {
        HTMLBuilder builder = new HTMLBuilder();
        emitCSS(builder);
        return builder.toString();
    }

    private void emitCSS(HTMLBuilder builder) {
        String selector = m_profile.getCSSSelector();

        // fonts
        String customCSS = m_profile.getCustomCSS();
        if (StringUtils.isNotBlank(customCSS)) {
            final StringBuilder fontsBuilder = new StringBuilder();

            if (m_fontHandler != null && StringUtils.isNotBlank(m_profile.getCSSFontsFolder())) {
                customCSS = ReplaceCallback.find(customCSS, "@font-face.*?\\{.*?src:.*?url\\s*?\\(['\"]?([^'\")]*?)['\"]?\\).*?;.*?\\}\\s*?", Pattern.DOTALL, new ReplaceCallback.Callback() {
                    public String matches(MatchResult matches) {
                        String strStyle = matches.group(0);
                        int startStyle = matches.start(0);
                        int endStyle = matches.end(0);

                        String strUrl = matches.group(1);
                        int startUrl = matches.start(1);
                        int endUrl = matches.end(1);

                        if (!m_fontHandler.hasFont(strUrl)) {
                            try {
                                URL url = new URL(FilenameUtils.concat(m_profile.getCSSFontsFolder(), strUrl));
                                File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
                                byte[] data = new byte[(int)file.length()];
                                FileInputStream fis = new FileInputStream(file);
                                fis.read(data);
                                fis.close();
                                m_fontHandler.putFont(strUrl, data);
                            } catch (IOException ex) {
                                Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        if (m_fontHandler.hasFont(strUrl)) {
                            strUrl = m_fontHandler.getCSSFontURL(strUrl);
                        }

                        if (fontsBuilder.length() > 0) {
                            fontsBuilder.append("\n\n");
                        }

                        fontsBuilder.append(strStyle.substring(0, startUrl - startStyle));
                        fontsBuilder.append(strUrl);
                        fontsBuilder.append(strStyle.substring(endUrl - startStyle));

                        return "";
                    }
                });
            }

            String fonts = fontsBuilder.toString();

            if (!StringUtils.isEmpty(fonts)) {
                builder.appendCss("/* Fonts */");
                builder.appendCss(fonts);
                builder.appendCss("");
            }
        }

        // list css
        builder.appendCss("/* Reset */");
        if (!StringUtils.isBlank(selector)) {
            builder.cssOpen(String.format("%s, %s ol, %s ul", selector, selector, selector));
        } else {
            builder.cssOpen("ol, ul");
        }
        builder.cssAttr("margin", "0");
        builder.cssAttr("padding", "0");
        builder.cssClose();

        if (!StringUtils.isBlank(selector)) {
            builder.cssOpen(String.format("%s li", selector));
        } else {
            builder.cssOpen("li");
        }
        builder.cssAttr("list-style-position", "outside");
        builder.cssClose();

        builder.appendCss("");

        // paragraphs and characters
        builder.appendCss("/* Paragraphs and texts */");
        for (Style style : m_paragraphStyles.values()) {
            if (style.getName().equals(m_defaultParagraphStyleName)) {
                if (!StringUtils.isBlank(selector)) {
                    builder.cssOpen(String.format("%s p", selector));
                } else {
                    builder.cssOpen("p");
                }
                style.emit(builder);
                builder.cssClose();
                if (!StringUtils.isBlank(selector)) {
                    builder.cssOpen(String.format("%s li", selector));
                } else {
                    builder.cssOpen("li");
                }
                style.emit(builder);
                builder.cssClose();
            } else {
                if (!StringUtils.isBlank(selector)) {
                    builder.cssOpen(String.format("%s .%s", selector, style.getCssClassName()));
                } else {
                    builder.cssOpen(String.format(".%s", style.getCssClassName()));
                }
                style.emit(builder);
                builder.cssClose();
            }
        }
        for (Style style : m_characterStyles.values()) {
            if (!StringUtils.isBlank(selector)) {
                builder.cssOpen(String.format("%s .%s", selector, style.getCssClassName()));
            } else {
                builder.cssOpen(String.format(".%s", style.getCssClassName()));
            }
            style.emit(builder);
            builder.cssClose();
        }
        builder.appendCss("");

        // footnotes & endnotes css
        builder.appendCss("/* Footnotes & endnotes */");
        emitNotesCss(builder, true);
        builder.appendCss("");

        // paragraphs to tables css
        if (m_rules.size() > 0) {
            builder.appendCss("/* Paragraphs to tables */");
            for (ParagraphsToTableRule rule:m_rules) {
                if (StringUtils.isNotBlank(rule.getTableCSS())) {
                    String[] lines = rule.getTableCSS().split("\\r?\\n");
                    for (String line:lines) {
                        builder.appendCss(line);
                    }
                }
            }
            builder.appendCss("");
        }

        // custom CSS (except fonts)
        if (StringUtils.isNotBlank(customCSS)) {
            if (StringUtils.isNotBlank(m_profile.getCSSImagesFolder())) {
                customCSS = ReplaceCallback.find(customCSS, "background(-image)?:.*url\\s*\\(['\"]?([^'\")]*)['\"]?\\).*;", 0, new ReplaceCallback.Callback() {
                    public String matches(MatchResult matches) {
                        String strStyle = matches.group(0);
                        int startStyle = matches.start(0);
                        int endStyle = matches.end(0);

                        String strUrl = matches.group(2);
                        int startUrl = matches.start(2);
                        int endUrl = matches.end(2);
                        
                        if (!m_imageHandler.hasImage(strUrl)) {
                            try {
                                URL url = new URL(FilenameUtils.concat(m_profile.getCSSImagesFolder(), strUrl));
                                File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
                                byte[] data = new byte[(int)file.length()];
                                FileInputStream fis = new FileInputStream(file);
                                fis.read(data);
                                fis.close();
                                m_imageHandler.putImage(strUrl, data);
                            } catch (IOException ex) {
                                Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        if (m_imageHandler.hasImage(strUrl)) {
                            strUrl = m_imageHandler.getCSSImageURL(strUrl);
                        }
                        
                        return strStyle.substring(0, startUrl - startStyle)
                                + strUrl
                                + strStyle.substring(endUrl - startStyle);
                    }
                });
            }

            builder.appendCss("/* Custom CSS */");
            builder.appendCss(customCSS.trim());
        }
    }

    public String emitHTML(CommandProgress progress, String title) {
        HTMLBuilder builder = new HTMLBuilder();

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
        builder.append(title);
        builder.endTag("title");

        builder.beginTag("style");
        builder.attr("type", "text/css");
        emitCSS(builder);
        builder.endTag("style");

        builder.endTag("head");

        builder.beginTag("body");
        emitBody(builder);
        builder.endTag("body");

        builder.endTag("html");

        return builder.toString();
    }

    // </editor-fold>

    // <editor-fold desc="Document init & load">

    final void init() throws NoSuchElementException, WrappedTargetException, Exception {
        XPropertySet xDocumentPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_xTextDocument);
        m_numParagraphs = (Integer) xDocumentPropertySet.getPropertyValue("ParagraphCount");

        initImages();

        // for styles
        XStyleFamiliesSupplier styleFamiliesSupplier = (XStyleFamiliesSupplier) UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, m_xTextDocument);
        XNameAccess styleFamilies = styleFamiliesSupplier.getStyleFamilies();
        m_xPageStyles = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, styleFamilies.getByName("PageStyles"));

        String[] pageStylesNames = m_xPageStyles.getElementNames();
        if (pageStylesNames.length > 0) {
            XPropertySet xPageStyle = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_xPageStyles.getByName(pageStylesNames[0]));
            int pageWidth = (Integer) xPageStyle.getPropertyValue("Width");
            int leftMargin = (Integer) xPageStyle.getPropertyValue("LeftMargin");
            int rightMargin = (Integer) xPageStyle.getPropertyValue("RightMargin");
            m_bodyWidth = pageWidth - leftMargin - rightMargin;
        }

        m_xParagraphStyles = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, styleFamilies.getByName("ParagraphStyles"));
        m_xCharacterStyles = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, styleFamilies.getByName("CharacterStyles"));
        m_defaultParagraphStyle = createParagraphStyle(m_defaultParagraphStyleName);

        // for footnotes
        XFootnotesSupplier xFootnoteSupplier = (XFootnotesSupplier) UnoRuntime.queryInterface(XFootnotesSupplier.class, m_xTextDocument);
        m_xFootnotes = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, xFootnoteSupplier.getFootnotes());
        XEndnotesSupplier xEndnoteSupplier = (XEndnotesSupplier) UnoRuntime.queryInterface(XEndnotesSupplier.class, m_xTextDocument);
        m_xEndnotes = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, xEndnoteSupplier.getEndnotes());
    }

    void initImages() throws Exception {
        initDocumentImages(m_xTextDocument);

        XTextSectionsSupplier xTextSectionsSupplier = (XTextSectionsSupplier) UnoRuntime.queryInterface(XTextSectionsSupplier.class, m_xTextDocument);
        if (xTextSectionsSupplier != null) {
            XNameAccess textSections = xTextSectionsSupplier.getTextSections();
            String[] textSectionsNames = textSections.getElementNames();
            for (String textSection:textSectionsNames) {
                Object oTextSection = textSections.getByName(textSection);
                XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, oTextSection);
                SectionFileLink fileLink = (SectionFileLink) xPropertySet.getPropertyValue("FileLink");
                if (fileLink != null && fileLink.FileURL != null && StringUtils.isNotBlank(fileLink.FileURL) && !m_imagesData.containsKey(fileLink.FileURL)) {
                    try {
                        Object desktop = Util.getContext().getServiceManager().createInstanceWithContext(
                            "com.sun.star.frame.Desktop", Util.getContext());

                        XComponentLoader xComponentLoader = (XComponentLoader)UnoRuntime.queryInterface(
                            XComponentLoader.class, desktop);

                        PropertyValue[] loadProps = new PropertyValue[2];
                        loadProps[0] = new PropertyValue();
                        loadProps[0].Name = "Hidden";
                        loadProps[0].Value = true;
                        loadProps[1] = new PropertyValue();
                        loadProps[1].Name = "ReadOnly";
                        loadProps[1].Value = true;

                        XComponent xComponent = xComponentLoader.loadComponentFromURL(fileLink.FileURL, "_blank", 0, loadProps);
                        if (xComponent != null) {
                            initDocumentImages(xComponent);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    private byte[] getImageData(String fileName, Object xImage) throws IOException, Exception {
        XStream xStream = (XStream) UnoRuntime.queryInterface(
            XStream.class, xImage);

        XInputStream xInputStream = (XInputStream) UnoRuntime.queryInterface(
            XInputStream.class, xStream.getInputStream());

        XOutputStreamToByteArrayAdapter outputStream = new XOutputStreamToByteArrayAdapter();

        byte[][] data = new byte[1][];

        xInputStream.readBytes(data, xInputStream.available());

        return data[0];
    }

    void initDocumentImages(Object xTextDocument) throws Exception {
        XStorageBasedDocument xStorageBasedDocument = (XStorageBasedDocument) UnoRuntime.queryInterface(
            XStorageBasedDocument.class, xTextDocument);

        XNameAccess xDocStorageNameAccess = (XNameAccess) UnoRuntime.queryInterface(
            XNameAccess.class, xStorageBasedDocument.getDocumentStorage());

        if (xDocStorageNameAccess.hasByName("Pictures")) {
            XNameAccess xNameAccess = (XNameAccess) UnoRuntime.queryInterface(
               XNameAccess.class, xDocStorageNameAccess.getByName("Pictures"));

            String[] imagesFileNames = xNameAccess.getElementNames();
            for (String imageFileName:imagesFileNames) {
                try {
                    byte[] imageData = getImageData(imageFileName, xNameAccess.getByName(imageFileName));
                    m_imagesData.put(imageFileName, imageData);
                } catch (IOException ex) {
                }
            }
        } 
    }

    public void usePageBreakFilter() throws Exception {
        if (m_profile.getPageBreakEnabled()) {
            m_filters.add(createPageBreakFilter(m_profile.getPageBreakFreq(), m_profile.getPageBreakHTML()));
        }
    }

    public void useListFilter() throws Exception {
        m_filters.add(createListFilter());
    }

    public void useMergeParagraphsFilter() throws Exception {
        m_filters.add(createMergeParagraphsFilter());
    }

    public void useTableToParagraphsFilter() throws Exception {
        m_rules = m_profile.getParagraphsToTableRules();
        if (m_rules.size() > 0) {
            m_filters.add(createParagraphsToTableFilter(m_rules));
        }
    }

    public void useFootnotesFilter() throws Exception {
        m_filters.add(createFootnotesFilter());
    }

    public void useDocumentTitleFilter() throws Exception {
        String documentTitleStyle = m_profile.getDocumentTitleStyle();
        if (StringUtils.isNotBlank(documentTitleStyle)) {
            m_filters.add(createDocumentTitleFilter(documentTitleStyle));
        }
    }

    private void loadTextFrames() throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {
        XTextFramesSupplier xSupplier = (XTextFramesSupplier)
                UnoRuntime.queryInterface(XTextFramesSupplier.class, m_xTextDocument);
        XNameAccess xObjects = xSupplier.getTextFrames();
        String[] objectNames =  xObjects.getElementNames();
        for (String name:objectNames) {
            XTextContent xTextContent = (XTextContent)
                UnoRuntime.queryInterface(XTextContent.class, xObjects.getByName(name));
            XPropertySet xPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextContent);
            m_xTextFrames.add(xTextContent);
        }
    }

    private void loadImages() throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {
        XTextGraphicObjectsSupplier xSupplier = (XTextGraphicObjectsSupplier)
            UnoRuntime.queryInterface(XTextGraphicObjectsSupplier.class, m_xTextDocument);
        XNameAccess xObjects = xSupplier.getGraphicObjects();
        String[] objectNames =  xObjects.getElementNames();
        for (String name:objectNames) {
            XTextContent xTextContent = (XTextContent)
                UnoRuntime.queryInterface(XTextContent.class, xObjects.getByName(name));
            XPropertySet xPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextContent);
            m_xImageFrames.add(xTextContent);
        }
    }
    
    private void loadEmbeddedObjects() throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {
        XTextEmbeddedObjectsSupplier xSupplier = (XTextEmbeddedObjectsSupplier)
            UnoRuntime.queryInterface(XTextEmbeddedObjectsSupplier.class, m_xTextDocument);
        XNameAccess xObjects = xSupplier.getEmbeddedObjects();
        String[] objectNames =  xObjects.getElementNames();
        for (String name:objectNames) {
            XTextContent xTextContent = (XTextContent)
                UnoRuntime.queryInterface(XTextContent.class, xObjects.getByName(name));
            XPropertySet xPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextContent);
            m_xEmbeddedObjectFrames.add(xTextContent);

            XEmbeddedObjectSupplier2 xEmbeddedObjectSupplier2 = (XEmbeddedObjectSupplier2)
                    UnoRuntime.queryInterface(XEmbeddedObjectSupplier2.class, xTextContent);
            if (xEmbeddedObjectSupplier2 != null) {
                XGraphic xGraphic = xEmbeddedObjectSupplier2.getReplacementGraphic();
                if (xGraphic != null) {
                    String imageName = (String) xPS.getPropertyValue("LinkDisplayName");
                    imageName = Normalizer.normalize(imageName, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                    imageName = FilenameUtils.removeExtension(imageName) + ".png";

                    Object oGraphicProvider;
                    try {
                        PropertyValue[] exportProperties = new PropertyValue[2];

                        exportProperties[0] = new PropertyValue();
                        exportProperties[0].Name = "MimeType";
                        exportProperties[0].Value = "image/png";

                        exportProperties[1] = new PropertyValue();
                        exportProperties[1].Name = "OutputStream";
                        ByteArrayXStream outputStream = new ByteArrayXStream();
                        exportProperties[1].Value = outputStream;

                        oGraphicProvider = Util.getContext().getServiceManager().createInstanceWithContext("com.sun.star.graphic.GraphicProvider", Util.getContext());
                        XGraphicProvider xGraphicProvider = (XGraphicProvider) UnoRuntime.queryInterface(XGraphicProvider.class, oGraphicProvider);
                        xGraphicProvider.storeGraphic(xGraphic, exportProperties);

                        m_imagesData.put(imageName, outputStream.getBuffer());
                    } catch (Exception ex) {
                        Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="Factory mathods">

    BlockContainer createBlockContainer() {
        return new BlockContainer(this);
    }

    BlockContainer createBlockContainer(Block block) {
        return new BlockContainer(this, block);
    }

    BlockContainer createBlockContainer(XText xText) {
        return new BlockContainer(this, xText);
    }

    BlockContainer createMainBlock(XText xText) {
        return new MainBlock(this, xText);
    }

    Paragraph createParagraph(Object xObject, boolean firstParagraph) {
        return new Paragraph(this, xObject, firstParagraph);
    }

    Paragraph createParagraph() {
        return new Paragraph(this);
    }

    Table createTable(Object xObject) {
        Table table = new Table(this, xObject);
        m_tables.add(table);
        return table;
    }

    Table createTable() {
        Table table = new Table(this);
        m_tables.add(table);
        return table;
    }

    PageBreak createPageBreak() {
        return new PageBreak(this);
    }

    public FootnotesBlock createFootnotesBlock(List<FootnoteTextPortion> footnotes) {
        return new FootnotesBlock(this, footnotes);
    }

    ListBlock createListBlock(ListBlock parent, short numberingType) {
        return new ListBlock(this, parent, numberingType);
    }

    DivBlock createDivBlock() {
        return new DivBlock(this);
    }

    TextFrame createTextFrame(Object xObject) {
        TextFrame textFrame = new TextFrame(this, xObject);
        m_textFrames.add(textFrame);
        return textFrame;
    }

    ImageFrame createImageFrame(Object xObject) {
        ImageFrame imageFrame = new ImageFrame(this, xObject);
        m_imageFrames.add(imageFrame);
        return imageFrame;
    }

    EmbeddedObjectFrame createEmbeddedObjectFrame(Object xObject) {
        EmbeddedObjectFrame embeddedObjectFrame = new EmbeddedObjectFrame(this, xObject);
        m_embeddedObjectFrames.add(embeddedObjectFrame);
        return embeddedObjectFrame;
    }

    TextTextPortion createTextTextPortion(Paragraph paragraph, Object xObject) {
        return new TextTextPortion(paragraph, this, xObject);
    }

    TextTextPortion createTextTextPortion() {
        return new TextTextPortion(this);
    }

    BookmarkTextPortion createBookmarkTextPortion(Paragraph paragraph, Object xObject) {
        return new BookmarkTextPortion(paragraph, this, xObject);
    }

    FootnoteTextPortion createFootnoteTextPortion(Paragraph paragraph, Object xObject) {
        FootnoteTextPortion footnote = new FootnoteTextPortion(paragraph, this, xObject);

        if (footnote.isFootnoteOrEndnote()) {
            int index = m_footnotes.size() + 1;
            footnote.setIndex(index);
            m_footnotes.put(index, footnote);
        } else {
            int index = m_endnotes.size() + 1;
            footnote.setIndex(index);
            m_endnotes.put(index, footnote);
        }

        return footnote;
    }

    TableRow createTableRow() {
        return new TableRow(this);
    }

    TableRow createTableRow(Object xTableRow) {
        return new TableRow(this, xTableRow);
    }

    TableCell createTableCell(XCell xCell) {
        return new TableCell(this, xCell);
    }

    TableCell createTableCell() {
        return new TableCell(this);
    }

    Style createParagraphStyle(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        Style style = m_paragraphStyles.get(name);
        if (style != null) {
            return style;
        }

        Object oStyle;
        try {
            oStyle = m_xParagraphStyles.getByName(name);
        } catch (Exception ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        style = new Style(this, oStyle, name.equals(m_defaultParagraphStyleName) ? null : m_defaultParagraphStyle);
        style.setName(name);
        style.loadParagraphProperties();
        m_paragraphStyles.put(name, style);

        return style;
    }

    Style createParagraphStyle(Object xObject, String parentStyleName) {
        Style parentStyle = createParagraphStyle(parentStyleName);
        Style style = new Style(this, xObject, parentStyle);
        style.loadParagraphProperties();
        return style;
    }

    Style createCharacterStyle(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        Style style = m_characterStyles.get(name);
        if (style != null) {
            return style;
        }

        Object oStyle;
        try {
            oStyle = m_xCharacterStyles.getByName(name);
        } catch (Exception ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        style = new Style(this, oStyle);
        style.setName(name);
        style.loadCharacterProperties();
        m_characterStyles.put(name, style);

        return style;
    }

    Style createCharacterStyle(Object xObject, String characterStyleParentName, Style paragraphStyleParent) {
        Style characterStyleParent = createCharacterStyle(characterStyleParentName);
        Style style = new Style(this, xObject, characterStyleParent, paragraphStyleParent);
        style.loadCharacterProperties();
        return style;
    }

    Style createStyle(Object xObject) {
        return new Style(this, xObject);
    }

    Style createStyle() {
        return new Style(this);
    }

    ListFilter createListFilter() {
        return new ListFilter(this);
    }

    MergeParagraphsFilter createMergeParagraphsFilter() {
        return new MergeParagraphsFilter(this);
    }

    ParagraphsToTableFilter createParagraphsToTableFilter(List<ParagraphsToTableRule> rules) {
        return new ParagraphsToTableFilter(this, rules);
    }

    public PageBreakFilter createPageBreakFilter(int freq, String html) {
        return new PageBreakFilter(this, freq, html);
    }

    public FootnotesFilter createFootnotesFilter() {
        return new FootnotesFilter(this);
    }

    public DocumentTitleFilter createDocumentTitleFilter(String documentTitleStyle) {
        return new DocumentTitleFilter(this, documentTitleStyle);
    }

    // </editor-fold>

    // <editor-fold desc="Helper methods">

    boolean compareTextRanges(XTextRangeCompare xTextRangeCompare, XTextRange xTextRange1, XTextRange xTextRange2) {
        if (xTextRange1 == null || xTextRange2 == null) {
            return false;
        }

        if (UnoRuntime.areSame(xTextRange1.getText(), xTextRange2.getText())) {
            try {
                if (xTextRangeCompare.compareRegionStarts(xTextRange1, xTextRange2) == 0 || xTextRangeCompare.compareRegionEnds(xTextRange1, xTextRange2) == 0) {
                    return true;
                }
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return false;
    }

    List<Block> getFramesAtBlock(Block block) {
        List<Block> frames = new ArrayList<Block>();

        XTextRange xBlockTextRange = block.getTextRange();
        if (xBlockTextRange == null) {
            return frames;
        }

        XTextRangeCompare xTextRangeCompare = (XTextRangeCompare) UnoRuntime.queryInterface(XTextRangeCompare.class, xBlockTextRange.getText());

        Iterator<XTextContent> textFramesIterator = m_xTextFrames.iterator();
        while (textFramesIterator.hasNext()) {
            XTextContent xTextContent = textFramesIterator.next();
            if (compareTextRanges(xTextRangeCompare, xTextContent.getAnchor(), xBlockTextRange)) {
                textFramesIterator.remove();
                frames.add(createTextFrame(xTextContent));
            }
        }

        Iterator<XTextContent> imageFramesIterator = m_xImageFrames.iterator();
        while (imageFramesIterator.hasNext()) {
            XTextContent xTextContent = imageFramesIterator.next();
            if (compareTextRanges(xTextRangeCompare, xTextContent.getAnchor(), xBlockTextRange)) {
                imageFramesIterator.remove();
                frames.add(createImageFrame(xTextContent));
            }
        }

        Iterator<XTextContent> embeddedObjectsIterator = m_xEmbeddedObjectFrames.iterator();
        while (embeddedObjectsIterator.hasNext()) {
            XTextContent xTextContent = embeddedObjectsIterator.next();
            if (compareTextRanges(xTextRangeCompare, xTextContent.getAnchor(), xBlockTextRange)) {
                embeddedObjectsIterator.remove();
                frames.add(createEmbeddedObjectFrame(xTextContent));
            }
        }

        // sort frames according to VertOrientPosition
        if (frames.size() > 1) {
            List<Block> sorted = new ArrayList<Block>();

            sorted.add(frames.get(0));
            for (int i = 1; i < frames.size(); ++i) {
                Block frame = frames.get(i);
                Integer pos1 = (Integer) frame.getProperty("VertOrientPosition");

                int j;
                for (j = 0; j < sorted.size(); ++j) {
                    Integer pos2 = (Integer) sorted.get(j).getProperty("VertOrientPosition");
                    if (pos1 < pos2) {
                        sorted.add(j, frame);
                        break;
                    }
                }

                if (j == sorted.size()) {
                    sorted.add(frame);
                }
            }

            frames = sorted;
        }


        return frames;
    }

    boolean isFootnoteOrEndnote(Object xObject) {
        XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xObject);
        Short referenceId;
        try {
            referenceId = (Short) xPropertySet.getPropertyValue("ReferenceId");
        } catch (Exception ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
            return true;
        }

        for (int i = 0; i < m_xEndnotes.getCount(); ++i) {
                Object oEndnote;
                try {
                    oEndnote = m_xEndnotes.getByIndex(i);
                } catch (Exception ex) {
                    Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }

                XPropertySet xEndnotePropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, oEndnote);
                Short endnoteReferenceId;
                try {
                    endnoteReferenceId = (Short) xEndnotePropertySet.getPropertyValue("ReferenceId");
                } catch (Exception ex) {
                    Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }

                if (referenceId.equals(endnoteReferenceId)) {
                    return false;
                }
        }

        return true;
    }

    String registerImageURL(String graphicURL, String name) {
        if (graphicURL.startsWith("vnd.sun.star.GraphicObject:")) {
            graphicURL = graphicURL.substring("vnd.sun.star.GraphicObject:".length());
            
            int max = 0;
            Entry<String, byte[]> foundImage = null;
            
            for (Entry<String, byte[]> image: m_imagesData.entrySet()) {
                String imageKey = image.getKey();

                int i;
                for (i = 0; i < imageKey.length() && i < graphicURL.length(); ++i) {
                    if (imageKey.charAt(i) != graphicURL.charAt(i)) {
                        break;
                    }
                }

                if (i > max) {
                    max = i;
                    foundImage = image;
                }
            }

            if (foundImage != null) {
                name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                name = FilenameUtils.removeExtension(name) + "." + FilenameUtils.getExtension(foundImage.getKey());
                m_imageHandler.putImage(name, foundImage.getValue());
            }
        }
        return name;
    }

    String registerEmbeddedObjectImageURL(String name) {
        name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        name = FilenameUtils.removeExtension(name) + ".png";
        m_imageHandler.putImage(name, m_imagesData.get(name));
        return name;
    }

    String getImageURL(String name) {
        return m_imageHandler.getImageURL(name);
    }

    void genNotesCss(HTMLBuilder builder, 
            String dividerID, String noteID, String border, String footnoteAClass,
            String aStyle, String pStyle) {
        String selector = m_profile.getCSSSelector();

        if (!StringUtils.isBlank(selector)) {
            builder.cssOpen(String.format("%s #%s", selector, dividerID));
        } else {
            builder.cssOpen(String.format("#%s", dividerID));
        }
        builder.cssAttr("clear", "both");
        builder.cssAttr("margin-top", "2em");
        builder.cssAttr("width", "25%");
        builder.cssAttr("border-top", border);
        builder.cssClose();

        if (!StringUtils.isBlank(selector)) {
            builder.cssOpen(String.format("%s #%s", selector, noteID));
        } else {
            builder.cssOpen(String.format("#%s", noteID));
        }
        builder.cssStyle(pStyle);
        builder.cssClose();

        if (!StringUtils.isBlank(selector)) {
            builder.cssOpen(String.format("%s #%s td.label", selector, noteID));
        } else {
            builder.cssOpen(String.format("#%s td.label", noteID));
        }
        builder.cssAttr("text-align", "right");
        builder.cssAttr("vertical-align", "top");
        builder.cssClose();

        if (!StringUtils.isBlank(selector)) {
            builder.cssOpen(String.format("%s #%s td.content", selector, noteID));
        } else {
            builder.cssOpen(String.format("#%s td.content", noteID));
        }
        builder.cssClose();

        if (!StringUtils.isBlank(selector)) {
            builder.cssOpen(String.format("%s a.%s", selector, footnoteAClass));
        } else {
            builder.cssOpen(String.format("a.%s", footnoteAClass));
        }
        builder.cssStyle(aStyle);
        builder.cssClose();
    }

    void emitNotesCss(HTMLBuilder builder, boolean always) {
        if (always || !m_footnotes.isEmpty()) {
            String aStyle = "font-size: 80%; vertical-align: super;";
            String pStyle = (m_footnotes.isEmpty())
                    ? "font-size: 80%; text-align: justify;"
                    : m_footnotes.get(1).getBodyCSSStyle();
            genNotesCss(builder, FOOTNOTES_DIVIDER_ID, FOOTNOTES_ID, "1px solid black", FOOTNOTE_A_CLASS, aStyle, pStyle);
        }

        if (always || !m_endnotes.isEmpty()) {
            String aStyle = "font-size: 80%; vertical-align: super;";
            String pStyle = (m_endnotes.isEmpty())
                    ? "font-size: 80%; text-align: justify;"
                    : m_endnotes.get(1).getBodyCSSStyle();
            genNotesCss(builder, ENDNOTES_DIVIDER_ID, ENDNOTES_ID, "3px double black", ENDNOTE_A_CLASS, aStyle, pStyle);
        }
    }

    boolean isDeafaultParagraph(String paraStyleName) {
        return paraStyleName.equals(m_defaultParagraphStyleName);
    }

    List<Block> filterBlocks(BlockContainer parent, List<Block> blocks) {
        for (Filter filter:m_filters) {
            blocks = filter.process(parent, blocks);
        }
        return blocks;
    }

    List<Block> filterBlocksWithoutMergeParagraphsFilter(BlockContainer parent, List<Block> blocks) {
        for (Filter filter:m_filters) {
            if (!(filter instanceof MergeParagraphsFilter)) {
                blocks = filter.process(parent, blocks);
            }
        }
        return blocks;
    }

    String getHtmlCodeStyle() {
        return m_profile.getHtmlCodeStyle();
    }


    int getBodyWidth() {
        return m_bodyWidth;
    }

    int getScaling() {
        return m_profile.getScaling();
    }

    boolean getConvertToPixels() {
        return m_profile.getConvertToPixels();
    }

    void progressParagraph() {
        if (m_progress != null) {
            m_progress.progress(++m_paragraphCounter, 1, m_numParagraphs);
        }
    }

    void filterStatus(String desription) {
        if (m_progress != null) {
            m_progress.status(desription);
        }
    }

    void filterProgress(int i, int total) {
        if (m_progress != null) {
            m_progress.progress(i, 1, total);
        }
    }

    String getFontName(String fontName) {
        List<FontAlternative> alternatives = m_profile.getFontAlternatives();
        for (FontAlternative alternative:alternatives) {
            if (StringUtils.equalsIgnoreCase(alternative.getFont(), fontName)) {
                return fontName + ", " + alternative.getAlternative();
            }
        }
        return fontName;
    }

    // </editor-fold>

    String shortenURL(String url) {
        String result = url;
        if (m_profile.getShortenWebLinks()) {
            String homeAddress = m_profile.getBlogAddress();
            if (StringUtils.startsWithIgnoreCase(homeAddress, "http://") || StringUtils.startsWithIgnoreCase(homeAddress, "https://")) {
                int i = homeAddress.indexOf("/", homeAddress.indexOf("//") + 2);
                if (i != -1) {
                    homeAddress = homeAddress.substring(0, i);
                }
                if (StringUtils.startsWithIgnoreCase(url, homeAddress)) {
                    result = url.substring(homeAddress.length());
                    if (StringUtils.isEmpty(result)) {
                        result = "/";
                    }
                }
            }
        }
        return result;
    }
    
    class OutlineNumberingRule {
        short numberingType;
        short startWith;
        short index;
        String prefix;
        String suffix;
    }
    
    private OutlineNumberingRule[] outlineNumberingRules = null;
    
    private void loadNumberingRules() throws IndexOutOfBoundsException, WrappedTargetException, UnknownPropertyException {
        XNumberingRulesSupplier xSupplier = (XNumberingRulesSupplier)
            UnoRuntime.queryInterface(XNumberingRulesSupplier.class, m_xTextDocument);
        XIndexAccess xObjects = xSupplier.getNumberingRules();
        for (int i = 0; i < xObjects.getCount(); ++i) {
            Object xObject = xObjects.getByIndex(i);
            XPropertySet xPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xObject);
            Boolean numberingIsOutline = (Boolean) xPS.getPropertyValue("NumberingIsOutline");
            if (numberingIsOutline) {
                XIndexAccess xOutlineNumberingRules = (XIndexAccess) UnoRuntime.queryInterface(XIndexAccess.class, xObject);

                outlineNumberingRules = new OutlineNumberingRule[xOutlineNumberingRules.getCount()];
                
                for (int j = 0; j < xOutlineNumberingRules.getCount(); ++j) {
                    PropertyValue[] properties = (PropertyValue[]) xOutlineNumberingRules.getByIndex(j);
                    
                    outlineNumberingRules[j] = new OutlineNumberingRule();
                    outlineNumberingRules[j].numberingType = 0;
                    outlineNumberingRules[j].startWith = 1;
                    outlineNumberingRules[j].index = -1;
                    outlineNumberingRules[j].prefix = "";
                    outlineNumberingRules[j].suffix = ".";
                    
                    for (int k = 0; k < properties.length; ++k) {
                        PropertyValue property = properties[k];
                        if (property.Name.equals("NumberingType")) {
                            outlineNumberingRules[j].numberingType = (Short)property.Value;
                        } else if (property.Name.equals("StartWith")) {
                            outlineNumberingRules[j].startWith = (Short)property.Value;
                        } else if (property.Name.equals("Prefix")) {
                            outlineNumberingRules[j].prefix = (String)property.Value;
                        } else if (property.Name.equals("Sufffix")) {
                            outlineNumberingRules[j].suffix = (String)property.Value;
                        }
                    }
                }
            }
        }
    }

    void emitOutlineNumber(HTMLBuilder builder, short level, short parentNumbering, short startWith, String prefix, String suffix) {
        if (outlineNumberingRules == null) return;
        
        if (level  < outlineNumberingRules.length) {
            boolean appendSpace = false;

            for (int i = 0; i <= level && i < outlineNumberingRules.length; ++i) {
                OutlineNumberingRule rule = outlineNumberingRules[i];
                if (rule.numberingType == NumberingType.ARABIC) {
                    builder.append(rule.prefix);

                    if (rule.index == -1) {
                        rule.index = rule.startWith;
                    } else {
                        if (i == level) {
                            ++rule.index;
                        }
                    }
                    builder.append(Short.toString(rule.index));

                    builder.append(rule.suffix);
                    appendSpace = true;
                }
            }

            if (appendSpace) {
                builder.append(" ");
            }

            for (int i = level + 1; i < outlineNumberingRules.length; ++i) {
                outlineNumberingRules[i].index = -1;
            }
        }
    }
}
