package net.sourceforge.ganttproject.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class has to be used to add or remove new custom columns. It will
 * perform the changes to the linked treetable.
 *
 * @author bbaranne (Benoit Baranne) Mar 4, 2005
 */
public class CustomColumnsManager {
    private final List myListeners;
	private final CustomColumnsStorage myStorage;

    /**
     * Creates an instance of CustomColumnsManager for the given treetable.
     *
     * @param treetable
     */
    public CustomColumnsManager(CustomColumnsStorage storage) {
        myListeners = new ArrayList();
        myStorage = storage;
    }

    public CustomColumnsStorage getStorage() {
        return myStorage;
    }

    /**
     * Add a new custom column to the treetable.
     */
    public void addNewCustomColumn(CustomColumn customColumn) {
    	assert customColumn!=null;
        myStorage.addCustomColumn(customColumn);
    }

    /**
     * Delete the custom column whose name is given in parameter from the
     * treetable.
     *
     * @param name
     *            Name of the column to delete.
     */
    public void deleteCustomColumn(String name) {
        myStorage.removeCustomColumn(name);
    }

    public void changeCustomColumnName(String oldName, String newName) {
        //ganttTreeTable.renameCustomcolumn(oldName, newName);
        myStorage.renameCustomColumn(oldName, newName);
    }

    public void changeCustomColumnDefaultValue(String colName,
            Object newDefaultValue) throws CustomColumnsException {
        // ganttTreeTable.changeDefaultValue(colName, newDefaultValue);
        myStorage.changeDefaultValue(colName, newDefaultValue);
    }

    public void addCustomColumnsListener(CustomColumsListener listener) {
        myStorage.addCustomColumnsListener(listener);
    }

}
