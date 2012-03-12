/*
 * Created on 30.04.2005
 */
package net.sourceforge.ganttproject.importer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.ganttproject.gui.options.GPOptionChoicePanel;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
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
