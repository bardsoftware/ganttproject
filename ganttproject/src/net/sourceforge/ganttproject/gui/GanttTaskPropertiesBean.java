/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Calendar;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.gui.options.model.ColorOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultColorOption;
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
import net.sourceforge.ganttproject.util.collect.Pair;

import org.jdesktop.swing.JXDatePicker;

/**
 * Real panel for editing task properties
 */
public class GanttTaskPropertiesBean extends JPanel {

  private ColorOption myTaskColorOption = new DefaultColorOption("");
  private JXDatePicker myStartDatePicker;
  private JXDatePicker myEndDatePicker;
  private JXDatePicker myThirdDatePicker;

  protected GanttTask[] selectedTasks;

  private static final GanttLanguage language = GanttLanguage.getInstance();

  private GanttCalendar myStart;

  private GanttCalendar myEnd;

  private GanttCalendar myThird;

  private JTabbedPane tabbedPane; // TabbedPane that includes the following four
                                  // items

  private JPanel generalPanel;

  private JComponent predecessorsPanel;

  private JPanel resourcesPanel;

  private JPanel notesPanel;

  private JTextField nameField1;

  private JTextField durationField1;

  private JTextField tfWebLink;

  private JButton bWebLink;

  private JSpinner percentCompleteSlider;

  private JComboBox priorityComboBox;

  private JComboBox thirdDateComboBox;

  private JCheckBox mileStoneCheckBox1;

  private JCheckBox projectTaskCheckBox1;

  /** Shape chooser combo Box */
  private JPaintCombo shapeComboBox;

  private JScrollPane scrollPaneNotes;

  private JTextArea noteAreaNotes;

  private JPanel secondRowPanelNotes;

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

  private TaskDependenciesPanel myDependenciesPanel;

  private TaskAllocationsPanel myAllocationsPanel;

  private final HumanResourceManager myHumanResourceManager;

  private final RoleManager myRoleManager;

  private Task myUnpluggedClone;
  private final TaskManager myTaskManager;
  private final IGanttProject myProject;
  private final UIFacade myUIfacade;

  private TaskMutator mutator;

  public GanttTaskPropertiesBean(GanttTask[] selectedTasks, IGanttProject project, UIFacade uifacade) {
    this.selectedTasks = selectedTasks;
    setInitialValues(selectedTasks[0]);
    myHumanResourceManager = project.getHumanResourceManager();
    myRoleManager = project.getRoleManager();
    myTaskManager = project.getTaskManager();
    myProject = project;
    myUIfacade = uifacade;
    init();
    setSelectedTask();
  }

  private static void addEmptyRow(JPanel form) {
    form.add(Box.createRigidArea(new Dimension(1, 10)));
    form.add(Box.createRigidArea(new Dimension(1, 10)));
  }

