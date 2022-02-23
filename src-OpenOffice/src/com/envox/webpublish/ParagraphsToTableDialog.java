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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class ParagraphsToTableDialog
        extends StandardDialog
        implements XActionListener, XItemListener, XMouseListener {

    List<ParagraphsToTableRule> m_rules;

    ParagraphsToTableDialog(List<ParagraphsToTableRule> rules) {
        m_rules = rules;
    }

    List<ParagraphsToTableRule> getRules() { return m_rules; }

    public boolean show() throws Exception {
        return show("ParagraphsToTable");
    }

    protected void initControls() {
        m_controlContainer.getListBox("lbParagraphsToTables").addItemListener(this);
        m_controlContainer.getWindow("lbParagraphsToTables").addMouseListener(this);

        m_controlContainer.getButton("btnAdd").setActionCommand("btnAdd");
        m_controlContainer.getButton("btnAdd").addActionListener(this);

        m_controlContainer.getButton("btnEdit").setActionCommand("btnEdit");
        m_controlContainer.getButton("btnEdit").addActionListener(this);

        m_controlContainer.getButton("btnDelete").setActionCommand("btnDelete");
        m_controlContainer.getButton("btnDelete").addActionListener(this);

        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        for (ParagraphsToTableRule rule : m_rules) {
            addRule(rule);
        }
        if (m_rules.size() > 0) {
            m_controlContainer.getListBox("lbParagraphsToTables").selectItemPos((short) 0, true);
        }
    }

    protected void updateControls() {
        short pos = m_controlContainer.getListBox("lbParagraphsToTables").getSelectedItemPos();
        m_controlContainer.getWindow("btnEdit").setEnable(pos != -1);
        m_controlContainer.getWindow("btnDelete").setEnable(pos != -1);
    }

    protected void fromControls() {
    }

    private void addRule(ParagraphsToTableRule rule) {
        m_controlContainer.getListBox("lbParagraphsToTables").addItem(rule.getParagraphs(), (short) m_rules.size());
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("btnAdd")) {
            ParagraphsToTableRule rule = new ParagraphsToTableRule();
            ParagraphsToTableRuleDialog dlg = new ParagraphsToTableRuleDialog(rule, m_rules);
            try {
                if (dlg.show()) {
                    m_rules.add(rule);
                    addRule(rule);
                    m_controlContainer.getListBox("lbParagraphsToTables").selectItemPos((short) (m_rules.size() - 1), true);
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (arg0.ActionCommand.equals("btnEdit")) {
            editRule();
        } else if (arg0.ActionCommand.equals("btnDelete")) {
            short pos = m_controlContainer.getListBox("lbParagraphsToTables").getSelectedItemPos();
            if (pos != -1) {
                m_controlContainer.getListBox("lbParagraphsToTables").removeItems(pos, (short) 1);
                m_rules.remove(pos);
                if (pos == m_rules.size()) {
                    --pos;
                }
                if (pos >= 0) {
                    m_controlContainer.getListBox("lbParagraphsToTables").selectItemPos(pos, true);
                } else {
                    updateControls();
                }
            }
        } else if (arg0.ActionCommand.equals("OK")) {
            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    private void editRule() {
        short pos = m_controlContainer.getListBox("lbParagraphsToTables").getSelectedItemPos();
        if (pos != -1) {
            ParagraphsToTableRule rule = m_rules.get(pos);
            ParagraphsToTableRuleDialog dlg = new ParagraphsToTableRuleDialog(rule, m_rules);
            try {
                if (dlg.show()) {
                    m_controlContainer.getListBox("lbParagraphsToTables").removeItems(pos, (short) 1);
                    m_controlContainer.getListBox("lbParagraphsToTables").addItem(rule.getParagraphs(), pos);
                    m_controlContainer.getListBox("lbParagraphsToTables").selectItemPos(pos, true);
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void disposing(EventObject arg0) {
    }

    public void itemStateChanged(ItemEvent arg0) {
        updateControls();
    }

    public void mousePressed(MouseEvent arg0) {
        if (arg0.ClickCount == 2) {
            editRule();
        }
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }
}
