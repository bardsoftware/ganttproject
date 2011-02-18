package net.sourceforge.ganttproject.gui.baseline;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JLabel;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;

public class BaselineDialogAction extends GPAction {
    private IGanttProject myProject;
    private UIFacade myUiFacade;
    public BaselineDialogAction(IGanttProject project, UIFacade uiFacade) {
        super("baselineDialogAction");
        myProject = project;
        myUiFacade = uiFacade;
    }
    @Override
    public void actionPerformed(ActionEvent arg0) {
        myUiFacade.showDialog(new JLabel("Baselines will be here"), new Action[] {
            new OkAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            }
        });
    }
}
