package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.CustomPropertyDefinition;

public class CustomPropertyEvent {

    public static final int EVENT_ADD = 0;

    public static final int EVENT_REMOVE = 1;

    public static final int EVENT_REBUILD = 2;

    public static final int EVENT_NAME_CHANGE = 3;

    public static final int EVENT_TYPE_CHANGE = 4;

    private final int myType;


    private CustomPropertyDefinition myDefinition;

    private CustomPropertyDefinition myOldDef;

    public CustomPropertyEvent(int type, CustomPropertyDefinition definition) {
        myType = type;
        myDefinition = definition;
    }

    public CustomPropertyEvent(int type, CustomPropertyDefinition def, CustomPropertyDefinition oldDef) {
        myType = type;
        myDefinition = def;
        myOldDef = oldDef;
    }

    public CustomPropertyDefinition getDefinition() {
        return myDefinition;
    }

    public CustomPropertyDefinition getOldValue() {
        return myOldDef;
    }

    public String getOldName() {
        return myOldDef.getName();
    }

    public String getColName() {
        return myDefinition.getName();
    }

    public int getType() {
        return myType;
    }

}