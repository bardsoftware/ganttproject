@echo off

SET GP_HOME=.

SET "JAVA_COMMAND=%JAVA_HOME%\bin\java.exe"
IF "%JAVA_HOME%"=="" SET JAVA_COMMAND=java

SET LOCAL_CLASSPATH=%GP_HOME%\eclipsito.jar;%GP_HOME%
SET JVM_ARGS=-Xmx512m  -ea -Dsun.java2d.d3d=false
SET "ECLIPSITO_ARGS=--verbosity 1  --version-dirs plugins;~/.ganttproject.d/updates --app net.sourceforge.ganttproject.GanttProject"

SET BOOT_CLASS=com.bardsoftware.eclipsito.Launch

"%JAVA_COMMAND%" -classpath "%CLASSPATH%;%LOCAL_CLASSPATH%" %JVM_ARGS% %BOOT_CLASS% %ECLIPSITO_ARGS% -log true  %1 %2 %3 %4 %5 %6
if %ERRORLEVEL% EQU 9009 goto ERROR_NO_JAVA
goto END

:ERROR_NO_JAVA
echo "Please set up JAVA_HOME variable"

:END

