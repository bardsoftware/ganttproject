package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.CustomPropertyDefinition;

public class CustomPropertyEvent {

    public static final int EVENT_ADD = 0;

    public static final int EVENT_REMOVE = 1;

    public static final int EVENT_REBUILD = 2;

    public static final int EVENT_PROPERTY_CHANGE = 3;
    private final int myType;


    private CustomPropertyDefinition myDefinition;

    private String myOldName;

    public CustomPropertyEvent(int type, CustomPropertyDefinition definition) {
        myType = type;
        myDefinition = definition;
    }

    public CustomPropertyEvent(int type, CustomPropertyDefinition def, String oldName) {
        myType = type;
        myDefinition = def;
        myOldName = oldName;
    }

    public CustomPropertyDefinition getDefinition() {
        return myDefinition;
    }

    public String getOldName() {
        return myOldName;
    }

    public String getColName() {
        return myDefinition.getName();
    }

    public int getType() {
        return myType;
    }

}