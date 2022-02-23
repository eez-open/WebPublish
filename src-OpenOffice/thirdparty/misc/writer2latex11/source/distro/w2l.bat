@echo off
rem *Very* simple batch file to run Writer2LaTeX
rem Created by Henrik Just, october 2003
rem Modfied december 2003 as suggested by Juan Julian Merelo Guervos
rem Last modified july 2007

rem Please edit the following line to contain the full path to Writer2LaTeX:

set W2LPATH="c:\writer2latex10"

rem If the Java executable is not in your path, please edit the following
rem line to contain the full path and file name

set JAVAEXE="java"

%JAVAEXE% -jar %W2LPATH%\writer2latex.jar %1 %2 %3 %4 %5 %6 %7 %8 %9

set W2LPATH=
set JAVAEXE=

