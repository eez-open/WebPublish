/************************************************************************
 *
 *  BatchHandlerImpl.java
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
 *  Version 1.0 (2008-10-03) 
 *
 */
 
package writer2latex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import writer2latex.api.BatchHandler;

/** This class implements a <code>BatchHandler</code> for command line usage
 */
public class BatchHandlerImpl implements BatchHandler {
    private int nIndent = 0;
	
    private void writeMessage(String sMsg) {
        for (int i=0; i<nIndent; i++) {
            System.out.print("  ");
        }
        System.out.println(sMsg);
    }
	
    public void startConversion() {
        System.out.println("Press Enter to cancel the conversion");
    }
	
    public void endConversion() {
        // No message
    }
	
    public void startDirectory(String sName) {
        writeMessage("Converting directory "+sName);
        nIndent++;
    }
	
    public void endDirectory(String sName, boolean bSuccess) {
        nIndent--;
        if (!bSuccess) {
            writeMessage("--> Conversion of the directory "+sName+" failed!");
        }
    }
	
    public void startFile(String sName) {
        writeMessage("Converting file "+sName);
        nIndent++;
    }
	
    public void endFile(String sName, boolean bSuccess) {
        nIndent--;
        if (!bSuccess) {
            writeMessage("--> Conversion of the file "+sName+" failed!");
        }
    }
	
    public boolean cancel() {
        try {
            if (System.in.available()>0) {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                in.readLine();
                System.out.print("Do you want to cancel the conversion (y/n)? ");
                String s = in.readLine();
                if (s!= null && s.toLowerCase().startsWith("y")) { return true; }
            }
        }
        catch (IOException e) {
        }
        return false;
    }

}
