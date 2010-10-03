package net.sourceforge.ganttproject.gui;

/**
 *
 * <p>Title: </p>
 *
 * <p>Description: Provide the properties of selected task</p>
 *
 * <p>Copyright: Copyright (c) 2003</p>
 *
 * <p>Company: </p>
 *
 * @author ganttproject
 *
 * @version 1.0
 *
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttGraphicArea;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.taskproperties.CustomColumnsPanel;
import net.sourceforge.ganttproject.gui.taskproperties.TaskAllocationsPanel;
import net.sourceforge.ganttproject.gui.taskproperties.TaskDependenciesPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.shape.JPaintCombo;
import net.sourceforge.ganttproject.shape.ShapeConstants;
import net.sourceforge.ganttproject.shape.ShapePaint;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskImpl;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.util.BrowserControl;

import org.jdesktop.swing.JXDatePicker;

/**
 * Real panel for editing task properties
 */
public class GanttTaskPropertiesBean extends JPanel {

    private static final JColorChooser colorChooser = new JColorChooser();

    private JXDatePicker myStartDatePicker;
    private JXDatePicker myEndDatePicker;
    private JXDatePicker myThirdDatePicker;
    // Input attributes

    protected GanttTask[] selectedTasks;

    //private GanttTree2 tree; // GanttTree that contain all the tasks information

    private final GanttLanguage language = GanttLanguage.getInstance(); // language

    // Output attributes: you can find the definition is GanttTask

    private GanttCalendar start;

    private GanttCalendar end;

    private GanttCalendar third;

    // private GanttTask selectedTaskClone;

    // private Hashtable managerHash;

    // private Hashtable assignedResources = new Hashtable();

    // private attributes for internal use

    private final GridBagConstraints gbc = new GridBagConstraints();

    private JTabbedPane tabbedPane; // TabbedPane that includes the following four items

    private JPanel generalPanel;

    private JComponent predecessorsPanel;

    private JPanel resourcesPanel;

    private JPanel notesPanel;

    // Components on general panel

    private JTextField nameField1;

    private JTextField durationField1;

    private JTextField tfWebLink;

    private JButton bWebLink;

    private JSpinner percentCompleteSlider;

    private JComboBox priorityComboBox;

    private JComboBox thirdDateComboBox;

    private JCheckBox mileStoneCheckBox1;

    private JCheckBox projectTaskCheckBox1;

    private boolean isColorChanged;

    private JButton colorButton;

    private JButton defaultColorButton;

    /** Shape chooser combo Box */
    private JPaintCombo shapeComboBox;

    // Components on predecessors panel

    private JTextField nameFieldNotes;

    private JTextField durationFieldNotes;

    private JScrollPane scrollPaneNotes;

    private JTextArea noteAreaNotes;

    private JPanel firstRowPanelNotes;

    private JPanel secondRowPanelNotes;

    private boolean onlyOneTask = false;

    // Original values of the selected Task

    private String taskWebLink;

    private boolean taskIsMilestone;

    private GanttCalendar taskStartDate;

    private GanttCalendar taskThirdDate;

    private int taskThirdDateConstraint;

    private boolean taskIsProjectTask;

    private int taskLength;

    private String taskNotes;

    private int taskCompletionPercentage;

    private Task.Priority taskPriority;

    private ShapePaint taskShape;

    private CustomColumnsPanel myCustomColumnPanel = null;

    // private ResourcesTableModel myResourcesTableModel;
    private TaskDependenciesPanel myDependenciesPanel;

    private TaskAllocationsPanel[] myAllocationsPanel;

    //private boolean isStartFixed;

//    private boolean isFinishFixed;

    private final HumanResourceManager myHumanResourceManager;

    private final RoleManager myRoleManager;

    private Task myUnpluggedClone;
    private final TaskManager myTaskManager;
    private final IGanttProject myProject;
    private final UIFacade myUIfacade;

    private TaskMutator mutator;

    /** add a component to container by using GridBagConstraints. */
    private void addUsingGBL(Container container, Component component,
            GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.weighty = 0;
        container.add(component, gbc);
    }

