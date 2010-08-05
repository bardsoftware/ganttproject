package net.sourceforge.ganttproject.gui.options.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class GPAbstractOption implements GPOption, ChangeValueDispatcher {
    private final String myID;

    protected boolean isLocked;

    private List myListeners = new ArrayList();
    
    protected GPAbstractOption(String id) {
        myID = id;
    }

    public String getID() {
        return myID;
    }

    public void lock() {
        if (isLocked) {
            throw new IllegalStateException("Already locked");
        }
        isLocked = true;
    }

    public void commit() {
        if (!isLocked()) {
            throw new IllegalStateException("Can't commit not locked option");
        }
        setLocked(false);

    }

    public void rollback() {
        if (!isLocked()) {
            throw new IllegalStateException("Can't rollback not locked option");
        }
        setLocked(false);
    }

    protected boolean isLocked() {
        return isLocked;
    }

    protected void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }
    
    public void addChangeValueListener(ChangeValueListener listener) {
        myListeners.add(listener);
    }

    protected void fireChangeValueEvent(ChangeValueEvent event) {
        Iterator it = myListeners.iterator();
        while (it.hasNext()) {
            ((ChangeValueListener) it.next()).changeValue(event);
        }
    }
    

}
