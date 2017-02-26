/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.parser;

import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bbaranne
 */
public class TaskDisplayColumnsTagHandler extends AbstractTagHandler {

  private final List<Column> myBuffer = new ArrayList<Column>();
  private final String myIDPropertyName;
  private final String myOrderPropertyName;
  private final String myWidthPropertyName;
  private final String myVisiblePropertyName;
  private boolean isEnabled;

  public TaskDisplayColumnsTagHandler(String tagName, String idPropertyName,
      String orderPropertyName, String widthPropertyName, String visiblePropertyName) {
    super(tagName);
    myIDPropertyName = idPropertyName;
    myOrderPropertyName = orderPropertyName;
    myWidthPropertyName = widthPropertyName;
    myVisiblePropertyName = visiblePropertyName;
  }

  void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    if (!isEnabled) {
      return false;
    }
    loadTaskDisplay(attrs);
    return true;
  }

  private void loadTaskDisplay(Attributes atts) {
    String id = atts.getValue(myIDPropertyName);
    String orderStr = atts.getValue(myOrderPropertyName);
    if (orderStr == null) {
      orderStr = String.valueOf(myBuffer.size());
    }
    String widthStr = atts.getValue(myWidthPropertyName);
    int order = Integer.parseInt(orderStr);
    int width = widthStr == null ? -1 : Integer.parseInt(widthStr);
    boolean visible = true;
    if (atts.getValue(myVisiblePropertyName) != null) {
      visible = Boolean.parseBoolean(atts.getValue(myVisiblePropertyName));
    }
    myBuffer.add(new ColumnList.ColumnStub(id, id, visible, order, width));
  }

  public static TaskDisplayColumnsTagHandler createPilsenHandler() {
    return new TaskDisplayColumnsTagHandler("field", "id", "order", "width", "visible");
  }

  public static TaskDisplayColumnsTagHandler createLegacyHandler() {
    TaskDisplayColumnsTagHandler result = new TaskDisplayColumnsTagHandler("displaycolumn", "property-id", "order", "width", "NONAME");
    result.setEnabled(true);
    return result;
  }

  public static ParsingListener createTaskDisplayColumnsWrapper(final ColumnList visibleFields, final TaskDisplayColumnsTagHandler pilsenHandler, final TaskDisplayColumnsTagHandler legacyHandler) {
    return new ParsingListener() {
      @Override
      public void parsingStarted() {
        visibleFields.clear();
      }

      @Override
      public void parsingFinished() {
        List<Column> buffer = pilsenHandler.myBuffer.isEmpty() ? legacyHandler.myBuffer : pilsenHandler.myBuffer;
        visibleFields.importData(ColumnList.Immutable.fromList(buffer));
      }

    };
  }

  public static ParsingListener createTaskDisplayColumnsWrapper(final ColumnList visibleFields, final TaskDisplayColumnsTagHandler displayColumnsTagHandler) {
    return new ParsingListener() {
      @Override
      public void parsingStarted() {
        visibleFields.clear();
      }

      @Override
      public void parsingFinished() {
        visibleFields.importData(ColumnList.Immutable.fromList(displayColumnsTagHandler.myBuffer));
      }
    };
  }
}
