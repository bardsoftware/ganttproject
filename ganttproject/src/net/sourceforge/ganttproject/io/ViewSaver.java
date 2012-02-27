/*
GanttProject is an opensource project management tool.
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
package net.sourceforge.ganttproject.io;

import javax.xml.transform.sax.TransformerHandler;

import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * @author bard
 */
class ViewSaver extends SaverBase {
    public void save(UIFacade facade, TransformerHandler handler)
            throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("zooming-state", facade.getZoomManager().getZoomState()
                .getPersistentName(), attrs);
        addAttribute("id", "gantt-chart", attrs);
        emptyElement("view", attrs, handler);

        addAttribute("id", "resource-table", attrs);
        startElement("view", attrs, handler);
        writeColumns(facade.getResourceTree().getVisibleFields(), handler);

        endElement("view", handler);

    }

    protected void writeColumns(TableHeaderUIFacade visibleFields, TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        int totalWidth = 0;
        for (int i=0; i<visibleFields.getSize(); i++) {
            if (visibleFields.getField(i).isVisible()) {
                totalWidth += visibleFields.getField(i).getWidth();
            }
        }
        for (int i=0; i<visibleFields.getSize(); i++) {
            TableHeaderUIFacade.Column field = visibleFields.getField(i);
            if (field.isVisible()) {
                addAttribute("id", field.getID(), attrs);
                addAttribute("name", field.getName(), attrs);
                addAttribute("width", field.getWidth()*100/totalWidth, attrs);
                addAttribute("order", field.getOrder(), attrs);
                emptyElement("field", attrs, handler);
            }
        }
    }
}
