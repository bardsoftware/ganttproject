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
public class TaskPropertiesTagHandler extends AbstractTagHandler {
  private final CustomPropertyManager myCustomPropertyManager;

  public TaskPropertiesTagHandler(CustomPropertyManager customPropertyManager) {
    super("taskproperty");
    myCustomPropertyManager = customPropertyManager;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
     loadTaskProperty(attrs);
     return true;
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
}