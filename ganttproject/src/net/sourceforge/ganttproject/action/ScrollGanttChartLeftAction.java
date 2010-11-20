package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class ScrollGanttChartLeftAction extends GPAction implements
        RolloverAction {
    private final ScrollingManager myScrollingManager;
	private final TaskManager myTaskManager;

    public ScrollGanttChartLeftAction(ScrollingManager scrollingManager, TaskManager taskManager,
            String iconSize) {
        super("ScrollLeft", iconSize);
        myScrollingManager = scrollingManager;
        myTaskManager = taskManager;
    }

    public void actionPerformed(ActionEvent e) {
        myScrollingManager.scrollBy(myTaskManager.createLength(1));
    }

    protected String getIconFilePrefix() {
        return "prev_";
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

}
