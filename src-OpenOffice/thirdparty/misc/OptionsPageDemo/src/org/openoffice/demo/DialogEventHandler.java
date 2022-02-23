/*
 *  OptionsPageDemo - OpenOffice.org Demo Extension
 *  "How to access and update configuration data of an extension"
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You can use this code in your application without any restrictions.
 */

package org.openoffice.demo;

import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.Exception;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XControlContainer;
import com.sun.star.container.XNameAccess;
import com.sun.star.beans.XPropertySet;
import com.sun.star.util.XChangesBatch;

/**
 * A handler which supports an options page which with different controls.
 * Can be easily adapted to support multiple options pages.
 * @author OpenOffice.org
 */
public class DialogEventHandler
{
  public static class _DialogEventHandler extends WeakBase
    implements XServiceInfo, XContainerWindowEventHandler
  {
    /**
     * Names of supported options pages. The name derives from the
     * actual file names of the .xdl files not a XML attribute.
     */
    public static String[] SupportedWindowNames = {"FooOptionsPage"};

    /**
     * Names of the controls which are supported by this handler. All these
     * controls must have a "Text" property.
     */
    public static String[] ControlNames = {"chkFoobarA", "chkFoobarB", "lstLikeMost"};

    static private final String __serviceName = "org.openoffice.demo.DialogEventHandler";

    private XComponentContext context;
    private XNameAccess       accessLeaves;

    public _DialogEventHandler(XComponentContext xCompContext)
    {
      this.context = xCompContext;
      this.accessLeaves = ConfigurationAccess.createUpdateAccess(context,
        "/org.openoffice.demo.OptionsPageDemo/Leaves");
    }

    /**
     * This method returns an array of all supported service names.
     * @return Array of supported service names.
     */
    public String[] getSupportedServiceNames()
    {
      return getServiceNames();
    }

    /**
     * This method is a simple helper function to used in the
     * static component initialisation functions as well as in
     * getSupportedServiceNames.
     */
    public static String[] getServiceNames()
    {
      String[] sSupportedServiceNames = { __serviceName };
      return sSupportedServiceNames;
    }

    /** This method returns true, if the given service will be
      * supported by the component.
      * @param sServiceName Service name.
      * @return True, if the given service name will be supported.
      */
    public boolean supportsService( String sServiceName )
    {
      return sServiceName.equals( __serviceName );
    }

    /**
     * Return the class name of the component.
     * @return Class name of the component.
     */
    public String getImplementationName()
    {
      return _DialogEventHandler.class.getName();
    }

    /**
     * Is called by the OOo event system.
     * @param aWindow
     * @param aEventObject
     * @param sMethod
     * @return
     * @throws com.sun.star.lang.WrappedTargetException
     */
    public boolean callHandlerMethod(com.sun.star.awt.XWindow aWindow, Object aEventObject, String sMethod)
      throws WrappedTargetException
    {
      if (sMethod.equals("external_event") )
      {
        try
        {
          return handleExternalEvent(aWindow, aEventObject);
        }
        catch (com.sun.star.uno.RuntimeException re)
        {
          throw re;
        }
        catch (com.sun.star.uno.Exception e)
        {
          e.printStackTrace();
          throw new WrappedTargetException(sMethod, this, e);
        }
      }

      // return false when event was not handled
      return false;
    }

    /**
     * @return A String array containing the method names supported by this handler.
     */
    public String[] getSupportedMethodNames()
    {
      return new String[] {"external_event"};
    }

    private boolean handleExternalEvent(com.sun.star.awt.XWindow aWindow, Object aEventObject)
      throws com.sun.star.uno.Exception
    {
      try
      {
        String sMethod = AnyConverter.toString(aEventObject);
        if (sMethod.equals("ok"))
        {
          saveData(aWindow);
        }
        else if (sMethod.equals("back") || sMethod.equals("initialize"))
        {
          loadData(aWindow);
        }
      }
      catch (com.sun.star.lang.IllegalArgumentException ex)
      {
        ex.printStackTrace();
          throw new com.sun.star.lang.IllegalArgumentException(
            "Method external_event requires a string in the event object argument.",
            this, (short) -1);
      }
      return true;
    }

    /**
     * Saves data from the dialog into the configuration.
     * @param aWindow
     * @throws com.sun.star.lang.IllegalArgumentException
     * @throws com.sun.star.uno.Exception
     */
    private void saveData(com.sun.star.awt.XWindow aWindow)
      throws com.sun.star.lang.IllegalArgumentException, com.sun.star.uno.Exception
    {
      // Determine the name of the options page. This serves two purposes. First, if this
      // options page is supported by this handler and second we use the name two locate
      // the corresponding data in the registry.
      String sWindowName = getWindowName(aWindow);
      if (sWindowName == null)
        throw new com.sun.star.lang.IllegalArgumentException(
          "This window is not supported by this handler", this, (short) -1);

      // To access the separate controls of the window we need to obtain the
      // XControlContainer from the window implementation
      XControlContainer xContainer = (XControlContainer) UnoRuntime.queryInterface(
        XControlContainer.class, aWindow);
      if (xContainer == null)
        throw new com.sun.star.uno.Exception(
          "Could not get XControlContainer from window.", this);

      // This is an implementation which will be used for several options pages
      // which all have the same controls. m_arStringControls is an array which
      // contains the names.
      for (int i = 0; i < ControlNames.length; i++)
      {
        // To obtain the data from the controls we need to get their model.
        // First get the respective control from the XControlContainer.
        XControl xControl = xContainer.getControl(ControlNames[i]);

        // This generic handler and the corresponding registry schema support
        // up to five text controls. However, if a options page does not use all
        // five controls then we will not complain here.
        if (xControl == null)
          continue;

        // From the control we get the model, which in turn supports the
        // XPropertySet interface, which we finally use to get the data from
        // the control.
        XPropertySet xProp = (XPropertySet) UnoRuntime.queryInterface(
          XPropertySet.class, xControl.getModel());

        if (xProp == null)
          throw new com.sun.star.uno.Exception(
            "Could not get XPropertySet from control.", this);

        // Retrieve the data we want to store from the components.
        // We do not know which property contains data we want, so
        // we decide through the components name. This only works if all
        // components have been named properly:
        // Text fields start with "txt",
        // Check boxes with "chk",
        // List boxes with "lst"
        // You should adapt this behavior to your needs.
        Object   aObj  = null;
        Object[] value = new Object[1];
        String[] keys  = new String[] {ControlNames[i]};
        try
        {
          if(ControlNames[i].startsWith("txt"))
          {
            aObj     = xProp.getPropertyValue("Text");
            value[0] = AnyConverter.toString(aObj);
          }
          else if(ControlNames[i].startsWith("lst"))
          {
            keys  = new String[]{ControlNames[i] + "Selected", ControlNames[i]};
            value = new Object[2];

            // Read out indices of selected items
            aObj     = xProp.getPropertyValue("SelectedItems");
            value[0] = AnyConverter.toArray(aObj);

            // Read out items (they are read-only though, but perhaps someone wants to change this)
            aObj     = xProp.getPropertyValue("StringItemList");
            value[1] = AnyConverter.toArray(aObj);
          }
          else if(ControlNames[i].startsWith("chk"))
          {
            aObj     = xProp.getPropertyValue("State");
            value[0] = new Short(AnyConverter.toShort(aObj)).toString();
          }
        }
        catch (com.sun.star.lang.IllegalArgumentException ex)
        {
          ex.printStackTrace();
          throw new com.sun.star.lang.IllegalArgumentException(
            "Wrong property type.", this, (short) -1);
        }

        // Now we have the actual string value of the control. What we need now is
        // the XPropertySet of the respective property in the registry, so that we
        // can store the value.
        // To access the registry we have previously created a service instance
        // of com.sun.star.configuration.ConfigurationUpdateAccess which supports
        // com.sun.star.container.XNameAccess. The XNameAccess is used to get the
        // particular registry node which represents this options page.
        // Fortunately the name of the window is the same as the registry node.
        XPropertySet xLeaf = (XPropertySet) UnoRuntime.queryInterface(
          XPropertySet.class, accessLeaves.getByName(sWindowName));
        if (xLeaf == null)
          throw new com.sun.star.uno.Exception("XPropertySet not supported.", this);

        // Finally we can set the values
        for(int n = 0; n < keys.length; n++)
          xLeaf.setPropertyValue(keys[n], value[n]);
      }

      // Committing the changes will cause or changes to be written to the registry.
      XChangesBatch xUpdateCommit =
      (XChangesBatch) UnoRuntime.queryInterface(XChangesBatch.class, accessLeaves);
      xUpdateCommit.commitChanges();
    }

    /**
     * Loads data from the configuration into the dialog.
     * @param aWindow
     * @throws com.sun.star.uno.Exception
     */
    private void loadData(com.sun.star.awt.XWindow aWindow)
      throws com.sun.star.uno.Exception
    {
      // Determine the name of the window. This serves two purposes. First, if this
      // window is supported by this handler and second we use the name two locate
      // the corresponding data in the registry.
      String sWindowName = getWindowName(aWindow);
      if (sWindowName == null)
        throw new com.sun.star.lang.IllegalArgumentException(
          "The window is not supported by this handler", this, (short) -1);

      // To acces the separate controls of the window we need to obtain the
      // XControlContainer from window implementation
      XControlContainer xContainer = (XControlContainer) UnoRuntime.queryInterface(
        XControlContainer.class, aWindow);
      if (xContainer == null)
        throw new com.sun.star.uno.Exception(
          "Could not get XControlContainer from window.", this);

      // This is an implementation which will be used for several options pages
      // which all have the same controls. m_arStringControls is an array which
      // contains the names.
      for (int i = 0; i < ControlNames.length; i++)
      {
        // load the values from the registry
        // To access the registry we have previously created a service instance
        // of com.sun.star.configuration.ConfigurationUpdateAccess which supports
        // com.sun.star.container.XNameAccess. We obtain now the section
        // of the registry which is assigned to this options page.
        XPropertySet xLeaf = (XPropertySet) UnoRuntime.queryInterface(
          XPropertySet.class, this.accessLeaves.getByName(sWindowName));
        if (xLeaf == null)
          throw new com.sun.star.uno.Exception("XPropertySet not supported.", this);

        // The properties in the registry have the same name as the respective
        // controls. We use the names now to obtain the property values.
        Object aValue = xLeaf.getPropertyValue(ControlNames[i]);

        // Now that we have the value we need to set it at the corresponding
        // control in the window. The XControlContainer, which we obtained earlier
        // is the means to get hold of all the controls.
        XControl xControl = xContainer.getControl(ControlNames[i]);

        // This generic handler and the corresponding registry schema support
        // up to five text controls. However, if a options page does not use all
        // five controls then we will not complain here.
        if (xControl == null)
          continue;

        // From the control we get the model, which in turn supports the
        // XPropertySet interface, which we finally use to set the data at the
        // control
        XPropertySet xProp = (XPropertySet) UnoRuntime.queryInterface(
          XPropertySet.class, xControl.getModel());

        if (xProp == null)
          throw new com.sun.star.uno.Exception("Could not get XPropertySet from control.", this);

        // Some default handlings: you can freely adapt the behaviour to your
        // needs, this is only an example.
        // For text controls we set the "Text" property.
        if(ControlNames[i].startsWith("txt"))
        {
          xProp.setPropertyValue("Text", aValue);
        }
        // The available properties for a checkbox are defined in file
        // offapi/com/sun/star/awt/UnoControlCheckBoxModel.idl
        else if(ControlNames[i].startsWith("chk"))
        {
          xProp.setPropertyValue("State", aValue);
        }
        // The available properties for a checkbox are defined in file
        // offapi/com/sun/star/awt/UnoControlListBoxModel.idl
        else if(ControlNames[i].startsWith("lst"))
        {
          xProp.setPropertyValue("StringItemList", aValue);
          
          aValue = xLeaf.getPropertyValue(ControlNames[i] + "Selected");
          xProp.setPropertyValue("SelectedItems", aValue);
        }
      }
    }

    // Checks if the name property of the window is one of the supported names and returns
    // always a valid string or null
    private String getWindowName(com.sun.star.awt.XWindow aWindow)
      throws com.sun.star.uno.Exception
    {
      if (aWindow == null)
        new com.sun.star.lang.IllegalArgumentException(
          "Method external_event requires that a window is passed as argument",
          this, (short) -1);

      // We need to get the control model of the window. Therefore the first step is
      // to query for it.
      XControl xControlDlg = (XControl) UnoRuntime.queryInterface(
        XControl.class, aWindow);

      if (xControlDlg == null)
        throw new com.sun.star.uno.Exception(
          "Cannot obtain XControl from XWindow in method external_event.");
      // Now get model
      XControlModel xModelDlg = xControlDlg.getModel();

      if (xModelDlg == null)
        throw new com.sun.star.uno.Exception(
          "Cannot obtain XControlModel from XWindow in method external_event.", this);
      
      // The model itself does not provide any information except that its
      // implementation supports XPropertySet which is used to access the data.
      XPropertySet xPropDlg = (XPropertySet) UnoRuntime.queryInterface(
        XPropertySet.class, xModelDlg);
      if (xPropDlg == null)
        throw new com.sun.star.uno.Exception(
          "Cannot obtain XPropertySet from window in method external_event.", this);

      // Get the "Name" property of the window
      Object aWindowName = xPropDlg.getPropertyValue("Name");

      // Get the string from the returned com.sun.star.uno.Any
      String sName = null;
      try
      {
        sName = AnyConverter.toString(aWindowName);
      }
      catch (com.sun.star.lang.IllegalArgumentException ex)
      {
        ex.printStackTrace();
        throw new com.sun.star.uno.Exception(
          "Name - property of window is not a string.", this);
      }

      // Eventually we can check if we this handler can "handle" this options page.
      // The class has a member m_arWindowNames which contains all names of windows
      // for which it is intended
      for (int i = 0; i < SupportedWindowNames.length; i++)
      {
        if (SupportedWindowNames[i].equals(sName))
        {
          return sName;
        }
      }
      return null;
    }
  }

