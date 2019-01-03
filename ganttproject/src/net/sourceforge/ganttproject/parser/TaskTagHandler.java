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
import biz.ganttproject.core.time.GanttCalendar;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManager.TaskBuilder;
import org.xml.sax.Attributes;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

public class TaskTagHandler extends AbstractTagHandler implements ParsingListener {
  private final ParsingContext myContext;
  private final TaskManager myManager;
  private final TaskTreeUIFacade myTreeFacade;
  private final Map<Integer, Boolean> myTaskIdToExpansionState = Maps.newHashMap();
  public TaskTagHandler(TaskManager mgr, ParsingContext context, TaskTreeUIFacade treeFacade) {
    super("task");
    myManager = mgr;
    myContext = context;
    myTreeFacade = treeFacade;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    loadTask(attrs);
    return true;
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
    if (isMyTag(qName) && isTagStarted()) {
      myContext.popTask();
      if (myContext.isStackEmpty()) {
        setTagStarted(false);
      }
    }
  }

  private void loadTask(Attributes attrs) {
    String taskIdAsString = attrs.getValue("id");
    int taskId;
    try {
      taskId = Integer.parseInt(taskIdAsString);
    } catch (NumberFormatException e) {
      throw new RuntimeException(
          "Failed to parse the value '" + taskIdAsString + "' of attribute 'id' of tag <task>", e);
    }
    TaskBuilder builder = getManager().newTaskBuilder().withId(taskId);

    String taskName = attrs.getValue("name");
    if (taskName != null) {
      builder = builder.withName(taskName);
    }

    String start = attrs.getValue("start");
    if (start != null) {
      builder = builder.withStartDate(GanttCalendar.parseXMLDate(start).getTime());
    }

    String duration = attrs.getValue("duration");
    if (duration != null) {
      try {
        int length = Integer.parseInt(duration);
        builder = builder.withDuration(getManager().createLength(length));
      } catch (NumberFormatException e) {
        throw new RuntimeException(
            "Failed to parse the value '" + duration + "' of attribute 'duration' of tag <task>", e);
      }
    }

    if (!myContext.isStackEmpty()) {
      builder = builder.withParent(myContext.peekTask());
    }
    String isExpanded = attrs.getValue("expand");
    if (isExpanded != null) {
      builder = builder.withExpansionState(Boolean.parseBoolean(isExpanded));
    }

    String isLegacyMilestone = attrs.getValue("meeting");
    if (Boolean.parseBoolean(isLegacyMilestone)) {
      builder = builder.withLegacyMilestone();
    }
    Task task = builder.build();

    myTaskIdToExpansionState.put(task.getTaskID(), task.getExpand());
    String project = attrs.getValue("project");
    if (project != null) {
      task.setProjectTask(true);
    }

    String complete = attrs.getValue("complete");
    if (complete != null) {
      try {
        task.setCompletionPercentage(Integer.parseInt(complete));
      } catch (NumberFormatException e) {
        throw new RuntimeException(
            "Failed to parse the value '" + complete + "' of attribute 'complete' of tag <task>", e);
      }
    }

    String priority = attrs.getValue("priority");
    if (priority != null) {
      task.setPriority(Task.Priority.fromPersistentValue(priority));
    }

    String color = attrs.getValue("color");
    if (color != null) {
      task.setColor(ColorValueParser.parseString(color));
    }

    String fixedStart = attrs.getValue("fixed-start");
    if ("true".equals(fixedStart)) {
      myContext.addTaskWithLegacyFixedStart(task);
    }

    String third = attrs.getValue("thirdDate");
    if (third != null) {
      task.setThirdDate(GanttCalendar.parseXMLDate(third));
    }
    String thirdConstraint = attrs.getValue("thirdDate-constraint");
    if (thirdConstraint != null) {
      try {
        task.setThirdDateConstraint(Integer.parseInt(thirdConstraint));
      } catch (NumberFormatException e) {
        throw new RuntimeException("Failed to parse the value '" + thirdConstraint
            + "' of attribute 'thirdDate-constraint' of tag <task>", e);
      }
    }

    String webLink_enc = attrs.getValue("webLink");
    String webLink = webLink_enc;
    if (webLink_enc != null)
      try {
        webLink = URLDecoder.decode(webLink_enc, Charsets.UTF_8.name());
      } catch (UnsupportedEncodingException e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
    if (webLink != null) {
      task.setWebLink(webLink);
    }

    String shape = attrs.getValue("shape");
    if (shape != null) {
      java.util.StringTokenizer st1 = new java.util.StringTokenizer(shape, ",");
      int[] array = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
      String token = "";
      int count = 0;
      while (st1.hasMoreTokens()) {
        token = st1.nextToken();
        array[count] = (new Integer(token)).intValue();
        count++;
      }
      task.setShape(new ShapePaint(4, 4, array, Color.white, task.getColor()));
    }

    String costValue = attrs.getValue("cost-manual-value");
    String costCalculated = attrs.getValue("cost-calculated");
    if (costCalculated != null) {
      task.getCost().setCalculated(Boolean.valueOf(costCalculated));
      task.getCost().setValue(new BigDecimal(costValue));
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
    List<Task> tasksBottomUp = Lists.reverse(myManager.getTaskHierarchy().breadthFirstSearch(null, false));

    for (Task t : tasksBottomUp) {
      myTreeFacade.setExpanded(t, Objects.firstNonNull(myTaskIdToExpansionState.get(t.getTaskID()), Boolean.TRUE));
    }
  }
}
