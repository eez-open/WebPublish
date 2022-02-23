/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.envox.webpublish;

import com.envox.webpublish.GUIHelper.StandardDialog;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;

/**
 *
 * @author martin
 */
public class ProfileDialog
        extends StandardDialog
        implements XActionListener, XItemListener {

    Profile m_profile;
    List<Profile> m_profiles;
    private static String[] PageBreakControlNames = {
        "lblPageBreakFreq",
        "txtPageBreakFreq",
        "lblPageBreakHTML",
        "txtPageBreakHTML"
    };
    List<ParagraphsToTableRule> m_paragraphsToTableRules;
    List<ClassToTagRule> m_classToTagRules;
    String m_cssSelector;
    String m_customCSS;
    String m_cssImagesFolder;
    String m_cssFontsFolder;
    List<FontAlternative> m_fontAlternatives;

    ProfileDialog(Profile profile, List<Profile> profiles) {
        m_profile = profile;
        m_profiles = profiles;
    }

    public boolean show() throws Exception {
        createDialog(m_profile.isPublishProfile() ? "PublishProfile" : "ProfileDialog");

        startLoadBlogsThread();

        m_dialogResult = 0;
        m_xDialog.execute();
        if (m_dialogResult == 1) {
            fromControls();
            return true;
        }

        return false;
    }

    protected void initControls() {
        m_controlContainer.getButton("chkPageBreakEnabled").setActionCommand("pageBreakEnabled");
        m_controlContainer.getButton("chkPageBreakEnabled").addActionListener(this);

        if (m_profile.isPublishProfile()) {
            m_controlContainer.getButton("btnTest").setActionCommand("btnTest");
            m_controlContainer.getButton("btnTest").addActionListener(this);
        }

        m_controlContainer.getButton("btnParagraphsToTable").setActionCommand("btnParagraphsToTable");
        m_controlContainer.getButton("btnParagraphsToTable").addActionListener(this);

        m_controlContainer.getButton("btnHTMLOptions").setActionCommand("btnHTMLOptions");
        m_controlContainer.getButton("btnHTMLOptions").addActionListener(this);

        m_controlContainer.getButton("btnCSSOptions").setActionCommand("btnCSSOptions");
        m_controlContainer.getButton("btnCSSOptions").addActionListener(this);

        m_controlContainer.getButton("btnFontAlternatives").setActionCommand("btnFontAlternatives");
        m_controlContainer.getButton("btnFontAlternatives").addActionListener(this);

        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        m_controlContainer.getTextComponent("txtProfileName").setText(m_profile.getProfileName());
        if (m_profile.isPublishProfile()) {
            m_controlContainer.getTextComponent("txtBlogAddress").setText(m_profile.getBlogAddress());
            m_controlContainer.getTextComponent("txtBlogUsername").setText(m_profile.getBlogUsername());
            m_controlContainer.getTextComponent("txtBlogPassword").setText(m_profile.getBlogPassword());
            m_controlContainer.getCheckBox("chkShortenWebLinks").setState(m_profile.getShortenWebLinks() ? (short) 1 : (short) 0);
        }
        m_controlContainer.getTextComponent("txtDocumentTitleStyle").setText(m_profile.getDocumentTitleStyle());
        m_controlContainer.getTextComponent("txtHtmlCodeStyle").setText(m_profile.getHtmlCodeStyle());
        m_controlContainer.getTextComponent("txtScaling").setText(Integer.toString(m_profile.getScaling()));
        m_controlContainer.getCheckBox("chkConvertToPixels").setState(m_profile.getConvertToPixels() ? (short) 1 : (short) 0);
        m_controlContainer.getCheckBox("chkPageBreakEnabled").setState(m_profile.getPageBreakEnabled() ? (short) 1 : (short) 0);
        m_controlContainer.getTextComponent("txtPageBreakFreq").setText(Integer.toString(m_profile.getPageBreakFreq()));
        m_controlContainer.getTextComponent("txtPageBreakHTML").setText(m_profile.getPageBreakHTML());
        m_paragraphsToTableRules = ParagraphsToTableRule.clone(m_profile.getParagraphsToTableRules());
        m_classToTagRules = ClassToTagRule.clone(m_profile.getClassToTagRules());
        m_cssSelector = m_profile.getCSSSelector();
        m_customCSS = m_profile.getCustomCSS();
        m_cssImagesFolder = m_profile.getCSSImagesFolder();
        m_cssFontsFolder = m_profile.getCSSFontsFolder();
        m_fontAlternatives = FontAlternative.clone(m_profile.getFontAlternatives());
        m_controlContainer.getTextComponent("txtHtmlImagesOutputFolder").setText(m_profile.getHTMLImagesOutputFolder());
    }

    protected void updateControls() {
        m_controlContainer.enableControls(PageBreakControlNames,
                m_controlContainer.getCheckBox("chkPageBreakEnabled").getState() == 1);
    }

    protected void fromControls() {
        m_profile.setProfileName(m_controlContainer.getTextComponent("txtProfileName").getText());
        if (m_profile.isPublishProfile()) {
            m_profile.setBlogAddress(m_controlContainer.getTextComponent("txtBlogAddress").getText());
            m_profile.setBlogUsername(m_controlContainer.getTextComponent("txtBlogUsername").getText());
            m_profile.setBlogPassword(m_controlContainer.getTextComponent("txtBlogPassword").getText());
            m_profile.setBlogID(getBlogID());
            m_profile.setShortenWebLinks(m_controlContainer.getCheckBox("chkShortenWebLinks").getState() == 1 ? true : false);
        }
        m_profile.setDocumentTitleStyle(m_controlContainer.getTextComponent("txtDocumentTitleStyle").getText());
        m_profile.setHtmlCodeStyle(m_controlContainer.getTextComponent("txtHtmlCodeStyle").getText());
        m_profile.setScaling(Integer.parseInt(m_controlContainer.getTextComponent("txtScaling").getText()));
        m_profile.setConvertToPixels(m_controlContainer.getCheckBox("chkConvertToPixels").getState() == 1 ? true : false);
        m_profile.setPageBreakEnabled(m_controlContainer.getCheckBox("chkPageBreakEnabled").getState() == 1 ? true : false);
        m_profile.setPageBreakFreq(Integer.parseInt(m_controlContainer.getTextComponent("txtPageBreakFreq").getText()));
        m_profile.setPageBreakHTML(m_controlContainer.getTextComponent("txtPageBreakHTML").getText());
        m_profile.setParagraphsToTableRules(m_paragraphsToTableRules);
        m_profile.setClassToTagRules(m_classToTagRules);
        m_profile.setCSSSelector(m_cssSelector);
        m_profile.setCustomCSS(m_customCSS);
        m_profile.setCSSImagesFolder(m_cssImagesFolder);
        m_profile.setCSSFontsFolder(m_cssFontsFolder);
        m_profile.setFontAlternatives(m_fontAlternatives);
        XTextComponent xTextComponent = m_controlContainer.getTextComponent("txtHtmlImagesOutputFolder");
        if (xTextComponent != null) {
            m_profile.setHTMLImagesOutputFolder(xTextComponent.getText());
        } else {
            m_profile.setHTMLImagesOutputFolder("");
        }
    }

    MoveableTypeAPIClient m_client;
    Object[] m_blogs;

    private String getBlogID() {
        if (m_blogs != null) {
            XListBox xBlogsListBox = m_controlContainer.getListBox("lbBlogs");
            String selectedBlogName = xBlogsListBox.getSelectedItem();
            for (Object oBlog:m_blogs) {
                HashMap blog = (HashMap)oBlog;
                if (StringUtils.equals((String)blog.get("blogName"), selectedBlogName)) {
                    return blog.get("blogid").toString();
                }
            }
        }
        return null;
    }

    private void startLoadBlogsThread() {
        if (!m_profile.isPublishProfile()) {
            initControls();
            toControls();
            updateControls();
            return;
        }

        m_controlContainer.enableControls(false);

        Rectangle rect = m_controlContainer.getWindow("btnTest").getPosSize();
        insertThrobber((rect.X + rect.Width) * 100 / rect.Width + 4, rect.Y * 14 / rect.Height + 2, 10, 10);
        setThrobberActive(true);

        Thread thread = new Thread(new RunnableWithCallback() {
            public void run() {
                m_blogs = null;
                try {
                    String strBlogAddress = m_profile.getBlogAddress();
                    String strBlogUsername = m_profile.getBlogUsername();
                    String strBlogPassword = m_profile.getBlogPassword();
                    if (StringUtils.isNotBlank(strBlogAddress) && StringUtils.isNotBlank(strBlogUsername) && StringUtils.isNotBlank(strBlogPassword)) {
                        m_client = new MoveableTypeAPIClient(strBlogAddress, strBlogUsername, strBlogPassword, null);
                        m_blogs = (Object[])m_client.getBlogs();
                    }
                } catch (XmlRpcException ex) {
                } catch (MalformedURLException ex) {
                }
                Util.mainThreadNotify(this, null);
            }

            public void notify(Object arg0) {
                setThrobberActive(false);
                m_controlContainer.enableControls(true);

                if (m_blogs != null) {
                    XListBox xBlogsListBox = m_controlContainer.getListBox("lbBlogs");

                    String selectedBlogName = "";

                    xBlogsListBox.removeItems((short)0, xBlogsListBox.getItemCount());
                    for (Object oBlog:m_blogs) {
                        HashMap blog = (HashMap)oBlog;
                        xBlogsListBox.addItem((String)blog.get("blogName"), xBlogsListBox.getItemCount());
                        if (StringUtils.equals(blog.get("blogid").toString(), m_profile.getBlogID())) {
                            selectedBlogName = (String)blog.get("blogName");
                        }
                    }

                    xBlogsListBox.selectItem(selectedBlogName, true);

                    if (xBlogsListBox.getSelectedItemPos() == -1) {
                        xBlogsListBox.selectItemPos((short)0, true);
                    }
                }

                initControls();
                toControls();
                updateControls();
            }
        });

        thread.start();
    }

    private void test(String strBlogAddress, String strBlogUsername, String strBlogPassword) {
        try {
            m_client = new MoveableTypeAPIClient(strBlogAddress, strBlogUsername, strBlogPassword, null);

            m_controlContainer.enableControls(false);

            Rectangle rect = m_controlContainer.getWindow("btnTest").getPosSize();
            insertThrobber((rect.X + rect.Width) * 100 / rect.Width + 4, rect.Y * 14 / rect.Height + 2, 10, 10);
            setThrobberActive(true);

            Thread thread = new Thread(new RunnableWithCallback() {
                XmlRpcException m_ex;

                public void run() {
                    try {
                        m_blogs = (Object[])m_client.getBlogs();
                    } catch (XmlRpcException ex) {
                        m_ex = ex;
                    }
                    Util.mainThreadNotify(this, null);
                }

                public void notify(Object arg0) {
                    setThrobberActive(false);
                    m_controlContainer.enableControls(true);

                    if (m_ex == null) {
                        XListBox xBlogsListBox = m_controlContainer.getListBox("lbBlogs");

                        String selectedBlogName = xBlogsListBox.getSelectedItem();

                        xBlogsListBox.removeItems((short)0, xBlogsListBox.getItemCount());
                        for (Object oBlog:m_blogs) {
                            HashMap blog = (HashMap)oBlog;
                            xBlogsListBox.addItem((String)blog.get("blogName"), xBlogsListBox.getItemCount());
                        }

                        xBlogsListBox.selectItem(selectedBlogName, true);

                        if (xBlogsListBox.getSelectedItemPos() == -1) {
                            xBlogsListBox.selectItemPos((short)0, true);
                        }
                    } else {
                        Util.showMessageBox("WebPublish", String.format("XML-RPC error:\n%s", m_ex.getMessage()));
                        return;
                    }
                }
            });

            thread.start();
        } catch (MalformedURLException ex) {
            Util.showMessageBox("WebPublish", "Malformed blog URL.");
        } catch (XmlRpcException ex) {
            Util.showMessageBox("WebPublish", String.format("XML-RPC error:\n%s", ex.getMessage()));
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("pageBreakEnabled")) {
            updateControls();
        } else if(arg0.ActionCommand.equals("btnTest")) {
            if (m_profile.isPublishProfile()) {
                String strBlogAddress = m_controlContainer.getTextComponent("txtBlogAddress").getText();
                if (StringUtils.isBlank(strBlogAddress)) {
                    Util.showMessageBox("WebPublish", "Enter blog address.");
                    return;
                }

                String strBlogUsername = m_controlContainer.getTextComponent("txtBlogUsername").getText();
                if (StringUtils.isBlank(strBlogUsername)) {
                    Util.showMessageBox("WebPublish", "Enter user name.");
                    return;
                }

                String strBlogPassword = m_controlContainer.getTextComponent("txtBlogPassword").getText();
                if (StringUtils.isBlank(strBlogPassword)) {
                    Util.showMessageBox("WebPublish", "Enter password.");
                    return;
                }

                test(strBlogAddress, strBlogUsername, strBlogPassword);
            }
        } else if (arg0.ActionCommand.equals("btnParagraphsToTable")) {
            ParagraphsToTableDialog dlg = new ParagraphsToTableDialog(ParagraphsToTableRule.clone(m_paragraphsToTableRules));
            try {
                if (dlg.show()) {
                    m_paragraphsToTableRules = dlg.getRules();
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (arg0.ActionCommand.equals("btnHTMLOptions")) {
            HTMLOptionsDialog dlg = new HTMLOptionsDialog(ClassToTagRule.clone(m_classToTagRules));
            try {
                if (dlg.show()) {
                    m_classToTagRules = dlg.getRules();
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (arg0.ActionCommand.equals("btnCSSOptions")) {
            CSSOptionsDialog dlg = new CSSOptionsDialog(m_cssSelector, m_customCSS, m_cssImagesFolder, m_cssFontsFolder);
            try {
                if (dlg.show()) {
                    m_cssSelector = dlg.getCSSSelector();
                    m_customCSS = dlg.getCustomCSS();
                    m_cssImagesFolder = dlg.getCSSImagesFolder();
                    m_cssFontsFolder = dlg.getCSSFontsFolder();
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (arg0.ActionCommand.equals("btnFontAlternatives")) {
            FontAlternativesDialog dlg = new FontAlternativesDialog(FontAlternative.clone(m_fontAlternatives));
            try {
                if (dlg.show()) {
                    m_fontAlternatives = dlg.getFontAlternatives();
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (arg0.ActionCommand.equals("OK")) {
            String profileName = m_controlContainer.getTextComponent("txtProfileName").getText();
            if (StringUtils.isBlank(profileName)) {
                Util.showMessageBox("WebPublish", "Enter profile name.");
                m_controlContainer.getWindow("txtProfileName").setFocus();
                return;
            }

            for (Profile profile:m_profiles) {
                if (profile != m_profile && profile.getProfileName().equals(profileName)) {
                    Util.showMessageBox("WebPublish", "Profile with the same name already exists.");
                    m_controlContainer.getWindow("txtProfileName").setFocus();
                    return;
                }
            }

            if (m_profile.isPublishProfile()) {
                String strBlogAddress = m_controlContainer.getTextComponent("txtBlogAddress").getText();
                if (StringUtils.isBlank(strBlogAddress)) {
                    Util.showMessageBox("WebPublish", "Enter blog address.");
                    m_controlContainer.getWindow("txtBlogAddress").setFocus();
                    return;
                }

                String strBlogUsername = m_controlContainer.getTextComponent("txtBlogUsername").getText();
                if (StringUtils.isBlank(strBlogUsername)) {
                    Util.showMessageBox("WebPublish", "Enter user name.");
                    m_controlContainer.getWindow("txtBlogUsername").setFocus();
                    return;
                }

                String strBlogPassword = m_controlContainer.getTextComponent("txtBlogPassword").getText();
                if (StringUtils.isBlank(strBlogPassword)) {
                    Util.showMessageBox("WebPublish", "Enter password.");
                    m_controlContainer.getWindow("txtBlogPassword").setFocus();
                    return;
                }

                if (StringUtils.isBlank(getBlogID())) {
                    Util.showMessageBox("WebPublish", "Select blog.");
                    m_controlContainer.getWindow("lbBlogs").setFocus();
                    return;
                }
            }

            int scaling = 0;
            try {
                scaling = Integer.parseInt(m_controlContainer.getTextComponent("txtScaling").getText());
            } catch (NumberFormatException ex) {
            }
            if (scaling <= 0) {
                Util.showMessageBox("WebPublish", "Scaling should be number greater than 0.");
                m_controlContainer.getWindow("txtScaling").setFocus();
                return;
            }

            if (m_controlContainer.getCheckBox("chkPageBreakEnabled").getState() == 1) {
                int pageBreakFreq = 0;
                try {
                    pageBreakFreq = Integer.parseInt(m_controlContainer.getTextComponent("txtPageBreakFreq").getText());
                } catch (NumberFormatException ex) {
                }
                if (pageBreakFreq <= 0) {
                    Util.showMessageBox("WebPublish", "Frequency should be number greater than 0.");
                    m_controlContainer.getWindow("txtPageBreakFreq").setFocus();
                    return;
                }

                String pageBreakHTML = m_controlContainer.getTextComponent("txtPageBreakHTML").getText();
                if (StringUtils.isBlank(pageBreakHTML)) {
                    Util.showMessageBox("WebPublish", "Enter page break HTML.");
                    m_controlContainer.getWindow("txtPageBreakHTML").setFocus();
                    return;
                }
            }

            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }

    public void itemStateChanged(ItemEvent arg0) {
        updateControls();
    }
}
