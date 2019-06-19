
set INPUT=%2
set OUTPUT=%1
set FILE_ASSOCIATIONS=%4
set VERSION=%3
set APP_ICON=%5

set MODULE_PATH=%JAVA_HOME%\jmods
call "%JAVA_HOME%\bin\jlink.exe" ^
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported ^
  --module-path "%MODULE_PATH%" ^
  --no-header-files --no-man-pages ^
  --output "%OUTPUT%\runtime" ^
  --strip-debug ^
  --compress=2


java --module-path "%JAVA_HOME%\bin" ^
  --add-opens jdk.jlink/jdk.tools.jlink.internal.packager=jdk.packager ^
  -m jdk.packager/jdk.packager.Main ^
  create-installer ^
    --verbose ^
    --echo-mode ^
    --name GanttProject ^
    --output "%OUTPUT%/dist" ^
    --input "%INPUT%" ^
    --main-jar eclipsito.jar ^
    --class com.bardsoftware.eclipsito.Launch ^
    --version %VERSION% ^
    --arguments "--verbosity 4 --version-dirs . --app net.sourceforge.ganttproject.GanttProject"  ^
    --runtime-image "%OUTPUT%\runtime" ^
    --identifier biz.ganttproject ^
    --description "Free desktop project scheduling and project management application" ^
    --category "Office" ^
    --copyright "Copyright 2019 BarD Software s.r.o" ^
    --vendor "BarD Software s.r.o" ^
    --license-file LICENSE ^
    --file-associations build-cfg\file-associations.properties ^
    --icon build-cfg\ganttproject.ico