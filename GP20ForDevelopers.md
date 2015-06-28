
---

> This page is **deprecated**. Please refer to BuildingGanttProject page for GanttProject 2.5 build instructions

---


# How to checkout and build GanttProject 2.0.X #

This page explains how to checkout the source code of GanttProject 2.0.X (as of Jun 2010, X=10), or GanttProject Praha, build it using command line tools and Eclipse and run/debug it from Eclipse.

## Prerequisites ##
A bare minimum that you need to is Sun's [Java SE Development Kit](http://java.sun.com/javase/) (JDK) >=1.5, [Mercurial](http://mercurial.selenic.com/) command line client (`hg` command) and [ANT](http://ant.apache.org/) built tool.

Please make sure that you can run `java`, `javac`, `hg` and `ant` commands.

## Checking out the sources ##

GanttProject 2.0.X source code is stored in [Mercurial repository](http://code.google.com/p/ganttproject/source/browse/) on [Google Code hosting](http://code.google.com/hosting). You can checkout the latest sources of GP 2.0.X anonymously with the following command line:

```
hg clone https://ganttproject.googlecode.com/hg/ ganttproject-2.0
```

The rest of this page assumes that you execute this command in `/tmp` directory and thus the sources reside in `/tmp/ganttproject-2.0` directory.

## Choosing the right branch ##

We use this Mercurial repository for keeping the sources of both GP 2.0.X and GP Praha. Build instructions are essentially the same, and what you build depends on the Mercurial branch. Branch 'default' is used for GP Praha, branch 'branch\_2\_0\_X' is used for GP 2.0.X.

## Building with ANT ##

If everything is OK with your environment then the following will build a binary distribution of GanttProject in `dist-bin` directory:

```
cd /tmp/ganttproject-2.0/ganttproject-builder
ant
```

Now you may run GanttProject using `ganttproject.sh` or `ganttproject.bat` script:

```
cd dist-bin
sh ganttproject.sh
```

Basically, that's everything that you need to be able to change the sources using any text editor, build and run the changed code.

## Editing, compiling and running in Eclipse ##

The following instructions were tested with Eclipse 3.6.1, Sun's Java 6 SE and Ubuntu 8.10. However, they should be valid for many other versions of Eclipse, JDK and OSes.

You may use the same instructions to build sources extracted from our [Source code ZIP distribution](http://code.google.com/p/ganttproject/downloads/list?can=2&q=Source+Code)

### Import projects into Eclipse and build ###
  * Start Eclipse and when _Workspace Launcher_ asks you to select a workspace, select `/tmp/ganttproject-2.0`
  * Eclipse shows you a _Welcome_ pane. Click _File->Import..._ menu item and select _General->Existing Projects into Workspace_ in a dialog. Press _Next_
  * On the second page, type `/tmp/ganttproject-2.0` in _Select root directory_ field. _Projects_ list should be filled with a few projects (5 for GP 2.0.X and 6 for GP Praha). Press _Finish_ and close the _Welcome_ pane
  * Eclipse will start building the sources and will probably report 2 errors in `GanttCalendar` class. In this case you have to tell the compiler to use Java 1.4 syntax. Click _Window->Preferences_ menu item and select _Java->Compiler_ item in the preferences tree. Select "1.4" in _Compiler compliance level_ drop down, uncheck _Use default compliance settings_ and select "1.4" in both _compatibility_ drop downs. Press OK and do a full rebuild.
  * Now you can edit the sources and Eclipse will build them automatically (make sure that _Project->Build Automatically_ is checked)

### Run and Debug GanttProject ###
  * Click _Run->Run Configurations_ menu item
  * Right-click _Eclipse Application_ in the list and click _New_.

Eclipse creates a new Run Configuration and shows its settings.

  * Switch radio button in _Program to Run_ group, select _Run an application_ and choose _net.sourceforge.ganttproject.GanttProject_ in a drop down.
  * You may want to give some meaningful name to this run configuration (say, _GanttProject_).
  * Apply and Run.

It is expected that GanttProject starts, shows a splash screen and operates normally. Next time you may just press `Ctrl+F11` to start GP again or click _Run_ button in the toolbar. You may also start debug code with this configuration.