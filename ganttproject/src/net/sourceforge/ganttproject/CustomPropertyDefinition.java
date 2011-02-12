package net.sourceforge.ganttproject;

import org.eclipse.core.runtime.IStatus;

public interface CustomPropertyDefinition {
    CustomPropertyClass getPropertyClass();
    IStatus canSetPropertyClass(CustomPropertyClass propertyClass);
    IStatus setPropertyClass(CustomPropertyClass propertyClass);
    Class getType();
    String getTypeAsString();
    String getID();
    Object getDefaultValue();
    String getName();
    void setName(String name);

    String getDefaultValueAsString();
    void setDefaultValueAsString(String value);
}