#!/bin/bash
# Copyright 2014 BarD Software s.r.o
# This script launches GanttProject. It can be symlinked and can be ran from
# any working directory

SCRIPT_FILE="$0"

WORKING_DIR="$(pwd)"
# We want to find the directory where the real script file resides.
# If real file is symlinked (possibly many times) then we need to follow
# symlinks until we reach the real script
# After that we run pwd to get directory path
cd "$(dirname "$SCRIPT_FILE")"
SCRIPT_FILE="$(basename "$SCRIPT_FILE")"

while [ -L "$SCRIPT_FILE" ]; do
  SCRIPT_FILE="$(readlink "$SCRIPT_FILE")"
  cd "$(dirname "$SCRIPT_FILE")"
  SCRIPT_FILE="$(basename "$SCRIPT_FILE")"
done

GP_HOME="$(pwd)"

cd "$WORKING_DIR"

# Create log directory
GP_LOG_DIR="$HOME/.ganttproject.d"
# Check if log dir is present (or create it)
if [ ! -d $GP_LOG_DIR ]; then
  if [ -e  $GP_LOG_DIR ]; then
    echo "file $GP_LOG_DIR exists and is not a directory" >&2
    exit 1
  fi
  if ! mkdir $GP_LOG_DIR ; then
    echo "Could not create $GP_LOG_DIR directory" >&2
    exit 1
  fi
fi

# Create unique name for log file
LOG_FILE="$GP_LOG_DIR/.ganttproject-"$(date +%Y%m%d%H%M%S)".log"
if [ -e "$LOG_FILE" ] && [ ! -w "$LOG_FILE" ]; then
  echo "Log file $LOG_FILE is not writable" >2
  exit 1
fi

# Find usable java executable
if [ -z "$JAVA_HOME" ]; then
  JAVA_COMMAND=$(which java)
  if [ "1" = "$?" ]; then
    echo "No executable java found. Please set JAVA_HOME variable" >&2
    exit 1
  fi
else
  JAVA_COMMAND=$JAVA_HOME/bin/java
fi
if [ ! -e "$JAVA_COMMAND" ]; then
  echo "$JAVA_COMMAND does not exist" >&2
  exit 1
fi
if [ ! -x "$JAVA_COMMAND" ]; then
  echo "$JAVA_COMMAND is not executable" >&2
  exit 1
fi

CLASSPATH="$CLASSPATH:$GP_HOME/eclipsito.jar:$GP_HOME"
export CLASSPATH
CONFIGURATION_FILE=ganttproject-eclipsito-config.xml
BOOT_CLASS=org.bardsoftware.eclipsito.Boot

JAVA_ARGS="-Xmx256m $BOOT_CLASS $CONFIGURATION_FILE -log true -log_file $LOG_FILE"
if [ -n "$(echo \"$*\" | sed -n '/\(^\|\s\)-/{p;}')" ]; then
  "$JAVA_COMMAND" $JAVA_ARGS "$@"
else
  "$JAVA_COMMAND" $JAVA_ARGS "$@" &
fi

