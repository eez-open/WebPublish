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
public class FontAlternativesDialog
        extends StandardDialog
        implements XActionListener, XItemListener, XMouseListener {

    List<FontAlternative> m_alternatives;

    FontAlternativesDialog(List<FontAlternative> alternatives) {
        m_alternatives = alternatives;
    }

    List<FontAlternative> getFontAlternatives() { return m_alternatives; }

    public boolean show() throws Exception {
        return show("FontAlternatives");
    }

    protected void initControls() {
        m_controlContainer.getListBox("lbFontAlternatives").addItemListener(this);
        m_controlContainer.getWindow("lbFontAlternatives").addMouseListener(this);

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
        for (FontAlternative alternative : m_alternatives) {
            addAlternativeToList(alternative);
        }
        if (m_alternatives.size() > 0) {
            m_controlContainer.getListBox("lbFontAlternatives").selectItemPos((short) 0, true);
        }
    }

    protected void updateControls() {
        short pos = m_controlContainer.getListBox("lbFontAlternatives").getSelectedItemPos();
        m_controlContainer.getWindow("btnEdit").setEnable(pos != -1);
        m_controlContainer.getWindow("btnDelete").setEnable(pos != -1);
    }

    protected void fromControls() {
    }

    private void addAlternativeToList(FontAlternative alternative) {
        m_controlContainer.getListBox("lbFontAlternatives").addItem(alternative.getFont() + " - " + alternative.getAlternative(), (short) m_alternatives.size());
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("btnAdd")) {
            FontAlternative alternative = new FontAlternative();
            FontAlternativeDialog dlg = new FontAlternativeDialog(alternative, m_alternatives);
            try {
                if (dlg.show()) {
                    m_alternatives.add(alternative);
                    addAlternativeToList(alternative);
                    m_controlContainer.getListBox("lbFontAlternatives").selectItemPos((short) (m_alternatives.size() - 1), true);
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfileDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (arg0.ActionCommand.equals("btnEdit")) {
            editAlternative();
        } else if (arg0.ActionCommand.equals("btnDelete")) {
            short pos = m_controlContainer.getListBox("lbFontAlternatives").getSelectedItemPos();
            if (pos != -1) {
                m_controlContainer.getListBox("lbFontAlternatives").removeItems(pos, (short) 1);
                m_alternatives.remove(pos);
                if (pos == m_alternatives.size()) {
                    --pos;
                }
                if (pos >= 0) {
                    m_controlContainer.getListBox("lbFontAlternatives").selectItemPos(pos, true);
                } else {
                    updateControls();
                }
            }
        } else if (arg0.ActionCommand.equals("OK")) {
            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    private void editAlternative() {
        short pos = m_controlContainer.getListBox("lbFontAlternatives").getSelectedItemPos();
        if (pos != -1) {
            FontAlternative alternative = m_alternatives.get(pos);
            FontAlternativeDialog dlg = new FontAlternativeDialog(alternative, m_alternatives);
            try {
                if (dlg.show()) {
                    m_controlContainer.getListBox("lbFontAlternatives").removeItems(pos, (short) 1);
                    m_controlContainer.getListBox("lbFontAlternatives").addItem(alternative.getFont() + " - " + alternative.getAlternative(), pos);
                    m_controlContainer.getListBox("lbFontAlternatives").selectItemPos(pos, true);
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
            editAlternative();
        }
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }
}
