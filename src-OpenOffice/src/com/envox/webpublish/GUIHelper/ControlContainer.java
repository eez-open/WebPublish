/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.GUIHelper;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDateField;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XProgressBar;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTimeField;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XAnimation;
import com.sun.star.uno.UnoRuntime;

/**
 *
 * @author martin
 */
public class ControlContainer {
    XControlContainer m_xControlContainer;

    public ControlContainer(XControlContainer xControlContainer) {
        m_xControlContainer = xControlContainer;
    }

    XControlContainer getXControlContainer() {
        return m_xControlContainer;
    }

    XControl[] getControls() {
        return m_xControlContainer.getControls();
    }

    public XControl getControl(String name) {
        return m_xControlContainer.getControl(name);
    }

    public XWindow getWindow(String name) {
        return (XWindow) UnoRuntime.queryInterface(XWindow.class, m_xControlContainer.getControl(name));
    }

    public XFixedText getFixedText(String name) {
        return (XFixedText) UnoRuntime.queryInterface(XFixedText.class, m_xControlContainer.getControl(name));
    }

    public XTextComponent getTextComponent(String name) {
        return (XTextComponent) UnoRuntime.queryInterface(XTextComponent.class, m_xControlContainer.getControl(name));
    }

    public XButton getButton(String name) {
        return (XButton) UnoRuntime.queryInterface(XButton.class, m_xControlContainer.getControl(name));
    }

    public XCheckBox getCheckBox(String name) {
        return (XCheckBox) UnoRuntime.queryInterface(XCheckBox.class, m_xControlContainer.getControl(name));
    }

    public XRadioButton getRadioButton(String name) {
        return (XRadioButton) UnoRuntime.queryInterface(XRadioButton.class, m_xControlContainer.getControl(name));
    }

    public XListBox getListBox(String name) {
        return (XListBox) UnoRuntime.queryInterface(XListBox.class, m_xControlContainer.getControl(name));
    }

    public XDateField getDateField(String name) {
        return (XDateField) UnoRuntime.queryInterface(XDateField.class, m_xControlContainer.getControl(name));
    }

    public XTimeField getTimeField(String name) {
        return (XTimeField) UnoRuntime.queryInterface(XTimeField.class, m_xControlContainer.getControl(name));
    }

    public XProgressBar getProgressBar(String name) {
        return (XProgressBar) UnoRuntime.queryInterface(XProgressBar.class, m_xControlContainer.getControl(name));
    }

    public XAnimation getThrobber(String name) {
        return (XAnimation) UnoRuntime.queryInterface(XAnimation.class, m_xControlContainer.getControl(name));
    }

    public void enableControls(boolean enable) {
        for (XControl control:getControls()) {
            ((XWindow) UnoRuntime.queryInterface(XWindow.class, control)).setEnable(enable);
        }
    }

    public void enableControls(String[] names, boolean enable) {
        for (String name:names) {
            getWindow(name).setEnable(enable);
        }
    }
}
