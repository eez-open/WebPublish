/************************************************************************
 *
 *  ConverterPalette.java
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
 *  Version 1.0 (2009-03-02) 
 *
 */

package writer2latex.latex;

import org.w3c.dom.Element;

import java.io.IOException;
import java.util.LinkedList;

import writer2latex.api.Config;
import writer2latex.api.ConverterFactory;
//import writer2latex.api.ConverterResult;
import writer2latex.base.ConverterBase;
import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.latex.i18n.I18n;
import writer2latex.latex.i18n.XeTeXI18n;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.Info;
import writer2latex.util.CSVList;
import writer2latex.util.ExportNameCollection;
import writer2latex.util.Misc;
import writer2latex.office.MIMETypes;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;

/**
 *  <p>This class converts a Writer XML file to a LaTeX file<.</p>
 */
public final class ConverterPalette extends ConverterBase {

    // Configuration
    private LaTeXConfig config;
	
    public Config getConfig() { return config; }

    // The main outfile
    private LaTeXDocument texDoc;

    // Various data used in conversion
    private Context mainContext; // main context
    private CSVList globalOptions; // global options

    // The helpers (the "colors" of the palette)
    private I18n i18n;
    private ColorConverter colorCv;
    private CharStyleConverter charSc;
    private ListStyleConverter listSc;
    private PageStyleConverter pageSc;
    private BlockConverter blockCv;
    private ParConverter parCv;
    private HeadingConverter headingCv;
    private IndexConverter indexCv;
    private BibConverter bibCv;
    private SectionConverter sectionCv;
    private TableConverter tableCv;
    private NoteConverter noteCv;
    private CaptionConverter captionCv;
    private InlineConverter inlineCv;
    private FieldConverter fieldCv;
    private DrawConverter drawCv;
    private MathmlConverter mathmlCv;
    private Info info;
	
    // Constructor
    public ConverterPalette() {
        super();
        config = new LaTeXConfig();
    }
	
    // Accessor methods for data
	
    public String getOutFileName() { return sTargetFileName; }

    public Context getMainContext() { return mainContext; }
	
    public void addGlobalOption(String sOption) {
	    globalOptions.addValue(sOption);
    }

