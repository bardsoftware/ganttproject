/*
Copyright 2002-2019 Alexandre Thomas, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject;

import biz.ganttproject.LoggerApi;
import biz.ganttproject.app.*;
import biz.ganttproject.customproperty.CalculatedPropertyUpdater;
import biz.ganttproject.customproperty.CustomPropertyHolder;
import biz.ganttproject.lib.fx.TreeTableCellsKt;
import biz.ganttproject.platform.UpdateOptions;
import biz.ganttproject.storage.cloud.GPCloudOptions;
import biz.ganttproject.storage.cloud.GPCloudStatusBar;
import com.beust.jcommander.Parameter;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import kotlin.Unit;
import net.sourceforge.ganttproject.action.*;
import net.sourceforge.ganttproject.action.edit.EditMenu;
import net.sourceforge.ganttproject.action.help.HelpMenu;
import net.sourceforge.ganttproject.action.project.ProjectMenu;
import net.sourceforge.ganttproject.action.resource.ResourceActionSet;
import net.sourceforge.ganttproject.action.view.ViewCycleAction;
import net.sourceforge.ganttproject.action.view.ViewMenu;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.io.GanttXMLSaver;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceEvent;
import net.sourceforge.ganttproject.resource.ResourceView;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.undo.GPUndoListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main frame of the project
 */
public class GanttProject extends GanttProjectBase implements ResourceView, GanttLanguage.Listener {

  private final LoggerApi<Logger> boundsLogger = GPLogger.create("Window.Bounds");
  private final LoggerApi<Logger> gpLogger = GPLogger.create("GanttProject");

  /**
   * GanttGraphicArea for the calendar with Gantt
   */
  private final GanttGraphicArea area;

  /**
   * GanttPeoplePanel to edit person that work on the project
   */
  private GanttResourcePanel resp;

  private final EditMenu myEditMenu;

  private final ProjectMenu myProjectMenu;

  /**
   * Informations for the current project.
   */
  public PrjInfos prjInfos = new PrjInfos();

  /**
   * Boolean to know if the file has been modify
   */
  public boolean askForSave = false;

  private final ZoomActionSet myZoomActions;

  private UIConfiguration myUIConfiguration;

  private final GanttOptions options;

  private ArrayList<GanttPreviousState> myPreviousStates = new ArrayList<>();

  private MouseListener myStopEditingMouseListener = null;

  private final GanttChartTabContentPanel myGanttChartTabContent;

  private final ResourceChartTabContentPanel myResourceChartTabContent;

  private final List<RowHeightAligner> myRowHeightAligners = Lists.newArrayList();

  private ParserFactory myParserFactory;

  private static Consumer<Boolean> ourQuitCallback = withSystemExit -> {
    if (withSystemExit) {
      System.exit(0);
    } else {
      System.err.println("Quit application was called without System.exit() request");
    }
  };

  private FXSearchUi mySearchUi;

