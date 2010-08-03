package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.chart.GanttChart;

import org.xml.sax.Attributes;

public class BlankLineTagHandler implements TagHandler {

    private final GanttChart myGanttChart;

    public BlankLineTagHandler(GanttChart chart) {
        myGanttChart = chart;
    }

    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) throws FileFormatException {
        if (qName.equals("blankline")) {
            //myGanttChart.appendBlankRow();
        }

    }

    public void endElement(String namespaceURI, String sName, String qName) {
        // TODO Auto-generated method stub

    }
}
