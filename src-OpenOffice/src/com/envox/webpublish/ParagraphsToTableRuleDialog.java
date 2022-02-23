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
public class ParagraphsToTableRuleDialog
        extends StandardDialog
        implements XActionListener {

    ParagraphsToTableRule m_rule;
    List<ParagraphsToTableRule> m_rules;

    ParagraphsToTableRuleDialog(ParagraphsToTableRule rule, List<ParagraphsToTableRule> rules) {
        m_rule = rule;
        m_rules = rules;
    }

    public boolean show() throws Exception {
        return show("ParagraphsToTableRule");
    }

    protected void initControls() {
        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        m_controlContainer.getTextComponent("txtParagraphs").setText(m_rule.getParagraphs());
        m_controlContainer.getTextComponent("txtTableClass").setText(m_rule.getTableClass());
        m_controlContainer.getTextComponent("txtTableCSS").setText(m_rule.getTableCSS());
    }

    protected void updateControls() {
    }

    protected void fromControls() {
        m_rule.setParagraphs(m_controlContainer.getTextComponent("txtParagraphs").getText());
        m_rule.setTableClass(m_controlContainer.getTextComponent("txtTableClass").getText());
        m_rule.setTableCSS(m_controlContainer.getTextComponent("txtTableCSS").getText());
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            String paragraphs = m_controlContainer.getTextComponent("txtParagraphs").getText();
            if (StringUtils.isBlank(paragraphs)) {
                Util.showMessageBox("WebPublish", "Enter paragraphs.");
                m_controlContainer.getWindow("txtParagraphs").setFocus();
                return;
            }
            
            String[] paragraphStyles = StringUtils.stripAll(StringUtils.split(paragraphs, ","));
            for (String paragraphStyle:paragraphStyles) {
                for (ParagraphsToTableRule rule:m_rules) {
                    if (rule != m_rule) {
                        if (ArrayUtils.contains(StringUtils.stripAll(StringUtils.split(rule.getParagraphs(), ",")), paragraphStyle)) {
                            Util.showMessageBox("WebPublish", String.format("Rule for the paragraph style '%s' is alredy defined.", paragraphStyle));
                            m_controlContainer.getWindow("txtParagraphs").setFocus();
                            return;
                        }
                    }
                }

            }

            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }
}
