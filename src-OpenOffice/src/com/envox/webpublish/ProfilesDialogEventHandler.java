/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.envox.webpublish;

import com.envox.webpublish.GUIHelper.ControlContainer;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.lang.EventObject;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.Exception;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XMouseListener;
import com.sun.star.uno.UnoRuntime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A handler which supports an options page which with different controls.
 * Can be easily adapted to support multiple options pages.
 * @author OpenOffice.org
 */
public class ProfilesDialogEventHandler {

    public static class _ProfilesDialogEventHandler extends WeakBase
            implements XServiceInfo, XContainerWindowEventHandler, XActionListener, XItemListener, XMouseListener {

        static private final String __serviceName = "com.envox.webpublish.ProfilesDialogEventHandler";
        private XComponentContext context;

        ProfilesDialogHandler m_dialogHandler;

        public _ProfilesDialogEventHandler(XComponentContext xCompContext) {
            this.context = xCompContext;
            m_dialogHandler = new ProfilesDialogHandler();
        }

        /**
         * This method returns an array of all supported service names.
         * @return Array of supported service names.
         */
        public String[] getSupportedServiceNames() {
            return getServiceNames();
        }

        /**
         * This method is a simple helper function to used in the
         * static component initialisation functions as well as in
         * getSupportedServiceNames.
         */
        public static String[] getServiceNames() {
            String[] sSupportedServiceNames = {__serviceName};
            return sSupportedServiceNames;
        }

        /** This method returns true, if the given service will be
         * supported by the component.
         * @param sServiceName Service name.
         * @return True, if the given service name will be supported.
         */
        public boolean supportsService(String sServiceName) {
            return sServiceName.equals(__serviceName);
        }

        /**
         * Return the class name of the component.
         * @return Class name of the component.
         */
        public String getImplementationName() {
            return _ProfilesDialogEventHandler.class.getName();
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
                throws WrappedTargetException {
            if (sMethod.equals("external_event")) {
                try {
                    return handleExternalEvent(aWindow, aEventObject);
                } catch (com.sun.star.uno.RuntimeException re) {
                    throw re;
                } catch (com.sun.star.uno.Exception ex) {
                    Logger.getLogger(_ProfilesDialogEventHandler.class.getName()).log(Level.SEVERE, null, ex);
                    throw new WrappedTargetException(sMethod, this, ex);
                }
            }

            // return false when event was not handled
            return false;
        }

        /**
         * @return A String array containing the method names supported by this handler.
         */
        public String[] getSupportedMethodNames() {
            return new String[]{"external_event"};
        }

        private boolean handleExternalEvent(com.sun.star.awt.XWindow aWindow, Object aEventObject)
                throws com.sun.star.uno.Exception {
            try {
                String sMethod = AnyConverter.toString(aEventObject);
                if (sMethod.equals("ok")) {
                    saveData(aWindow);
                } else if (sMethod.equals("back") || sMethod.equals("initialize")) {
                    loadData(aWindow);
                }
            } catch (com.sun.star.lang.IllegalArgumentException ex) {
                Logger.getLogger(_ProfilesDialogEventHandler.class.getName()).log(Level.SEVERE, null, ex);
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
                throws com.sun.star.lang.IllegalArgumentException, com.sun.star.uno.Exception {
            m_dialogHandler.fromControls();
        }

        /**
         * Loads data from the configuration into the dialog.
         * @param aWindow
         * @throws com.sun.star.uno.Exception
         */
        private void loadData(com.sun.star.awt.XWindow aWindow)
                throws com.sun.star.uno.Exception {

            // To acces the separate controls of the window we need to obtain the
            // XControlContainer from window implementation
            XControlContainer xContainer = (XControlContainer) UnoRuntime.queryInterface(
                    XControlContainer.class, aWindow);
            if (xContainer == null) {
                throw new com.sun.star.uno.Exception(
                        "Could not get XControlContainer from window.", this);
            }

            m_dialogHandler.initContainer(new ControlContainer(xContainer), this, this, this);
            m_dialogHandler.initControls();
            m_dialogHandler.toControls();
            m_dialogHandler.updateControls();
        }

        public void actionPerformed(ActionEvent arg0) {
            m_dialogHandler.actionPerformed(arg0);
        }

        public void disposing(EventObject arg0) {
        }

        public void itemStateChanged(ItemEvent arg0) {
            m_dialogHandler.itemStateChanged(arg0);
        }

        public void mousePressed(MouseEvent arg0) {
            m_dialogHandler.mousePressed(arg0);
        }

        public void mouseReleased(MouseEvent arg0) {
        }

        public void mouseEntered(MouseEvent arg0) {
        }

        public void mouseExited(MouseEvent arg0) {
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
    public static XSingleComponentFactory __getComponentFactory(String sImplName) {
        System.out.println("ProfilesDialogEventHandler::_getComponentFactory");
        XSingleComponentFactory xFactory = null;

        if (sImplName.equals(_ProfilesDialogEventHandler.class.getName())) {
            xFactory = Factory.createComponentFactory(_ProfilesDialogEventHandler.class,
                    _ProfilesDialogEventHandler.getServiceNames());
        }

        return xFactory;
    }

    /**
     * Writes the service information into the given registry key.
     * This method is called by the CentralRegistrationClass.
     * @return returns true if the operation succeeded
     * @param regKey the registryKey
     * @see com.sun.star.comp.loader.JavaLoader
     */
    public static boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
        System.out.println("ProfilesDialogEventHandler::__writeRegistryServiceInfo");
        return Factory.writeRegistryServiceInfo(_ProfilesDialogEventHandler.class.getName(),
                _ProfilesDialogEventHandler.getServiceNames(),
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
    public void initialize(Object[] object)
            throws com.sun.star.uno.Exception {
    }
}
