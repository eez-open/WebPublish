/************************************************************************
 *
 *  ConverterBase.java
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

package writer2latex.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import writer2latex.api.GraphicConverter;
import writer2latex.api.Converter;
import writer2latex.api.ConverterResult;
import writer2latex.api.OutputFile;
import writer2latex.office.ImageLoader;
import writer2latex.office.MetaData;
import writer2latex.office.OfficeReader;
import writer2latex.xmerge.EmbeddedObject;
import writer2latex.xmerge.ConvertData;
import writer2latex.xmerge.OfficeDocument;

/**<p>Abstract base implementation of <code>writer2latex.api.Converter</code></p>
 */
public abstract class ConverterBase implements Converter {

    // Helper	
    protected GraphicConverter graphicConverter;

    // The source document
    protected OfficeDocument odDoc;
    protected OfficeReader ofr;
    protected MetaData metaData;
    protected ImageLoader imageLoader;

    // The output file(s)
    protected String sTargetFileName;
    protected ConvertData convertData;
		
    // Constructor
    public ConverterBase() {
        graphicConverter = null;
        convertData = new ConvertData();
    }
	
    // Implement the interface
    public void setGraphicConverter(GraphicConverter graphicConverter) {
        this.graphicConverter = graphicConverter;
    }
	
    // Provide a do noting fallback method
    public void readTemplate(InputStream is) throws IOException { }
	
    // Provide a do noting fallback method
    public void readTemplate(File file) throws IOException { }

    public ConverterResult convert(File source, String sTargetFileName) throws FileNotFoundException,IOException {
        return convert(new FileInputStream(source), sTargetFileName);
    }

    public ConverterResult convert(InputStream is, String sTargetFileName) throws IOException {
        // Read document
        odDoc = new OfficeDocument("InFile");
        odDoc.read(is);
        ofr = new OfficeReader(odDoc,false);
        metaData = new MetaData(odDoc);
        imageLoader = new ImageLoader(odDoc,sTargetFileName,true);
        imageLoader.setGraphicConverter(graphicConverter);

        // Prepare output
        this.sTargetFileName = sTargetFileName;
        convertData.reset();
		
        convertInner();
		
        return convertData;
    }
	
    // The subclass must provide the implementation
    public abstract void convertInner() throws IOException;

    public MetaData getMetaData() { return metaData; }
    
    public ImageLoader getImageLoader() { return imageLoader; }
	
    public void addDocument(OutputFile doc) { convertData.addDocument(doc); }
	
    public EmbeddedObject getEmbeddedObject(String sHref) {
        return odDoc.getEmbeddedObject(sHref);
    }



}