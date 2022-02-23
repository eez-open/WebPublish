/*
 * OfficeUNOClientApp.java
 *
 * Created on 2011.02.24 - 16:51:16
 *
 */

package com.example;

import com.sun.star.uno.UnoRuntime;

/**
 *
 * @author martin
 */
public class OfficeUNOClientApp {
    
    /** Creates a new instance of OfficeUNOClientApp */
    public OfficeUNOClientApp() {
    }
    
        public static void main(String args[]) {


        //oooooooooooooooooooooooooooStep 1oooooooooooooooooooooooooooooooooooooooo
        // bootstrap UNO and get the remote component context. The context can
        // be used to get the service manager
        //*************************************************************************
        com.sun.star.uno.XComponentContext xContext = null;

        try {
            // get the remote office component context
            xContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
            if( xContext != null )
                System.out.println("Connected to a running office ...");
        }
        catch( Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        //oooooooooooooooooooooooooooStep 2oooooooooooooooooooooooooooooooooooooooo
        // open an empty document. In this case it's a writer document.
        // For this purpose an instance of com.sun.star.frame.Desktop
        // is created. It's interface XDesktop provides the XComponentLoader,
        // which is used to open the document via loadComponentFromURL
        //*************************************************************************

        //Open Writer document

        System.out.println("Opening an empty Writer document");
        com.sun.star.text.XTextDocument myDoc = openWriter(xContext);

        //*************************************************************************


        //oooooooooooooooooooooooooooStep 3oooooooooooooooooooooooooooooooooooooooo
        // insert some text.
        // For this purpose get the Text-Object of the document an create the
        // cursor. Now it is possible to insert a text at the cursor-position
        // via insertString
        //*************************************************************************


        //getting the text object
        com.sun.star.text.XText xText = myDoc.getText();

        //create a cursor object
        com.sun.star.text.XTextCursor xTCursor = xText.createTextCursor();

        //inserting some Text
        xText.insertString( xTCursor, "The first line in the newly created text document.\n", false );

        //inserting a second line
        xText.insertString( xTCursor, "Now we're in the second line\n", false );

        //*************************************************************************


        //oooooooooooooooooooooooooooStep 4oooooooooooooooooooooooooooooooooooooooo
        // insert a text table.
        // For this purpose get MultiServiceFactory of the document, create an
        // instance of com.sun.star.text.TextTable and initialize it. Now it can
        // be inserted at the cursor position via insertTextContent.
        // After that some properties are changed and some data is inserted.
        //*************************************************************************

        //inserting a text table
        System.out.println("Inserting a text table");

        //getting MSF of the document
        com.sun.star.lang.XMultiServiceFactory xDocMSF =
            (com.sun.star.lang.XMultiServiceFactory) UnoRuntime.queryInterface(
                com.sun.star.lang.XMultiServiceFactory.class, myDoc);

        //create instance of a text table
        com.sun.star.text.XTextTable xTT = null;

        try {
            Object oInt = xDocMSF.createInstance("com.sun.star.text.TextTable");
            xTT = (com.sun.star.text.XTextTable)
                UnoRuntime.queryInterface(com.sun.star.text.XTextTable.class,oInt);
        } catch (Exception e) {
            System.err.println("Couldn't create instance "+ e);
            e.printStackTrace(System.err);
        }

        //initialize the text table with 4 columns an 4 rows
        xTT.initialize(4,4);

        com.sun.star.beans.XPropertySet xTTRowPS = null;

        //insert the table
        try {
            xText.insertTextContent(xTCursor, xTT, false);
            // get first Row
            com.sun.star.container.XIndexAccess xTTRows = xTT.getRows();
            xTTRowPS = (com.sun.star.beans.XPropertySet)UnoRuntime.queryInterface(
                com.sun.star.beans.XPropertySet.class, xTTRows.getByIndex(0));

        } catch (Exception e) {
            System.err.println("Couldn't insert the table " + e);
            e.printStackTrace(System.err);
        }


        // get the property set of the text table

        com.sun.star.beans.XPropertySet xTTPS = (com.sun.star.beans.XPropertySet)
            UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xTT);

        // Change the BackColor
        try {
            xTTPS.setPropertyValue("BackTransparent", new Boolean(false));
            xTTPS.setPropertyValue("BackColor",new Integer(13421823));
            xTTRowPS.setPropertyValue("BackTransparent", new Boolean(false));
            xTTRowPS.setPropertyValue("BackColor",new Integer(6710932));

        } catch (Exception e) {
            System.err.println("Couldn't change the color " + e);
            e.printStackTrace(System.err);
        }

        // write Text in the Table headers
        System.out.println("Write text in the table headers");

        insertIntoCell("A1","FirstColumn", xTT);
        insertIntoCell("B1","SecondColumn", xTT) ;
        insertIntoCell("C1","ThirdColumn", xTT) ;
        insertIntoCell("D1","SUM", xTT) ;


        //Insert Something in the text table
        System.out.println("Insert something in the text table");

        (xTT.getCellByName("A2")).setValue(22.5);
        (xTT.getCellByName("B2")).setValue(5615.3);
        (xTT.getCellByName("C2")).setValue(-2315.7);
        (xTT.getCellByName("D2")).setFormula("sum <A2:C2>");

        (xTT.getCellByName("A3")).setValue(21.5);
        (xTT.getCellByName("B3")).setValue(615.3);
        (xTT.getCellByName("C3")).setValue(-315.7);
        (xTT.getCellByName("D3")).setFormula("sum <A3:C3>");

        (xTT.getCellByName("A4")).setValue(121.5);
        (xTT.getCellByName("B4")).setValue(-615.3);
        (xTT.getCellByName("C4")).setValue(415.7);
        (xTT.getCellByName("D4")).setFormula("sum <A4:C4>");


        //oooooooooooooooooooooooooooStep 5oooooooooooooooooooooooooooooooooooooooo
        // insert a colored text.
        // Get the propertySet of the cursor, change the CharColor and add a
        // shadow. Then insert the Text via InsertString at the cursor position.
        //*************************************************************************

        // get the property set of the cursor
        com.sun.star.beans.XPropertySet xTCPS = (com.sun.star.beans.XPropertySet)
            UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class,
                                      xTCursor);

