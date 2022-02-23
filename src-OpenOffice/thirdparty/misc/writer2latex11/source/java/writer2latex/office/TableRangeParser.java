/************************************************************************
 *
 *  TableRangeParser.java
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
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2009-02-16)
 *
 */

package writer2latex.office;

import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/**
 * This class parses a space separated list of table ranges. A table range is of the form
 * <sheet name>.<column><row>:<sheet name>.<column><row>
 * where the sheet name is quoted (single quotes) if it contains spaces
 * the column is one or two uppercase letters (A-Z)
 * the row is an integer.
 * The sheet name is currently ignored, and so are any syntax errors
 */
public class TableRangeParser {
    private SimpleInputBuffer inbuf;
    
    public TableRangeParser(String s) {
        inbuf = new SimpleInputBuffer(s);
        inbuf.skipSpaces();
    }
    
    public boolean hasMoreRanges() {
        return !inbuf.atEnd();
    }
    
    // returns { first col, first row, last col, last row }
    public int[] getRange() {
        if (!inbuf.atEnd()) {
            int[] nFirst = parseAddress();
            parseCharacter(':');
            int[] nLast = parseAddress();
            int[] nResult = { nFirst[0], nFirst[1], nLast[0], nLast[1] };
            return nResult;
        }
        else {
            return null;
        }
    }
    
    private void parseCharacter(char c) {
        // ignore the character if it's missing
        if (inbuf.peekChar()==c) { inbuf.getChar(); }
        inbuf.skipSpaces();
    }
    
    // returns { col, row }
    private int[] parseAddress() {
        parseSheetName();
        parseCharacter('.');
        int[] nResult = new int[2];
        nResult[0] = parseColumn();
        nResult[1] = parseRow();
        inbuf.skipSpaces();
        return nResult;
    }
    
    private void parseSheetName() {
        if (inbuf.peekChar()=='.') {
            // The sheet name may be omitted
        }
        else if (inbuf.peekChar()=='\'') {
            // The sheet name must be quoted if it contains space, dots or apostrophes
            inbuf.getChar(); // skip leading '
            while (!inbuf.atEnd() && inbuf.peekChar()!='\'') {
                inbuf.getChar();
                if (inbuf.peekChar()=='\'' && inbuf.peekFollowingChar()=='\'') {
                    // Escaped '
                    inbuf.getChar(); inbuf.getChar();
                }
            }
            inbuf.getChar(); // skip trailing '
        }
        else {
            // Unquoted sheet name, ends with dot
            while (!inbuf.atEnd() && inbuf.peekChar()!='.') {
                inbuf.getChar();
            }
        }
        inbuf.skipSpaces();
    }
    
    // Return column number, starting with 0
    private int parseColumn() {
        if (inbuf.peekChar()>='A' && inbuf.peekChar()<='Z') {
            int nFirst = inbuf.getChar()-65;
            if (inbuf.peekChar()>='A' && inbuf.peekChar()<='Z') {
                int nSecond = inbuf.getChar()-65;
                return 26*(nFirst+1)+nSecond;
            }
            else {
                return nFirst;
            }
        }
        else {
            return 0; // syntax error
        }
    }
    
    // Return row number, starting with 0
    private int parseRow() {
        return Misc.getPosInteger(inbuf.getInteger(),1)-1;
    }
	
		
}