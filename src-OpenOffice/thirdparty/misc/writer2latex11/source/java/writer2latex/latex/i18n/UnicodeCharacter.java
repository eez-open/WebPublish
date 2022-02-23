/************************************************************************
 *
 *  UnicodeCharacter.java
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

// Helper class: A struct to hold the LaTeX representations of a unicode character
class UnicodeCharacter implements Cloneable {
    final static int NORMAL = 0;     // this is a normal character
    final static int COMBINING = 1;  // this character should be ignored
    final static int IGNORE = 2;     // this is a combining character
    final static int UNKNOWN = 3;     // this character is unknown
	
    int nType;       // The type of character
    String sMath;    // LaTeX representation in math mode 
    String sText;    // LaTeX representation in text mode
    int nFontencs;   // Valid font encoding(s) for the text mode representation
    boolean bDashes; // This character is represented by dashes (-,--,---)
	
    protected Object clone() {
        UnicodeCharacter uc = new UnicodeCharacter();
        uc.nType = this.nType;
        uc.sMath = this.sMath;
        uc.sText = this.sText;
        uc.nFontencs = this.nFontencs;
        uc.bDashes = this.bDashes;
        return uc;
    }
}

