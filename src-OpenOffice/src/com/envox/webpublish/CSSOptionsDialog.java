/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.GUIHelper.StandardDialog;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class CSSOptionsDialog
        extends StandardDialog
        implements XActionListener {

    String m_cssSelector;
    String m_customCSS;
    String m_cssImagesFolder;
    String m_cssFontsFolder;

    CSSOptionsDialog(String cssSelector, String customCSS, String cssImagesFolder, String cssFontsFolder) {
        m_cssSelector = cssSelector;
        m_customCSS = customCSS;
        m_cssImagesFolder = cssImagesFolder;
        m_cssFontsFolder = cssFontsFolder;
    }

    String getCSSSelector() { return m_cssSelector; }
    String getCustomCSS() { return m_customCSS; }
    String getCSSImagesFolder() { return m_cssImagesFolder; }
    String getCSSFontsFolder() { return m_cssFontsFolder; }

    public boolean show() throws Exception {
        return show("CSSOptions");
    }

    protected void initControls() {
        m_controlContainer.getButton("btnBrowse").setActionCommand("Browse");
        m_controlContainer.getButton("btnBrowse").addActionListener(this);
        m_controlContainer.getButton("btnBrowseFonts").setActionCommand("BrowseFonts");
        m_controlContainer.getButton("btnBrowseFonts").addActionListener(this);
        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        m_controlContainer.getTextComponent("txtCSSSelector").setText(m_cssSelector);
        m_controlContainer.getTextComponent("txtCustomCSS").setText(m_customCSS);
        m_controlContainer.getTextComponent("txtCSSImagesFolder").setText(m_cssImagesFolder);
        m_controlContainer.getTextComponent("txtCSSFontsFolder").setText(m_cssFontsFolder);
    }

    protected void updateControls() {
    }

    protected void fromControls() {
        m_cssSelector = m_controlContainer.getTextComponent("txtCSSSelector").getText();
        m_customCSS = m_controlContainer.getTextComponent("txtCustomCSS").getText();
        m_cssImagesFolder = m_controlContainer.getTextComponent("txtCSSImagesFolder").getText();
        m_cssFontsFolder = m_controlContainer.getTextComponent("txtCSSFontsFolder").getText();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("Browse")) {
            String cssImagesFolder = Util.showFolderPickerDialog(m_cssImagesFolder, "Browse for CSS images folder");
            if (StringUtils.isNotBlank(cssImagesFolder)) {
                m_cssImagesFolder = cssImagesFolder;
                m_controlContainer.getTextComponent("txtCSSImagesFolder").setText(m_cssImagesFolder);
            }
        } else if (arg0.ActionCommand.equals("BrowseFonts")) {
            String cssFontsFolder = Util.showFolderPickerDialog(m_cssFontsFolder, "Browse for fonts folder");
            if (StringUtils.isNotBlank(cssFontsFolder)) {
                m_cssFontsFolder = cssFontsFolder;
                m_controlContainer.getTextComponent("txtCSSFontsFolder").setText(m_cssFontsFolder);
            }
        } else if (arg0.ActionCommand.equals("OK")) {
            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }
}
