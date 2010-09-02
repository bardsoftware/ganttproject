package net.sourceforge.ganttproject;

import java.util.GregorianCalendar;
import java.util.List;

import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

public interface CustomPropertyManager {
	List<CustomPropertyDefinition> getDefinitions();
	CustomPropertyDefinition createDefinition(String id, String typeAsString, String name, String defaultValueAsString);
	void importData(CustomPropertyManager source);

	class PropertyTypeEncoder {
	    public static String encodeFieldType(Class fieldType) {
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
			final Class type;
			final Object defaultValue;
            if (typeAsString.equals("text")) {
                type = String.class;
                defaultValue = valueAsString==null ? null : valueAsString.toString();
            } else if (typeAsString.equals("boolean")) {
                type = Boolean.class;
                defaultValue = valueAsString==null ? null : Boolean.valueOf(valueAsString);
            } else if (typeAsString.equals("int")) {
                type = Integer.class;
                defaultValue = valueAsString==null ? null : Integer.valueOf(valueAsString);
            } else if (typeAsString.equals("double")) {
                type = Double.class;
                defaultValue = valueAsString==null ? null : Double.valueOf(valueAsString);
            } else if (typeAsString.equals("date")) {
                type = GregorianCalendar.class;
                if (valueAsString!=null) {
	                GanttCalendar c = null;
	                try {
	                    c = new GanttCalendar(DateParser.parse(valueAsString));
	                } catch (InvalidDateException e) {
	                	if (!GPLogger.log(e)) {
	                		e.printStackTrace(System.err);
	                	}
	                }
	                defaultValue = c;
                }
                else {
                	defaultValue = null;
                }
            } else {
                type = String.class; // TODO remove if(...text)
                defaultValue = "";
            }
            return new CustomPropertyDefinition() {
				public Object getDefaultValue() {
					return defaultValue;
				}
				public String getDefaultValueAsString() {
					return valueAsString;
				}
				public String getID() {
					return null;
				}
				public String getName() {
					return null;
				}
				public Class getType() {
					return type;
				}
				public String getTypeAsString() {
					return typeAsString;
				}
            };
		}

	}
}
