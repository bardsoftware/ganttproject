package net.sourceforge.ganttproject.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

/**
 * Action connected to the menu item for insert a new resource
 */
public class NewHumanAction extends ResourceAction {
    private final GanttProject myProject;

    public NewHumanAction(HumanResourceManager hrManager, GanttProject project) {
        super(hrManager);
        myProject = project;

        this.putValue(AbstractAction.NAME, GanttProject
                .correctLabel(getLanguage().getText("newHuman")));
        this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                KeyEvent.VK_H, MENU_MASK));
        URL iconUrl = this.getClass().getClassLoader().getResource(
                "icons/insert_16.gif");
        if (iconUrl != null) {
            this.putValue(Action.SMALL_ICON, new ImageIcon(iconUrl));
        }
    }

    public void actionPerformed(ActionEvent event) {
        myProject.newHumanResource();
    }

    public void languageChanged() {
        this.putValue(AbstractAction.NAME, GanttProject
                .correctLabel(getLanguage().getText("newHuman")));
    }

    private final int MENU_MASK = Toolkit.getDefaultToolkit()
            .getMenuShortcutKeyMask();;
}
