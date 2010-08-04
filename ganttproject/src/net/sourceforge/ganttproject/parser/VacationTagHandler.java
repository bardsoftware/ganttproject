/**
 * 
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceManager;

import org.xml.sax.Attributes;

/**
 * @author nbohn
 */
public class VacationTagHandler implements TagHandler, ParsingListener {
    private ResourceManager myResourceManager;

    public VacationTagHandler(ResourceManager resourceManager) {
        myResourceManager = (HumanResourceManager) resourceManager;
    }

    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {

        if (qName.equals("vacation")) {
            loadResource(attrs);
        }

    }

    private void loadResource(Attributes atts) {
        try {
            // <vacation start="2005-04-14" end="2005-04-14" resourceid="0"/>
            // GanttCalendar.parseXMLDate(attrs.getValue(i)).getTime()

            String startAsString = atts.getValue("start");
            String endAsString = atts.getValue("end");
            String resourceIdAsString = atts.getValue("resourceid");
            HumanResource hr;
            hr = (HumanResource) myResourceManager.getById(Integer
                    .parseInt(resourceIdAsString));
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

    public void endElement(String namespaceURI, String sName, String qName) {
        // TODO Auto-generated method stub

    }

    public void parsingStarted() {
        // TODO Auto-generated method stub

    }

    public void parsingFinished() {
        // TODO Auto-generated method stub

    }

}
