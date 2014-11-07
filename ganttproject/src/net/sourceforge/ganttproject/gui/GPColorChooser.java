/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.painter.Painter;

import biz.ganttproject.core.option.ListOption;

import com.google.common.collect.Lists;

/**
 * Custom component which adds a configurable list of recently used colors to Swing standard color chooser.
 *
 * Component per se is not a singleton. However, a list of recent colors is a singleton. The reason is that this
 * component is used both in options and in properties, and is instantiated through OptionsPageBuilder instance. Passing
 * the same list of recent colors to all these places isn't easy and seems to be unnecessary, because we want this list
 * to be the same at least inside one project.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GPColorChooser {
  private static List<Color> ourRecentColors = Lists.newArrayList();
  private JColorChooser myChooserImpl;
  private List<Color> myRecentColors;
  private Color mySelectedColor;

  public GPColorChooser() {
    this(ourRecentColors);
  }

  public GPColorChooser(List<Color> recentColors) {
    myChooserImpl = new JColorChooser();
    myChooserImpl.getSelectionModel().addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent arg0) {
        mySelectedColor = myChooserImpl.getColor();
      }
    });
    myRecentColors = recentColors;
  }

  public JComponent buildComponent() {
    JPanel result = new JPanel(new BorderLayout());
    result.add(myChooserImpl, BorderLayout.CENTER);
    JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(new JLabel("Recent colors"), BorderLayout.WEST);

    Box colorLabels = Box.createHorizontalBox();
    for (final Color c : myRecentColors) {
      final JXLabel label = new JXLabel();
      label.setBackgroundPainter(new Painter<JXLabel>() {
        @Override
        public void paint(Graphics2D g, JXLabel object, int width, int height) {
          g.setColor(c);
          g.fillRect(4, 4, width-8, height-8);
        }
      });
      label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      label.setFocusable(true);
      label.setPreferredSize(new Dimension(20, 20));
      label.setMaximumSize(new Dimension(20, 20));

      final Border outsideFocusBorder = BorderFactory.createLineBorder(c.darker(), 2);
      final Border outsideNoFocusBorder = BorderFactory.createEmptyBorder(2,2,2,2);
      label.setBorder(outsideNoFocusBorder);
      label.addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          label.setBorder(outsideFocusBorder);
          myChooserImpl.setColor(c);
          mySelectedColor = c;
        }
        @Override
        public void focusLost(FocusEvent e) {
          label.setBorder(outsideNoFocusBorder);
        }
      });
      label.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          label.requestFocus();
        }
      });
      colorLabels.add(label);
      colorLabels.add(Box.createHorizontalStrut(5));
    }
    colorLabels.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0));
    southPanel.add(colorLabels, BorderLayout.CENTER);
    southPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
    result.add(southPanel, BorderLayout.SOUTH);
    return result;
  }

  public Color getColor() {
    return mySelectedColor;
  }

  public void setColor(Color color) {
    myChooserImpl.setColor(color);
  }

  public static void setRecentColors(List<Color> colors) {
    ourRecentColors = colors;
  }

  public static List<Color> getRecentColors() {
    return ourRecentColors;
  }

  public static ListOption<Color> getRecentColorsOption() {
    return new RecentColorsOption(ourRecentColors);
  }
}
