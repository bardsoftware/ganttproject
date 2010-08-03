/*
 * Created on 03.05.2005
 */
package net.sourceforge.ganttproject.export;

import java.awt.Component;
import java.awt.event.ActionEvent;

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
    private final Exporter[] myExporters;

    private final State myState;
    private final GanttLanguage language = GanttLanguage.getInstance();

    /**
     *
     */
    ExporterChooserPage(Exporter[] exporters, ExportFileWizardImpl.State state) {
        myExporters = exporters;
        myState = state;

    }

    public String getTitle() {
        return language.getText("option.exporter.title");
    }

    public Component getComponent() {
        int selectedGroupIndex = 0;
        Action[] choiceChangeActions = new Action[myExporters.length];
        GPOptionGroup[] choiceOptions = new GPOptionGroup[myExporters.length];
        for (int i = 0; i < myExporters.length; i++) {
            final Exporter nextExporter = myExporters[i];
            if (nextExporter==myState.getExporter()) {
                selectedGroupIndex = i;
            }
            Action nextAction = new AbstractAction(nextExporter
                    .getFileTypeDescription()) {
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

    public void setActive(boolean b) {
        if (false==b) {
            for (int i=0; i<myExporters.length; i++) {
                if (myExporters[i].getOptions()!=null) {
                    myExporters[i].getOptions().commit();
                }
            }
        }
        else {
            for (int i=0; i<myExporters.length; i++) {
                if (myExporters[i].getOptions()!=null) {
                    myExporters[i].getOptions().lock();
                }
            }

        }
    }

}
