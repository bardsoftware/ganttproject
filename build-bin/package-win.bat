jlink ^
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.sql,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop,javafx.controls,javafx.swing,javafx.web ^
  --no-header-files --no-man-pages ^
  --output runtime ^
  --strip-debug ^
  --compress=2

jpackage -t msi -d build -i ganttproject-builder/dist-bin/ -n "GanttProject 3.3" ^
  --main-class com.bardsoftware.eclipsito.Launch --main-jar eclipsito.jar ^
  --java-options "-DversionDirs=app/plugins -Dapp=net.sourceforge.ganttproject.GanttProject" ^
  --java-options "--add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.base/com.sun.javafx.logging=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
  --java-options "--add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED" ^
  --java-options "--add-opens java.desktop/sun.swing=ALL-UNNAMED" ^
  --runtime-image runtime --win-dir-chooser --win-menu ^
  --description "Free desktop project scheduling and project management application" ^
  --copyright "Copyright 2022 BarD Software s.r.o" ^
  --vendor "BarD Software s.r.o" ^
  --license-file LICENSE ^
  --file-associations build-cfg\file-associations.properties ^
  --icon build-cfg\ganttproject.ico ^
  --verbose 
