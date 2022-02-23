/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.GUIHelper;

import com.envox.webpublish.Util;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDateField;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTimeField;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.awt.XAnimation;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.DateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author martin
 */
public class Dialog {
    protected XDialog m_xDialog;
    protected ControlContainer m_controlContainer;

    protected void createDialog(String dialogName) throws Exception {
        m_xDialog = Util.createDialog(dialogName);
        m_controlContainer = new ControlContainer((XControlContainer)
                UnoRuntime.queryInterface(XControlContainer.class, m_xDialog));
    }

    protected void insertControl(String type, String name, int X, int Y, int Width, int Height) {
        try {
            XControl xDialogControl = (XControl) UnoRuntime.queryInterface(XControl.class, m_xDialog);
            XControlModel xDialogModel = null;
            if (xDialogControl != null) {
                xDialogModel = xDialogControl.getModel();
            }

            XMultiServiceFactory xDialogFactory = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, xDialogModel);
            if (xDialogFactory != null) {
                XControlModel xThrobberModel = (XControlModel) UnoRuntime.queryInterface(XControlModel.class, xDialogFactory.createInstance(type));
                XPropertySet xThrobberProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xThrobberModel);
                if (xThrobberProps != null) {
                    xThrobberProps.setPropertyValue("Name", name);
                    xThrobberProps.setPropertyValue("PositionX", new Integer(X));
                    xThrobberProps.setPropertyValue("PositionY", new Integer(Y));
                    xThrobberProps.setPropertyValue("Width", new Integer(Width));
                    xThrobberProps.setPropertyValue("Height", new Integer(Height));

                    XNameContainer xDialogContainer = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, xDialogModel);
                    xDialogContainer.insertByName("throbberWebPublishDialog", xThrobberModel);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void insertThrobber(int X, int Y, int Width, int Height) {
        insertControl("com.sun.star.awt.SpinningProgressControlModel", "throbberWebPublishDialog", X, Y, Width, Height);
        setThrobberVisible(false);
    }

    protected void setThrobberActive(boolean bActive) {
        XAnimation xThrobber = m_controlContainer.getThrobber("throbberWebPublishDialog");
        if (xThrobber != null) {
            if (bActive) {
                xThrobber.startAnimation();
            } else {
                xThrobber.stopAnimation();
            }
            setThrobberVisible(bActive);
        }
    }

    protected void setThrobberVisible(boolean bVisible) {
        XWindow xWindow = m_controlContainer.getWindow("throbberWebPublishDialog");
        if (xWindow != null) {
            xWindow.setVisible(bVisible);
        }
    }

    protected void toDateTimeControl(DateTime date, XDateField dateField, XTimeField timeField) {
        if (date != null) {
            dateField.setDate(Integer.parseInt(String.format("%d%02d%02d",
                date.Year, date.Month, date.Day)));
            timeField.setTime(Integer.parseInt(String.format("%02d%02d%02d%02d",
                date.Hours, date.Minutes, date.Seconds, date.HundredthSeconds)));
        }
    }

    protected DateTime fromDateTimeControl(XDateField dateField, XTimeField timeField) {
        if (dateField.getDate() == 0) {
            return null;
        }

        DateTime date = new DateTime();

        date.Year = (short) (dateField.getDate() / 10000);
        date.Month = (short) ((dateField.getDate() % 10000) / 100);
        date.Day = (short) (dateField.getDate() % 100);

        date.Hours = (short) (timeField.getTime() / 1000000);
        date.Minutes = (short) ((timeField.getTime() % 1000000) / 10000);
        date.Seconds = (short) ((timeField.getTime() % 10000) / 100);
        date.HundredthSeconds = (short) (timeField.getTime() % 100);

        return date;
    }

    protected void addItems(String[] strs, XListBox xListBox) {
        if (strs == null)
            return;

        for (String str:strs) {
            String[] items = xListBox.getItems();

            short i;

            for (i = 0; i < items.length; ++i) {
                if (items[i].compareTo(str) == 0) {
                    break;
                } else if (items[i].compareTo(str) > 0) {
                    xListBox.addItem(str, i);
                    break;
                }
            }

            if (i == items.length) {
                xListBox.addItem(str, i);
            }
        }
    }


    protected void selectItems(String[] strs, XListBox xListBox) {
        if (strs == null)
            return;

        short[] positions = new short[strs.length];

        int i = 0;
        for (String str:strs) {
            String[] items = xListBox.getItems();
            for (short j = 0; j < items.length; ++j) {
                if (items[j].compareTo(str) == 0) {
                    positions[i++] = j;
                    break;
                }
            }
        }

        xListBox.selectItemsPos(positions, true);
    }
}
