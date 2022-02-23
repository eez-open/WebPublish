/************************************************************************
 *
 *  Application.java
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
 *  Version 1.0 (2008-11-22) 
 *
 */
 
package writer2latex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import writer2latex.api.BatchConverter;
import writer2latex.api.Converter;
import writer2latex.api.ConverterFactory;
import writer2latex.api.ConverterResult;
import writer2latex.api.MIMETypes;
//import writer2latex.api.OutputFile;

import writer2latex.util.Misc;

/**
 * <p>Commandline utility to convert an OpenOffice.org Writer XML file into XHTML/LaTeX/BibTeX</p>
 * <p>The utility is invoked with the following command line:</p>
 * <pre>java -jar writer2latex.jar [options] source [target]</pre>
 * <p>Where the available options are
 * <ul>
 * <li><code>-latex</code>, <code>-bibtex</code>, <code>-xhtml</code>,
       <code>-xhtml+mathml</code>, <code>-xhtml+mathml+xsl</code>
 * <li><code>-recurse</code>
 * <li><code>-ultraclean</code>, <code>-clean</code>, <code>-pdfscreen</code>,
 * <code>-pdfprint</code>, <code>-cleanxhtml</code>
 * <li><code>-config[=]filename</code>
 * <li><code>-template[=]filename</code>
 * <li><code>-option[=]value</code>
 * </ul>
 * <p>where <code>option</code> can be any simple option known to Writer2LaTeX
 * (see documentation for the configuration file).</p>
 */
public final class Application {
	
    /* Based on command-line parameters. */
    private String sTargetMIME = MIMETypes.LATEX;
    private boolean bRecurse = false;
    private Vector configFileNames = new Vector();
    private String sTemplateFileName = null;
    private Hashtable options = new Hashtable();
    private String sSource = null;
    private String sTarget = null;

    /**
     *  Main method
     *
     *  @param  args  The argument passed on the command line.
     */
    public static final void main (String[] args){
        try {
            Application app = new Application();
            app.parseCommandLine(args);
            app.doConversion();
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            showUsage(msg);
        }
    }
	
