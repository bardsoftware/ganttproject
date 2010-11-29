package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;

public class SwitchViewAction extends AbstractAction {
    private JTabbedPane tabbedPane = null;

    public SwitchViewAction(GanttProject project) {
        tabbedPane = project.getTabs();
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F7,
                GPAction.MENU_MASK));
    }

    public void actionPerformed(ActionEvent arg0) {
        tabbedPane.setSelectedIndex((tabbedPane.getSelectedIndex() + 1)
                % tabbedPane.getTabCount());
    }

}
