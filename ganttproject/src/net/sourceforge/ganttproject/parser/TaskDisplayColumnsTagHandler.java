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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;

/**
 * @author bbaranne
 */
public class TaskDisplayColumnsTagHandler extends AbstractTagHandler implements ParsingListener {

  private final ColumnList myVisibleFields;
  private final List<Column> myBuffer = new ArrayList<Column>();
  private final String myIDPropertyName;
  private final String myOrderPropertyName;
  private final String myWidthPropertyName;
  private final String myVisiblePropertyName;

  public TaskDisplayColumnsTagHandler(ColumnList visibleFields) {
    this(visibleFields, "displaycolumn", "property-id", "order", "width", "visible");
  }

  public TaskDisplayColumnsTagHandler(ColumnList visibleFields, String tagName, String idPropertyName,
      String orderPropertyName, String widthPropertyName, String visiblePropertyName) {
    super(tagName);
    myVisibleFields = visibleFields;
    myIDPropertyName = idPropertyName;
    myOrderPropertyName = orderPropertyName;
    myWidthPropertyName = widthPropertyName;
    myVisiblePropertyName = visiblePropertyName;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    loadTaskDisplay(attrs);
    return true;
  }

  @Override
  public void parsingStarted() {
    myVisibleFields.clear();
  }

  @Override
  public void parsingFinished() {
    myVisibleFields.importData(ColumnList.Immutable.fromList(myBuffer));
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
}
