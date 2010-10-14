/*
 LICENSE:

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Copyright (C) 2004, GanttProject Development Team
 */
package net.sourceforge.ganttproject.task;

import java.awt.Color;
import java.net.URL;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.resource.ResourceManager;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface TaskManagerConfig {
    Color getDefaultColor();

    GPCalendar getCalendar();

    TimeUnitStack getTimeUnitStack();

    ResourceManager getResourceManager();

    URL getProjectDocumentURL();
}
