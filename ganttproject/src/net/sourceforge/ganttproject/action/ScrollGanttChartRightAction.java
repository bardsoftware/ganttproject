package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class ScrollGanttChartRightAction extends GPAction implements
        RolloverAction {
    private final ScrollingManager myScrollingManager;
	private final TaskManager myTaskManager;

    public ScrollGanttChartRightAction(ScrollingManager scrollingManager, TaskManager taskManager,
            String iconSize) {
        super("ScrollRight", iconSize);
        myScrollingManager = scrollingManager;
        myTaskManager = taskManager;
    }

    public void actionPerformed(ActionEvent e) {
        myScrollingManager.scrollBy(myTaskManager.createLength(-1));
    }

    protected String getIconFilePrefix() {
        return "next_";
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

}
