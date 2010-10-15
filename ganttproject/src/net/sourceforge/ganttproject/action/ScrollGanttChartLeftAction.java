package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class ScrollGanttChartLeftAction extends GPAction implements
        RolloverAction {
    private final ScrollingManager myScrollingManager;

    public ScrollGanttChartLeftAction(ScrollingManager scrollingManager,
            String iconSize) {
        super("ScrollLeft", iconSize);
        myScrollingManager = scrollingManager;
    }

    public void actionPerformed(ActionEvent e) {
        myScrollingManager.scrollLeft(1);
    }

    protected String getIconFilePrefix() {
        return "prev_";
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

}
