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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXHyperlink;

import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;

import com.google.common.collect.ImmutableList;

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
  private final UIFacade myUiFacade;
  private JXHyperlink myLockHyperlink;
  private JXHyperlink mySchedulingHyperlink;

  public TaskScheduleDatesPanel(UIFacade uiFacade) {
    myUiFacade = uiFacade;
  }

  public void setUnpluggedClone(Task unpluggedClone) {
    myUnpluggedClone = unpluggedClone;
  }

  private static JComponent createLabel(String title, final BooleanOption isLocked,
      final JComponent controlledComponent, final GPAction lockAction) {
    final JPanel labelPanel = new JPanel(new BorderLayout());
    JLabel result = new JLabel(title);
    isLocked.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        UIUtil.setEnabledTree(labelPanel, !isLocked.getValue());
        UIUtil.setEnabledTree(controlledComponent, !isLocked.getValue());
        lockAction.setEnabled(!isLocked.getValue());
        lockAction.putValue(Action.SELECTED_KEY, isLocked.getValue());
      }
    });
    labelPanel.add(result, BorderLayout.WEST);
    UIUtil.setEnabledTree(labelPanel, !isLocked.getValue());
    UIUtil.setEnabledTree(controlledComponent, !isLocked.getValue());
    lockAction.setEnabled(!isLocked.getValue());
    return labelPanel;
  }

  private void swapLocks(BooleanOption newLock) {
    newLock.setValue(true);
    myPrevLock.setValue(false);
    myPrevLock = newLock;
  }
  private GPAction createLockAction(String key, final BooleanOption lock) {
    return new GPAction(key) {
      {
        putValue(Action.SELECTED_KEY, lock.isChecked());
      }
      @Override
      public void actionPerformed(ActionEvent e) {
        swapLocks(lock);
      }
    };
  }

  private static Action createDisabledAction(String key) {
    return new GPAction(key) {
      {
        setEnabled(false);
      }
      @Override
      public void actionPerformed(ActionEvent e) {
      }
    };
  }
  protected List<Action> createSchedulingMenu() {
    return Arrays.asList(new Action[] {
        createDisabledAction("option.taskProperties.main.scheduling.automated.value.start"),
        createDisabledAction("option.taskProperties.main.scheduling.automated.value.duration"),
        null,
        createDisabledAction("option.taskProperties.main.scheduling.automated.value.hint")
    });
  }

  protected void showPopup(List<Action> actions, JComponent owner, JComponent anchor) {
    myUiFacade.showPopupMenu(owner, actions, anchor.getLocation().x, anchor.getLocation().y);
  }

  public void insertInto(final JPanel propertiesPanel) {
    // Begin date
    myStartDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setStart(CalendarFactory.createGanttCalendar(((JXDatePicker) e.getSource()).getDate()), false);
      }
    });
    final GPAction startDateLockAction = createLockAction("option.taskProperties.main.scheduling.manual.value.start", myStartDateLock);
    JComponent startDateLabel = createLabel(language.getText("dateOfBegining"), myStartDateLock, myStartDatePicker, startDateLockAction);


    // End date
    myEndDatePicker = UIUtil.createDatePicker(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GanttCalendar c = CalendarFactory.createGanttCalendar(((JXDatePicker) e.getSource()).getDate());
        c.add(Calendar.DATE, 1);
        setEnd(c, false);
      }
    });
    final GPAction endDateLockAction = createLockAction("option.taskProperties.main.scheduling.manual.value.end", myEndDateLock);
    JComponent endDateLabel = createLabel(language.getText("dateOfEnd"), myEndDateLock, myEndDatePicker, endDateLockAction);

    // Duration
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
    final GPAction durationLockAction = createLockAction("option.taskProperties.main.scheduling.manual.value.duration", myDurationLock);
    JComponent durationLabel = createLabel(language.getText("length"), myDurationLock, durationField1, durationLockAction);

    propertiesPanel.add(new JLabel(language.getText("option.taskProperties.main.scheduling.label")));
    final Box box = Box.createHorizontalBox();
    myLockHyperlink = new JXHyperlink(new GPAction("option.taskProperties.main.scheduling.manual.label") {
      @Override
      public void actionPerformed(ActionEvent e) {
        showPopup(ImmutableList.<Action>of(startDateLockAction, endDateLockAction, durationLockAction), box, myLockHyperlink);
      }
    });
    box.add(myLockHyperlink);
    box.add(Box.createHorizontalStrut(15));
    mySchedulingHyperlink = new JXHyperlink(new GPAction("option.taskProperties.main.scheduling.automated.label") {
      @Override
      public void actionPerformed(ActionEvent e) {
        showPopup(createSchedulingMenu(), box, mySchedulingHyperlink);
      }
    });
    box.add(mySchedulingHyperlink);
    propertiesPanel.add(box);

    propertiesPanel.add(startDateLabel);
    propertiesPanel.add(myStartDatePicker);
    propertiesPanel.add(endDateLabel);
    propertiesPanel.add(myEndDatePicker);
    propertiesPanel.add(durationLabel);
    propertiesPanel.add(durationField1);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        swapLocks(myEndDateLock);
      }
    });
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
    if (!enable) {
      myStartDateLock.setValue(false);
      myDurationLock.setValue(true);
      myEndDateLock.setValue(true);
      myLockHyperlink.setEnabled(false);
    } else {
      myDurationLock.setValue(false);
      myEndDateLock.setValue(false);
      myLockHyperlink.setEnabled(true);
      myPrevLock.setValue(true);
    }
  }
}
