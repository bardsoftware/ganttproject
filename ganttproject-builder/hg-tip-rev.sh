#!/bin/sh
REV=$(git rev-list --count ganttproject-2.7..)
echo $(($REV+1891))
