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
package net.sourceforge.ganttproject.io;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;


class GanttChartViewSaver extends SaverBase {

  void save(ColumnList tableHeader, TransformerHandler handler) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    boolean allHidden = true;
    for (int i = 0; i < tableHeader.getSize(); i++) {
      Column column = tableHeader.getField(i);
      if (column.isVisible()) {
        allHidden = false;
        break;
      }
    }
    startElement("taskdisplaycolumns", handler);
    for (int i = 0; i < tableHeader.getSize(); i++) {
      Column column = tableHeader.getField(i);
      addAttribute("property-id", column.getID(), attrs);
      addAttribute("order", column.getOrder(), attrs);
      addAttribute("width", column.getWidth(), attrs);
      if (allHidden && column.getID().equals(TaskDefaultColumn.NAME.getStub().getID())) {
        addAttribute("visible", true, attrs);
      } else {
        addAttribute("visible", column.isVisible(), attrs);
      }
      emptyElement("displaycolumn", attrs, handler);
    }
    endElement("taskdisplaycolumns", handler);
  }
}
