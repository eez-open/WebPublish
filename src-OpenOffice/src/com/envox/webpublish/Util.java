/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.envox.webpublish;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.VclWindowPeerAttribute;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XCallback;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XRequestCallback;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.task.UrlRecord;
import com.sun.star.task.XInteractionHandler;
import com.sun.star.task.XMasterPasswordHandling;
import com.sun.star.task.XPasswordContainer;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.ui.dialogs.XFilePicker;
import com.sun.star.ui.dialogs.XFilterManager;
import com.sun.star.ui.dialogs.XFolderPicker;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author martin
 */
public class Util {
    private static XFrame m_xFrame;
    private static XToolkit m_xToolkit;
    private static XComponentContext m_xContext;

    private static Profiles m_profiles;

    public static void setFrame(XFrame xFrame) {
        m_xFrame = xFrame;
    }

    public static void setToolkit(XToolkit xToolkit) {
        m_xToolkit = xToolkit;
    }

    public static XComponentContext getContext() {
        return m_xContext;
    }

    public static void setContext(XComponentContext xContext) {
        m_xContext = xContext;
    }

    public static void showMessageBox(String sTitle, String sMessage) {
        try {
            if (m_xFrame != null && m_xToolkit != null) {
                // describe window properties.
                WindowDescriptor aDescriptor = new WindowDescriptor();
                aDescriptor.Type = WindowClass.MODALTOP;
                aDescriptor.WindowServiceName = "infobox";
                aDescriptor.ParentIndex = -1;
                aDescriptor.Parent = (XWindowPeer) UnoRuntime.queryInterface(XWindowPeer.class, m_xFrame.getContainerWindow());
                aDescriptor.Bounds = new Rectangle(0, 0, 300, 200);
                aDescriptor.WindowAttributes = WindowAttribute.BORDER | WindowAttribute.MOVEABLE | WindowAttribute.CLOSEABLE;

                XWindowPeer xPeer = m_xToolkit.createWindow(aDescriptor);
                if (xPeer != null) {
                    XMessageBox xMsgBox = (XMessageBox) UnoRuntime.queryInterface(XMessageBox.class, xPeer);
                    if (null != xMsgBox) {
                        xMsgBox.setCaptionText(sTitle);
                        xMsgBox.setMessageText(sMessage);
                        xMsgBox.execute();
                    }
                }
            }
        } catch (com.sun.star.uno.Exception e) {
            // do your error handling
        }
    }

    public static void showErrorMessageBox(String sTitle, String sMessage) {
        try {
            if (m_xFrame != null && m_xToolkit != null) {
                // describe window properties.
                WindowDescriptor aDescriptor = new WindowDescriptor();
                aDescriptor.Type = WindowClass.MODALTOP;
                aDescriptor.WindowServiceName = "errorbox";
                aDescriptor.ParentIndex = -1;
                aDescriptor.Parent = (XWindowPeer) UnoRuntime.queryInterface(XWindowPeer.class, m_xFrame.getContainerWindow());
                aDescriptor.Bounds = new Rectangle(0, 0, 300, 200);
                aDescriptor.WindowAttributes = WindowAttribute.BORDER | WindowAttribute.MOVEABLE | WindowAttribute.CLOSEABLE | VclWindowPeerAttribute.OK;

                XWindowPeer xPeer = m_xToolkit.createWindow(aDescriptor);
                if (xPeer != null) {
                    XMessageBox xMsgBox = (XMessageBox) UnoRuntime.queryInterface(XMessageBox.class, xPeer);
                    if (null != xMsgBox) {
                        xMsgBox.setCaptionText(sTitle);
                        xMsgBox.setMessageText(sMessage);
                        xMsgBox.execute();
                    }
                }
            }
        } catch (com.sun.star.uno.Exception e) {
            // do your error handling
        }
    }

