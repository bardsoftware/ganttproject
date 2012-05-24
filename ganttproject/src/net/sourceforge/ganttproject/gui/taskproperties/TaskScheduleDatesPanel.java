/*
Copyright 2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

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
package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Calendar;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;

import org.jdesktop.swingx.JXDatePicker;

/**
 * Encapsulates start date/end date/duration field.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskScheduleDatesPanel {
  private static final GanttLanguage language = GanttLanguage.getInstance();
  private JXDatePicker myStartDatePicker;
  private JXDatePicker myEndDatePicker;
  private JTextField durationField1;

  /** Radio button to lock the start field */
  private JRadioButton startLock;

  /** Radio button to lock the end field */
  private JRadioButton endLock;

  /** Radio button to lock the duration field */
  private JRadioButton durationLock;

  private GanttCalendar myStart;

  private GanttCalendar myEnd;

  private Task myUnpluggedClone;

  public TaskScheduleDatesPanel() {
  }

  public void setUnpluggedClone(Task unpluggedClone) {
    myUnpluggedClone = unpluggedClone;
  }

  public void insertInto(JPanel propertiesPanel) {
    // Begin date
    propertiesPanel.add(new JLabel(language.getText("dateOfBegining")));
    Box startBox = Box.createHorizontalBox();
    myStartDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setStart(new GanttCalendar(((JXDatePicker) e.getSource()).getDate()), false);
      }
    });
    startBox.add(myStartDatePicker);
    startLock = new JRadioButton(language.getText("lockField"));
    startBox.add(startLock);
    propertiesPanel.add(startBox);

    // End date
    propertiesPanel.add(new JLabel(language.getText("dateOfEnd")));
    Box endBox = Box.createHorizontalBox();
    myEndDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GanttCalendar c = new GanttCalendar(((JXDatePicker) e.getSource()).getDate());
        c.add(Calendar.DATE, 1);
        setEnd(c, false);
      }
    });
    endBox.add(myEndDatePicker);
    endLock = new JRadioButton(language.getText("lockField"));
    endBox.add(endLock);
    propertiesPanel.add(endBox);

    // Duration
    propertiesPanel.add(new JLabel(language.getText("length")));
    Box durationBox = Box.createHorizontalBox();
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
    durationBox.add(durationField1);
    durationLock = new JRadioButton(language.getText("lockField"));
    durationBox.add(durationLock);
    propertiesPanel.add(durationBox);

    // Group radio buttons
    ButtonGroup group = new ButtonGroup();
    group.add(startLock);
    group.add(endLock);
    group.add(durationLock);

    // Enable lock start by default
    // TODO It is nicer to remember the previous used lock
    startLock.setSelected(true);
  }

  public void setStart(GanttCalendar start, boolean test) {
    myStart = start;
    myStartDatePicker.setDate(myStart.getTime());
    if (test == true) {
      return;
    }
    if (endLock.isSelected()) {
      // End is locked, so adjust duration
      adjustLength();
    } else {
      myEnd = myStart.clone();
      myEnd.add(Calendar.DATE, getLength() - 1);
      myEndDatePicker.setDate(myEnd.getTime());
    }
  }

  public void setEnd(GanttCalendar end, boolean test) {
    myEnd = end;
    myEndDatePicker.setDate(myEnd.newAdd(Calendar.DATE, -1).getTime());
    if (test == true) {
      return;
    }
    if (startLock.isSelected()) {
      // Start is locked, so adjust duration
      adjustLength();
    } else {
      myStart = myEnd.clone();
      myStart.add(Calendar.DATE, -1 * getLength());
      myStartDatePicker.setDate(myStart.getTime());
    }
  }

  public int getLength() {
    try {
      return Integer.parseInt(durationField1.getText().trim());
    } catch (NumberFormatException e) {
      return 0;
    }
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

    if (endLock.isSelected()) {
      // Calculate the start date for the given length
      myStart = myEnd.clone();
      myStart.add(Calendar.DATE, -1 * getLength());
      myStartDatePicker.setDate(myStart.getTime());
    } else {
      // Calculate the end date for the given length
      myEnd = myStart.clone();
      myEnd.add(Calendar.DATE, 1 * getLength());
      myEndDatePicker.setDate(myEnd.getTime());
    }
  }

  private void adjustLength() {
    myUnpluggedClone.setStart(myStart);
    myUnpluggedClone.setEnd(myEnd);
    int length = myUnpluggedClone.getDuration().getLength();
    if (length > 0) {
      durationField1.setText(String.valueOf(length));
    } else {
      // Start is bigger than end Date, so set length to 1 and adjust the
      // non-locked field
      // TODO It would be nice if the user is notified of the illegal date he
      // selected
      changeLength(1);
    }
  }

  public GanttCalendar getEnd() {
    return myEnd;
  }

  public GanttCalendar getStart() {
    return myStart;
  }

  public void enableMilestoneUnfriendlyControls(boolean enable) {
    myEndDatePicker.setEnabled(enable);
    durationField1.setEnabled(enable);
  }
}
