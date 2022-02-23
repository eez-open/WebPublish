/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.GUIHelper.Dialog;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XDateField;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTimeField;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;

/**
 *
 * @author martin
 */
public class PostPropertiesDialog
        extends Dialog
        implements XItemListener, XActionListener {

    private MoveableTypeAPIClient m_blogClient;
    private PostProperties m_postProperties;

    protected int m_dialogResult;

    private XTextComponent m_txtPostId;

    private XTextComponent m_txtPublishProfile;

    private XTextComponent m_txtTitle;

    private XCheckBox m_chkPostAsPage;

    private XListBox m_lbPrimaryCategory;
    private XCheckBox m_chkAdditionalCategories;
    private XListBox m_lbAdditionalCategories;

    private XRadioButton m_rbNoPublish;
    private XRadioButton m_rbPublishNow;
    private XRadioButton m_rbSetPublishDate;
    private XDateField m_datePublish;
    private XTimeField m_timePublish;

    private XTextComponent m_txtKeywords;
    private XListBox m_lbComments;
    //private XListBox m_lbPings;
    private XTextComponent m_txtExcerpt;

    private Thread m_loadCategoriesThread;

    public PostPropertiesDialog(MoveableTypeAPIClient blogClient, PostProperties postProperties) {
        m_blogClient = blogClient;
        m_postProperties = postProperties;
    }

    public boolean show() throws Exception {
        createDialog("PostProperties");

        startLoadCategoriesThread();

        m_dialogResult = 0;
        m_xDialog.execute();
        if (m_dialogResult == 1) {
            fromControls();
            return true;
        }

        return false;
    }

    private void initControls() {
        m_txtPostId = m_controlContainer.getTextComponent("txtPostId");

        m_txtPublishProfile = m_controlContainer.getTextComponent("txtPublishProfile");

        m_txtTitle = m_controlContainer.getTextComponent("txtTitle");

        m_chkPostAsPage = m_controlContainer.getCheckBox("chkPostAsPage");
        m_chkPostAsPage.addItemListener(this);

        m_lbPrimaryCategory = m_controlContainer.getListBox("lbPrimaryCategory");
        m_lbPrimaryCategory.addItemListener(this);
        
        m_chkAdditionalCategories = m_controlContainer.getCheckBox("chkAdditionalCategories");
        m_chkAdditionalCategories.addItemListener(this);
        m_lbAdditionalCategories = m_controlContainer.getListBox("lbAdditionalCategories");
        initCategories();

        m_rbNoPublish = m_controlContainer.getRadioButton("rbNoPublish");
        m_rbNoPublish.addItemListener(this);
        m_rbPublishNow = m_controlContainer.getRadioButton("rbPublishNow");
        m_rbPublishNow.addItemListener(this);
        m_rbSetPublishDate = m_controlContainer.getRadioButton("rbSetPublishDate");
        m_rbSetPublishDate.addItemListener(this);
        m_datePublish = m_controlContainer.getDateField("datePublish");
        m_timePublish = m_controlContainer.getTimeField("timePublish");

        m_txtKeywords = m_controlContainer.getTextComponent("txtKeywords");

        m_lbComments = m_controlContainer.getListBox("lbComments");
        initComments();
        
        //m_lbPings = m_controlContainer.getListBox("lbPings");
        //initPings();

        m_txtExcerpt = m_controlContainer.getTextComponent("txtExcerpt");

        m_controlContainer.getButton("btnOK").setActionCommand("OK");
        m_controlContainer.getButton("btnOK").addActionListener(this);
    }

    private void initCategories() {
        String[] categories = m_blogClient.getCategoriesNames();
        addItems(categories, m_lbPrimaryCategory);
        addItems(categories, m_lbAdditionalCategories);
    }

    private void initComments() {
        m_lbComments.addItem("(default)", (short)0);
        m_lbComments.addItem("None", (short)1);
        m_lbComments.addItem("Open", (short)2);
        m_lbComments.addItem("Closed", (short)3);
    }

    /*
    private void initPings() {
        m_lbPings.addItem("(default)", (short)0);
        m_lbPings.addItem("Allow", (short)1);
        m_lbPings.addItem("Deny", (short)2);
    }
    */

    private void toControls() {
        if (m_postProperties.getPostAsPage()) {
            m_txtPostId.setText(m_postProperties.getPostId());
        } else {
            m_txtPostId.setText(m_postProperties.getPageId());
        }

        if (m_postProperties.getPublishProfile() != null) {
            m_txtPublishProfile.setText(m_postProperties.getPublishProfile());
        }

        if (m_postProperties.getTitle() != null) {
            m_txtTitle.setText(m_postProperties.getTitle());
        }

        m_chkPostAsPage.setState((short)(m_postProperties.getPostAsPage() ? 1 : 0));

        String primaryCategoryName = m_blogClient.getCategoryNameFromId(m_postProperties.getPrimaryCategory());
        if (primaryCategoryName != null) {
            m_lbPrimaryCategory.selectItem(primaryCategoryName, true);
        }

        String[] additionalCategoriesNames = m_blogClient.getCategoriesNamesFromIds(m_postProperties.getAdditionalCategories());
        if (additionalCategoriesNames != null && additionalCategoriesNames.length > 0) {
            m_chkAdditionalCategories.setState((short)1);
            selectItems(additionalCategoriesNames, m_lbAdditionalCategories);
        } else {
            m_chkAdditionalCategories.setState((short)0);
        }

        if (m_postProperties.getPublishOption() == 0) {
            m_rbNoPublish.setState(true);
        } else if (m_postProperties.getPublishOption() == 2) {
            m_rbSetPublishDate.setState(true);
            toDateTimeControl(m_postProperties.getPublishDate(), m_datePublish, m_timePublish);
        } else {
            m_rbPublishNow.setState(true);
        }

        if (m_postProperties.getKeywords() != null) {
            m_txtKeywords.setText(StringUtils.join(m_postProperties.getKeywords(), ", "));
        }

        switch (m_postProperties.getComments()) {
            case -1: m_lbComments.selectItemPos((short)0, true); break;
            case 0: m_lbComments.selectItemPos((short)1, true); break;
            case 1: m_lbComments.selectItemPos((short)2, true); break;
            case 2: m_lbComments.selectItemPos((short)3, true); break;
        }

        /*
        switch (m_postProperties.getPings()) {
            case -1: m_lbPings.selectItemPos((short)0, true); break;
            case 1: m_lbPings.selectItemPos((short)1, true); break;
            case 0: m_lbPings.selectItemPos((short)2, true); break;
        }
        */

        m_txtExcerpt.setText(m_postProperties.getExcerpt());
    }

    private void updateControls() {
        m_controlContainer.getWindow("chkPostAsPage").setEnable(m_postProperties.isWordPressProfile());

        if (m_postProperties.isWordPressProfile() && m_chkPostAsPage.getState() == 1) {
            m_txtPostId.setText(m_postProperties.getPageId());

            m_controlContainer.getWindow("lblPrimaryCategory").setEnable(false);
            m_controlContainer.getWindow("lbPrimaryCategory").setEnable(false);
            m_controlContainer.getWindow("chkAdditionalCategories").setEnable(false);
            m_controlContainer.getWindow("lbAdditionalCategories").setEnable(false);
        } else {
            m_txtPostId.setText(m_postProperties.getPostId());

            m_controlContainer.getWindow("lblPrimaryCategory").setEnable(true);
            m_controlContainer.getWindow("lbPrimaryCategory").setEnable(true);
            m_controlContainer.getWindow("chkAdditionalCategories").setEnable(true);
            m_controlContainer.getWindow("lbAdditionalCategories").setEnable(m_chkAdditionalCategories.getState() == 1 ? true : false);
            m_controlContainer.getWindow("datePublish").setEnable(m_rbSetPublishDate.getState());
            m_controlContainer.getWindow("timePublish").setEnable(m_rbSetPublishDate.getState());

            // remove primary category from the list of additional categories
            String[] categories = m_blogClient.getCategoriesNames();
            String primaryCategory = m_lbPrimaryCategory.getSelectedItem();

            if (primaryCategory != null) {
                for (short i = 0; i < m_lbAdditionalCategories.getItemCount(); ++i) {
                    if (m_lbAdditionalCategories.getItem(i).equals(primaryCategory)) {
                        m_lbAdditionalCategories.removeItems(i, (short)1);
                        break;
                    }
                }
            }

            for (String category:categories) {
                if (primaryCategory == null || !category.equals(primaryCategory)) {
                    for (short i = 0; i < m_lbAdditionalCategories.getItemCount(); ++i) {
                        int cmp = category.compareTo(m_lbAdditionalCategories.getItem(i));
                        if (cmp < 0) {
                            m_lbAdditionalCategories.addItem(category, i);
                            break;
                        } else if (cmp == 0) {
                            break;
                        } else if (i == m_lbAdditionalCategories.getItemCount() - 1) {
                            m_lbAdditionalCategories.addItem(category, (short) (i+1));
                        }
                    }
                }
            }
        }
    }

    private void fromControls() {
        m_postProperties.setTitle(m_txtTitle.getText());

        if (m_postProperties.isWordPressProfile()) {
            m_postProperties.setPostAsPage(m_chkPostAsPage.getState() == 1 ? true : false);
        }

        m_postProperties.setPrimaryCategory(m_blogClient.getCategoryIdFromName(m_lbPrimaryCategory.getSelectedItem()));

        if (m_chkAdditionalCategories.getState() == 1) {
            m_postProperties.setAdditionalCategories(m_blogClient.getCategoriesIdsFromNames(m_lbAdditionalCategories.getSelectedItems()));
        } else {
            m_postProperties.setAdditionalCategories(null);
        }

        if (m_rbNoPublish.getState()) {
            m_postProperties.setPublishOption(0);
            m_postProperties.setPublishDate(null);
        } else if (m_rbSetPublishDate.getState()) {
            m_postProperties.setPublishOption(2);
            m_postProperties.setPublishDate(fromDateTimeControl(m_datePublish, m_timePublish));
        } else {
            m_postProperties.setPublishOption(1);
            m_postProperties.setPublishDate(null);
        }

        m_postProperties.setKeywords(StringUtils.stripAll(StringUtils.split(m_txtKeywords.getText(), ",")));

        switch (m_lbComments.getSelectedItemPos()) {
            case 0: m_postProperties.setComments(-1); break;
            case 1: m_postProperties.setComments(0); break;
            case 2: m_postProperties.setComments(1); break;
            case 3: m_postProperties.setComments(2); break;
        }

        /*
        switch (m_lbPings.getSelectedItemPos()) {
            case 0: m_postProperties.setPings(-1); break;
            case 1: m_postProperties.setPings(1); break;
            case 2: m_postProperties.setPings(0); break;
        }
        */

        m_postProperties.setExcerpt(m_txtExcerpt.getText());
    }

    public void itemStateChanged(ItemEvent arg0) {
        updateControls();
    }

    public void disposing(EventObject arg0) {
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.ActionCommand.equals("OK")) {
            String title = m_txtTitle.getText();
            if (title == null || title.trim().equals("")) {
                Util.showMessageBox("WebPublish", "Enter title.");
                m_controlContainer.getWindow("txtTitle").setFocus();
                return;
            }

            if (m_chkPostAsPage.getState() == 0) {
                if (m_blogClient.getCategoryIdFromName(m_lbPrimaryCategory.getSelectedItem()) == null) {
                    Util.showMessageBox("WebPublish", "Select primary category.");
                    m_controlContainer.getWindow("lbPrimaryCategory").setFocus();
                    return;
                }
            }

            if (m_rbSetPublishDate.getState() && fromDateTimeControl(m_datePublish, m_timePublish) == null) {
                Util.showMessageBox("WebPublish", "Set publish date.");
                m_controlContainer.getWindow("datePublish").setFocus();
                return;
            }

            m_dialogResult = 1;
            m_xDialog.endExecute();
        }
    }

    private void startLoadCategoriesThread() {
        m_controlContainer.enableControls(false);

        Rectangle rect = m_controlContainer.getWindow("btnCancel").getPosSize();
        insertThrobber(6, rect.Y * 14 / rect.Height, 10, 10);
        setThrobberActive(true);

        m_loadCategoriesThread = new Thread(new RunnableWithCallback() {
            XmlRpcException m_ex;

            public void run() {
                try {
                    m_blogClient.init();
                } catch (XmlRpcException ex) {
                    m_ex = ex;
                }
                Util.mainThreadNotify(this, null);
            }

            public void notify(Object arg0) {
                if (m_ex != null) {
                    Util.showErrorMessageBox("WebPublish", "Connection error. Please, check your profile in WebPublish extension options.");
                    m_xDialog.endExecute();
                }

                setThrobberActive(false);
                m_controlContainer.enableControls(true);

                initControls();
                toControls();
                updateControls();
            }
        });

        m_loadCategoriesThread.start();
    }
}
