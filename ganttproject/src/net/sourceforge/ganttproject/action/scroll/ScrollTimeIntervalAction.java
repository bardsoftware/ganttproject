package net.sourceforge.ganttproject.action.scroll;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.task.TaskLength;

public class ScrollTimeIntervalAction extends GPAction {
    private final ScrollingManager myScrollingManager;
    private final TaskLength myInterval;

    public ScrollTimeIntervalAction(String name, TaskLength interval, ScrollingManager scrollingManager) {
        super(name);
        myScrollingManager = scrollingManager;
        myInterval = interval;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        myScrollingManager.scrollBy(myInterval);
    }
}
