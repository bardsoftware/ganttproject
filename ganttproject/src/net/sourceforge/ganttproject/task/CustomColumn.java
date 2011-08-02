package net.sourceforge.ganttproject.task;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;

public class CustomColumn implements CustomPropertyDefinition {
    private String id = null;

    private String name = null;

    private Object defaultValue = null;

    private final CustomColumnsManager myManager;

    private CustomPropertyClass myPropertyClass;

    CustomColumn(
            CustomColumnsManager manager, String colName,
            CustomPropertyClass propertyClass, Object colDefaultValue) {
        name = colName;
        myPropertyClass = propertyClass;
        defaultValue = colDefaultValue;
        myManager = manager;
    }

    public String getId() {
        return id;
    }

    public void setId(String newId) {
        id = newId;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }


    public void setDefaultValueAsString(String value) {
        CustomPropertyDefinition stub = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(
                getTypeAsString(), value);
        defaultValue = stub.getDefaultValue();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        myManager.fireDefinitionChanged(this, oldName);
    }

    public CustomPropertyClass getPropertyClass() {
        return myPropertyClass;
    }

    public Class<?> getType() {
        return myPropertyClass.getJavaClass();
    }

    public String toString() {
        return this.name + " [" + getType() + "] <" + this.defaultValue + ">";
    }

    public String getDefaultValueAsString() {
        return this.defaultValue==null ? null : this.defaultValue.toString();
    }

    public String getID() {
        return getId();
    }

    public String getTypeAsString() {
        return CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(getType());
    }

    public IStatus canSetPropertyClass(CustomPropertyClass propertyClass) {
        return Status.OK_STATUS;
    }

    public IStatus setPropertyClass(CustomPropertyClass propertyClass) {
        myPropertyClass = propertyClass;
        setDefaultValueAsString(getDefaultValueAsString());
        return Status.OK_STATUS;
    }
}