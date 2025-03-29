/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.taskproperties.TaskAllocationsPanel;
import net.sourceforge.ganttproject.gui.taskproperties.TaskDependenciesPanel;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskMutator;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Real panel for editing task properties
 */
public class GanttTaskPropertiesBean extends JPanel {

  private ColorOption myTaskColorOption = new DefaultColorOption("");
  private final GPAction mySetDefaultColorAction = new GPAction("defaultColor") {
    @Override
    public void actionPerformed(ActionEvent e) {
      myTaskColorOption.setValue(myUIfacade.getGanttChart().getTaskDefaultColorOption().getValue());
    }
  };

  private GanttTask[] selectedTasks;

  JComponent predecessorsPanel;

  JPanel resourcesPanel;


  private TaskDependenciesPanel myDependenciesPanel;

  private TaskAllocationsPanel myAllocationsPanel;

  private final HumanResourceManager myHumanResourceManager;

  private final RoleManager myRoleManager;

  private final UIFacade myUIfacade;

  public GanttTaskPropertiesBean(GanttTask[] selectedTasks, IGanttProject project, UIFacade uifacade) {
    this.selectedTasks = selectedTasks;
    myHumanResourceManager = project.getHumanResourceManager();
    myRoleManager = project.getRoleManager();
    myUIfacade = uifacade;
    init();
  }


  /** Construct the predecessors tabbed pane */
  private void constructPredecessorsPanel() {
    myDependenciesPanel = new TaskDependenciesPanel();
    myDependenciesPanel.init(selectedTasks[0]);
    predecessorsPanel = myDependenciesPanel.getComponent();
  }

  /** Construct the resources panel */
  private void constructResourcesPanel() {
    myAllocationsPanel = new TaskAllocationsPanel(selectedTasks[0], myHumanResourceManager, myRoleManager);
    resourcesPanel = myAllocationsPanel.getComponent();
  }

  /** Initialize the widgets */
  private void init() {
    constructPredecessorsPanel();
    constructResourcesPanel();
  }

  public void save(TaskMutator mutator) {
    myAllocationsPanel.commit(mutator);
    mutator.commit();
    myDependenciesPanel.commit();
  }
}
