/************************************************************************
 *
 *  UnicodeRow.java
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
 *  Copyright: 2002-2007 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 0.5 (2007-07-24) 
 * 
 */

package writer2latex.latex.i18n;

// Helper class: A row of 256 unicode characters
class UnicodeRow implements Cloneable {
    UnicodeCharacter[] entries;
    UnicodeRow(){ entries=new UnicodeCharacter[256]; }
	
    protected Object clone() {
        UnicodeRow ur = new UnicodeRow();
        for (int i=0; i<256; i++) {
            if (this.entries[i]!=null) {
                ur.entries[i] = (UnicodeCharacter) this.entries[i].clone();
            }
        }
        return ur;
    }
}
