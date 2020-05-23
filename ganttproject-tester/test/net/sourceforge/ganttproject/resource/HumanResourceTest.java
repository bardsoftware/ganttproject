package net.sourceforge.ganttproject.resource;

import biz.ganttproject.core.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class HumanResourceTest extends TaskTestCase {

    public void testUnpluggedClone() {

        String mail = "test@uantwerpen.be";
        String phone = "888888888";
        String description = "test description";
        String newName = "resource.copy.prefix";

        HumanResourceManager resourceManager = new HumanResourceManager(null, new CustomColumnsManager());
        HumanResource humanResource = new HumanResource("Foo", 1, resourceManager);
        humanResource.setMail(mail);
        humanResource.setDescription(description);
        humanResource.setPhone(phone);

        Date startDate = new GregorianCalendar(2020, Calendar.MAY, 20).getTime();
        Date endDate = new GregorianCalendar(2020, Calendar.MAY, 22).getTime();
        GanttDaysOff ganttDaysOff = new GanttDaysOff(startDate, endDate);
        humanResource.addDaysOff(ganttDaysOff);

        HumanResource copyOfHumanResource = humanResource.unpluggedClone();

        assertEquals(-1, copyOfHumanResource.getId());
        assertEquals(newName, copyOfHumanResource.getName());
        assertEquals(humanResource.getDescription(), copyOfHumanResource.getDescription());
        assertEquals(description, copyOfHumanResource.getDescription());
        assertEquals(humanResource.getMail(), copyOfHumanResource.getMail());
        assertEquals(mail, copyOfHumanResource.getMail());
        assertEquals(humanResource.getPhone(), copyOfHumanResource.getPhone());
        assertEquals(phone, copyOfHumanResource.getPhone());
        assertEquals(humanResource.getRole(), copyOfHumanResource.getRole());
        assertNull(copyOfHumanResource.getRole());
        assertEquals(humanResource.getStandardPayRate(), copyOfHumanResource.getStandardPayRate());
        assertEquals(ganttDaysOff, copyOfHumanResource.getDaysOff().get(0));
        assertEquals(humanResource.getCustomProperties(), copyOfHumanResource.getCustomProperties());

        // setting null values
        humanResource.setMail(null);
        humanResource.setPhone(null);
        copyOfHumanResource = humanResource.unpluggedClone();
        assertEquals(mail, copyOfHumanResource.getMail());
        assertEquals(phone, copyOfHumanResource.getPhone());
    }

    public void testCustomProperty() {

        HumanResourceManager resourceManager = new HumanResourceManager(null, new CustomColumnsManager());
        HumanResource humanResource = new HumanResource("Foo", 1, resourceManager);

        CustomPropertyDefinition propertyDefinition1 = resourceManager.getCustomPropertyManager().createDefinition(
                CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop1", null);
        CustomPropertyDefinition propertyDefinition2 = resourceManager.getCustomPropertyManager().createDefinition(
                CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop2", null);

        humanResource.addCustomProperty(propertyDefinition1, "1");
        humanResource.addCustomProperty(propertyDefinition2, "2");

        List<CustomProperty> listOfProperties = humanResource.getCustomProperties();

        assertEquals(2, listOfProperties.size());
        assertEquals(CustomColumn.class, listOfProperties.get(0).getDefinition().getClass());
        assertEquals(CustomColumn.class, listOfProperties.get(1).getDefinition().getClass());
        assertEquals(propertyDefinition2, listOfProperties.get(0).getDefinition());
        assertEquals(propertyDefinition1, listOfProperties.get(1).getDefinition());
        assertEquals("2", listOfProperties.get(0).getValue());
        assertEquals("1", listOfProperties.get(1).getValue());
        assertEquals("2", listOfProperties.get(0).getValueAsString());
        assertEquals("1", listOfProperties.get(1).getValueAsString());
    }

    public void testLoadDistribution() {
        HumanResourceManager resourceManager = new HumanResourceManager(null, new CustomColumnsManager());
        HumanResource humanResource = new HumanResource("Foo", 1, resourceManager);

        assertNotNull(humanResource.getLoadDistribution());
        humanResource.resetLoads();
        assertNotNull(humanResource.getLoadDistribution());
    }
}