        Object oldValue = null;

        // Change the CharColor and add a Shadow
        try {
            xTCPS.setPropertyValue("CharColor",new Integer(255));
            xTCPS.setPropertyValue("CharShadowed", new Boolean(true));
        } catch (Exception e) {
            System.err.println("Couldn't change the color " + e);
            e.printStackTrace(System.err);
        }

        //create a paragraph break
        try {
            xText.insertControlCharacter(xTCursor,
                      com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);

        } catch (Exception e) {
            System.err.println("Couldn't insert break "+ e);
            e.printStackTrace(System.err);
        }

        //inserting colored Text
        System.out.println("Inserting colored Text");

        xText.insertString(xTCursor, " This is a colored Text - blue with shadow\n",
                           false );

        //create a paragraph break
        try {
            xText.insertControlCharacter(xTCursor,
                      com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false);

        } catch (Exception e) {
            System.err.println("Couldn't insert break "+ e);
            e.printStackTrace(System.err);
        }

        //oooooooooooooooooooooooooooStep 6oooooooooooooooooooooooooooooooooooooooo
        // insert a text frame.
        // create an instance of com.sun.star.text.TextFrame using the MSF of the
        // document. Change some properties an insert it.
        // Now get the text-Object of the frame an the corresponding cursor.
        // Insert some text via insertString.
        //*************************************************************************

        // Create a TextFrame
        com.sun.star.text.XTextFrame xTF = null;
        com.sun.star.drawing.XShape xTFS = null;

        try {
            Object oInt = xDocMSF.createInstance("com.sun.star.text.TextFrame");
            xTF = (com.sun.star.text.XTextFrame) UnoRuntime.queryInterface(
                com.sun.star.text.XTextFrame.class,oInt);
            xTFS = (com.sun.star.drawing.XShape) UnoRuntime.queryInterface(
                com.sun.star.drawing.XShape.class,oInt);

            com.sun.star.awt.Size aSize = new com.sun.star.awt.Size();
            aSize.Height = 400;
            aSize.Width = 15000;

            xTFS.setSize(aSize);
        } catch (Exception e) {
            System.err.println("Couldn't create instance "+ e);
            e.printStackTrace(System.err);
        }

