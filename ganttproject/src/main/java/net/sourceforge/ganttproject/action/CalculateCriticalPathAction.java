/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskManager;

public class CalculateCriticalPathAction extends GPAction {
  private final TaskManager myTaskManager;

  private final static String ICON_PREFIX_ON = "criticalPathOn_";

  private final static String ICON_PREFIX_OFF = "criticalPathOff_";

  private final UIConfiguration myUIConfiguration;

  private final UIFacade myUiFacade;

  public CalculateCriticalPathAction(TaskManager taskManager, UIConfiguration uiConfiguration, UIFacade uiFacade) {
    super("criticalPath.toggle");
    myTaskManager = taskManager;
    myUIConfiguration = uiConfiguration;
    myUiFacade = uiFacade;
  }

  @Override
  protected String getIconFilePrefix() {
    return isOn() ? ICON_PREFIX_ON : ICON_PREFIX_OFF;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    setOn(!isOn());
    updateAction();
    if (isOn()) {
      myTaskManager.processCriticalPath(myTaskManager.getRootTask());
    }
    myUiFacade.refresh();
  }

  private void setOn(boolean on) {
    myUIConfiguration.setCriticalPathOn(on);
  }

  private boolean isOn() {
    return myUIConfiguration == null ? false : myUIConfiguration.isCriticalPathOn();
  }

  @Override
  public String getID() {
    // Override ID, so the text will change depending on the state
    return isOn() ? "criticalPath.action.hide" : "criticalPath.action.show";
  }
}
