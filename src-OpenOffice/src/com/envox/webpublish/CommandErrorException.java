/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

/**
 *
 * @author martin
 */
public class CommandErrorException extends Exception {
    public CommandErrorException(CommandError error) {
        super(String.format("An unexpected error (%s) has occured.", error.toString()));
    }

    public CommandErrorException(String message) {
        super(message);
    }
}
