@echo off

SET GP_HOME=.

SET "JAVA_COMMAND=%JAVA_HOME%\bin\java.exe"
IF "%JAVA_HOME%"=="" SET JAVA_COMMAND=java

SET LOCAL_CLASSPATH=%GP_HOME%\eclipsito.jar;%GP_HOME%\lib\slf4j-api-2.0.4.jar;%GP_HOME%\lib\slf4j-jdk14-2.0.4.jar;%GP_HOME%
SET JVM_ARGS=-Dgpcloud=prod -Dorg.jooq.no-logo=true -Xmx2048m  -ea -Dsun.java2d.d3d=false
SET "ECLIPSITO_ARGS=--verbosity 4  --version-dirs plugins;~/.ganttproject.d/updates --app net.sourceforge.ganttproject.GanttProject"
SET "JAVA_EXPORTS=--add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED  --add-exports javafx.base/com.sun.javafx.logging=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED --add-opens java.desktop/sun.swing=ALL-UNNAMED"

SET BOOT_CLASS=com.bardsoftware.eclipsito.Launch

"%JAVA_COMMAND%" -classpath "%CLASSPATH%;%LOCAL_CLASSPATH%" %JVM_ARGS% %JAVA_EXPORTS% %BOOT_CLASS% %ECLIPSITO_ARGS% -log true  %1 %2 %3 %4 %5 %6
if %ERRORLEVEL% EQU 9009 goto ERROR_NO_JAVA
goto END

:ERROR_NO_JAVA
echo "Please set up JAVA_HOME variable"

:END

