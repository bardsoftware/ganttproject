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
import javax.swing.SpringLayout;
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
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
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

    private JScrollPane scrollPaneNotes;

    private JTextArea noteAreaNotes;

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

    private TaskAllocationsPanel myAllocationsPanel;

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
        JPanel propertiesPanel = new JPanel(new SpringLayout());

        propertiesPanel.add(new JLabel(language.getText("name") + ":"));
        nameField1 = new JTextField(20);
        nameField1.setName("name_of_task");
        propertiesPanel.add(nameField1);

        propertiesPanel.add(new JLabel(language.getText("length") + ":"));
        durationField1 = new JTextField(8);
        durationField1.setName("length");
        durationField1.addFocusListener(new FocusListener() {
                       public void focusLost(FocusEvent e) {
                               fireDurationChanged();
                       }
                       public void focusGained(FocusEvent e) {}
               });
        propertiesPanel.add(durationField1);

        propertiesPanel.add(new JLabel(language.getText("advancement")));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
        percentCompleteSlider = new JSpinner(spinnerModel);
        propertiesPanel.add(percentCompleteSlider);
        
        propertiesPanel.add(new JLabel(language.getText("priority")));
        priorityComboBox = new JComboBox();
        for (Task.Priority p: Task.Priority.values()) {
            priorityComboBox.addItem(language.getText(p.getI18nKey()));
        }
        priorityComboBox.setEditable(false);
        propertiesPanel.add(priorityComboBox);

        propertiesPanel.add(new JLabel(language.getText("dateOfBegining") + ":"));
        myStartDatePicker = createDatePicker(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStart(new GanttCalendar(((JXDatePicker)e.getSource()).getDate()), false);
            }
        });
        propertiesPanel.add(myStartDatePicker);
        
        propertiesPanel.add(new JLabel(language.getText("dateOfEnd") + ":"));
        myEndDatePicker = createDatePicker(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setEnd(new GanttCalendar(((JXDatePicker)e.getSource()).getDate()).newAdd(1), false);
            }
        });
        propertiesPanel.add(myEndDatePicker);
        
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
        propertiesPanel.add(thirdDateComboBox);
        myThirdDatePicker = createDatePicker(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setThird(new GanttCalendar(((JXDatePicker)e.getSource()).getDate()), false);
            }
        });
        propertiesPanel.add(myThirdDatePicker);

        // fourth row

        JCheckBox checkBox = constructCheckBox ();
        propertiesPanel.add(checkBox);
        propertiesPanel.add(new JPanel());

        propertiesPanel.add(new JLabel("  " + language.getText("shape") + " "));
        shapeComboBox = new JPaintCombo(ShapeConstants.PATTERN_LIST);
        propertiesPanel.add(shapeComboBox);

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
        propertiesPanel.add(colorButton);
        propertiesPanel.add(defaultColorButton);
        
        propertiesPanel.add(new JLabel(language.getText("webLink")));
        
        JPanel webLinkPanel = new JPanel(new FlowLayout());
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
        propertiesPanel.add(webLinkPanel);
        SpringUtilities.makeCompactGrid(propertiesPanel, 11, 2, 1, 1, 3, 5);
        
        generalPanel = new JPanel(new SpringLayout());
        generalPanel.add(propertiesPanel);
        generalPanel.add(notesPanel);
        SpringUtilities.makeCompactGrid(generalPanel, 1, 2, 1, 1, 10, 5);
    }

    /** Add the different action listeners on the different widgets */
    public void addActionListener(ActionListener l) {

        nameField1.addActionListener(l);

        thirdDateComboBox.addActionListener(l);

        durationField1.addActionListener(l);

    }

    /** Change the name of the task on all text fields containing task name */
    public void changeNameOfTask() {
        if (nameField1 != null) {
            String nameOfTask = nameField1.getText().trim();
            nameField1.setText(nameOfTask);
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
    	myAllocationsPanel = new TaskAllocationsPanel(
    			selectedTasks[0], myHumanResourceManager, myRoleManager);
    	resourcesPanel = myAllocationsPanel.getComponent();
    }

    /** construct the notes panel */
    private void constructNotesPanel() {
        secondRowPanelNotes = new JPanel(new BorderLayout());
        secondRowPanelNotes.setBorder(new TitledBorder(
        		new EtchedBorder(), language.getText("notesTask") + ":"));

        noteAreaNotes = new JTextArea(8, 40);
        noteAreaNotes.setLineWrap(true);
        noteAreaNotes.setWrapStyleWord(true);
        noteAreaNotes.setBackground(new Color(1.0f, 1.0f, 1.0f));

        scrollPaneNotes = new JScrollPane(noteAreaNotes);
        secondRowPanelNotes.add(scrollPaneNotes, BorderLayout.CENTER);
        notesPanel = secondRowPanelNotes;
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
        constructNotesPanel();

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

//        tabbedPane.addTab(language.getText("notesTask"), new ImageIcon(
//                getClass().getResource("/icons/note_16.gif")), notesPanel);

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
            myAllocationsPanel.commit();
            returnTask[i].applyThirdDateConstraint();
        }

        return returnTask;

    }

    /** as the name indicates */
    public void setSelectedTask() {
        // this.selectedTask = selectedTask;

        nameField1.setText(selectedTasks[0].getName());

        setName(selectedTasks[0].toString());

        durationField1.setText(selectedTasks[0].getLength() + "");

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
    }

    private void setInitialValues(GanttTask task) {
        this.taskWebLink = task.getWebLink();
        this.taskIsMilestone = task.isMilestone();
        this.taskStartDate = task.getStart();
        this.taskLength = task.getLength();
        this.taskNotes = task.getNotes();
        this.taskCompletionPercentage = task.getCompletionPercentage();
        this.taskPriority = task.getPriority();
        this.taskShape = task.getShape();
        this.taskThirdDate = task.getThird();
        this.taskThirdDateConstraint = task.getThirdDateConstraint();
        this.taskIsProjectTask = task.isProjectTask();
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
