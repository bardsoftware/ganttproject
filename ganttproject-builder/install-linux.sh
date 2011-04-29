#! /bin/bash

# Location where to install GanttProject
INSTALL_PATH=/usr/local/share/ganttproject/

# Location of executable of GanttProject
GP_EXECUTABLE=/usr/local/bin/ganttproject

# Assuming that writing at INSTALL_PATH and GP_EXECUTABLE needs root rights:
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

rm -rf ${INSTALL_PATH} ${GP_EXECUTABLE}
mkdir $INSTALL_PATH
cp -r * ${INSTALL_PATH}

chmod +x ${INSTALL_PATH}ganttproject
ln -s ${INSTALL_PATH}ganttproject ${GP_EXECUTABLE}
