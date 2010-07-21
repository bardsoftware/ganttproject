/**
 * 
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;

import org.xml.sax.Attributes;

/**
 * @author nbohn
 */
public class HolidayTagHandler implements TagHandler, ParsingListener {
    private IGanttProject project;

    public HolidayTagHandler(IGanttProject project) {
        this.project = project;
        project.getActiveCalendar().getPublicHolidays().clear();
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(String,
     *      String, String)
     */
    public void endElement(String namespaceURI, String sName, String qName) {

    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(String,
     *      String, String, Attributes)
     */
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {

        if (qName.equals("date")) {
            loadHoliday(attrs);
        }
    }

    private void loadHoliday(Attributes atts) {
        // HumanResource hr;

        try {
            String yearAsString = atts.getValue("year");
            if (yearAsString == null)
                System.out.println("yearAsString==null");
            String monthAsString = atts.getValue("month");
            String dateAsString = atts.getValue("date");
            int month = Integer.parseInt(monthAsString);
            int date = Integer.parseInt(dateAsString);
            if (yearAsString.equals("")) {
                project.getActiveCalendar().setPublicHoliDayType(month, date);
            } else {
                int year = Integer.parseInt(yearAsString);
                project.getActiveCalendar().setPublicHoliDayType(
                        new GanttCalendar(year, month - 1, date).getTime());
            }
        } catch (NumberFormatException e) {
            System.out
                    .println("ERROR in parsing XML File year is not numeric: "
                            + e.toString());
            return;
        }

    }

    public void parsingStarted() {
        // TODO Auto-generated method stub

    }

    public void parsingFinished() {
        // TODO Auto-generated method stub

    }
}
