/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.action.EditMenu;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.ImportResources;
import net.sourceforge.ganttproject.action.NewArtefactAction;
import net.sourceforge.ganttproject.action.NewHumanAction;
import net.sourceforge.ganttproject.action.NewTaskAction;
import net.sourceforge.ganttproject.action.ResourceActionSet;
import net.sourceforge.ganttproject.action.SwitchViewAction;
import net.sourceforge.ganttproject.action.project.ProjectMenu;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.ToggleChartAction;
import net.sourceforge.ganttproject.delay.DelayManager;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentsMRU;
import net.sourceforge.ganttproject.document.HttpDocument;
import net.sourceforge.ganttproject.document.OpenDocumentAction;
import net.sourceforge.ganttproject.export.CommandLineExportApplication;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.NotificationManagerImpl;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.about.AboutDialog;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.importer.Importer;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.io.GanttXMLSaver;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.print.PrintManager;
import net.sourceforge.ganttproject.print.PrintPreview;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceContext;
import net.sourceforge.ganttproject.resource.ResourceEvent;
import net.sourceforge.ganttproject.resource.ResourceView;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.algorithm.AdjustTaskBoundsAlgorithm;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskCompletionPercentageAlgorithm;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.util.BrowserControl;

/**
 * Main frame of the project
 */
