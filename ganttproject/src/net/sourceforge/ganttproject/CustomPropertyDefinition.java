package net.sourceforge.ganttproject;

public interface CustomPropertyDefinition {
	Class getType();
	String getTypeAsString();
	String getID();
	Object getDefaultValue();
	String getName();
	String getDefaultValueAsString();
}
