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

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import com.google.common.collect.ImmutableList;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.UIUtil.DateValidator;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
  private static final BooleanOption ourStartDateLock = new DefaultBooleanOption("", false);
  private static final BooleanOption ourEndDateLock = new DefaultBooleanOption("", true);
  private static final BooleanOption ourDurationLock = new DefaultBooleanOption("", false);
  private static BooleanOption ourPrevLock = ourEndDateLock;
  private final UIFacade myUiFacade;
  private JXHyperlink myLockHyperlink;
  private boolean isMilestone;
  private JLabel myLockLabel;

  public TaskScheduleDatesPanel(UIFacade uiFacade) {
    myUiFacade = uiFacade;
  }

  public void setUnpluggedClone(Task unpluggedClone) {
    myUnpluggedClone = unpluggedClone;
    isMilestone = unpluggedClone.isMilestone();
    DateValidator validator = new UIUtil.DateValidator() {
      @Override
      public Pair<Boolean, String> apply(Date value) {
        return DateValidator.Default.aroundProjectStart(myUnpluggedClone.getManager().getProjectStart()).apply(value);
      }
    };
    UIUtil.setupDatePicker(myStartDatePicker, myUnpluggedClone.getStart().getTime(), validator, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Date date = ((JXDatePicker) e.getSource()).getDate();
        if (date != null) {
          setStart(CalendarFactory.createGanttCalendar(date), true);
        }
      }
    });
    UIUtil.setupDatePicker(myEndDatePicker, myUnpluggedClone.getEnd().getTime(), validator, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GanttCalendar c = CalendarFactory.createGanttCalendar(((JXDatePicker) e.getSource()).getDate());
        c.add(Calendar.DATE, 1);
        setEnd(c, true);
      }
    });
    setStart(myUnpluggedClone.getStart(), false);
    setEnd(myUnpluggedClone.getEnd(), false);
    adjustLength();
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
    ourPrevLock.setValue(false);
    ourPrevLock = newLock;
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

  protected void showPopup(List<Action> actions, JComponent owner, JComponent anchor) {
    myUiFacade.showPopupMenu(owner, actions, anchor.getLocation().x, anchor.getLocation().y);
  }

  public void insertInto(final JPanel propertiesPanel) {
    // Begin date
    myStartDatePicker = UIUtil.createDatePicker();
    final GPAction startDateLockAction = createLockAction("option.taskProperties.main.scheduling.manual.value.start", ourStartDateLock);
    JComponent startDateLabel = createLabel(language.getText("dateOfBegining"), ourStartDateLock, myStartDatePicker, startDateLockAction);


    // End date
    myEndDatePicker = UIUtil.createDatePicker();
    final GPAction endDateLockAction = createLockAction("option.taskProperties.main.scheduling.manual.value.end", ourEndDateLock);
    JComponent endDateLabel = createLabel(language.getText("dateOfEnd"), ourEndDateLock, myEndDatePicker, endDateLockAction);

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
    final GPAction durationLockAction = createLockAction("option.taskProperties.main.scheduling.manual.value.duration", ourDurationLock);
    JComponent durationLabel = createLabel(language.getText("length"), ourDurationLock, durationField1, durationLockAction);

    myLockLabel = new JLabel(language.getText("option.taskProperties.main.scheduling.label"));
    propertiesPanel.add(myLockLabel);
    final Box box = Box.createHorizontalBox();
    myLockHyperlink = new JXHyperlink(new GPAction("option.taskProperties.main.scheduling.manual.label") {
      @Override
      public void actionPerformed(ActionEvent e) {
        showPopup(ImmutableList.<Action>of(startDateLockAction, endDateLockAction, durationLockAction), box, myLockHyperlink);
      }
    });
    box.add(myLockHyperlink);
//    box.add(Box.createHorizontalStrut(15));
//    mySchedulingHyperlink = new JXHyperlink(new GPAction("option.taskProperties.main.scheduling.automated.label") {
//      @Override
//      public void actionPerformed(ActionEvent e) {
//        showPopup(createSchedulingMenu(), box, mySchedulingHyperlink);
//      }
//    });
//    box.add(mySchedulingHyperlink);
    propertiesPanel.add(box);

    propertiesPanel.add(startDateLabel);
    propertiesPanel.add(myStartDatePicker);
    propertiesPanel.add(endDateLabel);
    propertiesPanel.add(myEndDatePicker);
    propertiesPanel.add(durationLabel);
    propertiesPanel.add(durationField1);
  }

  private GPCalendarCalc getCalendar() {
    return myUnpluggedClone.getManager().getCalendar();
  }

  private void setStart(GanttCalendar start, boolean recalculateEnd) {
    myStart = start;
    myStartDatePicker.setDate(myStart.getTime());
    if (!recalculateEnd) {
      return;
    }
    if (!isMilestone && ourDurationLock.isChecked()) {
      adjustLength();
    } else {
      GanttCalendar endDate = isMilestone ? myStart : CalendarFactory.createGanttCalendar(
          getCalendar().shiftDate(myStart.getTime(), myUnpluggedClone.getDuration()));
      setEnd(endDate, false);
    }
  }

  private void setEnd(GanttCalendar end, boolean recalculateStart) {
    myEnd = end;
    myEndDatePicker.setDate((isMilestone ? end : end.getDisplayValue()).getTime());
    if (!recalculateStart) {
      return;
    }
    if (!isMilestone && ourDurationLock.isChecked()) {
      adjustLength();
    } else {
      setStart(CalendarFactory.createGanttCalendar(
          getCalendar().shiftDate(myEnd.getTime(), myUnpluggedClone.getDuration().reverse())), false);
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
    myUnpluggedClone.setDuration(myUnpluggedClone.getManager().createLength(length));
    if (ourStartDateLock.isChecked()) {
      // Calculate the start date for the given length
      setStart(CalendarFactory.createGanttCalendar(
          getCalendar().shiftDate(myEnd.getTime(), myUnpluggedClone.getDuration().reverse())), false);
    } else {
      // Calculate the end date for the given length
      setEnd(CalendarFactory.createGanttCalendar(
          getCalendar().shiftDate(myStart.getTime(), myUnpluggedClone.getDuration())), false);
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

  public void setupFields(boolean isMilestone, boolean isSupertask) {
    if (isMilestone) {
      ourStartDateLock.setValue(false);
      ourDurationLock.setValue(true);
      ourEndDateLock.setValue(true);
      myLockHyperlink.setEnabled(false);
      myLockLabel.setEnabled(false);
    } else {
      if (isSupertask) {
        ourStartDateLock.setValue(true);
        ourDurationLock.setValue(true);
        ourEndDateLock.setValue(true);
        myLockHyperlink.setEnabled(false);
        myLockLabel.setEnabled(false);
      } else {
        ourStartDateLock.setValue(false);
        ourDurationLock.setValue(false);
        ourEndDateLock.setValue(false);
        myLockHyperlink.setEnabled(true);
        myLockLabel.setEnabled(true);
        ourPrevLock.setValue(true);
      }
    }
    this.isMilestone = isMilestone;
  }
}
