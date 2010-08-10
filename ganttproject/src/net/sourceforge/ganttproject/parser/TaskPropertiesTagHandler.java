/*
 * Created on Mar 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;

import org.xml.sax.Attributes;

public class TaskPropertiesTagHandler implements TagHandler, ParsingListener {
	private final CustomColumnsStorage myColumnStorage;

	public TaskPropertiesTagHandler(CustomColumnsStorage columnStorage) {
    	myColumnStorage = columnStorage;
    }

    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {
        if (qName.equals("taskproperty"))
            loadTaskProperty(attrs);
    }

    public void endElement(String namespaceURI, String sName, String qName) {
    }

    private void loadTaskProperty(Attributes atts) {
        String name = atts.getValue("name");
        String id = atts.getValue("id");
        String type = atts.getValue("valuetype");

        if (atts.getValue("type").equals("custom")) {
            CustomColumn cc;
            String valueStr = atts.getValue("defaultvalue");
	            CustomPropertyDefinition stubDefinition = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(type, valueStr); 
            cc = new CustomColumn(name, stubDefinition.getType(), stubDefinition.getDefaultValue());
            cc.setId(id);

          	myColumnStorage.addCustomColumn(cc);
        }
    }

    public void parsingStarted() {
    }

    public void parsingFinished() {
    }
}
