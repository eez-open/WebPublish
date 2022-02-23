/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.envox.webpublish;

import com.envox.webpublish.GUIHelper.StandardDialog;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XMouseListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;

/**
 *
 * @author martin
 */
public class ProfilesDialog
        extends StandardDialog
        implements XActionListener, XItemListener, XMouseListener {

    ProfilesDialogHandler m_dialogHandler;

    ProfilesDialog() {
    }

    private ProfilesDialogHandler getDialogHandler() {
        if (m_dialogHandler == null) {
            m_dialogHandler = new ProfilesDialogHandler();
            m_dialogHandler.initContainer(m_controlContainer, this, this, this);
        }
        return m_dialogHandler;
    }

    public boolean show() throws Exception {
        return show("Profiles");
    }

    protected void initControls() {
        getDialogHandler().initControls();

        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        getDialogHandler().toControls();
    }

    protected void updateControls() {
        getDialogHandler().updateControls();
    }

    protected void fromControls() {
        getDialogHandler().fromControls();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            m_dialogResult = 1;
            m_xDialog.endExecute();
        } else {
            getDialogHandler().actionPerformed(arg0);
        }
    }

    public void disposing(EventObject arg0) {
    }

    public void itemStateChanged(ItemEvent arg0) {
        getDialogHandler().itemStateChanged(arg0);
    }

    public void mousePressed(MouseEvent arg0) {
        getDialogHandler().mousePressed(arg0);
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }
}
