package net.sourceforge.ganttproject.io;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.GanttTreeTable;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;

class GanttChartViewSaver extends SaverBase {

    void save(GanttTreeTable treeTable, TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        startElement("taskdisplaycolumns", handler);
        final TableHeaderUIFacade tableHeader = treeTable.getVisibleFields();
        for (int i=0; i<tableHeader.getSize(); i++) {
            Column column = tableHeader.getField(i);
            if (column.isVisible()) {
                addAttribute("property-id", column.getID(), attrs);
                addAttribute("order", column.getOrder(), attrs);
                addAttribute("width", column.getWidth(), attrs);
                emptyElement("displaycolumn", attrs, handler);
            }
        }
        endElement("taskdisplaycolumns", handler);
    }
}
