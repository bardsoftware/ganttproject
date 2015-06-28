# Installing GanttProject from platform-independent ZIP archive #

## Why ##
There are a few cases when you may want to install GanttProject from ZIP archive:
  * there is no installer or other native package for your operating system (e.g. you are on OpenSolaris)
  * launcher from the native installer doesn't work (e.g. nothing happens when you click GanttProject icon in your Programs menu on Windows)
  * you want to do some other debugging stuff, e.g. see messages printed to the console
  * you want to keep a few different versions of GanttProject
  * you want to modify GanttProject startup parameters, e.g. use some other JRE or give more memory to the Java VM

## How ##
In the most siple case installing is very easy: unzip _ganttproject.zip_ file to any directory on your disk drive, and run a launcher script from the installation directory. The script you need to run is _ganttproject.bat_ on Windows, _ganttproject.command_ on MacOSX and _ganttproject.sh_ on Linux/UNIX systems. If your environment meets prerequisites, it is very likely that you will see GanttProject starting and running.

## What if it doesn't work ##
### Prerequisites ###
**Hard prerequisite** is having Java Runtime Environment installed. We recommend using the latest JRE from Sun Microsystems. GanttProject may or may not run on Java from other vendors, and it **won't run on GIJ/GCJ** Java Runtime which comes by default in some Linux distributions.

**Soft prerequisite** is ability to run _java_ executable from console. On Windows console application is called _Command line prompt_, on MacOSX it is called _Terminal_. Linux guys hopefully know how to open console (okay, it is called _Terminal_ on GNOME desktops or _Konsole_ in KDE). Open console, type _java -version_ and press enter. If you see something like

```
$ java -version
java version "1.6.0_10"
Java(TM) SE Runtime Environment (build 1.6.0_10-b33)
Java HotSpot(TM) Client VM (build 11.0-b15, mixed mode, sharing)
```

then you're OK. If you see a message which says something similar to "java not found", bad luck. You may try reinstalling Java, but if for some reasons you can't or don't want to do it, please read the next section

### Setting `JAVA_HOME` environment variable ###
You need to do it if you're absolutely sure that JRE is installed, but you can't run it from console or if you want to use some other non-default JRE. The following text assumes that your system is Windows and GanttProject is installed in _C:\GanttProject_

Your task it to point a launcher script to the location of the JRE on your system. First, find that location :) If you have absolutely no idea how to do it, search for "java.exe" file. JRE is usually installed in a directory with the following structure:
```
<lots of files>
bin
  |
  +-- <lots of files>
  +-- java.exe
lib   
  |
  +-- <lots of files>
```

This directory is your Java home. Copy the full path to that directory (e.g. _C:\Program Files\Java\jre6_), open _C:\GanttProject\ganttproject.bat_ in your favorite text editor and add the following at the first line:
```
SET JAVA_HOME="C:\Path\To\Your\jre"
```

replacing _C:\Path\To\Your\jre_ with the actual location of your Java Runtime.