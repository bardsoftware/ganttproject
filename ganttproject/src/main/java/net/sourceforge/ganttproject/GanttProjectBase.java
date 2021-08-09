/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Dmitry Barashev, GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionChangeListener;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import biz.ganttproject.ganttview.TaskTable;
import biz.ganttproject.ganttview.TaskTableActionConnector;
import biz.ganttproject.ganttview.TaskTableChartConnector;
import biz.ganttproject.lib.fx.SimpleTreeCollapseView;
import biz.ganttproject.lib.fx.TreeCollapseView;
import biz.ganttproject.task.TaskActions;
import com.bardsoftware.eclipsito.update.Updater;
import com.google.common.base.Suppliers;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import kotlin.jvm.functions.Function0;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.client.RssFeedChecker;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentCreator;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.GanttLookAndFeelInfo;
import net.sourceforge.ganttproject.gui.GanttStatusBar;
import net.sourceforge.ganttproject.gui.GanttTabbedPane;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.NotificationManagerImpl;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.ProjectUIFacadeImpl;
import net.sourceforge.ganttproject.gui.TaskSelectionContext;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.gui.view.ViewManagerImpl;
import net.sourceforge.ganttproject.gui.window.ContentPaneBuilder;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskView;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.undo.UndoManagerImpl;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

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
  protected final static GanttLanguage language = GanttLanguage.getInstance();
  protected final WeekendCalendarImpl myCalendar = new WeekendCalendarImpl();
  private final ViewManagerImpl myViewManager;
  private final List<ProjectEventListener> myModifiedStateChangeListeners = new ArrayList<ProjectEventListener>();
  private final UIFacadeImpl myUIFacade;
  private final GanttStatusBar statusBar;
  private final TimeUnitStack myTimeUnitStack = new GPTimeUnitStack();;
  private final ProjectUIFacadeImpl myProjectUIFacade;
  private final DocumentManager myDocumentManager;
  protected final SimpleObjectProperty<Document> myObservableDocument = new SimpleObjectProperty<>();
  /** The tabbed pane with the different parts of the project */
  private final GanttTabbedPane myTabPane;
  private final GPUndoManager myUndoManager;

  private final CustomColumnsManager myResourceCustomPropertyManager = new CustomColumnsManager();

  private final RssFeedChecker myRssChecker;
  protected final ContentPaneBuilder myContentPaneBuilder;
  final TaskManagerConfigImpl myTaskManagerConfig;
  private final TaskManager myTaskManager;
  private Updater myUpdater;
  protected final TaskActions myTaskActions;

  TaskTableChartConnector myTaskTableChartConnector = new TaskTableChartConnector(
      new SimpleIntegerProperty(-1),
      FXCollections.observableArrayList(),
      new SimpleDoubleProperty(0.0),
      true,
      new SimpleDoubleProperty(0.0),
      () -> null,
      () -> null
  );
  private TreeCollapseView<Task> myTaskCollapseView = new SimpleTreeCollapseView<>();
  protected final Supplier<TaskTable> myTaskTableSupplier;


  class TaskManagerConfigImpl implements TaskManagerConfig {
    final DefaultColorOption myDefaultColorOption = new GanttProjectImpl.DefaultTaskColorOption();
    final DefaultBooleanOption mySchedulerDisabledOption = new DefaultBooleanOption("scheduler.disabled", false);

    @Override
    public Color getDefaultColor() {
      return getUIFacade().getGanttChart().getTaskDefaultColorOption().getValue();
    }

    @Override
    public ColorOption getDefaultColorOption() {
      return myDefaultColorOption;
    }

    @Override
    public BooleanOption getSchedulerDisabledOption() {
      return mySchedulerDisabledOption;
    }

    @Override
    public GPCalendarCalc getCalendar() {
      return GanttProjectBase.this.getActiveCalendar();
    }

    @Override
    public TimeUnitStack getTimeUnitStack() {
      return GanttProjectBase.this.getTimeUnitStack();
    }

    @Override
    public HumanResourceManager getResourceManager() {
      return GanttProjectBase.this.getHumanResourceManager();
    }

    @Override
    public URL getProjectDocumentURL() {
      try {
        return getDocument().getURI().toURL();
      } catch (MalformedURLException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    public NotificationManager getNotificationManager() {
      return getUIFacade().getNotificationManager();
    }

    GPOptionGroup getTaskOptions() {
      return new GPOptionGroup("task", mySchedulerDisabledOption);
    }
  }

  protected GanttProjectBase() {
    super("GanttProject");
    myTaskManagerConfig = new TaskManagerConfigImpl();
    myTaskManager = TaskManager.Access.newInstance(null, myTaskManagerConfig);

    statusBar = new GanttStatusBar(this);
    myTabPane = new GanttTabbedPane();
    myContentPaneBuilder = new ContentPaneBuilder(getTabs(), getStatusBar());

    NotificationManagerImpl notificationManager = new NotificationManagerImpl(myContentPaneBuilder.getAnimationHost());
    myUIFacade = new UIFacadeImpl(this, statusBar, notificationManager, getProject(), this);
    GPLogger.setUIFacade(myUIFacade);
    myTaskActions = new TaskActions(getProject(), getUIFacade(), getTaskSelectionManager(),
        () -> getViewManager(),
        new Function0<>() {
      @Override
      public TaskTableActionConnector invoke() {
        return myTaskTableSupplier.get().getActionConnector();
      }
    });
    myTaskTableSupplier = Suppliers.synchronizedSupplier(Suppliers.memoize(
        () -> new TaskTable(getProject(), getTaskManager(),
            myTaskTableChartConnector, myTaskCollapseView, getTaskSelectionManager(), myTaskActions, getUndoManager())
    ));
    myDocumentManager = new DocumentCreator(this, getUIFacade(), null) {
      @Override
      protected ParserFactory getParserFactory() {
        return GanttProjectBase.this.getParserFactory();
      }

      @Override
      protected ColumnList getVisibleFields() {
        return myTaskTableSupplier.get().getColumnList();
      }

      @Override
      protected ColumnList getResourceVisibleFields() {
        return getUIFacade().getResourceTree().getVisibleFields();
      }
    };
    myUndoManager = new UndoManagerImpl(this, null, myDocumentManager) {
      @Override
      protected ParserFactory getParserFactory() {
        return GanttProjectBase.this.getParserFactory();
      }
    };
    myViewManager = new ViewManagerImpl(getProject(), myUIFacade, myTabPane, getUndoManager());
    myProjectUIFacade = new ProjectUIFacadeImpl(myUIFacade, myDocumentManager, myUndoManager);
    myRssChecker = new RssFeedChecker((GPTimeUnitStack) getTimeUnitStack(), myUIFacade);
    myUIFacade.addOptions(myRssChecker.getUiOptions());
  }

  @Override
  public void restore(Document fromDocument) throws Document.DocumentException, IOException {
    GanttProjectImplKt.restoreProject(this, fromDocument, myModifiedStateChangeListeners);
  }

  @Override
  public void addProjectEventListener(ProjectEventListener listener) {
    myModifiedStateChangeListeners.add(listener);
  }

  @Override
  public void removeProjectEventListener(ProjectEventListener listener) {
    myModifiedStateChangeListeners.remove(listener);
  }

  protected void fireProjectModified(boolean isModified) {
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

  protected void fireProjectCreated() {
    for (ProjectEventListener modifiedStateChangeListener : myModifiedStateChangeListeners) {
      modifiedStateChangeListener.projectCreated();
    }
    // A new project just got created, so it is not yet modified
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        setModified(false);
      }
    });
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

  // ////////////////////////////////////////////////////////////////
  // UIFacade
  public ProjectUIFacade getProjectUIFacade() {
    return myProjectUIFacade;
  }

  public UIFacade getUIFacade() {
    return myUIFacade;
  }

  protected UIFacadeImpl getUiFacadeImpl() {
    return myUIFacade;
  }

  @Override
  public Frame getMainFrame() {
    return myUIFacade.getMainFrame();
  }

  @Override
  public Image getLogo() {
    return myUIFacade.getLogo();
  }

  @Override
  public void setLookAndFeel(GanttLookAndFeelInfo laf) {
    myUIFacade.setLookAndFeel(laf);
  }

  @Override
  public GanttLookAndFeelInfo getLookAndFeel() {
    return myUIFacade.getLookAndFeel();
  }

  @Override
  public DefaultEnumerationOption<Locale> getLanguageOption() {
    return myUIFacade.getLanguageOption();
  }

  @Override
  public IntegerOption getDpiOption() {
    return myUIFacade.getDpiOption();
  }

  @Override
  public GPOption<String> getLafOption() {
    return myUIFacade.getLafOption();
  }

  @Override
  public GPOptionGroup[] getOptions() {
    return myUIFacade.getOptions();
  }

  @Override
  public void addOnUpdateComponentTreeUi(Runnable callback) {
    myUIFacade.addOnUpdateComponentTreeUi(callback);
  }

  @Override
  public ScrollingManager getScrollingManager() {
    return myUIFacade.getScrollingManager();
  }

  @Override
  public ZoomManager getZoomManager() {
    return myUIFacade.getZoomManager();
  }

  @Override
  public GPUndoManager getUndoManager() {
    return myUndoManager;
  }

  @Override
  public void setStatusText(String text) {
    myUIFacade.setStatusText(text);
  }

  @Override
  public Dialog createDialog(Component content, Action[] buttonActions, String title) {
    return myUIFacade.createDialog(content, buttonActions, title);
  }

  @Override
  public UIFacade.Choice showConfirmationDialog(String message, String title) {
    return myUIFacade.showConfirmationDialog(message, title);
  }

  @Override
  public void showOptionDialog(int messageType, String message, Action[] actions) {
    myUIFacade.showOptionDialog(messageType, message, actions);
  }

  @Override
  public void showErrorDialog(String message) {
    myUIFacade.showErrorDialog(message);
  }

  @Override
  public void showErrorDialog(Throwable e) {
    myUIFacade.showErrorDialog(e);
  }

  @Override
  public void showNotificationDialog(NotificationChannel channel, String message) {
    myUIFacade.showNotificationDialog(channel, message);
  }

  @Override
  public void showSettingsDialog(String pageID) {
    myUIFacade.showSettingsDialog(pageID);
  }

  @Override
  public NotificationManager getNotificationManager() {
    return myUIFacade.getNotificationManager();
  }

  @Override
  public void showPopupMenu(Component invoker, Action[] actions, int x, int y) {
    myUIFacade.showPopupMenu(invoker, actions, x, y);
  }

  @Override
  public void showPopupMenu(Component invoker, Collection<Action> actions, int x, int y) {
    myUIFacade.showPopupMenu(invoker, actions, x, y);
  }

  @Override
  public TaskSelectionContext getTaskSelectionContext() {
    return myUIFacade.getTaskSelectionContext();
  }

  @Override
  public TaskSelectionManager getTaskSelectionManager() {
    return myUIFacade.getTaskSelectionManager();
  }

  @Override
  public TaskView getCurrentTaskView() {
    return myUIFacade.getCurrentTaskView();
  }
  @Override
  public TreeCollapseView<Task> getTaskCollapseView() {
    return myTaskCollapseView;
  }

  @Override
  public ColumnList getTaskColumnList() {
    return myTaskTableSupplier.get().getColumnList();
  }

  @Override
  public void setWorkbenchTitle(String title) {
    myUIFacade.setWorkbenchTitle(title);
  }

  public GPViewManager getViewManager() {
    return myViewManager;
  }

  @Override
  public Chart getActiveChart() {
    return myViewManager.getSelectedView().getChart();
  }

  protected static class RowHeightAligner implements GPOptionChangeListener {
    private final ChartModelBase myChartModel;

    private final TreeTableContainer myTreeView;

    public RowHeightAligner(TreeTableContainer treeView, ChartModelBase chartModel) {
      myChartModel = chartModel;
      myTreeView = treeView;
      myChartModel.addOptionChangeListener(this);
    }

    @Override
    public void optionsChanged() {
      myTreeView.getTreeTable().setRowHeight(myChartModel.calculateRowHeight());
      AbstractTableModel model = (AbstractTableModel) myTreeView.getTreeTable().getModel();
      model.fireTableStructureChanged();
      myTreeView.updateUI();
    }
  }

  protected void createContentPane(JComponent toolbar) {
    myContentPaneBuilder.build(toolbar, getContentPane());
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension windowSize = getPreferredSize();
    // Put the frame at the middle of the screen
    setLocation(screenSize.width / 2 - (windowSize.width / 2), screenSize.height / 2 - (windowSize.height / 2));
    pack();
  }

  public GanttTabbedPane getTabs() {
    return myTabPane;
  }

  public IGanttProject getProject() {
    return this;
  }

  @Override
  public TimeUnitStack getTimeUnitStack() {
    return myTimeUnitStack;
  }

  @Override
  public CustomPropertyManager getTaskCustomColumnManager() {
    return getTaskManager().getCustomPropertyManager();
  }

  @Override
  public CustomPropertyManager getResourceCustomPropertyManager() {
    return myResourceCustomPropertyManager;
  }

  protected void setUpdater(Updater updater) {
    myUpdater = updater;
  }

  protected Updater getUpdater() {
    return myUpdater;
  }


  protected RssFeedChecker getRssFeedChecker() {
    return myRssChecker;
  }

  @Override
  public abstract String getProjectName();

  @Override
  public abstract void setProjectName(String projectName);

  @Override
  public abstract String getDescription();

  @Override
  public abstract void setDescription(String description);

  @Override
  public abstract String getOrganization();

  @Override
  public abstract void setOrganization(String organization);

  @Override
  public abstract String getWebLink();

  @Override
  public abstract void setWebLink(String webLink);

  @Override
  public abstract UIConfiguration getUIConfiguration();

  @Override
  public abstract HumanResourceManager getHumanResourceManager();

  @Override
  public abstract RoleManager getRoleManager();

  @Override
  public TaskManager getTaskManager() {
    return myTaskManager;
  }

//  @Override
//  public abstract TaskContainmentHierarchyFacade getTaskContainment();

  @Override
  public GPCalendarCalc getActiveCalendar() {
    return myCalendar;
  }

  @Override
  public abstract void setModified();

  @Override
  public abstract void close();

  @Override
  public abstract Document getDocument();

  protected GanttStatusBar getStatusBar() {
    return statusBar;
  }

  @Override
  public DocumentManager getDocumentManager() {
    return myDocumentManager;
  }

  protected abstract ParserFactory getParserFactory();

}
