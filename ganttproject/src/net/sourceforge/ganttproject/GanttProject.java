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

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.GPCalendarListener;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.TimeUnitStack;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.action.ActiveActionProvider;
import net.sourceforge.ganttproject.action.ArtefactAction;
import net.sourceforge.ganttproject.action.ArtefactDeleteAction;
import net.sourceforge.ganttproject.action.ArtefactNewAction;
import net.sourceforge.ganttproject.action.ArtefactPropertiesAction;
import net.sourceforge.ganttproject.action.GPAction;
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
import net.sourceforge.ganttproject.chart.overview.GPToolbar;
import net.sourceforge.ganttproject.chart.overview.ToolbarBuilder;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.document.DocumentCreator;
import net.sourceforge.ganttproject.export.CommandLineExportApplication;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.ProjectMRUMenu;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
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
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.task.TaskManagerImpl;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Main frame of the project
 */
public class GanttProject extends GanttProjectBase implements ResourceView, GanttLanguage.Listener {
  private static final ExecutorService ourExecutor = Executors.newSingleThreadExecutor();

  /**
   * The JTree part.
   */
  private GanttTree2 tree;

  /**
   * GanttGraphicArea for the calendar with Gantt
   */
  private GanttGraphicArea area;

  /**
   * GanttPeoplePanel to edit person that work on the project
   */
  private GanttResourcePanel resp;

  private final EditMenu myEditMenu;

  private final ProjectMenu myProjectMenu;

  /**
   * The project filename
   */
  public Document projectDocument = null;

  /**
   * Informations for the current project.
   */
  public PrjInfos prjInfos = new PrjInfos();

  /**
   * Boolean to know if the file has been modify
   */
  public boolean askForSave = false;

  /**
   * Is the application only for viewer.
   */
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

  private GanttChartTabContentPanel myGanttChartTabContent;

  private ResourceChartTabContentPanel myResourceChartTabContent;

  private List<RowHeightAligner> myRowHeightAligners = Lists.newArrayList();

  private final WeekendCalendarImpl myCalendar = new WeekendCalendarImpl();

  private ParserFactory myParserFactory;

  private HumanResourceManager myHumanResourceManager;

  private RoleManager myRoleManager;

  private static Runnable ourQuitCallback;


