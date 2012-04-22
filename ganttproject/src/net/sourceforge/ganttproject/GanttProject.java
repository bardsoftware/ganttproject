/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Alexandre Thomas, Dmitry Barashev, GanttProject Team

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import net.sourceforge.ganttproject.action.ActiveActionProvider;
import net.sourceforge.ganttproject.action.ArtefactAction;
import net.sourceforge.ganttproject.action.ArtefactDeleteAction;
import net.sourceforge.ganttproject.action.ArtefactNewAction;
import net.sourceforge.ganttproject.action.ArtefactPropertiesAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.GPAction.IconSize;
import net.sourceforge.ganttproject.action.edit.EditMenu;
import net.sourceforge.ganttproject.action.help.HelpMenu;
import net.sourceforge.ganttproject.action.project.ProjectMenu;
import net.sourceforge.ganttproject.action.resource.ResourceActionSet;
import net.sourceforge.ganttproject.action.view.ViewCycleAction;
import net.sourceforge.ganttproject.action.view.ViewMenu;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.delay.DelayManager;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.document.DocumentsMRU;
import net.sourceforge.ganttproject.document.HttpDocument;
import net.sourceforge.ganttproject.export.CommandLineExportApplication;
import net.sourceforge.ganttproject.gui.ProjectMRUMenu;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.importer.Importer;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.io.GanttXMLSaver;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.print.PrintManager;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceEvent;
import net.sourceforge.ganttproject.resource.ResourceView;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.task.algorithm.AdjustTaskBoundsAlgorithm;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskCompletionPercentageAlgorithm;
import net.sourceforge.ganttproject.time.TimeUnitStack;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Main frame of the project
 */
public class GanttProject extends GanttProjectBase implements ResourceView, GanttLanguage.Listener {

  /** The current version of ganttproject */
  public static final String version = GPVersion.V2_0_X;

  /** The JTree part. */
  private GanttTree2 tree;

  /** GanttGraphicArea for the calendar with Gantt */
  private GanttGraphicArea area;

  /** GanttPeoplePanel to edit person that work on the project */
  private GanttResourcePanel resp;

  private final EditMenu myEditMenu;

  private final ProjectMenu myProjectMenu;

  /** List containing the Most Recent Used documents */
  private final DocumentsMRU myMRU = new DocumentsMRU(5);

  private TestGanttRolloverButton bNew;

  /** The project filename */
  public Document projectDocument = null;

  /** Informations for the current project. */
  public PrjInfos prjInfos = new PrjInfos();

  /** Boolean to know if the file has been modify */
  public boolean askForSave = false;

  /** Is the application only for viewer. */
  public boolean isOnlyViewer;

  private final ResourceActionSet myResourceActions;

  private final ZoomActionSet myZoomActions;

  private final TaskManager myTaskManager;

  private final FacadeInvalidator myFacadeInvalidator;

  private UIConfiguration myUIConfiguration;

  private final GanttOptions options;

  private TaskContainmentHierarchyFacadeImpl myCachedFacade;

  private ArrayList<GanttPreviousState> myPreviousStates = new ArrayList<GanttPreviousState>();

  private MouseListener myStopEditingMouseListener = null;

  private DelayManager myDelayManager;

  private GanttChartTabContentPanel myGanttChartTabContent;

  private ResourceChartTabContentPanel myResourceChartTabContent;

  private RowHeightAligner myRowHeightAligner;

