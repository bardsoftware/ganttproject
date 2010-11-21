package net.sourceforge.ganttproject.task;

public class CustomColumEvent {

    public static final int EVENT_ADD = 0;

    public static final int EVENT_REMOVE = 1;

    public static final int EVENT_REBUILD = 2;

	public static final int EVENT_RENAME = 3;
    protected final int myType;

    protected final String myColName;

    private final String myOldName;

	private final CustomColumn myColumn;

    public CustomColumEvent(int type, String colName) {
        myType = type;
        myColName = colName;
        myColumn = null;
        myOldName = colName;
    }

    public CustomColumEvent(int type, CustomColumn column) {
    	myType = type;
    	myColumn = column;
    	myColName = column.getName();
    	myOldName = myColName;
	}

    CustomColumEvent(String oldName, CustomColumn column) {
    	myOldName = oldName;
    	myType = EVENT_RENAME;
    	myColName = column.getName();
    	myColumn = column;
    }

    public CustomColumn getColumn() {
    	return myColumn;
    }

	public String getColName() {
        return myColName;
    }

    public int getType() {
        return myType;
    }

	public String getOldName() {
		return myOldName;
	}

}

