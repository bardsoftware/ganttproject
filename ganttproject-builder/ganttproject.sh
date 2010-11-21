#!/bin/bash

GP_HOME=.
COMMAND_PATH=`echo ${0} | sed -e "s/\(.*\)\/.*$/\1/g"`
cd ${COMMAND_PATH}

LOCAL_CLASSPATH=$GP_HOME/eclipsito.jar

CONFIGURATION_FILE=ganttproject-eclipsito-config.xml
BOOT_CLASS=org.bardsoftware.eclipsito.Boot
LOG_FILE=$HOME/.ganttproject.log

if [ -z $JAVA_HOME ]; then
  JAVA_COMMAND=`which java`
  if [ "$?" = "1" ]; then
    echo "No executable java found. Please set JAVA_HOME variable";
    exit;
  fi
else
  JAVA_COMMAND=$JAVA_HOME/bin/java
fi
if [ ! -x $JAVA_COMMAND ]; then
  echo "$JAVA_COMMAND is not executable. Please check the permissions."
  exit
fi

if [ -e $LOG_FILE ] && [ ! -w $LOG_FILE ]; then
  echo "Log file $LOG_FILE is not writable"
  exit
fi

$JAVA_COMMAND -Xmx256m -classpath $CLASSPATH:$LOCAL_CLASSPATH $BOOT_CLASS $CONFIGURATION_FILE -log $LOG_FILE "$@" 


