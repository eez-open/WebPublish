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

/**
 *
 * @author martin
 */
public class SelectProfileTypeDialog
        extends StandardDialog
        implements XActionListener {

    Profile.Type m_profileType = Profile.Type.HTML;

    public SelectProfileTypeDialog() {
    }

    Profile.Type getType() { return m_profileType; }

    public boolean show() throws Exception {
        return show("SelectProfileType");
    }

    protected void initControls() {
        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        if (m_profileType == Profile.Type.HTML) {
            m_controlContainer.getRadioButton("rbHTML").setState(true);
        } else if (m_profileType == Profile.Type.EPUB) {
            m_controlContainer.getRadioButton("rbEPUB").setState(true);
        } else if (m_profileType == Profile.Type.Joomla) {
            m_controlContainer.getRadioButton("rbJoomla").setState(true);
        } else if (m_profileType == Profile.Type.WordPress) {
            m_controlContainer.getRadioButton("rbWordPress").setState(true);
        }
    }

    protected void updateControls() {
    }

    protected void fromControls() {
        if (m_controlContainer.getRadioButton("rbHTML").getState()) {
            m_profileType = Profile.Type.HTML;
        } else if (m_controlContainer.getRadioButton("rbEPUB").getState()) {
            m_profileType = Profile.Type.EPUB;
        } else if (m_controlContainer.getRadioButton("rbJoomla").getState()) {
            m_profileType = Profile.Type.Joomla;
        } else if (m_controlContainer.getRadioButton("rbWordPress").getState()) {
            m_profileType = Profile.Type.WordPress;
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }
}
