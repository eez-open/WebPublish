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
 *  Copyright: 2002-2008 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2008-12-15)
 *
 */

package writer2latex.xhtml;

import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;
import writer2latex.office.XMLString;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.OfficeReader;
//import writer2latex.office.TableLine;
import writer2latex.office.TableRange;
import writer2latex.office.TableReader;
import writer2latex.office.TableView;

public class TableConverter extends ConverterHelper {

    // The collection of all table names
    // TODO: Navigation should be handled here rather than in Converter.java
    protected Vector sheetNames = new Vector();
	
    public TableConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
    }
	
    /** Converts an office node as a complete table (spreadsheet) document
     *
     *  @param onode the Office node containing the content to convert
     */
    public void convertTableContent(Element onode) {
        Element hnode = null;
        if (!onode.hasChildNodes()) { return; }
        if (!config.xhtmlCalcSplit()) { hnode = nextOutFile(); }
        NodeList nList = onode.getChildNodes();
        int nLen = nList.getLength();
        for (int i=0; i<nLen; i++) {
            Node child = nList.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sNodeName = child.getNodeName();
                if (sNodeName.equals(XMLString.TABLE_TABLE)) {
                    StyleWithProperties style = ofr.getTableStyle(
                        Misc.getAttribute(child,XMLString.TABLE_STYLE_NAME));
                    if ((config.xhtmlDisplayHiddenSheets() || style==null
                            || !"false".equals(style.getProperty(XMLString.TABLE_DISPLAY)))
                            && (!config.applyPrintRanges() || ofr.getTableReader((Element)child).getPrintRangeCount()>0)) {
                        if (config.xhtmlCalcSplit()) { hnode = nextOutFile(); }
                        // Collect name
                        String sName = Misc.getAttribute(child,XMLString.TABLE_NAME);
                        sheetNames.add(sName);

                        // Add sheet name as heading, if required
                        if (config.xhtmlUseSheetNamesAsHeadings()) {
                            Element heading = converter.createElement("h2");
                            hnode.appendChild(heading);
                            heading.setAttribute("id","tableheading"+(sheetNames.size()-1));
                            heading.appendChild(converter.createTextNode(sName));
                        }
    
                        // Handle the table
                        handleTable(child,hnode);
    
                        // Add frames belonging to this table
                        Element div = converter.createElement("div");
                        Element shapes = Misc.getChildByTagName(child,XMLString.TABLE_SHAPES);
                        if (shapes!=null) {
                            Node shape = shapes.getFirstChild();
                            while (shape!=null) {
                                if (OfficeReader.isDrawElement(shape)) {
                                    // Actually only the first parameter is used
                                    getDrawCv().handleDrawElement((Element)shape,div,null,DrawConverter.CENTERED);
                                }
                                shape = shape.getNextSibling();
                            }
                        }
                        getDrawCv().flushFrames(div);
                        if (div.hasChildNodes()) { hnode.appendChild(div); }
                    }
                }
            }
        }
    	if (converter.getOutFileIndex()<0) {
    		// No files, add an empty one (This may happen if apply_print_ranges=true
    		// and the document does not contain any print ranges)
    		nextOutFile();
    	}
    }
	
    private Element nextOutFile() {
        Element hnode = converter.nextOutFile();
        // Add title, if required by config
        if (config.xhtmlUseTitleAsHeading()) {
            String sTitle = converter.getMetaData().getTitle();
            if (sTitle!=null) {
                Element title = converter.createElement("h1");
                hnode.appendChild(title);
                title.appendChild(converter.createTextNode(sTitle));
            }
        }
        return hnode;
    }
	
    /** Process a table:table tag 
     * 
     *  @param onode the Office node containing the table element 
     *  @param hnode the XHTML node to which the table should be attached
     */
    public void handleTable(Node onode, Node hnode) {
        TableReader tblr = ofr.getTableReader((Element)onode);
        if (config.applyPrintRanges()) {
            if (tblr.getPrintRangeCount()>0) {
                Element div = converter.createElement("div");
                if (!tblr.isSubTable()) {
                    converter.addTarget(div,tblr.getTableName()+"|table");
                }
                hnode.appendChild(div);
                int nCount = tblr.getPrintRangeCount();
                for (int nRange=0; nRange<nCount; nRange++) {
                    Element table = createTable(tblr);
                    div.appendChild(table);
                    TableRange range = tblr.getPrintRange(nRange);
                    range.setIncludeHidden(config.displayHiddenRowsCols());
                    range.setIncludeFiltered(config.displayFilteredRowsCols());
                    traverseTable(range.createTableView(),table);
                }
            }
        }
        else {
            // Create table
            Element table = createTable(tblr);
            if (!tblr.isSubTable()) {
                converter.addTarget(table,tblr.getTableName()+"|table");
            }
            hnode.appendChild(table);

            // Create view (full table)
            TableRange range = new TableRange(tblr);
            if (ofr.isSpreadsheet()) {
                // skip trailing empty rows and columns
                range.setLastRow(tblr.getMaxRowCount()-1);
                range.setLastCol(tblr.getMaxColCount()-1);	
            }
            range.setIncludeHidden(config.displayHiddenRowsCols());
            range.setIncludeFiltered(config.displayFilteredRowsCols());
            traverseTable(range.createTableView(),table);
        }
    }
    
    private Element createTable(TableReader tblr) {
        Element table = converter.createElement("table");
        // Apply table style
        // IE needs the cellspacing attribute, as it doesn't understand the css border-spacing attribute
        table.setAttribute("cellspacing","0");
        applyTableStyle(tblr.getTableStyleName(), table, tblr.isSubTable());
        return table;
    }
		
    private void traverseTable(TableView view, Element hnode) {
        int nRowCount = view.getRowCount();
        int nColCount = view.getColCount();
        // Check to see, if the first row contains any colspan
        boolean bFirstRowColSpan = false;
        for (int nCol=0; nCol<nColCount; nCol++) {
            Node cell = view.getCell(0,nCol);
            if (cell!=null && XMLString.TABLE_TABLE_CELL.equals(cell.getNodeName())) {
                String sColSpan = Misc.getAttribute(cell,XMLString.TABLE_NUMBER_COLUMNS_SPANNED);
                if (Misc.getPosInteger(sColSpan,1)>1) {
                    bFirstRowColSpan = true;
                }
            }
        }

        // Create columns; only for tables with relative width
        // Otherwise we set the cell width. Reason: IE and Mozilla does not
        // interpret column width the same way. IE excludes padding and border,
        // Mozilla (like OOo) includes them.
        // If the first row contains colspan we have to add <col> anyway
        if (!config.xhtmlIgnoreTableDimensions()) {
            if (view.getRelTableWidth()!=null) {
                for (int nCol=0; nCol<nColCount; nCol++) {
                    Element col = converter.createElement("col");
                    hnode.appendChild(col);
                    col.setAttribute("style","width:"+view.getRelColumnWidth(nCol));
                }
            }
            else if (bFirstRowColSpan) {
                for (int nCol=0; nCol<nColCount; nCol++) {
                    Element col = converter.createElement("col");
                    hnode.appendChild(col);
                    col.setAttribute("style","width:"+getTableSc().colScale(view.getColumnWidth(nCol)));
                }
            }
        }

        // Indentify head
        int nBodyStart = 0;
        while (nBodyStart<nRowCount && view.getRow(nBodyStart).isHeader()) {
            nBodyStart++;
        }
        if (nBodyStart==0 || nBodyStart==nRowCount) {
            // all body or all head
            traverseRows(view,0,nRowCount,hnode);
        }
        else {
            // Create thead
            Element thead = converter.createElement("thead");
            hnode.appendChild(thead);
            traverseRows(view,0,nBodyStart,thead);
            // Create tbody
            Element tbody = converter.createElement("tbody");
            hnode.appendChild(tbody);
            traverseRows(view,nBodyStart,nRowCount,tbody);
        }
       
    }
    
    private void traverseRows(TableView view, int nFirstRow, int nLastRow, Element hnode) {
        for (int nRow=nFirstRow; nRow<nLastRow; nRow++) {
            // Create row and apply row style
            Element tr = converter.createElement("tr");
            hnode.appendChild(tr);
            applyRowStyle(view.getRow(nRow).getStyleName(),tr);

            for (int nCol=0; nCol<view.getColCount(); nCol++) {
                Node cell = view.getCell(nRow,nCol);
                if (cell!=null && XMLString.TABLE_TABLE_CELL.equals(cell.getNodeName())) {
                    // Create cell
                    Element td = converter.createElement("td");
                    tr.appendChild(td);
                    int nRowSpan = view.getRowSpan(nRow,nCol);
                    if (nRowSpan>1) {
                        td.setAttribute("rowspan",Integer.toString(nRowSpan));
                    }
                    int nColSpan = view.getColSpan(nRow,nCol);
                    if (nColSpan>1) {
                        td.setAttribute("colspan",Integer.toString(nColSpan));
                    }
    
                    // Handle content
                    if (!isEmptyCell(cell)) {
                       getTextCv().traverseBlockText(cell,td);
                    }
                    else {
                        // Hack to display empty cells even in msie...
                        Element par = converter.createElement("p");
                        td.appendChild(par);
                        par.setAttribute("style","margin:0;font-size:1px");
                        par.appendChild(converter.createTextNode("\u00A0"));
                    }

                    // Is this a subtable?
                    Node subTable = Misc.getChildByTagName(cell,XMLString.TABLE_SUB_TABLE);
                    String sTotalWidth=null;
                    if (nColSpan==1) {
                        sTotalWidth = view.getCellWidth(nRow,nCol);
                    }
                    String sValueType = ofr.isOpenDocument() ?
                        Misc.getAttribute(cell,XMLString.OFFICE_VALUE_TYPE) :
                        Misc.getAttribute(cell,XMLString.TABLE_VALUE_TYPE);
                    applyCellStyle(view.getCellStyleName(nRow,nCol), sTotalWidth, sValueType, td, subTable!=null);
                }
                else if (XMLString.TABLE_COVERED_TABLE_CELL.equals(cell.getNodeName())) {
                    // covered table cells are not part of xhtml table model
                }
            }
        }
    }
	
    private boolean isEmptyCell(Node cell) {
        if (!cell.hasChildNodes()) {
            return true;
        }
        else if (OfficeReader.isSingleParagraph(cell)) {
            Element par = Misc.getChildByTagName(cell,XMLString.TEXT_P);
            return par==null || !par.hasChildNodes();
        }
        return false;
    }
	
    private void applyTableStyle(String sStyleName, Element table, boolean bIsSubTable) {
        StyleInfo info = new StyleInfo();
        getTableSc().applyStyle(sStyleName,info);

        if (!config.xhtmlIgnoreTableDimensions()) {
            StyleWithProperties style = ofr.getTableStyle(sStyleName);
            if (style!=null) {
                // Set table width
                String sWidth = style.getProperty(XMLString.STYLE_REL_WIDTH);
                if (sWidth!=null) {
                    info.props.addValue("width",sWidth);
                }
                else {
                    sWidth = style.getProperty(XMLString.STYLE_WIDTH);
                    if (sWidth!=null) {
                        info.props.addValue("width",getTableSc().colScale(sWidth));
                    }
                }
            }
        }

        // Writer uses a separating border model, Calc a collapsing:
        // props.addValue("border-collapse", bCalc ? "collapse" : "separate");
        // For now always use separating model:
        info.props.addValue("border-collapse", "separate");
        info.props.addValue("border-spacing", "0");

        info.props.addValue("table-layout","fixed");

        //info.props.addValue("empty-cells","show"); use &nbsp; instead...

        if (ofr.isSpreadsheet()) { info.props.addValue("white-space","nowrap"); }

        if (bIsSubTable) {
            // Should try to fill the cell; hence:
            info.props.addValue("width","100%");
            info.props.addValue("margin","0");
        }
        applyStyle(info,table);
    }

    private void applyRowStyle(String sStyleName, Element row) {
        StyleInfo info = new StyleInfo();
        getRowSc().applyStyle(sStyleName,info);

        if (!config.xhtmlIgnoreTableDimensions()) {
            StyleWithProperties style = ofr.getRowStyle(sStyleName);
            if (style!=null) {
                // Translates row style properties
                // OOo offers style:row-height and style:min-row-height
                // In css row heights are always minimal, so both are exported as height
                // If neither is specified, the tallest cell rules; this fits with css.
                String s = style.getAbsoluteProperty(XMLString.STYLE_ROW_HEIGHT);
                // Do not export minimal row height; causes trouble with ie
                //if (s==null) { s = style.getAbsoluteProperty(XMLString.STYLE_MIN_ROW_HEIGHT); }
                if (s!=null) { info.props.addValue("height",getRowSc().scale(s)); }
            }
        }

        applyStyle(info,row);
    }
	
    private void applyCellStyle(String sStyleName, String sTotalWidth, String sValueType, Element cell, boolean bIsSubTable) {
        StyleInfo info = new StyleInfo();
        getCellSc().applyStyle(sStyleName,info);

        StyleWithProperties style = ofr.getCellStyle(sStyleName);
        if (style!=null) {
            if (!config.xhtmlIgnoreTableDimensions()) {
                String sEdge = "0";
    
                // Set the cell width. This is calculated as
                // "total cell width" - "border" - "padding"
                String s = style.getProperty(XMLString.FO_PADDING_LEFT);
                if (s!=null) {
                    sEdge=Misc.add(sEdge,getTableSc().colScale(s));
                }
                s = style.getProperty(XMLString.FO_PADDING_RIGHT);
                if (s!=null) { 
                    sEdge=Misc.add(sEdge,getTableSc().colScale(s));
                }
                s = style.getProperty(XMLString.FO_PADDING);
                if (s!=null) {
                    sEdge=Misc.add(sEdge,Misc.multiply("200%",getTableSc().colScale(s)));
                }
                s = style.getProperty(XMLString.FO_BORDER_LEFT);
                if (s!=null) {
                    sEdge=Misc.add(sEdge,getTableSc().colScale(borderWidth(s)));
                }
                s = style.getProperty(XMLString.FO_BORDER_RIGHT);
                if (s!=null) { 
                    sEdge=Misc.add(sEdge,getTableSc().colScale(borderWidth(s)));
                }
                s = style.getProperty(XMLString.FO_BORDER);
                if (s!=null) {
                    sEdge=Misc.add(sEdge,Misc.multiply("200%",getTableSc().colScale(borderWidth(s))));
                }

                if (sTotalWidth!=null) {
                    info.props.addValue("width",Misc.sub(getTableSc().colScale(sTotalWidth),sEdge));
                }
            }

            // Automatic horizontal alignment (calc only)
            if (ofr.isSpreadsheet() && !"fix".equals(style.getProperty(XMLString.STYLE_TEXT_ALIGN_SOURCE))) {
                // Strings go left, other types (float, time, date, percentage, currency, boolean) go right
                // The default is string
                info.props.addValue("text-align", sValueType==null || "string".equals(sValueType) ? "left" : "right");
            }
        }
		
        if (!cell.hasChildNodes()) { // hack to handle empty cells even in msie
            // info.props.addValue("line-height","1px"); TODO: Reenable this...
            cell.appendChild( converter.createTextNode("\u00A0") );
        }
		
        if (bIsSubTable) {
            // Cannot set height of a subtable, if the subtable does not fill
            // the entire cell it is placed at the top
            info.props.addValue("vertical-align","top");
            // Don't add padding if there's a subtable in the cell!
            info.props.addValue("padding","0");
        }

        applyStyle(info,cell);
    }
	
    // TODO: Move me to a more logical place!
    public String borderWidth(String sBorder) {
        if (sBorder==null || sBorder.equals("none")) { return "0"; }
        SimpleInputBuffer in = new SimpleInputBuffer(sBorder);
        while (in.peekChar()!='\0') {
            // Skip spaces
            while(in.peekChar()==' ') { in.getChar(); }
            // If it's a number it must be a unit -> get it
            if ('0'<=in.peekChar() && in.peekChar()<='9') {
                return in.getNumber()+in.getIdentifier();
            }
            // skip other characters
            while (in.peekChar()!=' ' && in.peekChar()!='\0') { } 
        }
        return "0";
    }

	
}
