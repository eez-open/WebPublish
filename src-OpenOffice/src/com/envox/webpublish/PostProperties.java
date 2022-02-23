/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.sun.star.text.XTextDocument;
import com.sun.star.util.DateTime;
import java.util.HashMap;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class PostProperties {
    DocumentProperties m_documentProperties;

    private String m_postId;
    private String m_pageId;
    private String m_publishProfile;
    private String m_title;
    private String m_postType;
    private String m_primaryCategory;
    private String[] m_additionalCategories;
    private int m_publishOption;
    private DateTime m_publishDate;
    private String[] m_keywords;
    private int m_comments;
    //private int m_pings;
    private String m_excerpt;
    private HashMap<String, String> m_imageURLs = new HashMap<String, String>();

    PostProperties(XTextDocument xTextDocument) {
        m_documentProperties = new DocumentProperties(xTextDocument);
    }

    public String getPostId() { return m_postId; }
    public void setPostId(String id) { m_postId = id; }

    public String getPageId() { return m_pageId; }
    public void setPageId(String id) { m_pageId = id; }

    public String getPublishProfile() { return m_publishProfile; }
    public void setPublishProfile(String publishProfile) { m_publishProfile = publishProfile; }

    public boolean isWordPressProfile() {
        if (!StringUtils.isBlank(m_publishProfile)) {
            Profile profile = Util.getProfiles().getProfile(m_publishProfile);
            if (profile != null && profile.getType() == Profile.Type.WordPress) {
                return true;
            }
        }
        return false;
    }

    public String getTitle() { return m_title; }
    public void setTitle(String title) { m_title = title; }

    public String getPostType() { return m_postType; }
    public void setPostType(String postType) { m_postType = postType; }

    public boolean isPage() {
        return m_postType != null && m_postType.compareTo("page") == 0;
    }

    public String getPrimaryCategory() { return m_primaryCategory; }
    public void setPrimaryCategory(String primaryCategory) { m_primaryCategory = primaryCategory; }

    public String[] getAdditionalCategories() { return m_additionalCategories; }
    public void setAdditionalCategories(String[] additionalCategories) { m_additionalCategories = additionalCategories; }

    public int getPublishOption() { return m_publishOption; }
    public void setPublishOption(int publishOption) { m_publishOption = publishOption; }
    
    public DateTime getPublishDate() { return m_publishDate; }
    public void setPublishDate(DateTime publishDate) { m_publishDate = publishDate; }

    public String[] getKeywords() { return m_keywords; }
    public void setKeywords(String[] keywords) { m_keywords = keywords; }

    public int getComments() { return m_comments; }
    public void setComments(int comments) { m_comments = comments; }

    //public int getPings() { return m_pings; }
    //public void setPings(int pings) { m_pings = pings; }

    public String getExcerpt() { return m_excerpt; }
    public void setExcerpt(String excerpt) { m_excerpt = excerpt; }

    public HashMap<String, String> getImageURLs() { return m_imageURLs; }
    public void setImageURLs(HashMap<String, String> value) { m_imageURLs = value; }

    public void read() {
        m_postId = m_documentProperties.readCustomProperty("WebPublish_Post_Id", "");

        m_pageId = m_documentProperties.readCustomProperty("WebPublish_Post_PageId", "");

        m_publishProfile = m_documentProperties.readCustomProperty("WebPublish_Post_Profile", null);
        if (m_publishProfile == null) {
            if (!StringUtils.isBlank(m_postId)) {
                m_publishProfile = "Joomla";
            } else {
                m_publishProfile = "";
            }
        }

        m_title = m_documentProperties.getTitle();
        if (m_title == null) {
            m_title = "";
        }

        if (isWordPressProfile()) {
            m_postType = m_documentProperties.readCustomProperty("WebPublish_Post_PostType", null);
            if (m_postType == null) {
                boolean postAsPage = m_documentProperties.readCustomProperty("WebPublish_Post_PostAsPage", false);
                if (postAsPage) {
                    m_postType = "page";
                } else {
                    m_postType = "post";
                }
            }
        } else {
            m_postType = null;
        }

        m_primaryCategory = m_documentProperties.readCustomProperty("WebPublish_Post_PrimaryCategory", "");

        m_additionalCategories = StringUtils.stripAll(StringUtils.split(m_documentProperties.readCustomProperty("WebPublish_Post_AdditionalCategories", ""), ","));

        String publishOption = m_documentProperties.readCustomProperty("WebPublish_Post_PublishOption", "1");
        if (publishOption.equals("No publish")) {
            m_publishOption = 0;
        } else if (publishOption.equals("Set publish date")) {
            m_publishOption = 2;
            m_publishDate = (DateTime) m_documentProperties.readCustomProperty("WebPublish_Post_PublishDate");
        } else {
            m_publishOption = 1;
        }

        m_keywords = m_documentProperties.getKeywords();

        m_comments = m_documentProperties.readCustomProperty("WebPublish_Post_Comments", -1);
        //m_pings = m_documentProperties.readCustomProperty("WebPublish_Post_Pings", -1);
        m_excerpt = m_documentProperties.readCustomProperty("WebPublish_Post_Excerpt", "");
        m_imageURLs = Util.stringToMap(m_documentProperties.readCustomProperty("WebPublish_Post_ImageURLs", ""));
    }
    
    public void write() {
        m_documentProperties.writeCustomProperty("WebPublish_Post_Id", "", m_postId);
        m_documentProperties.writeCustomProperty("WebPublish_Post_PageId", "", m_pageId);

        m_documentProperties.writeCustomProperty("WebPublish_Post_Profile", "", m_publishProfile);

        m_documentProperties.setTitle(m_title);

        if (isWordPressProfile()) {
            m_documentProperties.writeCustomProperty("WebPublish_Post_PostType", "", m_postType);
        } else {
            m_documentProperties.writeCustomProperty("WebPublish_Post_PostType", "", "");
        }

        m_documentProperties.writeCustomProperty("WebPublish_Post_PrimaryCategory", "", m_primaryCategory);
        m_documentProperties.writeCustomProperty("WebPublish_Post_AdditionalCategories", "", StringUtils.join(m_additionalCategories, ", "));

        if (m_publishOption == 0) {
            m_documentProperties.writeCustomProperty("WebPublish_Post_PublishOption", "", "No publish");
        } else if (m_publishOption == 2) {
            m_documentProperties.writeCustomProperty("WebPublish_Post_PublishOption", "", "Set publish date");
        } else {
            m_documentProperties.writeCustomProperty("WebPublish_Post_PublishOption", "", "Publish now");
        }
        m_documentProperties.writeCustomProperty("WebPublish_Post_PublishDate", new DateTime(), m_publishDate);

        m_documentProperties.setKeywords(m_keywords);

        m_documentProperties.writeCustomProperty("WebPublish_Post_Comments", "-1", Integer.valueOf(m_comments).toString());
        //m_documentProperties.writeCustomProperty("WebPublish_Post_Pings", "-1", Integer.valueOf(m_pings).toString());

        m_documentProperties.writeCustomProperty("WebPublish_Post_Excerpt", "", m_excerpt);

        m_documentProperties.writeCustomProperty("WebPublish_Post_ImageURLs", "", Util.mapToString(m_imageURLs));
    }
}
