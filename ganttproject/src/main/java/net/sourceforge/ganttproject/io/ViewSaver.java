/*
Copyright (C) 2014 BarD Software s.r.o
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.io;

import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.ganttview.TaskFilterManager;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import net.sourceforge.ganttproject.gui.GPColorChooser;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.view.ViewProvider;
import net.sourceforge.ganttproject.task.Task;
import org.apache.commons.text.StringEscapeUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.sax.TransformerHandler;
import java.util.Set;

/**
 * Serializes Gantt chart and resource chart view data to XML.
 * View data is data which is required for presentation purposes but
 * can be ignored in the logical model. Such data includes visible columns,
 * label options, tasks shown in the timeline.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class ViewSaver extends SaverBase {
  public void save(UIFacade facade, ViewProvider ganttViewProvider, ViewProvider resourceViewProvider, ColumnList taskColumnList,
                   TaskFilterManager taskFilterManager, ColumnList resourceColumnList, TransformerHandler handler) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    addAttribute("zooming-state", facade.getZoomManager().getZoomState().getPersistentName(), attrs);
    addAttribute("id", "gantt-chart", attrs);
    startElement("view", attrs, handler);
    writeColumns(taskColumnList, handler);
    writeTimelineTasks(facade, handler);
    new OptionSaver().saveOptionList(handler, facade.getGanttChart().getTaskLabelOptions().getOptions());
    new OptionSaver().saveOptionList(handler, ganttViewProvider.getOptions());
    writeFilters(handler, taskFilterManager);
    writeRecentColors(handler);
    endElement("view", handler);

    addAttribute("id", "resource-table", attrs);
    startElement("view", attrs, handler);
    writeColumns(resourceColumnList, handler);

    endElement("view", handler);

  }

  private void writeRecentColors(TransformerHandler handler) throws SAXException {
    new OptionSaver().saveOptionList(handler, GPColorChooser.getRecentColorsOption());
  }

  /**
   * Writes tasks explicitly shown in the timeline as comma-separated list of task identifiers in CDATA section
   * of <timeline> element.
   */
  private void writeTimelineTasks(UIFacade facade, TransformerHandler handler) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    Set<Task> timelineTasks = facade.getCurrentTaskView().getTimelineTasks();
    if (timelineTasks.isEmpty()) {
      return;
    }
    Function<Task, String> getId = new Function<Task, String>() {
      @Override
      public String apply(Task t) {
        return String.valueOf(t.getTaskID());
      }
    };
    cdataElement("timeline", Joiner.on(',').join(Collections2.transform(timelineTasks, getId)), attrs, handler);
  }

  protected void writeColumns(ColumnList visibleFields, TransformerHandler handler) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    for (ColumnList.Column field : visibleFields.exportData()) {
      if (field.isVisible()) {
        addAttribute("id", field.getID(), attrs);
        addAttribute("name", field.getName(), attrs);
        addAttribute("width", field.getWidth(), attrs);
        addAttribute("order", field.getOrder(), attrs);
        emptyElement("field", attrs, handler);
      }
    }
  }

  private void writeFilters(TransformerHandler handler, TaskFilterManager taskFilterManager) throws SAXException {
    startElement("filters", new AttributesImpl(), handler);
    taskFilterManager.getFilters().forEach(filter -> {
      var attrs = new AttributesImpl();
      addAttribute("title", filter.getTitle(), attrs);
      addAttribute("description", filter.getDescription(), attrs);
      addAttribute("is-built-in", filter.isBuiltIn(), attrs);
      addAttribute("is-enabled", filter.isEnabledProperty().getValue(), attrs);
      try {
        startElement("filter", attrs, handler);
        if (filter.getExpression() != null) {
          var expressionAttrs = new AttributesImpl();
          addAttribute("where", StringEscapeUtils.escapeXml11(filter.getExpression()), expressionAttrs);
          emptyElement("simple-select", expressionAttrs, handler);
        }
        endElement("filter", handler);
      } catch (SAXException e) {
        throw new RuntimeException(e);
      }

    });
    endElement("filters", handler);
  }
}
