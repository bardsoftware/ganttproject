set MODULE_PATH=%1
set INPUT=%2
set OUTPUT=%3
set JAR=%4
set VERSION=%5
set APP_ICON=%6

call "%JAVA_HOME%\bin\jlink.exe" ^
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported ^
  --module-path "%MODULE_PATH%" ^
  --no-header-files --no-man-pages ^
  --output "%OUTPUT%\runtime" ^
  --strip-debug ^
  --compress=2

call "%JAVA_HOME%\bin\jpackager.exe" ^
  create-installer "exe" ^
  --verbose ^
  --echo-mode ^
  --input "%INPUT%" ^
  --output "%OUTPUT%\dist" ^
  --name GanttProject ^
  --main-jar eclipsito.jar ^
  --class org.bardsoftware.eclipsito.Boot ^
  --version "%VERSION%" ^
  --file-associations "%FILE_ASSOCIATIONS%" ^
  --icon "%APP_ICON%" ^
  --arguments "-plugins-dir plugins-%VERSION% -app net.sourceforge.ganttproject.GanttProject" ^
  --identifier biz.ganttproject ^
  --description "Free desktop project scheduling and project management application" ^
  --category "Office" ^
  --copyright "Copyright 2019 BarD Software s.r.o" ^
  --vendor "BarD Software s.r.o" ^
  --license-file LICENSE ^
  --runtime-image "%OUTPUT%/runtime"
