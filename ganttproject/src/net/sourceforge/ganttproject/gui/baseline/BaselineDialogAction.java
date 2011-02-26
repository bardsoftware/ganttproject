package net.sourceforge.ganttproject.gui.baseline;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;

import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.EditableList;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class BaselineDialogAction extends GPAction {
    private IGanttProject myProject;
    private UIFacade myUiFacade;
    private List<GanttPreviousState> myBaselines;
    private List<GanttPreviousState> myTrash = new ArrayList<GanttPreviousState>();

    public BaselineDialogAction(IGanttProject project, UIFacade uiFacade) {
        super("baselineDialogAction");
        myProject = project;
        myUiFacade = uiFacade;
    }
    @Override
    public void actionPerformed(ActionEvent arg0) {
        myBaselines = new ArrayList<GanttPreviousState>(myProject.getBaselines());

        final EditableList<GanttPreviousState> list = new EditableList<GanttPreviousState>(
                myBaselines, Collections.<GanttPreviousState>emptyList()) {
            @Override
            protected GanttPreviousState updateValue(GanttPreviousState newValue, GanttPreviousState curValue) {
                curValue.setName(newValue.getName());
                return curValue;
            }

            @Override
            protected GanttPreviousState createValue(GanttPreviousState prototype) {
                try {
                    prototype.init();
                    prototype.saveFile();
                    return prototype;
                } catch (IOException e) {
                    myUiFacade.showErrorDialog(e);
                    return null;
                }
            }

            @Override
            protected GanttPreviousState createPrototype(Object editValue) {
                if (editValue == null) {
                    return null;
                }
                GanttPreviousState newBaseline = new GanttPreviousState(
                    String.valueOf(editValue), GanttPreviousState.createTasks(myProject.getTaskManager()));
                return newBaseline;
            }

            @Override
            protected void deleteValue(GanttPreviousState value) {
                for (Iterator<GanttPreviousState> it = myBaselines.iterator(); it.hasNext();) {
                    GanttPreviousState baseline = it.next();
                    if (baseline.getName().equals(value.getName())) {
                        myTrash.add(baseline);
                        break;
                    }
                }
            }

            @Override
            protected String getStringValue(GanttPreviousState baseline) {
                return baseline.getName();
            }
        };
        list.setUndefinedValueLabel(GanttLanguage.getInstance().getText("baselineDialog.undefinedValueLabel"));
        list.getTableAndActions().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.getTableAndActions().addSelectionListener(
                new AbstractTableAndActionsComponent.SelectionListener<GanttPreviousState>() {
            @Override
            public void selectionChanged(List<GanttPreviousState> selection) {
                if (selection.isEmpty()) {
                    myUiFacade.getGanttChart().setBaseline(null);
                } else {
                    myUiFacade.getGanttChart().setBaseline(selection.get(0));
                }
                myUiFacade.getGanttChart().reset();
            }
        });
        list.getTableAndActions().addAction(new GPAction("baselineDialog.hideBaselines") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                list.getTableAndActions().setSelection(null);
            }
        });
        JPanel result = new JPanel(new BorderLayout());
        result.add(list.getTableComponent(), BorderLayout.CENTER);
        result.add(list.getActionsComponent(), BorderLayout.NORTH);
        result.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        myUiFacade.showDialog(result, new Action[] {
            new OkAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    myProject.getBaselines().clear();
                    myProject.getBaselines().addAll(myBaselines);
                    for (GanttPreviousState trashBaseline : myTrash) {
                        trashBaseline.remove();
                    }
                }
            },
            new CancelAction() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                }
            }
        });
    }

    @Override
    protected String getLocalizedName() {
        return MessageFormat.format("<html><b>&nbsp;{0}&nbsp;</b></html>", super.getLocalizedName());
    }

}
