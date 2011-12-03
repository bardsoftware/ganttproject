package net.sourceforge.ganttproject;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

public interface CustomPropertyManager {
    List<CustomPropertyDefinition> getDefinitions();
    CustomPropertyDefinition createDefinition(String id, String typeAsString, String name, String defaultValueAsString);
    CustomPropertyDefinition createDefinition(String typeAsString, String colName, String defValue);
    CustomPropertyDefinition getCustomPropertyDefinition(String id);
    void deleteDefinition(CustomPropertyDefinition def);
    void importData(CustomPropertyManager source);
    void addListener(CustomPropertyListener listener);
    class PropertyTypeEncoder {
        public static String encodeFieldType(Class<?> fieldType) {
            String result = null;
            if (fieldType.equals(String.class)) {
                result = "text";
            }
            else if (fieldType.equals(Boolean.class)) {
                result = "boolean";
            }
            else if (fieldType.equals(Integer.class)) {
                result = "int";
            }
            else if (fieldType.equals(Double.class)) {
                result = "double";
            }
            else if (fieldType.isAssignableFrom(GregorianCalendar.class)) {
                result = "date";
            }
            return result;
        }

        public static CustomPropertyDefinition decodeTypeAndDefaultValue(final String typeAsString, final String valueAsString) {
            final CustomPropertyClass propertyClass;
            final Object defaultValue;
            if (typeAsString.equals("text")) {
                propertyClass = CustomPropertyClass.TEXT;
                defaultValue = valueAsString==null ? null : valueAsString.toString();
            } else if (typeAsString.equals("boolean")) {
                propertyClass = CustomPropertyClass.BOOLEAN;
                defaultValue = valueAsString==null ? null : Boolean.valueOf(valueAsString);
            } else if (typeAsString.equals("int") || "integer".equals(typeAsString)) {
                propertyClass = CustomPropertyClass.INTEGER;
                Integer intValue;
                try {
                    intValue = valueAsString==null ? null : Integer.valueOf(valueAsString);
                }
                catch (NumberFormatException e) {
                    intValue = null;
                }
                defaultValue = intValue;
            } else if (typeAsString.equals("double")) {
                propertyClass = CustomPropertyClass.DOUBLE;
                Double doubleValue;
                try {
                    doubleValue = valueAsString==null ? null : Double.valueOf(valueAsString);
                }
                catch (NumberFormatException e) {
                    doubleValue = null;
                }
                defaultValue = doubleValue;
            } else if (typeAsString.equals("date")) {
                propertyClass = CustomPropertyClass.DATE;
                if (valueAsString==null) {
                    defaultValue = null;
                }
                else {
                    Date defaultDate;
                    try {
                        defaultDate = DateParser.parse(valueAsString);
                    } catch (InvalidDateException e) {
                        defaultDate = null;
                    }
                    defaultValue = defaultDate;
                }
            } else {
                propertyClass = CustomPropertyClass.TEXT;
                defaultValue = "";
            }
            return new CustomPropertyDefinition() {
                @Override
                public Object getDefaultValue() {
                    return defaultValue;
                }
                @Override
                public String getDefaultValueAsString() {
                    return valueAsString;
                }
                @Override
                public void setDefaultValueAsString(String value) {
                    throw new UnsupportedOperationException();
                }
                @Override
                public String getID() {
                    return null;
                }
                @Override
                public String getName() {
                    return null;
                }
                @Override
                public void setName(String name) {
                    throw new UnsupportedOperationException();
                }
                @Override
                public Class<?> getType() {
                    return propertyClass.getJavaClass();
                }
                @Override
                public String getTypeAsString() {
                    return typeAsString;
                }
                @Override
                public CustomPropertyClass getPropertyClass() {
                    return propertyClass;
                }
                @Override
                public IStatus canSetPropertyClass(CustomPropertyClass propertyClass) {
                    return Status.CANCEL_STATUS;
                }
                @Override
                public IStatus setPropertyClass(CustomPropertyClass propertyClass) {
                    throw new UnsupportedOperationException();
                }

            };
        }

    }
}