/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Dmitry Barashev, GanttProject team

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
package net.sourceforge.ganttproject;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.client.RssFeedChecker;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentCreator;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.GanttLookAndFeelInfo;
import net.sourceforge.ganttproject.gui.GanttStatusBar;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.NotificationManagerImpl;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.ProjectUIFacadeImpl;
import net.sourceforge.ganttproject.gui.TaskSelectionContext;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOptionChangeListener;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.undo.UndoManagerImpl;

import org.eclipse.core.runtime.IAdaptable;

/**
 * This class is designed to be a GanttProject-after-refactorings. I am going to
 * refactor GanttProject in order to make true view communicating with other
 * views through interfaces. This class is intentionally package local to
 * prevent using it in other packages (use interfaces rather than concrete
 * implementations!)
 *
 * @author dbarashev
 */
abstract class GanttProjectBase extends JFrame implements IGanttProject, UIFacade {
    private final ViewManagerImpl myViewManager;
    private final List<ProjectEventListener> myModifiedStateChangeListeners = new ArrayList<ProjectEventListener>();
    private final UIFacadeImpl myUIFacade;
    private final GanttStatusBar statusBar;
    private final TimeUnitStack myTimeUnitStack;
    private final ProjectUIFacadeImpl myProjectUIFacade;
    private final DocumentManager myDocumentManager;
    /** The tabbed pane with the different parts of the project */
    private GanttTabbedPane myTabPane;
    private final GPUndoManager myUndoManager;
    private final CustomColumnsManager myTaskCustomColumnManager;
    private final CustomColumnsStorage myTaskCustomColumnStorage;

    private final CustomColumnsManager myResourceCustomPropertyManager =
        new CustomColumnsManager(new CustomColumnsStorage());

    private final RssFeedChecker myRssChecker;

    protected GanttProjectBase() {
        super("Gantt Chart");
        statusBar = new GanttStatusBar(this);
        myTabPane = new GanttTabbedPane();
        myViewManager = new ViewManagerImpl(myTabPane);
        addProjectEventListener(myViewManager.getProjectEventListener());
        myTimeUnitStack = new GPTimeUnitStack(getLanguage());
        NotificationManagerImpl notificationManager = new NotificationManagerImpl(getTabs().getAnimationHost());
        myUIFacade =new UIFacadeImpl(this, statusBar, notificationManager, getProject(), this);
        GPLogger.setUIFacade(myUIFacade);
        myDocumentManager = new DocumentCreator(this, getUIFacade(), null) {
            protected ParserFactory getParserFactory() {
                return GanttProjectBase.this.getParserFactory();
            }

            protected TableHeaderUIFacade getVisibleFields() {
                return getUIFacade().getTaskTree().getVisibleFields();
            }

        };
        myUndoManager = new UndoManagerImpl(this, null, myDocumentManager) {
            protected ParserFactory getParserFactory() {
                return GanttProjectBase.this.getParserFactory();
            }
        };
        myProjectUIFacade = new ProjectUIFacadeImpl(myUIFacade, myDocumentManager, myUndoManager);
        myTaskCustomColumnStorage = new CustomColumnsStorage();
        myTaskCustomColumnManager = new CustomColumnsManager(myTaskCustomColumnStorage);
        myRssChecker = new RssFeedChecker((GPTimeUnitStack) getTimeUnitStack(), myUIFacade);
    }

    private GanttLanguage getLanguage() {
        return GanttLanguage.getInstance();
    }

    public void addProjectEventListener(ProjectEventListener listener) {
        myModifiedStateChangeListeners.add(listener);
    }

    public void removeProjectEventListener(ProjectEventListener listener) {
        myModifiedStateChangeListeners.remove(listener);
    }

    protected void fireProjectModified(boolean isModified){
        for (ProjectEventListener modifiedStateChangeListener : myModifiedStateChangeListeners) {
            try {
                if (isModified) {
                    modifiedStateChangeListener.projectModified();
                } else {
                    modifiedStateChangeListener.projectSaved();
                }
            } catch (Exception e) {
                showErrorDialog(e);
            }
        }
    }

    protected void fireProjectClosed() {
        for (ProjectEventListener modifiedStateChangeListener : myModifiedStateChangeListeners) {
            modifiedStateChangeListener.projectClosed();
        }
    }

    protected void fireProjectOpened() {
        for (ProjectEventListener modifiedStateChangeListener : myModifiedStateChangeListeners) {
            modifiedStateChangeListener.projectOpened();
        }
    }


