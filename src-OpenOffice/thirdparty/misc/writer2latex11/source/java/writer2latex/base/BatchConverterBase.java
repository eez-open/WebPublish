/************************************************************************
 *
 *  BatchConverterBase.java
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
 *  Version 1.0 (2008-10-15)
 *
 */
 
package writer2latex.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import writer2latex.api.BatchConverter;
import writer2latex.api.BatchHandler;
import writer2latex.api.Converter;
import writer2latex.api.ConverterResult;
import writer2latex.api.IndexPageEntry;
import writer2latex.api.OutputFile;

import writer2latex.util.Misc;

/**
 * Abstract base implementation of <code>writer2latex.api.BatchConverter</code>.
 * The base implementation handles the traversal of directories and files, and
 * leaves the handling of indexpages to the subclass.
 */
public abstract class BatchConverterBase implements BatchConverter {
	
    private Converter converter;

    public BatchConverterBase() {
        converter = null;
    }
	
    // Partial implementation of the interface

    public void setConverter(Converter converter) {
        this.converter = converter;
    }
	
    public void convert(File source, File target, boolean bRecurse, BatchHandler handler) {
        handler.startConversion();
        convertDirectory(source, target, bRecurse, source.getName(), handler);
        handler.endConversion();
    }
	
    protected abstract String getIndexFileName();
	
    // Convert files and directories in the directory indir
    // (return false if conversion has been cancelled by the BatchHandler)
    private boolean convertDirectory(File indir, File outdir, boolean bRecurse, String sHeading, BatchHandler handler) {
        handler.startDirectory(indir.getPath());

        // Step 1: Get the directory
        File[] contents = indir.listFiles();
        int nLen = contents.length;
        IndexPageEntry[] entries = new IndexPageEntry[nLen];
		
        // Step 2: Traverse subdirectories, if allowed
        if (bRecurse) {
            String sUplink = getConfig().getOption("uplink");
            for (int i=0; i<nLen; i++) {
                if (contents[i].isDirectory()) {
                    getConfig().setOption("uplink","../"+getIndexFileName());
                    File newOutdir = new File(outdir,contents[i].getName());
                    String sNewHeading = sHeading + " - " + contents[i].getName();
                    boolean bResult = convertDirectory(contents[i],newOutdir,bRecurse,sNewHeading,handler);
                    getConfig().setOption("uplink", sUplink);
                    if (!bResult) { return false; }
                    // Create entry for this subdirectory
                    IndexPageEntry entry = new IndexPageEntry(Misc.makeHref(contents[i].getName()+"/"+getIndexFileName()),true);
                    entry.setDisplayName(contents[i].getName());
                    entries[i]=entry;
                }
            }
        }

        // Step 3: Traverse documents, if we have a converter
        if (converter!=null) {
            String sUplink = getConfig().getOption("uplink");
            for (int i=0; i<nLen; i++) {
                if (contents[i].isFile()) {
                    getConfig().setOption("uplink",getIndexFileName());
                    String sLinkFile = convertFile(contents[i],outdir,handler);
                    getConfig().setOption("uplink", sUplink);
                    if (sLinkFile!=null) {
                        // Create entry for this file
                        IndexPageEntry entry = new IndexPageEntry(Misc.makeHref(sLinkFile),false);
                        entry.setDisplayName(Misc.removeExtension(sLinkFile));
                        entries[i]=entry;
                        if (handler.cancel()) { return false; }
                    }
                }
            }
        }

        // Step 4: Create and write out the index file
        OutputFile indexFile = createIndexFile(sHeading, entries);

        if (!outdir.exists()) { outdir.mkdirs(); }
        
        boolean bSuccess = true;
        File outfile = new File(outdir,indexFile.getFileName());
        try {
            FileOutputStream fos = new FileOutputStream(outfile);
            indexFile.write(fos);
            fos.flush();
            fos.close();
        } catch (Exception writeExcept) {
            bSuccess = false;
        }
		
        handler.endDirectory(indir.getPath(), bSuccess);

        return !handler.cancel();
    }
	
    // Convert a single file, returning the name of the master file
    // Returns null if conversion fails
    private String convertFile(File infile, File outdir, BatchHandler handler) {
        handler.startFile(infile.getPath());

        // Currently we discriminate based on file extension
        if (!(infile.getName().endsWith(".odt") || infile.getName().endsWith(".ods") || infile.getName().endsWith(".odp"))) {
            handler.endFile(infile.getPath(),false);
            return null;
        }
		
        // Do conversion
        ConverterResult dataOut = null;
        try {
            // The target file name is always the same as the source
            dataOut = converter.convert(infile,Misc.removeExtension(infile.getName()));
        }
        catch (FileNotFoundException e) {
            handler.endFile(infile.getPath(),false);
            return null;
        }
        catch (IOException e) {
            handler.endFile(infile.getPath(),false);
            return null;
        }

        // Write out files
        if (!outdir.exists()) { outdir.mkdirs(); }
        
        try {
            dataOut.write(outdir);
        }
        catch (IOException e) {
            handler.endFile(infile.getPath(),false);
            return null;
        }
        
        handler.endFile(infile.getPath(),true);

        return dataOut.getMasterDocument().getFileName();
    }
	

}
