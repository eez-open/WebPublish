/************************************************************************
 *
 *  BibTeXDocument.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2009-03-08)
 *
 */

package writer2latex.bibtex;

import writer2latex.xmerge.Document;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import writer2latex.api.ConverterFactory;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.latex.i18n.I18n;
import writer2latex.util.ExportNameCollection;
//import writer2latex.util.Misc;
import writer2latex.office.BibMark;

/**
 * <p>Class representing a BibTeX document.</p>
 *
 */
public class BibTeXDocument implements Document {
    private static final String FILE_EXTENSION = ".bib";
	
    private String sName;
    private Hashtable entries = new Hashtable();
    private ExportNameCollection exportNames = new ExportNameCollection(true);
    private I18n i18n;

    /**
     * <p>Constructs a new BibTeX Document.</p>
     *
     * <p>This new document is empty. Bibliographic data must added
     *    using the <code>put</code> method.</p>
     *
     * @param   sName    The name of the <code>BibTeXDocument</code>.
     */
    public BibTeXDocument(String sName) {
        this.sName = trimDocumentName(sName);
        // Use default config (only ascii, no extra font packages)
        i18n = new ClassicI18n(new LaTeXConfig());
    }
    
    /**
     * <p>This method is supposed to read <code>byte</code> data from the InputStream.
     * Currently it does nothing, since we don't need it.</p>
     * 
     * @param   is      InputStream containing a BibTeX data file.
     *
     * @throws  IOException     In case of any I/O errors.
     */
    public void read(InputStream is) throws IOException {
        // Do nothing.
    }
  
    
    /**
     * <p>Returns the <code>Document</code> name with no file extension.</p>
     *
     * @return  The <code>Document</code> name with no file extension.
     */
    public String getName() {
        return sName;
    }
    
    
    /**
     * <p>Returns the <code>Document</code> name with file extension.</p>
     *
     * @return  The <code>Document</code> name with file extension.
     */
    public String getFileName() {
        return new String(sName + FILE_EXTENSION);
    }
    
    
    /**
     * <p>Writes out the <code>Document</code> content to the specified
     * <code>OutputStream</code>.</p>
     *
     * <p>This method may not be thread-safe.
     * Implementations may or may not synchronize this
     * method.  User code (i.e. caller) must make sure that
     * calls to this method are thread-safe.</p>
     *
     * @param  os  <code>OutputStream</code> to write out the
     *             <code>Document</code> content.
     *
     * @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {
        // BibTeX files are plain ascii
        OutputStreamWriter osw = new OutputStreamWriter(os,"ASCII");
        osw.write("%% This file was converted to BibTeX by Writer2BibTeX ver. "+ConverterFactory.getVersion()+".\n");
        osw.write("%% See http://writer2latex.sourceforge.net for more info.\n");
        osw.write("\n");
        Enumeration enumeration = entries.elements();
        while (enumeration.hasMoreElements()) {
            BibMark entry = (BibMark) enumeration.nextElement();
            osw.write("@");
            osw.write(entry.getEntryType().toUpperCase());
            osw.write("{");
            osw.write(exportNames.getExportName(entry.getIdentifier()));
            osw.write(",\n");
            for (int i=0; i<BibMark.FIELD_COUNT; i++) {
                String sValue = entry.getField(i);
                if (sValue!=null) {
                    if (i==BibMark.AUTHOR || i==BibMark.EDITOR) {
                        // OOo uses ; to separate authors and editors - BibTeX uses and
                        sValue = sValue.replaceAll(";" , " and ");
                    }
                    osw.write("    ");
                    osw.write(getFieldName(i).toUpperCase());
                    osw.write(" = {");
                    for (int j=0; j<sValue.length(); j++) {
                        String s = i18n.convert(Character.toString(sValue.charAt(j)),false,"en");
                        if (s.charAt(0)=='\\') { osw.write("{"); }
                        osw.write(s);
                        if (s.charAt(0)=='\\') { osw.write("}"); }
                    }
                    osw.write("},\n");
                }
            }
            osw.write("}\n\n");
        }
        osw.flush();
        osw.close();
    }	

    /** 
     * <p> Return BibTeX name of field </p>
     */
    public static final String getFieldName(int nField) {
        switch (nField) {
            case BibMark.ADDRESS: return "address";
            case BibMark.ANNOTE: return "annote";  
            case BibMark.AUTHOR: return "author";  
            case BibMark.BOOKTITLE: return "booktitle";  
            case BibMark.CHAPTER: return "chapter";  
            // case BibMark.CROSSREF: return "croosref"; // not in OOo  
            case BibMark.EDITION: return "edition";  
            case BibMark.EDITOR: return "editor";  
            case BibMark.HOWPUBLISHED: return "howpublished";  
            case BibMark.INSTITUTION: return "institution";  
            case BibMark.JOURNAL: return "journal";  
            // case BibMark.KEY: return "key";  // not in OOo
            case BibMark.MONTH: return "month";  
            case BibMark.NOTE: return "note";  
            case BibMark.NUMBER: return "number";  
            case BibMark.ORGANIZATIONS: return "organization";  
            case BibMark.PAGES: return "pages";
            case BibMark.PUBLISHER: return "publisher";  
            case BibMark.SCHOOL: return "school";  
            case BibMark.SERIES: return "series";  
            case BibMark.TITLE: return "title";  
            case BibMark.REPORT_TYPE: return "type";
            case BibMark.VOLUME: return "volume";  
            case BibMark.YEAR: return "year";
            case BibMark.URL: return "url"; 
            case BibMark.CUSTOM1: return "custom1"; 
            case BibMark.CUSTOM2: return "custom2";  
            case BibMark.CUSTOM3: return "custom3";  
            case BibMark.CUSTOM4: return "custom4";  
            case BibMark.CUSTOM5: return "custom5";  
            case BibMark.ISBN: return "isbn";
            default: return null;
        }
    }


    /*
     * <p>Check if this entry exists</p>
     */
    public boolean containsKey(String sIdentifier) {
        return entries.containsKey(sIdentifier);
    }

    /*
     * <p>Add an entry</p>
     */
    public void put(BibMark entry) {
        entries.put(entry.getIdentifier(),entry);
        exportNames.addName(entry.getIdentifier());
    }

    /*
     * <p>Get export name for an identifier</p>
     */
    public String getExportName(String sIdentifier) {
        return exportNames.getExportName(sIdentifier);
    }

    /*
     * Utility method to make sure the document name is stripped of any file
     * extensions before use.
     */
    private String trimDocumentName(String name) {
        String temp = name.toLowerCase();
        
        if (temp.endsWith(FILE_EXTENSION)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - FILE_EXTENSION.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }
}
    