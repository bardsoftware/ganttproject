/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.importer;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

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
        return GanttLanguage.getInstance().getCorrectedLabel("import");
    }
}
