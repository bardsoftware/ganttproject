/*
 * Created on 30.04.2005
 */
package net.sourceforge.ganttproject.importer;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;

/**
 * @author bard
 */
public class ImportFileAction extends GPAction {

    private final UIFacade myUIFacade;

    private final GanttProject myProject;

    public ImportFileAction(UIFacade uiFacade, GanttProject project) {
        super(null, "16");
        myUIFacade = uiFacade;
        myProject = project;
    }

    public void actionPerformed(ActionEvent e) {
        ImportFileWizardImpl wizard = new ImportFileWizardImpl(
                myUIFacade, myProject, myProject.getGanttOptions());
        wizard.show();
    }

    protected String getIconFilePrefix() {
        return "import_";
    }

    public void isIconVisible(boolean isNull) {
    }

    protected String getLocalizedName() {
        return getI18n("import");
    }
}
