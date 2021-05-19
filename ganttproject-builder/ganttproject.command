#!/bin/bash
# Copyright 2014 BarD Software s.r.o
# This script launches GanttProject. It can be symlinked and can be ran from
# any working directory

SCRIPT_FILE="$0"

find_ganttproject_home() {
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

  pwd
}
GP_HOME="$(find_ganttproject_home)"
if [ -z "$GP_HOME" ]; then
  echo "GanttProject home directory is not set. Please point GP_HOME environment variable to the directory with GanttProject files."
  exit 1
fi


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

  VERSION="$( $JAVA_COMMAND -version 2>&1 | head -n 1)"
  log "...found $VERSION"
  [[ "$VERSION" =~ "11." ]] && return 0;
  [[ "$VERSION" =~ "12." ]] && return 0;
  [[ "$VERSION" =~ "13." ]] && return 0;
  [[ "$VERSION" =~ "14." ]] && return 0;
  [[ "$VERSION" =~ "15." ]] && return 0;
  [[ "$VERSION" =~ "16." ]] && return 0;
  log "... this seems to be an old Java Runtime";
  JAVA_COMMAND=""
  return 1
}

find_java() {
  if [ ! -z "$JAVA_HOME" ]; then
    check_java "$JAVA_HOME/bin/java" && return 0;
  fi
  JAVA_COMMAND=$(which java)
  if [ "0" = "$?" ]; then
    check_java "$JAVA_COMMAND" && return 0;
  fi

  if [ -x /usr/libexec/java_home ]; then
    check_java "$(/usr/libexec/java_home)/bin/java" && return 0;
  fi

  if [ -x /usr/libexec/java_home ]; then
    check_java "$(/usr/libexec/java_home -v 1.8+)/bin/java" && return 0;
  fi

  for f in $(ls /Library/Java/JavaVirtualMachines/); do
    check_java "/Library/Java/JavaVirtualMachines/$f/Contents/Home/bin/java" && return 0;
  done;

  check_java "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java" && return 0;
  check_java /Library/Java/Home/bin/java && return 0;

  check_java /System/Library/Frameworks/JavaVM.framework/Home/bin/java && return 0;
  report_java_not_found && exit 1;
}

report_java_not_found() {
  if [ -z "$LOG_TEXT" ]; then
    LOG_TEXT="$(cat /tmp/ganttproject-launcher.log)"
  fi

  LOG_TEXT=$(echo "$LOG_TEXT" | sed s/\"/\\\\\"/g)
  osascript -e 'tell app "System Events" to display alert "Java Runtime not found" message "GanttProject cannot find a suitable Java Runtime.\n\nWhat we have tried:\n'"$LOG_TEXT"'\n\nYou can find this log in /tmp/ganttproject-launcher.log file\nProceed to http://docs.ganttproject.biz/user/troubleshooting-installation to learn how to fix this."'
}

find_java
CLASSPATH="$CLASSPATH:$GP_HOME/eclipsito.jar:$GP_HOME"
export CLASSPATH

BOOT_CLASS=com.bardsoftware.eclipsito.Launch
ECLIPSITO_ARGS="--verbosity 4 --version-dirs plugins --app net.sourceforge.ganttproject.GanttProject"
MACOS_ARGS="-Dapple.laf.useScreenMenuBar=true -Dcom.apple.macos.useScreenMenuBar=true	-Dcom.apple.mrj.application.apple.menu.about.name=GanttProject -Xdock:name=GanttProject -Xdock:icon=ganttproject.icns"
JAVA_ARGS="-Duser.dir=$GP_HOME -Xmx1024m -ea -Dfile.encoding=UTF-8 $MACOS_ARGS $BOOT_CLASS $ECLIPSITO_ARGS -log true -log_file $LOG_FILE"

if [ -n "$(echo \"$*\" | sed -n '/\(^\|\s\)-/{p;}')" ]; then
  "$JAVA_COMMAND" $JAVA_ARGS "$@"
else
  echo $JAVA_COMMAND
  "$JAVA_COMMAND" $JAVA_ARGS "$@" &
fi

