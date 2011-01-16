/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart.overview;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListDataListener;

import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;

class ToolbarBuilder {
    private final JToolBar myToolbar;
    private final TimelineChart myChart;

    ToolbarBuilder(TimelineChart chart) {
        myChart = chart;
        myToolbar = new JToolBar();
        myToolbar.setBackground(myChart.getStyle().getSpanningHeaderBackgroundColor());
        myToolbar.setFloatable(false);
        myToolbar.setBorderPainted(false);
        myToolbar.setRollover(true);
    }
    
    ToolbarBuilder addButton(Action action) {
        if (myToolbar.getComponentCount() != 0) {
            myToolbar.add(new JLabel(" | "));
        }
        final JButton button = new TestGanttRolloverButton(action);
        button.setIcon(new ImageIcon(getClass().getResource("/icons/blank_big.gif")));
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setBackground(myChart.getStyle().getSpanningHeaderBackgroundColor());
        myToolbar.add(button);
        return this;
    }
    ToolbarBuilder addComboBox(final Action[] actions) {
        class ComboBoxModelImpl extends AbstractListModel implements ComboBoxModel {
            private Object mySelectedItem;
            @Override
            public Object getElementAt(int idx) {
                return actions[idx].getValue(Action.NAME);
            }
            @Override
            public int getSize() {
                return actions.length;
            }
            @Override
            public Object getSelectedItem() {
                return mySelectedItem;
            }
            @Override
            public void setSelectedItem(Object item) {
                mySelectedItem = item;
                for (Action a : actions) {
                    if (a.getValue(Action.NAME).equals(item)) {
                        a.actionPerformed(null);
                        break;
                    }
                }
            }
        }
        final JButton button = new TestGanttRolloverButton();
        button.setAction(new AbstractAction("foooobar") {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                JPopupMenu popupMenu = new JPopupMenu();
                for (Action a : actions) {
                    popupMenu.add(a);
                }
                popupMenu.show(myToolbar, button.getLocation().x, button.getLocation().y+20);
            }
        });
//        JComboBox result = new JComboBox(new ComboBoxModelImpl());
//        final JButton buttonComponent = (JButton) result.getComponent(0);
//        buttonComponent.setRolloverEnabled(true);
//        buttonComponent.setBorderPainted(false);
//        result.getComponent(0).addMouseListener(new MouseAdapter() {
//            public void mouseEntered(MouseEvent e) {
//                buttonComponent.setBorderPainted(true);
//            }
//    
//            public void mouseExited(MouseEvent e) {
//                buttonComponent.setBorderPainted(false);
//            }
//        });
//        
        myToolbar.add(button);
        return this;
    }
    
    JToolBar build() {
        return myToolbar;
    }
}
