/*
 * OfficeUNOClientApp.java
 *
 * Created on 2011.03.21 - 12:36:18
 *
 */

package com.example;

/**
 *
 * @author martin
 */
public class OfficeUNOClientApp {
    
    /** Creates a new instance of OfficeUNOClientApp */
    public OfficeUNOClientApp() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
  try {
            com.sun.star.uno.XComponentContext xContext =
                    com.sun.star.comp.helper.Bootstrap.bootstrap();

            com.sun.star.uno.XInterface xInterface =
                    (com.sun.star.uno.XInterface)
                        xContext.getServiceManager().createInstanceWithContext(
                            "com.sun.star.frame.ModuleManager", xContext);

            com.sun.star.container.XNameAccess xNameAccess =
                    (com.sun.star.container.XNameAccess)
                        com.sun.star.uno.UnoRuntime.queryInterface(
                            com.sun.star.container.XNameAccess.class, xInterface);

            String[] modules = xNameAccess.getElementNames();
            for (String module : modules) {
                System.out.println( module );
            }

        } catch (com.sun.star.comp.helper.BootstrapException e){
            e.printStackTrace();
        } catch (com.sun.star.uno.Exception e){
            e.printStackTrace();
        } catch (java.lang.Exception e){
            e.printStackTrace();
        } finally {
            System.exit( 0 );
        }
    }



}