  public GanttProject(boolean isOnlyViewer) {
    LoggerApi<Logger> startupLogger = GPLogger.create("Window.Startup");
    startupLogger.debug("Creating main frame...");
    ToolTipManager.sharedInstance().setInitialDelay(200);
    ToolTipManager.sharedInstance().setDismissDelay(60000);

    getProjectImpl().getHumanResourceManager().addView(this);
    myCalendar.addListener(GanttProject.this::setModified);

    setFocusable(true);
    startupLogger.debug("1. loading look'n'feels");
    options = new GanttOptions(getRoleManager(), getDocumentManager(), isOnlyViewer);
    myUIConfiguration = options.getUIConfiguration();
    myUIConfiguration.setChartFontOption(getUiFacadeImpl().getChartFontOption());
    myUIConfiguration.setDpiOption(getUiFacadeImpl().getDpiOption());

    addProjectEventListener(getTaskManager().getProjectListener());
    getActiveCalendar().addListener(getTaskManager().getCalendarListener());
    ImageIcon icon = new ImageIcon(getClass().getResource("/icons/ganttproject-logo-512.png"));
    setIconImage(icon.getImage());

    area = new GanttGraphicArea(this, getTaskManager(), getZoomManager(), getUndoManager(),
        myTaskTableChartConnector,
      Suppliers.memoize(() -> myTaskTableSupplier.get().getActionConnector())::get);
    options.addOptionGroups(getUIFacade().getOptions());
    options.addOptionGroups(getUIFacade().getGanttChart().getOptionGroups());
    options.addOptionGroups(getUIFacade().getResourceChart().getOptionGroups());
    options.addOptionGroups(getProjectUIFacade().getOptionGroups());
    options.addOptionGroups(getDocumentManager().getNetworkOptionGroups());
    options.addOptions(GPCloudOptions.INSTANCE.getOptionGroup());
    options.addOptions(getRssFeedChecker().getOptions());
    options.addOptions(UpdateOptions.INSTANCE.getOptionGroup());
    options.addOptions(myTaskManagerConfig.getTaskOptions());
    startupLogger.debug("2. loading options");
    initOptions();

    getUIFacade().setLookAndFeel(getUIFacade().getLookAndFeel());
    getUiFacadeImpl().getAppFontOption().addChangeValueListener(event -> getGanttChart().reset());
    TreeTableCellsKt.initFontProperty(getUiFacadeImpl().getAppFontOption(), getUiFacadeImpl().getRowPaddingOption());
    TreeTableCellsKt.initColorProperties();
    getZoomManager().addZoomListener(area.getZoomListener());

    ScrollingManager scrollingManager = getScrollingManager();
    scrollingManager.addScrollingListener(area.getViewState());
    scrollingManager.addScrollingListener(getResourcePanel().area.getViewState());

    startupLogger.debug("3. creating menus...");
    ResourceActionSet myResourceActions = getResourcePanel().getResourceActionSet();
    myZoomActions = new ZoomActionSet(getZoomManager());
    JMenuBar bar = new JMenuBar();
    setJMenuBar(bar);
    // Allocation of the menus

    myProjectMenu = new ProjectMenu(this, "project");
    bar.add(myProjectMenu);

    myEditMenu = new EditMenu(getProject(), getUIFacade(), getViewManager(), () -> mySearchUi.requestFocus(), "edit");
    bar.add(myEditMenu);
    getResourcePanel().getTreeTable().setupActionMaps(myEditMenu.getSearchAction());

    ViewMenu viewMenu = new ViewMenu(getProject(), getViewManager(), getUiFacadeImpl().getDpiOption(), getUiFacadeImpl().getChartFontOption(), "view");
    bar.add(viewMenu);

    {
      JMenu mTask = UIUtil.createTooltiplessJMenu(GPAction.createVoidAction("task"));
      mTask.add(myTaskActions.getCreateAction());
      mTask.add(myTaskActions.getPropertiesAction());
      mTask.add(myTaskActions.getDeleteAction());
      getResourcePanel().setTaskPropertiesAction(myTaskActions.getPropertiesAction());
      bar.add(mTask);
    }
    JMenu mHuman = UIUtil.createTooltiplessJMenu(GPAction.createVoidAction("human"));
    for (AbstractAction a : myResourceActions.getActions()) {
      mHuman.add(a);
    }
    mHuman.add(myResourceActions.getResourceSendMailAction());
    mHuman.add(myResourceActions.getCloudResourceList());
    bar.add(mHuman);

    HelpMenu helpMenu = new HelpMenu(getProject(), getUIFacade(), getProjectUIFacade());
    bar.add(helpMenu.createMenu());

    startupLogger.debug("4. creating views...");

    myGanttChartTabContent = new GanttChartTabContentPanel(
        getProject(), getUIFacade(), area.getJComponent(),
        getUIConfiguration(), myTaskTableSupplier, myTaskActions, myUiInitializationPromise);

    getViewManager().createView(myGanttChartTabContent, new ImageIcon(getClass().getResource("/icons/tasks_16.gif")));
    getViewManager().toggleVisible(myGanttChartTabContent);

    myResourceChartTabContent = new ResourceChartTabContentPanel(getProject(), getUIFacade(), getResourcePanel(),
        getResourcePanel().area);
    getViewManager().createView(myResourceChartTabContent, new ImageIcon(getClass().getResource("/icons/res_16.gif")));
    getViewManager().toggleVisible(myResourceChartTabContent);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        SwingUtilities.invokeLater(() -> {
          getGanttChart().reset();
          getResourceChart().reset();
          // This will clear any modifications which might be caused by
          // adjusting widths of table columns during initial layout process.
          getProject().setModified(false);
        });
      }
    });
    startupLogger.debug("5. calculating size and packing...");

    FXToolbar fxToolbar = createToolbar();
    Platform.runLater(() -> {
      GPCloudStatusBar cloudStatusBar = new GPCloudStatusBar(
          myObservableDocument, getUIFacade(), getProjectUIFacade(), getProject()
      );
      Scene statusBarScene = new Scene(cloudStatusBar.getLockPanel(), Color.TRANSPARENT);
      statusBarScene.getStylesheets().add("biz/ganttproject/app/StatusBar.css");
      getStatusBar().setLeftScene(statusBarScene);
    });

    createContentPane(fxToolbar.getComponent());
    //final FXToolbar toolbar = fxToolbar;
    //final List<? extends JComponent> buttons = addButtons(getToolBar());
    // Chart tabs
    getTabs().setSelectedIndex(0);

    startupLogger.debug("6. changing language ...");
    languageChanged(null);
    // Add Listener after language update (to be sure that it is not updated
    // twice)
    language.addListener(this);

    startupLogger.debug("7. first attempt to restore bounds");
    restoreBounds();
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        quitApplication(true);
      }

      @Override
      public void windowOpened(WindowEvent e) {
        boundsLogger.debug("Resizing window...");
        boundsLogger.debug("Bounds after opening: {}", new Object[]{GanttProject.this.getBounds()}, ImmutableMap.of());
        restoreBounds();
        // It is important to run aligners after look and feel is set and font sizes
        // in the UI manager updated.
        SwingUtilities.invokeLater(() -> {
          for (RowHeightAligner aligner : myRowHeightAligners) {
            aligner.optionsChanged();
          }
        });
        getUiFacadeImpl().getDpiOption()
            .addChangeValueListener(event -> SwingUtilities.invokeLater(() -> getContentPane().doLayout()));
        getGanttChart().reset();
        getResourceChart().reset();
        // This will clear any modifications which might be caused by
        // adjusting widths of table columns during initial layout process.
        getProject().setModified(false);
      }
    });

    startupLogger.debug("8. finalizing...");
    // applyComponentOrientation(GanttLanguage.getInstance()
    // .getComponentOrientation());
    getTaskManager().addTaskListener(GanttProjectImplKt.createProjectModificationListener(this, getUIFacade()));
    addMouseListenerToAllContainer(this.getComponents());

    // Add globally available actions/key strokes
    GPAction viewCycleForwardAction = new ViewCycleAction(getViewManager(), true);
    UIUtil.pushAction(getTabs(), true, viewCycleForwardAction.getKeyStroke(), viewCycleForwardAction);

    GPAction viewCycleBackwardAction = new ViewCycleAction(getViewManager(), false);
    UIUtil.pushAction(getTabs(), true, viewCycleBackwardAction.getKeyStroke(), viewCycleBackwardAction);

    try {
      myObservableDocument.set(getDocumentManager().newUntitledDocument());
    } catch (IOException e) {
      gpLogger.error(Arrays.toString(e.getStackTrace()), new Object[]{}, ImmutableMap.of(), e);
    }
    var calculatedPropertyUpdater = new CalculatedPropertyUpdater(myProjectDatabase, getTaskCustomColumnManager(),
      () -> {
        var mapping = new HashMap<Integer, CustomPropertyHolder>();
        for (Task t : getTaskManager().getTasks()) {
          mapping.put(t.getTaskID(), t.getCustomValues());
        }
        return mapping;
      });
    getProjectImpl().addProjectEventListener(new ProjectEventListener.Stub() {
      @Override
      public void projectOpened(BarrierEntrance barrierRegistry, Barrier<IGanttProject> barrier) {
        barrier.await(iGanttProject -> {
          calculatedPropertyUpdater.update();
          return Unit.INSTANCE;
        });
      }
    });
    getUndoManager().addUndoableEditListener(new GPUndoListener() {
      @Override
      public void undoOrRedoHappened() {

      }

      @Override
      public void undoReset() {

      }

      @Override
      public void undoableEditHappened(UndoableEditEvent e) {
        calculatedPropertyUpdater.update();
      }
    });
  }


  private void restoreBounds() {
    if (options.isLoaded()) {
      if (options.isMaximized()) {
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
      }
      Rectangle bounds = new Rectangle(options.getX(), options.getY(), options.getWidth(), options.getHeight());
      boundsLogger.debug("Bounds stored in the  options: {}", new Object[]{bounds}, ImmutableMap.of());

      UIUtil.MultiscreenFitResult fit = UIUtil.multiscreenFit(bounds);
      // If more than 1/4 of the rectangle is visible on screen devices then leave it where it is
      if (fit.totalVisibleArea < 0.25 || Math.max(bounds.width, bounds.height) < 100) {
        // Otherwise if it is visible on at least one device, try to fit it there
        if (fit.argmaxVisibleArea != null) {
          bounds = fitBounds(fit.argmaxVisibleArea, bounds);
        } else {
          UIUtil.MultiscreenFitResult currentFit = UIUtil.multiscreenFit(this.getBounds());
          if (currentFit.argmaxVisibleArea != null) {
            // If there are no devices where rectangle is visible, fit it on the current device
            bounds = fitBounds(currentFit.argmaxVisibleArea, bounds);
          } else {
            boundsLogger.debug(
                "We have not found the display corresponding to bounds {}. Leaving the window where it is",
                new Object[]{bounds}, ImmutableMap.of()
            );
            return;
          }
        }
      }
      setBounds(bounds);
    }
  }

  static private Rectangle fitBounds(GraphicsConfiguration display, Rectangle bounds) {
    Rectangle displayBounds = display.getBounds();
    Rectangle visibleBounds = bounds.intersection(displayBounds);
    int fitX = visibleBounds.x;
    if (fitX + bounds.width > displayBounds.x + displayBounds.width) {
      fitX = Math.max(displayBounds.x, displayBounds.x + displayBounds.width - bounds.width);
    }
    int fitY = visibleBounds.y;
    if (fitY + bounds.height > displayBounds.y + displayBounds.height) {
      fitY = Math.max(displayBounds.y, displayBounds.y + displayBounds.height - bounds.height);
    }
    return new Rectangle(fitX, fitY, bounds.width, bounds.height);

  }


  private void initOptions() {
    options.setUIConfiguration(myUIConfiguration);
    options.load();
    myUIConfiguration = options.getUIConfiguration();
  }

  private void addMouseListenerToAllContainer(Component[] containers) {
    for (Component container : containers) {
      container.addMouseListener(getStopEditingMouseListener());
      if (container instanceof Container) {
        addMouseListenerToAllContainer(((Container) container).getComponents());
      }
    }
  }

  /**
   * @return A mouseListener that stop the edition in the ganttTreeTable.
   */
  private MouseListener getStopEditingMouseListener() {
    if (myStopEditingMouseListener == null)
      myStopEditingMouseListener = new MouseAdapter() {
        // @Override
        // public void mouseClicked(MouseEvent e) {
        // if (e.getSource() != bNew && e.getClickCount() == 1) {
        // tree.stopEditing();
        // }
        // if (e.getButton() == MouseEvent.BUTTON1
        // && !(e.getSource() instanceof JTable)
        // && !(e.getSource() instanceof AbstractButton)) {
        // Task taskUnderPointer =
        // area.getChartImplementation().findTaskUnderPointer(e.getX(),
        // e.getY());
        // if (taskUnderPointer == null) {
        // getTaskSelectionManager().clear();
        // }
        // }
        // }
      };
    return myStopEditingMouseListener;
  }

  /**
   * @return the options of ganttproject.
   */
  public GanttOptions getGanttOptions() {
    return options;
  }

  /**
   * Function to change language of the project
   */
  @Override
  public void languageChanged(Event event) {
    applyComponentOrientation(language.getComponentOrientation());
    area.repaint();
    getResourcePanel().area.repaint();

    CustomColumnsStorage.changeLanguage(language);

    applyComponentOrientation(language.getComponentOrientation());
  }

  /**
   * @return the ToolTip in HTML (with gray bgcolor)
   */
  public static String getToolTip(String msg) {
    return "<html><body bgcolor=#EAEAEA>" + msg + "</body></html>";
  }

  /**
   * Create the button on toolbar
   */
  private FXToolbar createToolbar() {
    FXToolbarBuilder builder = new FXToolbarBuilder();
    builder.addButton(myProjectMenu.getOpenProjectAction().asToolbarAction())
        .addButton(myProjectMenu.getSaveProjectAction().asToolbarAction())
        .addWhitespace();

    final ArtefactAction newAction;
    {
      final GPAction taskNewAction = myTaskActions.getCreateAction().asToolbarAction();
      final GPAction resourceNewAction = getResourceTree().getNewAction().asToolbarAction();
      newAction = new ArtefactNewAction(() -> getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? taskNewAction : resourceNewAction, new Action[]{taskNewAction, resourceNewAction});
      builder.addButton(taskNewAction).addButton(resourceNewAction);
    }

    final ArtefactAction deleteAction;
    {
      final GPAction taskDeleteAction = myTaskActions.getDeleteAction();
      final GPAction resourceDeleteAction = getResourceTree().getDeleteAction().asToolbarAction();
      deleteAction = new ArtefactDeleteAction(() -> getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? taskDeleteAction : resourceDeleteAction, new Action[]{taskDeleteAction, resourceDeleteAction});
    }
    builder.setArtefactActions(newAction, deleteAction);

    final ArtefactAction propertiesAction;
    {
      final GPAction taskPropertiesAction = myTaskActions.getPropertiesAction().asToolbarAction();
      final GPAction resourcePropertiesAction = getResourceTree().getPropertiesAction().asToolbarAction();
      propertiesAction = new TaskResourcePropertiesAction(
        taskPropertiesAction, resourcePropertiesAction,
        () -> getTabs().getSelectedIndex(),
        () -> getTaskSelectionManager().getSelectedTasks());
    }

    UIUtil.registerActions(getRootPane(), false, newAction, propertiesAction, deleteAction);
    UIUtil.registerActions(myGanttChartTabContent.getComponent(), true, newAction, propertiesAction, deleteAction);
    UIUtil.registerActions(myResourceChartTabContent.getComponent(), true, newAction, propertiesAction, deleteAction);
    getTabs().getModel().addChangeListener(e -> {
      // Tell artefact actions that the active provider changed, so they
      // are able to update their state according to the current delegate
      newAction.actionStateChanged();
      propertiesAction.actionStateChanged();
      deleteAction.actionStateChanged();
      getTabs().getSelectedComponent().requestFocus();
    });

    builder.addButton(deleteAction)
        .addWhitespace()
        .addButton(propertiesAction)
        .addButton(getCutAction().asToolbarAction())
        .addButton(getCopyAction().asToolbarAction())
        .addButton(getPasteAction().asToolbarAction())
        .addWhitespace()
        .addButton(myEditMenu.getUndoAction().asToolbarAction())
        .addButton(myEditMenu.getRedoAction().asToolbarAction());
    mySearchUi = new FXSearchUi(getProject(), getUIFacade(), myEditMenu.getSearchAction());
    builder.addSearchBox(mySearchUi);
    builder.withClasses("toolbar-common", "toolbar-main", "toolbar-big");
    builder.withScene();
    //return result;
    return builder.build();
  }

  void doShow() {
    setVisible(true);
    boundsLogger.debug("Bounds after setVisible: {}", new Object[]{getBounds()}, ImmutableMap.of());
    DesktopIntegration.setup(GanttProject.this);
    getActiveChart().reset();
    getRssFeedChecker().setOptionsVersion(getGanttOptions().getVersion());
    getRssFeedChecker().run();
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  }

  @Override
  public @NotNull List<GanttPreviousState> getBaselines() {
    return myPreviousStates;
  }

  /**
   * Create a new project
   */
  public void newProject() {
    getProjectUIFacade().createProject(getProject());
    try {
      Document newDocument = getDocumentManager().newUntitledDocument();
      getProject().setDocument(newDocument);
      myObservableDocument.set(newDocument);
      getProjectImpl().fireProjectCreated();
    } catch (IOException e) {
      gpLogger.error(Arrays.toString(e.getStackTrace()), new Object[]{}, ImmutableMap.of(), e);
    }
  }

  @Override
  public void open(Document document) throws IOException, DocumentException {
    document.read();
    getDocumentManager().addToRecentDocuments(document);
    //myMRU.add(document.getPath(), true);
    myObservableDocument.set(document);
    setTitle(language.getText("appliTitle") + " [" + document.getFileName() + "]");
    for (Chart chart : PluginManager.getCharts()) {
      chart.reset();
    }

    // myDelayManager.fireDelayObservation(); // it is done in repaint2
    addMouseListenerToAllContainer(this.getComponents());

    getProjectImpl().fireProjectOpened();
  }

  /**
   * @return the UIConfiguration.
   */
  @Override
  public @NotNull UIConfiguration getUIConfiguration() {
    return myUIConfiguration;
  }

  private boolean myQuitEntered = false;

  /**
   * Quit the application
   */
  @Override
  public boolean quitApplication(boolean withSystemExit) {
    if (myQuitEntered) {
      return false;
    }
    myQuitEntered = true;
    try {
      options.setWindowPosition(getX(), getY());
      options.setWindowSize(getWidth(), getHeight(), (getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);
      options.setUIConfiguration(myUIConfiguration);
      options.save();
      var barrier = getProjectUIFacade().ensureProjectSaved(getProject());
      barrier.await(result -> {
        if (result) {
          getProject().close();
          setVisible(false);
          dispose();
          doQuitApplication(withSystemExit);
        } else {
          setVisible(true);
        }
        return Unit.INSTANCE;
      });
    } finally {
      myQuitEntered = false;
    }
    return true;
  }

  public void setAskForSave(boolean afs) {
    getProjectImpl().fireProjectModified(afs, (ex) -> getUIFacade().showErrorDialog(ex) );
    String title = getTitle();
    askForSave = afs;
    if (System.getProperty("mrj.version") != null) {
      rootPane.putClientProperty("windowModified", afs);
      // see http://developer.apple.com/qa/qa2001/qa1146.html
    } else {
      if (askForSave) {
        if (!title.endsWith(" *")) {
          setTitle(title + " *");
        }
      }
    }
  }

  public GanttResourcePanel getResourcePanel() {
    if (this.resp == null) {
      this.resp = new GanttResourcePanel(this, getUIFacade());
      this.resp.init();
      myRowHeightAligners.add(this.resp.getRowHeightAligner());
      getHumanResourceManager().addView(this.resp);
    }
    return this.resp;
  }

  public GanttGraphicArea getArea() {
    return this.area;
  }

  public GPAction getCopyAction() {
    return getViewManager().getCopyAction();
  }

  public GPAction getCutAction() {
    return getViewManager().getCutAction();
  }

  public GPAction getPasteAction() {
    return getViewManager().getPasteAction();
  }

  @Override
  public ZoomActionSet getZoomActionSet() {
    return myZoomActions;
  }

  public static class Args {

    @Parameter(names = "-log", description = "Enable logging", arity = 1)
    public boolean log = true;

    @Parameter(names = "-log_file", description = "Log file name")
    public String logFile = "auto";

    @Parameter(names = {"-h", "-help"}, description = "Print usage")
    public boolean help = false;

    @Parameter(names = {"-version"}, description = "Print version number")
    public boolean version = false;

    @Parameter(names = "--fix-menu-bar-title", description = "Fixes the application title in the menu bar on Linux with Unity desktop environment")
    public boolean fixMenuBarTitle = false;

    @Parameter(description = "Input file name")
    public List<String> file = null;
  }

  // ///////////////////////////////////////////////////////
  // IGanttProject implementation
  @Override
  public @NotNull String getProjectName() {
    return prjInfos.getName();
  }

  @Override
  public void setProjectName(@NotNull String projectName) {
    prjInfos.setName(projectName);
    setAskForSave(true);
  }

  @Override
  public @NotNull String getDescription() {
    return Objects.requireNonNullElse(prjInfos.getDescription(), "");
  }

  @Override
  public void setDescription(@NotNull String description) {
    prjInfos.setDescription(description);
    setAskForSave(true);
  }

  @Override
  public @NotNull String getOrganization() {
    return prjInfos.getOrganization();
  }

  @Override
  public void setOrganization(@NotNull String organization) {
    prjInfos.setOrganization(organization);
    setAskForSave(true);
  }

  @Override
  public @NotNull String getWebLink() {
    return prjInfos.getWebLink();
  }

  @Override
  public void setWebLink(@NotNull String webLink) {
    prjInfos.setWebLink(webLink);
    setAskForSave(true);
  }

  @Override
  public @NotNull HumanResourceManager getHumanResourceManager() {
    return getProjectImpl().getHumanResourceManager();
  }

  @Override
  public @NotNull RoleManager getRoleManager() {
    return getProjectImpl().getRoleManager();
  }

  @Override
  public @NotNull Document getDocument() {
    return myObservableDocument.get();
  }

  @Override
  public void setDocument(@NotNull Document document) {
    myObservableDocument.set(document);
  }

  @Override
  public void setModified() {
    setModified(true);
  }

  @Override
  public void setModified(boolean modified) {
    setAskForSave(modified);

    String title = getTitle();
    if (!modified && title.endsWith(" *")) {
      // Remove * from title
      setTitle(title.substring(0, title.length() - 2));
    }
  }

  @Override
  public boolean isModified() {
    return askForSave;
  }

  @Override
  public void close() {
    getProjectImpl().fireProjectClosed();
    prjInfos = new PrjInfos();
    RoleManager.Access.getInstance().clear();
    myObservableDocument.set(null);
    getTaskCustomColumnManager().reset();
    getResourceCustomPropertyManager().reset();

    for (GanttPreviousState myPreviousState : myPreviousStates) {
      myPreviousState.remove();
    }
    myPreviousStates = new ArrayList<>();
    myCalendar.reset();
    //myFacadeInvalidator.projectClosed();
  }

  @Override
  protected ParserFactory getParserFactory() {
    if (myParserFactory == null) {
      myParserFactory = new ParserFactoryImpl();
    }
    return myParserFactory;
  }

  // ///////////////////////////////////////////////////////////////
  // ResourceView implementation
  @Override
  public void resourceAdded(ResourceEvent event) {
    setAskForSave(true);
  }

  @Override
  public void resourcesRemoved(ResourceEvent event) {
    setAskForSave(true);
  }

  @Override
  public void resourceChanged(ResourceEvent e) {
    setAskForSave(true);
  }

  @Override
  public void resourceAssignmentsChanged(ResourceEvent e) {
    setAskForSave(true);
  }

  // ///////////////////////////////////////////////////////////////
  // UIFacade

  @Override
  public GanttChart getGanttChart() {
    return getArea();
  }

  @Override
  public TimelineChart getResourceChart() {
    return getResourcePanel().area;
  }

  @Override
  public int getGanttDividerLocation() {
    return myGanttChartTabContent.getDividerLocation();
  }

  @Override
  public void setGanttDividerLocation(int location) {
    myGanttChartTabContent.setDividerLocation(location);
  }

  @Override
  public int getResourceDividerLocation() {
    return myResourceChartTabContent.getDividerLocation();
  }

  @Override
  public void setResourceDividerLocation(int location) {
    myResourceChartTabContent.setDividerLocation(location);
  }

  @Override
  public ResourceTreeUIFacade getResourceTree() {
    return getResourcePanel();
  }

  private class ParserFactoryImpl implements ParserFactory {
    @Override
    public GPParser newParser() {
      return new GanttXMLOpen(prjInfos, getTaskManager(), getUIFacade());
    }

    @Override
    public GPSaver newSaver() {
      return new GanttXMLSaver(GanttProject.this, getArea(), getUIFacade(),
        () -> myTaskTableSupplier.get().getColumnList(), () -> myTaskFilterManager);
    }
  }

  @Override
  public int getViewIndex() {
    if (getTabs() == null) {
      return -1;
    }
    return getTabs().getSelectedIndex();
  }

  @Override
  public void setViewIndex(int viewIndex) {
    if (getTabs().getTabCount() > viewIndex) {
      getTabs().setSelectedIndex(viewIndex);
    }
  }

  public static void setApplicationQuitCallback(Consumer<Boolean> callback) {
    ourQuitCallback = callback;
  }

  public static void doQuitApplication(boolean withSystemExit) {
    ourQuitCallback.accept(withSystemExit);
  }
  @Override
  public void refresh() {
    getResourcePanel().getResourceTreeTableModel().updateResources();
    getResourcePanel().getResourceTreeTable().setRowHeight(getResourceChart().getModel().calculateRowHeight());
    for (Chart chart : PluginManager.getCharts()) {
      chart.reset();
    }
    super.repaint();
  }
}
