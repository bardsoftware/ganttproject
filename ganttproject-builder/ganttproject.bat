@echo off

SET GP_HOME=.

SET "JAVA_COMMAND=%JAVA_HOME%\bin\java.exe"
IF "%JAVA_HOME%"=="" SET JAVA_COMMAND=java

SET LOCAL_CLASSPATH=%GP_HOME%\eclipsito.jar;%GP_HOME%

SET CONFIGURATION_FILE=ganttproject-eclipsito-config.xml
SET BOOT_CLASS=org.bardsoftware.eclipsito.Boot

"%JAVA_COMMAND%" -Xmx512m -classpath "%CLASSPATH%;%LOCAL_CLASSPATH%" -ea -Dsun.java2d.d3d=false %BOOT_CLASS% "%CONFIGURATION_FILE%" -log true  %1 %2 %3 %4 %5 %6 
if %ERRORLEVEL% EQU 9009 goto ERROR_NO_JAVA
goto END

:ERROR_NO_JAVA
echo "Please set up JAVA_HOME variable"

:END