public class GanttProject extends GanttProjectBase implements ActionListener,
        IGanttProject, ResourceView, KeyListener, UIFacade {

    /** The current version of ganttproject */
    public static final String version = GPVersion.V2_0_X;

    /** The language use */
    private GanttLanguage language = GanttLanguage.getInstance();

    /** The JTree part. */
    private GanttTree2 tree;

    /** GanttGraphicArea for the calendar with Gantt */
    private GanttGraphicArea area;

    /** GanttPeoplePanel to edit person that work on the project */
    private GanttResourcePanel resp;

    /** Menu */
    private JMenu mProject, mMRU, /*mEdit,*/ mTask, mHuman, mHelp, mServer,
            mCalendar;

    // public JMenu mView;

    /** Menuitem */
    private JMenuItem miPreview,/* miCut, miCopy, miPaste, miOptions,*/
            miDeleteTask, /* miUp, miDown, */miDelHuman, miSendMailHuman,
            miPrjCal, miWebPage, miAbout, miChartOptions;

    private static final int maxSizeMRU = 5;

    private DocumentsMRU documentsMRU = new DocumentsMRU(maxSizeMRU);

    /** Toolbar button */
    private TestGanttRolloverButton bSave, bCopy, bCut, bPaste, bNewTask, bDelete,
            bProperties;

    private TestGanttRolloverButton bUndo, bRedo;

    /** The project filename */
    public Document projectDocument = null;

    /** Informations for the current project. */
    public PrjInfos prjInfos = new PrjInfos();

    /** Boolean to know if the file has been modify */
    public boolean askForSave = false;

    /** Is the application only for viewer. */
    public boolean isOnlyViewer;

    /** The list of all managers installed in this project */
    private Hashtable<String, Object> managerHash = new Hashtable<String, Object>();

    private ResourceActionSet myResourceActions;

    private final TaskManager myTaskManager;

    private final FacadeInvalidator myFacadeInvalidator;

    private UIConfiguration myUIConfiguration;

    private final GanttOptions options;

    private JMenuBar bar;

    private JToolBar toolBar;

    private Action myTaskPropertiesAction;

    private NewTaskAction myNewTaskAction;

    private NewHumanAction myNewHumanAction;

    private NewArtefactAction myNewArtefactAction;

    private Action myDeleteHumanAction;

    private TaskContainmentHierarchyFacadeImpl myCachedFacade;

    private ArrayList<GanttPreviousState> myPreviousStates = new ArrayList<GanttPreviousState>();

    private MouseListener myStopEditingMouseListener = null;

    private DelayManager myDelayManager;

    private ProjectMenu myProjectMenu;

    private GanttChartTabContentPanel myGanttChartTabContent;

    private ResourceChartTabContentPanel myResourceChartTabContent;

    private RowHeightAligner myRowHeightAligner;

    private final EditMenu myEditMenu;

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
        options.setDocumentsMRU(documentsMRU);
        if (options.load()) {
            GanttGraphicArea.taskDefaultColor = options.getDefaultColor();

            HttpDocument.setLockDAVMinutes(options.getLockDAVMinutes());
        }

        myUIConfiguration = options.getUIConfiguration();
    }

    public GanttProject(boolean isOnlyViewer, boolean isApplet) {
        System.err.println("Creating main frame...");
        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(60000);

        Mediator.registerTaskSelectionManager(getTaskSelectionManager());
        /*
         * [bbaranne] I add a Mediator object so that we can get the
         * GanttProject singleton where ever we are in the source code. Perhaps
         * some of you don't like this, but I believe that it is practical...
         */
        Mediator.registerGanttProject(this);

        this.isOnlyViewer = isOnlyViewer;
        if (!isOnlyViewer)
            setTitle(language.getText("appliTitle"));
        else
            setTitle("GanttViewer");
        setFocusable(true);
        System.err.println("1. loading look'n'feels");
        options = new GanttOptions(getRoleManager(), getDocumentManager(),
                isOnlyViewer);
        myUIConfiguration = options.getUIConfiguration();
        class TaskManagerConfigImpl implements TaskManagerConfig {
            public Color getDefaultColor() {
                return myUIConfiguration.getTaskColor();
            }

            public GPCalendar getCalendar() {
                return GanttProject.this.getActiveCalendar();
            }

            public TimeUnitStack getTimeUnitStack() {
                return GanttProject.this.getTimeUnitStack();
            }

            public HumanResourceManager getResourceManager() {
                return GanttProject.this.getHumanResourceManager();
            }

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
        myTaskManager = TaskManager.Access.newInstance(
                new TaskContainmentHierarchyFacade.Factory() {
                    public TaskContainmentHierarchyFacade createFacede() {
                        return GanttProject.this.getTaskContainment();
                    }
                }, taskConfig, getCustomColumnsStorage());
        ImageIcon icon = new ImageIcon(getClass().getResource(
                "/icons/ganttproject.png"));
        setIconImage(icon.getImage());

        // Create each objects
        myFacadeInvalidator = new FacadeInvalidator(getTree().getJTree()
                .getModel());
        getProject().addProjectEventListener(myFacadeInvalidator);
        area = new GanttGraphicArea(this, getTree(), getTaskManager(),
                getZoomManager(), getUndoManager());
        options.addOptionGroups(new GPOptionGroup[] {getUIFacade().getOptions()});
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

        miChartOptions = new JMenuItem(area.getOptionsDialogAction());

        getZoomManager().addZoomListener(area.getZoomListener());


        System.err.println("3. creating menu...");

        bar = new JMenuBar();
        setJMenuBar(bar);
        // Allocation of the menus
        mProject = new JMenu();
        mMRU = new JMenu();
        mMRU.setIcon(new ImageIcon(getClass().getResource(
                "/icons/recent_16.gif")));
        // mView = new JMenu ();
        mTask = new JMenu();
        mHuman = new JMenu();
        mHelp = new JMenu();
        mCalendar = new JMenu();

        createProjectMenu();
        bar.add(mProject);
        myEditMenu = new EditMenu(getProject(), getUIFacade(), getViewManager());
        bar.add(myEditMenu.create());

        myNewTaskAction = new NewTaskAction(getProject());
        mTask.add(myNewTaskAction);
        miDeleteTask = createNewItem("/icons/delete_16.gif");
        mTask.add(miDeleteTask);
        myTaskPropertiesAction = getTree().getTaskPropertiesAction();
        mTask.add(myTaskPropertiesAction);
        getResourcePanel().setTaskPropertiesAction(myTaskPropertiesAction);

        myNewHumanAction = new NewHumanAction(getHumanResourceManager(), this) {
            public void actionPerformed(ActionEvent event) {
                super.actionPerformed(event);
                getTabs().setSelectedIndex(UIFacade.RESOURCES_INDEX);
            }
        };

        mHuman.add(myNewHumanAction);
        myDeleteHumanAction = getResourceActions().getDeleteHumanAction();
        miDelHuman = new JMenuItem(myDeleteHumanAction);
        mHuman.add(miDelHuman);
        // miPropHuman = createNewItem("/icons/properties_16.gif");
        // mHuman.add(miPropHuman);
        mHuman.add(getResourcePanel().getResourcePropertiesAction());
        miSendMailHuman = createNewItem("/icons/send_mail_16.gif");
        mHuman.add(miSendMailHuman);

        mHuman.add(new ImportResources(getHumanResourceManager(),
                getTaskManager(), getRoleManager(), this));

        miPrjCal = createNewItem("/icons/default_calendar_16.gif");
        mCalendar.add(miPrjCal);
        miWebPage = createNewItem("/icons/home_16.gif");
        mHelp.add(miWebPage);
        miAbout = createNewItem("/icons/manual_16.gif");
        mHelp.add(miAbout);
        JMenu viewMenu = createViewMenu();
        if (viewMenu != null)
            bar.add(viewMenu);
        // bar.add (mView);
        bar.add(mTask);
        bar.add(mHuman);
        // bar.add(mCalendar);
        bar.add(mHelp);
        setMnemonic();
        // to create a default project
        // createDefaultTree(tree);
        System.err.println("4. creating views...");
        myGanttChartTabContent = new GanttChartTabContentPanel(
            getProject(), getUIFacade(), getTaskTree(), area, getUIConfiguration());
        GPView ganttView = getViewManager().createView(myGanttChartTabContent,
                new ImageIcon(getClass().getResource("/icons/tasks_16.gif")));
        ganttView.setVisible(true);
        myResourceChartTabContent = new ResourceChartTabContentPanel(
                getProject(), getUIFacade(), getResourcePanel(),
                getResourcePanel().area);
        GPView resourceView = getViewManager().createView(
                myResourceChartTabContent,
                new ImageIcon(getClass().getResource("/icons/res_16.gif")));
        resourceView.setVisible(true);
        getTabs().setSelectedIndex(0);

        // pert area
        // getTabs().setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        getTabs().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                bNewTask
                        .setEnabled(getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX
                                || getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX);

                bDelete
                        .setEnabled(getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX
                                || getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX);

                bProperties
                        .setEnabled(getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX
                                || getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX);

                if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) { // Gantt
                    // Chart
                    bNewTask.setToolTipText(getToolTip(correctLabel(language
                            .getText("createTask"))));
                    bDelete.setToolTipText(getToolTip(correctLabel(language
                            .getText("deleteTask"))));
                    bProperties.setToolTipText(getToolTip(correctLabel(language
                            .getText("propertiesTask"))));

                    if (options.getButtonShow() != GanttOptions.ICONS) {
                        bNewTask.setText(correctLabel(language
                                .getText("createTask")));
                        bDelete.setText(correctLabel(language
                                .getText("deleteTask")));
                        bProperties.setText(correctLabel(language
                                .getText("propertiesTask")));
                    }

                } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) { // Resources
                    // Chart
                    bNewTask.setToolTipText(getToolTip(correctLabel(language
                            .getText("newHuman"))));
                    bDelete.setToolTipText(getToolTip(correctLabel(language
                            .getText("deleteHuman"))));
                    bProperties.setToolTipText(getToolTip(correctLabel(language
                            .getText("propertiesHuman"))));

                    if (options.getButtonShow() != GanttOptions.ICONS) {
                        bNewTask.setText(correctLabel(language
                                .getText("newHuman")));
                        bDelete.setText(correctLabel(language
                                .getText("deleteHuman")));
                        bProperties.setText(correctLabel(language
                                .getText("propertiesHuman")));
                    }
                }
            }
        });
        // Add tab pane on the content pane
        getContentPane().add(getTabs(), BorderLayout.CENTER);
        // Add toolbar
        toolBar = new JToolBar();
        toolBar.addComponentListener(new ComponentListener() {

            public void componentResized(ComponentEvent arg0) {
                setHiddens();
                refresh();
            }

            public void componentMoved(ComponentEvent arg0) {
            }

            public void componentShown(ComponentEvent arg0) {
            }

            public void componentHidden(ComponentEvent arg0) {
            }
        });
        this.addButtons(toolBar);
        getContentPane()
                .add(
                        toolBar,
                        (toolBar.getOrientation() == JToolBar.HORIZONTAL) ? BorderLayout.NORTH
                                : BorderLayout.WEST);

        // add the status bar
        if (!isOnlyViewer)
            getContentPane().add(getStatusBar(), BorderLayout.SOUTH);
        getStatusBar().setVisible(options.getShowStatusBar());

        // add a keyboard listener
        addKeyListener(this);

        SwitchViewAction switchAction = new SwitchViewAction(this);
        JMenuItem invisibleItem = new JMenuItem(switchAction);
        invisibleItem.setVisible(false);
        bar.add(invisibleItem);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }

            public void windowOpened(WindowEvent e) {
                myRowHeightAligner.optionsChanged();
                ((NotificationManagerImpl)getNotificationManager()).showPending();
                getRssFeedChecker().run();

            }

        });

        System.err.println("5. calculating size and packing...");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = getPreferredSize();
        // Put the frame at the middle of the screen
        setLocation(screenSize.width / 2 - (windowSize.width / 2),
                screenSize.height / 2 - (windowSize.height / 2));
        this.pack();

        System.err.println("6. changing language ...");
        changeLanguage();

        // changeUndoNumber ();
        System.err.println("7. changing look'n'feel ...");
        getUIFacade().setLookAndFeel(getUIFacade().getLookAndFeel());
        if (options.isLoaded()) {
            setBounds(options.getX(), options.getY(), options.getWidth(),
                    options.getHeight());
        }
        System.err.println("8. finalizing...");
        applyComponentOrientation(GanttLanguage.getInstance()
                .getComponentOrientation());
        myTaskManager.addTaskListener(new TaskModelModificationListener(this));
        if (ourWindowListener != null) {
            addWindowListener(ourWindowListener);
        }
        addMouseListenerToAllContainer(this.getComponents());
        myDelayManager = new DelayManager(myTaskManager, tree);
        Mediator.registerDelayManager(myDelayManager);
        myDelayManager.addObserver(tree);

        getRootPane()
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        getRootPane().getActionMap().put("refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getActiveChart().reset();
                repaint();
            }
        });
    }

    private void addMouseListenerToAllContainer(Component[] cont) {
        for (int i = 0; i < cont.length; i++) {
            cont[i].addMouseListener(getStopEditingMouseListener());
            if (cont[i] instanceof Container)
                addMouseListenerToAllContainer(((Container) cont[i])
                        .getComponents());
        }
    }

    /**
     * @return A mouseListener that stop the edition in the ganttTreeTable.
     */
    private MouseListener getStopEditingMouseListener() {
        if (myStopEditingMouseListener == null)
            myStopEditingMouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getSource() != bNewTask && e.getClickCount() == 1)
                        tree.stopEditing();
                    if (e.getButton() == MouseEvent.BUTTON1
                            && !(e.getSource() instanceof JTable)
                            && !(e.getSource() instanceof AbstractButton)) {
                        Task taskUnderPointer = area.new MouseSupport()
                                .findTaskUnderMousePointer(e.getX(), e.getY());
                        if (taskUnderPointer == null) {
                            getTaskSelectionManager().clear();
                        }
                    }
                }
            };
        return myStopEditingMouseListener;
    }

    private void createProjectMenu() {
        mServer = new JMenu();
        mServer.setIcon(new ImageIcon(getClass().getResource(
                "/icons/server_16.gif")));
        myProjectMenu = new ProjectMenu(this);
        mProject.add(myProjectMenu.getProjectSettingsAction());
        mProject.add(myProjectMenu.getNewProjectAction());
        mProject.add(myProjectMenu.getOpenProjectAction());
        mProject.add(mMRU);
        updateMenuMRU();
        mProject.addSeparator();
        mProject.add(myProjectMenu.getSaveProjectAction());
        mProject.add(myProjectMenu.getSaveProjectAsAction());
        mProject.addSeparator();

        mProject.add(myProjectMenu.getImportFileAction());
        mProject.add(myProjectMenu.getExportFileAction());
        mProject.addSeparator();

        mServer.add(myProjectMenu.getOpenURLAction());
        mServer.add(myProjectMenu.getSaveURLAction());
        mProject.add(mServer);
        mProject.addSeparator();
        mProject.add(myProjectMenu.getPrintAction());
        miPreview = createNewItem("/icons/preview_16.gif");
        mProject.add(miPreview);
        mProject.addSeparator();
        mProject.add(myProjectMenu.getExitAction());
    }

    private JMenu createViewMenu() {
        JMenu result = changeMenuLabel(new JMenu(), language.getText("view"));
        result.add(miChartOptions);
        List<Chart> charts = Mediator.getPluginManager().getCharts();

        if (!charts.isEmpty()) {
            result.addSeparator();
        }
        for (Chart chart : charts) {
            result.add(new JCheckBoxMenuItem(new ToggleChartAction(chart, getViewManager())));
        }
        return result;
    }

    public GanttProject(boolean isOnlyViewer) {
        this(isOnlyViewer, false);
    }

    /**
     * Updates the last open file menu items.
     */
    private void updateMenuMRU() {
        mMRU.removeAll();
        int index = 0;
        Iterator<Document> iterator = documentsMRU.iterator();
        while (iterator.hasNext()) {
            index++;
            Document document = iterator.next();
            JMenuItem mi = new JMenuItem(new OpenDocumentAction(index,
                    document, this));
            mMRU.add(mi);
        }
    }

    public String getXslDir() {
        return options.getXslDir();
    }

    /** @return the options of ganttproject. */
    public GanttOptions getGanttOptions() {
        return options;
    }

    public void restoreOptions() {
        options.initDefault();
        myUIConfiguration = options.getUIConfiguration();
        GanttGraphicArea.taskDefaultColor = new Color(140, 182, 206);
        area.repaint();
    }

    // /** @return the status Bar of the main frame. */
    // public GanttStatusBar getStatusBar() {
    // return statusBar;
    // }

    public String getXslFo() {
        return options.getXslFo();
    }

    /** Create mnemonic for keyboard */
    public void setMnemonic() {
        int MENU_MASK = GPAction.MENU_MASK;

        miDeleteTask.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_MASK));
    }

    /** Create an item with a label */
    public JMenuItem createNewItemText(String label) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(this);
        return item;
    }

    /** Create an item with an icon */
    public JMenuItem createNewItem(String icon) {
        URL url = getClass().getResource(icon);
        JMenuItem item = url == null ? new JMenuItem() : new JMenuItem(
                new ImageIcon(url));
        item.addActionListener(this);
        return item;
    }

    /** Create an item with a label and an icon */
    public JMenuItem createNewItem(String label, String icon) {
        JMenuItem item = new JMenuItem(label, new ImageIcon(getClass()
                .getResource(icon)));
        item.addActionListener(this);
        return item;
    }

    /** Function to change language of the project */
    public void changeLanguage() {
        applyComponentOrientation(language.getComponentOrientation());
        changeLanguageOfMenu();
        area.repaint();
        getResourcePanel().area.repaint();
        getResourcePanel().refresh(language);

        this.tree.changeLanguage(language);
        CustomColumnsStorage.changeLanguage(language);

        applyComponentOrientation(language.getComponentOrientation());
    }

    /**
     * @deprecated. Use GanttLanguage.correctLabel
     */
    public static String correctLabel(String label) {
        return GanttLanguage.getInstance().correctLabel(label);
    }

    /**
     * Change the label for menu, in fact check in the label contains a mnemonic
     */
    public JMenu changeMenuLabel(JMenu menu, String label) {
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            menu.setText(label.substring(0, index).concat(
                    label.substring(++index)));
            menu.setMnemonic(Character.toLowerCase(label.charAt(index)));
        } else {
            menu.setText(label);
            // menu.setMnemonic('');
        }
        return menu;
    }

    /**
     * Change the label for menuItem, in fact check in the label contains a
     * mnemonic
     */
    public JMenuItem changeMenuLabel(JMenuItem menu, String label) {
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            menu.setText(label.substring(0, index).concat(
                    label.substring(++index)));
            menu.setMnemonic(Character.toLowerCase(label.charAt(index)));
        } else {
            menu.setText(label);
            // menu.setMnemonic('');
        }
        return menu;
    }

    /**
     * Change the label for JCheckBoxmenuItem, in fact check in the label
     * contains a mnemonic
     */
    public JCheckBoxMenuItem changeMenuLabel(JCheckBoxMenuItem menu,
            String label) {
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            menu.setText(label.substring(0, index).concat(
                    label.substring(++index)));
            menu.setMnemonic(Character.toLowerCase(label.charAt(index)));
        } else {
            menu.setText(label);
            // menu.setMnemonic('');
        }
        return menu;
    }

    /** Set the menus language after the user select a different language */
    private void changeLanguageOfMenu() {
        mProject = changeMenuLabel(mProject, language.getText("project"));
        mTask = changeMenuLabel(mTask, language.getText("task"));
        mHuman = changeMenuLabel(mHuman, language.getText("human"));
        mHelp = changeMenuLabel(mHelp, language.getText("help"));
        mCalendar = changeMenuLabel(mCalendar, language.getText("calendars"));
        mMRU = changeMenuLabel(mMRU, language.getText("lastOpen"));

        mServer = changeMenuLabel(mServer, language.getText("webServer"));
        miPreview = changeMenuLabel(miPreview, language.getText("preview"));
        // miNewTask = changeMenuLabel(miNewTask,
        // language.getText("createTask"));
        miDeleteTask = changeMenuLabel(miDeleteTask, language
                .getText("deleteTask"));
        mHuman.insert(changeMenuLabel(mHuman.getItem(0), language
                .getText("newHuman")), 0);
        miDelHuman = changeMenuLabel(miDelHuman, language
                .getText("deleteHuman"));
        mHuman.insert(changeMenuLabel(mHuman.getItem(4), language
                .getText("importResources")), 4);
        miSendMailHuman = changeMenuLabel(miSendMailHuman, language
                .getText("sendMail"));

        miPrjCal = changeMenuLabel(miPrjCal, language
                .getText("projectCalendar"));

        miWebPage = changeMenuLabel(miWebPage, language.getText("webPage"));
        miAbout = changeMenuLabel(miAbout, language.getText("about"));
        miChartOptions = changeMenuLabel(miChartOptions, language
                .getText("chartOptions"));

        bNewTask.setToolTipText(getToolTip(correctLabel(language
                .getText("createTask"))));
        // bCut.setToolTipText(getToolTip(correctLabel(language.getText("cut"))));
        // bCopy
        // .setToolTipText(getToolTip(correctLabel(language
        // .getText("copy"))));
        // bPaste
        // .setToolTipText(getToolTip(correctLabel(language
        // .getText("paste"))));
        bDelete.setToolTipText(getToolTip(correctLabel(language
                .getText("deleteTask"))));
        bProperties.setToolTipText(getToolTip(correctLabel(language
                .getText("propertiesTask"))));
        bUndo
                .setToolTipText(getToolTip(correctLabel(language
                        .getText("undo"))));
        bRedo
                .setToolTipText(getToolTip(correctLabel(language
                        .getText("redo"))));
        getTabs().setTitleAt(1, correctLabel(language.getText("human")));
    }

    /** Invoked when a key has been pressed. */
    public void keyPressed(KeyEvent e) {
        // Consume the event to prevent it to go farther.
        int code = e.getKeyCode();
        if (code == KeyEvent.KEY_LOCATION_UNKNOWN) {
            e.consume();
        }

        switch (code) {
        case KeyEvent.VK_DELETE:
            e.consume();
            if (!isOnlyViewer) {
                if (getViewIndex() == UIFacade.GANTT_INDEX)
                    deleteTasks(true);
                else if (getViewIndex() == UIFacade.RESOURCES_INDEX) {
                    deleteResources();
                }
            }
            break;
        case KeyEvent.VK_ENTER:
            break;
        case KeyEvent.VK_F5:
            e.consume();
            getActiveChart().reset();
            repaint();
            break;
        }
    }

    /** Invoked when a key has been released. */
    public void keyReleased(KeyEvent e) {
    }

    /** Invoked when a key has been typed. */
    public void keyTyped(KeyEvent e) {
    }

    /** Return the ToolTip in HTML (with gray bgcolor) */
    public static String getToolTip(String msg) {
        return "<html><body bgcolor=#EAEAEA>" + msg + "</body></html>";
    }


    /** Create the button on toolbar */
    private void addButtons(JToolBar toolBar) {
        bSave = new TestGanttRolloverButton(myProjectMenu.getSaveProjectAction());
        bCut = new TestGanttRolloverButton(getCutAction());
        bCopy = new TestGanttRolloverButton(getCopyAction());
        bPaste = new TestGanttRolloverButton(getPasteAction());

        myNewArtefactAction = new NewArtefactAction(
                new NewArtefactAction.ActiveActionProvider() {
                    public AbstractAction getActiveAction() {
                        return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? (AbstractAction) myNewTaskAction
                                : (AbstractAction) myNewHumanAction;

                    }
                }, options.getIconSize());
        bNewTask = new TestGanttRolloverButton(myNewArtefactAction);
        bDelete = new TestGanttRolloverButton(
                new ImageIcon(getClass().getResource(
                        "/icons/delete_" + options.getIconSize() + ".gif")));
        bDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) {// Gantt
                    deleteTasks(true);
                } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) { // Resource
                    // chart
                    final HumanResource[] context = getResourcePanel()
                            .getContext().getResources();
                    if (context.length > 0) {
                        Choice choice = getUIFacade().showConfirmationDialog(
                                getLanguage().getText("msg6")
                                        + getDisplayName(context) + "?",
                                getLanguage().getText("question"));
                        if (choice == Choice.YES) {
                            getUndoManager().undoableEdit("Delete Human OK",
                                    new Runnable() {
                                        public void run() {
                                            for (int i = 0; i < context.length; i++) {
                                                context[i].delete();
                                            }
                                        }
                                    });
                            repaint2();
                            refreshProjectInfos();
                        }
                    }
                }
            }
        });

        bProperties = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource(
                        "/icons/properties_" + options.getIconSize() + ".gif")));
        bProperties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) {// Gantt
                    // Chart
                    propertiesTask();
                } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) { // Resource
                    // chart
                    getResourcePanel().getResourcePropertiesAction()
                            .actionPerformed(null);
                }
            }
        });

        ScrollingManager scrollingManager = getScrollingManager();
        scrollingManager.addScrollingListener(area.getViewState());
        scrollingManager.addScrollingListener(getResourcePanel().area
                .getViewState());
        bUndo = new TestGanttRolloverButton(myEditMenu.getUndoAction());
        bRedo = new TestGanttRolloverButton(myEditMenu.getRedoAction());

        toolBar.add(bSave);
        toolBar.add(bUndo);
        toolBar.add(bRedo);
        toolBar.addSeparator();
        toolBar.add(bCut);
        toolBar.add(bCopy);
        toolBar.add(bPaste);
        toolBar.addSeparator();
        toolBar.add(bNewTask);
        toolBar.add(bDelete);
        toolBar.add(bProperties);
    }

    public List<GanttPreviousState> getBaselines() {
        return myPreviousStates;
    }

    private void aboutDialog() {
        AboutDialog agp = new AboutDialog(this);
        agp.setVisible(true);
    }

    private String getDisplayName(Object[] objs) {
        if (objs.length == 1) {
            return objs[0].toString();
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < objs.length; i++) {
            result.append(objs[i].toString());
            if (i < objs.length - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {
        quitApplication();
    }

    /** A menu has been activate */
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() instanceof JMenuItem) {
            String arg = evt.getActionCommand();
            if (arg.equals(correctLabel(language.getText("preview")))) {
                previewPrint();
            } else if (arg.equals(correctLabel(language.getText("deleteTask")))) {
                deleteTasks(true);
            } else if (arg.equals(correctLabel(language
                    .getText("projectCalendar")))) {
                System.out.println("Project calendar");
            } else if (arg.equals(correctLabel(language.getText("webPage")))) {
                try {
                    openWebPage();
                } catch (Exception e) {
                    System.err.println(e);
                }
            } else if (arg.equals(correctLabel(language.getText("about")))) {
                aboutDialog();
            } else if (arg.equals(correctLabel(language.getText("sendMail")))) {
                getTabs().setSelectedIndex(1);
                getResourcePanel().sendMail(this);
            }
        } else if (evt.getSource() instanceof Document) {
            if (getProjectUIFacade().ensureProjectSaved(getProject())) {
                openStartupDocument((Document) evt.getSource());
            }
        }
    }

    public HumanResource newHumanResource() {
        final HumanResource people = ((HumanResourceManager) getHumanResourceManager())
                .newHumanResource();
        people.setRole(getRoleManager().getDefaultRole());
        GanttDialogPerson dp = new GanttDialogPerson(getUIFacade(),
                getLanguage(), people);
        dp.setVisible(true);
        if (dp.result()) {

            getUndoManager().undoableEdit("new Resource", new Runnable() {
                public void run() {
                    getHumanResourceManager().add(people);
                }
            });
        }
        return people;
    }

    /** Create a new task */
    public Task newTask() {

        getTabs().setSelectedIndex(UIFacade.GANTT_INDEX);

        int index = -1;
        MutableTreeNode selectedNode = getTree().getSelectedNode();
        if (selectedNode != null) {
            DefaultMutableTreeNode parent1 = (DefaultMutableTreeNode) selectedNode
                    .getParent();
            index = parent1.getIndex(selectedNode) + 1;
            tree.getTreeTable().getTree().setSelectionPath(
                    new TreePath(parent1.getPath()));
            tree.getTreeTable().getTreeTable().editingStopped(
                    new ChangeEvent(tree.getTreeTable().getTreeTable()));
        }

        GanttCalendar cal = new GanttCalendar(area.getStartDate());

        DefaultMutableTreeNode node = tree.getSelectedNode();
        String nameOfTask = getTaskManager().getTaskNamePrefixOption().getValue();
        GanttTask task = getTaskManager().createTask();
        task.setStart(cal);
        task.setDuration(getTaskManager().createLength(1));
        getTaskManager().registerTask(task);
        task.setName(nameOfTask + "_" + task.getTaskID());
        task.setColor(area.getTaskColor());
        tree.addObject(task, node, index);

        /*
         * this will add new custom columns to the newly created task.
         */
        getCustomColumnsStorage().processNewTask(task);

        AdjustTaskBoundsAlgorithm alg = getTaskManager()
                .getAlgorithmCollection().getAdjustTaskBoundsAlgorithm();
        alg.run(task);
        RecalculateTaskCompletionPercentageAlgorithm alg2 = getTaskManager()
                .getAlgorithmCollection()
                .getRecalculateTaskCompletionPercentageAlgorithm();
        alg2.run(task);
        area.repaint();
        setAskForSave(true);
        getUIFacade().setStatusText(language.getText("createNewTask"));
        // setQuickSave(true);
        tree.setEditingTask(task);
        if (options.getAutomatic()) {
            propertiesTask();
        }
        repaint2();
        return task;
    }

    public void deleteResources() {
        myDeleteHumanAction.actionPerformed(null);
    }

    /**
     * Delete the current task
     *
     * @param confirmation
     *            Unused at the moment, but might be used to show confirmation
     *            when true
     */
    public void deleteTasks(boolean confirmation) {
        getTree().getDeleteTasksAction().actionPerformed(null);
    }

    /** Edit task parameters */
    public void propertiesTask() {
        myTaskPropertiesAction.actionPerformed(null);
    }

    /** Refresh the informations of the project on the status bar. */
    public void refreshProjectInfos() {
        if (getTaskManager().getTaskCount() == 0 && resp.nbPeople() == 0)
            getStatusBar().setSecondText("");
        else
            getStatusBar().setSecondText(
                    correctLabel(language.getText("task")) + " : "
                            + getTaskManager().getTaskCount() + "  "
                            + correctLabel(language.getText("resources"))
                            + " : " + resp.nbPeople());
    }

    /** Print the project */
    public void printProject() {
        Chart chart = getUIFacade().getActiveChart();

        if (chart == null) {
            getUIFacade()
                    .showErrorDialog(
                            "Failed to find active chart.\nPlease report this problem to GanttProject development team");
            return;
        }
        try {
            PrintManager.printChart(chart, options.getExportSettings());
        } catch (OutOfMemoryError e) {
            getUIFacade().showErrorDialog(
                    GanttLanguage.getInstance().getText(
                            "printing.out_of_memory"));
        }
    }

    public void previewPrint() {

        Date startDate, endDate;
        Chart chart = getUIFacade().getActiveChart();

        if (chart == null) {
            getUIFacade()
                    .showErrorDialog(
                            "Failed to find active chart.\nPlease report this problem to GanttProject development team");
            return;
        }

        try {
            startDate = chart.getStartDate();
            endDate = chart.getEndDate();
        } catch (UnsupportedOperationException e) {
            startDate = null;
            endDate = null;
        }

        if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) {
            startDate = area.getChartModel().getStartDate();
            endDate = area.getChartModel().getEndDate();
        } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) {
            startDate = getResourcePanel().area.getChartModel().getStartDate();
            endDate = getResourcePanel().area.getChartModel().getEndDate();
        }
        try {
            PrintPreview preview = new PrintPreview(getProject(),
                    getUIFacade(), chart, startDate, endDate);
            preview.setVisible(true);
        } catch (OutOfMemoryError e) {
            getUIFacade().showErrorDialog(
                    GanttLanguage.getInstance().getText(
                            "printing.out_of_memory"));
            return;
        }
    }

    /** Create a new project */
    public void newProject() {
        getProjectUIFacade().createProject(getProject());
    }

    /** Open a local project file with dialog box (JFileChooser) */
    public void openFile() throws IOException {
        getProjectUIFacade().openProject(this);
    }

    /** Open a remote project file with dialog box (GanttURLChooser) */
    public void openURL() {
        try {
            getProjectUIFacade().openRemoteProject(getProject());
        } catch (IOException e) {
            getUIFacade().showErrorDialog(e);
        }
    }

    public void open(Document document) throws IOException {
        openDocument(document);
        if (document.getPortfolio() != null) {
            Document defaultDocument = document.getPortfolio()
                    .getDefaultDocument();
            openDocument(defaultDocument);
        }
    }

    private void openDocument(Document document) throws IOException {
        if (document.getDescription().toLowerCase().endsWith(".xml") == false
                && document.getDescription().toLowerCase().endsWith(".gan") == false) {
            // Unknown file extension
            String errorMessage = language.getText("msg2") + "\n"
                    + document.getDescription();
            throw new IOException(errorMessage);
        }

        boolean locked = document.acquireLock();
        if (!locked) {
            getUIFacade().logErrorMessage(
                    new Exception(language.getText("msg13")));
        }
        document.read();
        if (documentsMRU.add(document)) {
            updateMenuMRU();
        }
        if (locked) {
            projectDocument = document;
        }
        setTitle(language.getText("appliTitle") + " ["
                + document.getDescription() + "]");
        for (Chart chart : Mediator.getPluginManager().getCharts()) {
            chart.setTaskManager(myTaskManager);
            chart.reset();
        }

        // myDelayManager.fireDelayObservation(); // it is done in repaint2
        addMouseListenerToAllContainer(this.getComponents());
        getTaskManager().projectOpened();
        
        // As we just have opened a new file it is still unmodified, so mark it as such
        setModified(false);
    }

    public void openStartupDocument(String path) {
        if (path != null) {
            final Document document = getDocumentManager().getDocument(path);
            // openStartupDocument(document);
            getUndoManager().undoableEdit("OpenFile", new Runnable() {
                public void run() {
                    try {
                        getProjectUIFacade()
                                .openProject(document, getProject());
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
            if (Pattern.matches(".*(" + importer.getFileNamePattern()
                    + ")$", document.getFilePath())) {
                try {
                    importer.setContext(getProject(), getUIFacade(),
                            getGanttOptions().getPluginPreferences());
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

    private void openStartupDocument(Document document) {
        try {
            getProjectUIFacade().openProject(document, getProject());
        } catch (IOException e) {
            getUIFacade().showErrorDialog(e);
        }
    }

    /** Save the project as (with a dialog file chooser) */
    public boolean saveAsProject() throws IOException {
        getProjectUIFacade().saveProjectAs(getProject());
        return true;
    }

    /** Save the project on a server (with a GanttURLChooser) */
    public boolean saveAsURLProject() throws IOException {
        getProjectUIFacade().saveProjectRemotely(getProject());
        return true;
    }

    /** Save the project on a file */
    public void saveProject() throws IOException {
        getProjectUIFacade().saveProject(getProject());
    }

    public void changeWorkingDirectory(String newWorkDir) {
        if (null != newWorkDir)
            options.setWorkingDirectory(newWorkDir);
    }

    /** @return the UIConfiguration. */
    public UIConfiguration getUIConfiguration() {
        return myUIConfiguration;
    }

    /** Quit the application */
    public void quitApplication() {
        options.setWindowPosition(getX(), getY());
        options.setWindowSize(getWidth(), getHeight());
        options.setUIConfiguration(myUIConfiguration);
        options.setDocumentsMRU(documentsMRU);
        options.setToolBarPosition(toolBar.getOrientation());
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

    /** Open the web page */
    public void openWebPage() throws IOException {
        if (!BrowserControl.displayURL("http://ganttproject.biz/")) {
            GanttDialogInfo gdi = new GanttDialogInfo(this,
                    GanttDialogInfo.ERROR, GanttDialogInfo.YES_OPTION, language
                            .getText("msg4"), language.getText("error"));
            gdi.setVisible(true);
            return;
        }
        getUIFacade().setStatusText(
                GanttLanguage.getInstance().getText("opening")
                        + " www.ganttproject.biz");
    }

    public void setAskForSave(boolean afs) {
        if (isOnlyViewer)
            return;
        fireProjectModified(afs);
        String title = getTitle();
        // String last = title.substring(title.length() - 11, title.length());
        askForSave = afs;
        try {
            if (System.getProperty("mrj.version") != null) {
                rootPane.putClientProperty("windowModified", Boolean
                        .valueOf(afs));
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

    /** Print the help for ganttproject on the system.out */
    private static void usage() {
        System.out.println();
        System.out
                .println("GanttProject usage : java -jar ganttproject-(VERSION).jar <OPTIONS>");
        System.out.println();
        System.out.println("  Here are the possible options:");
        System.out.println("    -h, --help : Print this message");
        System.out
                .println("    [project_file_name] a XML file based on ganttproject format to directly open (project.xml or project.gan)");
        System.out
                .println("    -html [project_file_name] [export_directory_name], export directly a ganttproject file to web pages");
        System.out
                .println("         -xsl-dir [xsl_directory]                        localisation of the xsl directory for html export");
        System.out
                .println("    -pdf  [project_file_name] [pdf_file_name],         export directly a ganttproject file to a PDF file");
        System.out
                .println("         -xsl-fo [xsl_fo_file]                           localisation of the xsl-fo file for pdf export");
        System.out
                .println("    -csv  [project_file_name] [csv_image_filename],    export directly a ganttproject file to csv document compatible with spreadsheets");
        System.out
                .println("    -png  [project_file_name] [png_image_filename],    export directly a ganttproject file to png image");
        System.out
                .println("    -jpg  [project_file_name] [jpg_image_filename],    export directly a ganttproject file to jpg image");
        System.out
                .println("    -fig/-xfig  [project_file_name] [fig_image_filename],    export directly a ganttproject file to xfig image");
        System.out.println();
        System.out
                .println("    In all these cases the project_file_name can either be a file on local disk or an URL.");
        System.out
                .println("    If the URL is password-protected, you can give credentials this way:");
        System.out
                .println("      http://username:password@example.com/filename");
        System.out.println(" ");
    }

    public GanttResourcePanel getResourcePanel() {
        if (this.resp == null) {
            this.resp = new GanttResourcePanel(this, getTree(), getUIFacade());
            this.resp.setResourceActions(getResourceActions()); // TODO pass
            getHumanResourceManager().addView(this.resp);
        }
        return this.resp;
    }

    public GanttLanguage getLanguage() {
        return this.language;
    }

    public GanttGraphicArea getArea() {
        return this.area;
    }

    public GanttTree2 getTree() {
        if (this.tree == null) {
            this.tree = new GanttTree2(this, getTaskManager(), getTaskSelectionManager(), getUIFacade());
        }
        return this.tree;
    }

    public Action getCopyAction() {
        return getViewManager().getCopyAction();
    }

    public Action getCutAction() {
        return getViewManager().getCutAction();
    }

    public Action getPasteAction() {
        return getViewManager().getPasteAction();
    }

    private ResourceActionSet getResourceActions() {
        if (myResourceActions == null) {
            myResourceActions = new ResourceActionSet((IGanttProject) this,
                    (ResourceContext) getResourcePanel(), this, getUIFacade());
        }
        return myResourceActions;
    }

    /** The main */
    public static void main(String[] arg) {
        URL logConfig = GanttProject.class.getResource("/logging.properties");
        if (logConfig != null) {
            try {
                GPLogger.readConfiguration(logConfig);
            } catch (IOException e) {
                System.err
                        .println("Failed to setup logging: " + e.getMessage());
                e.printStackTrace();
            }
        }
        CommandLineExportApplication cmdlineApplication = new CommandLineExportApplication();
        HashMap<String, List<String>> parsedArgs = new HashMap<String, List<String>>();
        final List<String> emptyList = Collections.<String>emptyList();
        String argName = "";
        for (int i = 0; i < arg.length; i++) {
            String nextWord = arg[i];
            if (nextWord.charAt(0) == '-') {
                if (argName.length() != 0) {
                    parsedArgs.put(argName, emptyList);
                }
                argName = nextWord.toLowerCase();
            } else {
                List<String> values = parsedArgs.get(argName);
                if (values == null || values == emptyList) {
                    values = new ArrayList<String>();
                    parsedArgs.put(argName, values);
                }
                values.add(nextWord);
                if (!cmdlineApplication.getCommandLineFlags().contains(argName)) {
                    argName = "";
                }
            }
        }
        if (argName.length() > 0 && !parsedArgs.containsKey(argName)) {
            parsedArgs.put(argName, emptyList);
        }
        if (parsedArgs.containsKey("-h") || parsedArgs.containsKey("--help")) {
            usage();
            System.exit(0);
        }
        if (parsedArgs.containsKey("-log")) {
            try {
                List<String> values = parsedArgs.get("-log");
                String logFileName = values.isEmpty() ? System
                        .getProperty("user.home")
                        + "/.ganttproject.log" : values.get(0);
                GPLogger.setLogFile(logFileName);
                File logFile = new File(logFileName);
                System.setErr(new PrintStream(new FileOutputStream(logFile)));
                System.out.println("Writing log to "
                        + logFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to write log to file: "
                        + e.getMessage());
                e.printStackTrace();
            }
        }
        if (false == cmdlineApplication.export(parsedArgs)) {
            GanttSplash splash = new GanttSplash();
            try {
                splash.setVisible(true);
                GanttProject ganttFrame = new GanttProject(false);
                System.err.println("Main frame created");
                String startupDocument = null;
                if (parsedArgs.containsKey("")) {
                    List<String> values = parsedArgs.get("");
                    startupDocument = values.get(0);
                } else if (parsedArgs.containsKey("-open")) {
                    List<String> values = parsedArgs.get("-open");
                    startupDocument = values.isEmpty() ? null : values.get(0);
                }
                if (startupDocument != null) {
                    ganttFrame.openStartupDocument(startupDocument);
                }
                ganttFrame.setVisible(true);
                if (System.getProperty("os.name").toLowerCase().startsWith(
                        "mac os x")) {
                    OSXAdapter.registerMacOSXApplication(ganttFrame);
                }
                ganttFrame.getActiveChart().reset();
            } catch (Throwable e) {
                e.printStackTrace();
                return;
            } finally {
                splash.close();
                System.err.println("Splash closed");
            }
        }
    }

    public static final String HUMAN_RESOURCE_MANAGER_ID = "HUMAN_RESOURCE";

    public static final String ROLE_MANAGER_ID = "ROLE_MANAGER";

    private GPCalendar myFakeCalendar = new WeekendCalendarImpl();

    // private GPCalendar myFakeCalendar = new AlwaysWorkingTimeCalendarImpl();

    private ParserFactory myParserFactory;

    private static WindowListener ourWindowListener;

    /////////////////////////////////////////////////////////
    // IGanttProject implementation
    public String getProjectName() {
        return prjInfos.getName();
    }

    public void setProjectName(String projectName) {
        prjInfos.setName(projectName);
        setAskForSave(true);
    }

    public String getDescription() {
        return prjInfos.getDescription();
    }

    public void setDescription(String description) {
        prjInfos.setDescription(description);
        setAskForSave(true);
    }

    public String getOrganization() {
        return prjInfos.getOrganization();
    }

    public void setOrganization(String organization) {
        prjInfos.setOrganization(organization);
        setAskForSave(true);
    }

    public String getWebLink() {
        return prjInfos.getWebLink();
    }

    public void setWebLink(String webLink) {
        prjInfos.setWebLink(webLink);
        setAskForSave(true);
    }

    public HumanResourceManager getHumanResourceManager() {
        HumanResourceManager result = (HumanResourceManager) managerHash
                .get(HUMAN_RESOURCE_MANAGER_ID);
        if (result == null) {
            result = new HumanResourceManager(getRoleManager().getDefaultRole());
            // result.addView(getPeople());
            managerHash.put(HUMAN_RESOURCE_MANAGER_ID, result);
            result.addView(this);
        }
        return result;
    }

    public TaskManager getTaskManager() {
        return myTaskManager;
    }

    public RoleManager getRoleManager() {
        RoleManager result = (RoleManager) managerHash.get(ROLE_MANAGER_ID);
        if (result == null) {
            result = RoleManager.Access.getInstance();
            managerHash.put(ROLE_MANAGER_ID, result);
        }
        return result;
    }

    public Document getDocument() {
        return projectDocument;
    }

    public void setDocument(Document document) {
        projectDocument = document;
    }

    public GanttLanguage getI18n() {
        return getLanguage();
    }

    public GPCalendar getActiveCalendar() {
        return myFakeCalendar;
    }

    public void setModified() {
        setAskForSave(true);
    }

    public void setModified(boolean modified) {
        setAskForSave(modified);

        String title = getTitle();
        if(modified == false && title.endsWith(" *")) {
            // Remove * from title
            setTitle(title.substring(0, title.length() - 2));
        }
    }

    public boolean isModified() {
        return askForSave;
    }

    public void close() {
        fireProjectClosed();
        prjInfos = new PrjInfos();
        RoleManager.Access.getInstance().clear();
        if (null != projectDocument) {
            projectDocument.releaseLock();
        }
        projectDocument = null;
        getTaskManager().projectClosed();
        getCustomColumnsStorage().reset();

        for (int i = 0; i < myPreviousStates.size(); i++) {
            myPreviousStates.get(i).remove();
        }
        myPreviousStates = new ArrayList<GanttPreviousState>();
        setModified(false);
    }

    protected ParserFactory getParserFactory() {
        if (myParserFactory == null) {
            myParserFactory = new ParserFactoryImpl();
        }
        return myParserFactory;
    }

    /////////////////////////////////////////////////////////////////
    // ResourceView implementation
    public void resourceAdded(ResourceEvent event) {
        if (getStatusBar() != null) {
            // tabpane.setSelectedIndex(1);
            getUIFacade().setStatusText(
                    GanttLanguage.getInstance().correctLabel(
                            GanttLanguage.getInstance().getText("newHuman")));
            setAskForSave(true);
            refreshProjectInfos();
        }
    }

    public void resourcesRemoved(ResourceEvent event) {
        refreshProjectInfos();
        setAskForSave(true);
    }

    public void resourceChanged(ResourceEvent e) {
        setAskForSave(true);
    }

    public void resourceAssignmentsChanged(ResourceEvent e) {
        setAskForSave(true);
    }

    /////////////////////////////////////////////////////////////////
    // UIFacade

    public GanttChart getGanttChart() {
        return (GanttChart) getArea();
    }

    public Chart getResourceChart() {
        return (Chart) getResourcePanel().area;
    }

    public int getGanttDividerLocation() {
        // return mySplitPane.getDividerLocation();
        return myGanttChartTabContent.getDividerLocation();
    }

    public void setGanttDividerLocation(int location) {
        myGanttChartTabContent.setDividerLocation(location);
    }

    public int getResourceDividerLocation() {
        return myResourceChartTabContent.getDividerLocation();
        // return getResourcePanel().getDividerLocation();
    }

    public void setResourceDividerLocation(int location) {
        myResourceChartTabContent.setDividerLocation(location);
    }

    public TaskTreeUIFacade getTaskTree() {
        return getTree();
    }

    public ResourceTreeUIFacade getResourceTree() {
        return getResourcePanel();
    }

    private class ParserFactoryImpl implements ParserFactory {
        public GPParser newParser() {
            return new GanttXMLOpen(prjInfos, getUIConfiguration(),
                    getTaskManager(), getUIFacade());
        }

        public GPSaver newSaver() {
            return new GanttXMLSaver(GanttProject.this, (GanttTree2) getTree(),
                    getResourcePanel(), getArea(), getUIFacade());
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

    public void recalculateCriticalPath() {
        if (myUIConfiguration.isCriticalPathOn()) {
            getTaskManager().processCriticalPath((TaskNode) tree.getRoot());
            ArrayList<DefaultMutableTreeNode> projectTasks = tree
                    .getProjectTasks();
            if (projectTasks.size() != 0) {
                for (int i = 0; i < projectTasks.size(); i++) {
                    getTaskManager().processCriticalPath(
                            (TaskNode) projectTasks.get(i));
                }
            }
            repaint();
        }
    }

    public int getViewIndex() {
        if (getTabs() == null)
            return -1;
        return getTabs().getSelectedIndex();
    }

    public void setViewIndex(int viewIndex) {
        if (getTabs().getTabCount() > viewIndex) {
            getTabs().setSelectedIndex(viewIndex);
        }
    }

    public static void setWindowListener(WindowListener windowListener) {
        ourWindowListener = windowListener;
    }

    public void refresh() {
        getTaskManager().processCriticalPath(getTaskManager().getRootTask());
        getResourcePanel().getResourceTreeTableModel().updateResources();
        getResourcePanel().getResourceTreeTable().setRowHeight(20);
        if (myDelayManager != null)
            myDelayManager.fireDelayObservation();
        super.repaint();
    }

    public void setHiddens() {
    }

    public CustomPropertyManager getResourceCustomPropertyManager() {
        return getResourcePanel().getResourceTreeTable();
    }
}