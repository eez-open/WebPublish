/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

/**
 *
 * @author martin
 */
public interface ImageHandler {
    public boolean hasImage(String name);
    public void putImage(String name, byte[] data);
    public String getCSSImageURL(String name);
    public String getImageURL(String name);
}
