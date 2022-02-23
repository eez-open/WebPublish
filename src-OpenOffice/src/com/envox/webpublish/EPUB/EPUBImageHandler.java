/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.EPUB;

import com.envox.webpublish.CommandError;
import com.envox.webpublish.CommandErrorException;
import com.envox.webpublish.CommandProgress;
import com.envox.webpublish.DOM.ImageHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author martin
 */
public class EPUBImageHandler implements ImageHandler {
    private HashMap<String, byte[]> m_images = new HashMap<String, byte[]>();
    String m_imagesFolder;
    
    EPUBImageHandler(String imagesFolder) {
        m_imagesFolder = imagesFolder;
    }

    public boolean hasImage(String name) {
        return m_images.keySet().contains(name);
    }

    public String[] getImageNames() {
        return m_images.keySet().toArray(new String[0]);
    }

    public void putImage(String name, byte[] data) {
        m_images.put(name, data);
    }

    public String getImageURL(String name) {
        return "../" + m_imagesFolder + "/" + name;
    }

    public String getCSSImageURL(String name) {
        return "../" + m_imagesFolder + "/" + name;
    }

    public void saveImages(ZipOutputStream out, String basePath, CommandProgress progress) throws CommandErrorException {
	int iImage = 0;
	for (Map.Entry<String, byte[]> entry:m_images.entrySet()) {
            String imageName = entry.getKey();
            byte[] imageData = entry.getValue();
            if (imageData != null) {
                try {
                    ZipEntry zipEntry = new ZipEntry(basePath + "/" + imageName);
                    out.putNextEntry(zipEntry);
                    out.write(imageData, 0, imageData.length);
                } catch (IOException ex) {
                    throw new CommandErrorException(CommandError.ERROR_GRAPHIC_EXTRACT);
                }
            }
            progress.progress(++iImage, 1, m_images.size());
	}
    }
}