    // Accessor methods for helpers
    public I18n getI18n() { return i18n; }
    public ColorConverter getColorCv() { return colorCv; }
    public CharStyleConverter getCharSc() { return charSc; }
    public ListStyleConverter getListSc() { return listSc; }
    public PageStyleConverter getPageSc() { return pageSc; }
    public BlockConverter getBlockCv() { return blockCv; }
    public ParConverter getParCv() { return parCv; }
    public HeadingConverter getHeadingCv() { return headingCv; }
    public IndexConverter getIndexCv() { return indexCv; }
    public BibConverter getBibCv() { return bibCv; }
    public SectionConverter getSectionCv() { return sectionCv; }
    public TableConverter getTableCv() { return tableCv; }
    public NoteConverter getNoteCv() { return noteCv; }
    public CaptionConverter getCaptionCv() { return captionCv; }
    public InlineConverter getInlineCv() { return inlineCv; }
    public FieldConverter getFieldCv() { return fieldCv; }
    public DrawConverter getDrawCv() { return drawCv; }
    public MathmlConverter getMathmlCv() { return mathmlCv; }
    public Info getInfo() { return info; }
	
	
    // fill out inner converter method
    public void convertInner() throws IOException {
        sTargetFileName = Misc.trimDocumentName(sTargetFileName,".tex");
        imageLoader.setOutFileName(new ExportNameCollection(true).getExportName(sTargetFileName));
        
        imageLoader.setUseSubdir(config.saveImagesInSubdir());
		
        // Set graphics formats depending on backend
        if (config.getBackend()==LaTeXConfig.PDFTEX || config.getBackend()==LaTeXConfig.XETEX) {
            imageLoader.setDefaultFormat(MIMETypes.PNG);
            imageLoader.setDefaultVectorFormat(MIMETypes.PDF);
            imageLoader.addAcceptedFormat(MIMETypes.JPEG);
        }
        else if (config.getBackend()==LaTeXConfig.DVIPS) {
            imageLoader.setDefaultFormat(MIMETypes.EPS);
        }
        // Other values: keep original format
		
        // Inject user sequence names for tables and figures into OfficeReader
        if (config.getTableSequenceName().length()>0) {
            ofr.addTableSequenceName(config.getTableSequenceName());
        }
        if (config.getFigureSequenceName().length()>0) {
            ofr.addFigureSequenceName(config.getFigureSequenceName());
        }
		
        // Create helpers
        if (config.getBackend()!=LaTeXConfig.XETEX) {
            i18n = new ClassicI18n(ofr,config,this);        	
        }
        else {
            i18n = new XeTeXI18n(ofr,config,this);        	        	
        }
        colorCv = new ColorConverter(ofr,config,this);
        charSc = new CharStyleConverter(ofr,config,this);
        listSc = new ListStyleConverter(ofr,config,this);
        pageSc = new PageStyleConverter(ofr,config,this);
        blockCv = new BlockConverter(ofr,config,this);
        parCv = new ParConverter(ofr,config,this);
        headingCv = new HeadingConverter(ofr,config,this);
        indexCv = new IndexConverter(ofr,config,this);
        bibCv = new BibConverter(ofr,config,this);
        sectionCv = new SectionConverter(ofr,config,this);
        tableCv = new TableConverter(ofr,config,this);
        noteCv = new NoteConverter(ofr,config,this);
        captionCv = new CaptionConverter(ofr,config,this);
        inlineCv = new InlineConverter(ofr,config,this);
        fieldCv = new FieldConverter(ofr,config,this);
        drawCv = new DrawConverter(ofr,config,this);
        mathmlCv = new MathmlConverter(ofr,config,this);
        info = new Info(ofr,config,this);

        // Create master document and add this
        this.texDoc = new LaTeXDocument(sTargetFileName,config.getWrapLinesAfter());
        if (config.getBackend()!=LaTeXConfig.XETEX) {
            texDoc.setEncoding(ClassicI18n.writeJavaEncoding(config.getInputencoding()));        	
        }
        else {
            texDoc.setEncoding("UTF-8");        	
        	
        }
        convertData.addDocument(texDoc);

        // Create other data
        globalOptions = new CSVList(',');

        // Setup context.
        // The default language is specified in the default paragraph style:
        mainContext = new Context();
        mainContext.resetFormattingFromStyle(ofr.getDefaultParStyle());
        mainContext.setInMulticols(pageSc.isTwocolumn());
		
        // Create main LaTeXDocumentPortions
        LaTeXDocumentPortion packages = new LaTeXDocumentPortion(false);
        LaTeXDocumentPortion declarations = new LaTeXDocumentPortion(false);
        LaTeXDocumentPortion body = new LaTeXDocumentPortion(true);
        
        // Traverse the content
        Element content = ofr.getContent();
        blockCv.traverseBlockText(content,body,mainContext);
        noteCv.insertEndnotes(body);

        // Add declarations from our helpers
        i18n.appendDeclarations(packages,declarations);
        colorCv.appendDeclarations(packages,declarations);
        charSc.appendDeclarations(packages,declarations);
        headingCv.appendDeclarations(packages,declarations);
        parCv.appendDeclarations(packages,declarations);
        listSc.appendDeclarations(packages,declarations);
        pageSc.appendDeclarations(packages,declarations);
        blockCv.appendDeclarations(packages,declarations);
        indexCv.appendDeclarations(packages,declarations);
        bibCv.appendDeclarations(packages,declarations);
        sectionCv.appendDeclarations(packages,declarations);
        tableCv.appendDeclarations(packages,declarations);
        noteCv.appendDeclarations(packages,declarations);
        captionCv.appendDeclarations(packages,declarations);
        inlineCv.appendDeclarations(packages,declarations);
        fieldCv.appendDeclarations(packages,declarations);
        drawCv.appendDeclarations(packages,declarations);
        mathmlCv.appendDeclarations(packages,declarations);

        // Add custom preamble
        LinkedList customPreamble = config.getCustomPreamble();
        int nCPLen = customPreamble.size();
        for (int i=0; i<nCPLen; i++) {
            declarations.append( (String) customPreamble.get(i) ).nl();
        }

        // Set \title, \author and \date (for \maketitle)
        createMeta("title",metaData.getTitle(),declarations);
        if (config.metadata()) {
            createMeta("author",metaData.getCreator(),declarations);
            // According to the spec, the date has the format YYYY-MM-DDThh:mm:ss
            String sDate = metaData.getDate();
            if (sDate.length()==19 && sDate.charAt(10)=='T') {
                sDate = sDate.substring(0,10);
            }
            createMeta("date",sDate,declarations);
        }
		
        // Create options for documentclass
        if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
            StyleWithProperties dpStyle = ofr.getDefaultParStyle();
            if (dpStyle!=null) {
                String s = dpStyle.getProperty(XMLString.FO_FONT_SIZE);
                if ("10pt".equals(s)) { globalOptions.addValue("10pt"); }
                if ("11pt".equals(s)) { globalOptions.addValue("11pt"); }
                if ("12pt".equals(s)) { globalOptions.addValue("12pt"); }
            }
        }
		
        // Temp solution. TODO: Fix when new CSVList is implemented
        if (config.getGlobalOptions().length()>0) {
            globalOptions.addValue(config.getGlobalOptions());
        }

        // Assemble the document
        LaTeXDocumentPortion result = texDoc.getContents();

        if (!config.noPreamble()) {
            // Create document class declaration
	        result.append("% This file was converted to LaTeX by Writer2LaTeX ver. "+ConverterFactory.getVersion()).nl()
                  .append("% see http://writer2latex.sourceforge.net for more info").nl();
            result.append("\\documentclass");
            if (!globalOptions.isEmpty()) {
                result.append("[").append(globalOptions.toString()).append("]");
            }
            result.append("{").append(config.getDocumentclass()).append("}").nl();

            result.append(packages)
                  .append(declarations)
                  .append("\\begin{document}").nl();
        }

        result.append(body);

        if (!config.noPreamble()) {
            result.append("\\end{document}").nl();
        }
        else {
            result.append("\\endinput").nl();
        }
		
        // Add BibTeX document if there's any bibliographic data
        if (bibCv.getBibTeXDocument()!=null) {
            convertData.addDocument(bibCv.getBibTeXDocument());
        }
    }
	
    private void createMeta(String sName, String sValue,LaTeXDocumentPortion ldp) {
        if (sValue==null) { return; }
        // Meta data is assumed to be in the default language:
        ldp.append("\\"+sName+"{"+i18n.convert(sValue,false,mainContext.getLang())+"}").nl();
    }


}