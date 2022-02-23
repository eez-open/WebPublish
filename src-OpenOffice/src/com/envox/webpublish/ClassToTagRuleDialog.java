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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class ClassToTagRuleDialog
        extends StandardDialog
        implements XActionListener {

    ClassToTagRule m_rule;
    List<ClassToTagRule> m_rules;

    ClassToTagRuleDialog(ClassToTagRule rule, List<ClassToTagRule> rules) {
        m_rule = rule;
        m_rules = rules;
    }

    public boolean show() throws Exception {
        return show("HTMLOptionsClassToTagRule");
    }

    protected void initControls() {
        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        m_controlContainer.getTextComponent("txtClass").setText(m_rule.getClassName());
        m_controlContainer.getTextComponent("txtTag").setText(m_rule.getTagName());
    }

    protected void updateControls() {
    }

    protected void fromControls() {
        m_rule.setClassName(m_controlContainer.getTextComponent("txtClass").getText());
        m_rule.setTagName(m_controlContainer.getTextComponent("txtTag").getText());
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            String className = m_controlContainer.getTextComponent("txtClass").getText();
            if (StringUtils.isBlank(className)) {
                Util.showMessageBox("WebPublish", "Enter style name.");
                m_controlContainer.getWindow("txtClass").setFocus();
                return;
            }
            
            String tagName = m_controlContainer.getTextComponent("txtTag").getText();
            if (StringUtils.isBlank(tagName)) {
                Util.showMessageBox("WebPublish", "Enter tag name.");
                m_controlContainer.getWindow("txtTag").setFocus();
                return;
            }

            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }
}
