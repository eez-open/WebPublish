/************************************************************************
 *
 *  TableFormatter.java
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
 *  Version 1.0 (2009-05-22)
 *  
 */
 
package writer2latex.latex;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

/**
 *  <p>This class converts OOo table styles to LaTeX.</p>
 *  <p> In OOo the table style is distributed on table, column and cell styles.
 *  <p> In LaTeX we have to rearrange this information slightly, so this class
 *  takes care of that.</p>
 */
public class TableFormatter extends ConverterHelper {

    //private boolean bApplyCellFormat;
    private TableReader table;
    private char[][] cAlign;
    private char[] cGlobalAlign;
    private boolean[][] bHBorder;
    private boolean[][] bVBorder;
    private boolean[] bGlobalVBorder;
    private String[] sRowColor;
    private String[][] sCellColor;
    private String[] sColumnWidth; 
    private boolean bIsLongtable;
    private boolean bIsSupertabular;
    private boolean bIsTabulary;
    private boolean bIsColortbl;
    private boolean bIsSimple;
	
    /** <p>Constructor: Create from a TableReader.</p>
     */
    public TableFormatter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette,
        TableReader table, boolean bAllowPageBreak, boolean bIsInTable) {
        super(ofr,config,palette);
        this.table = table;
        //bApplyCellFormat = config.formatting()>=LaTeXConfig.CONVERT_MOST;
        int nRowCount = table.getRowCount();
        int nColCount = table.getColCount();
        int nSimpleTableLimit = config.getSimpleTableLimit();
		
        // Step 1: Collect alignment and identify simple tables
        bIsSimple = true;
        cAlign = new char[nRowCount][nColCount];
        cGlobalAlign = new char[nColCount];
        // Keep track of characters to be counted
        int[] nPendingChars = new int[nRowCount];
        int[] nPendingColSpan = new int[nRowCount];
        int nTableWidth = 0;
        
        for (int nCol=0; nCol<nColCount; nCol++) {
        	// Collect chars to be counted in this column
        	for (int nRow=0; nRow<nRowCount; nRow++) {
        		Element cell = table.getCell(nRow, nCol);
        		if (Misc.isElement(cell, XMLString.TABLE_TABLE_CELL)) {
        			// Now we're here: Collect alignment
        			if (OfficeReader.isSingleParagraph(cell)) {
                        Node par = Misc.getChildByTagName(cell,XMLString.TEXT_P);
                        StyleWithProperties style = ofr.getParStyle(Misc.getAttribute(par,XMLString.TEXT_STYLE_NAME));
                        cAlign[nRow][nCol] = 'l';
                        if (style!=null) {
                            String sAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
                            if ("center".equals(sAlign)) { cAlign[nRow][nCol] = 'c'; } 
                            else if ("end".equals(sAlign)) { cAlign[nRow][nCol] = 'r'; } 
                        }
        				
        			}
        			else {
        				// Found cell with more than one paragraph
        				bIsSimple = false;
        			}
        			// Collect characters (the cell contains this many characters that should be distributed over that many columns)
        			nPendingChars[nRow] = OfficeReader.getCharacterCount(cell);
        			nPendingColSpan[nRow] = Misc.getPosInteger(cell.getAttribute(XMLString.TABLE_NUMBER_COLUMNS_SPANNED), 1);
        		}
        	}
        	// Determine the number of characters to count *now* (because they cannot be postponed to next column)
        	int nColChars = 0;
        	for (int nRow=0; nRow<nRowCount; nRow++) {
        		if (nPendingColSpan[nRow]==1) {
        			nColChars = Math.max(nColChars, nPendingChars[nRow]);
        		}
        	}
        	// Reduce pending chars and increase table width
        	nTableWidth += nColChars;
        	for (int nRow=0; nRow<nRowCount; nRow++) {
        		if (nPendingColSpan[nRow]>=1) {
        			nPendingChars[nRow] = Math.max(0, nPendingChars[nRow]-nColChars);
        			nPendingColSpan[nRow]--;
        		}
        	}
        }
 		if (nTableWidth>nSimpleTableLimit) bIsSimple = false;
        
        // Step 2: Create global alignment
        for (int nCol=0; nCol<nColCount; nCol++) {
            int nCenter = 0;
            int nRight = 0;
            for (int nRow=0; nRow<nRowCount; nRow++) {
                if (cAlign[nRow][nCol]=='c') { nCenter++; }
                else if (cAlign[nRow][nCol]=='r') { nRight++; }
            }
            cGlobalAlign[nCol] = 'l';
            int nLeft = nColCount-nCenter-nRight;
            if (nCenter>nLeft) {
                if (nRight>nLeft) { cGlobalAlign[nCol] = 'r'; }
                else { cGlobalAlign[nCol] = 'c'; }
            }
            else if (nRight>nLeft) {
                cGlobalAlign[nCol] = 'r';
            }
        }
		
        // Step 3: Initialize borders:
        bHBorder = new boolean[nRowCount+1][nColCount];
        for (int nRow=0; nRow<=nRowCount; nRow++) {
            for (int nCol=0; nCol<nColCount; nCol++) {
                bHBorder[nRow][nCol] = false;
            }
        }
        bVBorder = new boolean[nRowCount][nColCount+1];
        for (int nRow=0; nRow<nRowCount; nRow++) {
            for (int nCol=0; nCol<=nColCount; nCol++) {
                bVBorder[nRow][nCol] = false;
            }
        }

        // Step 4: Collect borders from cell styles:
        for (int nRow=0; nRow<nRowCount; nRow++) {
            int nCol = 0;
            while (nCol<nColCount) {
                Node cell = table.getCell(nRow,nCol);
                String sStyleName = Misc.getAttribute(cell,XMLString.TABLE_STYLE_NAME);
                StyleWithProperties style = ofr.getCellStyle(sStyleName);
                int nColSpan = Misc.getPosInteger(Misc.getAttribute(cell,
                                   XMLString.TABLE_NUMBER_COLUMNS_SPANNED),1);
                boolean bLeft = false;
                boolean bRight = false;
                boolean bTop = false;
                boolean bBottom = false;
                if (style!=null) {
                    String sBorder = style.getProperty(XMLString.FO_BORDER);
                    if (sBorder!=null && !"none".equals(sBorder)) {
                        bLeft = true; bRight = true; bTop = true; bBottom = true;
                    }
                    sBorder = style.getProperty(XMLString.FO_BORDER_LEFT);
                    if (sBorder!=null && !"none".equals(sBorder)) {
                        bLeft = true;
                    }
                    sBorder = style.getProperty(XMLString.FO_BORDER_RIGHT);
                    if (sBorder!=null && !"none".equals(sBorder)) {
                        bRight = true;
                    }
                    sBorder = style.getProperty(XMLString.FO_BORDER_TOP);
                    if (sBorder!=null && !"none".equals(sBorder)) {
                        bTop = true;
                    }
                    sBorder = style.getProperty(XMLString.FO_BORDER_BOTTOM);
                    if (sBorder!=null && !"none".equals(sBorder)) {
                        bBottom = true;
                    }
                }
                bVBorder[nRow][nCol] |= bLeft;
                bVBorder[nRow][nCol+nColSpan] |= bRight;
                do {
                    bHBorder[nRow][nCol] |= bTop;
                    bHBorder[nRow+1][nCol] |= bBottom;
                    nCol++;
                } while (--nColSpan>0);
            }
        }
		
        // Step 5: Create global vertical borders based on simple majority
        // (in order to minimize the number of \multicolum{1} entries)
        bGlobalVBorder = new boolean[nColCount+1];
        for (int nCol=0; nCol<=nColCount; nCol++) {
            int nBalance = 0;
            for (int nRow=0; nRow<nRowCount; nRow++) {
                nBalance += bVBorder[nRow][nCol] ? 1 : -1;
            }
            bGlobalVBorder[nCol] = nBalance>0;
        }
		
        // Step 6: Get background colors
        sRowColor = new String[nRowCount];
        sCellColor = new String[nRowCount][nColCount];
        if (config.useColortbl()) {
            // Table background
            String sTableColor = null;
            StyleWithProperties tableStyle = ofr.getTableStyle(table.getTableStyleName());
            if (tableStyle!=null) {
                sTableColor = tableStyle.getProperty(XMLString.FO_BACKGROUND_COLOR);
            }
			
            // Row background
            for (int nRow=0; nRow<nRowCount; nRow++) {
                StyleWithProperties rowStyle = ofr.getRowStyle(table.getRow(nRow).getStyleName());
                if (rowStyle!=null) {
                    sRowColor[nRow] = rowStyle.getProperty(XMLString.FO_BACKGROUND_COLOR);
                }
                if (sRowColor[nRow]==null) {
                    sRowColor[nRow] = sTableColor;
                }
                if (sRowColor[nRow]!=null) {
                    bIsColortbl = true;
                }
            }

            // Cell background
            for (int nRow=0; nRow<nRowCount; nRow++) {
                for (int nCol=0; nCol<nColCount; nCol++) {
	                StyleWithProperties cellStyle = ofr.getCellStyle(Misc.getAttribute(table.getCell(nRow,nCol),XMLString.TABLE_STYLE_NAME));
                    if (cellStyle!=null) {
                        sCellColor[nRow][nCol] = cellStyle.getProperty(XMLString.FO_BACKGROUND_COLOR);
                        if (sCellColor[nRow][nCol]!=null) {
                            bIsColortbl = true;
                            if (sCellColor[nRow][nCol].equals(sRowColor[nRow])) {
                                // Avoid redundant cell background
                                sCellColor[nRow][nCol] = null;
                            }
                        }
                    }
                }
            }
			
        }
		
        // Step 7: Read column style information
        sColumnWidth = new String[nColCount];
        for (int nCol=0; nCol<nColCount; nCol++) {
            StyleWithProperties colStyle
                = ofr.getColumnStyle(table.getCol(nCol).getStyleName());
            if (colStyle!=null) {
                sColumnWidth[nCol]
                    = colStyle.getProperty(XMLString.STYLE_COLUMN_WIDTH);
            }
            if (sColumnWidth[nCol]==null) { // Emergency! should never happen!
                sColumnWidth[nCol]="2cm";
            }
        }
		
        // Step 8: Identify longtable, supertabular or tabulary
        bIsLongtable = false; bIsSupertabular = false; bIsTabulary = false;
        if (!table.isSubTable() && !bIsInTable) {
            String sStyleName = table.getTableStyleName();
            StyleWithProperties style = ofr.getTableStyle(sStyleName);
            boolean bMayBreak = style==null ||
                !"false".equals(style.getProperty(XMLString.STYLE_MAY_BREAK_BETWEEN_ROWS));
            if (config.useLongtable() && bMayBreak && bAllowPageBreak) {
                bIsLongtable = true;
            }
            else if (config.useSupertabular() && bMayBreak && bAllowPageBreak) {
                bIsSupertabular = true;
            }
            else if (!bIsSimple && config.useTabulary()) {
                bIsTabulary = true;
            }
        }
		
    }
	
    /** is this a longtable? */
    public boolean isLongtable() { return bIsLongtable; }
    
    /** is this a supertabular? */
    public boolean isSupertabular() { return bIsSupertabular; }

    /** is this a tabulary? */
    public boolean isTabulary() { return bIsTabulary; }
	
    /** is this a colortbl? */
    public boolean isColortbl() { return bIsColortbl; }
	
    /** is this a simple table (lcr columns rather than p{})? */
    public boolean isSimple() { return bIsSimple; }

    /**
     * <p>Create table environment based on table style.</p>
     * <p>Returns eg. "\begin{longtable}{m{2cm}|m{4cm}}", "\end{longtable}".</p>
     * @param ba the <code>BeforeAfter</code> to contain the table code
     * @param baAlign the <code>BeforeAfter</code> to contain the alignment code, if it's separate
     * @param bInFloat true if the table should be floating
     */
    public void applyTableStyle(BeforeAfter ba, BeforeAfter baAlign, boolean bInFloat) {
        // Read formatting info from table style
        // Only supported properties are alignment and may-break-between-rows.
        String sStyleName = table.getTableStyleName();
        StyleWithProperties style = ofr.getTableStyle(sStyleName);
        char cAlign = 'c';
        if (style!=null && !table.isSubTable()) {
            String s = style.getProperty(XMLString.TABLE_ALIGN);
            if ("left".equals(s)) { cAlign='l'; }
            else if ("right".equals(s)) { cAlign='r'; }
        }
        String sAlign="center";
        switch (cAlign) {
            case 'c': sAlign="center"; break;
            case 'r': sAlign="flushright"; break;
            case 'l': sAlign="flushleft";
        }
		
        // Create table alignment (for supertabular, tabular and tabulary)
        if (!bIsLongtable && !table.isSubTable()) {
        	if (bInFloat & !bIsSupertabular) {
        		// Inside a float we don't want the extra glue added by the flushleft/center/flushright environment
        		switch (cAlign) {
        		case 'c' : baAlign.add("\\centering\n", ""); break;
        		case 'r' : baAlign.add("\\raggedleft\n", ""); break;
        		case 'l' : baAlign.add("\\raggedright\n", "");
        		}
        	}
        	else {
        		// But outside floats we do want it
                baAlign.add("\\begin{"+sAlign+"}\n","\\end{"+sAlign+"}\n");        		
        	}
        }
		
        // Create table declaration
        if (bIsLongtable) {
            ba.add("\\begin{longtable}["+cAlign+"]", "\\end{longtable}");
        }
        else if (bIsSupertabular) {
            ba.add("\\begin{supertabular}","\\end{supertabular}");
        }
        else if (bIsTabulary) {
            ba.add("\\begin{tabulary}{"+table.getTableWidth()+"}","\\end{tabulary}");
        }
        else if (!table.isSubTable()) {
            ba.add("\\begin{tabular}","\\end{tabular}");
        }
        else { // subtables should occupy the entire width, including padding!
            ba.add("\\hspace*{-\\tabcolsep}\\begin{tabular}",
                   "\\end{tabular}\\hspace*{-\\tabcolsep}");
        }

        // columns
        ba.add("{","");
        if (bGlobalVBorder[0]) { ba.add("|",""); }
        int nColCount = table.getColCount();
        for (int nCol=0; nCol<nColCount; nCol++){
            if (bIsSimple) {
                ba.add(Character.toString(cGlobalAlign[nCol]),"");
            }
            else if (!bIsTabulary) {
                // note: The column width in OOo includes padding, which we subtract
                ba.add("m{"+Misc.add(sColumnWidth[nCol],"-0.2cm")+"}","");
            }
            else {
                ba.add("J","");
            }
            if (bGlobalVBorder[nCol+1]) { ba.add("|",""); }
        }
        ba.add("}","");
    }
	
    /** <p>Create interrow material</p> */
    public String getInterrowMaterial(int nRow) {
        int nColCount = table.getColCount();
        int nCount = 0;
        for (int nCol=0; nCol<nColCount; nCol++) {
            if (bHBorder[nRow][nCol]) { nCount++; }
        }
        if (nCount==0) { // no borders at this row
            return ""; 
        }
        else if (nCount==nColCount) { // complete set of borders
            return "\\hline";
        }
        else { // individual borders for each column
            StringBuffer buf = new StringBuffer();
            buf.append("\\hhline{");
            for (int nCol=0; nCol<nColCount; nCol++) {
                if (bHBorder[nRow][nCol]) { buf.append("-"); }
                else { buf.append("~"); }
            }
            buf.append("}");
/* TODO: hhline.sty should be optional, and i not used, do as before:
            boolean bInCline = false;
            for (int nCol=0; nCol<nColCount; nCol++) {
                if (bInCline && !bHBorder[nRow][nCol]) { // close \cline
                    buf.append(nCol).append("}");
                    bInCline = false;
                }
                else if (!bInCline && bHBorder[nRow][nCol]) { // open \cline
                    buf.append("\\cline{").append(nCol+1).append("-");
                    bInCline = true;
                }
            }
            if (bInCline) { buf.append(nColCount).append("}"); }
*/
            return buf.toString();
        }
    }
	
    /** <p>Get material to put before a table row (background color)
     */
    public void applyRowStyle(int nRow, BeforeAfter ba, Context context) {
        palette.getColorCv().applyBgColor("\\rowcolor",sRowColor[nRow],ba,context);
    }

    /** Get material to put before and after a table cell.
     *  In case of columnspan or different borders this will contain a \multicolumn command.
     */
    public void applyCellStyle(int nRow, int nCol, BeforeAfter ba, Context context) {
        Node cell = table.getCell(nRow,nCol);
        int nColSpan = Misc.getPosInteger(Misc.getAttribute(cell,
                           XMLString.TABLE_NUMBER_COLUMNS_SPANNED),1);
        // Construct column declaration as needed
        boolean bNeedLeft = (nCol==0) && (bVBorder[nRow][0]!=bGlobalVBorder[0]);
        boolean bNeedRight = bVBorder[nRow][nCol+1]!=bGlobalVBorder[nCol+1];
        boolean bNeedAlign = bIsSimple && cGlobalAlign[nCol]!=cAlign[nRow][nCol];
        // calculate column width
        String sTotalColumnWidth = sColumnWidth[nCol];
        for (int i=nCol+1; i<nCol+nColSpan; i++) {
             sTotalColumnWidth = Misc.add(sTotalColumnWidth,sColumnWidth[i]);
        }
        sTotalColumnWidth = Misc.add(sTotalColumnWidth,"-0.2cm");

        if (bNeedAlign || bNeedLeft || bNeedRight || nColSpan>1) {
            ba.add("\\multicolumn{"+nColSpan+"}{","");
            if (nCol==0 && bVBorder[nRow][0]) { ba.add("|",""); }
            if (bIsSimple) {
                ba.add(Character.toString(cAlign[nRow][nCol]),"");
            }
            else {
                ba.add("m{"+sTotalColumnWidth+"}","");
            }
            if (bVBorder[nRow][nCol+nColSpan]) { ba.add("|",""); }
            ba.add("}{","}");
        }
		
        palette.getColorCv().applyBgColor("\\cellcolor",sCellColor[nRow][nCol],ba,context);

    }
}