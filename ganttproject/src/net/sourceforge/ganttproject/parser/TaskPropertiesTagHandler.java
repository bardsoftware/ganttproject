/*
 * Created on Mar 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.CustomPropertyManager;

import org.xml.sax.Attributes;

/**
 * @author bbaranne Mar 10, 2005
 */
public class TaskPropertiesTagHandler implements TagHandler, ParsingListener {
    private final CustomPropertyManager myCustomPropertyManager;

    public TaskPropertiesTagHandler(CustomPropertyManager customPropertyManager) {
        myCustomPropertyManager = customPropertyManager;
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(String,
     *      String, String, Attributes)
     */
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {
        if (qName.equals("taskproperty"))
            loadTaskProperty(attrs);
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(String,
     *      String, String)
     */
    public void endElement(String namespaceURI, String sName, String qName) {
        // System.out.println(Mediator.getCustomColumnsStorage().toString());
    }

    private void loadTaskProperty(Attributes atts) {
        String name = atts.getValue("name");
        String id = atts.getValue("id");
        String type = atts.getValue("valuetype");

        if (atts.getValue("type").equals("custom")) {
            String valueStr = atts.getValue("defaultvalue");
            myCustomPropertyManager.createDefinition(id, type, name, valueStr);
        }
    }

    /**
     * @see net.sourceforge.ganttproject.parser.ParsingListener#parsingStarted()
     */
    public void parsingStarted() {
        // nothing to do.

    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.ganttproject.parser.ParsingListener#parsingFinished()
     */
    public void parsingFinished() {
        // this.treeTable.setDisplayedColumns(columns);
    }
}