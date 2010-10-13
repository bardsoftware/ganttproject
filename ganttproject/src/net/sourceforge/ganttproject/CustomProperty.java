package net.sourceforge.ganttproject;

public interface CustomProperty {
	CustomPropertyDefinition getDefinition();
	Object getValue();
	String getValueAsString();
}
