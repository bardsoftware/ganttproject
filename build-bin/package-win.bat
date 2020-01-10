
set INPUT=%2
set OUTPUT=%1
set VERSION=%3
set JAVAFX_MODS_PATH=%4
jlink ^
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web ^
  --no-header-files --no-man-pages ^
  --output "%OUTPUT%\runtime" ^
  --strip-debug ^
  --compress=2

dir "%OUTPUT%\runtime\bin"

dir jdk-14\bin
jdk-14\bin\jpackage -h

jdk-14\bin\jpackage ^
  create-installer ^
    --verbose ^
    --echo-mode ^
    --name GanttProject ^
    --output "%OUTPUT%\dist" ^
    --input "%INPUT%" ^
    --main-jar eclipsito.jar ^
    --class com.bardsoftware.eclipsito.Launch ^
    --version %VERSION% ^
    --arguments "--verbosity 4 --version-dirs plugins --app net.sourceforge.ganttproject.GanttProject"  ^
    --runtime-image "%OUTPUT%\runtime" ^
    --identifier biz.ganttproject ^
    --description "Free desktop project scheduling and project management application" ^
    --category "Office" ^
    --copyright "Copyright 2020 BarD Software s.r.o" ^
    --vendor "BarD Software s.r.o" ^
    --license-file LICENSE ^
    --file-associations build-cfg\file-associations.properties ^
    --icon build-cfg\ganttproject.ico ^
    --win-dir-chooser --win-menu
