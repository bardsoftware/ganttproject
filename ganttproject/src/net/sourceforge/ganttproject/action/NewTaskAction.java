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
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class NewTaskAction extends AbstractAction implements
        GanttLanguage.Listener {

    private final IGanttProject myProject;

    public NewTaskAction(IGanttProject project) {
        myProject = project;
        setText(project.getI18n());
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        URL iconUrl = this.getClass().getClassLoader().getResource(
                "icons/insert_16.gif");
        if (iconUrl != null) {
            this.putValue(Action.SMALL_ICON, new ImageIcon(iconUrl));
        }
        project.getI18n().addListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        Mediator.getUndoManager().undoableEdit("New Task", new Runnable() {
            public void run() {
                myProject.newTask();
            }
        });

    }

    public void languageChanged(GanttLanguage.Event event) {
        setText(event.getLanguage());
    }

    /**
     * @param language
     */
    private void setText(GanttLanguage language) {
        this.putValue(AbstractAction.NAME, GanttProject.correctLabel(language
                .getText("newTask")));
    }

}