    // Convert the directory or file
    private void doConversion() {
        // Step 1: Say hello...
        String sOutputFormat;
        if (MIMETypes.LATEX.equals(sTargetMIME)) { sOutputFormat = "LaTeX"; }
        else if (MIMETypes.BIBTEX.equals(sTargetMIME)) { sOutputFormat = "BibTeX"; }
        else { sOutputFormat = "xhtml"; }
        System.out.println();
        System.out.println("This is Writer2" + sOutputFormat + 
                           ", Version " + ConverterFactory.getVersion() + 
                           " (" + ConverterFactory.getDate() + ")");
        System.out.println();
        System.out.println("Starting conversion...");
		
        // Step 2: Examine source
        File source = new File(sSource);
        if (!source.exists()) {
            System.out.println("I'm sorry, I can't find "+sSource);
            System.exit(1);
        }
        if (!source.canRead()) {
            System.out.println("I'm sorry, I can't read "+sSource);
            System.exit(1);
        }
        boolean bBatch = source.isDirectory();

        // Step 3: Examine target
        File target;
        if (bBatch) {
            if (sTarget==null) {
                target=source;
            }
            else {
                target = new File(sTarget);
            }
        }
        else {
            if (sTarget==null) {
                target = new File(source.getParent(),Misc.removeExtension(source.getName()));
            }
            else {
                target = new File(sTarget);
                if (sTarget.endsWith(File.separator)) {
                    target = new File(target,Misc.removeExtension(source.getName()));
                }
            }
        }
		
        // Step 4: Create converters
        Converter converter = ConverterFactory.createConverter(sTargetMIME);
        if (converter==null) {
            System.out.println("Failed to create converter for "+sTargetMIME);
            System.exit(1);
        }
		
        BatchConverter batchCv = null;
        if (bBatch) {
            batchCv = ConverterFactory.createBatchConverter(MIMETypes.XHTML);
            if (batchCv==null) {
                System.out.println("Failed to create batch converter");
                System.exit(1);
            }
            batchCv.setConverter(converter);
        }
		
        // Step 5: Read template
        if (sTemplateFileName!=null) {
            try {
                System.out.println("Reading template "+sTemplateFileName);
                byte [] templateBytes = Misc.inputStreamToByteArray(new FileInputStream(sTemplateFileName));
                converter.readTemplate(new ByteArrayInputStream(templateBytes));
                if (batchCv!=null) {
                    // Currently we use the same template for the directory and the files
                    batchCv.readTemplate(new ByteArrayInputStream(templateBytes));
                }
            }
            catch (FileNotFoundException e) {
                System.out.println("--> This file does not exist!");
                System.out.println("    "+e.getMessage());
            }
            catch (IOException e) {
                System.out.println("--> Failed to read the template file!");
                System.out.println("    "+e.getMessage());
            }
        }
		
        // Step 6: Read config
        for (int i=0; i<configFileNames.size(); i++) {
            String sConfigFileName = (String) configFileNames.get(i);
            if (sConfigFileName.startsWith("*")) {
                sConfigFileName = sConfigFileName.substring(1);
                System.out.println("Reading default configuration "+sConfigFileName);
                try {
                    converter.getConfig().readDefaultConfig(sConfigFileName);
                }
                catch (IllegalArgumentException e) {
                    System.err.println("--> This configuration is unknown!");
                    System.out.println("    "+e.getMessage());
                }
            }
            else {
                System.out.println("Reading configuration file "+sConfigFileName);
                try {
                    byte[] configBytes = Misc.inputStreamToByteArray(new FileInputStream(sConfigFileName));
                    converter.getConfig().read(new ByteArrayInputStream(configBytes));
                    if (bBatch) {
                        // Currently we use the same configuration for the directory and the files
                        batchCv.getConfig().read(new ByteArrayInputStream(configBytes));
                    }
                }
                catch (IOException e) {
                    System.err.println("--> Failed to read the configuration!");
                    System.out.println("    "+e.getMessage());
                }
            }
        }
		
        // Step 7: Set options from command line
        Enumeration keys = options.keys();
        while (keys.hasMoreElements()) {
            String sKey = (String) keys.nextElement();
            String sValue = (String) options.get(sKey);
            converter.getConfig().setOption(sKey,sValue);
            if (batchCv!=null) {
                batchCv.getConfig().setOption(sKey,sValue);
            }
        }
	 	
        // Step 8: Perform conversion
        if (bBatch) {
            batchCv.convert(source,target,bRecurse, new BatchHandlerImpl());
        }
        else {
            System.out.println("Converting "+source.getPath());
            ConverterResult dataOut = null;

            try {
                dataOut = converter.convert(source,target.getName());
            }
            catch (FileNotFoundException e) {
                System.out.println("--> The file "+source.getPath()+" does not exist!");
                System.out.println("    "+e.getMessage());
                System.exit(1);
            }
            catch (IOException e) {
                System.out.println("--> Failed to convert the file "+source.getPath()+"!");
                System.out.println("    "+e.getMessage());
                System.exit(1);
            }

            // TODO: Should do some further checking on the feasability of writing
            // the directory and the files.
            File targetDir = target.getParentFile();
            if (targetDir!=null && !targetDir.exists()) { targetDir.mkdirs(); }
            try {
                dataOut.write(targetDir);
            }
            catch (IOException e) {
                System.out.println("--> Error writing out file!");
                System.out.println("    "+e.getMessage());
                System.exit(1);
            }
        
        }
		
        // Step 9: Say goodbye!
        System.out.println("Done!");
    }


    /**
     *  Display usage.
     */
    private static void showUsage(String msg) {
        System.out.println();
        System.out.println("This is Writer2LaTeX, Version " + ConverterFactory.getVersion() 
                           + " (" + ConverterFactory.getDate() + ")");
        System.out.println();
        if (msg != null) System.out.println(msg);
        System.out.println();
        System.out.println("Usage:");
        System.out.println("   java -jar <path>/writer2latex.jar <options> <source file/directory> [<target file/directory>]");
        System.out.println("where the available options are:");
        System.out.println("   -latex");
        System.out.println("   -bibtex");
        System.out.println("   -xhtml");
        System.out.println("   -xhtml+mathml");
        System.out.println("   -xhtml+mathml+xsl");
        System.out.println("   -recurse");
        System.out.println("   -template[=]<template file>");
        System.out.println("   -ultraclean");
        System.out.println("   -clean");
        System.out.println("   -pdfprint");
        System.out.println("   -pdfscreen");
        System.out.println("   -cleanxhtml");
        System.out.println("   -config[=]<configuration file>");
        System.out.println("   -<configuration option>[=]<value>");
        System.out.println("See the documentation for the available configuration options");
    }

