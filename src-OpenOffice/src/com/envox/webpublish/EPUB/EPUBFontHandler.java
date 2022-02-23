/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.EPUB;

import com.envox.webpublish.CommandError;
import com.envox.webpublish.CommandErrorException;
import com.envox.webpublish.CommandProgress;
import com.envox.webpublish.DOM.FontHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author martin
 */
public class EPUBFontHandler implements FontHandler {
    private HashMap<String, byte[]> m_fonts = new HashMap<String, byte[]>();
    String m_fontsFolder;
    
    EPUBFontHandler(String fontsFolder) {
        m_fontsFolder = fontsFolder;
    }

    public boolean hasFont(String name) {
        return m_fonts.keySet().contains(name);
    }

    public String[] getFontNames() {
        return m_fonts.keySet().toArray(new String[0]);
    }

    public void putFont(String name, byte[] data) {
        m_fonts.put(name, data);
    }

    public String getFontURL(String name) {
        return m_fontsFolder + "/" + name;
    }

    public String getCSSFontURL(String name) {
        return "../" + m_fontsFolder + "/" + name;
    }

    public void saveFonts(ZipOutputStream out, String basePath, CommandProgress progress) throws CommandErrorException {
	int iFont = 0;
	for (Map.Entry<String, byte[]> entry:m_fonts.entrySet()) {
            String fontName = entry.getKey();
            byte[] fontData = entry.getValue();
            if (fontData != null) {
                try {
                    ZipEntry zipEntry = new ZipEntry(basePath + "/" + fontName);
                    out.putNextEntry(zipEntry);
                    out.write(fontData, 0, fontData.length);
                } catch (IOException ex) {
                    throw new CommandErrorException(CommandError.ERROR_FONT_SAVE);
                }
            }
            progress.progress(++iFont, 1, m_fonts.size());
	}
    }
}
