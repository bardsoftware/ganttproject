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

import biz.ganttproject.app.Barrier;
import biz.ganttproject.app.TwoPhaseBarrierImpl;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.ImportCalendarOption;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import biz.ganttproject.core.option.*;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import biz.ganttproject.customproperty.CustomPropertyManager;
import biz.ganttproject.ganttview.*;
import biz.ganttproject.lib.fx.SimpleTreeCollapseView;
import biz.ganttproject.lib.fx.TreeCollapseView;
import biz.ganttproject.lib.fx.TreeTableCellsKt;
import biz.ganttproject.platform.UpdateKt;
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
import net.sourceforge.ganttproject.gui.*;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.gui.view.ViewManagerImpl;
import net.sourceforge.ganttproject.gui.window.ContentPaneBuilder;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.importer.BufferProject;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.storage.LazyProjectDatabaseProxy;
import net.sourceforge.ganttproject.storage.ProjectDatabase;
import net.sourceforge.ganttproject.storage.SqlProjectDatabaseImpl;
import net.sourceforge.ganttproject.task.*;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.undo.UndoManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
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
  private final UIFacadeImpl myUIFacade;
  private final GanttStatusBar statusBar;
  private final TimeUnitStack myTimeUnitStack = new GPTimeUnitStack();
  private final ProjectUIFacadeImpl myProjectUIFacade;
  private final DocumentManager myDocumentManager;
  protected final SimpleObjectProperty<Document> myObservableDocument = new SimpleObjectProperty<>();
  /** The tabbed pane with the different parts of the project */
  private final GanttTabbedPane myTabPane;
  private final GPUndoManager myUndoManager;

  private final RssFeedChecker myRssChecker;
  protected final ContentPaneBuilder myContentPaneBuilder;
  final TaskManagerConfigImpl myTaskManagerConfig;
  private final TaskManager myTaskManager;
  protected final TwoPhaseBarrierImpl<UIFacade> myUiInitializationPromise;
  private Updater myUpdater;
  protected final TaskActions myTaskActions;
  private final GanttProjectImpl myProjectImpl;

  TaskTableChartConnector myTaskTableChartConnector = new TaskTableChartConnector(
      new SimpleIntegerProperty(-1),
      FXCollections.observableArrayList(),
      new SimpleDoubleProperty(0.0),
      true,
      null,
      () -> null,
      () -> null,
      TreeTableCellsKt.getMinCellHeight()
  );
  private final TreeCollapseView<Task> myTaskCollapseView = new SimpleTreeCollapseView<>();
  protected final Supplier<TaskTable> myTaskTableSupplier;

  protected final TaskFilterManager myTaskFilterManager;

  protected final ProjectDatabase myProjectDatabase;

  @Override
  public @NotNull Map<Task, Task> importProject(
    @NotNull BufferProject bufferProject,
    @NotNull HumanResourceMerger.MergeResourcesOption mergeResourcesOption,
    @Nullable ImportCalendarOption importCalendarOption, boolean closeCurrentProject) {
    return GanttProjectImplKt.restoreProject(this, myProjectImpl.getListeners(), closeCurrentProject,
      () -> myProjectImpl.importProject(bufferProject, mergeResourcesOption, importCalendarOption, closeCurrentProject)
    );
  }


  class TaskManagerConfigImpl implements TaskManagerConfig {
    final DefaultColorOption myDefaultColorOption = new DefaultTaskColorOption();
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

  @Override
  public ProjectDatabase getProjectDatabase() {
    return myProjectDatabase;
  }

  protected GanttProjectBase() {
    super("GanttProject");
    var databaseProxy = new LazyProjectDatabaseProxy(SqlProjectDatabaseImpl.Factory::createInMemoryDatabase, this::getTaskManager);

    myProjectDatabase = databaseProxy;
    myTaskManagerConfig = new TaskManagerConfigImpl();
    myTaskManager = TaskManager.Access.newInstance(null, myTaskManagerConfig,
      myProjectDatabase::createTaskUpdateBuilder);
    myProjectImpl = new GanttProjectImpl((TaskManagerImpl) myTaskManager, databaseProxy);
    addProjectEventListener(databaseProxy.createProjectEventListener());
    myTaskManager.addTaskListener(databaseProxy.createTaskEventListener());
    statusBar = new GanttStatusBar(this);
    myTabPane = new GanttTabbedPane();
    myContentPaneBuilder = new ContentPaneBuilder(getTabs(), getStatusBar());

    NotificationManagerImpl notificationManager = new NotificationManagerImpl(myContentPaneBuilder.getAnimationHost());
    myUIFacade = new UIFacadeImpl(this, statusBar, notificationManager, getProject(), this);
    myUiInitializationPromise = new TwoPhaseBarrierImpl<>(myUIFacade);

    GPLogger.setUIFacade(myUIFacade);
    var newTaskActor = new NewTaskActor<Task>();
    newTaskActor.start();

    myTaskActions = new TaskActions(getProject(), getUIFacade(), getTaskSelectionManager(),
        this::getViewManager,
        new Function0<>() {
      @Override
      public TaskTableActionConnector invoke() {
        return myTaskTableSupplier.get().getActionConnector();
      }
    }, newTaskActor, myProjectDatabase);
    myTaskFilterManager = new TaskFilterManager(getTaskManager());
    myTaskTableSupplier = Suppliers.synchronizedSupplier(Suppliers.memoize(() ->
      new TaskTable(getProject(), getTaskManager(), myTaskTableChartConnector, myTaskCollapseView,
        getTaskSelectionManager(), myTaskActions, getUndoManager(), myTaskFilterManager, myUiInitializationPromise, newTaskActor)
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
    myUndoManager = new UndoManagerImpl(this, null, myDocumentManager, myProjectDatabase) {
      @Override
      protected ParserFactory getParserFactory() {
        return GanttProjectBase.this.getParserFactory();
      }
    };
//    myUndoManager.addUndoableEditListener(databaseProxy.createUndoListener());
    myViewManager = new ViewManagerImpl(getProject(), myUIFacade, myTabPane, getUndoManager());
    myProjectUIFacade = new ProjectUIFacadeImpl(myUIFacade, myDocumentManager, myUndoManager);
    myRssChecker = new RssFeedChecker((GPTimeUnitStack) getTimeUnitStack(), myUIFacade);
    myUIFacade.addOptions(myRssChecker.getUiOptions());
  }

  protected GanttProjectImpl getProjectImpl() {
    return myProjectImpl;
  }
  @Override
  public void restore(@NotNull Document fromDocument) throws Document.DocumentException, IOException {
    GanttProjectImplKt.restoreProject(this, fromDocument, myProjectImpl.getListeners());
  }

  @Override
  public void addProjectEventListener(@NotNull ProjectEventListener listener) {
    myProjectImpl.addProjectEventListener(listener);
  }

  @Override
  public void removeProjectEventListener(@NotNull ProjectEventListener listener) {
    myProjectImpl.removeProjectEventListener(listener);
  }


  public Barrier<UIFacade> getUiInitializationPromise() {
    return myUiInitializationPromise;
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
  public @NotNull TimeUnitStack getTimeUnitStack() {
    return myTimeUnitStack;
  }

  @Override
  public @NotNull CustomPropertyManager getTaskCustomColumnManager() {
    return getTaskManager().getCustomPropertyManager();
  }

  @Override
  public @NotNull CustomPropertyManager getResourceCustomPropertyManager() {
    return getProjectImpl().getResourceCustomPropertyManager();
  }

  protected void setUpdater(Updater updater) {
    myUpdater = updater;
    UpdateKt.checkAvailableUpdates(updater, getUIFacade());
  }

  protected Updater getUpdater() {
    return myUpdater;
  }


  protected RssFeedChecker getRssFeedChecker() {
    return myRssChecker;
  }

  @Override
  public abstract @NotNull String getProjectName();

  @Override
  public abstract void setProjectName(@NotNull String projectName);

  @Override
  public abstract @NotNull String getDescription();

  @Override
  public abstract void setDescription(@NotNull String description);

  @Override
  public abstract @NotNull String getOrganization();

  @Override
  public abstract void setOrganization(@NotNull String organization);

  @Override
  public abstract @NotNull String getWebLink();

  @Override
  public abstract void setWebLink(@NotNull String webLink);

  @Override
  public abstract @NotNull UIConfiguration getUIConfiguration();

  @Override
  public abstract @NotNull HumanResourceManager getHumanResourceManager();

  @Override
  public abstract @NotNull RoleManager getRoleManager();

  @Override
  public @NotNull TaskManager getTaskManager() {
    return myTaskManager;
  }

  @Override
  public @NotNull GPCalendarCalc getActiveCalendar() {
    return myCalendar;
  }

  @Override
  public abstract void setModified();

  @Override
  public abstract void close();

  @Override
  public abstract @NotNull Document getDocument();

  protected GanttStatusBar getStatusBar() {
    return statusBar;
  }

  @Override
  public @NotNull DocumentManager getDocumentManager() {
    return myDocumentManager;
  }

  protected abstract ParserFactory getParserFactory();
  public @NotNull TaskFilterManager getTaskFilterManager() {
    return myTaskFilterManager;
  }
}