    //////////////////////////////////////////////////////////////////
    // UIFacade
    public ProjectUIFacade getProjectUIFacade() {
        return myProjectUIFacade;
    }

    public UIFacade getUIFacade() {
        return myUIFacade;
    }

    public Frame getMainFrame() {
        return myUIFacade.getMainFrame();
    }

    @Override
    public void setLookAndFeel(GanttLookAndFeelInfo laf) {
        myUIFacade.setLookAndFeel(laf);
    }

    public GanttLookAndFeelInfo getLookAndFeel() {
        return myUIFacade.getLookAndFeel();
    }

    public GPOptionGroup getOptions() {
        return myUIFacade.getOptions();
    }

    public ScrollingManager getScrollingManager() {
        return myUIFacade.getScrollingManager();
    }

    public ZoomManager getZoomManager() {
        return myUIFacade.getZoomManager();
    }

    public GPUndoManager getUndoManager() {
        return myUndoManager;
    }
    public void setStatusText(String text) {
        myUIFacade.setStatusText(text);
    }

    public Dialog createDialog(Component content, Action[] buttonActions, String title) {
        return myUIFacade.createDialog(content, buttonActions, title);
    }


    public UIFacade.Choice showConfirmationDialog(String message, String title) {
        return myUIFacade.showConfirmationDialog(message, title);
    }

    public void showOptionDialog(int messageType, String message, Action[] actions) {
        myUIFacade.showOptionDialog(messageType, message, actions);
    }

    public void showErrorDialog(String message) {
        myUIFacade.showErrorDialog(message);
    }

    public void showErrorDialog(Throwable e) {
        myUIFacade.showErrorDialog(e);
    }

    public void logErrorMessage(Throwable e) {
        myUIFacade.logErrorMessage(e);
    }

    public NotificationManager getNotificationManager() {
        return myUIFacade.getNotificationManager();
    }

    public void showPopupMenu(Component invoker, Action[] actions, int x, int y) {
        myUIFacade.showPopupMenu(invoker, actions, x, y);
    }

    public TaskSelectionContext getTaskSelectionContext() {
        return myUIFacade.getTaskSelectionContext();
    }

    public TaskSelectionManager getTaskSelectionManager() {
        return myUIFacade.getTaskSelectionManager();
    }

    public void setWorkbenchTitle(String title) {
        myUIFacade.setWorkbenchTitle(title);
    }

    protected GPViewManager getViewManager() {
        return myViewManager;
    }

    public Chart getActiveChart() {
        GPViewImpl activeView = myViewManager.mySelectedView;
        return activeView.myChart;
//        Chart resourcesChart = getResourceChart();
//        Chart ganttChart = getGanttChart();
//        Chart visibleChart = (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) ? resourcesChart
//                : ganttChart;
//        return visibleChart;
    }

    private class ViewManagerImpl implements GPViewManager {
        private final GanttTabbedPane myTabs;
        private final List<GPView> myViews = new ArrayList<GPView>();
        private GPViewImpl mySelectedView;

        ViewManagerImpl(GanttTabbedPane tabs) {
            myTabs = tabs;
            myTabs.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    GPViewImpl selectedView = (GPViewImpl) myTabs.getSelectedUserObject();
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

        public Action getCopyAction() {
            return myCopyAction;
        }

        public Action getCutAction() {
            return myCutAction;
        }

        public Action getPasteAction() {
            return myPasteAction;
        }

        ProjectEventListener getProjectEventListener() {
            return new ProjectEventListener.Stub() {
                public void projectClosed() {
                    for (int i=0; i<myViews.size(); i++) {
                        GPViewImpl nextView = (GPViewImpl) myViews.get(i);
                        nextView.reset();
                    }
                }
            };
        }

        private void updateActions() {
            ChartSelection selection = mySelectedView.myChart.getSelection();
            myCopyAction.setEnabled(false==selection.isEmpty());
            myCutAction.setEnabled(false==selection.isEmpty() && selection.isDeletable().isOK());
        }

        // FIXME The actions below are also defined in separate classes/files in ganttproject.action package -> remove actions below?
        private final GPAction myCopyAction = new GPAction() {
            {
                putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
            }
            protected String getIconFilePrefix() {
                return "copy_";
            }
            public void actionPerformed(ActionEvent e) {
                mySelectedView.myChart.getSelection().startCopyClipboardTransaction();
            }
            protected String getLocalizedName() {
                return getI18n("copy");
            }
        };

        private final GPAction myCutAction = new GPAction() {
            {
                putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
            }
            protected String getIconFilePrefix() {
                return "cut_";
            }
            public void actionPerformed(ActionEvent e) {
                mySelectedView.myChart.getSelection().startMoveClipboardTransaction();
            }
            protected String getLocalizedName() {
                return getI18n("cut");
            }
        };

        private final GPAction myPasteAction = new GPAction() {
            {
                putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
            }
            protected String getIconFilePrefix() {
                return "paste_";
            }
            public void actionPerformed(ActionEvent e) {
                ChartSelection selection = mySelectedView.myChart.getSelection();
                mySelectedView.myChart.paste(selection);
                selection.commitClipboardTransaction();
            }

            protected String getLocalizedName() {
                return getI18n("paste");
            }
        };
    }

