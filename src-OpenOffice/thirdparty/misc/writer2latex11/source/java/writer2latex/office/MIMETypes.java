/************************************************************************
 *
 *  MIMETypes.java
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
 *  Copyright: 2002-2008 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2008-11-24)
 *
 */

package writer2latex.office;

/* Some helpers to handle the MIME types used by OOo
 */

public final class MIMETypes extends writer2latex.api.MIMETypes {
    // OOo MIME types, taken from
    // http://framework.openoffice.org/documentation/mimetypes/mimetypes.html
    public static final String WRITER="application/vnd.sun.xml.writer";
    public static final String CALC="application/vnd.sun.xml.calc";
    public static final String IMPRESS="application/vnd.sun.xml.impress";
    public static final String DRAW="application/vnd.sun.xml.draw";
    public static final String CHART="application/vnd.sun.xml.chart";
    public static final String MATH="application/vnd.sun.xml.math";
    // OpenDocument MIME types (from spec)
    public static final String ODT="application/vnd.oasis.opendocument.text";
    public static final String ODS="application/vnd.oasis.opendocument.spreadsheet";
    public static final String ODP="application/vnd.oasis.opendocument.presentation";
    public static final String ODF="application/vnd.oasis.opendocument.formula";
	
    // zip
    public static final String ZIP="application/zip";

	// Magic signatures for some binary files
    public static final byte[] PNG_SIG = { (byte) 0x89, 0x50, 0x4e, 0x47 }; // .PNG
    public static final byte[] JPEG_SIG = { (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0 };
    public static final byte[] GIF87_SIG = { 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 }; // GIF87a
    public static final byte[] GIF89_SIG = { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 }; // GIF89a
    public static final byte[] TIFF_SIG = { 0x49, 0x49, 0x2A }; // II*
    public static final byte[] BMP_SIG = { 0x42, 0x4d }; // BM
    public static final byte[] WMF_SIG = { (byte) 0xd7, (byte) 0xcd, (byte) 0xc6, (byte) 0x9a };
    public static final byte[] WMF30_SIG = { 1, 0, 9, 0 }; // Old WMF format, not reliable - see below
    public static final byte[] EPS_SIG = { 0x25, 0x21 }; // %!
    public static final byte[] SVM_SIG = { 0x56, 0x43, 0x4c, 0x4d, 0x54, 0x46 }; // VCLMTF
    public static final byte[] ZIP_SIG = { 0x50, 0x4b, 0x03, 0x04 }; // PK..
	
	
    // Preferred file extensions for some files
    public static final String LATEX_EXT = ".tex";
    public static final String BIBTEX_EXT = ".bib";
    public static final String XHTML_EXT = ".html";
    public static final String XHTML_MATHML_EXT = ".xhtml";
    public static final String XHTML_MATHML_XSL_EXT = ".xml";
    public static final String PNG_EXT = ".png";
    public static final String JPEG_EXT = ".jpg"; // this is the default in graphicx.sty
    public static final String GIF_EXT = ".gif";
    public static final String TIFF_EXT = ".tif";
    public static final String BMP_EXT = ".bmp";
    public static final String WMF_EXT = ".wmf";
    public static final String EPS_EXT = ".eps";
    public static final String SVM_EXT = ".svm";
    public static final String PDF_EXT = ".pdf";
	
    private static final boolean isType(byte[] blob, byte[] sig) {
        int n = sig.length;
        for (int i=0; i<n; i++) {
            if (blob[i]!=sig[i]) { return false; }
        }
        return true;
    }
	
    public static final String getMagicMIMEType(byte[] blob) {
        if (isType(blob,PNG_SIG)) { return PNG; }
        if (isType(blob,JPEG_SIG)) { return JPEG; }
        if (isType(blob,GIF87_SIG)) { return GIF; }
        if (isType(blob,GIF89_SIG)) { return GIF; }
        if (isType(blob,TIFF_SIG)) { return TIFF; }
        if (isType(blob,BMP_SIG)) { return BMP; }
        if (isType(blob,WMF_SIG)) { return WMF; }
        if (isType(blob,WMF30_SIG)) { return WMF; } // do not trust this..
        if (isType(blob,EPS_SIG)) { return EPS; }
        if (isType(blob,SVM_SIG)) { return SVM; }
        if (isType(blob,ZIP_SIG)) { return ZIP; }
        return "";
    }
	
    public static final String getFileExtension(String sMIME) {
        if (PNG.equals(sMIME)) { return PNG_EXT; }
        if (JPEG.equals(sMIME)) { return JPEG_EXT; }
        if (GIF.equals(sMIME)) { return GIF_EXT; }
        if (TIFF.equals(sMIME)) { return TIFF_EXT; }
        if (BMP.equals(sMIME)) { return BMP_EXT; }
        if (WMF.equals(sMIME)) { return WMF_EXT; }
        if (EPS.equals(sMIME)) { return EPS_EXT; }
        if (SVM.equals(sMIME)) { return SVM_EXT; }
        if (PDF.equals(sMIME)) { return PDF_EXT; }
        if (LATEX.equals(sMIME)) { return LATEX_EXT; }
        if (BIBTEX.equals(sMIME)) { return BIBTEX_EXT; }
        if (XHTML.equals(sMIME)) { return XHTML_EXT; }
        if (XHTML_MATHML.equals(sMIME)) { return XHTML_MATHML_EXT; }
        if (XHTML_MATHML_XSL.equals(sMIME)) { return XHTML_MATHML_XSL_EXT; }
        return "";
    }		
	
    public static boolean isVectorFormat(String sMIME) {
        return WMF.equals(sMIME) || EPS.equals(sMIME) || SVM.equals(sMIME) || PDF.equals(sMIME);
    }


}

/* Notes on old WMF format:
 Found on math type objects: 1 0 - 9 0 - 0 3 - 90 1 - 0 0

 Explanation from http://wvware.sourceforge.net/caolan/ora-wmf.html :
 The standard Windows metafile header is 18 bytes in length and is structured as follows:

typedef struct _WindowsMetaHeader
{
  WORD  FileType;        Type of metafile (0=memory, 1=disk) 
  WORD  HeaderSize;      Size of header in WORDS (always 9) 
  WORD  Version;         Version of Microsoft Windows used 
  DWORD FileSize;        Total size of the metafile in WORDs 
  WORD  NumOfObjects;    Number of objects in the file 
  DWORD MaxRecordSize;   The size of largest record in WORDs 
  WORD  NumOfParams;     Not Used (always 0) 
} WMFHEAD;

FileType contains a value which indicates the location of the metafile data. A value of 0 indicates that the metafile is stored in memory, while a 1 indicates that it is stored on disk.

HeaderSize contains the size of the metafile header in 16-bit WORDs. This value is always 9.

Version stores the version number of Microsoft Windows that created the metafile. This value is always read in hexadecimal format. For example, in a metafile created by Windows 3.0 and 3.1, this item would have the value 0x0300.

FileSize specifies the total size of the metafile in 16-bit WORDs.

NumOfObjects specifies the number of objects that are in the metafile.

MaxRecordSize specifies the size of the largest record in the metafile in WORDs.

NumOfParams is not used and is set to a value of 0.

*/ 
