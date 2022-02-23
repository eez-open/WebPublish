/*
 * CentralRegistrationClass.java
 *
 * Created on 2011.02.25 - 09:47:40
 *
 */
package com.envox.webpublish;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.registry.XRegistryKey;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class CentralRegistrationClass {
    
    public static XSingleComponentFactory __getComponentFactory( String sImplementationName ) {
        String regClassesList = getRegistrationClasses();
        StringTokenizer t = new StringTokenizer(regClassesList, " ");
        while (t.hasMoreTokens()) {
            String className = t.nextToken();
            if (className != null && className.length() != 0) {
                try {
                    Class regClass = Class.forName(className);
                    Method writeRegInfo = regClass.getDeclaredMethod("__getComponentFactory", new Class[]{String.class});
                    Object result = writeRegInfo.invoke(regClass, sImplementationName);
                    if (result != null) {
                       return (XSingleComponentFactory)result;
                    }
                }
                catch (ClassNotFoundException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassCastException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    public static boolean __writeRegistryServiceInfo( XRegistryKey xRegistryKey ) {
        boolean bResult = true;
        String regClassesList = getRegistrationClasses();
        StringTokenizer t = new StringTokenizer(regClassesList, " ");
        while (t.hasMoreTokens()) {
            String className = t.nextToken();
            if (className != null && className.length() != 0) {
                try {
                    Class regClass = Class.forName(className);
                    Method writeRegInfo = regClass.getDeclaredMethod("__writeRegistryServiceInfo", new Class[]{XRegistryKey.class});
                    Object result = writeRegInfo.invoke(regClass, xRegistryKey);
                    bResult &= ((Boolean)result).booleanValue();
                }
                catch (ClassNotFoundException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassCastException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return bResult;
    }

    private static String getRegistrationClasses() {
        CentralRegistrationClass c = new CentralRegistrationClass();
        String name = c.getClass().getCanonicalName().replace('.', '/').concat(".class");
        try {
            Enumeration<URL> urlEnum = c.getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (urlEnum.hasMoreElements()) {
                URL url = urlEnum.nextElement();
                String file = url.getFile();
                JarURLConnection jarConnection =
                    (JarURLConnection) url.openConnection();
                Manifest mf = jarConnection.getManifest();

                Attributes attrs = (Attributes) mf.getAttributes(name);
                if ( attrs != null ) {
                    String classes = attrs.getValue( "RegistrationClasses" );
                    return classes;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(CentralRegistrationClass.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        return "";
    }
    
    /** Creates a new instance of CentralRegistrationClass */
    private CentralRegistrationClass() {
    }
}
