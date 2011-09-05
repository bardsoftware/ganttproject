/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2005-2011 GanttProject Team

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

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class DateIntervalListEditor extends JPanel{
    public static class DateInterval {
        public final Date start;
        public final Date end;
        public DateInterval(Date start, Date end) {
            this.start = start;
            this.end = end;
        }
        @Override
        public boolean equals(Object obj) {
            if (false==obj instanceof DateInterval) {
                return false;
            }
            DateInterval rvalue = (DateInterval) obj;
            return this.start.equals(rvalue.start) && this.end.equals(rvalue.end);
        }
        @Override
        public int hashCode() {
            return this.start.hashCode();
        }

    }
    public static interface DateIntervalModel {
        DateInterval[] getIntervals();
        void remove(DateInterval interval);
        void add(DateInterval interval);
        int getMaxIntervalLength();
    }

    public static class DefaultDateIntervalModel implements DateIntervalModel {
        List/*<DatInterval>*/<DateInterval> myIntervals = new ArrayList<DateInterval>();
        public DateInterval[] getIntervals() {
            return myIntervals.toArray(new DateInterval[myIntervals.size()]);
        }
        public void remove(DateInterval interval) {
            myIntervals.remove(interval);
        }
        public void add(DateInterval interval) {
            myIntervals.add(interval);
        }
        public int getMaxIntervalLength() {
            return 1;
        }

    }
    private final DateIntervalModel myIntervalsModel;
    private final DateOption myStart;
    private final DateOption myFinish;
    private final GPAction myAddAction;
    private final GPAction myDeleteAction;

    private class MyListModel extends AbstractListModel {
        public int getSize() {
            return myIntervalsModel.getIntervals().length;
        }

        public Object getElementAt(int index) {
            DateInterval interval = myIntervalsModel.getIntervals()[index];
            StringBuffer result = new StringBuffer(GanttLanguage.getInstance().getDateFormat().format(interval.start));
            if (!interval.end.equals(interval.start)) {
                result.append("...");
                result.append(GanttLanguage.getInstance().getDateFormat().format(interval.end));
            }
            return result.toString();
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
                commit();
                if (intervalsModel.getMaxIntervalLength()==1) {
                    DateIntervalListEditor.this.myFinish.setValue(value);
                }
                DateIntervalListEditor.this.updateActions();
                lock();
            }
        };
        myFinish = new DefaultDateOption("generic.endDate") {
            @Override
            public void setValue(Date value) {
                super.setValue(value);
                commit();
                DateIntervalListEditor.this.updateActions();
                lock();
            }
        };
        myStart.lock();
        myFinish.lock();
        myAddAction = new GPAction("add") {
            @Override
            public void actionPerformed(ActionEvent e) {
                myIntervalsModel.add(new DateInterval(myStart.getValue(), myFinish.getValue()));
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
        if (myStart.getValue()!=null && myFinish.getValue()!=null && false==myFinish.getValue().before(myStart.getValue())) {
            myAddAction.setEnabled(true);
        }
        else {
            myAddAction.setEnabled(false);
        }
        myDeleteAction.setEnabled(false==myListSelectionModel.isSelectionEmpty());
    }
}
