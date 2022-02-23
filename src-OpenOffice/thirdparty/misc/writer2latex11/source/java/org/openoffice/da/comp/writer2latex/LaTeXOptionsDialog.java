/************************************************************************
 *
 *  LaTeXOptionsDialog.java
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
 *  Version 1.0 (2009-02-18)
 *  
 */

package org.openoffice.da.comp.writer2latex;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
//import com.sun.star.frame.XDesktop;
//import com.sun.star.lang.XComponent;
//import com.sun.star.text.XTextFieldsSupplier;
//import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import org.openoffice.da.comp.w2lcommon.filter.OptionsDialogBase;

/** This class provides a uno component which implements a filter ui for the
 *  LaTeX export
 */
public class LaTeXOptionsDialog extends OptionsDialogBase {

    // Translate list box items to configuration option values 
    private static final String[] BACKEND_VALUES =
        { "generic", "pdftex", "dvips", "unspecified" };
    private static final String[] INPUTENCODING_VALUES =
        { "ascii", "latin1", "latin2", "iso-8859-7", "cp1250", "cp1251", "koi8-r", "utf8" };
    private static final String[] NOTES_VALUES =
        { "ignore", "comment", "marginpar", "pdfannotation" };
    private static final String[] FLOATOPTIONS_VALUES =
        { "", "tp", "bp", "htp", "hbp" };
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.LaTeXOptionsDialog";