    /** set the first row in all the tabbed pane. thus give them a common look */
    private void setFirstRow(Container container, GridBagConstraints gbc,
            JLabel nameLabel, JTextField nameField, JLabel durationLabel,
            JTextField durationField) {
        container.setLayout(new GridBagLayout());
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 15;
        gbc.insets.left = 10;
        gbc.insets.top = 10;
        addUsingGBL(container, nameLabel, gbc, 0, 0, 1, 1);
        addUsingGBL(container, nameField, gbc, 1, 0, 1, 1);
        addUsingGBL(container, durationLabel, gbc, 2, 0, 1, 1);
        gbc.weightx = 1;
        addUsingGBL(container, durationField, gbc, 3, 0, 1, 1);
    }

    /** Construct the general panel */
    private void constructGeneralPanel() {
        // Define some labels local used labels
        JLabel percentCompleteLabel1;
        JLabel priorityLabel1;
        JLabel startDateLabel1;
        JLabel finishDateLabel1;
        JLabel nameLabel1;
        JLabel durationLabel1;
        JLabel lblWebLink;

        // Define locally used panels to specify the rows
        JPanel firstRowPanel1;
        JPanel secondRowPanel1;
        JPanel thirdRowPanel1;
        JPanel fourthRowPanel1;

        JPanel colorPanel;
        JPanel webLinkPanel;

        FlowLayout flowL = new FlowLayout(FlowLayout.LEFT, 10, 10);

        generalPanel = new JPanel(new GridBagLayout());

        // first row

        nameLabel1 = new JLabel(language.getText("name") + ":");
        nameField1 = new JTextField(20);
        nameField1.setName("name_of_task");

        if (!onlyOneTask) {
            nameLabel1.setVisible(false);
            nameField1.setVisible(false);
        }

        durationLabel1 = new JLabel(language.getText("length") + ":");
        durationField1 = new JTextField(8);
        durationField1.setName("length");
        durationField1.addFocusListener(new FocusListener() {
                       public void focusLost(FocusEvent e) {
                               fireDurationChanged();
                       }
                       public void focusGained(FocusEvent e) {}
               });
        firstRowPanel1 = new JPanel(flowL);
        setFirstRow(firstRowPanel1, gbc, nameLabel1, nameField1,
                durationLabel1, durationField1);

        // second row

        percentCompleteLabel1 = new JLabel(language.getText("advancement")); // Progress
        percentCompleteLabel1.setText(language.getText("advancement"));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
        percentCompleteSlider = new JSpinner(spinnerModel);

        secondRowPanel1 = new JPanel(flowL);
        secondRowPanel1.add(percentCompleteLabel1);
        // secondRowPanel1.add(percentCompleteField1);
        secondRowPanel1.add(percentCompleteSlider);
        priorityLabel1 = new JLabel(language.getText("priority"));
        secondRowPanel1.add(priorityLabel1);
        priorityComboBox = new JComboBox();
        for (Task.Priority p: Task.Priority.values()) {
            priorityComboBox.addItem(language.getText(p.getI18nKey()));
        }
        priorityComboBox.setEditable(false);

        secondRowPanel1.add(priorityComboBox);

        // third row

        startDateLabel1 = new JLabel(language.getText("dateOfBegining") + ":");
        finishDateLabel1 = new JLabel(language.getText("dateOfEnd") + ":");

        thirdDateComboBox = new JComboBox();
        thirdDateComboBox.addItem("");
        thirdDateComboBox.addItem(language.getText("earliestBegin"));
        thirdDateComboBox.setName("third");
        thirdDateComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switch (thirdDateComboBox.getSelectedIndex()) {
                case TaskImpl.EARLIESTBEGIN:
                    myThirdDatePicker.setEnabled(true);
                    break;
                case TaskImpl.NONE:
                    myThirdDatePicker.setEnabled(false);
                    break;
                }
            }
        });

        thirdRowPanel1 = new JPanel(flowL);
        thirdRowPanel1.setBorder(new TitledBorder(new EtchedBorder(), language
                .getText("date")));

        JPanel startDatePanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 0));
        startDatePanel.add(startDateLabel1);
        myStartDatePicker = createDatePicker(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStart(new GanttCalendar(((JXDatePicker)e.getSource()).getDate()), false);
            }
        });
        startDatePanel.add(myStartDatePicker);


        JPanel finishDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,
                0));
        finishDatePanel.add(finishDateLabel1);
        myEndDatePicker = createDatePicker(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setEnd(new GanttCalendar(((JXDatePicker)e.getSource()).getDate()).newAdd(1), false);
            }
        });
        finishDatePanel.add(myEndDatePicker);

        JPanel thirdDatePanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 0));
        thirdDatePanel.add(thirdDateComboBox);
        myThirdDatePicker = createDatePicker(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setThird(new GanttCalendar(((JXDatePicker)e.getSource()).getDate()), false);
            }
        });
        thirdDatePanel.add(myThirdDatePicker);

        addUsingGBL(thirdRowPanel1, startDatePanel, gbc, 0, 0, 1, 1);
        addUsingGBL(thirdRowPanel1, finishDatePanel, gbc, 0, 1, 1, 1);
        addUsingGBL(thirdRowPanel1, thirdDatePanel, gbc, 0, 2, 1, 1);

        // fourth row

        JCheckBox checkBox = constructCheckBox ();
        fourthRowPanel1 = new JPanel(flowL);
        if (checkBox != null) {
            fourthRowPanel1.add(checkBox);
        }

        JPanel shapePanel = new JPanel();
        shapePanel.setLayout(new BorderLayout());
        JLabel lshape = new JLabel("  " + language.getText("shape") + " ");
        shapeComboBox = new JPaintCombo(ShapeConstants.PATTERN_LIST);

        shapePanel.add(lshape, BorderLayout.WEST);
        shapePanel.add(shapeComboBox, BorderLayout.CENTER);

        colorButton = new JButton(language.getText("colorButton"));
        colorButton.setBackground(selectedTasks[0].getColor());
        final String colorChooserTitle = language.getText("selectColor");
        colorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JDialog dialog;
                dialog = JColorChooser.createDialog(GanttTaskPropertiesBean.this, colorChooserTitle,
                        true, colorChooser,
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                colorButton.setBackground(colorChooser.getColor());
                                isColorChanged = true;
                            }
                        }

                        , new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                // nothing to do for "Cancel"
                            }
                        });

                /*
                 * AbstractColorChooserPanel[] panels =
                 * GanttDialogProperties.colorChooser.getChooserPanels();
                 * GanttDialogProperties.colorChooser.removeChooserPanel(panels[0]);
                 * GanttDialogProperties.colorChooser.addChooserPanel(panels[0]);
                 */

                colorChooser.setColor(colorButton.getBackground());
                dialog.setVisible(true);
            }
        });

        defaultColorButton = new JButton(language.getText("defaultColor"));
        defaultColorButton.setBackground(GanttGraphicArea.taskDefaultColor);
        defaultColorButton.setToolTipText(GanttProject.getToolTip(language
                .getText("resetColor")));
        defaultColorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorButton.setBackground(GanttGraphicArea.taskDefaultColor);
                isColorChanged = true;
            }
        });

        colorPanel = new JPanel();
        colorPanel.setLayout(new BorderLayout());
        colorPanel.add(colorButton, "West");
        colorPanel.add(defaultColorButton, "Center");
        colorPanel.add(shapePanel, BorderLayout.EAST);
        fourthRowPanel1.add(colorPanel);

        // ---Set GridBagConstraints constant
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 15;
        gbc.insets.left = 10;
        gbc.insets.top = 10;
        addUsingGBL(generalPanel, firstRowPanel1, gbc, 0, 0, 1, 1);
        addUsingGBL(generalPanel, secondRowPanel1, gbc, 0, 1, 1, 1);
        addUsingGBL(generalPanel, thirdRowPanel1, gbc, 0, 2, 1, 1);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridheight = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 1;
        generalPanel.add(fourthRowPanel1, gbc);

        // The panel for the web link
        webLinkPanel = new JPanel(flowL);
        lblWebLink = new JLabel(language.getText("webLink"));
        webLinkPanel.add(lblWebLink);
        tfWebLink = new JTextField(30);
        webLinkPanel.add(tfWebLink);
        bWebLink = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/web_16.gif")));
        bWebLink.setToolTipText(GanttProject.getToolTip(language
                .getText("openWebLink")));
        webLinkPanel.add(bWebLink);

        bWebLink.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // link to open the web link
                try {
                    if (!BrowserControl.displayURL(tfWebLink.getText())) {
                        GanttDialogInfo gdi = new GanttDialogInfo(null,
                                GanttDialogInfo.ERROR,
                                GanttDialogInfo.YES_OPTION, language
                                        .getText("msg4"), language
                                        .getText("error"));
                        gdi.setVisible(true);
                    }
                } catch (Exception ex) {
                }
            }
        });

        gbc.gridy = 4;
        generalPanel.add(webLinkPanel, gbc);

    }

    /** Add the different action listeners on the different widgets */
    public void addActionListener(ActionListener l) {

        nameField1.addActionListener(l);

        thirdDateComboBox.addActionListener(l);

        durationField1.addActionListener(l);

    }

    /** Change the name of the task on all text fields containing task name */
    public void changeNameOfTask() {
        if (nameField1 != null && nameFieldNotes != null) {
            String nameOfTask = nameField1.getText().trim();
            nameField1.setText(nameOfTask);
//            if (onlyOneTask) {
//                myDependenciesPanel.nameChanged(nameOfTask);
//            }
            myAllocationsPanel[0].nameChanged(nameOfTask);
            nameFieldNotes.setText(nameOfTask);
        }
    }

    private void constructCustomColumnPanel(IGanttProject project) {
        myCustomColumnPanel = new CustomColumnsPanel(
                project.getTaskCustomColumnManager(), myUIfacade);
    }

    /** Construct the predecessors tabbed pane */
    private void constructPredecessorsPanel() {
        myDependenciesPanel = new TaskDependenciesPanel();
        myDependenciesPanel.init(selectedTasks[0]);
        predecessorsPanel = myDependenciesPanel.getComponent();
    }

    /** Construct the resources panel */
    private void constructResourcesPanel() {
        myAllocationsPanel = new TaskAllocationsPanel[selectedTasks.length];
        for (int i = 0; i < myAllocationsPanel.length; i++) {
            myAllocationsPanel[i] = new TaskAllocationsPanel(selectedTasks[i],
                    myHumanResourceManager, myRoleManager, onlyOneTask);
            if (i != 0) {
                myAllocationsPanel[i].getComponent();
            }
        }
        resourcesPanel = myAllocationsPanel[0].getComponent();
    }

    /** construct the notes panel */
    private void constructNotesPanel() {
        JLabel nameLabelNotes;
        JLabel durationLabelNotes;

        notesPanel = new JPanel(new GridBagLayout());

        // first row

        nameLabelNotes = new JLabel(language.getText("name") + ":");

        nameFieldNotes = new JTextField(20);

        if (!onlyOneTask) {
            nameLabelNotes.setVisible(false);
            nameFieldNotes.setVisible(false);
        }

        durationLabelNotes = new JLabel(language.getText("length") + ":");

        durationFieldNotes = new JTextField(8);

        nameFieldNotes.setEditable(false);

        durationFieldNotes.setEditable(false);

        firstRowPanelNotes = new JPanel();

        setFirstRow(firstRowPanelNotes, gbc, nameLabelNotes, nameFieldNotes,

        durationLabelNotes, durationFieldNotes);

        secondRowPanelNotes = new JPanel();

        secondRowPanelNotes.setBorder(new TitledBorder(new EtchedBorder(),
                language.getText("notesTask") + ":"));

        noteAreaNotes = new JTextArea(8, 40);
        noteAreaNotes.setLineWrap(true);
        noteAreaNotes.setWrapStyleWord(true);
        noteAreaNotes.setBackground(new Color(1.0f, 1.0f, 1.0f));

        scrollPaneNotes = new JScrollPane(noteAreaNotes);

        secondRowPanelNotes.add(scrollPaneNotes);

        JButton bdate = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/clock_16.gif")));
        bdate.setToolTipText(GanttProject.getToolTip(language
                .getText("putDate")));
        bdate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                noteAreaNotes.append("\n" + GanttCalendar.getDateAndTime()
                        + "\n");
            }
        });
        secondRowPanelNotes.add(bdate);

        gbc.anchor = GridBagConstraints.WEST;

        gbc.insets.right = 15;

        gbc.insets.left = 10;

        gbc.insets.top = 10;

        gbc.weighty = 0;

        addUsingGBL(notesPanel, firstRowPanelNotes, gbc, 0, 0, 1, 1);

        gbc.weighty = 1;

        gbc.gridx = 0;

        gbc.gridy = 1;

        gbc.gridwidth = 1;

        gbc.gridheight = 1;

        notesPanel.add(secondRowPanelNotes, gbc);

    }

    /** Constructor */
    public GanttTaskPropertiesBean(GanttTask[] selectedTasks, IGanttProject project, UIFacade uifacade) {
        this.onlyOneTask = false;
        if (selectedTasks.length == 1) {
            this.onlyOneTask = true;
        }
        this.selectedTasks = selectedTasks;
        setInitialValues(selectedTasks[0]);
        myHumanResourceManager = project.getHumanResourceManager();
        myRoleManager = project.getRoleManager();
        myTaskManager = project.getTaskManager();
        myProject = project;
        myUIfacade = uifacade;
//		setTree(tree);
        init();

        // this.managerHash = managerHash;

        setSelectedTask();
    }

    private JXDatePicker createDatePicker(ActionListener listener) {
        ImageIcon calendarImage = new ImageIcon(getClass().getResource(
        "/icons/calendar_16.gif"));
        Icon nextMonth = new ImageIcon(getClass()
                .getResource("/icons/nextmonth.gif"));
        Icon prevMonth = new ImageIcon(getClass()
                .getResource("/icons/prevmonth.gif"));
        UIManager.put("JXDatePicker.arrowDown.image", calendarImage);
        UIManager.put("JXMonthView.monthUp.image", prevMonth);
        UIManager.put("JXMonthView.monthDown.image", nextMonth);
        UIManager.put("JXMonthView.monthCurrent.image", calendarImage);
        JXDatePicker result = new JXDatePicker();
        result.addActionListener(listener);
        return result;
    }
    /** Init the widgets */
    private void init() {

        tabbedPane = new JTabbedPane();
        tabbedPane.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                changeNameOfTask();
                fireDurationChanged();
            }
        });
        constructGeneralPanel();

        tabbedPane.addTab(language.getText("general"), new ImageIcon(getClass()
                .getResource("/icons/properties_16.gif")), generalPanel);

        if (onlyOneTask) {
            constructPredecessorsPanel();
            tabbedPane.addTab(language.getText("predecessors"), new ImageIcon(
                    getClass().getResource("/icons/relashion.gif")),
                    predecessorsPanel);
        }

        constructResourcesPanel();

        tabbedPane.addTab(GanttProject.correctLabel(language.getText("human")),
                new ImageIcon(getClass().getResource("/icons/res_16.gif")),
                resourcesPanel);

        constructNotesPanel();

        tabbedPane.addTab(language.getText("notesTask"), new ImageIcon(
                getClass().getResource("/icons/note_16.gif")), notesPanel);

        setLayout(new BorderLayout());

        add(tabbedPane, BorderLayout.CENTER);

        constructCustomColumnPanel(myProject);
        tabbedPane.addTab(language.getText("customColumns"), new ImageIcon(
                getClass().getResource("/icons/custom.gif")),
                myCustomColumnPanel);
        tabbedPane.addFocusListener(new FocusAdapter() {
            private boolean isFirstFocusGain = true;
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                if (isFirstFocusGain) {
                    nameField1.requestFocus();
                    isFirstFocusGain = false;
                }
            }
        });
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(2,0,5,0));
    }

    // Input methods

    /**
     * as the name indicates, it will not replace the original GanttTask in the
     * Tree.
     */
    public Task[] getReturnTask() {
        GanttTask[] returnTask = new GanttTask[selectedTasks.length];

        for (int i = 0; i < selectedTasks.length; i++) {
            if (myAllocationsPanel[0].getTableModel().isChanged()) {
                if (i != 0) {
                    copyValues(myAllocationsPanel[0].getTableModel(),
                            myAllocationsPanel[i].getTableModel());
                }

            }
            myAllocationsPanel[i].getTableModel().commit();
            returnTask[i] = selectedTasks[i];

            // returnTask.setTaskID(selectedTask.getTaskID());
            mutator = selectedTasks[0].createMutator();
            if (onlyOneTask) {
                mutator.setName(getTaskName()); // getName()
                mutator.setProjectTask (false);
            }
            if (this.taskWebLink != null && !this.taskWebLink.equals(getWebLink())) {
                returnTask[i].setWebLink(getWebLink()); // getName()
            }
            if (mileStoneCheckBox1 != null) {
                if (this.taskIsMilestone != isMilestone()) {
                    mutator.setMilestone(isMilestone());
                }
            }
            else if (projectTaskCheckBox1 != null) {
                if (this.taskIsProjectTask != isProjectTask()) {
                    mutator.setProjectTask(isProjectTask());
                }
            }
            if (!this.taskStartDate.equals(getStart())) {
                mutator.setStart(getStart());
            }
            if (this.taskThirdDate == null && getThird() != null
                    || this.taskThirdDate != null && getThird() == null
                    || this.taskThirdDate != null && !this.taskThirdDate
                            .equals(getThird())
                    || this.taskThirdDateConstraint != getThirdDateConstraint()) {
                mutator.setThird(getThird(), getThirdDateConstraint());
            }

            if (getLength() > 0) {
                    mutator.setDuration(returnTask[i].getManager()
                            .createLength(getLength()));
            }
            if (!this.taskNotes.equals(getNotes())) {
                returnTask[i].setNotes(getNotes());
            }
            if (this.taskCompletionPercentage != getPercentComplete()) {
                mutator.setCompletionPercentage(getPercentComplete());
            }
            if (this.taskPriority != getPriority()) {
                returnTask[i].setPriority(getPriority());
            }
//            if (this.taskIsStartFixed != isStartFixed)
//                returnTask[i].setStartFixed(isStartFixed);
//            if (this.taskIsFinishFixed != isFinishFixed)
//                returnTask[i].setFinishFixed(isFinishFixed);
            if (isColorChanged) {
                returnTask[i].setColor(colorButton.getBackground());
            }
            if (this.taskShape == null && shapeComboBox.getSelectedIndex() != 0
                    || this.taskShape != null && !this.taskShape
                            .equals(shapeComboBox
                                    .getSelectedPaint())) {
                returnTask[i].setShape(new ShapePaint(
                        (ShapePaint) shapeComboBox.getSelectedPaint(),
                        Color.white, returnTask[i].getColor()));
            }
            if (returnTask[i].getShape() != null) {
                returnTask[i].setShape(new ShapePaint(returnTask[i].getShape(),
                        Color.white, returnTask[i].getColor()));
            }

            mutator.commit();
            myDependenciesPanel.commit();
//            if (onlyOneTask) {
//                myDependenciesPanel.getTableModel().commit();
//            }
            returnTask[i].applyThirdDateConstraint();
        }

        return returnTask;

    }

    /** as the name indicates */
    public void setSelectedTask() {
        // this.selectedTask = selectedTask;

        nameField1.setText(selectedTasks[0].getName());

        // nameField2.setText(selectedTask.toString());

        nameFieldNotes.setText(selectedTasks[0].toString());

        setName(selectedTasks[0].toString());

        durationField1.setText(selectedTasks[0].getLength() + "");

        durationFieldNotes.setText(selectedTasks[0].getLength() + "");

        percentCompleteSlider.setValue(new Integer(selectedTasks[0]
                .getCompletionPercentage()));

        priorityComboBox.setSelectedIndex(selectedTasks[0].getPriority().ordinal());

        if (selectedTasks[0].getThird() != null) {
            setThird(selectedTasks[0].getThird().Clone(), true);
        }

        setStart(selectedTasks[0].getStart().Clone(), true);

        setEnd(selectedTasks[0].getEnd().Clone(), true);

        thirdDateComboBox.setSelectedIndex(selectedTasks[0].getThirdDateConstraint());

        if (mileStoneCheckBox1 != null) {
            mileStoneCheckBox1.setSelected(selectedTasks[0].isMilestone());
        } else if (projectTaskCheckBox1 != null) {
            projectTaskCheckBox1.setSelected(selectedTasks[0].isProjectTask());
        }
        enableMilestoneUnfriendlyControls(!isMilestone());

        tfWebLink.setText(selectedTasks[0].getWebLink());

        if (selectedTasks[0].shapeDefined()) {
            for (int j = 0; j < ShapeConstants.PATTERN_LIST.length; j++) {
                if (selectedTasks[0].getShape().equals(
                        ShapeConstants.PATTERN_LIST[j])) {
                    shapeComboBox.setSelectedIndex(j);
                    break;
                }
            }
        }

        noteAreaNotes.setText(selectedTasks[0].getNotes());
        //setStartFixed(selectedTasks[0].isStartFixed());
//        setFinishFixed(selectedTasks[0].isFinishFixed());
        myUnpluggedClone = selectedTasks[0].unpluggedClone();
    }

    void enableMilestoneUnfriendlyControls(boolean enable) {
        myEndDatePicker.setEnabled(enable);
        durationField1.setEnabled(enable);
    }
    // Output methods

    /** as the name indicates */
    public boolean isMilestone() {
        if(mileStoneCheckBox1 == null) {
            return false;
        }
        return mileStoneCheckBox1.isSelected();
    }

    public boolean isProjectTask() {
        return projectTaskCheckBox1.isSelected();
    }

    public int getThirdDateConstraint() {
        return thirdDateComboBox.getSelectedIndex();
    }

    /** @returns the duration of the task */
    public int getLength() {
        int length;
        try {
            length = Integer.parseInt(durationField1.getText().trim());
        } catch (NumberFormatException e) {
            length = 0;
        }
        return length;
    }

    public void fireDurationChanged() {
        String value = durationField1.getText();
        try {
            int duration = Integer.parseInt(value);
            changeLength(duration);
        } catch (NumberFormatException e) {

        }

    }

    /** Set the duration of the task */
    public void changeLength(int _length) {
        if (_length <= 0) {
            _length = 1;
        }

        durationField1.setText(_length + "");
//        if (onlyOneTask) {
//            myDependenciesPanel.durationChanged(_length);
//        }
        myAllocationsPanel[0].durationChanged(_length);
        durationFieldNotes.setText(_length + "");

        // Calculate the end date for the given length
        myUnpluggedClone.setStart(start);
        myUnpluggedClone.setDuration(myUnpluggedClone.getManager().createLength(_length));
        setEnd(myUnpluggedClone.getEnd(), false);
    }

    /** @return the task notes */
    public String getNotes() {
        return noteAreaNotes.getText();
    }

    /** @return the name of the task */
    public String getTaskName() {
        String text = nameField1.getText();
        return text == null ? "" : text.trim();
    }

    /** @return the web link of the task. */
    public String getWebLink() {
        String text = tfWebLink.getText();
        return text == null ? "" : text.trim();
    }

    /** @return the task complete percentage */
    public int getPercentComplete() {
        return ((Integer) percentCompleteSlider.getValue()).hashCode();

    }

    /** @return the priority level of the task */
    public Task.Priority getPriority() {
        return Task.Priority.getPriority(priorityComboBox.getSelectedIndex());
    }

