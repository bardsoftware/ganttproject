package net.sourceforge.ganttproject.task;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyListener;
import net.sourceforge.ganttproject.CustomPropertyManager;

/**
 * This class has to be used to add or remove new custom columns. It will
 * perform the changes to the linked treetable.
 *
 * @author bbaranne (Benoit Baranne) Mar 4, 2005
 */
public class CustomColumnsManager implements CustomPropertyManager {
    private final CustomColumnsStorage myStorage;

    public CustomColumnsManager() {
        myStorage = new CustomColumnsStorage();
    }

    private void addNewCustomColumn(CustomColumn customColumn) {
        assert customColumn!=null;
        myStorage.addCustomColumn(customColumn);
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

    @Override
    public void addListener(CustomPropertyListener listener) {
        myStorage.addCustomColumnsListener(listener);
    }

    @Override
    public List<CustomPropertyDefinition> getDefinitions() {
        return new ArrayList<CustomPropertyDefinition>(myStorage.getCustomColums());
    }

    @Override
    public CustomPropertyDefinition createDefinition(String id, String typeAsString, String name,
            String defaultValueAsString) {
        CustomPropertyDefinition stub =
            CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(typeAsString, defaultValueAsString);
        CustomColumn result = new CustomColumn(this, name, stub.getPropertyClass(), stub.getDefaultValue());
        result.setId(id);
        addNewCustomColumn(result);
        return result;
    }

    @Override
    public CustomPropertyDefinition createDefinition(String typeAsString, String colName, String defValue) {
        return createDefinition("tpc"+getDefinitions().size(),typeAsString, colName, defValue);
    }

    @Override
    public void importData(CustomPropertyManager source) {
        myStorage.importData(((CustomColumnsManager)source).myStorage);
    }


    @Override
    public CustomPropertyDefinition getCustomPropertyDefinition(String id) {
        return myStorage.getCustomColumnByID(id);
    }

    @Override
    public void deleteDefinition(CustomPropertyDefinition def) {
        myStorage.removeCustomColumn(def);
    }

    void fireDefinitionChanged(int event, CustomPropertyDefinition def, CustomPropertyDefinition oldDef) {
        myStorage.fireDefinitionChanged(event, def, oldDef);
    }

    void fireDefinitionChanged(CustomPropertyDefinition def, String oldName) {
        myStorage.fireDefinitionChanged(def, oldName);
    }

    @Override
    public void reset() {
        myStorage.reset();
    }
}