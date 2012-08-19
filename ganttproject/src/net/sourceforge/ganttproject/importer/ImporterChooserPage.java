/*
Copyright 2005-2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.importer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.gui.options.GPOptionChoicePanel;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;
import net.sourceforge.ganttproject.importer.ImportFileWizardImpl.State;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
class ImporterChooserPage implements WizardPage {
  private final List<Importer> myImporters;

  private final State myState;

  ImporterChooserPage(List<Importer> importers, State state) {
    myImporters = importers;
    myState = state;
  }

  @Override
  public String getTitle() {
    return GanttLanguage.getInstance().getText("importerChooserPageTitle");
  }

  @Override
  public Component getComponent() {
    Action[] choiceChangeActions = new Action[myImporters.size()];
    GPOptionGroup[] choiceOptions = new GPOptionGroup[myImporters.size()];
    for (int i = 0; i < myImporters.size(); i++) {
      final Importer nextImporter = myImporters.get(i);
      Action nextAction = new AbstractAction(nextImporter.getFileTypeDescription()) {
        @Override
        public void actionPerformed(ActionEvent e) {
          ImporterChooserPage.this.myState.myImporter = nextImporter;
        }
      };
      choiceChangeActions[i] = nextAction;
      choiceOptions[i] = null;
      if (i == 0) {
        myState.myImporter = nextImporter;
      }
    }
    GPOptionChoicePanel panel = new GPOptionChoicePanel();
    return panel.getComponent(choiceChangeActions, choiceOptions, 0);
  }

  @Override
  public void setActive(boolean b) {
  }

}
