/*
 LICENSE:

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.

 Copyright (C) 2004, GanttProject Development Team
 */
package net.sourceforge.ganttproject.task;

import java.awt.Color;
import java.net.URL;

import biz.ganttproject.core.time.TimeUnitStack;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface TaskManagerConfig {
  Color getDefaultColor();

  GPCalendar getCalendar();

  TimeUnitStack getTimeUnitStack();

  HumanResourceManager getResourceManager();

  URL getProjectDocumentURL();
}
