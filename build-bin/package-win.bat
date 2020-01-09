
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

build\runtime\bin\java -version
build\runtime\bin\java --list-modules

java --module-path "build-bin\win" ^
  --add-modules jdk.jlink ^
  --add-opens jdk.jlink/jdk.tools.jlink.internal.packager=jdk.packager ^
  -m jdk.packager/jdk.packager.Main ^
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
    --copyright "Copyright 2019 BarD Software s.r.o" ^
    --vendor "BarD Software s.r.o" ^
    --license-file LICENSE ^
    --file-associations build-cfg\file-associations.properties ^
    --icon build-cfg\ganttproject.ico ^
    --win-dir-chooser --win-menu
