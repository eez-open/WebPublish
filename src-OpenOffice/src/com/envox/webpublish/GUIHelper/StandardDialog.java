/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.GUIHelper;

import com.sun.star.uno.Exception;

/**
 *
 * @author martin
 */
public abstract class StandardDialog extends Dialog {
    protected int m_dialogResult;

    protected boolean show(String dialogName) throws Exception {
        createDialog(dialogName);

        initControls();
        toControls();
        updateControls();

        m_dialogResult = 0;
        m_xDialog.execute();
        if (m_dialogResult == 1) {
            fromControls();
            return true;
        }

        return false;
    }

    abstract protected void initControls();
    abstract protected void toControls();
    abstract protected void updateControls();
    abstract protected void fromControls();
}
