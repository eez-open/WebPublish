/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

/**
 *
 * @author martin
 */
public interface FontHandler {
    public boolean hasFont(String name);
    public void putFont(String name, byte[] data);
    public String getCSSFontURL(String name);
    public String getFontURL(String name);
}
