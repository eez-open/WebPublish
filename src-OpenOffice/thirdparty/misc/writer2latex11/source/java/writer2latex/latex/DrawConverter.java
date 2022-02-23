/************************************************************************
 *
 *  DrawConverter.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0.1 (2010-03-01)
 *
 */
 
package writer2latex.latex;

import java.util.LinkedList;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
//import org.w3c.dom.Node;

import writer2latex.xmerge.EmbeddedObject;
import writer2latex.xmerge.EmbeddedXMLObject;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
//import writer2latex.office.ImageLoader;
import writer2latex.office.MIMETypes;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Misc;
import writer2latex.xmerge.BinaryGraphicsDocument;

/**
 *  <p>This class handles draw elements.</p>
 */
public class DrawConverter extends ConverterHelper {

    private boolean bNeedGraphicx = false;
    private boolean bNeedOOoLaTeXPreamble = false;

    // Keep track of floating frames (images, textboxes...)
    private Stack floatingFramesStack = new Stack();
	
    private Element getFrame(Element onode) {
        if (ofr.isOpenDocument()) return (Element) onode.getParentNode();
        else return onode;
    }
	
    public DrawConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        floatingFramesStack.push(new LinkedList());
    }

    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bNeedGraphicx) { 
            pack.append("\\usepackage");
            if (config.getBackend()==LaTeXConfig.PDFTEX) pack.append("[pdftex]");
            //else if (config.getBackend()==LaTeXConfig.XETEX) pack.append("[xetex]");
            else if (config.getBackend()==LaTeXConfig.DVIPS) pack.append("[dvips]");
            pack.append("{graphicx}").nl();
        }
        if (bNeedOOoLaTeXPreamble) {
            // The preamble may be stored in the description
            String sDescription = palette.getMetaData().getDescription();
            int nStart = sDescription.indexOf("%%% OOoLatex Preamble %%%%%%%%%%%%%%");
            int nEnd = sDescription.indexOf("%%% End OOoLatex Preamble %%%%%%%%%%%%");
            if (nStart>-1 && nEnd>nStart) {
                decl.append("% OOoLaTeX preamble").nl()
                    .append(sDescription.substring(nStart+37,nEnd));
            }
            // TODO: Otherwise try the user settings...
        }
    }
	
    public void handleCaption(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // Floating frames should be positioned *above* the label, hence
        // we use a separate ldp for the paragraphs and add this later
        LaTeXDocumentPortion capLdp = new LaTeXDocumentPortion(true);

        // Convert the caption
        if (oc.isInFigureFloat()) { // float
            capLdp.append("\\caption");
            palette.getCaptionCv().handleCaptionBody(node,capLdp,oc,false);
        }
        else { // nonfloat
            capLdp.append("\\captionof{figure}");
            palette.getCaptionCv().handleCaptionBody(node,capLdp,oc,true);
        }
		
        flushFloatingFrames(ldp,oc);
        ldp.append(capLdp);
    }
	
    public void handleDrawElement(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // node must be an elment in the draw namespace
        String sName = node.getTagName();
        if (sName.equals(XMLString.DRAW_OBJECT)) {
            handleDrawObject(node,ldp,oc);
        }		
        else if (sName.equals(XMLString.DRAW_OBJECT_OLE)) {
            handleDrawObject(node,ldp,oc);
        }		
        else if ((!oc.isInHeaderFooter()) && sName.equals(XMLString.DRAW_IMAGE)) {
            handleDrawImage(node,ldp,oc);
        }		
        else if ((!oc.isInHeaderFooter()) && sName.equals(XMLString.DRAW_TEXT_BOX)) {
            handleDrawTextBox(node,ldp,oc);
        }		
        else if (sName.equals(XMLString.DRAW_A)) {
            // we handle this like text:a
            palette.getFieldCv().handleAnchor(node,ldp,oc);
        }
        else if (sName.equals(XMLString.DRAW_FRAME)) {
            // OpenDocument: Get the actual draw element in the frame
            handleDrawElement(Misc.getFirstChildElement(node),ldp,oc);
        }
        else {
            // Other drawing objects (eg. shapes) are currently not supported
            ldp.append("[Warning: Draw object ignored]");
        }
    }
	
    //-----------------------------------------------------------------
    // handle draw:object elements (OOo objects such as Chart, Math,...)
		
    private void handleDrawObject(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sHref = Misc.getAttribute(node,XMLString.XLINK_HREF);
		
        if (sHref!=null) { // Embedded object in package or linked object
            if (ofr.isInPackage(sHref)) { // Embedded object in package
                if (sHref.startsWith("#")) { sHref=sHref.substring(1); }
                if (sHref.startsWith("./")) { sHref=sHref.substring(2); }
                EmbeddedObject object = palette.getEmbeddedObject(sHref); 
                if (object!=null) {
                    if (MIMETypes.MATH.equals(object.getType()) || MIMETypes.ODF.equals(object.getType())) { // Formula!
                        try {
                            Document settings = ((EmbeddedXMLObject) object).getSettingsDOM();
                            Document formuladoc = ((EmbeddedXMLObject) object).getContentDOM();
                            Element formula = Misc.getChildByTagName(formuladoc,XMLString.MATH); // Since OOo3.2
                            if (formula==null) {
                            	formula = Misc.getChildByTagName(formuladoc,XMLString.MATH_MATH);
                            }
                            ldp.append(" $")
                               .append(palette.getMathmlCv().convert(settings,formula))
                               .append("$");
                            if (Character.isLetterOrDigit(OfficeReader.getNextChar(node))) { ldp.append(" "); }
                        }
                        catch (org.xml.sax.SAXException e) {
                            e.printStackTrace();
                        }
                        catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
	                }
                    else { // unsupported object
                        boolean bIgnore = true;
                        if (ofr.isOpenDocument()) { // look for replacement image
                            Element replacementImage = Misc.getChildByTagName(getFrame(node),XMLString.DRAW_IMAGE);
                            if (replacementImage!=null) {
                                handleDrawImage(replacementImage,ldp,oc);
                                bIgnore = false;
                            }
                        }
                        if (bIgnore) { 
                            ldp.append("[Warning: object ignored]");
                        }
                    }

                }
            }
        }
        else { // flat xml, object is contained in node
            Element formula = Misc.getChildByTagName(node,XMLString.MATH); // Since OOo 3.2
            if (formula==null) {
            	formula = Misc.getChildByTagName(node,XMLString.MATH_MATH);
            }
            if (formula!=null) {
                ldp.append(" $")
                   .append(palette.getMathmlCv().convert(null,formula))
                   .append("$");
                if (Character.isLetterOrDigit(OfficeReader.getNextChar(node))) { ldp.append(" "); }
            }
            else { // unsupported object
                boolean bIgnore = true;
                if (ofr.isOpenDocument()) { // look for replacement image
                    Element replacementImage = Misc.getChildByTagName(getFrame(node),XMLString.DRAW_IMAGE);
                    if (replacementImage!=null) {
                        handleDrawImage(replacementImage,ldp,oc);
                        bIgnore = false;
                    }
                }
                if (bIgnore) { 
                    ldp.append("[Warning: object ignored]");
                }
            }

        }

    }
	
    //--------------------------------------------------------------------------
    // Create float environment
    private void applyFigureFloat(BeforeAfter ba, Context oc) {
        // todo: check context...
        if (config.floatFigures() && !oc.isInFrame() && !oc.isInTable()) {
           if (oc.isInMulticols()) {
               ba.add("\\begin{figure*}","\\end{figure*}\n");
           }
           else {
               ba.add("\\begin{figure}","\\end{figure}\n");
           }
           if (config.getFloatOptions().length()>0) {
               ba.add("["+config.getFloatOptions()+"]","");
           }
           ba.add("\n","");
           oc.setInFigureFloat(true);
        }
        if (!oc.isInFrame() && config.alignFrames()) {
            // Avoid nesting center environment
        	if (config.floatFigures()) {
        		// Inside floats we don't want the extra glue added by the center environment
        		ba.add("\\centering\n","\n");
        	}
        	else {
        		// Outside a float we certainly want it
                ba.add("\\begin{center}\n","\n\\end{center}\n");
        	}
        }
	
    }
	
    //--------------------------------------------------------------------------
    // Handle draw:image elements
	
    private void handleDrawImage(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // Include graphics if allowed by the configuration
        switch (config.imageContent()) {
        case LaTeXConfig.IGNORE:
            // Ignore graphics silently
            return;
        case LaTeXConfig.WARNING:
            System.err.println("Warning: Images are not allowed");
            return;
        case LaTeXConfig.ERROR:
            ldp.append("% Error in document: An image was ignored");
            return;
        }

        Element frame = getFrame(node);
        String sName = frame.getAttribute(XMLString.DRAW_NAME);
        palette.getFieldCv().addTarget(sName,"|graphic",ldp);
        String sAnchor = frame.getAttribute(XMLString.TEXT_ANCHOR_TYPE);

        // TODO: Recognize Jex equations (needs further testing of Jex)
        /*Element desc = Misc.getChildByTagName(frame,XMLString.SVG_DESC);
        if (desc!=null) {
            String sDesc = Misc.getPCDATA(desc);
            if (sDesc.startsWith("jex149$tex={") && sDesc.endsWith("}")) {
                String sTeX = sDesc.substring(12,sDesc.length()-1);
                if (sTeX.length()>0) {
                    // Succesfully extracted Jex equation!
                    ldp.append("$"+sTeX+"$"); 
                    return;
                }
            }
        }*/
		
        // Recognize OOoLaTeX equation
        // The LaTeX code is embedded in a custom style attribute:
        StyleWithProperties style = ofr.getFrameStyle(
            Misc.getAttribute(frame, XMLString.DRAW_STYLE_NAME));
        if (style!=null) {
            String sOOoLaTeX = style.getProperty("OOoLatexArgs");
            // The content of the attribute is <point size><paragraph sign><mode><paragraph sign><TeX code>
            int n=0;
            if (sOOoLaTeX!=null) {
                if ((n=sOOoLaTeX.indexOf("\u00A7display\u00A7"))>-1) {
                    ldp.append("\\[").append(sOOoLaTeX.substring(n+9)).append("\\]");
                    bNeedOOoLaTeXPreamble = true;
                    return;
                }
                else if ((n=sOOoLaTeX.indexOf("\u00A7inline\u00A7"))>-1) {
                    ldp.append("$").append(sOOoLaTeX.substring(n+8)).append("$");
                    bNeedOOoLaTeXPreamble = true;
                    return;
                }
                else if ((n=sOOoLaTeX.indexOf("\u00A7text\u00A7"))>-1) {
                    ldp.append(sOOoLaTeX.substring(n+6));
                    bNeedOOoLaTeXPreamble = true;
                    return;
                }
            }
        } 
        
        //if (oc.isInFrame() || "as-char".equals(sAnchor)) {
        if ("as-char".equals(sAnchor)) {
            handleDrawImageAsChar(node,ldp,oc);
        }
        else {
            ((LinkedList) floatingFramesStack.peek()).add(node);
        }
    }
	
    private void handleDrawImageAsChar(Element node, LaTeXDocumentPortion ldp, Context oc) {
        ldp.append(" ");
        includeGraphics(node,ldp,oc);
        ldp.append(" ");
    }

    private void handleDrawImageFloat(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();
        BeforeAfter ba = new BeforeAfter();

        applyFigureFloat(ba,ic);
		
        ldp.append(ba.getBefore());
        includeGraphics(node,ldp,ic);
        ldp.append(ba.getAfter());
    }

    private void includeGraphics(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFileName = null;
        boolean bCommentOut = true;
        String sHref = node.getAttribute(XMLString.XLINK_HREF);
		
        if (node.hasAttribute(XMLString.XLINK_HREF) && !ofr.isInPackage(sHref)) {
            // Linked image is not yet handled by ImageLoader. This is a temp.
            // solution (will go away when ImageLoader is finished)
            sFileName = sHref;
            // In OpenDocument package format ../ means "leave the package"
            if (ofr.isOpenDocument() && ofr.isPackageFormat() && sFileName.startsWith("../")) {
                sFileName=sFileName.substring(3);
            }
            int nExtStart = sHref.lastIndexOf(".");
            String sExt = nExtStart>=0 ? sHref.substring(nExtStart).toLowerCase() : "";
            // Accept only relative filenames and supported filetypes:
            bCommentOut = sFileName.indexOf(":")>-1 || !(
                config.getBackend()==LaTeXConfig.UNSPECIFIED ||
                (config.getBackend()==LaTeXConfig.PDFTEX && MIMETypes.JPEG_EXT.equals(sExt)) ||
                (config.getBackend()==LaTeXConfig.PDFTEX && MIMETypes.PNG_EXT.equals(sExt)) ||
                (config.getBackend()==LaTeXConfig.PDFTEX && MIMETypes.PDF_EXT.equals(sExt)) ||
                (config.getBackend()==LaTeXConfig.XETEX && MIMETypes.JPEG_EXT.equals(sExt)) ||
                (config.getBackend()==LaTeXConfig.XETEX && MIMETypes.PNG_EXT.equals(sExt)) ||
                (config.getBackend()==LaTeXConfig.XETEX && MIMETypes.PDF_EXT.equals(sExt)) ||
                (config.getBackend()==LaTeXConfig.DVIPS && MIMETypes.EPS_EXT.equals(sExt)));
        }
        else { // embedded or base64 encoded image
            BinaryGraphicsDocument bgd = palette.getImageLoader().getImage(node);
            if (bgd!=null) {
                palette.addDocument(bgd);
                sFileName = bgd.getFileName();
                String sMIME = bgd.getDocumentMIMEType();
                bCommentOut = !(
                    config.getBackend()==LaTeXConfig.UNSPECIFIED ||
                    (config.getBackend()==LaTeXConfig.PDFTEX && MIMETypes.JPEG.equals(sMIME)) ||
                    (config.getBackend()==LaTeXConfig.PDFTEX && MIMETypes.PNG.equals(sMIME)) ||
                    (config.getBackend()==LaTeXConfig.PDFTEX && MIMETypes.PDF.equals(sMIME)) ||
                    (config.getBackend()==LaTeXConfig.XETEX && MIMETypes.JPEG.equals(sMIME)) ||
                    (config.getBackend()==LaTeXConfig.XETEX && MIMETypes.PNG.equals(sMIME)) ||
                    (config.getBackend()==LaTeXConfig.XETEX && MIMETypes.PDF.equals(sMIME)) ||
                    (config.getBackend()==LaTeXConfig.DVIPS && MIMETypes.EPS.equals(sMIME)));
            }
        }
		
        if (sFileName==null) {
            ldp.append("[Warning: Image not found]");
            return;
        }
		
        // Now for the actual inclusion:
        bNeedGraphicx = true;
        /* TODO: handle cropping and mirror:
           style:mirror can be none, vertical (lodret), horizontal (vandret),
           horizontal-on-odd, or
           horizontal-on-even (horizontal on odd or even pages).
   		   mirror is handled with scalebox, eg:
        		%\\scalebox{-1}[1]{...}
		   can check for even/odd page first!!
	
          fo:clip="rect(t,r,b,l) svarer til trim
          value can be auto - no clip!
		  cropping is handled with clip and trim:
		  \\includegraphics[clip,trim=l b r t]{...}
		  note the different order from xsl-fo!
         */

        if (bCommentOut) {
            ldp.append(" [Warning: Image ignored] ");
            ldp.append("% Unhandled or unsupported graphics:").nl().append("%");
        }
        ldp.append("\\includegraphics");

        CSVList options = new CSVList(',');
        if (!config.originalImageSize()) {
            Element frame = getFrame(node);
            String sWidth = Misc.truncateLength(frame.getAttribute(XMLString.SVG_WIDTH));
            String sHeight = Misc.truncateLength(frame.getAttribute(XMLString.SVG_HEIGHT));
            if (sWidth!=null) { options.addValue("width="+sWidth); }
            if (sHeight!=null) { options.addValue("height="+sHeight); }
        }
        if (config.getImageOptions().length()>0) {
            options.addValue(config.getImageOptions()); // TODO: New CSVList...
        }
        if (!options.isEmpty()) {
            ldp.append("[").append(options.toString()).append("]");
        }

        if (config.removeGraphicsExtension()) {
            sFileName = Misc.removeExtension(sFileName);
        }
        ldp.append("{").append(sFileName).append("}");
        if (bCommentOut) { ldp.nl(); }
    }

    //--------------------------------------------------------------------------
    // handle draw:text-box element
	
    private void handleDrawTextBox(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Element frame = getFrame(node);
        String sName = frame.getAttribute(XMLString.DRAW_NAME);
        palette.getFieldCv().addTarget(sName,"|frame",ldp);
        String sAnchor = frame.getAttribute(XMLString.TEXT_ANCHOR_TYPE);
        //if (oc.isInFrame() || "as-char".equals(sAnchor)) {
        if ("as-char".equals(sAnchor)) {
            makeDrawTextBox(node, ldp, oc);
        }
        else {
            ((LinkedList) floatingFramesStack.peek()).add(node);
        }
    }
	
    private void handleDrawTextBoxFloat(Element node, LaTeXDocumentPortion ldp, Context oc) {
        BeforeAfter ba = new BeforeAfter();
        Context ic = (Context) oc.clone();

        applyFigureFloat(ba,ic);
		
        ldp.append(ba.getBefore());
        makeDrawTextBox(node, ldp, ic);
        ldp.append(ba.getAfter());
    }

    private void makeDrawTextBox(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();
        ic.setInFrame(true);
        ic.setNoFootnotes(true);
		
        // Check to see, if this is really a container for a figure caption
        boolean bIsCaption = false;
        if (OfficeReader.isSingleParagraph(node)) {
            Element par = Misc.getFirstChildElement(node);
            String sSeqName = ofr.getSequenceName(par);
            if (ofr.isFigureSequenceName(sSeqName)) { bIsCaption = true; }
        }

        String sWidth = Misc.truncateLength(getFrame(node).getAttribute(XMLString.SVG_WIDTH));
        if (!bIsCaption) {
            ldp.append("\\begin{minipage}{").append(sWidth).append("}").nl();
        }
        floatingFramesStack.push(new LinkedList());
        palette.getBlockCv().traverseBlockText(node,ldp,ic);
        flushFloatingFrames(ldp,ic);
        floatingFramesStack.pop();
        if (!bIsCaption) {
            ldp.append("\\end{minipage}");
        }
        if (!oc.isNoFootnotes()) { palette.getNoteCv().flushFootnotes(ldp,oc); }

    }

    //-------------------------------------------------------------------------
    //handle any pending floating frames
    
    public void flushFloatingFrames(LaTeXDocumentPortion ldp, Context oc) {
	    // todo: fix language
        LinkedList floatingFrames = (LinkedList) floatingFramesStack.peek();
        int n = floatingFrames.size();
        if (n==0) { return; }
        for (int i=0; i<n; i++) {
            Element node = (Element) floatingFrames.get(i);
            String sName = node.getNodeName();
            if (sName.equals(XMLString.DRAW_IMAGE)) {
                handleDrawImageFloat(node,ldp,oc);
            }
            else if (sName.equals(XMLString.DRAW_TEXT_BOX)) {
                handleDrawTextBoxFloat(node,ldp,oc);
            }
        }
        floatingFrames.clear();
    }
	
}