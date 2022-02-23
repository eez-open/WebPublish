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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class SelectProfileDialog
        extends StandardDialog
        implements XActionListener {

    public enum Type {
        All,
        EPUB,
        HTML,
        WebPublish
    }

    Type m_type;
    String m_profile;
    String m_selectProfile;

    public SelectProfileDialog(Type type, String profile) {
        m_type = type;
        m_selectProfile = profile;
    }

    String getProfile() { return m_profile; }

    public boolean show() throws Exception {
        if (getProfiles().isEmpty()) {
            if (m_type == Type.EPUB) {
                Util.showMessageBox("WebPublish", "There is no EPUB profile defined.");
            } else if (m_type == Type.HTML) {
                Util.showMessageBox("WebPublish", "There is no HTML profile defined.");
            } else if (m_type == Type.WebPublish) {
                Util.showMessageBox("WebPublish", "There is no Web Publish profile defined.");
            } else if (m_type == Type.All) {
                Util.showMessageBox("WebPublish", "There is no profile defined.");
            }
            ProfilesDialog dlg = new ProfilesDialog();
            dlg.show();
            return false;
        }
        return show("SelectProfile");
    }

    Map<String, Profile> getProfiles() {
        Map<String, Profile> profiles = new TreeMap<String, Profile>();

        for (Entry<String, Profile> entry: Util.getProfiles().get().entrySet()) {
            if (m_type == Type.All ||
                m_type == Type.EPUB && entry.getValue().getType() == Profile.Type.EPUB ||
                m_type == Type.HTML && entry.getValue().getType() == Profile.Type.HTML ||
                m_type == Type.WebPublish && (entry.getValue().getType() != Profile.Type.EPUB && entry.getValue().getType() != Profile.Type.HTML)) {
                profiles.put(entry.getKey(), entry.getValue());
            }
        }

        return profiles;
    }

    protected void initControls() {
        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    protected void toControls() {
        short iSelectProfile = 0;

        Set<Entry<String, Profile>> entrySet = getProfiles().entrySet();
        for (Entry<String, Profile> entry: entrySet) {
            short i = m_controlContainer.getListBox("lbProfiles").getItemCount();
            m_controlContainer.getListBox("lbProfiles").addItem(entry.getKey(), i);
            if (StringUtils.equals(entry.getKey(), m_selectProfile)) {
                iSelectProfile = i;
            }
        }

        m_controlContainer.getListBox("lbProfiles").selectItemPos(iSelectProfile, true);
    }

    protected void updateControls() {
    }

    protected void fromControls() {
        m_profile = m_controlContainer.getListBox("lbProfiles").getSelectedItem();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            String profile = m_controlContainer.getListBox("lbProfiles").getSelectedItem();
            if (StringUtils.isBlank(profile)) {
                Util.showMessageBox("WebPublish", "Select profile.");
                m_controlContainer.getWindow("lbProfiles").setFocus();
                return;
            }

            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    public void disposing(EventObject arg0) {
    }
}
