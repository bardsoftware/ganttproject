package net.sourceforge.ganttproject;

import java.util.GregorianCalendar;

import net.sourceforge.ganttproject.language.GanttLanguage;

public enum CustomPropertyClass {
    TEXT("text", String.class),
    INTEGER("integer", Integer.class),
    DOUBLE("double", Double.class),
    DATE("date", GregorianCalendar.class),
    BOOLEAN("boolean", Boolean.class);

    private final String myI18Ntifier;
    private final Class myJavaClass;

    private CustomPropertyClass(String i18ntifier, Class javaClass) {
        myI18Ntifier = i18ntifier;
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
    public static CustomPropertyClass fromJavaClass(Class javaClass) {
        for (CustomPropertyClass klass : CustomPropertyClass.values()) {
            if (klass.getJavaClass().equals(javaClass)) {
                return klass;
            }
        }
        return null;
    }
}