/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.envox.webpublish;

import com.sun.star.datatransfer.DataFlavor;
import com.sun.star.datatransfer.XTransferable;

/**
 *
 * @author martin
 */
public final class TextTransferable implements XTransferable {
    private final String UNICODE_CONTENT_TYPE = "text/plain;charset=utf-16";
    private String text;

    public TextTransferable(String text) {
        this.text = text;
    }

    public Object getTransferData(DataFlavor dataFlavor) {
        if (dataFlavor.MimeType.equals(UNICODE_CONTENT_TYPE )) {
            return text;
        }
        return null;
    }

    public DataFlavor[] getTransferDataFlavors() {
        DataFlavor[] dataFlavors = new DataFlavor[1];
        dataFlavors[0] = new DataFlavor();
        dataFlavors[0].MimeType = UNICODE_CONTENT_TYPE ;
        return dataFlavors;
    }

    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        if (dataFlavor.MimeType.equals(UNICODE_CONTENT_TYPE )) {
            return true;
        }
        return false;
    }
}
