package net.sourceforge.ganttproject.customProperty;

import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.task.CustomColumnsManager;

public class CustomPropertyImportTest extends TestCase {
    public void testImportDuplicatedProperties() {
        {
            CustomColumnsManager target = new CustomColumnsManager();
            target.createDefinition(CustomPropertyClass.TEXT.getID(), "col1", null);
            target.createDefinition(CustomPropertyClass.TEXT.getID(), "col2", null);

            CustomColumnsManager source = new CustomColumnsManager();
            source.createDefinition(CustomPropertyClass.TEXT.getID(), "col1", null);
            source.createDefinition(CustomPropertyClass.TEXT.getID(), "col3", null);

            target.importData(source);
            List<CustomPropertyDefinition> definitions = target.getDefinitions();
            assertEquals(3, definitions.size());
        }
        {
            CustomColumnsManager target = new CustomColumnsManager();
            target.createDefinition(CustomPropertyClass.TEXT.getID(), "col1", null);
            target.createDefinition(CustomPropertyClass.TEXT.getID(), "col2", null);

            CustomColumnsManager source = new CustomColumnsManager();
            source.createDefinition(CustomPropertyClass.DATE.getID(), "col1", null);
            source.createDefinition(CustomPropertyClass.TEXT.getID(), "col3", null);

            target.importData(source);
            List<CustomPropertyDefinition> definitions = target.getDefinitions();
            assertEquals(4, definitions.size());
        }
    }
}
