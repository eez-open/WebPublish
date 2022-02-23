/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.EPUB;

import com.envox.webpublish.CommandErrorException;
import com.envox.webpublish.CommandProgress;
import com.envox.webpublish.DOM.Block;
import com.envox.webpublish.DOM.Document;
import com.envox.webpublish.DOM.Paragraph;
import com.envox.webpublish.Profile;
import com.envox.webpublish.Util;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author martin
 */
public class Book {
    static final String ROOT = "OEBPS";

    static final String MIMETYPE_FILE_PATH = "mimetype";
    
    static final String CONTAINER_XML_FILE_PATH = "META-INF/container.xml";

    static final String BOOK_OPF_FILE_NAME = "book.opf";
    static final String BOOK_OPF_FILE_PATH  = ROOT + "/" + BOOK_OPF_FILE_NAME;

    static final String BOOK_NCX_FILE_NAME = "book.ncx";
    static final String BOOK_NCX_FILE_PATH = ROOT + "/" + BOOK_NCX_FILE_NAME;

    static final String CHAPTER_ID_PREFIX = "chapter";

    static final String CHAPTERS_FOLDER = "text";
    static final String CHAPTERS_PATH = ROOT + "/" + CHAPTERS_FOLDER;
    static final String CHAPTER_XML_FILE_NAME_FORMAT = "chapter%d.xml";

    static final String IMAGES_FOLDER = "images";
    static final String IMAGES_PATH = ROOT + "/" +  IMAGES_FOLDER;

    static final String FONTS_FOLDER = "fonts";
    static final String FONTS_PATH = ROOT + "/" + FONTS_FOLDER;

    static final String CHAPTER_CSS_FILE_NAME = "styles/chapter.css";
    static final String CHAPTER_CSS_FILE_PATH  = ROOT + "/" + CHAPTER_CSS_FILE_NAME;

    BookProperties m_properties;
    Document m_doc;
    Chapter m_mainChapter;
    int m_depth = 1;
    EPUBImageHandler m_imageHandler = new EPUBImageHandler(IMAGES_FOLDER);
    EPUBFontHandler m_fontHandler = new EPUBFontHandler(FONTS_FOLDER);

    public Book(BookProperties properties) {
        m_properties = properties;
    }

    public Document getDocument() {
        return m_doc;
    }

    public void loadDocument(XTextDocument xTextDocument, CommandProgress progress, Profile profile) throws NoSuchElementException, WrappedTargetException, Exception {
        m_doc = new Document(xTextDocument, profile);

        m_doc.useListFilter();
        m_doc.useMergeParagraphsFilter();
        m_doc.useTableToParagraphsFilter();

        m_doc.setImageHandler(m_imageHandler);
        m_doc.setFontHandler(m_fontHandler);

        progress.status("Analysing document ...");
        m_doc.load(progress);
        
        progress.status("Creating chapters ...");
        loadChapters(progress);
    }

    private void loadChapters(CommandProgress progress) {
        m_mainChapter = new Chapter(this);
        m_numChapters = 1;

        Chapter chapter = null;

        int iBlock = 0;
        List<Block> blocks = m_doc.getBlocks();
        for (Block block:blocks) {
            if (block instanceof Paragraph) {
                Paragraph paragraph = (Paragraph) block;
                if (paragraph.getOutlineLevel() > 0) {
                    m_depth = Math.max(m_depth, paragraph.getOutlineLevel());
                    if (chapter != null) {
                        if (paragraph.getOutlineLevel() > chapter.getOutlineLevel()) {
                            while (paragraph.getOutlineLevel() > chapter.getOutlineLevel()) {
                                chapter = new Chapter(this, paragraph.getText(), chapter, m_numChapters++);
                            }
                        } else {
                            while (paragraph.getOutlineLevel() < chapter.getOutlineLevel()) {
                                chapter = chapter.getParent();
                            }
                            chapter = new Chapter(this, paragraph.getText(), chapter.getParent(), m_numChapters++);
                        }
                    }
                }
            }

            if (chapter == null) {
                chapter = new Chapter(this, m_properties.getTitle(), m_mainChapter, m_numChapters++);
            }

            chapter.addBlock(block);
            progress.progress(++iBlock, 1, blocks.size());
        }
    }

