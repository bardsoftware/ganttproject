package net.sourceforge.ganttproject.customProperty;

import java.util.List;

import biz.ganttproject.customproperty.CustomPropertyClass;
import biz.ganttproject.customproperty.CustomPropertyDefinition;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.task.CustomColumnsManager;

public class CustomPropertyImportTest extends TestCase {
    public void testImportDuplicatedProperties() {
        {
            CustomColumnsManager target = new CustomColumnsManager();
            target.createDefinition(CustomPropertyClass.TEXT, "col1", null);
            target.createDefinition(CustomPropertyClass.TEXT, "col2", null);

            CustomColumnsManager source = new CustomColumnsManager();
            source.createDefinition(CustomPropertyClass.TEXT, "col1", null);
            source.createDefinition(CustomPropertyClass.TEXT, "col3", null);

            target.importData(source);
            List<CustomPropertyDefinition> definitions = target.getDefinitions();
            assertEquals(3, definitions.size());
        }
        {
            CustomColumnsManager target = new CustomColumnsManager();
            target.createDefinition(CustomPropertyClass.TEXT, "col1", null);
            target.createDefinition(CustomPropertyClass.TEXT, "col2", null);

            CustomColumnsManager source = new CustomColumnsManager();
            source.createDefinition(CustomPropertyClass.DATE, "col1", null);
            source.createDefinition(CustomPropertyClass.TEXT, "col3", null);

            target.importData(source);
            List<CustomPropertyDefinition> definitions = target.getDefinitions();
            assertEquals(4, definitions.size());
        }
    }
}
