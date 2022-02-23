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

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XModel;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

/**
 * Creates a "Demo" menu entry and a toolbar.
 * @author Christian Lins (christian.lins@sun.com)
 */
public final class OptionPageDemo extends WeakBase
  implements com.sun.star.lang.XServiceInfo,
             com.sun.star.frame.XDispatchProvider,
             com.sun.star.lang.XInitialization,
             com.sun.star.frame.XDispatch
{
  private static final String   implementationName = OptionPageDemo.class.getName();
  private static final String[] serviceNames = {
        "com.sun.star.frame.ProtocolHandler" };

  private final XComponentContext   context;
  private com.sun.star.frame.XFrame frame;
  private XNameAccess accessLeaves;

  public OptionPageDemo(XComponentContext context)
  {
    this.context = context;
    this.accessLeaves = ConfigurationAccess.createUpdateAccess(context,
      "/org.openoffice.demo.OptionsPageDemo/Leaves");
  };

  /**
   * Is called by CentralRegistrationClass.
   * @param sImplementationName
   * @return
   */
  public static XSingleComponentFactory __getComponentFactory( String sImplementationName )
  {
    XSingleComponentFactory xFactory = null;

    if ( sImplementationName.equals( implementationName ) )
      xFactory = Factory.createComponentFactory(OptionPageDemo.class, serviceNames);
    return xFactory;
  }

  /**
   * Is called by CentralRegistrationClass.
   * @param registryKey
   * @return
   */
  public static boolean __writeRegistryServiceInfo( XRegistryKey registryKey )
  {
    return Factory.writeRegistryServiceInfo(implementationName,
                                            serviceNames,
                                            registryKey);
  }


  public String getImplementationName()
  {
    return implementationName;
  }

  public boolean supportsService( String service )
  {
    for(String supportedService : serviceNames)
    {
      if(supportedService.equals(service))
        return true;
    }
    return false;
  }

  public String[] getSupportedServiceNames()
  {
    return serviceNames;
  }

  /**
   * Is called by the OOo event system when the user clicks on the "Demo"
   * menu item or the toolbar button.
   * @param aURL
   * @param sTargetFrameName
   * @param iSearchFlags
   * @return
   */
  public com.sun.star.frame.XDispatch queryDispatch(com.sun.star.util.URL aURL,
                                                    String sTargetFrameName,
                                                    int iSearchFlags)
  {
    if(aURL.Protocol.compareTo("org.openoffice.demo.optionpagedemo:") == 0)
    {
      if(aURL.Path.compareTo("DemoOptionCommand") == 0)
      {
        return this;
      }
    }
    return null;
  }

  // com.sun.star.frame.XDispatchProvider:
  public com.sun.star.frame.XDispatch[] queryDispatches(
         com.sun.star.frame.DispatchDescriptor[] seqDescriptors )
  {
    int cnt = seqDescriptors.length;
    com.sun.star.frame.XDispatch[] seqDispatcher =
      new com.sun.star.frame.XDispatch[seqDescriptors.length];

    for(int i=0; i < cnt; i++)
    {
      seqDispatcher[i] = queryDispatch(seqDescriptors[i].FeatureURL,
                                       seqDescriptors[i].FrameName,
                                       seqDescriptors[i].SearchFlags );
    }
    return seqDispatcher;
  }

  // com.sun.star.lang.XInitialization:
  public void initialize(Object[] object)
    throws com.sun.star.uno.Exception
  {
    if (object.length > 0)
    {
      frame = (com.sun.star.frame.XFrame)UnoRuntime.queryInterface(
                com.sun.star.frame.XFrame.class, object[0]);
    }
  }

  // com.sun.star.frame.XDispatch:
  public void dispatch( com.sun.star.util.URL aURL,
                        com.sun.star.beans.PropertyValue[] aArguments )
  {
    if(aURL.Protocol.compareTo("org.openoffice.demo.optionpagedemo:") == 0)
    {
      if ( aURL.Path.compareTo("DemoOptionCommand") == 0 )
      {
        // Write out to Office
        XModel xDocModel = this.frame.getController().getModel();

        // Getting the text document object
        XTextDocument xtextdocument = (XTextDocument) UnoRuntime.queryInterface(
                            XTextDocument.class, xDocModel);

        XText xText = xtextdocument.getText();

        // Construct the string we want to output to the Office document
        StringBuffer buf = new StringBuffer();
        buf.append("OptionsPageDemo - current values stored in OOo configuration:\n");
        buf.append("===============================================================\n");

        for(String controlName : DialogEventHandler._DialogEventHandler.ControlNames)
        {
          try
          {
            // Retrieve the configuration values from the OOo registry.
            // load the values from the registry
            // To access the registry we have previously created a service instance
            // of com.sun.star.configuration.ConfigurationUpdateAccess which supports
            // com.sun.star.container.XNameAccess. We obtain now the section
            // of the registry which is assigned to this options page.
            XPropertySet xLeaf = (XPropertySet) UnoRuntime.queryInterface(
            XPropertySet.class, this.accessLeaves.getByName("FooOptionsPage"));
            if (xLeaf == null)
              buf.append("XPropertySet not supported.");

            // The properties in the registry have the same name as the respective
            // controls. We use the names now to obtain the property values.

            Object value = xLeaf.getPropertyValue(controlName);
            if(controlName.startsWith("lst"))
            {
              value = ((String[])value)[
                ((short[])xLeaf.getPropertyValue(controlName + "Selected"))[0]];
            }

            buf.append(controlName);
            buf.append(": ");
            buf.append(value);
            buf.append("\n");
          }
          catch(Exception ex)
          {
            buf.append(ex.getLocalizedMessage());
            buf.append('\n');
          }
        }
        buf.append('\n');

        XTextRange xTextRange = xText.getEnd();
        xTextRange.setString(buf.toString());
        return;
      }
    }
  }

  public void addStatusListener( com.sun.star.frame.XStatusListener xControl,
                                 com.sun.star.util.URL aURL )
  {
    // add your own code here
  }

  public void removeStatusListener( com.sun.star.frame.XStatusListener xControl,
                                    com.sun.star.util.URL aURL )
  {
    // add your own code here
  }

}
