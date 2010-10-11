package net.sourceforge.ganttproject.io;

import java.util.List;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ProjectResource;

class ResourceSaver extends SaverBase {

    void save(IGanttProject project, TransformerHandler handler) throws SAXException {
        final AttributesImpl attrs = new AttributesImpl();
        startElement("resources", handler);
        saveCustomColumnDefinitions(project, handler);
        ProjectResource[] resources = project.getHumanResourceManager().getResourcesArray();
        for (int i = 0; i < resources.length; i++) {
            HumanResource p = (HumanResource) resources[i];
            addAttribute("id", p.getId(), attrs);
            addAttribute("name", p.getName(), attrs);
            addAttribute("function", p.getRole().getPersistentID(), attrs);
            addAttribute("contacts", p.getMail(), attrs);
            addAttribute("phone", p.getPhone(), attrs);
            startElement("resource", attrs, handler);
            {
            	saveCustomProperties(project, p, handler);
            }
            endElement("resource", handler);
        }
        endElement("resources", handler);
    }

	private void saveCustomProperties(IGanttProject project, HumanResource resource, TransformerHandler handler) throws SAXException {
		//CustomPropertyManager customPropsManager = project.getHumanResourceManager().getCustomPropertyManager();
		AttributesImpl attrs = new AttributesImpl();
		List<CustomProperty> properties = resource.getCustomProperties();
		for (int i=0; i<properties.size(); i++) {
			CustomProperty nextProperty = properties.get(i);
			CustomPropertyDefinition nextDefinition = nextProperty.getDefinition();
            if (nextProperty.getValue()!=null && !nextProperty.getValue().equals(nextDefinition.getDefaultValue())) {
    			addAttribute("definition-id", nextDefinition.getID(), attrs);
    			addAttribute("value", nextProperty.getValueAsString(), attrs);
    			emptyElement("custom-property", attrs, handler);
            }
		}
	}

	private void saveCustomColumnDefinitions(IGanttProject project, TransformerHandler handler) throws SAXException {
		CustomPropertyManager customPropsManager = project.getHumanResourceManager().getCustomPropertyManager();
		List/*<CustomPropertyDefinition>*/ definitions = customPropsManager.getDefinitions();
//		HumanResourceManager hrManager = (HumanResourceManager) project.getHumanResourceManager();
//		Map customFields = hrManager.getCustomFields();
//		if (customFields.size()==0) {
//			return;
//		}
        final AttributesImpl attrs = new AttributesImpl();
        //startElement("custom-properties-definition", handler);
		for (int i=0; i<definitions.size(); i++) {
			//ResourceColumn nextField = (ResourceColumn) fields.next();
			CustomPropertyDefinition nextDefinition = (CustomPropertyDefinition) definitions.get(i);
			addAttribute("id", nextDefinition.getID(), attrs);
			addAttribute("name", nextDefinition.getName(), attrs);
			addAttribute("type", nextDefinition.getTypeAsString(), attrs);
			addAttribute("default-value", nextDefinition.getDefaultValueAsString(), attrs);
			emptyElement("custom-property-definition", attrs, handler);
		}
		//endElement("custom-properties-definition", handler);
	}

	
}
