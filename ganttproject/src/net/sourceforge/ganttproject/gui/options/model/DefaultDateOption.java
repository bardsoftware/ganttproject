package net.sourceforge.ganttproject.gui.options.model;

import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

import java.util.Date;

public class DefaultDateOption extends GPAbstractOption<Date> implements DateOption {

    public DefaultDateOption(String id) {
        super(id);
    }

    public DefaultDateOption(String id, Date initialValue) {
        super(id, initialValue);
    }

    @Override
    public String getPersistentValue() {
        return DateParser.getIsoDateNoHours(getValue());
    }

    @Override
    public void loadPersistentValue(String value) {
        try {
            setValue(DateParser.parse(value), true);
        } catch (InvalidDateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
