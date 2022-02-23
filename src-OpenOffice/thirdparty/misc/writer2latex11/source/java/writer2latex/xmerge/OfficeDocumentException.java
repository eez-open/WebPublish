/************************************************************************
 *
 *  The Contents of this file are made available subject to the terms of
 *
 *         - GNU Lesser General Public License Version 2.1
 *
 *  Sun Microsystems Inc., October, 2000
 *
 *  GNU Lesser General Public License Version 2.1
 *  =============================================
 *  Copyright 2000 by Sun Microsystems, Inc.
 *  901 San Antonio Road, Palo Alto, CA 94303, USA
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
 *  The Initial Developer of the Original Code is: Sun Microsystems, Inc.
 *
 *  Copyright: 2000 by Sun Microsystems, Inc.
 *
 *  All Rights Reserved.
 *
 *  Contributor(s): _______________________________________
 *
 *
 ************************************************************************/
 
// This version is adapted for Writer2LaTeX
// Version 1.0 (2008-11-22)

package writer2latex.xmerge;

import java.io.IOException;

//import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

//import org.openoffice.xmerge.util.Resources;

/**
 *  Used by OfficeDocument to encapsulate exceptions.  It will add
 *  more details to the message string if it is of type
 *  <code>SAXParseException</code>.
 *
 *  @author      Herbie Ong
 */

public final class OfficeDocumentException extends IOException {

    StringBuffer message = null;


   /**
    *  Constructor, capturing additional information from the
    *  <code>SAXException</code>.
    *
    *  @param  e  The <code>SAXException</code>.
	*/
    public OfficeDocumentException(SAXException e) {
        super(e.toString());
        message = new StringBuffer();
        if (e instanceof SAXParseException) {
            String msgParseError =
                "PARSE_ERROR";
            String msgLine =
                "LINE";
            String msgColumn =
                "COLUMN";
            String msgPublicId =
                "PUBLIC_ID";
            String msgSystemId =
                "SYSTEM_ID";
            SAXParseException spe = (SAXParseException) e;
            message.append(msgParseError);
            message.append(": ");
            message.append(msgLine);
            message.append(": ");
            message.append(spe.getLineNumber());
            message.append(", ");
            message.append(msgColumn);
            message.append(": ");
            message.append(spe.getColumnNumber());
            message.append(", ");
            message.append(msgSystemId);
            message.append(": ");
            message.append(spe.getSystemId());
            message.append(", ");
            message.append(msgPublicId);
            message.append(": ");
            message.append(spe.getPublicId());
            message.append("\n");
        }

        // if there exists an embedded exception
        Exception ex = e.getException();
        if (ex != null) {
            message.append(ex.getMessage());
        }
    }


   /**
    *  Constructor, creates exception with provided message.
    *
    *  @param  s  Message value for the exception.
	*/
    public OfficeDocumentException(String s) {
        super(s);
    }


   /**
    *  Constructor, creates exception with the message
    *  corresponding to the message value of the provided
    *  exception.
    *
    *  @param  e  The Exception.
	*/
    public OfficeDocumentException(Exception e) {
        super(e.getMessage());
    }


   /**
    *  Returns the message value for the <code>Exception</code>.
    *
    * @return  The message value for the <code>Exception</code>.
	*/
    public String getMessage() {
        return message.toString() + super.getMessage();
    }
}

