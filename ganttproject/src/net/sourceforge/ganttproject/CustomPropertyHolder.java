package net.sourceforge.ganttproject;

import java.util.List;

public interface CustomPropertyHolder {
	List<CustomProperty> getCustomProperties();
	CustomProperty addCustomProperty(CustomPropertyDefinition  definition, String defaultValueAsString);
}
