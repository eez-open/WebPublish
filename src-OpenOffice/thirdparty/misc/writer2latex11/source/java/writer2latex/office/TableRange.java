/************************************************************************
 *
 *  TableRange.java
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
 *  Version 1.0 (2008-09-07) 
 *
 */

package writer2latex.office;

/**
 *  This class represent a table range within a table. A table range is defined
 *  as a rectangular area (such as a print range), possibly excluding filtered
 *  and hidden rows. A <code>TableView</code> can be derived from a table range,
 *  providing read access to the range.
 */
public class TableRange {
    private TableReader reader;
	
    private int nFirstRow;
    private int nLastRow;
    private int nFirstCol;
    private int nLastCol;
    private boolean bIncludeHidden;
    private boolean bIncludeFiltered;
	
    public TableRange(TableReader reader) {
        this.reader = reader;
		
        // The initial view is the complete table
        nFirstRow = 0;
        nLastRow = reader.getRowCount()-1;
        nFirstCol = 0;
        nLastCol = reader.getColCount()-1;
        bIncludeHidden = true;
        bIncludeFiltered = true;
    }
	
    public void setFirstRow(int nRow) {
        // Adjust to a valid value (in 0..nLastRow)
        if (nRow<0) { nFirstRow = 0; }
        else if (nRow>nLastRow) { nFirstRow = nLastRow; }
        else { nFirstRow = nRow; } 
    }
	
    public int getFirstRow() {
        return nFirstRow;
    }
	
    public void setLastRow(int nRow) {
        // Adjust to a valid value (in nFirstRow..(nRowCount-1))
        if (nRow<nFirstRow) { nLastRow = nFirstRow; }
        else if (nRow>=reader.getRowCount()) { nLastRow = reader.getRowCount()-1; }
        else { nLastRow = nRow; } 
    }
	
    public int getLastRow() {
        return nLastRow;
    }
	
    public void setFirstCol(int nCol) {
        // Adjust to a valid value (in 0..nLastCol)
        if (nCol<0) { nFirstCol = 0; }
        else if (nCol>nLastCol) { nFirstCol = nLastCol; }
        else { nFirstCol = nCol; }
    }
	
    public int getFirstCol() {
        return nFirstCol;
    }
	
    public void setLastCol(int nCol) {
        // Adjust to a valid value (in nFirstCol..(nColCount-1))
        if (nCol<nFirstCol) { nLastCol = nFirstCol; }
        else if (nCol>=reader.getColCount()) { nLastCol = reader.getColCount()-1; }
        else { nLastCol = nCol; } 
    }
	
    public int getLastCol() {
        return nLastCol;
    }
	
    public void setIncludeHidden(boolean b) {
        bIncludeHidden = b;
    }
	
    public boolean includeHidden() {
        return bIncludeHidden;
    }
	
    public void setIncludeFiltered(boolean b) {
        bIncludeFiltered = b;
    }
	
    public boolean includeFiltered() {
        return bIncludeFiltered;
    }
	
    public TableView createTableView() {
        return new TableView(reader, this);
    }


}