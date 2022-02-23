/************************************************************************
 *
 *  MessageBox.java
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
 *  Version 1.0 (2008-07-21)
 *
 */ 
 
package org.openoffice.da.comp.w2lcommon.helper;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** This class provides simple access to a uno awt message box
 */
public class MessageBox {

    private XFrame xFrame;
    private XToolkit xToolkit;
	
    /** Create a new MessageBox belonging to the current frame
     */
    public MessageBox(XComponentContext xContext) {
        this(xContext,null);
    }
	
    /** Create a new MessageBox belonging to a specific frame
     */
    public MessageBox(XComponentContext xContext, XFrame xFrame) {
        try {
            Object toolkit = xContext.getServiceManager()
                .createInstanceWithContext("com.sun.star.awt.Toolkit",xContext);
            xToolkit = (XToolkit) UnoRuntime.queryInterface(XToolkit.class,toolkit);
            if (xFrame==null) {
                Object desktop = xContext.getServiceManager()
                    .createInstanceWithContext("com.sun.star.frame.Desktop",xContext);
                XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class,desktop);
                xFrame = xDesktop.getCurrentFrame();
            }
            this.xFrame = xFrame;
        }
        catch (Exception e) {
            // Failed to get toolkit or frame
            xToolkit = null;
            xFrame = null;
        }
    }

	
    public void showMessage(String sTitle, String sMessage) {
        if (xToolkit==null || xFrame==null) { return; }
        try {
            WindowDescriptor descriptor = new WindowDescriptor();
            descriptor.Type = WindowClass.MODALTOP;
            descriptor.WindowServiceName = "infobox";
            descriptor.ParentIndex = -1;
            descriptor.Parent = (XWindowPeer) UnoRuntime.queryInterface(
                XWindowPeer.class,xFrame.getContainerWindow());
            descriptor.Bounds = new Rectangle(0,0,300,200);
            descriptor.WindowAttributes = WindowAttribute.BORDER |
                WindowAttribute.MOVEABLE | WindowAttribute.CLOSEABLE;
            XWindowPeer xPeer = xToolkit.createWindow(descriptor);
            if (xPeer!=null) {
                XMessageBox xMessageBox = (XMessageBox)
                    UnoRuntime.queryInterface(XMessageBox.class,xPeer);
                if (xMessageBox!=null) {
                    xMessageBox.setCaptionText(sTitle);
                    xMessageBox.setMessageText(sMessage);
                    xMessageBox.execute();
                }
            }
        }
        catch (Exception e) {
            // ignore, give up...
        }
    }
	
}
