package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.task.CustomPropertyEvent;

public interface CustomPropertyListener {
    public void customPropertyChange(CustomPropertyEvent event);
}