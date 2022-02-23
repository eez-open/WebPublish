/************************************************************************
 *
 *  TableConverter.java
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
import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

/** <p>This class converts OpenDocument tables to LaTeX.</p>
 *  <p>The following LaTeX packages are used; some of them are optional</p>
 *  <p>array.sty, longtable.sty, supertabular.sty, tabulary.sty, hhline.sty, 
 *  colortbl.sty.</p>
 *  <p>Options:</p>
 *  <ul>
 *    <li>use_longtable = true|false</li>
 *    <li>use_supertabular = true|false</li>
 *    <li>use_tabulary = true|false</li>
 *    <li>use_colortbl = true|false</li>
 *    <li>float_tables = true|false</li>
 *    <li>float_options = &lt;string&gt;</li>
 *    <li>table_content = accept|ignore|warning|error</li>
 *  </ul>
 *     
 */
public class TableConverter extends ConverterHelper {

    private boolean bNeedLongtable = false;
    private boolean bNeedSupertabular = false;
    private boolean bNeedTabulary = false;
    private boolean bNeedColortbl = false;
    private boolean bContainsTables = false;
	
    /** <p>Constructs a new <code>TableConverter</code>.</p>
     */
    public TableConverter(OfficeReader ofr, LaTeXConfig config,
        ConverterPalette palette) {
        super(ofr,config,palette);
    }

    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        pack.append("\\usepackage{array}").nl(); // TODO: Make this optional
        if (bNeedLongtable) { pack.append("\\usepackage{longtable}").nl(); }
        if (bNeedSupertabular) { pack.append("\\usepackage{supertabular}").nl(); }
        if (bNeedTabulary) { pack.append("\\usepackage{tabulary}").nl(); }
        pack.append("\\usepackage{hhline}").nl(); // TODO: Make this optional
        if (bNeedColortbl) { pack.append("\\usepackage{colortbl}").nl(); }

