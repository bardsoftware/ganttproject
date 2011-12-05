/***************************************************************************
 GanttViewer.java  -  description
 -------------------
 begin                : sep 2004
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

/** Launch a ganttproject as viewer. */
public class GanttViewer {
    public static void main(String[] args) {
        GanttProject ganttFrame = new GanttProject(true);
        ganttFrame.setVisible(true);
    }
}