  public GanttProject(boolean isOnlyViewer) {
    System.err.println("Creating main frame...");
    ToolTipManager.sharedInstance().setInitialDelay(200);
    ToolTipManager.sharedInstance().setDismissDelay(60000);

    Mediator.registerTaskSelectionManager(getTaskSelectionManager());
    /*
     * [bbaranne] I add a Mediator object so that we can get the GanttProject
     * singleton where ever we are in the source code. Perhaps some of you don't
     * like this, but I believe that it is practical...
     */
    Mediator.registerGanttProject(this);

    this.isOnlyViewer = isOnlyViewer;
    if (!isOnlyViewer) {
      setTitle(language.getText("appliTitle"));
    } else {
      setTitle("GanttViewer");
    }
    setFocusable(true);
    System.err.println("1. loading look'n'feels");
    options = new GanttOptions(getRoleManager(), getDocumentManager(), isOnlyViewer, myMRU);
    myUIConfiguration = options.getUIConfiguration();
    class TaskManagerConfigImpl implements TaskManagerConfig {
      @Override
      public Color getDefaultColor() {
        return myUIConfiguration.getTaskColor();
      }

      @Override
      public GPCalendar getCalendar() {
        return GanttProject.this.getActiveCalendar();
      }

      @Override
      public TimeUnitStack getTimeUnitStack() {
        return GanttProject.this.getTimeUnitStack();
      }

      @Override
      public HumanResourceManager getResourceManager() {
        return GanttProject.this.getHumanResourceManager();
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

    }
    TaskManagerConfig taskConfig = new TaskManagerConfigImpl();
    myTaskManager = TaskManager.Access.newInstance(new TaskContainmentHierarchyFacade.Factory() {
      @Override
      public TaskContainmentHierarchyFacade createFacade() {
        return GanttProject.this.getTaskContainment();
      }
    }, taskConfig);
    ImageIcon icon = new ImageIcon(getClass().getResource("/icons/ganttproject.png"));
    setIconImage(icon.getImage());

    // Create each objects
    myFacadeInvalidator = new FacadeInvalidator(getTree().getModel());
    getProject().addProjectEventListener(myFacadeInvalidator);
    area = new GanttGraphicArea(this, getTree(), getTaskManager(), getZoomManager(), getUndoManager());
    options.addOptionGroups(new GPOptionGroup[] { getUIFacade().getOptions() });
    options.addOptionGroups(getUIFacade().getGanttChart().getOptionGroups());
    options.addOptionGroups(getUIFacade().getResourceChart().getOptionGroups());
    options.addOptionGroups(new GPOptionGroup[] { getProjectUIFacade().getOptionGroup() });
    options.addOptionGroups(getDocumentManager().getNetworkOptionGroups());
    options.addOptions(getRssFeedChecker().getOptions());
    myRowHeightAligner = new RowHeightAligner(tree, area.getMyChartModel());
    area.getMyChartModel().addOptionChangeListener(myRowHeightAligner);

    System.err.println("2. loading options");
    initOptions();
    area.setUIConfiguration(myUIConfiguration);
    getTree().setGraphicArea(area);

    getZoomManager().addZoomListener(area.getZoomListener());

    ScrollingManager scrollingManager = getScrollingManager();
    scrollingManager.addScrollingListener(area.getViewState());
    scrollingManager.addScrollingListener(getResourcePanel().area.getViewState());

    System.err.println("3. creating menus...");
    myResourceActions = getResourcePanel().getResourceActionSet();
    myZoomActions = new ZoomActionSet(getZoomManager());
    JMenuBar bar = new JMenuBar();
    setJMenuBar(bar);
    // Allocation of the menus

    // Project menu related sub menus and items
    ProjectMRUMenu mruMenu = new ProjectMRUMenu(this, getUIFacade(), getProjectUIFacade(), "lastOpen");
    mruMenu.setIcon(new ImageIcon(getClass().getResource("/icons/recent_16.gif")));
    myMRU.addListener(mruMenu);

    myProjectMenu = new ProjectMenu(this, mruMenu, "project");
    bar.add(myProjectMenu);

    myEditMenu = new EditMenu(getProject(), getUIFacade(), getViewManager(), getSearchUi(), "edit");
    bar.add(myEditMenu);

    ViewMenu viewMenu = new ViewMenu(getProject(), getViewManager(), "view");
    bar.add(viewMenu);

    {
      TaskTreeUIFacade taskTree = getUIFacade().getTaskTree();
      JMenu mTask = new JMenu(GPAction.createVoidAction("task"));
      mTask.add(taskTree.getNewAction());
      mTask.add(taskTree.getPropertiesAction());
      mTask.add(taskTree.getDeleteAction());
      getResourcePanel().setTaskPropertiesAction(taskTree.getPropertiesAction());
      bar.add(mTask);
    }
    JMenu mHuman = new JMenu(GPAction.createVoidAction("human"));
    for (AbstractAction a : myResourceActions.getActions()) {
      mHuman.add(a);
    }
    mHuman.add(myResourceActions.getResourceSendMailAction());
    mHuman.add(myResourceActions.getResourceImportAction());
    bar.add(mHuman);

    HelpMenu helpMenu = new HelpMenu(getProject(), getUIFacade(), getProjectUIFacade());
    bar.add(helpMenu.createMenu());

    System.err.println("4. creating views...");
    myGanttChartTabContent = new GanttChartTabContentPanel(getProject(), getUIFacade(), getTaskTree(), area,
        getUIConfiguration());
    getViewManager().createView(myGanttChartTabContent, new ImageIcon(getClass().getResource("/icons/tasks_16.gif")));
    getViewManager().toggleVisible(myGanttChartTabContent);

    myResourceChartTabContent = new ResourceChartTabContentPanel(getProject(), getUIFacade(), getResourcePanel(),
        getResourcePanel().area);
    getViewManager().createView(myResourceChartTabContent, new ImageIcon(getClass().getResource("/icons/res_16.gif")));
    getViewManager().toggleVisible(myResourceChartTabContent);

    addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent evt) {
        quitApplication();
      }

      @Override
      public void windowOpened(WindowEvent e) {
        myRowHeightAligner.optionsChanged();
      }
    });

    System.err.println("5. calculating size and packing...");
    createContentPane();
    addButtons(getToolBar());
    // Chart tabs
    getTabs().setSelectedIndex(0);

    System.err.println("6. changing language ...");
    languageChanged(null);
    // Add Listener after language update (to be sure that it is not updated
    // twice)
    language.addListener(this);

    System.err.println("7. changing look'n'feel ...");
    getUIFacade().setLookAndFeel(getUIFacade().getLookAndFeel());
    if (options.isLoaded()) {
      setBounds(options.getX(), options.getY(), options.getWidth(), options.getHeight());
    }

    System.err.println("8. finalizing...");
    // applyComponentOrientation(GanttLanguage.getInstance()
    // .getComponentOrientation());
    myTaskManager.addTaskListener(new TaskModelModificationListener(this));
    if (ourWindowListener != null) {
      addWindowListener(ourWindowListener);
    }
    addMouseListenerToAllContainer(this.getComponents());
    myDelayManager = new DelayManager(myTaskManager, getUndoManager(), tree);
    Mediator.registerDelayManager(myDelayManager);
    myDelayManager.addObserver(tree);

    // Add globally available actions/key strokes
    GPAction viewCycleForwardAction = new ViewCycleAction(getViewManager(), true);
    UIUtil.pushAction(getTabs(), true, viewCycleForwardAction.getKeyStroke(), viewCycleForwardAction);

    GPAction viewCycleBackwardAction = new ViewCycleAction(getViewManager(), false);
    UIUtil.pushAction(getTabs(), true, viewCycleBackwardAction.getKeyStroke(), viewCycleBackwardAction);
  }

  @Override
  public TaskContainmentHierarchyFacade getTaskContainment() {
    if (myFacadeInvalidator == null) {
      return TaskContainmentHierarchyFacade.STUB;
    }
    if (!myFacadeInvalidator.isValid() || myCachedFacade == null) {
      myCachedFacade = new TaskContainmentHierarchyFacadeImpl(tree);
      myFacadeInvalidator.reset();
    }
    return myCachedFacade;
  }

  private void initOptions() {
    // Color color = GanttGraphicArea.taskDefaultColor;
    // myApplicationConfig.register(options);
    options.setUIConfiguration(myUIConfiguration);
    if (options.load()) {
      GanttGraphicArea.taskDefaultColor = options.getDefaultColor();

      HttpDocument.setLockDAVMinutes(options.getLockDAVMinutes());
    }

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

  /** @return A mouseListener that stop the edition in the ganttTreeTable. */
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

  /** @return the options of ganttproject. */
  public GanttOptions getGanttOptions() {
    return options;
  }

  /** @return the options of the GanttChart */
  public GPOptionGroup[] getGanttOptionsGroup() {
    return area.getOptionGroups();
  }

  public void restoreOptions() {
    options.initDefault();
    myUIConfiguration = options.getUIConfiguration();
    GanttGraphicArea.taskDefaultColor = new Color(140, 182, 206);
    area.repaint();
  }

  // TODO Move language updating methods which do not belong to GanttProject to
  // their own class with their own listener
  /** Function to change language of the project */
  @Override
  public void languageChanged(Event event) {
    applyComponentOrientation(language.getComponentOrientation());
    area.repaint();
    getResourcePanel().area.repaint();

    this.tree.changeLanguage(language);
    CustomColumnsStorage.changeLanguage(language);

    applyComponentOrientation(language.getComponentOrientation());
  }

  /** @return the ToolTip in HTML (with gray bgcolor) */
  public static String getToolTip(String msg) {
    return "<html><body bgcolor=#EAEAEA>" + msg + "</body></html>";
  }

  /** Create the button on toolbar */
  private void addButtons(JToolBar toolBar) {
    List<JButton> buttons = new ArrayList<JButton>();
    buttons.add(new TestGanttRolloverButton(myProjectMenu.getOpenProjectAction().withIcon(IconSize.TOOLBAR_SMALL)));
    buttons.add(new TestGanttRolloverButton(myProjectMenu.getSaveProjectAction().withIcon(IconSize.TOOLBAR_SMALL)));
    buttons.add(null);

    final ArtefactAction newAction;
    {
      newAction = new ArtefactNewAction(new ActiveActionProvider() {
        @Override
        public AbstractAction getActiveAction() {
          return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? getTaskTree().getNewAction().withIcon(
              IconSize.TOOLBAR_SMALL) : getResourceTree().getNewAction().withIcon(IconSize.TOOLBAR_SMALL);
        }
      });
    }

    final ArtefactAction deleteAction;
    {
      final GPAction taskDeleteAction = getTaskTree().getDeleteAction().withIcon(IconSize.TOOLBAR_SMALL);
      final GPAction resourceDeleteAction = getResourceTree().getDeleteAction().withIcon(IconSize.TOOLBAR_SMALL);
      deleteAction = new ArtefactDeleteAction(new ActiveActionProvider() {
        @Override
        public AbstractAction getActiveAction() {
          return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? taskDeleteAction : resourceDeleteAction;
        }
      }, new Action[] { taskDeleteAction, resourceDeleteAction });
    }

    final ArtefactAction propertiesAction;
    {
      final GPAction taskPropertiesAction = getTaskTree().getPropertiesAction().withIcon(IconSize.TOOLBAR_SMALL);
      final GPAction resourcePropertiesAction = getResourceTree().getPropertiesAction().withIcon(IconSize.TOOLBAR_SMALL);
      propertiesAction = new ArtefactPropertiesAction(new ActiveActionProvider() {
        @Override
        public AbstractAction getActiveAction() {
          return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? taskPropertiesAction : resourcePropertiesAction;
        }
      }, new Action[] { taskPropertiesAction, resourcePropertiesAction });
    }

    UIUtil.registerActions(getRootPane(), false, newAction, propertiesAction, deleteAction);
    // UIUtil.registerActions(toolBar, false, newAction, propertiesAction,
    // deleteAction);
    UIUtil.registerActions(myGanttChartTabContent.getComponent(), true, newAction, propertiesAction, deleteAction);
    UIUtil.registerActions(myResourceChartTabContent.getComponent(), true, newAction, propertiesAction, deleteAction);
    getTabs().addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        // Tell artefact actions that the active provider changed, so they
        // are able to update their state according to the current delegate
        newAction.actionStateChanged();
        propertiesAction.actionStateChanged();
        deleteAction.actionStateChanged();
        getTabs().getSelectedComponent().requestFocus();
      }
    });

    bNew = new TestGanttRolloverButton(newAction);
    buttons.add(bNew);
    buttons.add(new TestGanttRolloverButton(deleteAction));
    buttons.add(null);

    buttons.add(new TestGanttRolloverButton(propertiesAction));
    buttons.add(new TestGanttRolloverButton(getCutAction().withIcon(IconSize.TOOLBAR_SMALL)));
    buttons.add(new TestGanttRolloverButton(getCopyAction().withIcon(IconSize.TOOLBAR_SMALL)));
    buttons.add(new TestGanttRolloverButton(getPasteAction().withIcon(IconSize.TOOLBAR_SMALL)));
    buttons.add(null);

    buttons.add(new TestGanttRolloverButton(myEditMenu.getUndoAction().withIcon(IconSize.TOOLBAR_SMALL)));
    buttons.add(new TestGanttRolloverButton(myEditMenu.getRedoAction().withIcon(IconSize.TOOLBAR_SMALL)));

    JPanel paddingLeft = new JPanel();
    paddingLeft.setPreferredSize(new Dimension(12, 24));
    toolBar.add(paddingLeft);
    for (JButton b : buttons) {
      if (b == null) {
        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(24, 24));
        toolBar.add(separator);
      } else {
        b.setAlignmentY(TOP_ALIGNMENT);
        toolBar.add(b);
      }
    }

    JTextField searchBox = getSearchUi().getSearchField();
    searchBox.setMaximumSize(new Dimension(searchBox.getPreferredSize().width, buttons.get(0).getPreferredSize().height));
    JPanel tailPanel = new JPanel(new BorderLayout());

    JPanel searchPanel = new JPanel();
    searchPanel.add(searchBox);
    tailPanel.add(searchPanel, BorderLayout.EAST);
    tailPanel.setAlignmentY(TOP_ALIGNMENT);
    tailPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
    toolBar.add(tailPanel);
  }

  @Override
  public List<GanttPreviousState> getBaselines() {
    return myPreviousStates;
  }

  /** Create a new task */
  @Override
  public Task newTask() {
    getTabs().setSelectedIndex(UIFacade.GANTT_INDEX);

    int index = -1;
    TreeTableNode selectedNode = getTree().getSelectedNode();
    if (selectedNode != null) {
      TreeTableNode parent1 = selectedNode.getParent();
      index = parent1.getIndex(selectedNode) + 1;
      int selectedRow = tree.getTreeTable().getTree().getRowForPath(TreeUtil.createPath(parent1));
      tree.getTreeTable().getTree().getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
      tree.getTreeTable().getTreeTable().editingStopped(new ChangeEvent(tree.getTreeTable().getTreeTable()));
    }

    GanttCalendar cal = new GanttCalendar(area.getStartDate());

    MutableTreeTableNode node = tree.getSelectedNode();
    String nameOfTask = getTaskManager().getTaskNamePrefixOption().getValue();
    GanttTask task = getTaskManager().createTask();
    task.setStart(cal);
    task.setDuration(getTaskManager().createLength(1));
    getTaskManager().registerTask(task);
    task.setName(nameOfTask + "_" + task.getTaskID());
    task.setColor(area.getTaskColor());
    tree.addObject(task, node, index);

    // this will add new custom columns to the newly created task.
    AdjustTaskBoundsAlgorithm alg = getTaskManager().getAlgorithmCollection().getAdjustTaskBoundsAlgorithm();
    alg.run(task);
    RecalculateTaskCompletionPercentageAlgorithm alg2 = getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm();
    alg2.run(task);
    area.repaint();
    setAskForSave(true);
    getUIFacade().setStatusText(language.getText("createNewTask"));
    // setQuickSave(true);
    tree.setEditingTask(task);
    repaint2();
    return task;
  }

  /** Refresh the information of the project on the status bar. */
  public void refreshProjectInformation() {
    if (getTaskManager().getTaskCount() == 0 && resp.nbPeople() == 0) {
      getStatusBar().setSecondText("");
    } else {
      getStatusBar().setSecondText(
          language.getCorrectedLabel("task") + " : " + getTaskManager().getTaskCount() + "  "
              + language.getCorrectedLabel("resources") + " : " + resp.nbPeople());
    }
  }

  /** Print the project */
  public void printProject() {
    Chart chart = getUIFacade().getActiveChart();
    if (chart == null) {
      getUIFacade().showErrorDialog(
          "Failed to find active chart.\nPlease report this problem to GanttProject development team");
      return;
    }
    try {
      PrintManager.printChart(chart, options.getExportSettings());
    } catch (OutOfMemoryError e) {
      getUIFacade().showErrorDialog(GanttLanguage.getInstance().getText("printing.out_of_memory"));
    }
  }

  /** Create a new project */
  public void newProject() {
    getProjectUIFacade().createProject(getProject());
    fireProjectCreated();
  }

  @Override
  public void open(Document document) throws IOException, DocumentException {
    document.read();
    myMRU.add(document);
    projectDocument = document;
    setTitle(language.getText("appliTitle") + " [" + document.getFileName() + "]");
    for (Chart chart : PluginManager.getCharts()) {
      chart.reset();
    }

    // myDelayManager.fireDelayObservation(); // it is done in repaint2
    addMouseListenerToAllContainer(this.getComponents());

    getTaskManager().projectOpened();
    fireProjectOpened();
    // As we just have opened a new file it is still unmodified, so mark it as
    // such
    setModified(false);
  }

  public void openStartupDocument(String path) {
    if (path != null) {
      final Document document = getDocumentManager().getDocument(path);
      // openStartupDocument(document);
      getUndoManager().undoableEdit("OpenFile", new Runnable() {
        @Override
        public void run() {
          try {
            getProjectUIFacade().openProject(document, getProject());
          } catch (DocumentException e) {
            if (!tryImportDocument(document)) {
              getUIFacade().showErrorDialog(e);
            }
          } catch (IOException e) {
            if (!tryImportDocument(document)) {
              getUIFacade().showErrorDialog(e);
            }
          }
        }
      });
    }
  }

  private boolean tryImportDocument(Document document) {
    boolean success = false;
    List<Importer> importers = PluginManager.getExtensions(Importer.EXTENSION_POINT_ID, Importer.class);
    for (Importer importer : importers) {
      if (Pattern.matches(".*(" + importer.getFileNamePattern() + ")$", document.getFilePath())) {
        try {
          importer.setContext(getProject(), getUIFacade(), getGanttOptions().getPluginPreferences());
          importer.run(new File(document.getFilePath()));
          success = true;
          break;
        } catch (Throwable e) {
          if (!GPLogger.log(e)) {
            e.printStackTrace(System.err);
          }
        }
      }
    }
    return success;
  }

  /** Save the project as (with a dialog file chooser) */
  public boolean saveAsProject() {
    getProjectUIFacade().saveProjectAs(getProject());
    return true;
  }

  /** Save the project on a file */
  public void saveProject() {
    getProjectUIFacade().saveProject(getProject());
  }

  public void changeWorkingDirectory(String newWorkDir) {
    if (null != newWorkDir) {
      options.setWorkingDirectory(newWorkDir);
    }
  }

  /** @return the UIConfiguration. */
  @Override
  public UIConfiguration getUIConfiguration() {
    return myUIConfiguration;
  }

  /** Quit the application */
  public void quitApplication() {
    options.setWindowPosition(getX(), getY());
    options.setWindowSize(getWidth(), getHeight());
    options.setUIConfiguration(myUIConfiguration);
    options.save();
    if (getProjectUIFacade().ensureProjectSaved(getProject())) {
      getProject().close();
      setVisible(false);
      dispose();
      System.exit(0);
    } else {
      setVisible(true);
    }
  }

  public void setAskForSave(boolean afs) {
    if (isOnlyViewer) {
      return;
    }
    fireProjectModified(afs);
    String title = getTitle();
    askForSave = afs;
    try {
      if (System.getProperty("mrj.version") != null) {
        rootPane.putClientProperty("windowModified", Boolean.valueOf(afs));
        // see http://developer.apple.com/qa/qa2001/qa1146.html
      } else {
        if (askForSave) {
          if (!title.endsWith(" *")) {
            setTitle(title + " *");
          }
        }
      }
    } catch (AccessControlException e) {
      // This can happen when running in a sandbox (Java WebStart)
      System.err.println(e + ": " + e.getMessage());
    }
  }

  public GanttResourcePanel getResourcePanel() {
    if (this.resp == null) {
      this.resp = new GanttResourcePanel(this, getUIFacade());
      getHumanResourceManager().addView(this.resp);
    }
    return this.resp;
  }

  public GanttGraphicArea getArea() {
    return this.area;
  }

  public GanttTree2 getTree() {
    if (tree == null) {
      tree = new GanttTree2(this, getTaskManager(), getTaskSelectionManager(), getUIFacade());
    }
    return tree;
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
    @Parameter(names = "-log", description = "Enable logging")
    public boolean log = true;

    @Parameter(names = "-log_file", description = "Log file name")
    public String logFile = "auto";

    @Parameter(names = { "-h", "-help" }, description = "Print usage")
    public boolean help = false;

    @Parameter(description = "Input file name")
    public List<String> file = null;
  }

  /** The main */
  public static boolean main(String[] arg) {
    URL logConfig = GanttProject.class.getResource("/logging.properties");
    if (logConfig != null) {
      try {
        GPLogger.readConfiguration(logConfig);
      } catch (IOException e) {
        System.err.println("Failed to setup logging: " + e.getMessage());
        e.printStackTrace();
      }
    }

    CommandLineExportApplication cmdlineApplication = new CommandLineExportApplication();
    Args mainArgs = new Args();
    try {
      JCommander cmdLineParser = new JCommander(new Object[] { mainArgs, cmdlineApplication.getArguments() }, arg);
      if (mainArgs.help) {
        cmdLineParser.usage();
        System.exit(0);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return false;
    }
    if (mainArgs.log && "auto".equals(mainArgs.logFile)) {
      mainArgs.logFile = System.getProperty("user.home") + File.separator + "ganttproject.log";
    }
    if (mainArgs.log && !mainArgs.logFile.trim().isEmpty()) {
      try {
        GPLogger.setLogFile(mainArgs.logFile);
        File logFile = new File(mainArgs.logFile);
        System.setErr(new PrintStream(new FileOutputStream(logFile)));
        System.out.println("Writing log to " + logFile.getAbsolutePath());
      } catch (IOException e) {
        System.err.println("Failed to write log to file: " + e.getMessage());
        e.printStackTrace();
      }
    }

    GPLogger.logSystemInformation();
    // Check if an export was requested from the command line
    if (cmdlineApplication.export(mainArgs)) {
      // Export succeeded so exit application
      return false;
    }

    GanttSplash splash = new GanttSplash();
    try {
      splash.setVisible(true);
      GanttProject ganttFrame = new GanttProject(false);
      System.err.println("Main frame created");
      if (mainArgs.file != null && !mainArgs.file.isEmpty()) {
        ganttFrame.openStartupDocument(mainArgs.file.get(0));
      } else {
        ganttFrame.fireProjectCreated();
      }
      ganttFrame.setVisible(true);
      if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
        OSXAdapter.registerMacOSXApplication(ganttFrame);
      }
      ganttFrame.getActiveChart().reset();
      return true;
    } catch (Throwable e) {
      e.printStackTrace();
      return false;
    } finally {
      splash.close();
      System.err.println("Splash closed");
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
              GPLogger.log(e);
            }
          });
        }
      });
    }
  }

  public static final String HUMAN_RESOURCE_MANAGER_ID = "HUMAN_RESOURCE";

  public static final String ROLE_MANAGER_ID = "ROLE_MANAGER";

  private GPCalendar myFakeCalendar = new WeekendCalendarImpl();

  private ParserFactory myParserFactory;

  private HumanResourceManager myHumanResourceManager;

  private RoleManager myRoleManager;

  private static WindowListener ourWindowListener;

  // ///////////////////////////////////////////////////////
  // IGanttProject implementation
  @Override
  public String getProjectName() {
    return prjInfos.getName();
  }

  @Override
  public void setProjectName(String projectName) {
    prjInfos.setName(projectName);
    setAskForSave(true);
  }

  @Override
  public String getDescription() {
    return prjInfos.getDescription();
  }

  @Override
  public void setDescription(String description) {
    prjInfos.setDescription(description);
    setAskForSave(true);
  }

  @Override
  public String getOrganization() {
    return prjInfos.getOrganization();
  }

  @Override
  public void setOrganization(String organization) {
    prjInfos.setOrganization(organization);
    setAskForSave(true);
  }

  @Override
  public String getWebLink() {
    return prjInfos.getWebLink();
  }

  @Override
  public void setWebLink(String webLink) {
    prjInfos.setWebLink(webLink);
    setAskForSave(true);
  }

  @Override
  public HumanResourceManager getHumanResourceManager() {
    if (myHumanResourceManager == null) {
      myHumanResourceManager = new HumanResourceManager(getRoleManager().getDefaultRole(),
          getResourceCustomPropertyManager());
      myHumanResourceManager.addView(this);
    }
    return myHumanResourceManager;
  }

  @Override
  public TaskManager getTaskManager() {
    return myTaskManager;
  }

  @Override
  public RoleManager getRoleManager() {
    if (myRoleManager == null) {
      myRoleManager = RoleManager.Access.getInstance();
    }
    return myRoleManager;
  }

  @Override
  public Document getDocument() {
    return projectDocument;
  }

  @Override
  public void setDocument(Document document) {
    projectDocument = document;
  }

  @Override
  public GPCalendar getActiveCalendar() {
    return myFakeCalendar;
  }

  @Override
  public void setModified() {
    setAskForSave(true);
  }

  @Override
  public void setModified(boolean modified) {
    setAskForSave(modified);

    String title = getTitle();
    if (modified == false && title.endsWith(" *")) {
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
    fireProjectClosed();
    prjInfos = new PrjInfos();
    RoleManager.Access.getInstance().clear();
    if (null != projectDocument) {
      projectDocument.releaseLock();
    }
    projectDocument = null;
    getTaskManager().projectClosed();
    getTaskCustomColumnManager().reset();
    getResourceCustomPropertyManager().reset();

    for (int i = 0; i < myPreviousStates.size(); i++) {
      myPreviousStates.get(i).remove();
    }
    myPreviousStates = new ArrayList<GanttPreviousState>();
    getTaskManager().getCalendar().clearPublicHolidays();
    setModified(false);
    myFacadeInvalidator.projectClosed();
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
    if (getStatusBar() != null) {
      // tabpane.setSelectedIndex(1);
      String description = language.getCorrectedLabel("resource.new.description");
      if (description == null) {
        description = language.getCorrectedLabel("resource.new");
      }
      getUIFacade().setStatusText(description);
      setAskForSave(true);
      refreshProjectInformation();
    }
  }

  @Override
  public void resourcesRemoved(ResourceEvent event) {
    refreshProjectInformation();
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
  public Chart getResourceChart() {
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
  public TaskTreeUIFacade getTaskTree() {
    return getTree();
  }

  @Override
  public ResourceTreeUIFacade getResourceTree() {
    return getResourcePanel();
  }

  private class ParserFactoryImpl implements ParserFactory {
    @Override
    public GPParser newParser() {
      return new GanttXMLOpen(prjInfos, getUIConfiguration(), getTaskManager(), getUIFacade());
    }

    @Override
    public GPSaver newSaver() {
      return new GanttXMLSaver(GanttProject.this, getTree(), getResourcePanel(), getArea(), getUIFacade());
    }
  }

  public void setRowHeight(int value) {
    tree.getTreeTable().getTable().setRowHeight(value);
  }

  public void repaint2() {
    getResourcePanel().getResourceTreeTableModel().updateResources();
    getResourcePanel().getResourceTreeTable().setRowHeight(20);
    if (myDelayManager != null) {
      myDelayManager.fireDelayObservation();
    }
    super.repaint();
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

  public static void setWindowListener(WindowListener windowListener) {
    ourWindowListener = windowListener;
  }

  @Override
  public void refresh() {
    getTaskManager().processCriticalPath(getTaskManager().getRootTask());
    getResourcePanel().getResourceTreeTableModel().updateResources();
    getResourcePanel().getResourceTreeTable().setRowHeight(20);
    if (myDelayManager != null) {
      myDelayManager.fireDelayObservation();
    }
    for (Chart chart : PluginManager.getCharts()) {
      chart.reset();
    }
    super.repaint();
  }
}