  /** Construct the general panel */
  private void constructGeneralPanel() {
    JPanel propertiesPanel = new JPanel(new SpringLayout());

    propertiesPanel.add(new JLabel(language.getText("name")));
    nameField1 = new JTextField(20);
    nameField1.setName("name_of_task");
    propertiesPanel.add(nameField1);
    Pair<String, JCheckBox> checkBox = constructCheckBox();
    if (checkBox != null) {
      propertiesPanel.add(new JLabel(checkBox.first()));
      propertiesPanel.add(checkBox.second());
    }
    addEmptyRow(propertiesPanel);
    propertiesPanel.add(new JLabel(language.getText("dateOfBegining")));
    myStartDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setStart(new GanttCalendar(((JXDatePicker) e.getSource()).getDate()), false);
      }
    });
    propertiesPanel.add(myStartDatePicker);

    propertiesPanel.add(new JLabel(language.getText("dateOfEnd")));
    myEndDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GanttCalendar c = new GanttCalendar(((JXDatePicker) e.getSource()).getDate());
        c.add(Calendar.DATE, 1);
        setEnd(c, false);
      }
    });
    propertiesPanel.add(myEndDatePicker);

    propertiesPanel.add(new JLabel(language.getText("length")));
    durationField1 = new JTextField(8);
    durationField1.setName("length");
    durationField1.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent e) {
        fireDurationChanged();
      }

      @Override
      public void focusGained(FocusEvent e) {
      }
    });
    propertiesPanel.add(durationField1);

    Box extraConstraintBox = Box.createHorizontalBox();
    thirdDateComboBox = new JComboBox();
    thirdDateComboBox.addItem("");
    thirdDateComboBox.addItem(language.getText("earliestBegin"));
    thirdDateComboBox.setName("third");
    thirdDateComboBox.addActionListener(new ActionListener() {
      @Override
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
    extraConstraintBox.add(thirdDateComboBox);
    myThirdDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setThird(new GanttCalendar(((JXDatePicker) e.getSource()).getDate()), false);
      }
    });
    extraConstraintBox.add(Box.createHorizontalStrut(5));
    extraConstraintBox.add(myThirdDatePicker);
    propertiesPanel.add(new JLabel(language.getText("option.taskProperties.main.extraConstraint.label")));
    propertiesPanel.add(extraConstraintBox);

    addEmptyRow(propertiesPanel);

    propertiesPanel.add(new JLabel(language.getText("priority")));
    priorityComboBox = new JComboBox();
    for (Task.Priority p : Task.Priority.values()) {
      priorityComboBox.addItem(language.getText(p.getI18nKey()));
    }
    priorityComboBox.setEditable(false);
    propertiesPanel.add(priorityComboBox);

    propertiesPanel.add(new JLabel(language.getText("advancement")));
    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
    percentCompleteSlider = new JSpinner(spinnerModel);
    propertiesPanel.add(percentCompleteSlider);

    addEmptyRow(propertiesPanel);

    propertiesPanel.add(new JLabel(language.getText("shape")));
    shapeComboBox = new JPaintCombo(ShapeConstants.PATTERN_LIST);
    propertiesPanel.add(shapeComboBox);

    OptionsPageBuilder builder = new OptionsPageBuilder(GanttTaskPropertiesBean.this);
    Box colorBox = Box.createHorizontalBox();
    colorBox.add(builder.createColorComponent(myTaskColorOption));

    propertiesPanel.add(new JLabel(language.getText("colors")));
    propertiesPanel.add(colorBox);

    Box weblinkBox = Box.createHorizontalBox();
    tfWebLink = new JTextField(20);
    weblinkBox.add(tfWebLink);
    weblinkBox.add(Box.createHorizontalStrut(2));
    bWebLink = new TestGanttRolloverButton(new ImageIcon(getClass().getResource("/icons/web_16.gif")));
    bWebLink.setToolTipText(GanttProject.getToolTip(language.getText("openWebLink")));
    weblinkBox.add(bWebLink);

    bWebLink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // link to open the web link
        if (!BrowserControl.displayURL(tfWebLink.getText())) {
          GanttDialogInfo gdi = new GanttDialogInfo(null, GanttDialogInfo.ERROR, GanttDialogInfo.YES_OPTION,
              language.getText("msg4"), language.getText("error"));
          gdi.setVisible(true);
        }
      }
    });
    propertiesPanel.add(new JLabel(language.getText("webLink")));
    propertiesPanel.add(weblinkBox);

    SpringUtilities.makeCompactGrid(propertiesPanel, propertiesPanel.getComponentCount() / 2, 2, 1, 1, 5, 5);

    generalPanel = new JPanel(new SpringLayout());
    generalPanel.add(propertiesPanel);
    generalPanel.add(notesPanel);
    SpringUtilities.makeCompactGrid(generalPanel, 1, 2, 1, 1, 10, 5);
    generalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
  }

  /** Add the different action listeners on the different widgets */
  public void addActionListener(ActionListener l) {
    nameField1.addActionListener(l);
    thirdDateComboBox.addActionListener(l);
    durationField1.addActionListener(l);
  }

  /** Change the name of the task on all text fields containing task name */
  private void changeNameOfTask() {
    if (nameField1 != null) {
      String nameOfTask = nameField1.getText().trim();
      nameField1.setText(nameOfTask);
    }
  }

  private void constructCustomColumnPanel() {
    myCustomColumnPanel = new CustomColumnsPanel(myProject.getTaskCustomColumnManager(), myUIfacade,
        selectedTasks[0].getCustomValues(), myUIfacade.getTaskTree().getVisibleFields());
  }

  /** Construct the predecessors tabbed pane */
  private void constructPredecessorsPanel() {
    myDependenciesPanel = new TaskDependenciesPanel();
    myDependenciesPanel.init(selectedTasks[0]);
    predecessorsPanel = myDependenciesPanel.getComponent();
  }

  /** Construct the resources panel */
  private void constructResourcesPanel() {
    myAllocationsPanel = new TaskAllocationsPanel(selectedTasks[0], myHumanResourceManager, myRoleManager);
    resourcesPanel = myAllocationsPanel.getComponent();
  }

  /** Construct the notes panel */
  private void constructNotesPanel() {
    secondRowPanelNotes = new JPanel(new BorderLayout());
    UIUtil.createTitle(secondRowPanelNotes, language.getText("notesTask"));

    noteAreaNotes = new JTextArea(8, 40);
    noteAreaNotes.setLineWrap(true);
    noteAreaNotes.setWrapStyleWord(true);
    noteAreaNotes.setBackground(new Color(1.0f, 1.0f, 1.0f));

    scrollPaneNotes = new JScrollPane(noteAreaNotes);
    secondRowPanelNotes.add(scrollPaneNotes, BorderLayout.CENTER);
    notesPanel = secondRowPanelNotes;
  }

  /** Initialize the widgets */
  private void init() {
    constructNotesPanel();

    tabbedPane = new JTabbedPane();
    tabbedPane.getModel().addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        changeNameOfTask();
        fireDurationChanged();
      }
    });
    constructGeneralPanel();

    tabbedPane.addTab(language.getText("general"), new ImageIcon(getClass().getResource("/icons/properties_16.gif")),
        generalPanel);

    constructPredecessorsPanel();
    tabbedPane.addTab(language.getText("predecessors"), new ImageIcon(getClass().getResource("/icons/relashion.gif")),
        predecessorsPanel);

    constructResourcesPanel();

    tabbedPane.addTab(language.getCorrectedLabel("human"), new ImageIcon(getClass().getResource("/icons/res_16.gif")),
        resourcesPanel);

    setLayout(new BorderLayout());

    add(tabbedPane, BorderLayout.CENTER);

    constructCustomColumnPanel();
    tabbedPane.addTab(language.getText("customColumns"), new ImageIcon(getClass().getResource("/icons/custom.gif")),
        myCustomColumnPanel.getComponent());
    tabbedPane.addFocusListener(new FocusAdapter() {
      private boolean isFirstFocusGain = true;

      @Override
      public void focusGained(FocusEvent e) {
        super.focusGained(e);
        if (isFirstFocusGain) {
          nameField1.requestFocus();
          isFirstFocusGain = false;
        }
      }
    });
    tabbedPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 5, 0));
  }

  // TODO The name of the method is very confusing... Rename to applySettings
  // and remove return value?
  public Task[] getReturnTask() {
    GanttTask[] returnTask = new GanttTask[selectedTasks.length];

    for (int i = 0; i < selectedTasks.length; i++) {
      returnTask[i] = selectedTasks[i];

      mutator = selectedTasks[0].createMutator();
      mutator.setName(getTaskName()); // getName()
      mutator.setProjectTask(false);
      if (this.taskWebLink != null && !this.taskWebLink.equals(getWebLink())) {
        returnTask[i].setWebLink(getWebLink()); // getName()
      }
      if (mileStoneCheckBox1 != null) {
        if (this.taskIsMilestone != isMilestone()) {
          mutator.setMilestone(isMilestone());
        }
      } else if (projectTaskCheckBox1 != null) {
        if (this.taskIsProjectTask != isProjectTask()) {
          mutator.setProjectTask(isProjectTask());
        }
      }
      if (!this.taskStartDate.equals(getStart())) {
        mutator.setStart(getStart());
      }
      if (this.taskThirdDate == null && getThird() != null || this.taskThirdDate != null && getThird() == null
          || this.taskThirdDate != null && !this.taskThirdDate.equals(getThird())
          || this.taskThirdDateConstraint != getThirdDateConstraint()) {
        mutator.setThird(getThird(), getThirdDateConstraint());
      }

      if (getLength() > 0) {
        mutator.setDuration(returnTask[i].getManager().createLength(getLength()));
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
      returnTask[i].setColor(myTaskColorOption.getValue());
      if (this.taskShape == null && shapeComboBox.getSelectedIndex() != 0 || this.taskShape != null
          && !this.taskShape.equals(shapeComboBox.getSelectedPaint())) {
        returnTask[i].setShape(new ShapePaint((ShapePaint) shapeComboBox.getSelectedPaint(), Color.white,
            returnTask[i].getColor()));
      }
      if (returnTask[i].getShape() != null) {
        returnTask[i].setShape(new ShapePaint(returnTask[i].getShape(), Color.white, returnTask[i].getColor()));
      }

      mutator.commit();
      myDependenciesPanel.commit();
      myAllocationsPanel.commit();
      returnTask[i].applyThirdDateConstraint();
    }

    return returnTask;
  }

  private void setSelectedTask() {
    nameField1.setText(selectedTasks[0].getName());

    setName(selectedTasks[0].toString());

    durationField1.setText(String.valueOf(selectedTasks[0].getLength()));

    percentCompleteSlider.setValue(new Integer(selectedTasks[0].getCompletionPercentage()));
    priorityComboBox.setSelectedIndex(selectedTasks[0].getPriority().ordinal());

    setStart(selectedTasks[0].getStart().clone(), true);
    setEnd(selectedTasks[0].getEnd().clone(), true);
    if (selectedTasks[0].getThird() != null) {
      setThird(selectedTasks[0].getThird().clone(), true);
    }
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
        if (selectedTasks[0].getShape().equals(ShapeConstants.PATTERN_LIST[j])) {
          shapeComboBox.setSelectedIndex(j);
          break;
        }
      }
    }

    noteAreaNotes.setText(selectedTasks[0].getNotes());
    myTaskColorOption.setValue(selectedTasks[0].getColor());
    myUnpluggedClone = selectedTasks[0].unpluggedClone();
  }

  private void enableMilestoneUnfriendlyControls(boolean enable) {
    myEndDatePicker.setEnabled(enable);
    durationField1.setEnabled(enable);
  }

  private boolean isMilestone() {
    if (mileStoneCheckBox1 == null) {
      return false;
    }
    return mileStoneCheckBox1.isSelected();
  }

  private boolean isProjectTask() {
    return projectTaskCheckBox1.isSelected();
  }

  private int getThirdDateConstraint() {
    return thirdDateComboBox.getSelectedIndex();
  }

  private int getLength() {
    int length;
    try {
      length = Integer.parseInt(durationField1.getText().trim());
    } catch (NumberFormatException e) {
      length = 0;
    }
    return length;
  }

  private void fireDurationChanged() {
    String value = durationField1.getText();
    try {
      int duration = Integer.parseInt(value);
      changeLength(duration);
    } catch (NumberFormatException e) {

    }
  }

  private void changeLength(int length) {
    if (length <= 0) {
      length = 1;
    }
    durationField1.setText(String.valueOf(length));

    // Calculate the end date for the given length
    myUnpluggedClone.setStart(myStart);
    myUnpluggedClone.setDuration(myUnpluggedClone.getManager().createLength(length));
    setEnd(myUnpluggedClone.getEnd(), false);
  }

  private String getNotes() {
    return noteAreaNotes.getText();
  }

  private String getTaskName() {
    String text = nameField1.getText();
    return text == null ? "" : text.trim();
  }

  private String getWebLink() {
    String text = tfWebLink.getText();
    return text == null ? "" : text.trim();
  }

  private int getPercentComplete() {
    return ((Integer) percentCompleteSlider.getValue()).hashCode();
  }

  private Task.Priority getPriority() {
    return Task.Priority.getPriority(priorityComboBox.getSelectedIndex());
  }

  private GanttCalendar getStart() {
    return myStart;
  }

  private GanttCalendar getThird() {
    return myThird;
  }

  private void setStart(GanttCalendar start, boolean test) {
    myStart = start;
    myStartDatePicker.setDate(myStart.getTime());
    if (test == true) {
      return;
    }
    if (myStart.compareTo(myEnd) < 0) {
      adjustLength();
    } else {
      myEnd = myStart.clone();
      myEnd.add(Calendar.DATE, taskLength);
      myEndDatePicker.setDate(myEnd.getTime());
    }
  }

  private void setEnd(GanttCalendar end, boolean test) {
    myEnd = end;
    myEndDatePicker.setDate(myEnd.newAdd(Calendar.DATE, -1).getTime());
    if (test == true) {
      return;
    }
    if (myStart.compareTo(myEnd) < 0) {
      adjustLength();
    } else {
      myStart = myEnd.clone();
      myStart.add(Calendar.DATE, -1 * getLength());
    }
  }

  private void setThird(GanttCalendar third, boolean test) {
    myThird = third;
    myThirdDatePicker.setDate(myThird.getTime());
  }

  private void adjustLength() {
    int length;
    myUnpluggedClone.setStart(this.myStart);
    myUnpluggedClone.setEnd(this.myEnd);
    length = myUnpluggedClone.getDuration().getLength();
    durationField1.setText(String.valueOf(length));
  }

  private void setInitialValues(GanttTask task) {
    taskWebLink = task.getWebLink();
    taskIsMilestone = task.isMilestone();
    taskStartDate = task.getStart();
    taskLength = task.getLength();
    taskNotes = task.getNotes();
    taskCompletionPercentage = task.getCompletionPercentage();
    taskPriority = task.getPriority();
    taskShape = task.getShape();
    taskThirdDate = task.getThird();
    taskThirdDateConstraint = task.getThirdDateConstraint();
    taskIsProjectTask = task.isProjectTask();
  }

  private boolean canBeProjectTask(Task testedTask, TaskContainmentHierarchyFacade taskHierarchy) {
    Task[] nestedTasks = taskHierarchy.getNestedTasks(testedTask);
    if (nestedTasks.length == 0) {
      return false;
    }
    for (Task parent = taskHierarchy.getContainer(testedTask); parent != null; parent = taskHierarchy.getContainer(parent)) {
      if (parent.isProjectTask()) {
        return false;
      }
    }
    for (Task nestedTask : nestedTasks) {
      if (isProjectTaskOrContainsProjectTask(nestedTask)) {
        return false;
      }
    }
    return true;
  }

  private boolean isProjectTaskOrContainsProjectTask(Task task) {
    if (task.isProjectTask()) {
      return true;
    }
    for (Task nestedTask : task.getNestedTasks()) {
      if (isProjectTaskOrContainsProjectTask(nestedTask)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a milestone, a project task or no checkbox depending on the
   * selected task
   *
   * @return the created checkbox or null
   */
  private Pair<String, JCheckBox> constructCheckBox() {
    boolean canBeProjectTask = true;
    boolean canBeMilestone = true;
    TaskContainmentHierarchyFacade taskHierarchy = myTaskManager.getTaskHierarchy();
    for (Task task : selectedTasks) {
      canBeMilestone &= !taskHierarchy.hasNestedTasks(task);
      canBeProjectTask &= canBeProjectTask(task, taskHierarchy);
    }
    assert false == (canBeProjectTask && canBeMilestone);

    final Pair<String, JCheckBox> result;
    if (canBeProjectTask) {
      projectTaskCheckBox1 = new JCheckBox();
      result = Pair.create(language.getText("projectTask"), projectTaskCheckBox1);
    } else if (canBeMilestone) {
      mileStoneCheckBox1 = new JCheckBox(new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          enableMilestoneUnfriendlyControls(!isMilestone());
        }
      });
      result = Pair.create(language.getText("meetingPoint"), mileStoneCheckBox1);
    } else {
      result = null;
    }
    return result;
  }
}
