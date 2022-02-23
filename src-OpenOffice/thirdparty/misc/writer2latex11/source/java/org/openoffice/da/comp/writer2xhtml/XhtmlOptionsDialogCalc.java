/************************************************************************
 *
 *  XhtmlOptionsDialogCalc.java
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
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import org.openoffice.da.comp.w2lcommon.filter.OptionsDialogBase;

/** This class provides a uno component which implements a filter ui for the
 *  Xhtml export in Calc
 */
public class XhtmlOptionsDialogCalc extends OptionsDialogBase {
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writerxhtml.XhtmlOptionsDialogCalc";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogCalc";

    public String getDialogLibraryName() { return "W2XDialogs"; }

    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "XhtmlOptionsCalc"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2xhtml.Options/XhtmlOptionsCalc";
    }
	
    /** Create a new XhtmlOptionsDialogCalc */
    public XhtmlOptionsDialogCalc(XComponentContext xContext) {
        super(xContext);
        xMSF = W2XRegistration.xMultiServiceFactory;
    }
	
    /** Load settings from the registry to the dialog */
    protected void loadSettings(XPropertySet xProps) {
        // Style
        loadConfig(xProps);
        loadCheckBoxOption(xProps, "ConvertToPx");
        loadNumericOption(xProps, "Scaling");
        loadNumericOption(xProps, "ColumnScaling");
        loadCheckBoxOption(xProps, "OriginalImageSize");
		
        // Special content
        loadCheckBoxOption(xProps, "Notes");
        loadCheckBoxOption(xProps, "UseDublinCore");
			
        // Sheets
        loadCheckBoxOption(xProps, "DisplayHiddenSheets");
        loadCheckBoxOption(xProps, "DisplayHiddenRowsCols");
        loadCheckBoxOption(xProps, "DisplayFilteredRowsCols");
        loadCheckBoxOption(xProps, "ApplyPrintRanges");
        loadCheckBoxOption(xProps, "UseTitleAsHeading");
        loadCheckBoxOption(xProps, "UseSheetNamesAsHeadings");

        // Files
        loadCheckBoxOption(xProps, "CalcSplit");
        loadCheckBoxOption(xProps, "SaveImagesInSubdir");

        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    protected void saveSettings(XPropertySet xProps, PropertyHelper helper) {
        // Style
        short nConfig = saveConfig(xProps, helper);
        if (nConfig==0) {
            helper.put("ConfigURL","*default.xml");
        }
        else if (nConfig==1) {
            helper.put("ConfigURL","$(user)/writer2xhtml.xml");
            helper.put("AutoCreate","true");
        }
		
        saveCheckBoxOption(xProps, helper, "ConvertToPx", "convert_to_px");
        saveNumericOptionAsPercentage(xProps, helper, "Scaling", "scaling");
        saveNumericOptionAsPercentage(xProps, helper, "ColumnScaling", "column_scaling");
        saveCheckBoxOption(xProps, helper, "OriginalImageSize", "original_image_size");

        // Special content
        saveCheckBoxOption(xProps, helper, "Notes", "notes");
        saveCheckBoxOption(xProps, helper, "UseDublinCore", "use_dublin_core");
  		
        // Sheets
        saveCheckBoxOption(xProps, helper, "DisplayHiddenSheets", "display_hidden_sheets");
        saveCheckBoxOption(xProps, helper, "DisplayHiddenRowsCols", "display_hidden_rows_cols");
        saveCheckBoxOption(xProps, helper, "DisplayFilteredRowsCols", "display_filtered_rows_cols");
        saveCheckBoxOption(xProps, helper, "ApplyPrintRanges", "apply_print_ranges");
        saveCheckBoxOption(xProps, helper, "UseTitleAsHeading", "use_title_as_heading"); 
        saveCheckBoxOption(xProps, helper, "UseSheetNamesAsHeadings", "use_sheet_names_as_headings");

        // Files
        saveCheckBoxOption(xProps, helper, "CalcSplit", "calc_split");
        saveCheckBoxOption(xProps, helper, "SaveImagesInSubdir", "save_images_in_subdir");

    }
	
    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange")) {
            updateLockedOptions();
            enableControls();
        }
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange" };
        return sNames;
    }
	
    private void enableControls() {
        // Style
        setControlEnabled("ConvertToPx",!isLocked("convert_to_px"));
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("ColumnScalingLabel",!isLocked("column_scaling"));
        setControlEnabled("ColumnScaling",!isLocked("column_scaling"));
        setControlEnabled("OriginalImageSize",!isLocked("original_image_size"));

        // Special content
        setControlEnabled("Notes",!isLocked("notes"));
        setControlEnabled("UseDublinCore",!isLocked("use_dublin_core"));
			
        // Sheets
        setControlEnabled("DisplayHiddenSheets", !isLocked("display_hidden_sheets"));
        setControlEnabled("DisplayHiddenRowsCols", !isLocked("display_hidden_rows_cols"));
        setControlEnabled("DisplayFilteredRowsCols", !isLocked("display_filtered_rows_cols"));
        setControlEnabled("ApplyPrintRanges", !isLocked("apply_print_ranges"));
        setControlEnabled("UseTitleAsHeading", !isLocked("use_title_as_heading")); 
        setControlEnabled("UseSheetNamesAsHeadings", !isLocked("use_sheet_names_as_headings"));

        // Files
        setControlEnabled("CalcSplit",!isLocked("calc_split"));
        setControlEnabled("SaveImagesInSubdir",!isLocked("save_images_in_subdir"));
    }
		
}



