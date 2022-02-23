/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.EPUB;

import com.envox.webpublish.GUIHelper.StandardDialog;
import com.envox.webpublish.Util;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class BookPropertiesDialog
        extends StandardDialog
        implements XActionListener {

    BookProperties m_options;

    public BookPropertiesDialog(BookProperties options) {
        m_options = options;
    }

    public boolean show() throws Exception {
        return show("EPUBProperties");
    }

    protected void initControls() {
        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        m_controlContainer.getTextComponent("txtTitle").setText(m_options.getTitle());
        m_controlContainer.getTextComponent("txtAuthor").setText(m_options.getAuthor());
        m_controlContainer.getTextComponent("txtISBN").setText(m_options.getISBN());
        m_controlContainer.getTextComponent("txtPublisher").setText(m_options.getPublisher());
        m_controlContainer.getTextComponent("txtSubject").setText(m_options.getSubject());
        m_controlContainer.getTextComponent("txtDescription").setText(m_options.getDescription());
    }

    protected void updateControls() {
    }

    protected void fromControls() {
        m_options.setTitle(m_controlContainer.getTextComponent("txtTitle").getText());
        m_options.setAuthor(m_controlContainer.getTextComponent("txtAuthor").getText());
        m_options.setISBN(m_controlContainer.getTextComponent("txtISBN").getText());
        m_options.setPublisher(m_controlContainer.getTextComponent("txtPublisher").getText());
        m_options.setSubject(m_controlContainer.getTextComponent("txtSubject").getText());
        m_options.setDescription(m_controlContainer.getTextComponent("txtDescription").getText());
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            String title = m_controlContainer.getTextComponent("txtTitle").getText();
            if (StringUtils.isBlank(title)) {
                Util.showMessageBox("WebPublish", "Enter title.");
                m_controlContainer.getWindow("txtTitle").setFocus();
                return;
            }

            String author = m_controlContainer.getTextComponent("txtAuthor").getText();
            if (StringUtils.isBlank(author)) {
                Util.showMessageBox("WebPublish", "Enter author.");
                m_controlContainer.getWindow("txtAuthor").setFocus();
                return;
            }

            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }
}