    public void save(URL url, CommandProgress progress) throws IOException, CommandErrorException {
        File file = Util.toFile(url);

        progress.status(String.format("Saving to %s ...", file.getPath()));

        FileOutputStream dest = new FileOutputStream(file);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

        out.setMethod(ZipOutputStream.DEFLATED);
        out.setLevel(Deflater.DEFAULT_COMPRESSION);

        progress.status("Saving mimetype ...");
        saveMimetype(out);

        progress.status("Saving container.xml ...");
        saveContainerXml(out);

        progress.status("Saving chapters CSS ...");
        saveChapterCSS(out);

        progress.status("Saving chapters ...");
        saveChapters(out, progress);

        progress.status("Saving images ...");
        m_imageHandler.saveImages(out, IMAGES_PATH, progress);

        progress.status("Saving fonts ...");
        m_fontHandler.saveFonts(out, FONTS_PATH, progress);

        progress.status("Saving OPF ...");
        saveBookOPF(out);

        progress.status("Saving NCX ...");
        saveBookNCX(out);

        out.close();
    }

    private void saveMimetype(ZipOutputStream out) throws IOException {
        saveUncompressedFile(out, MIMETYPE_FILE_PATH,
                org.apache.commons.codec.binary.StringUtils.getBytesUsAscii("application/epub+zip"));
    }

    private void saveContainerXml(ZipOutputStream out) throws IOException {
        saveFile(out, CONTAINER_XML_FILE_PATH, org.apache.commons.codec.binary.StringUtils.getBytesUtf8(new StringBuilder()
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n")
                .append("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n")
                .append("   <rootfiles>\n")
                .append("       <rootfile full-path=\"" + BOOK_OPF_FILE_PATH + "\" media-type=\"application/oebps-package+xml\"/>\n")
                .append("   </rootfiles>\n")
                .append("</container>")
                .toString()));
    }

    private void saveBookOPF(ZipOutputStream out) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n")
                .append("<package version=\"2.0\" unique-identifier=\"PrimaryID\" xmlns=\"http://www.idpf.org/2007/opf\">\n")
                .append("    <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n")
                .append("        <dc:title>").append(m_properties.getTitle()).append("</dc:title>\n")
                .append("        <dc:identifier id=\"PrimaryID\" opf:scheme=\"URN\">urn:uuid:").append(m_properties.getID()).append("</dc:identifier>\n")
                .append("        <dc:language xsi:type=\"dcterms:RFC3066\">").append(m_properties.getLanguage()).append("</dc:language>\n");

        if (StringUtils.isNotBlank(m_properties.getAuthor())) {
            stringBuilder
                    .append("        <dc:creator>").append(m_properties.getAuthor()).append("</dc:creator>\n");
        }

        if (StringUtils.isNotBlank(m_properties.getISBN())) {
            stringBuilder
                    .append("        <dc:identifier opf:scheme=\"ISBN\">").append(m_properties.getISBN()).append("</dc:identifier>\n");
        }

        if (StringUtils.isNotBlank(m_properties.getPublisher())) {
            stringBuilder
                    .append("        <dc:publisher>").append(m_properties.getPublisher()).append("</dc:publisher>\n");
        }

        if (StringUtils.isNotBlank(m_properties.getSubject())) {
            stringBuilder
                    .append("        <dc:subject>").append(m_properties.getSubject()).append("</dc:subject>\n");
        }

        if (StringUtils.isNotBlank(m_properties.getDescription())) {
            stringBuilder
                    .append("        <dc:description>").append(m_properties.getDescription()).append("</dc:description>\n");
        }

        stringBuilder
                .append("    </metadata>\n")
                .append("    <manifest>\n")
                .append("        <item id=\"ncx\" href=\"").append(BOOK_NCX_FILE_NAME).append("\" media-type=\"application/x-dtbncx+xml\"/>\n");

        m_mainChapter.emitManifest(stringBuilder);

        int iImage = 0;
        String[] imageNames = m_imageHandler.getImageNames();
        for (String imageName:imageNames) {
            stringBuilder
                    .append("        <item id=\"img").append(++iImage).append("\" href=\"").append(IMAGES_FOLDER).append("/").append(imageName).append("\" media-type=\"").append(Util.getMediaType(imageName)).append("\"/>\n");
        }

