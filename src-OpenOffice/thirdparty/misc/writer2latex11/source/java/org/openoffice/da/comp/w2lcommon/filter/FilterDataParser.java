/************************************************************************
 *
 *  FilterDataParser.java
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
 *  Version 1.0 (2008-11-22)
 *
 */ 
 
package org.openoffice.da.comp.w2lcommon.filter;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import com.sun.star.beans.PropertyValue;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.ucb.CommandAbortedException;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XStringSubstitution;

import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToOutputStreamAdapter;

import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import writer2latex.api.Converter;


/** This class parses the FilterData property passed to the filter and
 *  applies it to a <code>Converter</code>
 *  All errors are silently ignored
 */
public class FilterDataParser {
    
    //private static XComponentContext xComponentContext = null;
    
    private XSimpleFileAccess2 sfa2;
    private XStringSubstitution xPathSub;
    
    public FilterDataParser(XComponentContext xComponentContext) {
        //this.xComponentContext = xComponentContext;

        // Get the SimpleFileAccess service
        sfa2 = null;
        try {
            Object sfaObject = xComponentContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.ucb.SimpleFileAccess", xComponentContext);
            sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get SimpleFileAccess service (should not happen)
        }
        
        // Get the PathSubstitution service
        xPathSub = null;
        try {
            Object psObject = xComponentContext.getServiceManager().createInstanceWithContext(
               "com.sun.star.util.PathSubstitution", xComponentContext);
            xPathSub = (XStringSubstitution) UnoRuntime.queryInterface(XStringSubstitution.class, psObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get PathSubstitution service (should not happen)
        }     
    }
    
    /** Apply the given FilterData property to the given converter
     *  @param data an Any containing the FilterData property
     *  @param converter a <code>writer2latex.api.Converter</code> implementation
     */
    public void applyFilterData(Object data, Converter converter) {
        // Get the array from the data, if possible
        PropertyValue[] filterData = null;
        if (AnyConverter.isArray(data)) {
            try {
                Object[] arrayData = (Object[]) AnyConverter.toArray(data);
                if (arrayData instanceof PropertyValue[]) {
                    filterData = (PropertyValue[]) arrayData;
                }
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to array; should not happen - ignore   
            }
        }
        if (filterData==null) { return; }
        
        PropertyHelper props = new PropertyHelper(filterData);
        
        // Get the special properties TemplateURL, ConfigURL and AutoCreate
        Object tpl = props.get("TemplateURL");
        String sTemplate = null;
        if (tpl!=null && AnyConverter.isString(tpl)) {
            try {
                sTemplate = substituteVariables(AnyConverter.toString(tpl));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }
        
        Object auto = props.get("AutoCreate");
        boolean bAutoCreate = false;
        if (auto!=null && AnyConverter.isString(auto)) {
            try {
                if ("true".equals(AnyConverter.toString(auto))) {
                    bAutoCreate = true;
                }
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }
        
        Object cfg = props.get("ConfigURL");
        String sConfig = null;
        if (cfg!=null && AnyConverter.isString(cfg)) {
            try {
                sConfig = substituteVariables(AnyConverter.toString(cfg));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }

        // Load the template from the specified URL, if any
        if (sfa2!=null && sTemplate!=null && sTemplate.length()>0) {
            try {
                XInputStream xIs = sfa2.openFileRead(sTemplate);
                if (xIs!=null) {
                    InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                    converter.readTemplate(is);
                    is.close();
                    xIs.closeInput();
                }
            }
            catch (IOException e) {
                // ignore
            }
            catch (NotConnectedException e) {
                // ignore
            }
            catch (CommandAbortedException e) {
                // ignore
            }
            catch (com.sun.star.uno.Exception e) {
                // ignore
            }
        }

        // Create config if required
        try {
            if (bAutoCreate && sfa2!=null && sConfig!=null && !sConfig.startsWith("*") && !sfa2.exists(sConfig)) {
                // Note: Requires random access, ie. must be a file URL:
                XOutputStream xOs = sfa2.openFileWrite(sConfig);
                if (xOs!=null) {
                    OutputStream os = new XOutputStreamToOutputStreamAdapter(xOs);
                    converter.getConfig().write(os);
                    os.flush();
                    os.close();
                    xOs.closeOutput();
                }
            }
        }
        catch (IOException e) {
            // ignore
        }
        catch (NotConnectedException e) {
            // ignore
        }
        catch (CommandAbortedException e) {
            // Ignore
        }
        catch (com.sun.star.uno.Exception e) {
          // Ignore
        }

        // Load the configuration from the specified URL, if any
        if (sConfig!=null) {
            if (sConfig.startsWith("*")) { // internal configuration
                try {
                    converter.getConfig().readDefaultConfig(sConfig.substring(1)); 
                }
                catch (IllegalArgumentException e) {
                    // ignore
                }
            }
            else if (sfa2!=null) { // real URL
                try {
                    XInputStream xIs = sfa2.openFileRead(sConfig);;
                    if (xIs!=null) {
                        InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                        converter.getConfig().read(is);
                        is.close();
                        xIs.closeInput();
                    }
                }
                catch (IOException e) {
                    // Ignore
                }
                catch (NotConnectedException e) {
                    // Ignore
                }
                catch (CommandAbortedException e) {
                    // Ignore
                }
                catch (com.sun.star.uno.Exception e) {
                    // Ignore
                }
            }
        }
        
        // Read further configuration properties
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String sKey = (String) keys.nextElement();
            if (!"ConfigURL".equals(sKey) && !"TemplateURL".equals(sKey) && !"AutoCreate".equals(sKey)) {
                Object value = props.get(sKey);
                if (AnyConverter.isString(value)) {
                    try {
                        converter.getConfig().setOption(sKey,AnyConverter.toString(value));
                    }
                    catch (com.sun.star.lang.IllegalArgumentException e) {
                        // Failed to convert to String; should not happen - ignore   
                    }
                }
            } 
        }
    }
    
    private String substituteVariables(String sUrl) {
        if (xPathSub!=null) {
            try {
                return xPathSub.substituteVariables(sUrl, false);
            }
            catch (com.sun.star.container.NoSuchElementException e) {
                // Found an unknown variable, no substitution
                // (This will only happen if false is replaced by true above)
                return sUrl;
            }
        }
        else { // Not path substitution available
            return sUrl;
        }
    }
    	
}



