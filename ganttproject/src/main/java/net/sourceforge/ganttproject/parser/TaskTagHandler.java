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
package net.sourceforge.ganttproject.parser;

import biz.ganttproject.core.chart.render.ShapePaint;
import biz.ganttproject.core.io.XmlProject;
import biz.ganttproject.core.io.XmlSerializerKt;
import biz.ganttproject.core.io.XmlTasks;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.lib.fx.TreeCollapseView;
import com.google.common.base.Charsets;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class TaskTagHandler extends AbstractTagHandler implements ParsingListener {
  private final TaskManager myManager;
  private final TreeCollapseView<Task> myTreeFacade;

  public TaskTagHandler(TaskManager mgr, ParsingContext context, TreeCollapseView<Task> treeFacade) {
    super("task");
    myManager = mgr;
    myTreeFacade = treeFacade;
  }

  @Override
  public void process(XmlProject xmlProject) {
    var taskLoader = new TaskLoader(getManager(), myTreeFacade);
    XmlSerializerKt.walkTasksDepthFirst(xmlProject, (parent, child) -> {
      taskLoader.loadTask(parent, child);
      return true;
    });
    TaskSerializerKt.loadDependencyGraph(taskLoader.getDependencies(), myManager, taskLoader.getLegacyFixedStartTasks());
  }

  private TaskManager getManager() {
    return myManager;
  }

  @Override
  public void parsingStarted() {
  }

  @Override
  public void parsingFinished() {
  }
}
