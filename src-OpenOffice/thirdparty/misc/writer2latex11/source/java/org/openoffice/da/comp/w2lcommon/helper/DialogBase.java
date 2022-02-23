/************************************************************************
 *
 *  DialogBase.java
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
 *  Version 1.0 (2008-09-11)
 *
 */ 
 
package org.openoffice.da.comp.w2lcommon.helper;

import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogEventHandler;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;


/** This class provides an abstract uno component which implements a dialog
 *  from an xml description (using the DialogProvider2 service)
 */
public abstract class DialogBase implements
        XTypeProvider, XServiceInfo, XServiceName, // Uno component
        XExecutableDialog, // Execute the dialog
        XDialogEventHandler { // Event handling for dialog

		
    //////////////////////////////////////////////////////////////////////////
    // The subclass must override the following; and override the
    // implementation of XDialogEventHandler if needed
    
    /** The component will be registered under this name.
     *  The subclass must override this with a suitable name
     */
    public static String __serviceName = "";

    /** The component should also have an implementation name.
     *  The subclass must override this with a suitable name
     */
    public static String __implementationName = "";
	
    /** Return the name of the library containing the dialog
     *  The subclass must override this to provide the name of the library
     */
    public abstract String getDialogLibraryName();

    /** Return the name of the dialog within the library
     *  The subclass must override this to provide the name of the dialog
     */
    public abstract String getDialogName();
	
    /** Initialize the dialog (eg. with settings from the registry)
     *  The subclass must implement this
     */
    protected abstract void initialize();
	
    /** Finalize the dialog after execution (eg. save settings to the registry)
     *  The subclass must implement this
     */
    protected abstract void finalize();
	
    //////////////////////////////////////////////////////////////////////////
    // Some constants
	
    // State of a checkbox
    protected static final short CHECKBOX_NOT_CHECKED = 0;
    protected static final short CHECKBOX_CHECKED = 1;
    protected static final short CHECKBOX_DONT_KNOW = 2;
	

    //////////////////////////////////////////////////////////////////////////
    // Some private global variables

    // The component context (from constructor)
    protected XComponentContext xContext;
	
    // The dialog (created by XExecutableDialog implementation)
    private XDialog xDialog;
    private String sTitle;
	
	
    //////////////////////////////////////////////////////////////////////////
    // The constructor
	
    /** Create a new OptionsDialogBase */
    public DialogBase(XComponentContext xContext) {
        this.xContext = xContext;
        xDialog = null;
        sTitle = null;
    }
	

    //////////////////////////////////////////////////////////////////////////
    // Implement uno interfaces
	
    // Implement the interface XTypeProvider
    public Type[] getTypes() {
        Type[] typeReturn = {};
        try {
            typeReturn = new Type[] {
            new Type( XServiceName.class ),
            new Type( XServiceInfo.class ),
            new Type( XTypeProvider.class ),
            new Type( XExecutableDialog.class ),
            new Type( XDialogEventHandler.class ) };
        } catch(Exception exception) {
        }
        return typeReturn;
    }

    public byte[] getImplementationId() {
        byte[] byteReturn = {};
        byteReturn = new String( "" + this.hashCode() ).getBytes();
        return( byteReturn );
    }


    // Implement the interface XServiceName
    public String getServiceName() {
        return __serviceName;
    }


    // Implement the interface XServiceInfo
    public boolean supportsService(String sServiceName) {
        return sServiceName.equals(__serviceName);
    }

    public String getImplementationName() {
        return __implementationName;
    }
    
    public String[] getSupportedServiceNames() {
        String[] sSupportedServiceNames = { __serviceName };
        return sSupportedServiceNames;
    }
	

    // Implement the interface XExecutableDialog
    public void setTitle(String sTitle) {
        this.sTitle = sTitle;
    }
	
    public short execute() {
        try {
            // Create the dialog
            XMultiComponentFactory xMCF = xContext.getServiceManager();
            Object provider = xMCF.createInstanceWithContext(
                "com.sun.star.awt.DialogProvider2", xContext);
            XDialogProvider2 xDialogProvider = (XDialogProvider2)
                UnoRuntime.queryInterface(XDialogProvider2.class, provider);
            String sDialogUrl = "vnd.sun.star.script:"+getDialogLibraryName()+"."
                +getDialogName()+"?location=application";
            xDialog = xDialogProvider.createDialogWithHandler(sDialogUrl, this);
            if (sTitle!=null) { xDialog.setTitle(sTitle); }

            // Do initialization using method from subclass
            initialize();

            // Execute the dialog
            short nResult = xDialog.execute();

            if (nResult == ExecutableDialogResults.OK) {
                // Finalize after execution of dialog using method from subclass
                finalize();
            }
            xDialog.endExecute();
            return nResult;
        }
        catch (Exception e) {
MessageBox msgBox = new MessageBox(xContext);
msgBox.showMessage("Error",e.toString()+" "+e.getStackTrace()[0].toString());

            // continue as if the dialog was executed OK
            return ExecutableDialogResults.OK; 
        }
    }


    // Implement the interface XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        // Do nothing, leaving the responsibility to the subclass
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        // We do not support any method names, subclass should take care of this
        return new String[0];
    }
	
	
    //////////////////////////////////////////////////////////////////////////
    // Helpers to access controls in the dialog (to be used by the subclass)
    // Note: The helpers fail silently if an exception occurs. Could query the
    // the ClassId property for the control type and check that the property
    // exists to ensure a correct behaviour in all cases, but as long as the
    // helpers are used correctly, this doesn't really matter.
	
    // Get the properties of a named control in the dialog
    private XPropertySet getControlProperties(String sControlName) {
        XControlContainer xContainer = (XControlContainer)
            UnoRuntime.queryInterface(XControlContainer.class, xDialog);
        XControl xControl = xContainer.getControl(sControlName);
        XControlModel xModel = xControl.getModel();
        XPropertySet xPropertySet = (XPropertySet)
            UnoRuntime.queryInterface(XPropertySet.class, xModel);
        return xPropertySet;
    }

	
    protected void setControlEnabled(String sControlName, boolean bEnabled) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("Enabled", new Boolean(bEnabled));
        }
        catch (Exception e) {
            // Will fail if the control does not exist
        }
    }
	
    protected short getCheckBoxState(String sControlName) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            return ((Short) xPropertySet.getPropertyValue("State")).shortValue();
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a checkbox
            return CHECKBOX_DONT_KNOW;
        }
    }
	
    protected boolean getCheckBoxStateAsBoolean(String sControlName) {
	    return getCheckBoxState(sControlName)==CHECKBOX_CHECKED;
    }
	
    protected void setCheckBoxState(String sControlName, short nState) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("State",new Short(nState));
        }
        catch (Exception e) {
            // will fail if the control does not exist or is not a checkbox or
            // nState has an illegal value
        }
    }
	
    protected void setCheckBoxStateAsBoolean(String sControlName, boolean bChecked) {
	    setCheckBoxState(sControlName,bChecked ? CHECKBOX_CHECKED : CHECKBOX_NOT_CHECKED);
    }
	
    protected String[] getListBoxStringItemList(String sControlName) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            return (String[]) xPropertySet.getPropertyValue("StringItemList");
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a list box
            return new String[0];
        }
    }
	
    protected void setListBoxStringItemList(String sControlName, String[] items) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("StringItemList",items);
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a list box
        }
    }
	
    protected short getListBoxSelectedItem(String sControlName) {
        // Returns the first selected element in case of a multiselection
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            short[] selection = (short[]) xPropertySet.getPropertyValue("SelectedItems");
            return selection[0];
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a list box
            return -1;
        }
    }
	
    protected void setListBoxSelectedItem(String sControlName, short nIndex) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            short[] selection = new short[1];
            selection[0] = nIndex;
            xPropertySet.setPropertyValue("SelectedItems",selection);
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a list box or
            // nIndex is an illegal value
        }
    }
	
    protected short getListBoxLineCount(String sControlName) {
        // Returns the first selected element in case of a multiselection
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            return ((Short) xPropertySet.getPropertyValue("LineCount")).shortValue();
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a list box
            return 0;
        }
    }
	
    protected void setListBoxLineCount(String sControlName, short nLineCount) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("LineCount",new Short(nLineCount));
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a list box or
            // nLineCount is an illegal value
        }
    }
	
    protected String getComboBoxText(String sControlName) {
        // Returns the text of a combobox
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            return (String) xPropertySet.getPropertyValue("Text");
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a combo
            return "";
        }
    }
	
    protected void setComboBoxText(String sControlName, String sText) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("Text", sText);
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a combo box or
            // nText is an illegal value
        }
    }
	
    protected String getTextFieldText(String sControlName) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            return (String) xPropertySet.getPropertyValue("Text");
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a text field
            return "";
        }
    }
	
    protected void setTextFieldText(String sControlName, String sText) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("Text",sText);
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a text field
        }
    }
	
    protected String getFormattedFieldText(String sControlName) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            return (String) xPropertySet.getPropertyValue("Text");
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a formatted field
            return "";
        }
    }
	
    protected void setFormattedFieldText(String sControlName, String sText) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("Text",sText);
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a formatted field
        }
    }
	
    protected int getNumericFieldValue(String sControlName) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            return ((Double) xPropertySet.getPropertyValue("Value")).intValue();
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a numeric field
            return 0;
        }
    }
	
    protected void setNumericFieldValue(String sControlName, int nValue) {
        XPropertySet xPropertySet = getControlProperties(sControlName);
        try {
            xPropertySet.setPropertyValue("Value",new Double(nValue));
        }
        catch (Exception e) {
            // Will fail if the control does not exist or is not a numeric field
        }
    }
	
    // Helpers for access to an XPropertySet. The helpers will fail silently if
    // names or data is provided, but the subclass is expected to use them with
    // correct data only...
    protected Object getPropertyValue(XPropertySet xProps, String sName) {
        try {
            return xProps.getPropertyValue(sName);
        }
        catch (UnknownPropertyException e) {
            return null;
        }
        catch (WrappedTargetException e) {
            return null;
        }
    } 
	
    protected void setPropertyValue(XPropertySet xProps, String sName, Object value) {
        try {
            xProps.setPropertyValue(sName,value);
        }
        catch (UnknownPropertyException e) {
        }
        catch (PropertyVetoException e) { // unacceptable value
        }
        catch (IllegalArgumentException e) {
        }
        catch (WrappedTargetException e) {
        }
    }
	
    protected String getPropertyValueAsString(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof String ? (String) value : "";
    }
	
    protected int getPropertyValueAsInteger(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }
	
    protected void setPropertyValue(XPropertySet xProps, String sName, int nValue) {
        setPropertyValue(xProps,sName,new Integer(nValue));
    }

    protected short getPropertyValueAsShort(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Short ? ((Short) value).shortValue() : 0;
    }
	
    protected void setPropertyValue(XPropertySet xProps, String sName, short nValue) {
        setPropertyValue(xProps,sName,new Short(nValue));
    }

    protected boolean getPropertyValueAsBoolean(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : false;
    }
	
    protected void setPropertyValue(XPropertySet xProps, String sName, boolean bValue) {
        setPropertyValue(xProps,sName,new Boolean(bValue));
    }

}



