package com.envox.webpublish;

import com.envox.webpublish.DOM.Document;
import com.envox.webpublish.DOM.ImageHandler;
import com.envox.webpublish.EPUB.Book;
import com.envox.webpublish.EPUB.BookProperties;
import com.envox.webpublish.EPUB.BookPropertiesDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.datatransfer.clipboard.XClipboard;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;

public final class WebPublish extends WeakBase
        implements com.sun.star.lang.XInitialization,
        com.sun.star.frame.XDispatch,
        com.sun.star.lang.XServiceInfo,
        com.sun.star.frame.XDispatchProvider {

    private final XComponentContext m_xContext;
    private XFrame m_xFrame;
    private XToolkit m_xToolkit;
    private static final String m_implementationName = WebPublish.class.getName();
    private static final String[] m_serviceNames = { "com.sun.star.frame.ProtocolHandler" };

    public WebPublish(XComponentContext context) {
        m_xContext = context;
        Util.setContext(m_xContext);
    }

    public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
        XSingleComponentFactory xFactory = null;
        if (sImplementationName.equals(m_implementationName)) {
            xFactory = Factory.createComponentFactory(WebPublish.class, m_serviceNames);
        }
        return xFactory;
    }

    public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
        return Factory.writeRegistryServiceInfo(m_implementationName,
                m_serviceNames,
                xRegistryKey);
    }

    // com.sun.star.lang.XInitialization:
    public void initialize(Object[] object)
            throws com.sun.star.uno.Exception {
        if (object.length > 0) {
            m_xFrame = (com.sun.star.frame.XFrame) UnoRuntime.queryInterface(
                    com.sun.star.frame.XFrame.class, object[0]);

            // Create the toolkit to have access to it later
            m_xToolkit = (XToolkit) UnoRuntime.queryInterface(
                    XToolkit.class,
                    m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.awt.Toolkit",
                    m_xContext));

            Util.setFrame(m_xFrame);
            Util.setToolkit(m_xToolkit);
        }
    }

    // com.sun.star.frame.XDispatch:
    public void dispatch(com.sun.star.util.URL aURL,
            com.sun.star.beans.PropertyValue[] aArguments) {
        if (aURL.Protocol.compareTo("com.envox.webpublish.webpublish:") == 0) {
            try {
                if (aURL.Path.compareTo("generateHTML") == 0) {
                    generateHTML();
                    return;
                } else if (aURL.Path.compareTo("generateBody") == 0) {
                    generateBody();
                    return;
                } else if (aURL.Path.compareTo("generateCSS") == 0) {
                    generateCSS();
                    return;
                } else if (aURL.Path.compareTo("publish") == 0) {
                    publish();
                    return;
                } else if (aURL.Path.compareTo("postProperties") == 0) {
                    postProperties();
                    return;
                } else if (aURL.Path.compareTo("generateEPUB") == 0) {
                    generateEPUB();
                    return;
                }
            } catch (CommandErrorException ex) {
                Util.showMessageBox("WebPublish", ex.getMessage());
            } catch (Throwable ex) {
                Util.showMessageBox("WebPublish", ex.getMessage());
            }
        }
    }

    public void addStatusListener(com.sun.star.frame.XStatusListener xControl,
            com.sun.star.util.URL aURL) {
    }

    public void removeStatusListener(com.sun.star.frame.XStatusListener xControl,
            com.sun.star.util.URL aURL) {
    }

    // com.sun.star.lang.XServiceInfo:
    public String getImplementationName() {
        return m_implementationName;
    }

    public boolean supportsService(String sService) {
        int len = m_serviceNames.length;

        for (int i = 0; i < len; i++) {
            if (sService.equals(m_serviceNames[i])) {
                return true;
            }
        }
        return false;
    }

    public String[] getSupportedServiceNames() {
        return m_serviceNames;
    }

    // com.sun.star.frame.XDispatchProvider:
    public com.sun.star.frame.XDispatch queryDispatch(com.sun.star.util.URL aURL,
            String sTargetFrameName,
            int iSearchFlags) {
        if (aURL.Protocol.compareTo("com.envox.webpublish.webpublish:") == 0) {
            if (aURL.Path.compareTo("generateHTML") == 0) {
                return this;
            } else if (aURL.Path.compareTo("generateBody") == 0) {
                return this;
            } else if (aURL.Path.compareTo("generateCSS") == 0) {
                return this;
            } else if (aURL.Path.compareTo("publish") == 0) {
                return this;
            } else if (aURL.Path.compareTo("postProperties") == 0) {
                return this;
            } else if (aURL.Path.compareTo("generateEPUB") == 0) {
                return this;
            }
        }
        return null;
    }

    // com.sun.star.frame.XDispatchProvider:
    public com.sun.star.frame.XDispatch[] queryDispatches(
            com.sun.star.frame.DispatchDescriptor[] seqDescriptors) {
        int nCount = seqDescriptors.length;
        com.sun.star.frame.XDispatch[] seqDispatcher =
                new com.sun.star.frame.XDispatch[seqDescriptors.length];

        for (int i = 0; i < nCount; ++i) {
            seqDispatcher[i] = queryDispatch(seqDescriptors[i].FeatureURL,
                    seqDescriptors[i].FrameName,
                    seqDescriptors[i].SearchFlags);
        }
        return seqDispatcher;
    }

    private XTextDocument getTextDocument() throws CommandErrorException {
        if (m_xFrame == null) {
            throw new CommandErrorException(CommandError.ERROR_NO_FRAME);
        }

        XController xController = m_xFrame.getController();
        if (xController == null) {
            throw new CommandErrorException(CommandError.ERROR_NO_CONTROLLER);
        }

        XModel xModel = xController.getModel();
        if (xModel == null) {
            throw new CommandErrorException(CommandError.ERROR_NO_MODEL);
        }

        XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(XTextDocument.class, xModel);
        if (xTextDocument == null) {
            throw new CommandErrorException(CommandError.ERROR_MODEL_NO_TEXT_DOCUMENT);
        }

        return xTextDocument;
    }

    private Document createHtmlDoc(CommandProgress progress, Profile profile, ImageHandler imageHandler, boolean loadDocument, boolean useDocumentTitleFilter) throws CommandErrorException {
        try {
            Document doc = new Document(getTextDocument(), profile);

            doc.usePageBreakFilter();
            doc.useListFilter();
            doc.useMergeParagraphsFilter();
            doc.useTableToParagraphsFilter();
            doc.useFootnotesFilter();
            if (useDocumentTitleFilter) {
                doc.useDocumentTitleFilter();
            }

            doc.setImageHandler(imageHandler);

            if (loadDocument) {
                doc.load(progress);
            }

            return doc;
        } catch (Exception ex) {
            throw new CommandErrorException(CommandError.ERROR_CONVERT_TO_HTML);
        }
    }

    private String getDocumentTitle(Profile profile) throws CommandErrorException {
        try {
            XTextDocument xTextDocument = getTextDocument();

            String documentTitleStyle = profile.getDocumentTitleStyle();
            if (StringUtils.isBlank(documentTitleStyle)) {
                return null;
            }
            String[] documentTitleStyles = StringUtils.stripAll(StringUtils.split(documentTitleStyle, ","));

            XText xText = xTextDocument.getText();
            XEnumerationAccess xParaAccess = (XEnumerationAccess) UnoRuntime.queryInterface(XEnumerationAccess.class, xText);
            XEnumeration xParaEnum = xParaAccess.createEnumeration();
            while (xParaEnum.hasMoreElements()) {
                XServiceInfo xParaInfo = (XServiceInfo) UnoRuntime.queryInterface(XServiceInfo.class, xParaEnum.nextElement());
                if (!xParaInfo.supportsService("com.sun.star.text.TextTable")) {
                    XPropertySet xParaPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaInfo);
                    String paraStyleName = (String) xParaPropertySet.getPropertyValue("ParaStyleName");
                    if (ArrayUtils.contains(documentTitleStyles, paraStyleName)) {
                        XTextRange xTextRange = (XTextRange) UnoRuntime.queryInterface(XTextRange.class, xParaInfo);
                        return xTextRange.getString();
                    }
                }
            }

            return null;
        } catch (Exception ex) {
            throw new CommandErrorException(CommandError.ERROR_CONVERT_TO_HTML);
        }
    }

    private void copyToClipboard(String text) throws CommandErrorException {
        Object oClipboard;
        try {
            oClipboard = m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.datatransfer.clipboard.SystemClipboard", m_xContext);
            XClipboard xClipboard = (XClipboard) UnoRuntime.queryInterface(XClipboard.class, oClipboard);
            if (xClipboard != null) {
                TextTransferable textTransferable = new TextTransferable(text);
                xClipboard.setContents(textTransferable, null);
            }
        } catch (com.sun.star.uno.Exception ex) {
            throw new CommandErrorException(CommandError.ERROR_CLIPBOARD_SERVICE_CREATION);
        }
    }

    String getSaveAsFileName(String extension) throws CommandErrorException {
        String fileName = null;

        XTextDocument xTextDocument = getTextDocument();
        XStorable xStoreable = (XStorable) UnoRuntime.queryInterface(XStorable.class, xTextDocument);
        if (xStoreable.hasLocation()) {
            fileName = xStoreable.getLocation();
            if (StringUtils.isNotBlank(fileName)) {
                try {
                    URL url;
                    try {
                        url = new URL(fileName);
                    } catch (MalformedURLException ex) {
                        return null;
                    }
                    File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
                    return FilenameUtils.removeExtension(file.getPath()) + extension;
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(WebPublish.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
            }
        }

        return fileName;
    }

    private void generateHTML() throws CommandErrorException {
        XTextDocument xTextDocument = getTextDocument();
        DocumentProperties documentProperties = new DocumentProperties(xTextDocument);
        String profileName = documentProperties.readCustomProperty("WebPublish_HTML_Profile", "");

        if (!Util.getProfiles().hasProfile(profileName)) {
            SelectProfileDialog dlg = new SelectProfileDialog(SelectProfileDialog.Type.HTML, "");
            try {
                if (!dlg.show()) {
                    return;
                }
                profileName = dlg.getProfile();
                documentProperties.writeCustomProperty("WebPublish_HTML_Profile", "", profileName);
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        final Profile profile = Util.getProfiles().getProfile(profileName);

        final URL url = Util.showFileSaveDialog(getSaveAsFileName(".html"), "HTML", "*.html;*.htm");
        if (url == null) {
            return;
        }

        CommandProgressDialog.start(new Command() {
            public void run(CommandProgress progress) throws CommandErrorException {
                File file = Util.toFile(url);

                String pathToImagesOutputFolder = FilenameUtils.getFullPath(file.getPath());
                String imagesOutputFolder = profile.getHTMLImagesOutputFolder();
                String imageFileNamePrefix;
                if (imagesOutputFolder == null || imagesOutputFolder.equals("")) {
                    imagesOutputFolder = FilenameUtils.getBaseName(file.getPath()) + " images";
                    imageFileNamePrefix = "";
                } else {
                    imageFileNamePrefix = FilenameUtils.getBaseName(file.getPath()) + "-";
                }
                HTMLImageHandler imageHandler = new HTMLImageHandler(pathToImagesOutputFolder, imagesOutputFolder, imageFileNamePrefix);

                progress.status("Analysing document ...");
                Document doc = createHtmlDoc(progress, profile, imageHandler, false, false);

                try {
                    doc.load(progress);
                } catch (Exception ex) {
                    throw new CommandErrorException(CommandError.ERROR_CONVERT_TO_HTML);
                }

                progress.status("Generating HTML ...");
                String html = doc.emitHTML(progress, getDocumentTitle(profile));
                
                progress.status("Saving HTML file ...");

                try {
                    FileOutputStream out = new FileOutputStream(file);
                    byte[] data = org.apache.commons.codec.binary.StringUtils.getBytesUtf8(html);
                    out.write(data, 0, data.length);
                    out.close();
                } catch (IOException ex) {
                    throw new CommandErrorException(CommandError.ERROR_IO_EXCEPTION);
                }

                progress.status("Saving images ...");
                imageHandler.saveImages(progress);

                progress.status("Congratulations, HTML has been saved.");
            }
        });
    }

    private void generateBody() throws CommandErrorException {
        final Profile profile;

        XTextDocument xTextDocument = getTextDocument();
        final PostProperties postProperties = new PostProperties(xTextDocument);
        postProperties.read();

        SelectProfileDialog dlg = new SelectProfileDialog(SelectProfileDialog.Type.All,
                postProperties.getPublishProfile());
        try {
            if (!dlg.show()) {
                return;
            }
            profile = Util.getProfiles().getProfile(dlg.getProfile());
        } catch (Exception ex) {
            Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        CommandProgressDialog.start(new Command() {
            public void run(CommandProgress progress) throws CommandErrorException {
                progress.status("Analysing document ...");
                Document doc = createHtmlDoc(progress, profile, new DummyImageHandler(), true, false);
                progress.status("Generating HTML body ...");
                copyToClipboard(doc.emitBody(progress));
                progress.status("HTML body is successfully copied to the clipboard.");
            }
        });
    }

    private void generateCSS() throws CommandErrorException {
        final Profile profile;

        XTextDocument xTextDocument = getTextDocument();
        final PostProperties postProperties = new PostProperties(xTextDocument);
        postProperties.read();

        SelectProfileDialog dlg = new SelectProfileDialog(SelectProfileDialog.Type.All,
                postProperties.getPublishProfile());
        try {
            if (!dlg.show()) {
                return;
            }
            profile = Util.getProfiles().getProfile(dlg.getProfile());
        } catch (Exception ex) {
            Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        CommandProgressDialog.start(new Command() {
            public void run(CommandProgress progress) throws CommandErrorException {
                progress.status("Analysing document ...");
                Document doc = createHtmlDoc(progress, profile, new DummyImageHandler(), true, false);
                progress.status("Generating CSS ...");
                copyToClipboard(doc.emitCSS(progress));
                progress.status("CSS is successfully copied to the clipboard.");
            }
        });
    }

    private void publish() throws CommandErrorException {
        XTextDocument xTextDocument = getTextDocument();

        final PostProperties postProperties = new PostProperties(xTextDocument);
        postProperties.read();

        if (!Util.getProfiles().hasProfile(postProperties.getPublishProfile())) {
            SelectProfileDialog dlg = new SelectProfileDialog(SelectProfileDialog.Type.WebPublish, "");
            try {
                if (!dlg.show()) {
                    return;
                }
                postProperties.setPublishProfile(dlg.getProfile());
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        final Profile profile = Util.getProfiles().getProfile(postProperties.getPublishProfile());
        if (profile == null) {
            throw new CommandErrorException(String.format("Profile '%s' not found.", postProperties.getPublishProfile()));
        }

        if (StringUtils.isBlank(postProperties.getTitle())) {
            postProperties.setTitle(getDocumentTitle(profile));
        }

        final MoveableTypeAPIClient blogClient;
        try {
            blogClient = profile.getBlogClient();
        } catch (MalformedURLException ex) {
            throw new CommandErrorException("Malformed blog URL.");
        } catch (XmlRpcException ex) {
            throw new CommandErrorException(String.format("XML-RPC error:\n%s", ex.getMessage()));
        }

        // show properties dialog if postId is null
        final boolean firstPublish = StringUtils.isBlank(postProperties.getPostAsPage() ? postProperties.getPageId() : postProperties.getPostId());
        if (firstPublish) {
            PostPropertiesDialog dlg = new PostPropertiesDialog(blogClient, postProperties);
            try {
                if (!dlg.show()) {
                    return;
                }
            } catch (Exception ex) {
                Logger.getLogger(WebPublish.class.getName()).log(Level.SEVERE, null, ex);
                throw new CommandErrorException(CommandError.ERROR_POST_PROPERTIES_DIALOG_CREATION);
            }
        }
        postProperties.write();

        CommandProgressDialog.start(new Command() {
            public void run(CommandProgress progress) throws CommandErrorException {
                try {
                    PublishImageHandler imageHandler = new PublishImageHandler();

                    progress.status("Analysing document ...");
                    Document doc = createHtmlDoc(progress, profile, imageHandler, true, true);

                    blogClient.init();

                    progress.status("Publishing images ...");
                    imageHandler.publishImages(postProperties, blogClient, progress);

                    progress.status("Generating HTML ...");
                    String body = doc.emitBody(progress);

                    progress.status("Publishing document ...");
                    blogClient.publish(postProperties, body);

                    if (firstPublish || imageHandler.isURLsChanged()) {
                        postProperties.write();
                    }

                    progress.status("Congratulations, your document has been published.");
                } catch (XmlRpcException ex) {
                    throw new CommandErrorException(
                            String.format("XML-RPC error:\n%s", ex.getMessage()));
                }
            }
        });
    }

    private void postProperties() throws CommandErrorException {
        XTextDocument xTextDocument = getTextDocument();

        PostProperties postProperties = new PostProperties(xTextDocument);
        postProperties.read();

        if (!Util.getProfiles().hasProfile(postProperties.getPublishProfile())) {
            SelectProfileDialog dlg = new SelectProfileDialog(SelectProfileDialog.Type.WebPublish, "");
            try {
                if (!dlg.show()) {
                    return;
                }
                postProperties.setPublishProfile(dlg.getProfile());
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        final Profile profile = Util.getProfiles().getProfile(postProperties.getPublishProfile());
        if (profile == null) {
            throw new CommandErrorException(String.format("Profile '%s' not found.", postProperties.getPublishProfile()));
        }

        if (StringUtils.isBlank(postProperties.getTitle())) {
            postProperties.setTitle(getDocumentTitle(profile));
        }

        final MoveableTypeAPIClient blogClient;
        try {
            blogClient = profile.getBlogClient();
        } catch (MalformedURLException ex) {
            throw new CommandErrorException("Malformed blog URL.");
        } catch (XmlRpcException ex) {
            throw new CommandErrorException(String.format("XML-RPC error:\n%s", ex.getMessage()));
        }

        PostPropertiesDialog dlg = new PostPropertiesDialog(blogClient, postProperties);
        try {
            if (dlg.show()) {
                postProperties.write();
            }
        } catch (Exception ex) {
            Logger.getLogger(WebPublish.class.getName()).log(Level.SEVERE, null, ex);
            throw new CommandErrorException(CommandError.ERROR_POST_PROPERTIES_DIALOG_CREATION);
        }
    }

    private void generateEPUB() throws CommandErrorException {
        final XTextDocument xTextDocument = getTextDocument();

        DocumentProperties documentProperties = new DocumentProperties(xTextDocument);
        String profileName = documentProperties.readCustomProperty("WebPublish_EPUB_Profile", "");
        if (!Util.getProfiles().hasProfile(profileName)) {
            SelectProfileDialog dlg = new SelectProfileDialog(SelectProfileDialog.Type.EPUB, "");
            try {
                if (!dlg.show()) {
                    return;
                }
                profileName = dlg.getProfile();
                documentProperties.writeCustomProperty("WebPublish_EPUB_Profile", "", profileName);
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
        final Profile profile = Util.getProfiles().getProfile(profileName);

        final BookProperties properties = new BookProperties(xTextDocument);
        properties.read();

        BookPropertiesDialog dlg = new BookPropertiesDialog(properties);
        try {
            if (dlg.show() == false) {
                return;
            }
        } catch (Exception ex) {
            Logger.getLogger(WebPublish.class.getName()).log(Level.SEVERE, null, ex);
            throw new CommandErrorException(CommandError.ERROR_BOOK_PROPERTIES_DIALOG_CREATION);
        }
        properties.write();

        final URL url = Util.showFileSaveDialog(getSaveAsFileName(".epub"), "EPUB", "*.epub");
        if (url == null) {
            return;
        }

        CommandProgressDialog.start(new Command() {
            public void run(CommandProgress progress) throws CommandErrorException {
                try {
                    progress.status("Starting ...");
                    Book book = new Book(properties);
                    book.loadDocument(xTextDocument, progress, profile);
                    book.save(url, progress);
                    progress.status("Congratulations, EPUB has been saved.");
                } catch (Exception ex) {
                    throw new CommandErrorException(CommandError.ERROR_GENERATE_EPUB);
                } catch (IOException ex) {
                    throw new CommandErrorException(CommandError.ERROR_GENERATE_EPUB);
                }
            }
        });
    }
}
