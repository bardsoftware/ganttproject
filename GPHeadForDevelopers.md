# How to checkout and build GanttProject Loire #

> 
---

> Please note that Loire is now orphaned. We're working on [GanttProject Praha](http://ganttproject.blogspot.com/2010/11/announcing-ganttproject-praha.html). Its code lives in Mercurial repository on Google Code Hosting and instructions for checking out are slightly different. Find them on [on this wiki page](GP20ForDevelopers.md)
> 
---


_Loire_ is a code name of the future major GanttProject release. This page explains how to checkout the source code of Loire, build it using command line tools and Eclipse and run/debug it from Eclipse.


## Prerequisites ##
A bare minimum that you need to is Sun's [Java SE Development Kit](http://java.sun.com/javase/) (JDK) >=1.5.X, CVS command line client and [ANT](http://ant.apache.org/) built tool.

Please make sure that you can run `java`, `javac`, `cvs` and `ant` commands.

## Checking out the sources ##

GanttProject Loire source code is stored in [CVS repository](.md) on [SourceForge hosting](http://sourceforge.net). You can checkout the latest sources of Loire anonymously with the following commands:

```
cvs -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject login
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P ganttproject
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P ganttproject-builder
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P com.jgoodies
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P org.ganttproject.chart.pert
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P org.ganttproject.core.test
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P org.ganttproject.impex.htmlpdf
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P org.ganttproject.impex.msproject
cvs -z3 -d:pserver:anonymous@ganttproject.cvs.sourceforge.net:/cvsroot/ganttproject co -P org.jdesktop
```

These commands will checkout a few CVS modules into your current directory. The rest of this page assumes that you execute them in `/tmp/ganttproject` directory. Alternatively, if you're using Eclipse, you may import a Team Project Set file and it will do checkout automatically. Please read the instructions at the end of this page.

## Building with ANT ##

If everything is OK with your environment then the following will build a binary distribution of GanttProject in `dist-bin` directory:

```
cd /tmp/ganttproject/ganttproject-builder
ant
```

Now you may run GanttProject using `ganttproject.sh` or `ganttproject.bat` script:

```
cd dist-bin
sh ganttproject.sh
```

Basically, that's everything that you need to be able to change the sources using any text editor, build and run the changed code.

## Editing, compiling and running using Eclipse ##

The following instructions were tested with Eclipse 3.4, Sun's Java 6 SE and Ubuntu 8.10. However, they should be valid for many other versions of Eclipse, JDK and OSes.

You may use the same instructions to build sources extracted from our [Source code ZIP distribution](http://code.google.com/p/ganttproject/downloads/list?can=2&q=Source+Code)

### Import projects into Eclipse and build ###
  * Start Eclipse and when _Workspace Launcher_ asks you to select a workspace, select `/tmp/ganttproject`
  * Eclipse shows you a _Welcome_ pane. Click _File->Import..._ menu item and select _General->Existing Projects into Workspace_ in a dialog. Press _Next_
  * On the second page, type `/tmp/ganttproject` in _Select root directory_ field. _Projects_ list should be filled with 8 projects. Press _Finish_ and close the _Welcome_ pane
  * Eclipse will start building the sources.
  * Now you can edit the sources and Eclipse will build them automatically (make sure that _Project->Build Automatically_ is checked)

### Run and Debug GanttProject ###
  * Click _Run->Run Configurations_ menu item
  * Right-click _Eclipse Application_ in the list and click _New_.

Eclipse creates a new Run Configuration and shows its settings.

  * Switch radio button in _Program to Run_ group, select _Run an application_ and choose _net.sourceforge.ganttproject.GanttProject_ in a drop down.
  * You may want to give some meaningful name to this run configuration (say, _GanttProject Loire_).
  * Apply and Run.

It is expected that GanttProject starts, shows a splash screen and operates normally. Next time you may just press `Ctrl+F11` to start GP again or click _Run_ button in the toolbar. You may also start debug code with this configuration.

### Using Team Project Set file ###
Eclipse users can use a .psf file which automates checkout step.

  * Download the file from here: http://ganttproject.cvs.sourceforge.net/viewvc/*checkout*/ganttproject/ganttproject-builder/ganttproject.psf and save it to, say `/tmp/ganttproject.psf`
  * Start Eclipse and when _Workspace Launcher_ asks you to select a workspace, select `/tmp/ganttproject`
  * Eclipse shows you a _Welcome_ pane. Click _File->Import..._ menu item and select _Team->Team Project Set_ in a dialog. Press _Next_
  * Type `/tmp/ganttproject.psf` in _File name_ field or use _Browse_ button to locate the file. Press _Finish_.

Eclipse will checkout and build the sources for you. After that, you can run and debug as explained above.