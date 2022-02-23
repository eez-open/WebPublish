/************************************************************************
 *
 *  ControlReader.java
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

//import java.util.Hashtable;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import writer2latex.util.Misc;

/** <p> This class reads a form control in an OOo document (a form:control
  * node). A control always has an owner form.
  * Properties and events are ignored.</p>
  */
public class ControlReader {

    private FormReader ownerForm; // a control always belongs to a form
    private String sId; // a control is identified by id
    private Element control; // the control element
    private Element controlType; // the type specific child element
    private Vector items = new Vector(); // the options/items of a list/combobox
	
    /** <p>The constructor reads the content of a control element</p>
     *  The representation in OpenDocument differs slightly from OOo 1.x.
     
     *  @param control a DOM element, which must be control node
     */
    public ControlReader(Element control, FormReader ownerForm) {
        this.ownerForm = ownerForm;
        this.control = control;
        sId = control.getAttribute(XMLString.FORM_ID);
        // Read the control type specific info
        if (control.getTagName().equals(XMLString.FORM_CONTROL)) { // old format
            controlType = Misc.getFirstChildElement(control);
        }
        else { // oasos
            controlType = control;
        }
        if (controlType!=null) { // must always be the case!
            // Collect options/items
            Node child = controlType.getFirstChild();
            while (child!=null) {
                if (child.getNodeType()==Node.ELEMENT_NODE && (
                    child.getNodeName().equals(XMLString.FORM_OPTION) || 
                    child.getNodeName().equals(XMLString.FORM_ITEM))) {
                    items.add(child);
                }
                child = child.getNextSibling();
            }
        }
    }
	
    /** <p>A control in OOo is identified by id (<code>form:control-id</code>
     * attribute. The id is accessed by this method.</p>
     *  @return the id of the control
     */
    public String getId() { return sId; }
	
    /** <p>A control in OOo belongs to a form.</p>
     *  @return the form containing this control
     */
    public FormReader getOwnerForm() { return ownerForm; }
 
    /** <p>Get an attribute of the control. If the attribute does not exist,
     *  this method returns <code>null</code>.
     *  @param sName the name of the attribute
     *  @return the value of the attribute, or <code>null</code>
     */
    public String getAttribute(String sName) {
        return control.hasAttribute(sName) ? control.getAttribute(sName) : null;
    }
	
    /** <p>The type of the control is identified by a name, eg. form:submit</p>
     *  @return the type of this control
     */
    public String getControlType() { return controlType.getTagName(); }
	
    /** <p>Get an attribute specific to this type of control.
     *  If the attribute does not exist, this method returns <code>null</code>.
     *  @param sName the name of the attribute
     *  @return the value of the attribute, or <code>null</code>
     */
    public String getTypeAttribute(String sName) {
        return controlType!=null && controlType.hasAttribute(sName) ?
               controlType.getAttribute(sName) : null;
    }
	
    /** <p>Return the number of options/items in this control.
     *  Only listbox (options) and combobox (items) controls can have these,
     *  for other controls this will return 0.
     *  @return the number of options/items
     */
    public int getItemCount() { return items.size(); } 

    /** <p>Get an attribute of an option/item.
     *  If the index and/or the attribute does not exist, this method returns
     *  <code>null</code>.
     *  @param nIndex the index of the option/item
     *  @param sName the name of the attribute
     *  @return the value of the attribute, or <code>null</code>
     */
    public String getItemAttribute(int nIndex, String sName) {
        if (0<=nIndex && nIndex<=items.size()) {
            return ((Element)items.get(nIndex)).hasAttribute(sName) ?
                   ((Element)items.get(nIndex)).getAttribute(sName) : null;
        }
        else {
            return null;
        }
    }

    /** <p>Get the value of an option/item.
     *  If the index does not exist, this method returns
     *  <code>null</code>.
     *  @param nIndex the index of the option/item
     *  @return the value of the option/item, or <code>null</code>
     */
    public String getItemValue(int nIndex) {
        if (0<=nIndex && nIndex<=items.size()) {
            return Misc.getPCDATA((Element)items.get(nIndex));
        }
        else {
            return null;
        }
    }
		
}