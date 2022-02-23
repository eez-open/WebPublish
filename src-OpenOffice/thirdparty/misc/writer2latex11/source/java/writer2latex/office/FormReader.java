/************************************************************************
 *
 *  FormReader.java
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
 */

// Version 1.0 (2008-11-22)
 
package writer2latex.office;

//import java.util.Hashtable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.util.Misc;

/** <p> This class reads a form in an OOo document (a form:form node)</p>
  * Note: Subforms, properties and events are ignored.
  */
public class FormReader {

    //private FormsReader forms; // the global collection of all forms
    private String sName; // a form is identified by name
    private Element form; // the form element
	
    /** <p>The constructor reads the content of a <code>form:form</code> element</p>
     *  @param form a DOM element, which must be <code>form:form</code> node
     */
    public FormReader(Element form, FormsReader forms) {
        //this.forms = forms;
        this.form = form;
        sName = form.getAttribute(XMLString.FORM_NAME);
        // Collect all controls contained in this form
        Node child = form.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                String sId = Misc.getAttribute((Element)child,XMLString.FORM_ID);
                if (sId!=null) {
                  ControlReader control = new ControlReader((Element) child, this);
                  forms.addControl(control);
                }
            }
            child = child.getNextSibling();
        }
    }
	
    /** <p>A form in OOo is identified by name (<code>form:name</code>
     * attribute. The name is accessed by this method.</p>
     *  @return the name of the form
     */
    public String getName() { return sName; }
	
    /** <p>Get an attribute of the form. If the attribute does not exist,
     *  this method returns <code>null</code>.
     *  @param sName the name of the attribute
     *  @return the value of the attribute, or <code>null</code>
     */
    public String getAttribute(String sName) {
        return form.hasAttribute(sName) ? form.getAttribute(sName) : null;
    }
	
}