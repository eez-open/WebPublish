/************************************************************************
 *
 *  ReplacementTrie.java
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
 *  Copyright: 2002-2006 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 0.5 (2006-11-02)
 *
 */

package writer2latex.latex.i18n;

/** This class contains a trie of string -> LaTeX code replacements 
*/
public class ReplacementTrie extends ReplacementTrieNode {

    public ReplacementTrie() {
        super('*',0);
    }
	
    public ReplacementTrieNode get(String sInput) {
        return get(sInput,0,sInput.length());
    }
	
    public ReplacementTrieNode get(String sInput, int nStart, int nEnd) {
        if (sInput.length()==0) { return null; }
        else { return super.get(sInput,nStart,nEnd); }
    }
	
    public void put(String sInput, String sLaTeXCode, int nFontencs) {
        if (sInput.length()==0) { return; }
        else { super.put(sInput,sLaTeXCode,nFontencs); }
    }
	
    public String[] getInputStrings() {
        return null; //TODO
    }


}
