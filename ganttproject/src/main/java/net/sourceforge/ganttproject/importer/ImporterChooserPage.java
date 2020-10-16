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
import javax.swing.JComponent;

import org.osgi.service.prefs.Preferences;

import biz.ganttproject.core.option.GPOptionGroup;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.GPOptionChoicePanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.wizard.AbstractWizard;
import net.sourceforge.ganttproject.wizard.WizardPage;

/**
 * @author bard
 */
class ImporterChooserPage implements WizardPage {
  private final List<Importer> myImporters;
  private AbstractWizard myWizard;
  private final UIFacade myUiFacade;
  private final Preferences myPrefs;
  private int mySelectedIndex;

  ImporterChooserPage(List<Importer> importers, UIFacade uiFacade, Preferences preferences) {
    myImporters = importers;
    myUiFacade = uiFacade;
    myPrefs = preferences;
  }

  @Override
  public String getTitle() {
    return GanttLanguage.getInstance().getText("importerChooserPageTitle");
  }

  @Override
  public JComponent getComponent() {
    Action[] choiceChangeActions = new Action[myImporters.size()];
    GPOptionGroup[] choiceOptions = new GPOptionGroup[myImporters.size()];
    for (int i = 0; i < myImporters.size(); i++) {
      final Importer importer = myImporters.get(i);
      final int index = i;
      Action nextAction = new AbstractAction(importer.getFileTypeDescription()) {
        @Override
        public void actionPerformed(ActionEvent e) {
          mySelectedIndex = index;
          onSelectImporter(importer);
        }
      };
      choiceChangeActions[i] = nextAction;
      choiceOptions[i] = null;
    }
    GPOptionChoicePanel panel = new GPOptionChoicePanel();
    return panel.getComponent(choiceChangeActions, choiceOptions, 0);
  }

  protected void onSelectImporter(Importer importer) {
    assert myWizard != null : "It is a bug: importer chooser has not been initialized properly";
    WizardPage filePage = new FileChooserPage(myUiFacade, importer, myPrefs.node(importer.getID()));
    myWizard.setNextPage(filePage);
  }

  @Override
  public void setActive(AbstractWizard wizard) {
    myWizard = wizard;
    if (wizard != null) {
      onSelectImporter(myImporters.get(mySelectedIndex));
    }
  }


}