  /**
   * Gives a factory for creating the service.
   * This method is called by the CentralRegistrationClass.
   * @return returns a <code>XSingleComponentFactory</code> for creating
   * the component
   * @param sImplName the name of the implementation for which a
   * service is desired
   * @see com.sun.star.comp.loader.JavaLoader
   */
  public static XSingleComponentFactory __getComponentFactory(String sImplName)
  {
    System.out.println("DialogEventHandler::_getComponentFactory");
    XSingleComponentFactory xFactory = null;

    if ( sImplName.equals( _DialogEventHandler.class.getName() ) )
    xFactory = Factory.createComponentFactory(_DialogEventHandler.class,
    _DialogEventHandler.getServiceNames());

    return xFactory;
  }

  /**
   * Writes the service information into the given registry key.
   * This method is called by the CentralRegistrationClass.
   * @return returns true if the operation succeeded
   * @param regKey the registryKey
   * @see com.sun.star.comp.loader.JavaLoader
   */
  public static boolean __writeRegistryServiceInfo(XRegistryKey regKey)
  {
    System.out.println("DialogEventHandler::__writeRegistryServiceInfo");
    return Factory.writeRegistryServiceInfo(_DialogEventHandler.class.getName(),
      _DialogEventHandler.getServiceNames(),
      regKey);
  }

  /**
   * This method is a member of the interface for initializing an object
   * directly after its creation.
   * @param object This array of arbitrary objects will be passed to the
   * component after its creation.
   * @throws Exception Every exception will not be handled, but will be
   * passed to the caller.
   */
  public void initialize( Object[] object )
    throws com.sun.star.uno.Exception
  {}
}