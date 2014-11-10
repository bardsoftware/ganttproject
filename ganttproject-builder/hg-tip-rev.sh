#!/bin/sh
REV=`hg log -l 1 | grep changeset: | cut -d: -f 2 | tr -d ' '`
echo $(($REV+1))
