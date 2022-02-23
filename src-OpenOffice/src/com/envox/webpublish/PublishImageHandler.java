/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.DOM.ImageHandler;
import java.util.HashMap;
import java.util.Map.Entry;
import org.apache.xmlrpc.XmlRpcException;

/**
 *
 * @author martin
 */
public class PublishImageHandler implements ImageHandler {
    private HashMap<String, byte[]> m_images = new HashMap<String, byte[]>();
    private HashMap<String, String> m_imageURLs = new HashMap<String, String>();
    private boolean m_bURLsChanged;

    public boolean hasImage(String name) {
        return m_images.keySet().contains(name);
    }

    public void putImage(String name, byte[] data) {
        m_images.put(name, data);
    }

    public String getImageURL(String name) {
        if (m_imageURLs.containsKey(name))
            return m_imageURLs.get(name);
        return name;
    }

    public String getCSSImageURL(String name) {
        return getImageURL(name);
    }

    public void publishImages(PostProperties postProperties, MoveableTypeAPIClient blogClient, CommandProgress progress) throws CommandErrorException, XmlRpcException {
        // URL-ovi su se promijenili ako je neki image obrisan
        HashMap<String, String> imageURLs = postProperties.getImageURLs();
        for (Entry<String, String> image: postProperties.getImageURLs().entrySet()) {
            if (!m_images.containsKey(image.getKey())) {
                m_bURLsChanged = true;
                break;
            }
        }

	int iImage = 0;
        for (Entry<String, byte[]> image: m_images.entrySet()) {
            if (imageURLs.containsKey(image.getKey())) {
                m_imageURLs.put(image.getKey(), imageURLs.get(image.getKey()));
            } else {
                String url = blogClient.publishImage(image.getKey(), image.getValue());
                m_imageURLs.put(image.getKey(), url);
                // URL-ovi su se promijenili ako je novi image dodan
                m_bURLsChanged = true;
            }

            progress.progress(++iImage, 1, m_images.size());
        }

        // ako su se URL-ovi promijenili spremi u document propertije
        if (m_bURLsChanged) {
            postProperties.setImageURLs(m_imageURLs);
        }
    }

    public boolean isURLsChanged() {
        return m_bURLsChanged;
    }
}