    public static URL showFileOpenDialog(String title, String filter) {
        try {
            Object oFilePicker = m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.ui.dialogs.FilePicker", m_xContext);

            XInitialization xInitialization = (XInitialization) UnoRuntime.queryInterface(XInitialization.class, oFilePicker);
            Object[] obj = new Object[1];
            obj[0] = Short.valueOf(TemplateDescription.FILEOPEN_SIMPLE);
            xInitialization.initialize(obj);

            XFilterManager xFilterManager = (XFilterManager) UnoRuntime.queryInterface(XFilterManager.class, oFilePicker);
            xFilterManager.appendFilter(title, filter);

            XFilePicker xFilePicker = (XFilePicker) UnoRuntime.queryInterface(XFilePicker.class, oFilePicker);
            xFilePicker.setMultiSelectionMode(false);

            if (xFilePicker.execute() == 1) {
                // convert URL to result file name
                return new URL(xFilePicker.getFiles()[0]);
            }

            return null;
        } catch (MalformedURLException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static URL showFileSaveDialog(String fileName, String title, String filter) {
        try {
            Object oFilePicker = m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.ui.dialogs.FilePicker", m_xContext);

            XInitialization xInitialization = (XInitialization) UnoRuntime.queryInterface(XInitialization.class, oFilePicker);
            Object[] obj = new Object[1];
            obj[0] = Short.valueOf(TemplateDescription.FILESAVE_SIMPLE);
            xInitialization.initialize(obj);

            XFilterManager xFilterManager = (XFilterManager) UnoRuntime.queryInterface(XFilterManager.class, oFilePicker);
            xFilterManager.appendFilter(title, filter);

            XFilePicker xFilePicker = (XFilePicker) UnoRuntime.queryInterface(XFilePicker.class, oFilePicker);
            xFilePicker.setMultiSelectionMode(false);
            if (StringUtils.isNotBlank(fileName)) {
                String name = FilenameUtils.getName(fileName);
                xFilePicker.setDefaultName(name);

                // TODO: vidjeti zašto ovo ne radi
                /* 
                String dir = FilenameUtils.getFullPath(fileName);
                File file = new File(dir);
                URI uri = file.toURI();
                String dirName = uri.toString();
                Util.showMessageBox("WebPublish", dirName);
                try {
                    xFilePicker.setDisplayDirectory(dirName);
                } catch (Exception ex) {
                    Util.showMessageBox("WebPublish", ex.getMessage());
                }
                */
            }

            if (xFilePicker.execute() == 1) {
                // convert URL to result file name
                String result = xFilePicker.getFiles()[0];
                String[] extensions = StringUtils.split(filter, ";");
                for (String extension:extensions) {
                    if (result.endsWith(extension.substring(1))) {
                        return new URL(result);
                    }
                }
                return new URL(result + extensions[0].substring(1));
            }

            return null;
        } catch (MalformedURLException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String showFolderPickerDialog(String displayDirectory, String title) {
        String returnFolder = "";
        XComponent xComponent = null;
        try {
            // instantiate the folder picker and retrieve the necessary interfaces...
            Object oFolderPicker = m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.ui.dialogs.FolderPicker", m_xContext);
            XFolderPicker xFolderPicker = (XFolderPicker) UnoRuntime.queryInterface(XFolderPicker.class, oFolderPicker);
            XExecutableDialog xExecutable = (XExecutableDialog) UnoRuntime.queryInterface(XExecutableDialog.class, oFolderPicker);
            xComponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, oFolderPicker);

            xFolderPicker.setDisplayDirectory(displayDirectory);

            xFolderPicker.setTitle(title);

            short nResult = xExecutable.execute();
            if (nResult == com.sun.star.ui.dialogs.ExecutableDialogResults.OK){
                returnFolder = xFolderPicker.getDirectory();
            }
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            if (xComponent != null){
                xComponent.dispose();
            }
        }
        return returnFolder;
    }

    public static String pathCombine(String path1, String path2)
    {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }

    public static String urlCombine(String url1, String url2)
    {
        url1 = StringUtils.removeEnd(url1, "/");
        url2 = StringUtils.removeStart(url2, "/");
        return url1 + "/" + url2;
    }

    public static String getStringConf(String pkg, String group, String name, String defaultValue) {
        try {
            XNameAccess accessLeaves = ConfigurationAccess.createUpdateAccess(m_xContext,
                String.format("/com.envox.webpublish.%s/Leaves", pkg));
            XPropertySet xLeaf = (XPropertySet) UnoRuntime.queryInterface(
                XPropertySet.class, accessLeaves.getByName(group));
            return (String) xLeaf.getPropertyValue(name);
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }

        return defaultValue;
    }

    public static void setStringConf(String pkg, String group, String name, String value) {
        try {
            XNameAccess accessLeaves = ConfigurationAccess.createUpdateAccess(m_xContext,
                String.format("/com.envox.webpublish.%s/Leaves", pkg));
            XPropertySet xLeaf = (XPropertySet) UnoRuntime.queryInterface(
                XPropertySet.class, accessLeaves.getByName(group));
            xLeaf.setPropertyValue(name, value);
            XChangesBatch xUpdateCommit = (XChangesBatch) UnoRuntime.queryInterface(
                XChangesBatch.class, accessLeaves);
            xUpdateCommit.commitChanges();
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static XDialog createDialog(String dialogName)
            throws NoSuchElementException, WrappedTargetException, Exception {
            XNameAccess xNameAccess = (XNameAccess) UnoRuntime.queryInterface(
                XNameAccess.class, m_xContext);
            Object oPIP = xNameAccess.getByName(
                    "/singletons/com.sun.star.deployment.PackageInformationProvider");
            XPackageInformationProvider xPIP = (XPackageInformationProvider)
                    UnoRuntime.queryInterface(
                    XPackageInformationProvider.class, oPIP);

            // get the url of the directory extension installed
            String sPackageURL = xPIP.getPackageLocation("com.envox.webpublish.WebPublish");
            String sDialogURL = sPackageURL + "/dialogs/" + dialogName + ".xdl";

            // dialog provider to make a dialog
            Object oDialogProvider = m_xContext.getServiceManager().createInstanceWithContext(
                    "com.sun.star.awt.DialogProvider2", m_xContext);
            XDialogProvider2 xDialogProvider = (XDialogProvider2) UnoRuntime.queryInterface(
                XDialogProvider2.class, oDialogProvider);

            return xDialogProvider.createDialog(sDialogURL);
    }

    public static void dispose(Object oObject) {
        if (oObject != null) {
            XComponent xComp = (XComponent) UnoRuntime.queryInterface(XComponent.class, oObject);
            if (xComp != null)
                xComp.dispose();
        }
    }

    public static void mainThreadNotify(XCallback callback, Object arg) {
        try {
            XRequestCallback xRequest = (XRequestCallback) UnoRuntime.queryInterface(
                XRequestCallback.class, m_xContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.awt.AsyncCallback", m_xContext));
            xRequest.addCallback(callback, arg);
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static boolean urlExists(String url) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException ex) {
        }
        return false;
    }

    public static String getMediaType(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        if (extension.equals("jpg") || extension.equals("jpeg")) {
            return "image/jpeg";
        } else if (extension.equals("png")) {
            return "image/png";
        } else if (extension.equals("gif")) {
            return "image/gif";
        } else if (extension.equals("tif")) {
            return "image/tiff";
        } else if (extension.equals("ttf") || extension.equals("ttc")) {
            return "application/x-font-ttf";
        } else if (extension.equals("otf")) {
            return "application/vnd.ms-opentype";
        } else if (extension.equals("eot")) {
            return "application/vnd.ms-fontobject";
        } else if (extension.equals("woff")) {
            return "application/x-font-woff";
        }
        return "";
    }

    public static Profiles getProfiles() {
        if (m_profiles == null) {
            m_profiles = new Profiles(m_xContext);
        }
        return m_profiles;
    }

    public static String getTagValue(String tagName, Element element){
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0)
            return "";
        nodeList = nodeList.item(0).getChildNodes();
        if (nodeList.getLength() == 0)
            return "";
        Node value = (Node) nodeList.item(0);
        return value.getNodeValue();
    }

    public static void setTagValue(String tagName, String value, Element parentElement) {
        Element childElement = parentElement.getOwnerDocument().createElement(tagName);
        childElement.appendChild(parentElement.getOwnerDocument().createTextNode(value));
        parentElement.appendChild(childElement);
    }

    public static File toFile(URL url) {
        String fileName = url.getFile();
        if (!StringUtils.isBlank(url.getHost()) && !url.getHost().equals("localhost")) {
            fileName = "\\\\" + url.getHost() + fileName;
        }
        try {
            return new File(URLDecoder.decode(fileName, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }


    public static String mapToString(HashMap<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String key : map.keySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            String value = map.get(key);
            try {
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return stringBuilder.toString();
    }

    public static HashMap<String, String> stringToMap(String input) {
        HashMap<String, String> map = new HashMap<String, String>();

        if (!StringUtils.isBlank(input)) {
            String[] nameValuePairs = input.split("&");
            for (String nameValuePair : nameValuePairs) {
                String[] nameValue = nameValuePair.split("=");
                try {
                    map.put(URLDecoder.decode(nameValue[0], "UTF-8"), nameValue.length > 1 ? URLDecoder.decode(
                            nameValue[1], "UTF-8") : "");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("This method requires UTF-8 encoding support", e);
                }
            }
        }

        return map;
    }

    private static XPasswordContainer m_xPasswordContainer;
    private static XInteractionHandler m_xInteractionHandler;

    synchronized private static boolean initPasswordContainer() {
        if (m_xPasswordContainer == null) {
            try {
               XMultiComponentFactory xFactory = m_xContext.getServiceManager();
                m_xPasswordContainer = (XPasswordContainer) UnoRuntime.queryInterface(
                                            XPasswordContainer.class,
                                            xFactory.createInstanceWithContext( "com.sun.star.task.PasswordContainer", m_xContext));
                m_xInteractionHandler = (XInteractionHandler)UnoRuntime.queryInterface(
                                        XInteractionHandler.class,
                                        xFactory.createInstanceWithContext( "com.sun.star.task.InteractionHandler", m_xContext));
            } catch (Exception ex) {
                Logger.getLogger(WebPublish.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }

        XMasterPasswordHandling xMasterHdl = (XMasterPasswordHandling)UnoRuntime.queryInterface(XMasterPasswordHandling.class, m_xPasswordContainer);
        if (!xMasterHdl.isPersistentStoringAllowed()) {
            xMasterHdl.allowPersistentStoring(true);
        }

        return true;
    }

    synchronized public static boolean resetPasswordContainer() {
        m_xPasswordContainer = null;
        return true;
    }

    public static String getPassword(String profileName) {
        if (!initPasswordContainer()) {
            return null;
        }

        UrlRecord urlRecord = m_xPasswordContainer.findForName("http://www.envox.hr/webpublish/profiles", profileName, m_xInteractionHandler);
        if (urlRecord == null || urlRecord.UserList == null || urlRecord.UserList.length == 0 || !urlRecord.UserList[0].UserName.equals(profileName) || urlRecord.UserList[0].Passwords.length == 0) {
            resetPasswordContainer();
            return null;
        }

        return urlRecord.UserList[0].Passwords[0];
    }

    public static void storePassword(String profileName, String password) {
        if (!initPasswordContainer()) {
            return;
        }

        m_xPasswordContainer.addPersistent("http://www.envox.hr/webpublish/profiles", profileName, new String[] { password }, m_xInteractionHandler);
    }
}
