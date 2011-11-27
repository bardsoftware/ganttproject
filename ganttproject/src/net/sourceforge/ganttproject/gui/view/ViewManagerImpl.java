package net.sourceforge.ganttproject.gui.view;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.ProjectEventListener.Stub;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.edit.CopyAction;
import net.sourceforge.ganttproject.action.edit.CutAction;
import net.sourceforge.ganttproject.action.edit.PasteAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;

import org.eclipse.core.runtime.IAdaptable;

public class ViewManagerImpl implements GPViewManager {
    private final GanttTabbedPane myTabs;
    private final List<GPView> myViews = new ArrayList<GPView>();
    GPView mySelectedView;

    private final CopyAction myCopyAction;
    private final CutAction myCutAction;
    private final PasteAction myPasteAction;

    public ViewManagerImpl(IGanttProject project, GanttTabbedPane tabs) {
        myTabs = tabs;
        project.addProjectEventListener(getProjectEventListener());
        // Create actions
        myCopyAction = new CopyAction(this);
        myCutAction = new CutAction(this);
        myPasteAction = new PasteAction(this);

        myTabs.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                GPView selectedView = (GPView) myTabs.getSelectedUserObject();
                if (mySelectedView == selectedView) {
                    return;
                }
                if (mySelectedView != null) {
                    mySelectedView.setActive(false);
                }
                mySelectedView = selectedView;
                mySelectedView.setActive(true);
            }
        });
    }

    public GPView createView(IAdaptable adaptable, Icon icon) {
        GPView view = new GPViewImpl(this, myTabs, (Container) adaptable
                .getAdapter(Container.class), (Chart)adaptable.getAdapter(Chart.class), icon);
        myViews.add(view);
        return view;
    }

    public GPAction getCopyAction() {
        return myCopyAction;
    }

    public GPAction getCutAction() {
        return myCutAction;
    }

    public GPAction getPasteAction() {
        return myPasteAction;
    }

    public ChartSelection getSelectedArtefacts() {
        return mySelectedView.getChart().getSelection();
    }

    ProjectEventListener getProjectEventListener() {
        return new ProjectEventListener.Stub() {
            @Override
            public void projectClosed() {
                for (int i=0; i<myViews.size(); i++) {
                    GPViewImpl nextView = (GPViewImpl) myViews.get(i);
                    nextView.reset();
                }
            }
        };
    }

    void updateActions() {
        ChartSelection selection = mySelectedView.getChart().getSelection();
        myCopyAction.setEnabled(false==selection.isEmpty());
        myCutAction.setEnabled(false==selection.isEmpty() && selection.isDeletable().isOK());
    }

    public Chart getActiveChart() {
        return mySelectedView.getChart();
    }

    public void activateNextView() {
        myTabs.setSelectedIndex((myTabs.getSelectedIndex() + 1) % myTabs.getTabCount());
    }

    public GPView getSelectedView() {
        return mySelectedView;
    }
}