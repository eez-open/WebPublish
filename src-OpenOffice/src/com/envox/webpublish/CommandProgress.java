/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

/**
 *
 * @author martin
 */
public interface CommandProgress {
    public void status(String description);
    public void progress(int value, int min, int max);
}
