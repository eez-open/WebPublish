/************************************************************************
 *
 *  ListCounter.java
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
 *  Copyright: 2002-2005 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2007-10-17)
 *
 */

package writer2latex.office;

import writer2latex.util.*;

/**
 * <p>This class produces labels for OOo lists/outlines (for xhtml
 * and text, which cannot produce them on their own).</p> 
 *
 */
public class ListCounter {
    private int nCounter[] = new int[11];
    private String sNumFormat[] = new String[11];
    private ListStyle style;
    private int nLevel=1; // current level
	
    public ListCounter() {
        // Create a dummy counter
        this.style = null;
        for (int i=1; i<=10; i++) {
            sNumFormat[i] = null;
        }
    }
	
    public ListCounter(ListStyle style) {
        this();
        if (style!=null) {
            this.style = style;
            for (int i=1; i<=10; i++) {
                sNumFormat[i] = style.getLevelProperty(i,XMLString.STYLE_NUM_FORMAT);
            }
        }
    }
	
    public ListCounter step(int nLevel) {
        // Make sure no higher levels are zero
        // This means that unlike eg. LaTeX, step(1).step(3) does not create
        // the value 1.0.1 but rather 1.1.1
        for (int i=1; i<nLevel; i++) {
            if (nCounter[i]==0) { nCounter[i]=1; }
        }
        // Then step this level
        nCounter[nLevel]++;
        // Finally clear lower levels
        if (nLevel<10) { restart(nLevel+1); }
        this.nLevel = nLevel;
        return this;
    }
	
    public ListCounter restart(int nLevel) {
        restart(nLevel,0);
        return this;
    }
	
    public ListCounter restart(int nLevel, int nValue) {
        nCounter[nLevel] = nValue;
        for (int i=nLevel+1; i<=10; i++) {
            nCounter[i] = 0;
        }
        return this;
    }
	
    public int getValue(int nLevel) {
        return nCounter[nLevel];
    }
	
    public int[] getValues() {
        int[] nCounterSnapshot = new int[11];
        System.arraycopy(nCounter,0,nCounterSnapshot,0,11);
        return nCounterSnapshot;
    }
	
    public String getLabel() {
        if (sNumFormat[nLevel]==null) return "";
        int nLevels = Misc.getPosInteger(style.getLevelProperty(nLevel,
                              XMLString.TEXT_DISPLAY_LEVELS),1);
        String sPrefix = style.getLevelProperty(nLevel,XMLString.STYLE_NUM_PREFIX);
        String sSuffix = style.getLevelProperty(nLevel,XMLString.STYLE_NUM_SUFFIX);
        String sLabel=""; 
        if (sPrefix!=null) { sLabel+=sPrefix; }
        for (int j=nLevel-nLevels+1; j<nLevel; j++) {
            sLabel+=formatNumber(nCounter[j],sNumFormat[j],true)+".";
        }
        // TODO: Lettersync
        sLabel+=formatNumber(nCounter[nLevel],sNumFormat[nLevel],true);
        if (sSuffix!=null) { sLabel+=sSuffix; }
        return sLabel;
    }
	
    // Utility method to generate number
    private String formatNumber(int number,String sStyle,boolean bLetterSync) {
        if ("a".equals(sStyle)) { return Misc.int2alph(number,bLetterSync); }
        else if ("A".equals(sStyle)) { return Misc.int2Alph(number,bLetterSync); }
        else if ("i".equals(sStyle)) { return Misc.int2roman(number); }
        else if ("I".equals(sStyle)) { return Misc.int2Roman(number); }
        else if ("1".equals(sStyle)) { return Misc.int2arabic(number); }
        else return "";
    }


}
