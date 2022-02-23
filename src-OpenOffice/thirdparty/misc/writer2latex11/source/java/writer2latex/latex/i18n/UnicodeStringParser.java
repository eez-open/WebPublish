/************************************************************************
 *
 *  UnicodeStringParser.java
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

// Helper class: Parse a unicode string.
// Note: Some 8-bit fonts have additional "spacer" characters that are used
// for manual placement of accents. These are ignored between the base character
// and the combining character, thus we are parsing according to the rule
// <base char> <spacer char>* <combining char>?
class UnicodeStringParser {
    private UnicodeTable table; // the table to use
    private String s; // the string
    private int i; // the current index
    private int nEnd; // the maximal index
    private char c; // the current character
    private char cc; // the current combining character

    protected void reset(UnicodeTable table, String s, int i, int nEnd) {
        this.table=table;
        this.s=s;
        this.i=i;
        this.nEnd=nEnd;
    }

    protected boolean next() {
        if (i>=nEnd) { return false; }
        // Pick up base character
        c = s.charAt(i++);
        if (table.getCharType(c)==UnicodeCharacter.COMBINING) {
            // Lonely combining character - combine with space
            cc = c;
            c = ' ';
            return true;
        }
		
        // Skip characters that should be ignored
        while (i<s.length() && table.getCharType(s.charAt(i))==UnicodeCharacter.IGNORE) { i++; }
        // Pick up combining character, if any
        if (i<s.length() && table.getCharType(s.charAt(i))==UnicodeCharacter.COMBINING) {
            cc = s.charAt(i++);
        }
        else {
            cc = '\u0000';
        }
		
        return true;
    }
	
    protected char getChar() { return c; }

    protected boolean hasCombiningChar() { return cc!='\u0000'; }
	
    protected char getCombiningChar() { return cc; }
	
}
