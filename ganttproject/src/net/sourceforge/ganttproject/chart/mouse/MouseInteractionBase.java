/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.chart.mouse;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Date;

import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.chart.mouse.MouseInteraction.TimelineFacade;

abstract class MouseInteractionBase {
  protected Date myStartDate;
  private final MouseInteraction.TimelineFacade myChartDateGrid;

  protected MouseInteractionBase(Date startDate, MouseInteraction.TimelineFacade chartDateGrid) {
    myStartDate = startDate;
    myChartDateGrid = chartDateGrid;
  }

  protected TimeDuration getLengthDiff(MouseEvent event) {
    Date dateUnderX = myChartDateGrid.getDateAt(event.getX());
    TimeDuration result = myChartDateGrid.createTimeInterval(myChartDateGrid.getTimeUnitStack().getDefaultTimeUnit(),
        myStartDate, dateUnderX);
    return result;
  }

  protected TimelineFacade getChartDateGrid() {
    return myChartDateGrid;
  }

  /**
   * Method to show the visible cues of the interaction
   * 
   * @param g
   *          is the graphics context
   */
  public void paint(Graphics g) {
  }

  protected void setStartDate(Date date) {
    myStartDate = date;
  }

  protected Date getStartDate() {
    return myStartDate;
  }
}