    /**
     *  Parse command-line arguments.
     *
     *  @param  args  Array of command line arguments.
     *
     *  @throws  IllegalArgumentException  If an argument is invalid.
     */
    private void parseCommandLine(String sArgs[])
        throws IllegalArgumentException {

        int i = 0;
		
        while (i<sArgs.length) {
            String sArg = getArg(i++,sArgs);
            if (sArg.startsWith("-")) { // found an option
                if ("-latex".equals(sArg)) { sTargetMIME = MIMETypes.LATEX; }
                else if ("-bibtex".equals(sArg)) { sTargetMIME = MIMETypes.BIBTEX; }
                else if ("-xhtml".equals(sArg)) { sTargetMIME = MIMETypes.XHTML; }
                else if ("-xhtml+mathml".equals(sArg)) { sTargetMIME = MIMETypes.XHTML_MATHML; }
                else if ("-xhtml+mathml+xsl".equals(sArg)) { sTargetMIME = MIMETypes.XHTML_MATHML_XSL; }
                else if ("-recurse".equals(sArg)) { bRecurse = true; }
                else if ("-ultraclean".equals(sArg)) { configFileNames.add("*ultraclean.xml"); }
                else if ("-clean".equals(sArg)) { configFileNames.add("*clean.xml"); }
                else if ("-pdfprint".equals(sArg)) { configFileNames.add("*pdfprint.xml"); }
                else if ("-pdfscreen".equals(sArg)) { configFileNames.add("*pdfscreen.xml"); }
                else if ("-cleanxhtml".equals(sArg)) { configFileNames.add("*cleanxhtml.xml"); }
                else { // option with argument
                    int j=sArg.indexOf("=");
                    String sArg2;
                    if (j>-1) { // argument is separated by =
                        sArg2 = sArg.substring(j+1);
                        sArg = sArg.substring(0,j);
                    }
                    else { // argument is separated by space
                        sArg2 = getArg(i++,sArgs);
                    }
                    if ("-config".equals(sArg)) { configFileNames.add(sArg2); }
                    else if ("-template".equals(sArg)) { sTemplateFileName = sArg2; }
                    else { // configuration option
                        options.put(sArg.substring(1),sArg2);
                    }
                }
            }
            else { // not an option, so this must be the source
                sSource = sArg;
                // Possibly followed by the target
                if (i<sArgs.length) {
                    String sArgument = getArg(i++,sArgs); 
                    if (sArgument.length()>0) { sTarget = sArgument; }
                }
                // Skip any trailing empty arguments and signal an error if there's more
                while (i<sArgs.length) {
                    String sArgument = getArg(i++,sArgs);
                    if (sArgument.length()>0) {
                        throw new IllegalArgumentException("I didn't expect "+sArgument+"?");
                    }
                }
            }
        }
        if (sSource==null) {
            throw new IllegalArgumentException("Please specify a source document/directory!");
        }
        // Parsing of command line ended successfully!
    }


    /**
     *  Extract the next argument from the array, while checking to see
     *  that the array size is not exceeded.  Throw a friendly error
     *  message in case the arg is missing.
     *
     *  @param  i     Argument index.
     *  @param  args  Array of command line arguments.
     *
     *  @return  The argument with the specified index.
     *
     *  @throws  IllegalArgumentException  If an argument is invalid.
     */
    private String getArg(int i, String args[])
        throws IllegalArgumentException {

        if (i < args.length) {
            return args[i];
        }
        else throw new
            IllegalArgumentException("I'm sorry, the commandline ended abnormally");
    }
	

}
