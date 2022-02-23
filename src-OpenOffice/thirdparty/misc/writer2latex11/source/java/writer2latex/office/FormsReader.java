/************************************************************************
 *
 *  FormsReader.java
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
 *  Version 1.0 (2008-11-23)
 *
 */

package writer2latex.office;

import java.util.Hashtable;
import java.util.Iterator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** <p> This class reads the collection of all forms in an OOo document
  * (the <code>office:forms</code> element).</p>
  * <p>An OOo document may contain any number of forms; these are declared
  * within this element. In OOo, unlike eg. html, the form declaration is
  * separated from the presentation. This element contains the
  * <em>declaration</em>. The <em>presentation</em> is given by inclusion of
  * <code>draw:control</code> elements in the document flow. These refer to form
  * controls by id.</p>
  * <p>Note: A form is identified by a unique name, a control is
  * identified by a (globally) unique id.</p> 
  */
public class FormsReader {

    private Element formsElement; // The office:forms element
    private Hashtable forms = new Hashtable(); // all forms, indexed by name
    private Hashtable controls = new Hashtable(); // all controls, indexed by id
	
    /** <p>Read the content of an <code>office:forms</code> element</p>
     *  @param formsElement a DOM element, which must be <code>office:forms</code> node
     */
    public void read(Element formsElement) {
        this.formsElement = formsElement;
        // Collect all forms
        Node child = formsElement.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE &&
                child.getNodeName().equals(XMLString.FORM_FORM)) {
                FormReader form = new FormReader((Element) child, this);
                forms.put(form.getName(),form);
            }
            child = child.getNextSibling();
        }
    }
	
    /** <p>Get an attribute of the forms. If the attribute does not exist,
     *  this method returns <code>null</code>.
     *  @param sName the name of the attribute
     *  @return the value of the attribute, or <code>null</code>
     */
    public String getAttribute(String sName) {
        return formsElement.hasAttribute(sName) ? formsElement.getAttribute(sName) : null;
    }

    /** <p>Get a <code>Iterator</code> over all forms.</p>
     *  @return a <code>Iterator</code> over all forms
     */
    public Iterator getFormsIterator() {
        return forms.values().iterator();
    }

    /** <p>Get a form by name</p>
     *  @param sName the <code>form:name</code> of the form
     *  @return the form as a <code>FormReader</code> object
     */
    public FormReader getForm(String sName) {
        return (FormReader) forms.get(sName);
    }

    /** <p>Get a <code>Iterator</code> over all controls.</p>
     *  @return a <code>Iterator</code> over all controls
     */
    public Iterator getControlsIterator() {
        return controls.values().iterator();
    }
	
    /** <p>Get a control by id</p>
     *  @param sId the <code>form:control-id</code> of the control
     *  @return the control as a <code>ControlReader</code> object
     */
    public ControlReader getControl(String sId) {
        return (ControlReader) controls.get(sId);
    }
	
    /** <p>Add a control</p>
     *  @param control a <code>ControlReader</code> representing the control
     */
    protected void addControl(ControlReader control) {
        controls.put(control.getId(),control);
    }
	
}