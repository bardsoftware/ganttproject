/***************************************************************************
 GanttCalendarDays.java  -  description
 -------------------
 begin                : jan 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject;

import java.util.ArrayList;

/**
 * 
 */
public class GanttCalendarDays {
    /** List of non working days */
    private ArrayList<GanttCalendar> dayList = null;

    /* Default constructor */
    public GanttCalendarDays() {
        dayList = new ArrayList<GanttCalendar>();
    }

    /* constructor */
    public GanttCalendarDays(ArrayList<GanttCalendar> dayList) {
        this.dayList = dayList;
    }

    /** Insert the day on the daylist */
    public void insert(GanttCalendar day) {
        if (!dayList.contains(day))
            dayList.add(day);
    }

    /** Remove the selected day */
    public void remove(GanttCalendar day) {
        int index = dayList.indexOf(day);
        if (index >= 0)
            dayList.remove(index);
    }

}
