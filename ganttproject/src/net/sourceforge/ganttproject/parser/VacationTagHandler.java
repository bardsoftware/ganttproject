/**
 * 
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

import org.xml.sax.Attributes;

/**
 * @author nbohn
 */
public class VacationTagHandler implements TagHandler, ParsingListener {
    private HumanResourceManager myResourceManager;

    public VacationTagHandler(HumanResourceManager resourceManager) {
        myResourceManager = resourceManager;
    }

    private void loadResource(Attributes atts) {
        try {
            // <vacation start="2005-04-14" end="2005-04-14" resourceid="0"/>
            // GanttCalendar.parseXMLDate(attrs.getValue(i)).getTime()

            String startAsString = atts.getValue("start");
            String endAsString = atts.getValue("end");
            String resourceIdAsString = atts.getValue("resourceid");
            HumanResource hr;
			hr = myResourceManager
					.getById(Integer.parseInt(resourceIdAsString));
            hr.addDaysOff(new GanttDaysOff(GanttCalendar
                    .parseXMLDate(startAsString), GanttCalendar
                    .parseXMLDate(endAsString)));
        } catch (NumberFormatException e) {
            System.out
                    .println("ERROR in parsing XML File year is not numeric: "
                            + e.toString());
            return;
        }
    }

    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {
        if (qName.equals("vacation")) {
            loadResource(attrs);
        }
    }

    public void endElement(String namespaceURI, String sName, String qName) {
    }

    public void parsingStarted() {
    }

    public void parsingFinished() {
    }

}
