jlink ^
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.sql,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web ^
  --no-header-files --no-man-pages ^
  --output runtime ^
  --strip-debug ^
  --compress=2

REM Options shared by the MSI package and by the app image which is wrapped into the NSIS installer.
SET JPACKAGE_COMMON_OPTS=-i ganttproject-builder/dist-bin/ ^
  --main-class com.bardsoftware.eclipsito.Launch --main-jar eclipsito.jar ^
  --java-options "-Duser.dir=$APPDIR -DversionDirs=plugins;~/.ganttproject.d/updates -Dapp=net.sourceforge.ganttproject.GanttProject -Dgpcloud=prod -Dorg.jooq.no-logo=true -Xms32m -Xmx2048m -Dsun.java2d.d3d=false -ea" ^
  --java-options "--add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.base/com.sun.javafx.logging=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.glass.ui=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED" ^
  --java-options "--add-opens java.desktop/sun.swing=ALL-UNNAMED" ^
  --runtime-image runtime ^
  --description "Free project scheduling and project management application with Gantt chart" ^
  --app-version %VERSION% ^
  --copyright "Copyright 2026 BarD Software s.r.o." ^
  --vendor "BarD Software s.r.o." ^
  --icon build-cfg\ganttproject.ico

jpackage -t msi -d build -n %WINDOWS_APP_FOLDER_NAME% %JPACKAGE_COMMON_OPTS% ^
  --win-dir-chooser --win-menu --win-menu-group "%WINDOWS_APP_FOLDER_NAME%" ^
  --license-file LICENSE ^
  --file-associations build-cfg\file-associations.properties ^
  --verbose
IF ERRORLEVEL 1 EXIT /B 1
dir build
mv "build\%WINDOWS_APP_FOLDER_NAME%-%VERSION%.msi" "build\ganttproject-%VERSION%.msi"

REM App image for the NSIS installer: build\app-image\GanttProject\GanttProject.exe hosts the JVM in-process,
REM so that Windows taskbar shows GanttProject icon rather than javaw.exe icon (as it happens with launch4j launcher).
IF EXIST build\app-image RMDIR /S /Q build\app-image
jpackage -t app-image -d build\app-image -n GanttProject %JPACKAGE_COMMON_OPTS% --verbose
IF ERRORLEVEL 1 EXIT /B 1
