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
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class FontAlternativeDialog
        extends StandardDialog
        implements XActionListener {

    FontAlternative m_alternative;
    List<FontAlternative> m_alternatives;

    FontAlternativeDialog(FontAlternative alternative, List<FontAlternative> alternatives) {
        m_alternative = alternative;
        m_alternatives = alternatives;
    }

    public boolean show() throws Exception {
        return show("FontAlternative");
    }

    protected void initControls() {
        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        m_controlContainer.getTextComponent("txtFont").setText(m_alternative.getFont());
        m_controlContainer.getTextComponent("txtAlternative").setText(m_alternative.getAlternative());
    }

    protected void updateControls() {
    }

    protected void fromControls() {
        m_alternative.setFont(m_controlContainer.getTextComponent("txtFont").getText());
        m_alternative.setAlternative(m_controlContainer.getTextComponent("txtAlternative").getText());
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            String font = m_controlContainer.getTextComponent("txtFont").getText();
            if (StringUtils.isBlank(font)) {
                Util.showMessageBox("WebPublish", "Enter font.");
                m_controlContainer.getWindow("txtFont").setFocus();
                return;
            }

            for (FontAlternative alternative:m_alternatives) {
                if (alternative != m_alternative && StringUtils.equalsIgnoreCase(alternative.getFont(), m_controlContainer.getTextComponent("txtFont").getText())) {
                    Util.showMessageBox("WebPublish", "Alternative for the font already defined.");
                    m_controlContainer.getWindow("txtFont").setFocus();
                    return;
                }
            }

            String alterative = m_controlContainer.getTextComponent("txtAlternative").getText();
            if (StringUtils.isBlank(alterative)) {
                Util.showMessageBox("WebPublish", "Enter alternative.");
                m_controlContainer.getWindow("txtAlternative").setFocus();
                return;
            }

            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }
}
