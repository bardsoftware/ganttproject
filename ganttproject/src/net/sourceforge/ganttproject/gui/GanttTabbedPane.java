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
package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTabbedPane;

public class GanttTabbedPane extends JTabbedPane {

  private Map<Component, Object> myUserObjectsMap = new HashMap<Component, Object>();

  public GanttTabbedPane() {
    super();
  }

  public void addTab(String title, Component component, Object userObject) {
    super.addTab(title, component);
    myUserObjectsMap.put(component, userObject);
  }

  public void addTab(String title, Icon icon, Component component, Object userObject) {
    super.addTab(title, icon, component);
    myUserObjectsMap.put(component, userObject);
  }

  public void addTab(String title, Icon icon, Component component, String tip, Object userObject) {
    super.addTab(title, icon, component, tip);
    myUserObjectsMap.put(component, userObject);
  }

  public Object getSelectedUserObject() {
    Object selectedComp = this.getSelectedComponent();
    return myUserObjectsMap.get(selectedComp);
  }

}