    /** The component should also have an implementation name.
     *  The subclass should override this with a suitable name
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2latex.LaTeXOptionsDialog";

    public String getDialogLibraryName() { return "W2LDialogs"; }
	

    /** Create a new LaTeXOptionsDialog */
    public LaTeXOptionsDialog(XComponentContext xContext) {
        super(xContext);
        xMSF = W2LRegistration.xMultiServiceFactory;
    }
	
    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "LaTeXOptions"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2LaTeX.Options/LaTeXOptions";
    }

    /** Load settings from the registry to the dialog */
    protected void loadSettings(XPropertySet xProps) {
        // General
        loadConfig(xProps);
        loadListBoxOption(xProps,"Backend");
        loadListBoxOption(xProps,"Inputencoding");
        loadCheckBoxOption(xProps,"Multilingual");
        loadCheckBoxOption(xProps,"GreekMath");
        loadCheckBoxOption(xProps,"AdditionalSymbols");
		
        // Bibliography
        loadCheckBoxOption(xProps,"UseBibtex");
        loadComboBoxOption(xProps,"BibtexStyle");
		
        // Files
        loadCheckBoxOption(xProps,"WrapLines");
        loadNumericOption(xProps,"WrapLinesAfter");
        loadCheckBoxOption(xProps,"SplitLinkedSections");
        loadCheckBoxOption(xProps,"SplitToplevelSections");
        loadCheckBoxOption(xProps,"SaveImagesInSubdir");
		
        // Special content
        loadListBoxOption(xProps,"Notes");
        loadCheckBoxOption(xProps,"Metadata");
		
        // Figures and tables
        loadCheckBoxOption(xProps,"OriginalImageSize");
        loadCheckBoxOption(xProps,"OptimizeSimpleTables");
        loadNumericOption(xProps,"SimpleTableLimit");
        loadCheckBoxOption(xProps,"FloatTables");
        loadCheckBoxOption(xProps,"FloatFigures");
        loadListBoxOption(xProps,"FloatOptions");

        // AutoCorrect
        loadCheckBoxOption(xProps,"IgnoreHardPageBreaks");
        loadCheckBoxOption(xProps,"IgnoreHardLineBreaks");
        loadCheckBoxOption(xProps,"IgnoreEmptyParagraphs");
        loadCheckBoxOption(xProps,"IgnoreDoubleSpaces");
		
        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    protected void saveSettings(XPropertySet xProps, PropertyHelper filterData) {
        // General
        short nConfig = saveConfig(xProps, filterData);
        switch (nConfig) {
            case 0: filterData.put("ConfigURL","*ultraclean.xml"); break;
            case 1: filterData.put("ConfigURL","*clean.xml"); break;
            case 2: filterData.put("ConfigURL","*default.xml"); break;
            case 3: filterData.put("ConfigURL","*pdfprint.xml"); break;
            case 4: filterData.put("ConfigURL","*pdfscreen.xml"); break;
            case 5: filterData.put("ConfigURL","$(user)/writer2latex.xml");
                    filterData.put("AutoCreate","true");
        }

        saveListBoxOption(xProps, filterData, "Backend", "backend", BACKEND_VALUES );
        if (getListBoxSelectedItem("Config")==4) {
            // pdfscreen locks the backend to pdftex
            filterData.put("backend","pdftex");
        }
        saveListBoxOption(xProps, filterData, "Inputencoding", "inputencoding", INPUTENCODING_VALUES);
        saveCheckBoxOption(xProps, filterData, "Multilingual", "multilingual");
        saveCheckBoxOption(xProps, filterData, "GreekMath", "greek_math");
        // AdditionalSymbols sets 5 different w2l options...
        saveCheckBoxOption(xProps, filterData, "AdditionalSymbols", "use_pifont");
        saveCheckBoxOption(xProps, filterData, "AdditionalSymbols", "use_ifsym");
        saveCheckBoxOption(xProps, filterData, "AdditionalSymbols", "use_wasysym");
        saveCheckBoxOption(xProps, filterData, "AdditionalSymbols", "use_eurosym");
        saveCheckBoxOption(xProps, filterData, "AdditionalSymbols", "use_tipa");
		
        // Bibliography
        saveCheckBoxOption(xProps, filterData, "UseBibtex", "use_bibtex");
        saveComboBoxOption(xProps, filterData, "BibtexStyle", "bibtex_style");
		
        // Files
        boolean bWrapLines = saveCheckBoxOption(xProps, "WrapLines");
        int nWrapLinesAfter = saveNumericOption(xProps, "WrapLinesAfter");
        if (!isLocked("wrap_lines_after")) {
            if (bWrapLines) {
                filterData.put("wrap_lines_after",Integer.toString(nWrapLinesAfter));
            }
            else {
                filterData.put("wrap_lines_after","0");
            }
        }
		
        saveCheckBoxOption(xProps, filterData, "SplitLinkedSections", "split_linked_sections");
        saveCheckBoxOption(xProps, filterData, "SplitToplevelSections", "split_toplevel_sections");
        saveCheckBoxOption(xProps, filterData, "SaveImagesInSubdir", "save_images_in_subdir");
		
        // Special content
        saveListBoxOption(xProps, filterData, "Notes", "notes", NOTES_VALUES);
        saveCheckBoxOption(xProps, filterData, "Metadata", "metadata");

        // Figures and tables
        saveCheckBoxOption(xProps, filterData, "OriginalImageSize", "original_image_size");
		
        boolean bOptimizeSimpleTables = saveCheckBoxOption(xProps,"OptimizeSimpleTables");
        int nSimpleTableLimit = saveNumericOption(xProps,"SimpleTableLimit");
        if (!isLocked("simple_table_limit")) {
            if (bOptimizeSimpleTables) {
                filterData.put("simple_table_limit",Integer.toString(nSimpleTableLimit));
            }
            else {
                filterData.put("simple_table_limit","0");
            }
        }

        saveCheckBoxOption(xProps, filterData, "FloatTables", "float_tables");
        saveCheckBoxOption(xProps, filterData, "FloatFigures", "float_figures");
        saveListBoxOption(xProps, filterData, "FloatOptions", "float_options", FLOATOPTIONS_VALUES);

        // AutoCorrect
        saveCheckBoxOption(xProps, filterData, "IgnoreHardPageBreaks", "ignore_hard_page_breaks");
        saveCheckBoxOption(xProps, filterData, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, filterData, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, filterData, "IgnoreDoubleSpaces", "ignore_double_spaces");
    }
	

    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange")) {
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("UseBibtexChange")) {
            enableBibtexStyle();
        }
        else if (sMethod.equals("WrapLinesChange")) {
            enableWrapLinesAfter();
        }
        else if (sMethod.equals("OptimizeSimpleTablesChange")) {
            enableSimpleTableLimit();
        }
        else if (sMethod.equals("FloatTablesChange")) {
            enableFloatOptions();
        }
        else if (sMethod.equals("FloatFiguresChange")) {
            enableFloatOptions();
        }
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "UseBibtexChange", "WrapLinesChange",
            "OptimizeSimpleTablesChange", "FloatTablesChange", "FloatFiguresChange" };
        return sNames;
    }
	
    protected boolean isLocked(String sOptionName) {
        if ("backend".equals(sOptionName)) {
            // backend must be pdf for pdfscreen
            return getListBoxSelectedItem("Config")==4 || super.isLocked(sOptionName);
        }
        else if ("additional_symbols".equals(sOptionName)) {
            // additional_symbols is disabled for custom config (where the 5
            // individual options can be set independently)
            return getListBoxSelectedItem("Config")==5 || super.isLocked(sOptionName);
        }
        else if ("use_pifont".equals(sOptionName)) {
            return isLocked("additional_symbols");
        }
        else if ("use_ifsym".equals(sOptionName)) {
            return isLocked("additional_symbols");
        }
        else if ("use_wasysym".equals(sOptionName)) {
            return isLocked("additional_symbols");
        }
        else if ("use_eurosym".equals(sOptionName)) {
            return isLocked("additional_symbols");
        }
        else if ("use_tipa".equals(sOptionName)) {
            return isLocked("additional_symbols");
        }
        else {
            return super.isLocked(sOptionName); 
        }
    }
	
    private void enableControls() {
        // General
        setControlEnabled("BackendLabel",!isLocked("backend"));
        setControlEnabled("Backend",!isLocked("backend"));
        setControlEnabled("InputencodingLabel",!isLocked("inputencoding"));
        setControlEnabled("Inputencoding",!isLocked("inputencoding"));
        setControlEnabled("Multilingual",!isLocked("multilingual"));
        setControlEnabled("GreekMath",!isLocked("greek_math"));
        setControlEnabled("AdditionalSymbols",!isLocked("additional_symbols"));

        // Bibliography
        setControlEnabled("UseBibtex",!isLocked("use_bibtex"));
        boolean bUseBibtex = getCheckBoxStateAsBoolean("UseBibtex");
        setControlEnabled("BibtexStyleLabel",!isLocked("bibtex_style") && bUseBibtex);
        setControlEnabled("BibtexStyle",!isLocked("bibtex_style") && bUseBibtex);

        // Files
        setControlEnabled("WrapLines",!isLocked("wrap_lines_after"));
        boolean bWrapLines = getCheckBoxStateAsBoolean("WrapLines");
        setControlEnabled("WrapLinesAfterLabel",!isLocked("wrap_lines_after") && bWrapLines);
        setControlEnabled("WrapLinesAfter",!isLocked("wrap_lines_after") && bWrapLines);
        setControlEnabled("SplitLinkedSections",!isLocked("split_linked_sections"));
        setControlEnabled("SplitToplevelSections",!isLocked("split_toplevel_sections"));
        setControlEnabled("SaveImagesInSubdir",!isLocked("save_images_in_subdir"));

        // Special content
        setControlEnabled("NotesLabel",!isLocked("notes"));
        setControlEnabled("Notes",!isLocked("notes"));
        setControlEnabled("Metadata",!isLocked("metadata"));

        // Figures and tables
        setControlEnabled("OriginalImageSize",!isLocked("original_image_size"));
        setControlEnabled("OptimizeSimpleTables",!isLocked("simple_table_limit"));
        boolean bOptimizeSimpleTables = getCheckBoxStateAsBoolean("OptimizeSimpleTables");
        setControlEnabled("SimpleTableLimitLabel",!isLocked("simple_table_limit") && bOptimizeSimpleTables);
        setControlEnabled("SimpleTableLimit",!isLocked("simple_table_limit") && bOptimizeSimpleTables);
        setControlEnabled("FloatTables",!isLocked("float_tables"));
        setControlEnabled("FloatFigures",!isLocked("float_figures"));
        boolean bFloat = getCheckBoxStateAsBoolean("FloatFigures") ||
            getCheckBoxStateAsBoolean("FloatTables");
        setControlEnabled("FloatOptionsLabel",!isLocked("float_options") && bFloat);
        setControlEnabled("FloatOptions",!isLocked("float_options") && bFloat);

        // AutoCorrect
        setControlEnabled("IgnoreHardPageBreaks",!isLocked("ignore_hard_page_breaks"));
        setControlEnabled("IgnoreHardLineBreaks",!isLocked("ignore_hard_line_breaks"));
        setControlEnabled("IgnoreEmptyParagraphs",!isLocked("ignore_empty_paragraphs"));
        setControlEnabled("IgnoreDoubleSpaces",!isLocked("ignore_double_spaces"));
    }
	
    private void enableBibtexStyle() {
        if (!isLocked("bibtex_style")) {
            boolean bState = getCheckBoxStateAsBoolean("UseBibtex");
            setControlEnabled("BibtexStyleLabel",bState);
            setControlEnabled("BibtexStyle",bState);
        }
    }

    private void enableWrapLinesAfter() {
        if (!isLocked("wrap_lines_after")) {
            boolean bState = getCheckBoxStateAsBoolean("WrapLines");
            setControlEnabled("WrapLinesAfterLabel",bState);
            setControlEnabled("WrapLinesAfter",bState);
        }
    }

    private void enableSimpleTableLimit() {
        if (!isLocked("simple_table_limit")) {
            boolean bState = getCheckBoxStateAsBoolean("OptimizeSimpleTables");
            setControlEnabled("SimpleTableLimitLabel",bState);
            setControlEnabled("SimpleTableLimit",bState);
        }
    }

    private void enableFloatOptions() {
        if (!isLocked("float_options")) {
            boolean bState = getCheckBoxStateAsBoolean("FloatFigures") ||
                getCheckBoxStateAsBoolean("FloatTables");
            setControlEnabled("FloatOptionsLabel",bState);
            setControlEnabled("FloatOptions",bState);
        }
    }
	
}



