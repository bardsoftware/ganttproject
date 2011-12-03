/*
 * Created on 03.05.2005
 */
package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.ganttproject.export.ExportFileWizardImpl.State;
import net.sourceforge.ganttproject.gui.options.GPOptionChoicePanel;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
class ExporterChooserPage implements WizardPage {
    private final List<Exporter> myExporters;

    private final State myState;
    private final GanttLanguage language = GanttLanguage.getInstance();

    ExporterChooserPage(List<Exporter> exporters, ExportFileWizardImpl.State state) {
        myExporters = exporters;
        myState = state;

    }

    @Override
    public String getTitle() {
        return language.getText("option.exporter.title");
    }

    @Override
    public Component getComponent() {
        int selectedGroupIndex = 0;
        Action[] choiceChangeActions = new Action[myExporters.size()];
        GPOptionGroup[] choiceOptions = new GPOptionGroup[myExporters.size()];
        for (int i = 0; i < myExporters.size(); i++) {
            final Exporter nextExporter = myExporters.get(i);
            if (nextExporter==myState.getExporter()) {
                selectedGroupIndex = i;
            }
            Action nextAction = new AbstractAction(nextExporter
                    .getFileTypeDescription()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ExporterChooserPage.this.myState.setExporter(nextExporter);
                }
            };
            GPOptionGroup nextOptions = nextExporter.getOptions();
            if (nextOptions!=null) {
                nextOptions.lock();
            }
            choiceChangeActions[i] = nextAction;
            choiceOptions[i] = nextOptions;
        }
        GPOptionChoicePanel choicePanel = new GPOptionChoicePanel();
        return choicePanel.getComponent(choiceChangeActions, choiceOptions, selectedGroupIndex);
    }

    @Override
    public void setActive(boolean b) {
        if (false==b) {
            for (Exporter e : myExporters) {
                if (e.getOptions()!=null) {
                    e.getOptions().commit();
                }
            }
        }
        else {
            for (Exporter e : myExporters) {
                if (e.getOptions()!=null) {
                    e.getOptions().lock();
                }
            }

        }
    }

}
