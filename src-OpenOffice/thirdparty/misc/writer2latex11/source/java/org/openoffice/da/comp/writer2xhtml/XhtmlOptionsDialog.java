/************************************************************************
 *
 *  XhtmlOptionsDialog.java
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
 *  Version 1.0 (2008-11-16)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import org.openoffice.da.comp.w2lcommon.filter.OptionsDialogBase;

/** This class provides a uno component which implements a filter ui for the
 *  Xhtml export
 */
public class XhtmlOptionsDialog extends OptionsDialogBase {
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.XhtmlOptionsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialog";
	
    public String getDialogLibraryName() { return "W2XDialogs"; }
	
    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "XhtmlOptions"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2xhtml.Options/XhtmlOptions";
    }
	
    /** Create a new XhtmlOptionsDialog */
    public XhtmlOptionsDialog(XComponentContext xContext) {
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
			
        // AutoCorrect
        loadCheckBoxOption(xProps, "IgnoreHardLineBreaks");
        loadCheckBoxOption(xProps, "IgnoreEmptyParagraphs");
        loadCheckBoxOption(xProps, "IgnoreDoubleSpaces");

        // Files
        loadCheckBoxOption(xProps, "Split");
        loadListBoxOption(xProps, "SplitLevel");
        loadListBoxOption(xProps, "RepeatLevels");
        loadCheckBoxOption(xProps, "SaveImagesInSubdir");
        loadTextFieldOption(xProps, "XsltPath");

        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    protected void saveSettings(XPropertySet xProps, PropertyHelper helper) {
        // Style
        short nConfig = saveConfig(xProps, helper);
        String[] sCoreStyles = { "Chocolate", "Midnight", "Modernist",
            "Oldstyle", "Steely", "Swiss", "Traditional", "Ultramarine" };
        switch (nConfig) {
            case 0: helper.put("ConfigURL","*default.xml"); break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: helper.put("ConfigURL","*cleanxhtml.xml");
                    helper.put("custom_stylesheet",
                        "http://www.w3.org/StyleSheets/Core/"+sCoreStyles[nConfig-1]);
                    break;
            case 9: helper.put("ConfigURL","$(user)/writer2xhtml.xml");
                    helper.put("AutoCreate","true");
        }
		
        saveCheckBoxOption(xProps, helper, "ConvertToPx", "convert_to_px");
        saveNumericOptionAsPercentage(xProps, helper, "Scaling", "scaling");
        saveNumericOptionAsPercentage(xProps, helper, "ColumnScaling", "column_scaling");
        saveCheckBoxOption(xProps, helper, "OriginalImageSize", "original_image_size");

        // Special content
        saveCheckBoxOption(xProps, helper, "Notes", "notes");
        saveCheckBoxOption(xProps, helper, "UseDublinCore", "use_dublin_core");
  		
        // AutoCorrect
        saveCheckBoxOption(xProps, helper, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, helper, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, helper, "IgnoreDoubleSpaces", "ignore_double_spaces");

        // Files
        boolean bSplit = saveCheckBoxOption(xProps, "Split");
        short nSplitLevel = saveListBoxOption(xProps, "SplitLevel");
        short nRepeatLevels = saveListBoxOption(xProps, "RepeatLevels");
        if (!isLocked("split_level")) {
            if (bSplit) {
               helper.put("split_level",Integer.toString(nSplitLevel+1));
               helper.put("repeat_levels",Integer.toString(nRepeatLevels));
            }
            else {
                helper.put("split_level","0");
            }
        }
    		
        saveCheckBoxOption(xProps, helper, "SaveImagesInSubdir", "save_images_in_subdir");
        saveTextFieldOption(xProps, helper, "XsltPath", "xslt_path");

    }
	
	
    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange")) {
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("SplitChange")) {
            enableSplitLevel();
        }
        return true;
    }

    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "SplitChange" };
        return sNames;
    }
	
    private void enableControls() {
        // Style
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("ColumnScalingLabel",!isLocked("column_scaling"));
        setControlEnabled("ColumnScaling",!isLocked("column_scaling"));
        setControlEnabled("ConvertToPx",!isLocked("convert_to_px"));
        setControlEnabled("OriginalImageSize",!isLocked("original_image_size"));

        // Special content
        setControlEnabled("Notes",!isLocked("notes"));
        setControlEnabled("UseDublinCore",!isLocked("use_dublin_core"));
			
        // AutoCorrect
        setControlEnabled("IgnoreHardLineBreaks",!isLocked("ignore_hard_line_breaks"));
        setControlEnabled("IgnoreEmptyParagraphs",!isLocked("ignore_empty_paragraphs"));
        setControlEnabled("IgnoreDoubleSpaces",!isLocked("ignore_double_spaces"));

        // Files
        boolean bSplit = getCheckBoxStateAsBoolean("Split");
        setControlEnabled("Split",!isLocked("split_level"));
        setControlEnabled("SplitLevelLabel",!isLocked("split_level") && bSplit);
        setControlEnabled("SplitLevel",!isLocked("split_level") && bSplit);
        setControlEnabled("RepeatLevelsLabel",!isLocked("repeat_levels") && !isLocked("split_level") && bSplit);
        setControlEnabled("RepeatLevels",!isLocked("repeat_levels") && !isLocked("split_level") && bSplit);
        setControlEnabled("SaveImagesInSubdir",!isLocked("save_images_in_subdir"));
        setControlEnabled("XsltPathLabel",(this instanceof XhtmlOptionsDialogXsl) && !isLocked("xslt_path"));
        setControlEnabled("XsltPath",(this instanceof XhtmlOptionsDialogXsl) && !isLocked("xslt_path"));
    }
	
    private void enableSplitLevel() {
        if (!isLocked("split_level")) {
            boolean bState = getCheckBoxStateAsBoolean("Split");
            setControlEnabled("SplitLevelLabel",bState);
            setControlEnabled("SplitLevel",bState);
            if (!isLocked("repeat_levels")) {
                setControlEnabled("RepeatLevelsLabel",bState);
                setControlEnabled("RepeatLevels",bState);
            }
        }
    }

	
}



