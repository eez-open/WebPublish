/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.DOM.ImageHandler;

/**
 *
 * @author martin
 */
public class DummyImageHandler implements ImageHandler {
    public boolean hasImage(String name) {
        return false;
    }

    public void putImage(String name, byte[] data) {
    }

    public String getImageURL(String name) {
        return name;
    }

    public String getCSSImageURL(String name) {
        return name;
    }
}
