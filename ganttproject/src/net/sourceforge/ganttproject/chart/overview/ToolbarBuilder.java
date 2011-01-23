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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.util.TextLengthCalculatorImpl;

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
    ToolbarBuilder addComboBox(final Action[] actions, final Action selected) {
        class MyComboBox extends TestGanttRolloverButton {
            private Action mySelectedAction = null;
            private final Action[] myActions;
            private Rectangle myIconRect;
            private Dimension myPreferredSize;

            private MyComboBox(Action[] actions) {
                myActions = actions;
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        onMouseClicked(e);
                    }
                });
                setIcon(new ImageIcon(getClass().getResource("/icons/dropdown_16.png")) {
                    @Override
                    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                        super.paintIcon(c, g, x, y);
                        if (myIconRect == null) {
                            myIconRect = new Rectangle(x, y, 16, 16);
                        }
                    }
                });
                setHorizontalTextPosition(LEADING);
                setVerticalTextPosition(SwingConstants.CENTER);
                int maxLength = 0;
                for (Action a : actions) {
                    if (getActionName(a).length() > maxLength) {
                        maxLength = getActionName(a).length();
                    }
                }
                setSelectedAction(selected);
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            private void setSelectedAction(Action selected) {
                mySelectedAction = selected;
                getButton().setText(formatActionName(selected));
            }
            private String getActionName(Action a) {
                return a.getValue(Action.NAME).toString();
            }
            private String formatActionName(Action a) {
                String name = getActionName(a);
                return MessageFormat.format("<html><b>{0}</b></html>", name);
            }
            protected void onMouseClicked(MouseEvent e) {
                if (myIconRect.contains(e.getX(), e.getY())) {
                    showPopup();
                } else {
                    mySelectedAction.actionPerformed(null);
                }
            }
            private void showPopup() {
                JPopupMenu popupMenu = new JPopupMenu();
                for (final Action a : myActions) {
                    popupMenu.add(new AbstractAction(a.getValue(Action.NAME).toString()) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            a.actionPerformed(e);
                            setSelectedAction(a);
                        }
                    });
                }
                popupMenu.show(myToolbar, getButton().getLocation().x, getButton().getHeight());                
            }
            private JButton getButton() {
                return MyComboBox.this;
            }
            @Override
            public Dimension getPreferredSize() {
                if (myPreferredSize != null) {
                    return myPreferredSize;
                }
                Dimension d = super.getPreferredSize();
                Graphics g = getGraphics();
                if (g == null) {
                    return d;
                }
                int maxLength = 0;
                TextLengthCalculatorImpl textLength = new TextLengthCalculatorImpl(g);
                for (Action a : myActions) {
                    int length = textLength.getTextLength(a.getValue(Action.NAME).toString());
                    if (maxLength < length) {
                        maxLength = length;
                    }
                }
                int width = (int)(maxLength*1.1) + 16 + getIconTextGap();
                Insets insets = getInsets();
                myPreferredSize = new Dimension(width + insets.left + insets.right, d.height);
                return myPreferredSize;
        }
        }
        final MyComboBox button = new MyComboBox(actions);
        
        if (myToolbar.getComponentCount() != 0) {
            myToolbar.add(new JLabel(" | "));
        }
        myToolbar.add(button);
        return this;
    }
    
    JToolBar build() {
        return myToolbar;
    }
}