        // get the property set of the text frame
        com.sun.star.beans.XPropertySet xTFPS = (com.sun.star.beans.XPropertySet)
            UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xTF);

        // Change the AnchorType
        try {
            xTFPS.setPropertyValue("AnchorType",
                      com.sun.star.text.TextContentAnchorType.AS_CHARACTER);
        } catch (Exception e) {
            System.err.println("Couldn't change the color " + e);
            e.printStackTrace(System.err);
        }

        //insert the frame
        System.out.println("Insert the text frame");

        try {
            xText.insertTextContent(xTCursor, xTF, false);
        } catch (Exception e) {
            System.err.println("Couldn't insert the frame " + e);
            e.printStackTrace(System.err);
        }

        //getting the text object of Frame
        com.sun.star.text.XText xTextF = xTF.getText();

        //create a cursor object
        com.sun.star.text.XTextCursor xTCF = xTextF.createTextCursor();

        //inserting some Text
        xTextF.insertString(xTCF,
                 "The first line in the newly created text frame.", false);


        xTextF.insertString(xTCF,
                 "\nWith this second line the height of the frame raises.", false);

        //insert a paragraph break
        try {
            xText.insertControlCharacter(xTCursor,
                      com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK, false );

        } catch (Exception e) {
            System.err.println("Couldn't insert break "+ e);
            e.printStackTrace(System.err);
        }

        // Change the CharColor and add a Shadow
        try {
            xTCPS.setPropertyValue("CharColor",new Integer(65536));
            xTCPS.setPropertyValue("CharShadowed", new Boolean(false));
        } catch (Exception e) {
            System.err.println("Couldn't change the color " + e);
            e.printStackTrace(System.err);
        }

        xText.insertString(xTCursor, " That's all for now !!", false );

        System.out.println("done");

        System.exit(0);
    }


    public static com.sun.star.text.XTextDocument openWriter(
        com.sun.star.uno.XComponentContext xContext)
    {
        //define variables
        com.sun.star.frame.XComponentLoader xCLoader;
        com.sun.star.text.XTextDocument xDoc = null;
        com.sun.star.lang.XComponent xComp = null;

        try {
            // get the remote office service manager
            com.sun.star.lang.XMultiComponentFactory xMCF =
                xContext.getServiceManager();

            Object oDesktop = xMCF.createInstanceWithContext(
                                        "com.sun.star.frame.Desktop", xContext);

            xCLoader = (com.sun.star.frame.XComponentLoader)
                UnoRuntime.queryInterface(com.sun.star.frame.XComponentLoader.class,
                                          oDesktop);
            com.sun.star.beans.PropertyValue [] szEmptyArgs =
                new com.sun.star.beans.PropertyValue [0];
            String strDoc = "private:factory/swriter";
            xComp = xCLoader.loadComponentFromURL(strDoc, "_blank", 0, szEmptyArgs);
            xDoc = (com.sun.star.text.XTextDocument)
                UnoRuntime.queryInterface(com.sun.star.text.XTextDocument.class,
                                          xComp);

        } catch(Exception e){
            System.err.println(" Exception " + e);
            e.printStackTrace(System.err);
        }
        return xDoc;
    }

    public static void insertIntoCell(String CellName, String theText,
                                      com.sun.star.text.XTextTable xTTbl) {

        com.sun.star.text.XText xTableText = (com.sun.star.text.XText)
            UnoRuntime.queryInterface(com.sun.star.text.XText.class,
                                      xTTbl.getCellByName(CellName));

        //create a cursor object
        com.sun.star.text.XTextCursor xTC = xTableText.createTextCursor();

        com.sun.star.beans.XPropertySet xTPS = (com.sun.star.beans.XPropertySet)
            UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xTC);

        try {
            xTPS.setPropertyValue("CharColor",new Integer(16777215));
        } catch (Exception e) {
            System.err.println(" Exception " + e);
            e.printStackTrace(System.err);
        }

        //inserting some Text
        xTableText.setString( theText );

    }

}