        // Set padding for table cells (1mm is default in OOo!)
        // For vertical padding we can only specify a relative size
        if (bContainsTables) {
            decl.append("\\setlength\\tabcolsep{1mm}").nl();
            decl.append("\\renewcommand\\arraystretch{1.3}").nl();
        }
    }
	
    // Export a lonely table caption
    public void handleCaption(Element node, LaTeXDocumentPortion ldp, Context oc) {
        ldp.append("\\captionof{table}");
        palette.getCaptionCv().handleCaptionBody(node,ldp,oc,true);
    }
	
    /** <p> Process a table (table:table or table:sub-table tag)</p>
     * @param node The element containing the table
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleTable(Element node, Element caption, boolean bCaptionAbove,
        LaTeXDocumentPortion ldp, Context oc) {

        // Export table, if allowed by the configuration
        switch (config.tableContent()) {
        case LaTeXConfig.ACCEPT:
            new SingleTableConverter().handleTable(node,caption,bCaptionAbove,ldp,oc);
            bContainsTables = true;
            break;
        case LaTeXConfig.IGNORE:
            // Ignore table silently
            break;
        case LaTeXConfig.WARNING:
            System.err.println("Warning: Tables are not allowed");
            break;
        case LaTeXConfig.ERROR:
            ldp.append("% Error in document: A table was ignored");
        }
    }
	
    // Inner class to convert a single table
    private class SingleTableConverter {
        private TableReader table;
        private TableFormatter formatter;
        private Element caption;
        private boolean bCaptionAbove;
        private BeforeAfter baTable;
        private BeforeAfter baTableAlign;
        
        private void handleTable(Element node, Element caption, boolean bCaptionAbove,
        LaTeXDocumentPortion ldp, Context oc) {
            // Store the caption
            this.caption = caption;
            this.bCaptionAbove = bCaptionAbove;

            // Read the table
            table = ofr.getTableReader(node);
			
            // Get formatter and update flags according to formatter
            formatter = new TableFormatter(ofr,config,palette,table,!oc.isInMulticols(),oc.isInTable());
            bContainsTables = true;
            bNeedLongtable |= formatter.isLongtable();
            bNeedSupertabular |= formatter.isSupertabular();
            bNeedTabulary |= formatter.isTabulary();
            bNeedColortbl |= formatter.isColortbl();
			
            // Update the context
            Context ic = (Context) oc.clone();
            ic.setInTable(true);
            ic.setInSimpleTable(formatter.isSimple());
            // Never allow footnotes in tables
            // (longtable.sty *does* allow footnotes in body, but not in head -
            // only consistent solution is to disallow all footnotes)
            ic.setNoFootnotes(true);

            // Get table declarations
            baTable = new BeforeAfter();
            baTableAlign = new BeforeAfter();
            formatter.applyTableStyle(baTable,baTableAlign,config.floatTables() && !ic.isInFrame() && !table.isSubTable());
			
            // Convert table
            if (formatter.isSupertabular()) {
                handleSupertabular(ldp,ic);
            }
            else if (formatter.isLongtable()) {
                handleLongtable(ldp,ic);
            }
            else if (config.floatTables() && !ic.isInFrame() && !table.isSubTable()) {
                handleTableFloat(ldp,ic);
            }
            else {
                handleTabular(ldp,ic);
            }
			
            // Insert any pending footnotes
            palette.getNoteCv().flushFootnotes(ldp,oc);
        }
		
        private void handleSupertabular(LaTeXDocumentPortion ldp, Context oc) {
            ldp.append(baTableAlign.getBefore());

            // Caption
            if (caption!=null) {
                handleCaption(bCaptionAbove ? "\\topcaption" : "\\bottomcaption",
                    ldp,oc);
            }

            // Table head
            ldp.append("\\tablehead{");
            handleHeaderRows(ldp,oc);
            ldp.append("}\n");

            // The table
            handleHyperTarget(ldp);
            ldp.append(baTable.getBefore()).nl();
            handleBodyRows(ldp,oc);
            ldp.append(baTable.getAfter()).nl();

            ldp.append(baTableAlign.getAfter());
        }
		
        private void handleLongtable(LaTeXDocumentPortion ldp, Context oc) {
            handleHyperTarget(ldp);
            ldp.append(baTable.getBefore()).nl();

            // Caption above
            if (caption!=null && bCaptionAbove) {
                handleCaption("\\caption",ldp,oc);
                ldp.append("\\\\").nl();
                handleHeaderRows(ldp,oc);
                ldp.nl().append("\\endfirsthead").nl();
            }
			
            // Table head
            if (table.getFirstBodyRow()>0) {
                handleHeaderRows(ldp,oc);
                ldp.nl().append("\\endhead").nl();
            }
			
            // Caption below
            if (caption!=null && !bCaptionAbove) {
                handleCaption("\\caption",ldp,oc);
                ldp.append("\\endlastfoot").nl();
            }
			
            // Table body
            handleBodyRows(ldp,oc);
			
            ldp.append(baTable.getAfter()).nl();
        }
		
        private void handleTableFloat(LaTeXDocumentPortion ldp, Context oc) {
            ldp.append("\\begin{table}");
            if (config.getFloatOptions().length()>0) {
                ldp.append("[").append(config.getFloatOptions()).append("]");
            }
            ldp.nl();
			
            ldp.append(baTableAlign.getBefore());
		
            // Caption above
            if (caption!=null && bCaptionAbove) {
                handleCaption("\\caption",ldp,oc);
            }

            // The table
            handleHyperTarget(ldp);
            ldp.append(baTable.getBefore()).nl();
            handleHeaderRows(ldp,oc);
            ldp.nl();
            handleBodyRows(ldp,oc);
            ldp.append(baTable.getAfter()).nl();
			
            // Caption below
            if (caption!=null && !bCaptionAbove) {
                handleCaption("\\caption",ldp,oc);
            }
			
            ldp.append(baTableAlign.getAfter());

            ldp.append("\\end{table}").nl();
        }
		
        private void handleTabular(LaTeXDocumentPortion ldp, Context oc) {
            ldp.append(baTableAlign.getBefore());

            // Caption above
            if (caption!=null && bCaptionAbove) {
                TableConverter.this.handleCaption(caption,ldp,oc);
            }

            // The table
            handleHyperTarget(ldp);
            ldp.append(baTable.getBefore()).nl();
            if (table.getFirstBodyRow()>0) {
                handleHeaderRows(ldp,oc);
                ldp.nl();
            }
            handleBodyRows(ldp,oc);
            ldp.append(baTable.getAfter()).nl();
			
            // Caption below
            if (caption!=null && !bCaptionAbove) {
                TableConverter.this.handleCaption(caption,ldp,oc);
            }

            ldp.append(baTableAlign.getAfter());
        }
		
        private void handleCaption(String sCommand, LaTeXDocumentPortion ldp, Context oc) {
            ldp.append(sCommand);
            palette.getCaptionCv().handleCaptionBody(caption,ldp,oc,false);
        }
		
        private void handleHyperTarget(LaTeXDocumentPortion ldp) {
            // We may need a hyperlink target
            if (!table.isSubTable()) {
                palette.getFieldCv().addTarget(table.getTableName(),"|table",ldp);
            }
        }
		
        private void handleHeaderRows(LaTeXDocumentPortion ldp, Context oc) {
            // Note: does *not* add newline after last row
            if (table.getFirstBodyRow()>0) {

                // Add interrow material before first row:
                String sInter = formatter.getInterrowMaterial(0);
                if (sInter.length()>0) { 
                    ldp.append(sInter).nl();
                }
				
                // Add header rows
                handleRows(0,table.getFirstBodyRow(),ldp,oc);
            }
        }
		
        private void handleBodyRows(LaTeXDocumentPortion ldp, Context oc) {
            if (table.getFirstBodyRow()==0) {
                // No head, add interrow material before first row:
                String sInter = formatter.getInterrowMaterial(0);
                if (sInter.length()>0) { 
                    ldp.append(sInter).nl();
                }
            }				

            // Add body rows
            handleRows(table.getFirstBodyRow(),table.getRowCount(),ldp,oc);
            ldp.nl();
        }
		
        private void handleRows(int nStart, int nEnd, LaTeXDocumentPortion ldp, Context oc) {
            int nColCount = table.getColCount();
            for (int nRow=nStart; nRow<nEnd; nRow++) {
                // Export columns in this row
                Context icRow = (Context) oc.clone();
                BeforeAfter baRow = new BeforeAfter();
                formatter.applyRowStyle(nRow,baRow,icRow);
                if (!baRow.isEmpty()) {
                    ldp.append(baRow.getBefore());
                    if (!formatter.isSimple()) { ldp.nl(); }
                }   
                int nCol = 0;
                while (nCol<nColCount) {
                    Element cell = (Element) table.getCell(nRow,nCol);
                    if (XMLString.TABLE_TABLE_CELL.equals(cell.getNodeName())) {
                        Context icCell = (Context) icRow.clone();
                        BeforeAfter baCell = new BeforeAfter();
                        formatter.applyCellStyle(nRow,nCol,baCell,icCell);
                        ldp.append(baCell.getBefore());
                        if (nCol==nColCount-1) { icCell.setInLastTableColumn(true); }
                        palette.getBlockCv().traverseBlockText(cell,ldp,icCell);
                        ldp.append(baCell.getAfter());
                    }
                    // Otherwise ignore; the cell is covered by a \multicolumn entry.
                    // (table:covered-table-cell)
                    int nColSpan = Misc.getPosInteger(cell.getAttribute(
                                       XMLString.TABLE_NUMBER_COLUMNS_SPANNED),1);
                    if (nCol+nColSpan<nColCount) {
                        if (formatter.isSimple()) { ldp.append(" & "); }
                        else { ldp.append(" &").nl(); }
                    }
                    nCol+=nColSpan;
                }
                ldp.append("\\\\").append(formatter.getInterrowMaterial(nRow+1));
                // Add newline, except after last row
                if (nRow<nEnd-1) { ldp.nl(); }
            }
        }
	
    }

    
}
