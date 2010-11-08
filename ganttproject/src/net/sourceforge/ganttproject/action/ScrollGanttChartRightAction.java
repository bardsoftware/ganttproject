package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class ScrollGanttChartRightAction extends GPAction implements
        RolloverAction {
    private final ScrollingManager myScrollingManager;

    public ScrollGanttChartRightAction(ScrollingManager scrollingManager,
            String iconSize) {
        super("ScrollRight", iconSize);
        myScrollingManager = scrollingManager;
    }

    public void actionPerformed(ActionEvent e) {
        myScrollingManager.scrollBy(-1);
    }

    protected String getIconFilePrefix() {
        return "next_";
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

}
