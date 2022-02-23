/************************************************************************
 *
 *  W2XRegistration.java
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
 *  Version 1.0 (2008-10-04) 
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.registry.XRegistryKey;

import com.sun.star.comp.loader.FactoryHelper;

/** This class provides a static method to instantiate our uno components
 * on demand (__getServiceFactory()), and a static method to give
 * information about the components (__writeRegistryServiceInfo()).
 * Furthermore, it saves the XMultiServiceFactory provided to the
 * __getServiceFactory method for future reference by the componentes.
 */
public class W2XRegistration {
    
    public static XMultiServiceFactory xMultiServiceFactory;

    /**
     * Returns a factory for creating the service.
     * This method is called by the <code>JavaLoader</code>
     *
     * @return  returns a <code>XSingleServiceFactory</code> for creating the
     *          component
     *
     * @param   implName     the name of the implementation for which a
     *                       service is desired
     * @param   multiFactory the service manager to be used if needed
     * @param   regKey       the registryKey
     *
     * @see                  com.sun.star.comp.loader.JavaLoader
     */
    public static XSingleServiceFactory __getServiceFactory(String implName,
        XMultiServiceFactory multiFactory, XRegistryKey regKey) {
        xMultiServiceFactory = multiFactory;
        XSingleServiceFactory xSingleServiceFactory = null;
        if (implName.equals(W2XExportFilter.class.getName()) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(W2XExportFilter.class,
            W2XExportFilter.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(BatchConverter.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(BatchConverter.class,
            BatchConverter.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(XhtmlOptionsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(XhtmlOptionsDialog.class,
            XhtmlOptionsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(XhtmlOptionsDialogXsl.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(XhtmlOptionsDialogXsl.class,
            XhtmlOptionsDialogXsl.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(XhtmlOptionsDialogCalc.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(XhtmlOptionsDialogCalc.class,
            XhtmlOptionsDialogCalc.__serviceName,
            multiFactory,						    
            regKey);
        }
        
        return xSingleServiceFactory;
    }
    
    /**
     * Writes the service information into the given registry key.
     * This method is called by the <code>JavaLoader</code>
     * <p>
     * @return  returns true if the operation succeeded
     * @param   regKey       the registryKey
     * @see                  com.sun.star.comp.loader.JavaLoader
     */
    public static boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
        return
            FactoryHelper.writeRegistryServiceInfo(BatchConverter.__implementationName,
                BatchConverter.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(W2XExportFilter.__implementationName,
                W2XExportFilter.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(XhtmlOptionsDialog.__implementationName,
                XhtmlOptionsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(XhtmlOptionsDialogXsl.__implementationName,
                XhtmlOptionsDialogXsl.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(XhtmlOptionsDialogCalc.__implementationName,
                XhtmlOptionsDialogCalc.__serviceName, regKey);
    }
}

