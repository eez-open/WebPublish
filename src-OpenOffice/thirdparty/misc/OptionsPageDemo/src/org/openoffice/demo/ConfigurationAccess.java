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

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Provides generic factory methods around configuration access.
 * @author Christian Lins (christian.lins@sun.com)
 */
public class ConfigurationAccess
{
  /**
   * Creates a XNameAccess instance for read and write access to the
   * configuration at the given node path.
   * @param context
   * @param path
   * @return
   */
  public static XNameAccess createUpdateAccess(XComponentContext context, String path)
  {
    XNameAccess access;

    // Create the com.sun.star.configuration.ConfigurationUpdateAccess
    // for the registry node which contains the data for our option
    // pages.
    XMultiServiceFactory xConfig;

    try
    {
      xConfig = (XMultiServiceFactory) UnoRuntime.queryInterface(
        XMultiServiceFactory.class,
        context.getServiceManager().createInstanceWithContext(
          "com.sun.star.configuration.ConfigurationProvider", context));
    }
    catch (com.sun.star.uno.Exception e)
    {
      e.printStackTrace();
      return null;
    }

    // One argument for creating the ConfigurationUpdateAccess is the "nodepath".
    // Our nodepath point to the node of which the direct subnodes represent the
    // different options pages.
    Object[] args = new Object[1];
    args[0] = new PropertyValue("nodepath", 0, path,PropertyState.DIRECT_VALUE);
    
     // We get the com.sun.star.container.XNameAccess from the instance of
    // ConfigurationUpdateAccess and save it for later use.
    try
    {
      access = (XNameAccess) UnoRuntime.queryInterface(
        XNameAccess.class, xConfig.createInstanceWithArguments(
          "com.sun.star.configuration.ConfigurationUpdateAccess", args));
    }
    catch (com.sun.star.uno.Exception e)
    {
      e.printStackTrace();
      return null;
    }

    return access;
  }
}
