/************************************************************************
 *
 *  BeforeAfter.java
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
 *  Version 1.0 (2008-12-02)
 *
 */

package writer2latex.latex.util;

/** Utility class to hold LaTeX code to put before/after other LaTeX code
 */
public class BeforeAfter {
    private String sBefore="";
    private String sAfter="";
    
    /** Constructor to initialize the object with a pair of strings
     *  @param sBefore1 LaTeX code to put before
     *  @param sAfter1 LaTeX code to put after  
     */
    public BeforeAfter(String sBefore1, String sAfter1) {
        sBefore=sBefore1; sAfter=sAfter1;
    }
    
    /** Default constructor: Create with empty strings
     */
    public BeforeAfter() { }

    /** <p>Add data to the <code>BeforeAfter</code></p>
     *  <p>The new data will be be added "inside", thus for example</p>
     *  <ul><li><code>add("\textsf{","}");</code>
     *  <li><code>add("\textit{","}");</code></ul>
     *  <p>will create the pair <code>\textsf{\textit{</code>, <code>}}</code></p>
     *
     *  @param sBefore1 LaTeX code to put before
     *  @param sAfter1 LaTeX code to put after  
     */
    public void add(String sBefore1, String sAfter1) {
        sBefore+=sBefore1; sAfter=sAfter1+sAfter;
    }
	
    /** Get LaTeX code to put before
     *  @return then LaTeX code
     */
    public String getBefore() { return sBefore; }

    /** Get LaTeX code to put after
     *  @return then LaTeX code
     */
    public String getAfter() { return sAfter; }
	
    /** Check if this <code>BeforeAfter</code> contains any data
     *  @return true if there is data in at least one part
     */
    public boolean isEmpty() { return sBefore.length()==0 && sAfter.length()==0; }
   	
}