/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

/**
 *
 * @author martin
 */
public interface Command {
    public void run(CommandProgress progress) throws CommandErrorException;
}
