/************************************************************************
 *
 *  ImageLoader.java
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
 *  Version 1.0 (2008-11-22)
 *
 */

package writer2latex.office;

//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
import java.util.HashSet;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2latex.api.GraphicConverter;
import writer2latex.util.Base64;
import writer2latex.util.Misc;
import writer2latex.xmerge.BinaryGraphicsDocument;
import writer2latex.xmerge.EmbeddedObject;
import writer2latex.xmerge.EmbeddedBinaryObject;
import writer2latex.xmerge.OfficeDocument;

//import writer2latex.util.*;

/**
 *  <p>This class extracts images from an OOo file.
 *  The images are returned as BinaryGraphicsDocument.</p>
 */
public final class ImageLoader {
    // The Office document to load images from
    private OfficeDocument oooDoc;
	
    // Data for file name generation
    private String sOutFileName;
    private boolean bUseSubdir = false;
    private int nImageCount = 0;
	
    // should EPS be extracted from SVM?
    private boolean bExtractEPS;
	
    // Data for image conversion
    private GraphicConverter gcv = null;
    private boolean bAcceptOtherFormats = true;
    private String sDefaultFormat = null;
    private String sDefaultVectorFormat = null;
    private HashSet acceptedFormats = new HashSet();

    public ImageLoader(OfficeDocument oooDoc, String sOutFileName, boolean bExtractEPS) {
        this.oooDoc = oooDoc;
        this.sOutFileName = sOutFileName;
        this.bExtractEPS = bExtractEPS;
    }
	
    public void setOutFileName(String sOutFileName) { this.sOutFileName = sOutFileName; }
    
    public void setUseSubdir(boolean bUseSubdir) { this.bUseSubdir = bUseSubdir; }
	
    public void setAcceptOtherFormats(boolean b) { bAcceptOtherFormats = b; }
	
    public void setDefaultFormat(String sMime) {
        addAcceptedFormat(sMime);
        sDefaultFormat = sMime;
    }
	
    public void setDefaultVectorFormat(String sMime) {
        addAcceptedFormat(sMime);
        sDefaultVectorFormat = sMime;
    }
	
    public void addAcceptedFormat(String sMime) { acceptedFormats.add(sMime); }
	
    private boolean isAcceptedFormat(String sMime) { return acceptedFormats.contains(sMime); }
	
    public void setGraphicConverter(GraphicConverter gcv) { this.gcv = gcv; }
	
    public BinaryGraphicsDocument getImage(Node node) {
        // node must be a draw:image element.
        // variables to hold data about the image:
        String sMIME = null;
        String sExt = null;
        byte[] blob = null;

        String sHref = Misc.getAttribute(node,XMLString.XLINK_HREF);
        if (sHref==null || sHref.length()==0) {
            // Image must be contained in an office:binary-element as base64:
            Node obd = Misc.getChildByTagName(node,XMLString.OFFICE_BINARY_DATA);
            if (obd!=null) {
                StringBuffer buf = new StringBuffer();
                NodeList nl = obd.getChildNodes();
                int nLen = nl.getLength();
                for (int i=0; i<nLen; i++) {
                    if (nl.item(i).getNodeType()==Node.TEXT_NODE) {
                        buf.append(nl.item(i).getNodeValue());
                    }
                }
                blob = Base64.decode(buf.toString());
                sMIME = MIMETypes.getMagicMIMEType(blob);
                sExt = MIMETypes.getFileExtension(sMIME);
            }
        }
        else {
            // Image may be embedded in package:
            if (sHref.startsWith("#")) { sHref = sHref.substring(1); }
            if (sHref.startsWith("./")) { sHref = sHref.substring(2); }
            EmbeddedObject obj = oooDoc.getEmbeddedObject(sHref);
            if (obj!=null && obj instanceof EmbeddedBinaryObject) {
                EmbeddedBinaryObject object = (EmbeddedBinaryObject) obj;
                blob = object.getBinaryData();
                sMIME = object.getType();
                sExt = MIMETypes.getFileExtension(sMIME);
            }
            else {
                // This is a linked image
                // TODO: Perhaps we should download the image from the url in sHref?
                // Alternatively BinaryGraphicsDocument should be extended to
                // handle external graphics.
            }
        }

        if (blob==null) { return null; }

        // Assign a name (without extension) 
        String sName = sOutFileName+"-img"+(++nImageCount);
        if (bUseSubdir) { sName = sOutFileName + "-img/" + sName; }
     
        BinaryGraphicsDocument bgd = null;

        if (bExtractEPS && MIMETypes.SVM.equals(MIMETypes.getMagicMIMEType(blob))) {
            // Look for postscript:
            int[] offlen = new int[2];
            if (SVMReader.readSVM(blob,offlen)) {
                bgd = new BinaryGraphicsDocument(sName,
                             MIMETypes.EPS_EXT,MIMETypes.EPS);
                bgd.read(blob,offlen[0],offlen[1]);
             }
        }

        if (bgd==null) {
            // If we have a converter AND a default format AND this image
            // is not in an accepted format AND the converter knows how to
            // convert it - try to convert...
            if (gcv!=null && !isAcceptedFormat(sMIME) && sDefaultFormat!=null) {
                String sTargetMIME = null;

                if (MIMETypes.isVectorFormat(sMIME) && sDefaultVectorFormat!=null &&
                    gcv.supportsConversion(sMIME,sDefaultVectorFormat,false,false)) {
                    sTargetMIME = sDefaultVectorFormat;
                }
                else if (gcv.supportsConversion(sMIME,sDefaultFormat,false,false)) {
                    sTargetMIME = sDefaultFormat;
                }

                if (sTargetMIME!=null) {
                    byte[] newBlob = gcv.convert(blob,sMIME,sTargetMIME);
                    if (newBlob!=null) {
                        // Conversion succesful - create new data
                        blob = newBlob;
                        sMIME = sTargetMIME;
                        sExt = MIMETypes.getFileExtension(sMIME);
                    }
                }
            }

            if (isAcceptedFormat(sMIME) || bAcceptOtherFormats) {
                bgd = new BinaryGraphicsDocument(sName,sExt,sMIME);
                bgd.read(blob);
            }
        }
		
        return bgd;
    }
}
