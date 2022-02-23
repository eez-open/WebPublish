This directory contains idl specifications for the custom uno interfaces
defined by the Writer2LaTeX and Writer2xhtml extensions 

To avoid dependencies on the OOo SDK in the build process, compiled versions
are included here.

If you need to rebuild it, the complete SDK is required. These are the steps: 

To create the registry database for Writer2LaTeX:

idlc -I<path to SDK>\idl XW2LStarMathConverter.idl
regmerge writer2latex.rdb /UCR XW2LStarMathConverter.urd

To create the java interface

javamaker -BUCR -Torg.openoffice.da.writer2latex.XW2LStarMathConverter -nD <path to the OOo installation>\program\types.rdb writer2latex.rdb

and likewise for Writer2xhtml:

idlc -I<path to SDK>\idl XBatchConverter.idl
regmerge writer2xhtml.rdb /UCR XBatchConverter.urd

To create the java interfaces

javamaker -BUCR -Torg.openoffice.da.writer2xhtml.XBatchConverter -nD <path to the OOo installation>\program\types.rdb writer2xhtml.rdb
javamaker -BUCR -Torg.openoffice.da.writer2xhtml.XBatchHandler -nD <path to the OOo installation>\program\types.rdb writer2xhtml.rdb


If you need to use the interfaces from C++ you will also need to run cppumaker

