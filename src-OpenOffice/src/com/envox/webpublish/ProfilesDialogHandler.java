/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.GUIHelper.ControlContainer;
import com.envox.webpublish.ProfilesDialogEventHandler._ProfilesDialogEventHandler;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XMouseListener;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class ProfilesDialogHandler {
    ControlContainer m_controlContainer;

    XActionListener m_actionListener;
    XItemListener m_itemListener;
    XMouseListener m_mouseListener;

    List<Profile> m_profiles;

    public ProfilesDialogHandler() {
    }

    public void initContainer(ControlContainer controlContainer, XActionListener actionListener, XItemListener itemListener,  XMouseListener mouseListener) {
        m_controlContainer = controlContainer;
        m_actionListener = actionListener;
        m_itemListener = itemListener;
        m_mouseListener = mouseListener;
    }

    private void addProfileToList(Profile profile) {
        m_controlContainer.getListBox("lbProfiles").addItem(profile.getProfileName(), (short)m_profiles.size());
    }

    public void initControls() {
        m_controlContainer.getListBox("lbProfiles").addItemListener(m_itemListener);
        m_controlContainer.getWindow("lbProfiles").addMouseListener(m_mouseListener);

        m_controlContainer.getButton("btnAdd").setActionCommand("btnAdd");
        m_controlContainer.getButton("btnAdd").addActionListener(m_actionListener);

        m_controlContainer.getButton("btnEdit").setActionCommand("btnEdit");
        m_controlContainer.getButton("btnEdit").addActionListener(m_actionListener);

        m_controlContainer.getButton("btnDelete").setActionCommand("btnDelete");
        m_controlContainer.getButton("btnDelete").addActionListener(m_actionListener);

        m_controlContainer.getButton("btnLoad").setActionCommand("btnLoad");
        m_controlContainer.getButton("btnLoad").addActionListener(m_actionListener);

        m_controlContainer.getButton("btnSave").setActionCommand("btnSave");
        m_controlContainer.getButton("btnSave").addActionListener(m_actionListener);
    }

    public void toControls() {
        m_controlContainer.getListBox("lbProfiles").removeItems((short)0, m_controlContainer.getListBox("lbProfiles").getItemCount());
        m_profiles = Util.getProfiles().load();
        for (Profile profile:m_profiles) {
            addProfileToList(profile);
        }
        if (m_profiles.size() > 0) {
            m_controlContainer.getListBox("lbProfiles").selectItemPos((short)0, true);
        }
    }

    public void updateControls() {
        short pos = m_controlContainer.getListBox("lbProfiles").getSelectedItemPos();
        m_controlContainer.getWindow("btnEdit").setEnable(pos != -1);
    }

    public void fromControls() {
        Util.getProfiles().save(m_profiles);
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("btnAdd")) {
            try {
                SelectProfileTypeDialog dlgSelectProfileType = new SelectProfileTypeDialog();
                if (dlgSelectProfileType.show()) {
                    Profile profile = new Profile(dlgSelectProfileType.getType());
                    ProfileDialog dlgProfile = new ProfileDialog(profile, m_profiles);
                    if (dlgProfile.show()) {
                        m_profiles.add(profile);
                        addProfileToList(profile);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(ProfilesDialogEventHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (arg0.ActionCommand.equals("btnEdit")) {
            editProfile();
        } else if (arg0.ActionCommand.equals("btnDelete")) {
            short pos = m_controlContainer.getListBox("lbProfiles").getSelectedItemPos();
            if (pos != -1) {
                m_controlContainer.getListBox("lbProfiles").removeItems(pos, (short)1);
                m_profiles.remove(pos);
                if (pos == m_profiles.size()) {
                    --pos;
                }
                if (pos >= 0) {
                    m_controlContainer.getListBox("lbProfiles").selectItemPos(pos, true);
                } else {
                    updateControls();
                }
            }
        } else if (arg0.ActionCommand.equals("btnLoad")) {
            final URL url = Util.showFileOpenDialog("WebPublish Profiles", "*.wpp");
            if (url != null) {
                try {
                    m_profiles = Profiles.loadFromXML(url);
                } catch (java.lang.Exception ex) {
                    Logger.getLogger(ProfilesDialogEventHandler.class.getName()).log(Level.SEVERE, null, ex);
                    Util.showMessageBox("WebPublish", String.format("Load error: %s", ex.getMessage()));
                }

                m_controlContainer.getListBox("lbProfiles").removeItems((short)0, (short)m_controlContainer.getListBox("lbProfiles").getItemCount());
                for (Profile profile:m_profiles) {
                    addProfileToList(profile);
                }
                if (m_profiles.size() > 0) {
                    m_controlContainer.getListBox("lbProfiles").selectItemPos((short)0, true);
                }

                updateControls();

                Util.showMessageBox("WebPublish", "Profiles successfully loaded.");
            }
        } else if (arg0.ActionCommand.equals("btnSave")) {
            final URL url = Util.showFileSaveDialog("profiles.wpp", "WebPublish Profiles", "*.wpp");
            if (url != null) {
                try {
                    Profiles.saveToXML(m_profiles, url);
                    Util.showMessageBox("WebPublish", "Profiles file successfully saved.");
                } catch (java.lang.Exception ex) {
                    Logger.getLogger(ProfilesDialogEventHandler.class.getName()).log(Level.SEVERE, null, ex);
                    Util.showMessageBox("WebPublish", String.format("Save error: %s", ex.getMessage()));
                }
            }
        }
    }

    private void editProfile() {
        short pos = m_controlContainer.getListBox("lbProfiles").getSelectedItemPos();
        if (pos != -1) {
            Profile profile = m_profiles.get(pos);
            ProfileDialog dlg = new ProfileDialog(profile, m_profiles);
            try {
                if (dlg.show()) {
                    m_controlContainer.getListBox("lbProfiles").removeItems(pos, (short)1);
                    m_controlContainer.getListBox("lbProfiles").addItem(profile.getProfileName(), pos);
                    m_controlContainer.getListBox("lbProfiles").selectItemPos(pos, true);
                }
            } catch (Exception ex) {
                Logger.getLogger(_ProfilesDialogEventHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void itemStateChanged(ItemEvent arg0) {
        updateControls();
    }

    public void mousePressed(MouseEvent arg0) {
        if (arg0.ClickCount == 2) {
            editProfile();
        }
    }
}
