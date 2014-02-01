/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import biz.ganttproject.core.option.DateOption;
import biz.ganttproject.core.option.DefaultDateOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class DateIntervalListEditor extends JPanel {
  public static class DateInterval {
    public final Date start;
    private final Date myVisibleEnd;
    private final Date myModelEnd;

    private DateInterval(Date start, Date visibleEnd, Date modelEnd) {
      this.start = start;
      myVisibleEnd = visibleEnd;
      myModelEnd = modelEnd;
    }

    @Override
    public boolean equals(Object obj) {
      if (false == obj instanceof DateInterval) {
        return false;
      }
      DateInterval rvalue = (DateInterval) obj;
      return this.start.equals(rvalue.start) && this.getEnd().equals(rvalue.getEnd());
    }

    @Override
    public int hashCode() {
      return this.start.hashCode();
    }

    public Date getVisibleEnd() {
      return myVisibleEnd;
    }

    public Date getEnd() {
      return myModelEnd;
    }

    public static DateInterval createFromModelDates(Date start, Date end) {
      return new DateInterval(start, GPTimeUnitStack.DAY.adjustLeft(GPTimeUnitStack.DAY.jumpLeft(end)), end);
    }

    public static DateInterval createFromVisibleDates(Date start, Date end) {
      return new DateInterval(start, end, GPTimeUnitStack.DAY.adjustRight(end));
    }
  }

  public static interface DateIntervalModel {
    DateInterval[] getIntervals();

    void remove(DateInterval interval);

    void add(DateInterval interval);

    int getMaxIntervalLength();

    boolean canRemove(DateInterval interval);

    String format(DateInterval interval);
  }

  public static class DefaultDateIntervalModel implements DateIntervalModel {
    private List<DateInterval> myIntervals = new ArrayList<DateInterval>();

    @Override
    public DateInterval[] getIntervals() {
      return myIntervals.toArray(new DateInterval[myIntervals.size()]);
    }

    @Override
    public void remove(DateInterval interval) {
      myIntervals.remove(interval);
    }

    @Override
    public void add(DateInterval interval) {
      myIntervals.add(interval);
    }

    @Override
    public int getMaxIntervalLength() {
      return 1;
    }

    @Override
    public boolean canRemove(DateInterval interval) {
      return true;
    }

    @Override
    public String format(DateInterval interval) {
      StringBuffer result = new StringBuffer(GanttLanguage.getInstance().getDateFormat().format(interval.start));
      if (!interval.getEnd().equals(interval.start)) {
        result.append("...");
        result.append(GanttLanguage.getInstance().getDateFormat().format(interval.getVisibleEnd()));
      }
      return result.toString();
    }
  }

  private final DateIntervalModel myIntervalsModel;
  private final DateOption myStart;
  private final DateOption myFinish;
  private final GPAction myAddAction;
  private final GPAction myDeleteAction;

  private class MyListModel extends AbstractListModel {
    @Override
    public int getSize() {
      return myIntervalsModel.getIntervals().length;
    }

    @Override
    public Object getElementAt(int index) {
      DateInterval interval = myIntervalsModel.getIntervals()[index];
      return myIntervalsModel.format(interval);
    }

    public void update() {
      fireContentsChanged(this, 0, myIntervalsModel.getIntervals().length);
    }

  };

  private MyListModel myListModel = new MyListModel();
  private ListSelectionModel myListSelectionModel;

  public DateIntervalListEditor(final DateIntervalModel intervalsModel) {
    super(new BorderLayout());
    myIntervalsModel = intervalsModel;
    myStart = new DefaultDateOption("generic.startDate") {
      @Override
      public void setValue(Date value) {
        super.setValue(value);
        if (intervalsModel.getMaxIntervalLength() == 1) {
          DateIntervalListEditor.this.myFinish.setValue(value);
        }
        DateIntervalListEditor.this.updateActions();
      }
    };
    myFinish = new DefaultDateOption("generic.endDate") {
      @Override
      public void setValue(Date value) {
        super.setValue(value);
        DateIntervalListEditor.this.updateActions();
      }
    };
    myAddAction = new GPAction("add") {
      @Override
      public void actionPerformed(ActionEvent e) {
        myIntervalsModel.add(DateInterval.createFromVisibleDates(myStart.getValue(), myFinish.getValue()));
        myListModel.update();
      }
    };
    myDeleteAction = new GPAction("delete") {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = myListSelectionModel.getMinSelectionIndex();
        myIntervalsModel.remove(myIntervalsModel.getIntervals()[selected]);
        myListModel.update();
        myListSelectionModel.removeIndexInterval(selected, selected);
        updateActions();
      }
    };
    JPanel topPanel = new JPanel(new BorderLayout());
    OptionsPageBuilder builder = new OptionsPageBuilder();
    builder.setOptionKeyPrefix("");
    GPOptionGroup group = myIntervalsModel.getMaxIntervalLength() == 1 ? new GPOptionGroup("",
        new GPOption[] { myStart }) : new GPOptionGroup("", new GPOption[] { myStart, myFinish });
    group.setTitled(false);
    JComponent datesBox = builder.buildPlanePage(new GPOptionGroup[] { group });
    topPanel.add(datesBox, BorderLayout.CENTER);

    Box buttonBox = Box.createHorizontalBox();
    buttonBox.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
    buttonBox.add(new JButton(myAddAction));
    buttonBox.add(Box.createHorizontalStrut(5));
    buttonBox.add(new JButton(myDeleteAction));
    topPanel.add(buttonBox, BorderLayout.SOUTH);
    topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
    add(topPanel, BorderLayout.NORTH);

    JList list = new JList(myListModel);
    list.setName("list");
    list.setBorder(BorderFactory.createLoweredBevelBorder());
    myListSelectionModel = list.getSelectionModel();
    myListSelectionModel.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        updateActions();
      }
    });
    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    scrollPane.setPreferredSize(new Dimension(120, 200));
    add(scrollPane, BorderLayout.CENTER);
    updateActions();
  }

  private void updateActions() {
    if (myStart.getValue() != null && myFinish.getValue() != null
        && false == myFinish.getValue().before(myStart.getValue())) {
      myAddAction.setEnabled(true);
    } else {
      myAddAction.setEnabled(false);
    }
    if (myListSelectionModel.isSelectionEmpty()) {
      myDeleteAction.setEnabled(false);
    } else {
      int idxSelected = myListSelectionModel.getMinSelectionIndex();
      if (idxSelected >= 0 && idxSelected < myIntervalsModel.getIntervals().length) {
        myDeleteAction.setEnabled(myIntervalsModel.canRemove(myIntervalsModel.getIntervals()[idxSelected]));
      }
    }

  }
}
