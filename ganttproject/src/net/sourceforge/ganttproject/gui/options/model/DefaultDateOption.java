package net.sourceforge.ganttproject.gui.options.model;

import java.util.Date;

public class DefaultDateOption extends GPAbstractOption<Date> implements DateOption {

    public DefaultDateOption(String id) {
        super(id);
    }

    public DefaultDateOption(String id, Date initialValue) {
        super(id, initialValue);
    }

    public String getPersistentValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public void loadPersistentValue(String value) {
        // TODO Auto-generated method stub
    }

}
