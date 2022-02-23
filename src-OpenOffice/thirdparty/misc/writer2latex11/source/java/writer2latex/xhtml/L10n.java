/************************************************************************
 *
 *  L10n.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0.2 (2010-04-26)
 *
 */

package writer2latex.xhtml;

// This class handles localized strings (used for navigation)
public class L10n {
    public final static int UP = 0;
    public final static int FIRST = 1;
    public final static int PREVIOUS = 2;
    public final static int NEXT = 3;
    public final static int LAST = 4;
    public final static int CONTENTS = 5;
    public final static int INDEX = 6;
    public final static int HOME = 7;
    public final static int DIRECTORY = 8;
    public final static int DOCUMENT = 9;

    private String sLocale="en-US";
	
    public void setLocale(String sLocale) {
        if (sLocale!=null) { this.sLocale = sLocale;}
    }
	
    public void setLocale(String sLanguage, String sCountry) {
        if (sLanguage!=null) {
            if (sCountry!=null) { sLocale = sLanguage + "-" + sCountry; }
            else  { sLocale = sLanguage; }
        }
    }
	
    public String get(int nString) {
        if (sLocale.startsWith("de")) { // german
            switch (nString) {
                case UP: return "Nach oben";
                case FIRST : return "Anfang";
                case PREVIOUS : return "Vorheriges";
                case NEXT : return "N\u00e4chstes";
                case LAST : return "Ende";
                case CONTENTS : return "Inhalte";
                case INDEX : return "Index";
                case HOME : return "Home";
                case DIRECTORY: return "Verzeichnis";
                case DOCUMENT: return "Dokument";
            }
        }
        if (sLocale.startsWith("fr")) { // french
            switch (nString) {
            	case UP: return "Haut";
            	case FIRST : return "D\u00e9but";
            	case PREVIOUS : return "Pr\u00e9c\u00e9dent";
            	case NEXT : return "Suivant";
            	case LAST : return "Dernier";
            	case CONTENTS : return "Contenus";
            	case INDEX : return "Index";
                case HOME : return "Documents Personnels";
            	case DIRECTORY: return "R\u00e9pertoire";
            	case DOCUMENT: return "Document";
            }
        }
        if (sLocale.startsWith("es")) { // spanish
            switch (nString) {
                case UP: return "Arriba";
                case FIRST : return "Primero";
                case PREVIOUS : return "Previo";
                case NEXT : return "Siguiente";
                case LAST : return "\u00daltimo";
                case CONTENTS : return "Contenido";
                case INDEX : return "\u00cdndice";
                case HOME : return "Inicio";
                case DIRECTORY: return "Directorio";
                case DOCUMENT: return "Documento";
            }
        }
        if (sLocale.startsWith("it")) { // italian
            switch (nString) {
            	case UP: return "Su";
            	case FIRST : return "Inizio";
            	case PREVIOUS : return "Precedente";
            	case NEXT : return "Successivo";
            	case LAST : return "Fine";
            	case CONTENTS : return "Sommario";
            	case INDEX : return "Indice";
            	case HOME : return "Home";
            	case DIRECTORY: return "Cartella";
            	case DOCUMENT: return "Documento";     
            }
        }
        if (sLocale.startsWith("pt")) { // (brazilian) portuguese
            switch (nString) {
            	case UP: return "Acima";
            	case FIRST : return "Primeiro";
            	case PREVIOUS : return "Anterior";
            	case NEXT : return "Pr\u00f3ximo";
            	case LAST : return "\u00daltimo";
            	case CONTENTS : return "Conte\u00fado";
            	case INDEX : return "\u00cdndice";
            	case HOME : return "Home";
            	case DIRECTORY: return "Diret\u00f3rio";
            	case DOCUMENT: return "Documento";     
            }
        }
        if (sLocale.startsWith("cs")) { // czech
            switch (nString) {
            	case UP: return "Nahoru";
            	case FIRST : return "Prvn\u00ed";
            	case PREVIOUS : return "P\u0159edchoz\u00ed";
            	case NEXT : return "Dal\u0161\u00ed";
            	case LAST : return "Posledn\u00ed";
            	case CONTENTS : return "Obsah";
            	case INDEX : return "Rejst\u0159\u00edk";
            	case HOME : return "Dom\u016f";
            	case DIRECTORY: return "Adres\u00e1\u0159 (slo\u017eka)";
            	case DOCUMENT: return "Dokument";     
            }
        }
        if (sLocale.startsWith("nl")) { // dutch
            switch (nString) {
            case UP: return "Omhoog";
            case FIRST : return "Eerste";
            case PREVIOUS : return "Vorige";
            case NEXT : return "Volgende";
            case LAST : return "Laatste";
            case CONTENTS : return "Inhoud";
            case INDEX : return "Index";
            case HOME : return "Hoofdpagina";
            case DIRECTORY: return "Directory";
            case DOCUMENT: return "Document";  
            }
        }
        if (sLocale.startsWith("da")) { // danish
            switch (nString) {
                case UP: return "Op";
                case FIRST : return "F\u00F8rste";
                case PREVIOUS : return "Forrige";
                case NEXT : return "N\u00E6ste";
                case LAST : return "Sidste";
                case CONTENTS : return "Indhold";
                case INDEX : return "Stikord";
                case HOME : return "Hjem";
                case DIRECTORY: return "Mappe";
                case DOCUMENT: return "Dokument";
            }
        }
        if (sLocale.startsWith("pl")) { // polish
        	switch (nString) {
        		case UP: return "W g\u00f3r\u0119";
        		case FIRST : return "Pierwsza";
        		case PREVIOUS : return "Poprzednia";
        		case NEXT : return "Nast\u0119pna";
        		case LAST : return "Ostatnia";
        		case CONTENTS : return "Spis tre\u015bci";
        		case INDEX : return "Indeks";
        		case HOME : return "Pocz\u0105tek";
        		case DIRECTORY: return "Katalog";
        		case DOCUMENT: return "Dokument";
        	}
        }
        if (sLocale.startsWith("fi")) { // finnish
        	switch (nString) {
        		case UP: return "Yl\u00f6s";
        		case FIRST : return "Ensimm\u00e4inen";
        		case PREVIOUS : return "Edellinen";
        		case NEXT : return "Seuraava";
        		case LAST : return "Viimeinen";
        		case CONTENTS : return "Sis\u00e4lt\u00f6";
        		case INDEX : return "Indeksi";
        		case HOME : return "Koti";
        		case DIRECTORY: return "Hakemisto";
        		case DOCUMENT: return "Dokumentti";
        	}
        }
        if (sLocale.startsWith("ru")) { // russian
            switch (nString) {
            	case UP: return "\u0412\u0432\u0435\u0440\u0445";
            	case FIRST : return "\u041f\u0435\u0440\u0432\u0430\u044f";
            	case PREVIOUS : return "\u041f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0430\u044f";
            	case NEXT : return "\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f";
            	case LAST : return "\u041f\u043e\u0441\u043b\u0435\u0434\u043d\u044f\u044f";
            	case CONTENTS : return "\u0421\u043e\u0434\u0435\u0440\u0436\u0430\u043d\u0438\u0435";
            	case INDEX : return "\u0421\u043f\u0438\u0441\u043e\u043a";
            	case HOME : return "\u0414\u043e\u043c\u043e\u0439";
            	case DIRECTORY: return "\u0414\u0438\u0440\u0435\u043a\u0442\u043e\u0440\u0438\u044f";
            	case DOCUMENT: return "\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442";
            }
        }
        if (sLocale.startsWith("uk")) { // ukrainian
            switch (nString) {
            	case UP: return "\u041d\u0430\u0433\u043e\u0440\u0443";
            	case FIRST : return "\u041f\u0435\u0440\u0448\u0430";
            	case PREVIOUS : return "\u041f\u043e\u043f\u0435\u0440\u0435\u0434\u043d\u044f";
            	case NEXT : return "\u041d\u0430\u0441\u0442\u0443\u043f\u043d\u0430";
            	case LAST : return "\u041e\u0441\u0442\u0430\u043d\u043d\u044f";
            	case CONTENTS : return "\u0417\u043c\u0456\u0441\u0442";
            	case INDEX : return "\u0421\u043f\u0438\u0441\u043e\u043a";
            	case HOME : return "\u0414\u043e\u0434\u043e\u043c\u0443";
            	case DIRECTORY: return "\u0422\u0435\u043a\u0430";
            	case DOCUMENT: return "\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442";
            }
        }
        if (sLocale.startsWith("tr")) { // turkish
            switch (nString) {
            	case UP: return "Yukar\u0131";
            	case FIRST : return "\u0130lk";
            	case PREVIOUS : return "\u00d6nceki";
            	case NEXT : return "Sonraki";
            	case LAST : return "Son";
            	case CONTENTS : return "\u0130\u00e7indekiler";
            	case INDEX : return "\u0130ndeks";
            	case HOME : return "Ev";
            	case DIRECTORY: return "Klas\u00f6r";
            	case DOCUMENT: return "D\u00f6k\u00fcman";
            }        	
        }
        if (sLocale.startsWith("hr")) { // croatian
            switch (nString) {
                case UP: return "Up";
                case FIRST : return "Prvi";
                case PREVIOUS : return "Prethodan";
                case NEXT : return "slijede\u0107i";
                case LAST : return "Zadnji";
                case CONTENTS : return "Sadr\u017Eaj";
                case INDEX : return "Indeks";
                case DIRECTORY: return "Directory";
                case DOCUMENT: return "Document";
            }
        }
        // english - default
        switch (nString) {
            case UP: return "Up";
            case FIRST : return "First";
            case PREVIOUS : return "Previous";
            case NEXT : return "Next";
            case LAST: return "Last";
            case CONTENTS : return "Contents";
            case INDEX : return "Index";
            case HOME : return "Home";
            case DIRECTORY: return "Directory";
            case DOCUMENT: return "Document";
        }
        return "???";
    }
}
