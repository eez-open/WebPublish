/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.sun.star.util.DateTime;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 *
 * @author martin
 */
public class MoveableTypeAPIClient {

    private XmlRpcClient m_client;

    String m_appKey;

    String m_blogID;

    String m_userName;
    String m_password;

    Object[] m_categories;

    MoveableTypeAPIClient(String url, String userName, String password, String blogID) throws MalformedURLException, XmlRpcException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(url));

        m_client = new XmlRpcClient();
        m_client.setConfig(config);

        m_appKey = "ignore";

        m_userName = userName;
        m_password = password;

        m_blogID = blogID;
    }

    public void init() throws XmlRpcException {
        if (StringUtils.isBlank(m_blogID)) {
            m_blogID = ((HashMap)getBlogs()[0]).get("blogid").toString();
        }
        if (m_categories == null) {
            m_categories = getCategories();
        }
    }

    private Object[] getCategories() throws XmlRpcException {
        return (Object[]) m_client.execute("mt.getCategoryList", new Object[]{
            m_blogID,
            m_userName,
            m_password
        });
    }

    public String[] getCategoriesNames() {
        List<String> items = new ArrayList<String>();
        for (Object objCategory:m_categories) {
            HashMap category = (HashMap) objCategory;
            items.add((String)category.get("categoryName"));
        }
        return items.toArray(new String[0]);
    }

    public String getCategoryNameFromId(String id) {
        if (id == null)
            return null;
        for (Object objCategory:m_categories) {
            HashMap category = (HashMap) objCategory;
            if (id.equals(category.get("categoryId").toString())) {
                return category.get("categoryName").toString();
            }
        }
        return null;
    }

    public String[] getCategoriesNamesFromIds(String[] ids) {
        if (ids == null) {
            return null;
        }
        List<String> names = new ArrayList<String>();
        for (String id:ids) {
            String name = getCategoryNameFromId(id);
            if (name != null) {
                names.add(name);
            }
        }
        return names.toArray(new String[0]);
    }

    public String getCategoryIdFromName(String name) {
        if (name == null)
            return null;
        for (Object objCategory:m_categories) {
            HashMap category = (HashMap) objCategory;
            if (name.equals((String)category.get("categoryName"))) {
                return category.get("categoryId").toString();
            }
        }
        return null;
    }

    public String[] getCategoriesIdsFromNames(String[] names) {
        if (names == null) {
            return null;
        }
        List<String> ids = new ArrayList<String>();
        for (String name:names) {
            String id = getCategoryIdFromName(name);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids.toArray(new String[0]);
    }

    public Object[] getBlogs() throws XmlRpcException {
        return (Object[]) m_client.execute("blogger.getUsersBlogs", new Object[]{
            m_appKey,
            m_userName,
            m_password
        });
    }

    public void publish(PostProperties postProperties, String description) throws XmlRpcException {
        if (postProperties.getPostAsPage()) {
            wpPublish(postProperties, description);
            return;
        }

        HashMap content = new HashMap();

        content.put("title", postProperties.getTitle());

        content.put("description", description);

        String[] categories = (String[]) ArrayUtils.addAll(
                new String[] { postProperties.getPrimaryCategory() },
                postProperties.getAdditionalCategories()
            );
        content.put("categories", getCategoriesNamesFromIds(categories));

        if (postProperties.getPublishOption() == 2) {
            DateTime dateTime = postProperties.getPublishDate();
            Calendar calendar = new GregorianCalendar();
            calendar.set(dateTime.Year, dateTime.Month - 1, dateTime.Day, dateTime.Hours, dateTime.Minutes, dateTime.Seconds);
            Date date = calendar.getTime();
            content.put("dateCreated", date);
            content.put("date_created_gmt", date);
        }

        if (postProperties.getKeywords().length > 0)
            content.put("mt_keywords", StringUtils.join(postProperties.getKeywords(), ", "));

        if (postProperties.getComments() != -1) {
            content.put("mt_allow_comments", postProperties.getComments());
        }

        /*
        if (postProperties.getPings() != -1) {
            content.put("mt_alloq_pings", postProperties.getPings());
        }
        */

        content.put("mt_excerpt", postProperties.getExcerpt());
        
        if (StringUtils.isBlank(postProperties.getPostId())) {
            Object result = m_client.execute("metaWeblog.newPost", new Object[]{
                m_blogID,
                m_userName,
                m_password,
                content,
                postProperties.getPublishOption() == 0 ? Boolean.FALSE : Boolean.TRUE
            });
            postProperties.setPostId((String)result);
        } else {
            m_client.execute("metaWeblog.editPost", new Object[]{
                postProperties.getPostId(),
                m_userName,
                m_password,
                content,
                postProperties.getPublishOption() == 0 ? Boolean.FALSE : Boolean.TRUE
            });
        }

        // setPostCategories
        HashMap[] postCategories = new HashMap[categories.length];
        for (int i = 0; i < categories.length; ++i) {
            postCategories[i] = new HashMap();
            postCategories[i].put("categoryId", categories[i]);
        }
        m_client.execute("mt.setPostCategories", new Object[]{
            postProperties.getPostId(),
            m_userName,
            m_password,
            postCategories
        });
    }

    public void wpPublish(PostProperties postProperties, String description) throws XmlRpcException {
        HashMap content = new HashMap();

        content.put("title", postProperties.getTitle());

        content.put("description", description);

        if (postProperties.getPublishOption() == 2) {
            DateTime dateTime = postProperties.getPublishDate();
            Calendar calendar = new GregorianCalendar();
            calendar.set(dateTime.Year, dateTime.Month - 1, dateTime.Day, dateTime.Hours, dateTime.Minutes, dateTime.Seconds);
            Date date = calendar.getTime();
            content.put("dateCreated", date);
            content.put("date_created_gmt", date);
        }

        if (postProperties.getKeywords().length > 0)
            content.put("mt_keywords", StringUtils.join(postProperties.getKeywords(), ", "));

        if (postProperties.getComments() != -1) {
            content.put("mt_allow_comments", postProperties.getComments());
        }

        /*
        if (postProperties.getPings() != -1) {
            content.put("mt_alloq_pings", postProperties.getPings());
        }
        */

        content.put("mt_excerpt", postProperties.getExcerpt());

        if (StringUtils.isBlank(postProperties.getPageId())) {
            Object result = m_client.execute("wp.newPage", new Object[]{
                m_blogID,
                m_userName,
                m_password,
                content,
                postProperties.getPublishOption() == 0 ? Boolean.FALSE : Boolean.TRUE
            });
            postProperties.setPageId((String)result);
        } else {
            String pageId = postProperties.getPageId();
            m_client.execute("wp.editPage", new Object[]{
                m_blogID,
                pageId,
                m_userName,
                m_password,
                content,
                postProperties.getPublishOption() == 0 ? Boolean.FALSE : Boolean.TRUE
            });
        }
    }

    public String publishImage(String name, byte[] imageData) throws XmlRpcException, CommandErrorException {
        HashMap fileStruct = new HashMap();
        fileStruct.put("name", name);
        fileStruct.put("bits", imageData);
        fileStruct.put("overwrite", Boolean.TRUE);
        HashMap result = (HashMap) m_client.execute("metaWeblog.newMediaObject", new Object[]{
            m_blogID,
            m_userName,
            m_password,
            fileStruct
        });
        return (String)result.get("url");
    }
}
