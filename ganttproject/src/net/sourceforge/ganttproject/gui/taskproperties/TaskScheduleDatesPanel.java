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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Calendar;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
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

  private GanttCalendar myStart;

  private GanttCalendar myEnd;

  private Task myUnpluggedClone;
  private final BooleanOption myStartDateLock = new DefaultBooleanOption("", true);
  private final BooleanOption myEndDateLock = new DefaultBooleanOption("", false);
  private final BooleanOption myDurationLock = new DefaultBooleanOption("", false);
  protected BooleanOption myPrevLock = myStartDateLock;

  public TaskScheduleDatesPanel() {
  }

  public void setUnpluggedClone(Task unpluggedClone) {
    myUnpluggedClone = unpluggedClone;
  }

  private static final Icon ICON_LOCKED = GPAction.getIcon("16", "status-locked.png");
  private static final Icon ICON_UNLOCKED = GPAction.getIcon("16", "status-unlocked.png");

  private static JComponent createLabel(String title, final BooleanOption isLocked, final MouseListener lockListener) {
    final JPanel labelPanel = new JPanel(new BorderLayout());
    JLabel result = new JLabel(title);
    final JLabel lock = isLocked.getValue() ? new JLabel(ICON_LOCKED) : new JLabel(ICON_UNLOCKED);
    lock.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (lock.isEnabled()) {
          lockListener.mouseClicked(e);
        }
      }

    });
    isLocked.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (isLocked.getValue()) {
          lock.setIcon(ICON_LOCKED);
        } else {
          lock.setIcon(ICON_UNLOCKED);
        }
        UIUtil.setEnabledTree(labelPanel, !isLocked.getValue());
      }
    });
    labelPanel.add(result, BorderLayout.WEST);
    labelPanel.add(lock, BorderLayout.EAST);
    UIUtil.setEnabledTree(labelPanel, !isLocked.getValue());
    return labelPanel;
  }

  public void insertInto(JPanel propertiesPanel) {
    // Begin date
    propertiesPanel.add(createLabel(language.getText("dateOfBegining"), myStartDateLock, new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myStartDateLock.setValue(true);
        myPrevLock.setValue(false);
        myPrevLock = myStartDateLock;
      }
    }));
    myStartDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setStart(new GanttCalendar(((JXDatePicker) e.getSource()).getDate()), false);
      }
    });
    propertiesPanel.add(myStartDatePicker);

    // End date
    propertiesPanel.add(createLabel(language.getText("dateOfEnd"), myEndDateLock, new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myEndDateLock.setValue(true);
        myPrevLock.setValue(false);
        myPrevLock = myEndDateLock;
      }

    }));
    myEndDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GanttCalendar c = new GanttCalendar(((JXDatePicker) e.getSource()).getDate());
        c.add(Calendar.DATE, 1);
        setEnd(c, false);
      }
    });
    propertiesPanel.add(myEndDatePicker);

    // Duration
    propertiesPanel.add(createLabel(language.getText("length"), myDurationLock, new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myDurationLock.setValue(true);
        myPrevLock.setValue(false);
        myPrevLock = myDurationLock;
      }

    }));
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
  }

  public void setStart(GanttCalendar start, boolean test) {
    myStart = start;
    myStartDatePicker.setDate(myStart.getTime());
    if (test == true) {
      return;
    }
    if (myEndDateLock.isChecked()) {
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
    if (myStartDateLock.isChecked()) {
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

    if (myEndDateLock.isChecked()) {
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
