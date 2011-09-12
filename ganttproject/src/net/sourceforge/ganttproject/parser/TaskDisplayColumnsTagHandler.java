/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;

import org.xml.sax.Attributes;

/**
 * @author bbaranne
 */
public class TaskDisplayColumnsTagHandler implements TagHandler,
        ParsingListener {

    private final TableHeaderUIFacade myVisibleFields;
    private final List<Column> myBuffer = new ArrayList<Column>();
    private final String myIDPropertyName;
    private final String myOrderPropertyName;
    private final String myWidthPropertyName;
    private final String myVisiblePropertyName;
    private final String myTagName;

    public TaskDisplayColumnsTagHandler(TableHeaderUIFacade visibleFields) {
        this(visibleFields, "displaycolumn", "property-id", "order", "width", "visible");
    }

    public TaskDisplayColumnsTagHandler(TableHeaderUIFacade visibleFields, String tagName, String idPropertyName,
            String orderPropertyName, String widthPropertyName, String visiblePropertyName) {
        myVisibleFields = visibleFields;
        myTagName = tagName;
        myIDPropertyName = idPropertyName;
        myOrderPropertyName = orderPropertyName;
        myWidthPropertyName = widthPropertyName;
        myVisiblePropertyName = visiblePropertyName;
    }

    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
            throws FileFormatException {
        if (qName.equals(myTagName)) {
            loadTaskDisplay(attrs);
        }

    }

    public void endElement(String namespaceURI, String sName, String qName) {
        // TODO Auto-generated method stub
    }

    public void parsingStarted() {
        myVisibleFields.clear();
    }

    public void parsingFinished() {
        myVisibleFields.importData(TableHeaderUIFacade.Immutable.fromList(myBuffer));
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
        boolean visible = Boolean.parseBoolean(atts.getValue(myVisiblePropertyName));
        myBuffer.add(new TableHeaderUIFacade.ColumnStub(id, id, visible, order, width));
    }
}