//    public void setStartFixed(boolean startFixed) {
//        isStartFixed = startFixed;
//        startDateField1.setForeground(isStartFixed ? Color.BLACK : Color.GRAY);
//    }

//    public void setFinishFixed(boolean startFixed) {
//        isFinishFixed = startFixed;
//        finishDateField1
//                .setForeground(isFinishFixed ? Color.BLACK : Color.GRAY);
//    }

    /** Return the start date of the task */
    public GanttCalendar getStart() {
        //start.setFixed(isStartFixed);
        return start;
    }

    public GanttCalendar getEnd() {
//        end.setFixed(isFinishFixed);
        return end;
    }

    public GanttCalendar getThird() {
        return third;
    }

    /** Change the start date of the task */
    public void setStart(GanttCalendar dstart, boolean test) {
        myStartDatePicker.setDate(dstart.getTime());
        this.start = dstart;
        if (test == true) {
            return;
        }

//        this.setStartFixed(dstart.isFixed());

        if (this.start.compareTo(this.end) < 0) {
            adjustLength();
        } else {
            GanttCalendar _end = start.newAdd(this.taskLength);
            this.end = _end;
            //finishDateField1.setText(_end.toString());
            this.myEndDatePicker.setDate(this.end.getTime());
        }
    }

    /** Change the end date of the task */
    public void setEnd(GanttCalendar dend, boolean test) {
        myEndDatePicker.setDate(dend.newAdd(-1).getTime());
        this.end = dend;
        if (test == true) {
            return;
        }
//        this.setFinishFixed(dend.isFixed());

        if (this.start.compareTo(this.end) < 0) {
            adjustLength();
        } else {
            GanttCalendar _start = this.end.newAdd(-1 * getLength());
            this.start = _start;
        }
    }

    /** Change the third date of the task */
    public void setThird(GanttCalendar dthird, boolean test) {
        myThirdDatePicker.setDate(dthird.getTime());
        this.third = dthird;
    }

    private void adjustLength() {
        int length;
        myUnpluggedClone.setStart(this.start);
        myUnpluggedClone.setEnd(this.end);
        length = (int) myUnpluggedClone.getDuration().getLength();
        durationField1.setText("" + length);
        // durationField2.setText(""+length);
        myAllocationsPanel[0].durationChanged(length);
        durationFieldNotes.setText("" + length);
    }

    private void setInitialValues(GanttTask task) {
        this.taskWebLink = task.getWebLink();
        this.taskIsMilestone = task.isMilestone();
        this.taskStartDate = task.getStart();
        this.taskLength = task.getLength();
        this.taskNotes = task.getNotes();
        this.taskCompletionPercentage = task.getCompletionPercentage();
        this.taskPriority = task.getPriority();
        //this.taskIsStartFixed = task.isStartFixed();
//        this.taskIsFinishFixed = task.isFinishFixed();
        this.taskShape = task.getShape();
        this.taskThirdDate = task.getThird();
        this.taskThirdDateConstraint = task.getThirdDateConstraint();
        this.taskIsProjectTask = task.isProjectTask();
    }

    private void copyValues(ResourcesTableModel original,
            ResourcesTableModel clone) {
        for (int i = 0; i < clone.getRowCount(); i++) {
            clone.setValueAt(null, i, 1);
        }
        for (int j = 0; j < original.getRowCount(); j++) {
            for (int k = 0; k < original.getColumnCount(); k++) {
                clone.setValueAt(original.getValueAt(j, k), j, k);
            }
        }
    }

    private boolean canBeProjectTask(Task testedTask, TaskContainmentHierarchyFacade taskHierarchy) {
        Task[] nestedTasks = taskHierarchy.getNestedTasks(testedTask);
        if (nestedTasks.length==0) {
            return false;
        }
        for (Task parent = taskHierarchy.getContainer(testedTask); parent!=null; parent = taskHierarchy.getContainer(parent)) {
            if (parent.isProjectTask()) {
                return false;
            }
        }
        for (int i=0; i<nestedTasks.length; i++) {
            if (isProjectTaskOrContainsProjectTask(nestedTasks[i])) {
                return false;
            }
        }
        return true;
    }
    private boolean isProjectTaskOrContainsProjectTask(Task task) {
        if (task.isProjectTask()) {
            return true;
        }
        boolean result = false;
        Task[] nestedTasks = task.getNestedTasks();
        for (int i=0; i<nestedTasks.length; i++) {
            if (isProjectTaskOrContainsProjectTask(nestedTasks[i])) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Creates a milestone, a project task or no checkbox depending on the selected task
     * @return the created checkbox or null
     */
    private JCheckBox constructCheckBox () {
        boolean canBeProjectTask = true;
        boolean canBeMilestone = true;
        TaskContainmentHierarchyFacade taskHierarchy = myTaskManager.getTaskHierarchy();
        for (int i = 0 ; i < selectedTasks.length ; i++) {
            canBeMilestone &= !taskHierarchy.hasNestedTasks(selectedTasks[i]);
            canBeProjectTask &= canBeProjectTask(selectedTasks[i], taskHierarchy);
        }
        assert false==(canBeProjectTask && canBeMilestone);

        final JCheckBox result;
        if (canBeProjectTask) {
            projectTaskCheckBox1 = new JCheckBox (language.getText("projectTask"));
            result = projectTaskCheckBox1;
        }
        else if (canBeMilestone) {
            mileStoneCheckBox1 = new JCheckBox(new AbstractAction(language.getText("meetingPoint")) {
                public void actionPerformed(ActionEvent arg0) {
                    enableMilestoneUnfriendlyControls(!isMilestone());
                }
            });
            result = mileStoneCheckBox1;
        }
        else {
            result = null;
        }
        return result;
    }

}