        int iFont = 0;
        String[] fontNames = m_fontHandler.getFontNames();
        for (String fontName:fontNames) {
            stringBuilder
                    .append("        <item id=\"font").append(++iFont).append("\" href=\"").append(FONTS_FOLDER).append("/").append(fontName).append("\" media-type=\"").append(Util.getMediaType(fontName)).append("\"/>\n");
        }

        stringBuilder
                .append("        <item id=\"chapter_style\" href=\"").append(CHAPTER_CSS_FILE_NAME).append("\" media-type=\"text/css\"/>\n");

        stringBuilder
                .append("    </manifest>\n")
                .append("    <spine toc=\"ncx\">\n");

        m_mainChapter.emitSpine(stringBuilder);

        stringBuilder
                .append("    </spine>\n")
                .append("</package>");

        saveFile(out, BOOK_OPF_FILE_PATH, org.apache.commons.codec.binary.StringUtils.getBytesUtf8(stringBuilder.toString()));
    }

    private void saveBookNCX(ZipOutputStream out) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<!DOCTYPE ncx PUBLIC\n")
                .append("     \"-//NISO//DTD ncx 2005-1//EN\"\n")
                .append("     \"http://www.daisy.org/z3986/2005/ncx-2005-1.dtd\">\n")
                .append("<ncx version=\"2005-1\" xml:lang=\"").append(m_properties.getLanguage()).append("\" xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">\n")
                .append("  <head>\n")
                .append("    <!-- The following four metadata items are required for all\n")
                .append("        NCX documents, including those conforming to the relaxed\n")
                .append("        constraints of OPS 2.0 -->\n")
                .append("    <meta name=\"dtb:uid\" content=\"").append(m_properties.getID()).append("\"/>\n")
                .append("    <meta name=\"dtb:depth\" content=\"").append(m_depth).append("\"/>\n")
                .append("    <meta name=\"dtb:totalPageCount\" content=\"0\"/>\n")
                .append("    <meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n")
                .append("  </head>\n")
                .append("  <docTitle>\n")
                .append("    <text>").append(m_properties.getTitle()).append("</text>\n")
                .append("  </docTitle>\n")
                .append("  <navMap>\n");

        m_mainChapter.emitNavPoint(stringBuilder, "    ");

        stringBuilder
                .append("  </navMap>\n")
                .append("</ncx>\n");

        saveFile(out, BOOK_NCX_FILE_PATH, org.apache.commons.codec.binary.StringUtils.getBytesUtf8(stringBuilder.toString()));
    }

    private CommandProgress m_progress;
    private int m_numChapters;
    private int m_chapterCounter;
    void progressChapter() {
        if (m_progress != null) {
            m_progress.progress(++m_chapterCounter, 1, m_numChapters);
        }
    }

    private void saveChapters(ZipOutputStream out, CommandProgress progress) throws IOException {
        m_progress = progress;
        m_chapterCounter = 0;
        m_mainChapter.save(out);
    }

    private void saveChapterCSS(ZipOutputStream out) throws IOException {
        String css = m_doc.emitCSS(null);
        saveFile(out, CHAPTER_CSS_FILE_PATH,
                org.apache.commons.codec.binary.StringUtils.getBytesUtf8(css));
    }

    private void saveFonts(ZipOutputStream out, CommandProgress progress) throws IOException {
        HashMap<String, byte[]> fonts = new HashMap<String, byte[]>();

	int iFont = 0;
	for (Map.Entry<String, byte[]> entry:fonts.entrySet()) {
            String fontName = entry.getKey();
            byte[] fontData = entry.getValue();
            saveFile(out, FONTS_PATH + "/" + fontName, fontData);
            progress.progress(++iFont, 1, fonts.size());
	}
    }

    private void saveFile(ZipOutputStream out, String fileName, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        out.putNextEntry(entry);
        out.write(data, 0, data.length);
    }

    private void saveUncompressedFile(ZipOutputStream out, String fileName, byte[] data) throws IOException {
        out.setMethod(ZipOutputStream.STORED);

        ZipEntry entry = new ZipEntry(fileName);
        
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);

        CRC32 crc32 = new CRC32();
        crc32.update(data);
        entry.setCrc(crc32.getValue());

        out.putNextEntry(entry);
        out.write(data, 0, data.length);

        out.setMethod(ZipOutputStream.DEFLATED);
    }

}
