/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.action.task;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class TaskUnlinkAction extends TaskActionBase {

  public TaskUnlinkAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
    super("task.unlink", taskManager, selectionManager, uiFacade, null);
  }

  @Override
  protected String getIconFilePrefix() {
    return "unlink_";
  }

  @Override
  protected boolean isEnabled(List<Task> selection) {
    for (Task task : selection) {
      if (task.getDependencies().hasLinks(selection)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void run(List<Task> selection) throws Exception {
    for (Task task : selection) {
      task.getDependencies().clear(selection);
    }
    // Update (un)link buttons
    getSelectionManager().fireSelectionChanged();
  }

  @Override
  public GPAction asToolbarAction() {
    final TaskUnlinkAction result = new TaskUnlinkAction(getTaskManager(), getSelectionManager(), getUIFacade());
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    this.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
          result.setEnabled((Boolean)evt.getNewValue());
        }
      }
    });
    result.setEnabled(this.isEnabled());
    return result;
  }
}
