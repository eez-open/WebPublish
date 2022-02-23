/************************************************************************
 *
 *  TableView.java
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

import org.w3c.dom.Element;

import writer2latex.util.Misc;

/**
 *  This class represents a view of a <code>TableRange</code>. A view provides
 *  read access to the range using a simple grid model.
 */
public class TableView {

    private TableReader reader;
    private TableRange range;
	
    // The size of the view (visible part of the range)
    private int nRowCount;
    private int nColCount;

    // Map view row/col index to original index
    private int[] nRowMap;
    private int[] nColMap;

    // The cells in the view 
    private CellView[][] cells;

    public TableView(TableReader reader, TableRange range) {
        this.reader = reader;
        this.range = range;
		
        // Count visible rows & cols in this range
        nRowCount = 0;
        for (int nRow=range.getFirstRow(); nRow<=range.getLastRow(); nRow++) {
            if (isVisibleRow(nRow)) { nRowCount++; }
        }
        nColCount = 0;
        for (int nCol=range.getFirstCol(); nCol<=range.getLastCol(); nCol++) {
            if (isVisibleCol(nCol)) { nColCount++; }
        }
		
        // Fill the row & col maps
        nRowMap = new int[nRowCount];
        int nRealRow = range.getFirstRow();
        for (int nRow=0; nRow<nRowCount; nRow++) {
            // Skip invisible rows
            while (!isVisibleRow(nRealRow)) { nRealRow++; }
            nRowMap[nRow] = nRealRow++;
        }
        nColMap = new int[nColCount];
        int nRealCol = range.getFirstCol();
        for (int nCol=0; nCol<nColCount; nCol++) {
            // Skip invisible cols
            while (!isVisibleCol(nRealCol)) { nRealCol++; }
            nColMap[nCol] = nRealCol++;
        }
		
        // Initialize the cell views
        cells = new CellView[nRowCount][nColCount];
        for (int nRow=0; nRow<nRowCount; nRow++) {
            for (int nCol=0; nCol<nColCount; nCol++) {
                cells[nRow][nCol] = new CellView();
            }
        }
		
        // Fill the cell views
        // (must start in the upper left corner of the original table)
        int nViewRow = 0;
        for (int nRow=0; nRow<=range.getLastRow(); nRow++) {
            if (nViewRow<nRowCount && nRowMap[nViewRow]<nRow) { nViewRow++; }
            int nViewCol = 0;
            for (int nCol=0; nCol<=range.getLastCol(); nCol++) {
                if (nViewCol<nColCount && nColMap[nViewCol]<nCol) { nViewCol++; }
                Element cell = reader.getCell(nRow,nCol);
                if (Misc.isElement(cell,XMLString.TABLE_TABLE_CELL)) {
                    int nRowSpan = Misc.getPosInteger(cell.getAttribute(XMLString.TABLE_NUMBER_ROWS_SPANNED),1);
                    int nColSpan = Misc.getPosInteger(cell.getAttribute(XMLString.TABLE_NUMBER_COLUMNS_SPANNED),1);
                    // Test if (parts of) the cell belongs the view
                    if (nViewRow<nRowCount && nRowMap[nViewRow]<nRow+nRowSpan &&
                        nViewCol<nColCount && nColMap[nViewCol]<nCol+nColSpan) {
                        cells[nViewRow][nViewCol].cell=cell;
                        cells[nViewRow][nViewCol].nOriginalRow=nRow;
                        cells[nViewRow][nViewCol].nOriginalCol=nCol;
                        // Calculate rowspan in view
                        int i=nViewRow+1;
                        while (i<nRowCount && nRowMap[i]<nRow+nRowSpan) { i++; }
                        cells[nViewRow][nViewCol].nRowSpan = i-nViewRow;
                        // Calculate colspan in view
                        int j=nViewCol+1;
                        while (j<nColCount && nColMap[j]<nCol+nColSpan) { j++; }
                        cells[nViewRow][nViewCol].nColSpan = j-nViewCol;
                    }
                }
                if (Misc.isElement(cell,XMLString.TABLE_COVERED_TABLE_CELL)) {
                    // Don't overwrite, the position may be occupied with a relocated cell
                    if (cells[nViewRow][nViewCol].cell==null) {
                        cells[nViewRow][nViewCol].cell=cell;
                        cells[nViewRow][nViewCol].nOriginalRow=nRow;
                        cells[nViewRow][nViewCol].nOriginalCol=nCol;
                    }
                 }
            }
        }
    }
	
    public String getRelTableWidth() { return reader.getRelTableWidth(); }
	
    public int getRowCount() { return nRowCount; }

    public int getColCount() { return nColCount; }

    public String getColumnWidth(int nCol) {
        return 0<=nCol && nCol<=nColCount ? reader.getColumnWidth(nColMap[nCol]) : null;
    }
	
    // TODO: Recalculate - the sum should be 100% even in a view!!
    public String getRelColumnWidth(int nCol) {
        return 0<=nCol && nCol<=nColCount ? reader.getRelColumnWidth(nColMap[nCol]) : null;
    }
	
    public TableLine getRow(int nRow) {
        return 0<=nRow && nRow<nRowCount ? reader.getRow(nRowMap[nRow]) : null;
    }

    public TableLine getCol(int nCol) {
        return 0<=nCol && nCol<nColCount ? reader.getCol(nColMap[nCol]) : null;
    }
	
    public Element getCell(int nRow, int nCol) {
        return 0<=nRow && nRow<nRowCount && 0<=nCol && nCol<nColCount ?
            cells[nRow][nCol].cell : null;  
    }
	
    public int getRowSpan(int nRow, int nCol) {
        return 0<=nRow && nRow<nRowCount && 0<=nCol && nCol<nColCount ?
            cells[nRow][nCol].nRowSpan : 1;  
    }
	
    public int getColSpan(int nRow, int nCol) {
        return 0<=nRow && nRow<nRowCount && 0<=nCol && nCol<nColCount ?
            cells[nRow][nCol].nColSpan : 1;  
    }
	
    public String getCellStyleName(int nRow, int nCol) {
        return 0<=nRow && nRow<nRowCount && 0<=nCol && nCol<nColCount ?
            reader.getCellStyleName(cells[nRow][nCol].nOriginalRow, cells[nRow][nCol].nOriginalCol) : null;  
    }
	
    // TODO: Not correct, see TableReader
    public String getCellWidth(int nRow, int nCol) {
        return 0<=nRow && nRow<nRowCount && 0<=nCol && nCol<nColCount ?
            reader.getCellWidth(cells[nRow][nCol].nOriginalRow, cells[nRow][nCol].nOriginalCol) : null;  
    }
	
    // Helper method: Is this row visible in this view?
    private boolean isVisibleRow(int nRow) {
        return nRow>=range.getFirstRow() && nRow<=range.getLastRow() &&
               (range.includeHidden() || !reader.getRow(nRow).isCollapse()) &&
               (range.includeFiltered() || !reader.getRow(nRow).isFilter());
    }
	
    // Helper method: Is this column visible in this view?
    private boolean isVisibleCol(int nCol) {
        return nCol>=range.getFirstCol() && nCol<=range.getLastCol() &&
               (range.includeHidden() || !reader.getCol(nCol).isCollapse()) &&
               (range.includeFiltered() || !reader.getCol(nCol).isFilter());
    }
	
	
	
}