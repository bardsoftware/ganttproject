package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskManager;

public class CalculateCriticalPathAction extends GPAction {
    private final TaskManager taskManager;

    private final static String ICON_PREFIX_ON = "criticalPathOn_";

    private final static String ICON_PREFIX_OFF = "criticalPathOff_";

    private final UIConfiguration myUIConfiguration;

    private final UIFacade myUiFacade;

    public CalculateCriticalPathAction(
            TaskManager taskManager, String iconSize, UIConfiguration uiConfiguration, UIFacade uiFacade) {
        super(null, iconSize);
        this.taskManager = taskManager;
        myUIConfiguration = uiConfiguration;
        myUiFacade = uiFacade;
    }

    @Override
    protected String getIconFilePrefix() {
        return isOn() ? ICON_PREFIX_ON : ICON_PREFIX_OFF;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setOn(!isOn());
        updateAction();
        if (isOn()) {
			taskManager.processCriticalPath(taskManager.getRootTask());
        }
        myUiFacade.refresh();
    }

    private void setOn(boolean on) {
        myUIConfiguration.setCriticalPathOn(on);
    }

    private boolean isOn() {
        return myUIConfiguration == null ? false : myUIConfiguration.isCriticalPathOn();
    }

    @Override
    protected String getLocalizedName() {
        return MessageFormat.format("<html><b>&nbsp;{0}&nbsp;</b></html>", getI18n(getKey()));
    }

    @Override
    protected String getKey() {
        return isOn() ? "criticalPath.action.hide" : "criticalPath.action.show";
    }
}