  public GanttProject(boolean isOnlyViewer) {
    System.err.println("Creating main frame...");
    ToolTipManager.sharedInstance().setInitialDelay(200);
    ToolTipManager.sharedInstance().setDismissDelay(60000);

    myCalendar.addListener(new GPCalendarListener() {
      @Override
      public void onCalendarChange() {
        GanttProject.this.setModified();
      }
    });
    Mediator.registerTaskSelectionManager(getTaskSelectionManager());

    this.isOnlyViewer = isOnlyViewer;
    if (!isOnlyViewer) {
      setTitle(language.getText("appliTitle"));
    } else {
      setTitle("GanttViewer");
    }
    setFocusable(true);
    System.err.println("1. loading look'n'feels");
    options = new GanttOptions(getRoleManager(), getDocumentManager(), isOnlyViewer);
    myUIConfiguration = options.getUIConfiguration();
    myUIConfiguration.setChartFontOption(getUiFacadeImpl().getChartFontOption());
    myUIConfiguration.setDpiOption(getUiFacadeImpl().getDpiOption());

    class TaskManagerConfigImpl implements TaskManagerConfig {
      final DefaultColorOption myDefaultColorOption = new GanttProjectImpl.DefaultTaskColorOption();

      @Override
      public Color getDefaultColor() {
        return getUIFacade().getGanttChart().getTaskDefaultColorOption().getValue();
      }

      @Override
      public ColorOption getDefaultColorOption() {
        return myDefaultColorOption;
      }

      @Override
      public GPCalendarCalc getCalendar() {
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

      @Override
      public NotificationManager getNotificationManager() {
        return getUIFacade().getNotificationManager();
      }

    }
    TaskManagerConfig taskConfig = new TaskManagerConfigImpl();
    myTaskManager = TaskManager.Access.newInstance(new TaskContainmentHierarchyFacade.Factory() {
      @Override
      public TaskContainmentHierarchyFacade createFacade() {
        return GanttProject.this.getTaskContainment();
      }
    }, taskConfig);
    addProjectEventListener(myTaskManager.getProjectListener());
    getActiveCalendar().addListener(myTaskManager.getCalendarListener());
    ImageIcon icon = new ImageIcon(getClass().getResource("/icons/ganttproject.png"));
    setIconImage(icon.getImage());


    myFacadeInvalidator = new FacadeInvalidator(getTree().getModel(), myRowHeightAligners);
    getProject().addProjectEventListener(myFacadeInvalidator);
    area = new GanttGraphicArea(this, getTree(), getTaskManager(), getZoomManager(), getUndoManager());
    getTree().init();
    options.addOptionGroups(getUIFacade().getOptions());
    options.addOptionGroups(getUIFacade().getGanttChart().getOptionGroups());
    options.addOptionGroups(getUIFacade().getResourceChart().getOptionGroups());
    options.addOptionGroups(getProjectUIFacade().getOptionGroups());
    options.addOptionGroups(getDocumentManager().getNetworkOptionGroups());
    options.addOptions(getRssFeedChecker().getOptions());

    System.err.println("2. loading options");
    initOptions();
    // Not a joke. This takes value from the option and applies it to the UI.
    getTree().setGraphicArea(area);
    getUIFacade().setLookAndFeel(getUIFacade().getLookAndFeel());
    myRowHeightAligners.add(getTree().getRowHeightAligner());
    getUiFacadeImpl().getAppFontOption().addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        for (RowHeightAligner aligner : myRowHeightAligners) {
          aligner.optionsChanged();
        }
      }
    });

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
    getDocumentManager().addListener(mruMenu);

    myProjectMenu = new ProjectMenu(this, mruMenu, "project");
    bar.add(myProjectMenu);

    myEditMenu = new EditMenu(getProject(), getUIFacade(), getViewManager(), getSearchUi(), "edit");
    bar.add(myEditMenu);

    ViewMenu viewMenu = new ViewMenu(getProject(), getViewManager(), getUiFacadeImpl().getDpiOption(), getUiFacadeImpl().getChartFontOption(), "view");
    bar.add(viewMenu);

    {
      TaskTreeUIFacade taskTree = getUIFacade().getTaskTree();
      JMenu mTask = UIUtil.createTooltiplessJMenu(GPAction.createVoidAction("task"));
      mTask.add(taskTree.getNewAction());
      mTask.add(taskTree.getPropertiesAction());
      mTask.add(taskTree.getDeleteAction());
      getResourcePanel().setTaskPropertiesAction(taskTree.getPropertiesAction());
      bar.add(mTask);
    }
    JMenu mHuman = UIUtil.createTooltiplessJMenu(GPAction.createVoidAction("human"));
    for (AbstractAction a : myResourceActions.getActions()) {
      mHuman.add(a);
    }
    mHuman.add(myResourceActions.getResourceSendMailAction());
    bar.add(mHuman);

    HelpMenu helpMenu = new HelpMenu(getProject(), getUIFacade(), getProjectUIFacade());
    bar.add(helpMenu.createMenu());

    System.err.println("4. creating views...");
    myGanttChartTabContent = new GanttChartTabContentPanel(getProject(), getUIFacade(), getTree(), area.getJComponent(),
        getUIConfiguration());
    getViewManager().createView(myGanttChartTabContent, new ImageIcon(getClass().getResource("/icons/tasks_16.gif")));
    getViewManager().toggleVisible(myGanttChartTabContent);

    myResourceChartTabContent = new ResourceChartTabContentPanel(getProject(), getUIFacade(), getResourcePanel(),
        getResourcePanel().area);
    getViewManager().createView(myResourceChartTabContent, new ImageIcon(getClass().getResource("/icons/res_16.gif")));
    getViewManager().toggleVisible(myResourceChartTabContent);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            getGanttChart().reset();
            getResourceChart().reset();
            // This will clear any modifications which might be caused by
            // adjusting widths of table columns during initial layout process.
            getProject().setModified(false);
          }
        });
      }
    });

    System.err.println("5. calculating size and packing...");
    final GPToolbar toolbar = createToolbar();
    createContentPane(toolbar.getToolbar());
    //final List<? extends JComponent> buttons = addButtons(getToolBar());
    // Chart tabs
    getTabs().setSelectedIndex(0);

    System.err.println("6. changing language ...");
    languageChanged(null);
    // Add Listener after language update (to be sure that it is not updated
    // twice)
    language.addListener(this);

    System.err.println("7. first attempt to restore bounds");
    restoreBounds();
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent windowEvent) {
        quitApplication();
      }

      @Override
      public void windowOpened(WindowEvent e) {
        System.err.println("Resizing window...");
        toolbar.updateButtons();
        GPLogger.log(String.format("Bounds after opening: %s", GanttProject.this.getBounds()));
        restoreBounds();
        // It is important to run aligners after look and feel is set and font sizes
        // in the UI manager updated.
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            for (RowHeightAligner aligner : myRowHeightAligners) {
              aligner.optionsChanged();
            }
          }
        });
        getUiFacadeImpl().getDpiOption().addChangeValueListener(new ChangeValueListener() {
          @Override
          public void changeValue(ChangeValueEvent event) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                getContentPane().doLayout();
              }
            });
          }
        });
      }
    });

    System.err.println("8. finalizing...");
    // applyComponentOrientation(GanttLanguage.getInstance()
    // .getComponentOrientation());
    myTaskManager.addTaskListener(new TaskModelModificationListener(this, getUIFacade()));
    addMouseListenerToAllContainer(this.getComponents());

    // Add globally available actions/key strokes
    GPAction viewCycleForwardAction = new ViewCycleAction(getViewManager(), true);
    UIUtil.pushAction(getTabs(), true, viewCycleForwardAction.getKeyStroke(), viewCycleForwardAction);

    GPAction viewCycleBackwardAction = new ViewCycleAction(getViewManager(), false);
    UIUtil.pushAction(getTabs(), true, viewCycleBackwardAction.getKeyStroke(), viewCycleBackwardAction);
  }


  private void restoreBounds() {
    if (options.isLoaded()) {
      if (options.isMaximized()) {
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
      }
      Rectangle bounds = new Rectangle(options.getX(), options.getY(), options.getWidth(), options.getHeight());
      GPLogger.log(String.format("Bounds stored in the  options: %s", bounds));

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
            GPLogger.log(String.format("We have not found the display corresponding to bounds %s. Leaving the window where it is", bounds));
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
   * @return the options of the GanttChart
   */
  public GPOptionGroup[] getGanttOptionsGroup() {
    return area.getOptionGroups();
  }

  public void restoreOptions() {
    options.initDefault();
    myUIConfiguration = options.getUIConfiguration();
    area.repaint();
  }

  // TODO Move language updating methods which do not belong to GanttProject to
  // their own class with their own listener

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
  private GPToolbar createToolbar() {
    ToolbarBuilder builder = new ToolbarBuilder()
        .withHeight(40)
        .withDpiOption(getUiFacadeImpl().getDpiOption())
        .withLafOption(getUiFacadeImpl().getLafOption(), new Function<String, Float>() {
          @Override
          public Float apply(@Nullable String s) {
            return (s.indexOf("nimbus") >= 0) ? 1.5f : 1f;
          }
        })
        .withSquareButtons()
        .withBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));
    builder.addButton(new TestGanttRolloverButton(myProjectMenu.getOpenProjectAction().asToolbarAction()))
        .addButton(new TestGanttRolloverButton(myProjectMenu.getSaveProjectAction().asToolbarAction()))
        .addWhitespace();

    final ArtefactAction newAction;
    {
      final GPAction taskNewAction = getTaskTree().getNewAction().asToolbarAction();
      final GPAction resourceNewAction = getResourceTree().getNewAction().asToolbarAction();
      newAction = new ArtefactNewAction(new ActiveActionProvider() {
        @Override
        public AbstractAction getActiveAction() {
          return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? taskNewAction : resourceNewAction;
        }
      }, new Action[]{taskNewAction, resourceNewAction});
      final TestGanttRolloverButton bNewTask = new TestGanttRolloverButton(taskNewAction);
      final TestGanttRolloverButton bnewResource = new TestGanttRolloverButton(resourceNewAction);
      builder.addButton(bNewTask).addButton(bnewResource);
      getTabs().addChangeListener(new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent changeEvent) {
          switch (getTabs().getSelectedIndex()) {
            case UIFacade.GANTT_INDEX:
              bNewTask.setVisible(true);
              bnewResource.setVisible(false);
              return;
            case UIFacade.RESOURCES_INDEX:
              bNewTask.setVisible(false);
              bnewResource.setVisible(true);
              return;
          }
        }
      });
    }

    final ArtefactAction deleteAction;
    {
      final GPAction taskDeleteAction = getTaskTree().getDeleteAction().asToolbarAction();
      final GPAction resourceDeleteAction = getResourceTree().getDeleteAction().asToolbarAction();
      deleteAction = new ArtefactDeleteAction(new ActiveActionProvider() {
        @Override
        public AbstractAction getActiveAction() {
          return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? taskDeleteAction : resourceDeleteAction;
        }
      }, new Action[]{taskDeleteAction, resourceDeleteAction});
    }

    final ArtefactAction propertiesAction;
    {
      final GPAction taskPropertiesAction = getTaskTree().getPropertiesAction().asToolbarAction();
      final GPAction resourcePropertiesAction = getResourceTree().getPropertiesAction().asToolbarAction();
      propertiesAction = new ArtefactPropertiesAction(new ActiveActionProvider() {
        @Override
        public AbstractAction getActiveAction() {
          return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? taskPropertiesAction : resourcePropertiesAction;
        }
      }, new Action[]{taskPropertiesAction, resourcePropertiesAction});
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

    builder.addButton(new TestGanttRolloverButton(deleteAction))
        .addWhitespace()
        .addButton(new TestGanttRolloverButton(propertiesAction))
        .addButton(new TestGanttRolloverButton(getCutAction().asToolbarAction()))
        .addButton(new TestGanttRolloverButton(getCopyAction().asToolbarAction()))
        .addButton(new TestGanttRolloverButton(getPasteAction().asToolbarAction()))
        .addWhitespace()
        .addButton(new TestGanttRolloverButton(myEditMenu.getUndoAction().asToolbarAction()))
        .addButton(new TestGanttRolloverButton(myEditMenu.getRedoAction().asToolbarAction()));

    JTextField searchBox = getSearchUi().getSearchField();
    //searchBox.setMaximumSize(new Dimension(searchBox.getPreferredSize().width, buttons.get(0).getPreferredSize().height));
    searchBox.setAlignmentY(CENTER_ALIGNMENT);
    JPanel tailPanel = new JPanel(new BorderLayout());

    //JPanel searchPanel = new JPanel();
    //searchPanel.add(searchBox);
    //searchPanel.setAlignmentY(CENTER_ALIGNMENT);
    tailPanel.add(searchBox, BorderLayout.EAST);
    //tailPanel.setAlignmentY(CENTER_ALIGNMENT);
    tailPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    builder.addPanel(tailPanel);

    //return result;
    final GPToolbar toolbar = builder.build();
    getUiFacadeImpl().addOnUpdateComponentTreeUi(new Runnable() {
      @Override
      public void run() {
        toolbar.resize();
      }
    });
    return toolbar;
  }

  @Override
  public List<GanttPreviousState> getBaselines() {
    return myPreviousStates;
  }

  /**
   * Refresh the information of the project on the status bar.
   */
  public void refreshProjectInformation() {
    if (getTaskManager().getTaskCount() == 0 && resp.nbPeople() == 0) {
      getStatusBar().setSecondText("");
    } else {
      getStatusBar().setSecondText(
          language.getCorrectedLabel("task") + " : " + getTaskManager().getTaskCount() + "  "
              + language.getCorrectedLabel("resources") + " : " + resp.nbPeople());
    }
  }

  /**
   * Print the project
   */
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

  /**
   * Create a new project
   */
  public void newProject() {
    getProjectUIFacade().createProject(getProject());
    fireProjectCreated();
  }

  @Override
  public void open(Document document) throws IOException, DocumentException {
    document.read();
    getDocumentManager().addToRecentDocuments(document);
    //myMRU.add(document.getPath(), true);
    projectDocument = document;
    setTitle(language.getText("appliTitle") + " [" + document.getFileName() + "]");
    for (Chart chart : PluginManager.getCharts()) {
      chart.reset();
    }

    // myDelayManager.fireDelayObservation(); // it is done in repaint2
    addMouseListenerToAllContainer(this.getComponents());

    fireProjectOpened();
  }

  public void openStartupDocument(String path) {
    if (path != null) {
      final Document document = getDocumentManager().getDocument(path);
      try {
        getProjectUIFacade().openProject(document, getProject());
      } catch (DocumentException e) {
        fireProjectCreated(); // this will create columns in the tables, which are removed by previous call to openProject()
        if (!tryImportDocument(document)) {
          getUIFacade().showErrorDialog(e);
        }
      } catch (IOException e) {
        fireProjectCreated(); // this will create columns in the tables, which are removed by previous call to openProject()
        if (!tryImportDocument(document)) {
          getUIFacade().showErrorDialog(e);
        }
      }
    }
  }

  private boolean tryImportDocument(Document document) {
    boolean success = false;
    List<Importer> importers = PluginManager.getExtensions(Importer.EXTENSION_POINT_ID, Importer.class);
    for (Importer importer : importers) {
      if (Pattern.matches(".*(" + importer.getFileNamePattern() + ")$", document.getFilePath())) {
        try {
          ((TaskManagerImpl) getTaskManager()).setEventsEnabled(false);
          importer.setContext(getProject(), getUIFacade(), getGanttOptions().getPluginPreferences());
          importer.setFile(new File(document.getFilePath()));
          importer.run();
          success = true;
          break;
        } catch (Throwable e) {
          if (!GPLogger.log(e)) {
            e.printStackTrace(System.err);
          }
        } finally {
          ((TaskManagerImpl) getTaskManager()).setEventsEnabled(true);
        }
      }
    }
    return success;
  }

  /**
   * Save the project as (with a dialog file chooser)
   */
  public boolean saveAsProject() {
    getProjectUIFacade().saveProjectAs(getProject());
    return true;
  }

  /**
   * Save the project on a file
   */
  public void saveProject() {
    getProjectUIFacade().saveProject(getProject());
  }

  public void changeWorkingDirectory(String newWorkDir) {
    if (null != newWorkDir) {
      options.setWorkingDirectory(newWorkDir);
    }
  }

  /**
   * @return the UIConfiguration.
   */
  @Override
  public UIConfiguration getUIConfiguration() {
    return myUIConfiguration;
  }

  private boolean myQuitEntered = false;

  /**
   * Quit the application
   */
  public boolean quitApplication() {
    if (myQuitEntered) {
      return false;
    }
    myQuitEntered = true;
    try {
      options.setWindowPosition(getX(), getY());
      options.setWindowSize(getWidth(), getHeight(), (getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);
      options.setUIConfiguration(myUIConfiguration);
      options.save();
      if (getProjectUIFacade().ensureProjectSaved(getProject())) {
        getProject().close();
        setVisible(false);
        dispose();
        if (ourQuitCallback != null) {
          ourQuitCallback.run();
        }
        return true;
      } else {
        setVisible(true);
        return false;
      }
    } finally {
      myQuitEntered = false;
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
      this.resp.init();
      myRowHeightAligners.add(this.resp.getRowHeightAligner());
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
    @Parameter(names = "-log", description = "Enable logging", arity = 1)
    public boolean log = true;

    @Parameter(names = "-log_file", description = "Log file name")
    public String logFile = "auto";

    @Parameter(names = {"-h", "-help"}, description = "Print usage")
    public boolean help = false;

    @Parameter(names = {"-version"}, description = "Print version number")
    public boolean version = false;

    @Parameter(description = "Input file name")
    public List<String> file = null;
  }

  /**
   * The main
   */
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
    final Args mainArgs = new Args();
    try {
      JCommander cmdLineParser = new JCommander(new Object[]{mainArgs, cmdlineApplication.getArguments()}, arg);
      if (mainArgs.help) {
        cmdLineParser.usage();
        System.exit(0);
      }
      if (mainArgs.version) {
        System.out.println(GPVersion.getCurrentVersionNumber());
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

    Runnable autosaveCleanup = DocumentCreator.createAutosaveCleanup();

    final AtomicReference<GanttSplash> splash = new AtomicReference<>(null);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        splash.set(new GanttSplash());
        splash.get().setVisible(true);
      }
    });
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e1) {
      GPLogger.log(e1);
    }
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          final GanttProject ganttFrame = new GanttProject(false);
          System.err.println("Main frame created");
          ganttFrame.fireProjectCreated();
          if (mainArgs.file != null && !mainArgs.file.isEmpty()) {
            ganttFrame.openStartupDocument(mainArgs.file.get(0));
          }
          ganttFrame.setVisible(true);
          GPLogger.log(String.format("Bounds after setVisible: %s", ganttFrame.getBounds()));
          try {
            Class.forName("java.awt.desktop.AboutHandler");
            DesktopIntegration.setup(ganttFrame);
          } catch (ClassNotFoundException e) {
            if (DesktopIntegration.isMacOs()) {
              OSXAdapter.registerMacOSXApplication(ganttFrame);
            }
          } finally {
            OSXAdapter.setupSystemProperties();
          }
          ganttFrame.getActiveChart().reset();
          ganttFrame.getRssFeedChecker().setOptionsVersion(ganttFrame.getGanttOptions().getVersion());
          ganttFrame.getRssFeedChecker().run();
          ganttFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        } catch (Throwable e) {
          e.printStackTrace();
        } finally {
          splash.get().close();
          System.err.println("Splash closed");
          Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
              GPLogger.log(e);
            }
          });
        }
      }
    });
    if (autosaveCleanup != null) {
      ourExecutor.submit(autosaveCleanup);
    }
    return true;
  }

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
  public GPCalendarCalc getActiveCalendar() {
    return myCalendar;
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
    projectDocument = null;
    getTaskCustomColumnManager().reset();
    getResourceCustomPropertyManager().reset();

    for (int i = 0; i < myPreviousStates.size(); i++) {
      myPreviousStates.get(i).remove();
    }
    myPreviousStates = new ArrayList<GanttPreviousState>();
    myCalendar.reset();
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

  public static void setApplicationQuitCallback(Runnable callback) {
    ourQuitCallback = callback;
  }

  @Override
  public void refresh() {
    getTaskManager().processCriticalPath(getTaskManager().getRootTask());
    getResourcePanel().getResourceTreeTableModel().updateResources();
    getResourcePanel().getResourceTreeTable().setRowHeight(getResourceChart().getModel().calculateRowHeight());
    for (Chart chart : PluginManager.getCharts()) {
      chart.reset();
    }
    super.repaint();
  }
}
