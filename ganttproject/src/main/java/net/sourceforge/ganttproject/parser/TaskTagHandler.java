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
  private final ParsingContext myContext;
  private final TaskManager myManager;
  private final TreeCollapseView<Task> myTreeFacade;

  public TaskTagHandler(TaskManager mgr, ParsingContext context, TreeCollapseView<Task> treeFacade) {
    super("task");
    myManager = mgr;
    myContext = context;
    myTreeFacade = treeFacade;
  }

  @Override
  public void process(XmlProject xmlProject) {
    XmlSerializerKt.walkTasksDepthFirst(xmlProject, (parent, child) -> {
      loadTask(parent, child);
      return true;
    });
  }

  private void loadTask(@Nullable XmlTasks.XmlTask parent, @NotNull XmlTasks.XmlTask child) {
    var builder = getManager().newTaskBuilder().withId(child.getId());
    builder = builder.withName(child.getName());

    String start = child.getStartDate();
    if (!start.isBlank()) {
      builder = builder.withStartDate(GanttCalendar.parseXMLDate(start).getTime());
    }
    builder = builder.withDuration(getManager().createLength(child.getDuration()));


    if (!myContext.isStackEmpty()) {
      builder = builder.withParent(myContext.peekTask());
    }
    builder = builder.withExpansionState(child.isExpanded());

    if (child.isMilestone()) {
      builder = builder.withLegacyMilestone();
    }
    Task task = builder.build();

    myTreeFacade.setExpanded(task, child.isExpanded());
    task.setProjectTask(child.isProjectTask());
    task.setCompletionPercentage(child.getCompletion());

    task.setPriority(Task.Priority.fromPersistentValue(child.getPriority()));
    if (child.getColor() != null) {
      task.setColor(ColorValueParser.parseString(child.getColor()));
    }

//
//    String fixedStart = attrs.getValue("fixed-start");
//    if ("true".equals(fixedStart)) {
//      myContext.addTaskWithLegacyFixedStart(task);
//    }

    String earliestStart = child.getEarliestStartDate();

    if (earliestStart != null) {
      task.setThirdDate(GanttCalendar.parseXMLDate(earliestStart));
    }
//    String thirdConstraint = attrs.getValue("thirdDate-constraint");
//    if (thirdConstraint != null) {
//      try {
//        task.setThirdDateConstraint(Integer.parseInt(thirdConstraint));
//      } catch (NumberFormatException e) {
//        throw new RuntimeException("Failed to parse the value '" + thirdConstraint
//            + "' of attribute 'thirdDate-constraint' of tag <task>", e);
//      }
//    }

    if (child.getWebLink() != null) {
      try {
        task.setWebLink(URLDecoder.decode(child.getWebLink(), Charsets.UTF_8.name()));
      } catch (UnsupportedEncodingException e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
    }

    if (child.getShape() != null) {
      java.util.StringTokenizer st1 = new java.util.StringTokenizer(child.getShape(), ",");
      int[] array = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
      String token = "";
      int count = 0;
      while (st1.hasMoreTokens()) {
        token = st1.nextToken();
        array[count] = Integer.parseInt(token);
        count++;
      }
      task.setShape(new ShapePaint(4, 4, array, Color.white, task.getColor()));
    }

    var costValue = child.getCostManualValue();
    var isCostCalculated = child.isCostCalculated();
    if (isCostCalculated != null) {
      task.getCost().setCalculated(isCostCalculated);
      task.getCost().setValue(costValue);
    } else {
      task.getCost().setCalculated(true);
    }
    myContext.pushTask(task);
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
