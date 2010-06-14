package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.Action;

import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;

public class CalculateCriticalPathAction extends GPAction implements
        RolloverAction {
    private final TaskManager taskManager;

    private final GanttTree2 tree;

    private final static String ICON_PREFIX_ON = "criticalPathOn_";

    private final static String ICON_PREFIX_OFF = "criticalPathOff_";

    private final UIConfiguration myUIConfiguration;

    private final GanttOptions myOptions;
    
    private final GanttProject appli;

    public CalculateCriticalPathAction(TaskManager taskManager, GanttTree2 tree,
            GanttOptions options, UIConfiguration uiConfiguration, GanttProject appli) {
        super(null, options.getIconSize());
        this.taskManager = taskManager;
        this.tree = tree;
        myUIConfiguration = uiConfiguration;
        myOptions = options;
        this.appli = appli;

       
    }

    protected String getIconFilePrefix() {
        return isOn() ? ICON_PREFIX_ON : ICON_PREFIX_OFF;
    }

    public void actionPerformed(ActionEvent e) {
        TaskNode root = (TaskNode) tree.getRoot();
        setOn(!isOn());
        putValue(Action.SMALL_ICON, createIcon(myOptions.getIconSize()));
        if (isOn()) {
			taskManager.processCriticalPath(root);
			ArrayList projectTasks = ((GanttTree2)tree).getProjectTasks();
	        if (projectTasks.size() != 0)
				for (int i = 0 ; i < projectTasks.size() ; i++)
					taskManager.processCriticalPath((TaskNode) projectTasks.get(i));       
        }

        Mediator.getGanttProjectSingleton().repaint();
        appli.getUIFacade().setStatusText(GanttLanguage.getInstance().getText("criticalPath"));
    }

    private void setOn(boolean on) {
        myUIConfiguration.setCriticalPathOn(on);
    }

    private boolean isOn() {
        return myUIConfiguration == null ? false : myUIConfiguration
                .isCriticalPathOn();
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

    protected String getLocalizedName() {
        return GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "criticalPath"));
    }
}
