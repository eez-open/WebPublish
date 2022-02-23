/************************************************************************
 *
 *  BibConverter.java
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
 *  Version 1.0 (2009-08-31)
 *
 */

package writer2latex.latex;

import org.w3c.dom.Element;

import writer2latex.bibtex.BibTeXDocument;

import writer2latex.latex.util.Context;

import writer2latex.office.BibMark;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;

import writer2latex.util.Misc;

/**
 *  This class handles the bibliography. The result depends on these
 *  configuration options. The citations will be treated like this:
 *  <ul>
 *  <li><code>use_bibtex</code>: If true, citations will be exported as \cite
 *  commands. If false, citations will be exported as static text</li>
 *  </ul>
 *  The bibliography will be treated like this:
 *  <ul>
 *  <li><code>use_index</code>: If false, the bibliography will be omitted</li>
 *  <li><code>use_bibtex</code> true and <code>external_bibtex_files</code>
 *  empty: The citations will be exported to a BibTeX file, which will be used
 *  for the bibliography</li>
 *  <li><code>use_bibtex</code> true and <code>external_bibtex_files</code>
 *  non-empty: The citations will be not be exported to a BibTeX file, the
 *  files referred to by the option will be used instead</li>
 *  <li><code>use_bibtex</code> false: The bibliography will be exported as
 *  static text.
 *  <li><code>bibtex_style</code> If BibTeX is used, this style will be applied
 *  </ul>
 */
public class BibConverter extends ConverterHelper {

    private BibTeXDocument bibDoc;

    /** Construct a new BibConverter.
     * @param config the configuration to use 
     * @param palette the ConverterPalette to use
     */
    public BibConverter(OfficeReader ofr,LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
    }

    /** Append declarations needed by the <code>BibConverter</code> to
     * the preamble.
     * @param pack the LaTeXDocumentPortion to which
     * declarations of packages should be added (\\usepackage).
     * @param decl the LaTeXDocumentPortion to which
     * other declarations should be added.
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        // Currently nothing; may add support for eg. natbib later
    }

    /** Process a bibliography (text:bibliography tag)
     * @param node The element containing the Bibliography
     * @param ldp the LaTeXDocumentPortion to which LaTeX code should be added
     * @param oc the current context
     */
    public void handleBibliography (Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.noIndex()) { return; }
        if (config.useBibtex()) {
            // Use the style given in the configuration
            // TODO: Create a bst file from the settings of the text:bibliography
            ldp.append("\\bibliographystyle{")
               .append(config.bibtexStyle())
               .append("}").nl();

            // Use BibTeX file from configuration, or exported BibTeX file
            if (config.externalBibtexFiles().length()>0) {
                ldp.append("\\bibliography{")
                   .append(config.externalBibtexFiles())
                   .append("}").nl();
            }
            else {
                if (bibDoc==null) { bibDoc = new BibTeXDocument(palette.getOutFileName()); }
                ldp.append("\\bibliography{")
                   .append(bibDoc.getName())
                   .append("}").nl();
            }
        }
        else { // typeset current content
            Element body = Misc.getChildByTagName(node,XMLString.TEXT_INDEX_BODY);
            if (body!=null) {
                Element title = Misc.getChildByTagName(body,XMLString.TEXT_INDEX_TITLE);
                if (title!=null) { palette.getBlockCv().traverseBlockText(title,ldp,oc); }
                palette.getBlockCv().traverseBlockText(body,ldp,oc);
            }
        }     
    }
	
    /** Process a Bibliography Mark (text:bibliography-mark tag)
     * @param node The element containing the Mark
     * @param ldp the LaTeXDocumentPortion to which LaTeX code should be added
     * @param oc the current context
     */
    public void handleBibliographyMark(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (config.useBibtex()) {
            String sIdentifier = node.getAttribute(XMLString.TEXT_IDENTIFIER);
            if (sIdentifier!=null) {
                if (config.externalBibtexFiles().length()==0) {
                    if (bibDoc==null) { bibDoc = new BibTeXDocument(palette.getOutFileName()); }
                    if (!bibDoc.containsKey(sIdentifier)) {
                        bibDoc.put(new BibMark(node));
                    }
                }
                // Insert citation: Original if using external files; stripped if exporting BibTeX
                ldp.append("\\cite{")
                   .append(config.externalBibtexFiles().length()==0 ? bibDoc.getExportName(sIdentifier) : sIdentifier)
                   .append("}");
            }
        }
        else { // use current value
            palette.getInlineCv().traverseInlineText(node,ldp,oc);
        }
    }
	
    /** Get the BibTeX document, if any (the document is only created if it's
     * specified in the configuration *and* the document contains bibliographic
     * data *and* the configuration does not specify external BibTeX files
     * @return the BiBTeXDocument, or null if it does not exist).
     */
    public BibTeXDocument getBibTeXDocument () {
        return bibDoc;
    }
	
    
}