    private class GPViewImpl implements GPView, ChartSelectionListener {
        private final GanttTabbedPane myTabs;

        private int myIndex;

        private Container myComponent;

        private boolean isVisible;

        private final Icon myIcon;

        private final Chart myChart;

        private final ViewManagerImpl myManager;

        GPViewImpl(ViewManagerImpl manager, GanttTabbedPane tabs, Container component, Chart chart, Icon icon) {
            myManager = manager;
            myTabs = tabs;
            myComponent = component;
            myIcon = icon;
            myChart = chart;
            assert myChart!=null;
        }

        public void setActive(boolean active) {
            if (active) {
                myChart.addSelectionListener(this);
            }
            else {
                myChart.removeSelectionListener(this);
            }
        }

        public void reset() {
            myChart.reset();
        }

        public void setVisible(boolean isVisible) {
            if (isVisible) {
                myChart.setTaskManager(GanttProjectBase.this.getTaskManager());
                String tabName = myChart.getName();
                myTabs.addTab(tabName, myIcon, myComponent, tabName, this);
                myTabs.setSelectedComponent(myComponent);
                myIndex = myTabs.getSelectedIndex();

            } else {
                myTabs.remove(myIndex);
            }
            this.isVisible = isVisible;
        }

        public boolean isVisible() {
            return isVisible;
        }

        public void selectionChanged() {
            myManager.updateActions();
        }
    }

    protected static class RowHeightAligner implements GPOptionChangeListener {
        private ChartModelImpl myGanttViewModel;

        private GanttTree2 myTreeView;

        // TODO: 1.12 refactor and get rid of using concrete implementations of gantt view model and tree view
        public RowHeightAligner(GanttTree2 treeView,
                ChartModelImpl ganttViewModel) {
            myGanttViewModel = ganttViewModel;
            myTreeView = treeView;
        }

        public void optionsChanged() {
            myTreeView.getTable().setRowHeight(myGanttViewModel.calculateRowHeight());
            AbstractTableModel model = (AbstractTableModel) myTreeView.getTable().getModel();
            model.fireTableStructureChanged();
            myTreeView.updateUI();
        }
    }

    public GanttTabbedPane getTabs() {
        return myTabPane;
    }

    public IGanttProject getProject() {
        return this;
    }

    public TimeUnitStack getTimeUnitStack() {
        return myTimeUnitStack;
    }

    public CustomColumnsManager getTaskCustomColumnManager() {
        return myTaskCustomColumnManager;
    }

    public CustomPropertyManager getResourceCustomPropertyManager() {
        return myResourceCustomPropertyManager;
    }

    public CustomColumnsStorage getCustomColumnsStorage() {
        return myTaskCustomColumnStorage;
    }

    protected RssFeedChecker getRssFeedChecker() {
        return myRssChecker;
    }

    public abstract String getProjectName();

    public abstract void setProjectName(String projectName);

    public abstract String getDescription();

    public abstract void setDescription(String description);

    public abstract String getOrganization();

    public abstract void setOrganization(String organization);

    public abstract String getWebLink();

    public abstract void setWebLink(String webLink);

    public abstract Task newTask();

    public abstract GanttLanguage getI18n();

    public abstract UIConfiguration getUIConfiguration();

    public abstract HumanResourceManager getHumanResourceManager();

    public abstract RoleManager getRoleManager();

    public abstract TaskManager getTaskManager();

    public abstract TaskContainmentHierarchyFacade getTaskContainment();

    public abstract GPCalendar getActiveCalendar();

    public abstract void setModified();

    public abstract void close();

    public abstract Document getDocument();

    protected GanttStatusBar getStatusBar() {
        return statusBar;
    }

    public DocumentManager getDocumentManager() {
        return myDocumentManager;
    }

    protected abstract ParserFactory getParserFactory();
}