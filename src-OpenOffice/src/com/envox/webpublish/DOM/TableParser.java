/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.table.XTableRows;
import com.sun.star.text.TableColumnSeparator;
import com.sun.star.text.XTextTable;
import com.sun.star.uno.UnoRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author martin
 */
class TableParser {
    private XTextTable m_xTextTable;
    private String[] m_cellNames;

    private int m_nrows;
    private int m_ncols;
    private String[][] m_table;
    private int[][] m_colspan;
    private int[][] m_rowspan;
    private int m_tableWidth;
    private int[][] m_cellWidth;

    TableParser(XTextTable xTextTable) throws UnknownPropertyException, WrappedTargetException, IndexOutOfBoundsException {
        m_xTextTable = xTextTable;
        m_cellNames = m_xTextTable.getCellNames();

        XPropertySet xTablePS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_xTextTable);
        m_tableWidth = (Integer) xTablePS.getPropertyValue("Width");



        parse();
    }

    int getRows() { return m_nrows; }
    int getCols() { return m_ncols; }
    String getCell(int i, int j) { return m_table[i][j]; }
    int getColSpan(int i, int j) { return m_colspan[i][j]; }
    int getRowSpan(int i, int j) { return m_rowspan[i][j]; }
    int getTableWidth() { return m_tableWidth; }
    int getCellWidthAsPercent(int i, int j) { return m_cellWidth[i][j]; }

    class CellLocation {
        public int row;
        public int column;

        public CellLocation(String cellName) {
            row = 0;
            column = 0;
            for (int i = 0; i < cellName.length(); ++i) {
                char c = cellName.charAt(i);
                if (Character.isDigit(c)) {
                    row = row * 10 + c - '0';
                } else {
                    if (c <= 'Z') {
                        column = column * 52 + c - 'A';
                    } else {
                        column = column * 52 + 26 + (c - 'a');
                    }
                }
            }
            row--;
        }
    }

    private void parse() throws UnknownPropertyException, WrappedTargetException, IndexOutOfBoundsException {
        XTableRows xTableRows = m_xTextTable.getRows();

        // izraèunaj broj redaka
        m_nrows = xTableRows.getCount();

        // izraèunaj broj stupaca i pronaði pozicije granica kolona izražene relativno u odnosu na ukupnu širinu (relativeSum)
        XPropertySet xTablePS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, m_xTextTable);
        short relativeSum = (Short) xTablePS.getPropertyValue("TableColumnRelativeSum");

        Set<Short> positions = new TreeSet<Short>();
        positions.add((short)0);

        for (int i = 0; i < m_nrows; ++i) {
            XPropertySet xRowPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTableRows.getByIndex(i));
            TableColumnSeparator[] xSeparators = (TableColumnSeparator[]) xRowPS.getPropertyValue("TableColumnSeparators");
            for (int j = 0; j < xSeparators.length; ++j) {
                positions.add(xSeparators[j].Position);
            }
        }

        positions.add(relativeSum);

        m_ncols = positions.size() - 1;

        // mapa T mapira poziciju granice kolone u broj kolone koji ide od 0
        // mapa W mapira broj kolone u poziciju granice kolone
        Map<Short, Short> T = new TreeMap<Short, Short>();
        Map<Short, Short> W = new TreeMap<Short, Short>();
        {
            short i = 0;
            for (Short pos: positions) {
                T.put(pos, i);
                W.put(i, pos);
                ++i;
            }
        }

        // izraèunaj colspan za sve kolone u svim retcima
        //
        // row_1: colspan_1 colspan_2 ... colspan_n1
        // row_2: colspan_1 colspan_2 ... colspan_n2
        // ...
        // row_m: colspan_1 colspan_2 ... colspan_nm
        //
        List<List<Short>> colspans = new ArrayList<List<Short>>();

        for (int i = 0; i < m_nrows; ++i) {
            List<Short> cs = new ArrayList<Short>();

            XPropertySet xRowPS = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTableRows.getByIndex(i));
            TableColumnSeparator[] xSeparators = (TableColumnSeparator[]) xRowPS.getPropertyValue("TableColumnSeparators");

            Short p1 = new Short((short)0);
            for (int j = 0; j < xSeparators.length; ++j) {
                Short p2 = xSeparators[j].Position;
                cs.add((short)(T.get(p2) - T.get(p1)));
                p1 = p2;
            }

            cs.add((short)(T.get(relativeSum) - T.get(p1)));

            colspans.add(cs);
        }

        // pronaði položaj èelija unutar tablice, reprezentiraj kao 2D polje,
        // colspan i rowspan trebaju biti uraèunati, npr:
        //
        // A1 A1 B1 C1 D1
        // A1 A1 B2 C2 D1
        // A3 B3 C3 C2 E3
        //
        m_table = new String[m_nrows][m_ncols];

        // iteriraj po imenima æelija, od lijeva prema desno, gore prema dolje
        for (String name: m_cellNames) {
            CellLocation loc = new CellLocation(name);

            // npr. ako je na redu èelija B2:
            //
            // A1 A1 B1 C1 D1
            // XX XX
            //
            // (XX oznaèava null vrijednost)
            //
            // spustitu æemo èeliju A1 na prva dva mjesta u slijedeæi redak,
            // tako da dobijemo:
            //
            // A1 A1 B1 C1 D1
            // A1 A1
            //
            int j = 0;

            for (int i = 0; i < loc.column; ++i) {
                for (int k = 0; k < colspans.get(loc.row).get(i); ++k, ++j) {
                    if (m_table[loc.row][j] == null) {
                        m_table[loc.row][j] = m_table[loc.row - 1][j];
                    }
                }
            }

            // sada još treba postaviti æeliju B1 na svoje mjesto:
            //
            // A1 A1 B1 C1 D1
            // A1 A1 B1
            //
            for (int k = 0; k < colspans.get(loc.row).get(loc.column); ++k, ++j) {
                m_table[loc.row][j] = name;
            }
        }

        // neka mjesta na desnom rubu tablice æe možda biti null,
        // postavi na ta mjesta æeliju koja se nalazi u istoj koloni,
        // redak iznad, npr. od:
        //
        // A1 A1 B1 C1 D1
        // A1 A1 B2 C2 XX
        // A3 B3 C3 C2 E3
        //
        // æemo dobiti:
        //
        // A1 A1 B1 C1 D1
        // A1 A1 B2 C2 D1
        // A3 B3 C3 C2 E3
        //
        for (int i = 0; i < m_nrows; ++i) {
            for (int j = 0; j < m_ncols; ++j) {
                if (m_table[i][j] == null) {
                    m_table[i][j] = m_table[i - 1][j];
                }
            }
        }

        // izraèunaj colspan i rowspan za svaku æeliju,
        // koristi se tablica sa polažajem æelija, tako da se broji koliko neka
        // æelija zauzima uzastopnih mjesta horizontalno (za colspan) i
        // vertikalno (za rowspan)
        m_colspan = new int[m_nrows][m_ncols];
        m_rowspan = new int[m_nrows][m_ncols];

        for (int i = 0; i < m_nrows; ++i) {
            for (int j = 0; j < m_ncols; ++j) {
                if (m_table[i][j] != null) {
                    m_colspan[i][j] = 1;
                    for (int k = j + 1; k < m_ncols && m_table[i][k].equals(m_table[i][j]); ++k) {
                        ++m_colspan[i][j];
                    }

                    m_rowspan[i][j] = 1;
                    for (int k = i + 1; k < m_nrows && m_table[k][j].equals(m_table[i][j]); ++k) {
                        ++m_rowspan[i][j];
                    }
                }
            }
        }

        // obriši èelije koje se ponavljaju (ostavi samo gore-lijevu):
        // npr. za tablicu:
        //
        // A1 A1 B1 C1 D1
        // A1 A1 B2 C2 D1
        // A3 B3 C3 C2 E3
        //
        // treba dobiti:
        //
        // A1 XX B1 C1 D1
        // XX XX B2 C2 XX
        // A3 B3 C3 XX E3
        //
        // napravi isto za colspan i rowspan tablice
        for (int i = 0; i < m_nrows; ++i) {
            for (int j = 0; j < m_ncols; ++j) {
                if (m_table[i][j] != null) {
                    for (int m = 0; m < m_rowspan[i][j]; m++) {
                        for (int n = 0; n < m_colspan[i][j]; n++) {
                            if (m != 0 || n != 0) {
                                m_table[i+m][j+n] = null;
                                m_colspan[i+m][j+n] = 0;
                                m_rowspan[i+m][j+n] = 0;
                            }
                        }
                    }
                }
            }
        }

        // izraèunaj širinu svih æelija vodeæi raèuna o colspan,
        // širina æelija treba biti u postocima u odnosu na ukupnu širinu tablice
        m_cellWidth = new int[m_nrows][m_ncols];
        for (int i = 0; i < m_nrows; ++i) {

            short j1 = 0;
            for (int k = 0; k < colspans.get(i).size(); ++k) {
                short j2 = (short) (j1 + colspans.get(i).get(k));
                m_cellWidth[i][j1] = (int)Math.round((W.get(j2) - W.get(j1)) * 100.0 / relativeSum);
                j1 = j2;
            }
        }
    }
}
