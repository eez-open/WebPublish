/************************************************************************
 *
 *  OptionsDialogBase.java
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
 *  Version 1.0 (2008-10-14)
 *
 */ 
 
package org.openoffice.da.comp.w2lcommon.filter;

import java.util.HashSet;

import com.sun.star.awt.XDialogEventHandler;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertyAccess;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XDocumentInfoSupplier;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;
import com.sun.star.util.XMacroExpander;

import org.openoffice.da.comp.w2lcommon.helper.DialogBase;
import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;

/** This class provides an abstract uno component which implements a filter ui
 */
public abstract class OptionsDialogBase extends DialogBase implements
        XPropertyAccess { // Filter ui requires XExecutableDialog + XPropertyAccess
		
    //////////////////////////////////////////////////////////////////////////
    // The subclass must override the following; and override the
    // implementation of XDialogEventHandler if needed
	
    /** Load settings from the registry to the dialog
     *  The subclass must implement this
     */
    protected abstract void loadSettings(XPropertySet xRegistryProps);
	
    /** Save settings from the dialog to the registry and create FilterData
     *  The subclass must implement this
     */
    protected abstract void saveSettings(XPropertySet xRegistryProps, PropertyHelper filterData);
	
    /** Return the name of the library containing the dialog
     */
    public abstract String getDialogLibraryName();
	
    /** Return the name of the dialog within the library
     */
    public abstract String getDialogName();

    /** Return the path to the options in the registry */
    public abstract String getRegistryPath();
	
    /** Create a new OptionsDialogBase */
    public OptionsDialogBase(XComponentContext xContext) {
        super(xContext);
        this.xMSF = null; // must be set properly by subclass
        mediaProps = null;
        sConfigNames = null;
        lockedOptions = new HashSet();
    }

    //////////////////////////////////////////////////////////////////////////
    // Implement some methods required by DialogBase
	
    /** Initialize the dialog (eg. with settings from the registry)
     */
    public void initialize() {
        try {
            // Prepare registry view
            Object view = getRegistryView(false);
            XPropertySet xProps = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class,view);

            // Load settings using method from subclass
            loadSettings(xProps);

            // Dispose the registry view
            disposeRegistryView(view);
        }
        catch (com.sun.star.uno.Exception e) {
            // Failed to get registry view
        }
    }
	
    /** Finalize the dialog after execution (eg. save settings to the registry)
     */
    public void finalize() {
        try {
            // Prepare registry view
            Object rwview = getRegistryView(true);
            XPropertySet xProps = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class,rwview);

            // Save settings and create FilterData using method from subclass
            PropertyHelper filterData = new PropertyHelper();
            saveSettings(xProps, filterData);

            // Commit registry changes
            XChangesBatch  xUpdateContext = (XChangesBatch)
                UnoRuntime.queryInterface(XChangesBatch.class,rwview);
            try {
                xUpdateContext.commitChanges();
            }
            catch (Exception e) {
                // ignore
            }

            // Dispose the registry view
            disposeRegistryView(rwview);

            // Update the media properties with the FilterData
            PropertyHelper helper = new PropertyHelper(mediaProps);
            helper.put("FilterData",filterData.toArray());
            mediaProps = helper.toArray();
        }
        catch (com.sun.star.uno.Exception e) {
            // Failed to get registry view
        }
    }


    //////////////////////////////////////////////////////////////////////////
    // Some private global variables

    // The service factory
    protected XMultiServiceFactory xMSF;
	
    // The media properties (set/get via XPropertyAccess implementation) 
    private PropertyValue[] mediaProps;
	
    // Configuration names (stored during execution of dialog)
    private String[] sConfigNames;
	
    // Set of locked controls
    private HashSet lockedOptions;
	
	
    //////////////////////////////////////////////////////////////////////////
    // Some private utility methods
	
    // Perform macro extansion
    private String expandMacros(String s) {
        if (s.startsWith("vnd.sun.star.expand:")) {
            // The string contains a macro, usually as a result of using %origin% in the registry
            s = s.substring(20);
            Object expander = xContext.getValueByName("/singletons/com.sun.star.util.theMacroExpander");
            XMacroExpander xExpander = (XMacroExpander) UnoRuntime.queryInterface (XMacroExpander.class, expander);
            try {
                return xExpander.expandMacros(s);
            }
            catch (IllegalArgumentException e) {
                // Unknown macro name found, proceed and hope for the best
                return s;
            }
        }
        else {
            return s;
        }
    }
	
    // Get the template name from the document with ui focus
    private String getTemplateName() {
        try {
            // Get current component
            Object desktop = xContext.getServiceManager()
                .createInstanceWithContext("com.sun.star.frame.Desktop",xContext);
            XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class,desktop);
            XComponent xComponent = xDesktop.getCurrentComponent();
			
            // Get the document info property set
            XDocumentInfoSupplier xDocInfoSuppl = (XDocumentInfoSupplier)
                UnoRuntime.queryInterface(XDocumentInfoSupplier.class, xComponent);
            Object docInfo = xDocInfoSuppl.getDocumentInfo();
            XPropertySet xDocInfo = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class, docInfo);

            return getPropertyValueAsString(xDocInfo,"Template");
        }
        catch (Exception e) {
            return "";
        }
    }
	
    // Get a view of the options root in the registry
    private Object getRegistryView(boolean bUpdate) 
        throws com.sun.star.uno.Exception {
        Object provider = xMSF.createInstance(
            "com.sun.star.configuration.ConfigurationProvider");
        XMultiServiceFactory xProvider = (XMultiServiceFactory)
            UnoRuntime.queryInterface(XMultiServiceFactory.class,provider);
        PropertyValue[] args = new PropertyValue[1];
        args[0] = new PropertyValue();
        args[0].Name = "nodepath";
        args[0].Value = getRegistryPath();
        String sServiceName = bUpdate ?
            "com.sun.star.configuration.ConfigurationUpdateAccess" :
            "com.sun.star.configuration.ConfigurationAccess";
        Object view = xProvider.createInstanceWithArguments(sServiceName,args);
        return view;
    }
	
    // Dispose a previously obtained registry view
    private void disposeRegistryView(Object view) {
        XComponent xComponent = (XComponent)
            UnoRuntime.queryInterface(XComponent.class,view);
        xComponent.dispose();
    }

    //////////////////////////////////////////////////////////////////////////
    // Implement uno interfaces
	
    // Override getTypes() from the interface XTypeProvider
    public Type[] getTypes() {
        Type[] typeReturn = {};
        try {
            typeReturn = new Type[] {
            new Type( XServiceName.class ),
            new Type( XServiceInfo.class ),
            new Type( XTypeProvider.class ),
            new Type( XExecutableDialog.class ),
            new Type( XPropertyAccess.class ),
            new Type( XDialogEventHandler.class ) };
        } catch(Exception exception) {
        }
        return typeReturn;
    }


    // Implement the interface XPropertyAccess
    public PropertyValue[] getPropertyValues() {
        return mediaProps;
    }
	
    public void setPropertyValues(PropertyValue[] props) { 
        mediaProps = props;
    }
	

    //////////////////////////////////////////////////////////////////////////
    // Various utility methods to be used by the sublasses
	
    // Helpers to load and save settings
	
    protected void updateLockedOptions() {
        lockedOptions.clear();
        short nItem = getListBoxSelectedItem("Config");
        int nStdConfigs = getListBoxStringItemList("Config").length - sConfigNames.length;
        if (nItem>=nStdConfigs) {
            // Get current configuration name
            String sName = sConfigNames[nItem-nStdConfigs];
			
            try {
                // Prepare registry view
                Object view = getRegistryView(false);
                XPropertySet xProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,view);
	 		
                // Get the available configurations
                Object configurations = getPropertyValue(xProps,"Configurations");
                XNameAccess xConfigurations = (XNameAccess)
                    UnoRuntime.queryInterface(XNameAccess.class,configurations);
				
                // Get the LockedOptions node from the desired configuration
                String sLockedOptions = "";
                Object config = xConfigurations.getByName(sName);
                XPropertySet xCfgProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,config);
                sLockedOptions = getPropertyValueAsString(xCfgProps,"LockedOptions");
				
                // Dispose the registry view
                disposeRegistryView(view);
			
                // Feed lockedOptions with the comma separated list of options
                int nStart = 0;
                for (int i=0; i<sLockedOptions.length(); i++) {
                    if (sLockedOptions.charAt(i)==',') {
                        lockedOptions.add(sLockedOptions.substring(nStart,i).trim());
                        nStart = i+1;
                    }
                }
                if (nStart<sLockedOptions.length()) {
                    lockedOptions.add(sLockedOptions.substring(nStart).trim());
                }
            }
            catch (Exception e) {
                // no options will be locked...
            }
        }
        
    }    
	
    protected boolean isLocked(String sOptionName) {
        return lockedOptions.contains(sOptionName);
    }
	
    // Configuration
    protected void loadConfig(XPropertySet xProps) {
        // The list box is extended with configurations from the registry
        String[] sStdConfigs = getListBoxStringItemList("Config");
        int nStdConfigs = sStdConfigs.length;

        Object configurations = getPropertyValue(xProps,"Configurations");
        XNameAccess xConfigurations = (XNameAccess)
            UnoRuntime.queryInterface(XNameAccess.class,configurations);
        sConfigNames = xConfigurations.getElementNames();
        int nRegConfigs = sConfigNames.length;

        String[] sAllConfigs = new String[nStdConfigs+nRegConfigs];
        for (short i=0; i<nStdConfigs; i++) {
            sAllConfigs[i] = sStdConfigs[i];
        }
		
        for (short i=0; i<nRegConfigs; i++) {
            try {
                Object config = xConfigurations.getByName(sConfigNames[i]);
                XPropertySet xCfgProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,config);
                sAllConfigs[nStdConfigs+i] = getPropertyValueAsString(xCfgProps,"DisplayName");
            }
            catch (Exception e) {
                sAllConfigs[nStdConfigs+i] = "";
            }
        }

        setListBoxStringItemList("Config",sAllConfigs);
        if (nStdConfigs+nRegConfigs<=12) {
            setListBoxLineCount("Config",(short) (nStdConfigs+nRegConfigs));
        }  
        else {
            setListBoxLineCount("Config",(short) 12);
        }
		
        // Select item based on template name
        String sTheTemplateName = getTemplateName();
        Object templates = getPropertyValue(xProps,"Templates");
        XNameAccess xTemplates = (XNameAccess)
            UnoRuntime.queryInterface(XNameAccess.class,templates);
        String[] sTemplateNames = xTemplates.getElementNames();
        for (int i=0; i<sTemplateNames.length; i++) {
            try {
                Object template = xTemplates.getByName(sTemplateNames[i]);
                XPropertySet xTplProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,template);
                String sTemplateName = getPropertyValueAsString(xTplProps,"TemplateName");
                if (sTemplateName.equals(sTheTemplateName)) {
                    String sConfigName = getPropertyValueAsString(xTplProps,"ConfigName");
                    for (short j=0; j<nRegConfigs; j++) {
                        if (sConfigNames[j].equals(sConfigName)) {
                            setListBoxSelectedItem("Config",(short) (nStdConfigs+j));
                            return;
                        }
                    }
                }
            }
            catch (Exception e) {
                // ignore
            }
        }

        // Select item based on value stored in registry
        short nConfig = getPropertyValueAsShort(xProps,"Config");
        if (nConfig<nStdConfigs) {
            setListBoxSelectedItem("Config",nConfig);
        }
        else { // Registry configurations are stored by name
            String sConfigName = getPropertyValueAsString(xProps,"ConfigName");
            for (short i=0; i<nRegConfigs; i++) {
                if (sConfigNames[i].equals(sConfigName)) {
                    setListBoxSelectedItem("Config",(short) (nStdConfigs+i));
                }
            }
        }
    }
	
    protected short saveConfig(XPropertySet xProps, PropertyHelper filterData) {
        // The Config list box is common for all dialogs
        Object configurations = getPropertyValue(xProps,"Configurations");
        XNameAccess xNameAccess = (XNameAccess)
            UnoRuntime.queryInterface(XNameAccess.class,configurations);

        boolean bFound = false;
        short nConfig = getListBoxSelectedItem("Config");
        int nStdConfigs = getListBoxStringItemList("Config").length - sConfigNames.length;
        if (nConfig>=nStdConfigs) { // only handle registry configurations
            int i = nConfig-nStdConfigs;
            try {
                Object config = xNameAccess.getByName(sConfigNames[i]);
                XPropertySet xCfgProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,config);
                filterData.put("ConfigURL",expandMacros(getPropertyValueAsString(xCfgProps,"ConfigURL")));
                filterData.put("TemplateURL",expandMacros(getPropertyValueAsString(xCfgProps,"TargetTemplateURL")));
                setPropertyValue(xProps,"ConfigName",sConfigNames[i]);
                bFound = true;
            }
            catch (Exception e) {
            }
        }
        setPropertyValue(xProps,"Config",nConfig);
        if (!bFound) { setPropertyValue(xProps,"ConfigName",""); }
        return nConfig;
    }
	
    // Check box option (boolean)
    protected boolean loadCheckBoxOption(XPropertySet xProps, String sName) {
        boolean bValue = getPropertyValueAsBoolean(xProps,sName);
        setCheckBoxStateAsBoolean(sName, bValue);
        return bValue;
    }
	
    protected boolean saveCheckBoxOption(XPropertySet xProps, String sName) {
        boolean bValue = getCheckBoxStateAsBoolean(sName);
        setPropertyValue(xProps, sName, bValue);
        return bValue;
    }

    protected boolean saveCheckBoxOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName) {
        boolean bValue = saveCheckBoxOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName, Boolean.toString(bValue));
        }
        return bValue;
    }
	
    // List box option
    protected short loadListBoxOption(XPropertySet xProps, String sName) {
        short nValue = getPropertyValueAsShort(xProps, sName);
        setListBoxSelectedItem(sName ,nValue);
        return nValue;
    }
	
    protected short saveListBoxOption(XPropertySet xProps, String sName) {
        short nValue = getListBoxSelectedItem(sName);
        setPropertyValue(xProps, sName, nValue);
        return nValue;
    }
	
    protected short saveListBoxOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName, String[] sValues) {
        short nValue = saveListBoxOption(xProps, sName);
        if (!isLocked(sOptionName) && (nValue>=0) && (nValue<sValues.length)) {
            filterData.put(sOptionName, sValues[nValue]);
        }
        return nValue;
    }
	
    // Combo box option
    protected String loadComboBoxOption(XPropertySet xProps, String sName) {
        String sValue = getPropertyValueAsString(xProps, sName);
        setComboBoxText(sName ,sValue);
        return sValue;
    }
	
    protected String saveComboBoxOption(XPropertySet xProps, String sName) {
        String sValue = getComboBoxText(sName);
        setPropertyValue(xProps, sName, sValue);
        return sValue;
    }
	
    protected String saveComboBoxOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName) {
        String sValue = saveComboBoxOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName, sValue);
        }
        return sValue;
    }

    // Text Field option
    protected String loadTextFieldOption(XPropertySet xProps, String sName) {
        String sValue = getPropertyValueAsString(xProps, sName);
        setTextFieldText(sName ,sValue);
        return sValue;
    }
	
    protected String saveTextFieldOption(XPropertySet xProps, String sName) {
        String sValue = getTextFieldText(sName);
        setPropertyValue(xProps, sName, sValue);
        return sValue;
    }
	
    protected String saveTextFieldOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName) {
        String sValue = saveTextFieldOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName, sValue);
        }
        return sValue;
    }

    // Numeric option
    protected int loadNumericOption(XPropertySet xProps, String sName) {
        int nValue = getPropertyValueAsInteger(xProps, sName);
        setNumericFieldValue(sName, nValue);
        return nValue;
    }
	
    protected int saveNumericOption(XPropertySet xProps, String sName) {
        int nValue = getNumericFieldValue(sName);
        setPropertyValue(xProps, sName, nValue);
        return nValue;
    }
	
    protected int saveNumericOptionAsPercentage(XPropertySet xProps,
        PropertyHelper filterData, String sName, String sOptionName) {
        int nValue = saveNumericOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName,Integer.toString(nValue)+"%");
        }
        return nValue;
    }
			
}



