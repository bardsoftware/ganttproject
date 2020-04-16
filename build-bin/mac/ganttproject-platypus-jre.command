#!/bin/bash
# Copyright 2020 BarD Software s.r.o
# This script is launched using Platypus in a distro with bundled Java Runtime
# It is similar to ganttproject.command except that it does not try to 
# change the working directory and searches for Java Runtime in a single directory.

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

LOG_TEXT=""
echo "" > /tmp/ganttproject-launcher.log || LOG_TEXT="----"  

log() {
  if [ ! -z "$LOG_TEXT" ]; then
    LOG_TEXT="$LOG_TEXT\n$1";
  else
    echo $1 >> /tmp/ganttproject-launcher.log
  fi
}

check_java() {
  JAVA_COMMAND=$1
  log  "Searching for Java in $JAVA_COMMAND"

  if [ ! -x "$JAVA_COMMAND" ]; then
    log "...missing or not executable"
    JAVA_COMMAND=""
    return 1
  fi
}

report_java_not_found() {
  if [ -z "$LOG_TEXT" ]; then
    LOG_TEXT="$(cat /tmp/ganttproject-launcher.log)"
  fi
 
  LOG_TEXT=$(echo "$LOG_TEXT" | sed s/\"/\\\\\"/g) 
  osascript -e 'tell app "System Events" to display alert "Java Runtime not found" message "GanttProject cannot find a suitable Java Runtime.\n\nWhat we have tried:\n'"$LOG_TEXT"'\n\nYou can find this log in /tmp/ganttproject-launcher.log file\nVisit http://docs.ganttproject.biz/user/troubleshooting-installation to learn how to fix this."'
}

find_java() {
  check_java "runtime/bin/java" && return 0;
  report_java_not_found 
  exit 1
}

find_java
CLASSPATH="$CLASSPATH:./eclipsito.jar"
export CLASSPATH
BOOT_CLASS=org.bardsoftware.eclipsito.Boot
ECLIPSITO_ARGS="-plugins-dir plugins -app net.sourceforge.ganttproject.GanttProject"

JAVA_ARGS="-Dapple.laf.useScreenMenuBar=true -Dcom.apple.macos.useScreenMenuBar=true  -Dcom.apple.mrj.application.apple.menu.about.name=GanttProject -Xdock:name=GanttProject -Xdock:icon=ganttproject.icns -Xmx512m -ea -Dfile.encoding=UTF-8 $BOOT_CLASS $ECLIPSITO_ARGS -log true -log_file $LOG_FILE"

if [ -n "$(echo \"$*\" | sed -n '/\(^\|\s\)-/{p;}')" ]; then
  "$JAVA_COMMAND" $JAVA_ARGS "$@"
else
  "$JAVA_COMMAND" $JAVA_ARGS "$@" &
fi

