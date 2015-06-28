# How to checkout and build GanttProject from the source code #

This page explains how to checkout the source code of GanttProject 2.7 Ostrava, 2.6 Brno or 2.5 Praha, build it using command line tools and Eclipse and run/debug it from Eclipse. This text assumes that you're using Linux. The process on Windows/Mac OSX is virtually the same, modulo differences in paths and the way command line terminal works.

## Prerequisites ##
A bare minimum which you need is:
  * [Java SE Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (JDK) from Oracle or [OpenJDK](http://openjdk.java.net/). Java 7 or Java 8 are okay, although earlier versions can be compiled with Java 5 and Java 6.
  * [ANT](http://ant.apache.org/) build tool.
  * If you want to checkout the latest bleeding edge sources, you'll also need Mercurial or Git command line client. However, if you're going to hack the source code of one of the stable releases, it is enough to be able to unzip an archive.

Please make sure that you can run `java`, `javac`, `hg` (or `git`) and `ant` commands.

## Checking out the sources ##

The whole source code is stored in [Mercurial repository](http://code.google.com/p/ganttproject/source/browse/) on [Google Code hosting](http://code.google.com/hosting). There is a mirror on Github, choose what you like more. You can checkout the repository with one of the following command lines:

```
hg clone https://code.google.com/p/ganttproject/
```

```
git clone https://github.com/bardsoftware/ganttproject.git
```

We also publish self-sufficient archives with the sources of the stable versions. The latest published is GanttProject 2.6.1: https://code.google.com/p/ganttproject/downloads/detail?name=ganttproject-2.6.1-r1499-src.zip Instructions below apply to code extracted from source archives as well.

The rest of this page assumes that you checked out the sources using one of the ways into `/tmp/ganttproject` directory.

## Branches ##
GanttProject 2.7 code sits in the default (aka master) branch.

GanttProject 2.6 code sits in a branch named BRANCH\_2\_6\_X. Having cloned the repository, you can switch to that branch with `hg up BRANCH_2_6_X` or `git checkout BRANCH_2_6_X`

GanttProject 2.5 code sits in a branch named BRANCH\_2\_5\_X. Having cloned the repository, you can switch to that branch with `hg up BRANCH_2_5_X` or `git checkout BRANCH_2_5_X`

## Building with ANT ##

If everything is OK with your environment then the following will build a binary distribution of GanttProject in `dist-bin` directory:

```
cd /tmp/ganttproject/ganttproject-builder
ant
```

Now you can run GanttProject using `ganttproject` or `ganttproject.bat` script:

```
cd dist-bin
./ganttproject
```

Basically, that's everything that you need to be able to change the sources using any text editor, build and run the changed code.

## Editing, compiling and running in Eclipse ##

**Eclipse is not required** for working on GanttProject code. However, it is arguably the most convenient IDE for this purpose.

The following instructions were tested with Eclipse Kepler Standard, OpenJDK 7 and Ubuntu 14.04. However, they should be valid for many other versions of Eclipse, JDK and OSes.


### Import projects into Eclipse and build ###

  * Start Eclipse and when _Workspace Launcher_ asks you to select a workspace, select `/tmp/ganttproject`
  * Eclipse shows you a _Welcome_ pane. Click _File->Import..._ menu item and select _General->Existing Projects into Workspace_ in a dialog. Press _Next_
  * On the second page, type `/tmp/ganttproject` in _Select root directory_ field. _Projects_ list should be filled with a few projects. Press _Finish_ and close the _Welcome_ pane
  * Eclipse will start building the sources and will probably report "API Baseline problem" in plugin "msproject2". The simplest is to convert this error to warning. In Eclipse _Preferences_ dialog find _API Baselines_ node and Choose _"Warning"_ next to _"Missing API Baseline"_ in the _Options_ group

  * Now you can edit the sources and Eclipse will build them automatically (make sure that _Project->Build Automatically_ is checked)

### Run and Debug GanttProject ###
  * Click _Run->Run Configurations_ menu item
  * Right-click _Eclipse Application_ in the list and click _New_.

Eclipse creates a new Run Configuration and shows its settings.

  * Switch radio button in _Program to Run_ group, select _Run an application_ and choose _net.sourceforge.ganttproject.GanttProject_ in a drop down.
  * You may want to give some meaningful name to this run configuration (say, _GanttProject_).
  * Apply and Run.

It is expected that GanttProject starts, shows a splash screen and operates normally. Next time you may just press `Ctrl+F11` to start GP again or click _Run_ button in the toolbar. You may also start debug code with this configuration.