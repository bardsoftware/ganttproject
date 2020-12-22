
set INPUT=%2
set OUTPUT=%1
set VERSION=%3
set JAVAFX_MODS_PATH=%4
jlink ^
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web ^
  --no-header-files --no-man-pages ^
  --output build\runtime ^
  --strip-debug ^
  --compress=2

dir "%OUTPUT%\runtime\bin"

jdk-14/bin/jpackage -h

jdk-14/bin/jpackage ^
    --input "%INPUT%" ^
    -d "%OUTPUT%"\dist ^
    --runtime-image "%OUTPUT%"\runtime ^
    --main-jar eclipsito.jar ^
    --main-class com.bardsoftware.eclipsito.Launch ^
    --arguments "--verbosity 4 --version-dirs app/plugins --app net.sourceforge.ganttproject.GanttProject"  ^
    --name GanttProject ^
    --app-version %VERSION% ^
    --type msi ^
    --description "Free desktop project scheduling and project management application" ^
    --copyright "Copyright 2020 BarD Software s.r.o" ^
    --vendor "BarD Software s.r.o" ^
    --license-file LICENSE ^
    --file-associations build-cfg\file-associations.properties ^
    --icon build-cfg\ganttproject.ico ^
    --verbose ^
    --win-dir-chooser --win-menu
