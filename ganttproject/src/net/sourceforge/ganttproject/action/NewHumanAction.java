package net.sourceforge.ganttproject.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.resource.ResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;

/**
 * Action connected to the menu item for insert a new resource
 */
public class NewHumanAction extends ResourceAction {
    // TODO Field is never read locally... remove?
    private final RoleManager myRoleManager;

    private final GanttProject myProject;

    public NewHumanAction(ResourceManager hrManager, RoleManager roleManager,
            JFrame projectFrame, GanttProject project) {
        super(hrManager);
        myRoleManager = roleManager;
        myProjectFrame = projectFrame;
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
        // final HumanResource people =
        // ((HumanResourceManager)getManager()).newHumanResource();
        // people.setRole(myRoleManager.getDefaultRole());
        // GanttDialogPerson dp = new GanttDialogPerson(getProjectFrame(),
        // getLanguage(), people);
        // dp.show();
        // if(dp.result()) {
        //
        // myProject.getUndoManager().undoableEdit("new Resource", new
        // Runnable(){
        // public void run() {
        // getManager().add(people);
        // }});
        // myProject.quickSave ("new Resource");
        // }
        myProject.newHumanResource();
    }

    public void languageChanged() {
        this.putValue(AbstractAction.NAME, GanttProject
                .correctLabel(getLanguage().getText("newHuman")));
    }

    private JFrame getProjectFrame() {
        return myProjectFrame;
    }

    private final int MENU_MASK = Toolkit.getDefaultToolkit()
            .getMenuShortcutKeyMask();

    private JFrame myProjectFrame;
}
