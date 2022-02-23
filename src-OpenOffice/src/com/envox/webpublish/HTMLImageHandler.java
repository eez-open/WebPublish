/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish;

import com.envox.webpublish.DOM.ImageHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author martin
 */
public class HTMLImageHandler implements ImageHandler {
    private final HashMap<String, byte[]> m_images = new HashMap<String, byte[]>();
    public String m_pathToImagesFolder;
    public String m_imagesFolder;
    public String m_imageFileNamePrefix;
    
    HTMLImageHandler(String pathToImagesFolder, String imagesFolder, String imageFileNamePrefix) {
        m_pathToImagesFolder = pathToImagesFolder;
        m_imagesFolder = imagesFolder;
        m_imageFileNamePrefix = imageFileNamePrefix;
    }

    public boolean hasImage(String name) {
        return m_images.keySet().contains(name);
    }

    public void putImage(String name, byte[] data) {
        m_images.put(name, data);
    }
    
    public String getImageURL(String name) {
        return m_imagesFolder + "/" + m_imageFileNamePrefix + name;
    }

    public String getCSSImageURL(String name) {
        return getImageURL(name);
    }

    public void saveImages(CommandProgress progress) throws CommandErrorException {
	int iImage = 0;
	for (Map.Entry<String, byte[]> entry:m_images.entrySet()) {
            String imageName = entry.getKey();
            byte[] imageData = entry.getValue();
            if (imageData != null) {
                try {
                    File dirFile = new File(FilenameUtils.concat(m_pathToImagesFolder, m_imagesFolder));
                    if (!dirFile.exists()) {
                        dirFile.mkdir();
                    }
                    
                    String imageFileName = this.getImageURL(imageName);
                    File outFile = new File(FilenameUtils.concat(m_pathToImagesFolder, imageFileName));

                    FileOutputStream fos = new FileOutputStream(outFile);
                    fos.write(imageData, 0, imageData.length);
                    fos.close();
                } catch (IOException ex) {
                    throw new CommandErrorException(CommandError.ERROR_GRAPHIC_EXTRACT);
                }
            }
            progress.progress(++iImage, 1, m_images.size());
	}
    }
}
