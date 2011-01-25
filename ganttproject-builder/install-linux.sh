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
cp -r *.jar *.gan plugins ${INSTALL_PATH}

# Modify ganttproject.sh to run from $INSTALL_PATH
sed -e "s#GP_HOME=.#GP_HOME=${INSTALL_PATH}#" < ganttproject.sh > ${INSTALL_PATH}ganttproject.sh
chmod +x ${INSTALL_PATH}ganttproject.sh

ln -s ${INSTALL_PATH}ganttproject.sh ${GP_EXECUTABLE}

# If we are on Ubuntu, install ganttproject in menu
# (Also true for other distributions?)
if [ -n "$(uname -a | sed -n '/Ubuntu/p')" ]; then
    cp ganttproject_32_2.ico ${INSTALL_PATH}
    # Modify ganttproject.desktop to find icon at INSTALL_PATH
    sed -e "s#Icon=ganttproject_32_2.ico#Icon=${INSTALL_PATH}ganttproject_32_2.ico#" < ganttproject.desktop > ${INSTALL_PATH}../applications/ganttproject.desktop
fi
