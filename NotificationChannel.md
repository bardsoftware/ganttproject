## Introduction ##

GanttProject users may opt-in into receiving important news via the RSS channel which shows up in GanttProject UI. See the details on http://ganttproject.biz/about/feed. This page describes how to opt-out from this channel

## How to ##

The channel settings are stored in GanttProject options file. There is no UI for editing them but you can edit the file in any text editor.

  1. Find `.ganttproject` file in your home directory (notice the dot at the first place) and open it in any text editor
  1. Find `<option id="updateRss.check" value="YES"/>` in the file
  1. Replace YES with NO