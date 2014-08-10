/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject;

import java.awt.Image;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.gui.zoom.ZoomManager.ZoomState;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.Task;

/** Class to store 3 boolean values */
public class GanttExportSettings {
  private Date startDate = null;

  private Date endDate = null;

  public boolean name, percent, depend, border3d, ok;

  private boolean onlySelectedItems;

  private List<Task> myVisibleTasks;

  private int myRowCount;

  private int myWidth = -1;

  private ZoomState myZoomLevel;

  private boolean isCommandLineMode;

  private Image myLogo;

  private String myExpandedResources;

  public GanttExportSettings() {
    name = percent = depend = ok = true;
    onlySelectedItems = false;
  }

  public GanttExportSettings(boolean bName, boolean bPercent, boolean bDepend, boolean b3dBorders) {
    name = bName;
    percent = bPercent;
    depend = bDepend;
    border3d = b3dBorders;
    ok = true;
    onlySelectedItems = false;
  }

  public void setOnlySelectedItem(boolean selected) {
    onlySelectedItems = selected;
  }

  public boolean isOnlySelectedItem() {
    return onlySelectedItems;
  }

  public void setStartDate(Date date) {
    startDate = date;
  }

  public void setEndDate(Date date) {
    endDate = date;
  }

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setVisibleTasks(List<Task> visibleTasks) {
    myVisibleTasks = visibleTasks;
    if (visibleTasks != null) {
      myRowCount = visibleTasks.size();
    }
  }

  public List<Task> getVisibleTasks() {
    return myVisibleTasks;
  }

  public int getRowCount() {
    return myRowCount;
  }

  public void setRowCount(int rowCount) {
    myRowCount = rowCount;
  }

  public int getWidth() {
    return myWidth;
  }

  public void setWidth(int width) {
    myWidth = width;
  }

  public void setZoomLevel(ZoomState zoomLevel) {
    myZoomLevel = zoomLevel;
  }

  public ZoomState getZoomLevel() {
    return myZoomLevel;
  }

  public void setCommandLineMode(boolean value) {
    isCommandLineMode = value;
  }

  public boolean isCommandLineMode() {
    return isCommandLineMode;
  }

  public Image getLogo() {
    return myLogo;
  }

  public void setLogo(Image logo) {
    myLogo = logo;
  }

  public void setExpandedResources(String list) {
    myExpandedResources = list;
  }

  public boolean isExpanded(HumanResource hr) {
    return "".equals(myExpandedResources);
  }
}
