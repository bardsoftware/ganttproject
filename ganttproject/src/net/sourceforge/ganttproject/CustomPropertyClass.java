package net.sourceforge.ganttproject;

import java.util.GregorianCalendar;

import net.sourceforge.ganttproject.language.GanttLanguage;

public enum CustomPropertyClass {
    TEXT("text", "", String.class),
    INTEGER("integer", "0", Integer.class),
    DOUBLE("double", "0.0", Double.class),
    DATE("date", null, GregorianCalendar.class),
    BOOLEAN("boolean", "false", Boolean.class);

    private final String myI18Ntifier;
    private final Class myJavaClass;
    private final String myDefaultValue;

    private CustomPropertyClass(String i18ntifier, String defaultValue, Class javaClass) {
        myI18Ntifier = i18ntifier;
        myDefaultValue = defaultValue;
        myJavaClass  = javaClass;
    }
    public String getDisplayName() {
        return GanttLanguage.getInstance().getText(myI18Ntifier);
    }

    public Class getJavaClass() {
        return myJavaClass;
    }

    public String toString() {
        return getDisplayName();
    }

    public String getID() {
        return myI18Ntifier;
    }

    public String getDefaultValueAsString() {
        return null;
    }

    public static CustomPropertyClass fromJavaClass(Class javaClass) {
        for (CustomPropertyClass klass : CustomPropertyClass.values()) {
            if (klass.getJavaClass().equals(javaClass)) {
                return klass;
            }
        }
        return null;
    }
}