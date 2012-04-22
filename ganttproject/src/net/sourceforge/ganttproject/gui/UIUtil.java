/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class UIUtil {
  static {
    ImageIcon calendarImage = new ImageIcon(UIUtil.class.getResource("/icons/calendar_16.gif"));
    ImageIcon nextMonth = new ImageIcon(UIUtil.class.getResource("/icons/nextmonth.gif"));
    ImageIcon prevMonth = new ImageIcon(UIUtil.class.getResource("/icons/prevmonth.gif"));
    UIManager.put("JXDatePicker.arrowDown.image", calendarImage);
    UIManager.put("JXMonthView.monthUp.image", prevMonth);
    UIManager.put("JXMonthView.monthDown.image", nextMonth);
    UIManager.put("JXMonthView.monthCurrent.image", calendarImage);
  }

  public static void repackWindow(JComponent component) {
    Window windowAncestor = SwingUtilities.getWindowAncestor(component);
    if (windowAncestor != null) {
      windowAncestor.invalidate();
      windowAncestor.pack();
      windowAncestor.repaint();
    }
    DialogAligner.centerOnScreen(windowAncestor);
  }

  public static void setEnabledTree(JComponent root, boolean isEnabled) {
    root.setEnabled(isEnabled);
    Component[] components = root.getComponents();
    for (int i = 0; i < components.length; i++) {
      if (components[i] instanceof JComponent) {
        setEnabledTree((JComponent) components[i], isEnabled);
      }
    }
  }

  public static void setBackgroundTree(JComponent root, Color background) {
    root.setBackground(background);
    Component[] components = root.getComponents();
    for (int i = 0; i < components.length; i++) {
      if (components[i] instanceof JComponent) {
        setBackgroundTree((JComponent) components[i], background);
      }
    }
  }

  public static void createTitle(JComponent component, String title) {
    Border lineBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK);
    component.setBorder(BorderFactory.createTitledBorder(lineBorder, title));
  }

  public static void registerActions(JComponent component, boolean recursive, GPAction... actions) {
    for (GPAction action : actions) {
      for (KeyStroke ks : GPAction.getAllKeyStrokes(action.getID())) {
        pushAction(component, recursive, ks, action);
      }
    }
  }

  public static void pushAction(JComponent root, boolean recursive, KeyStroke keyStroke, GPAction action) {
    root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, action.getID());
    root.getActionMap().put(action.getID(), action);
    for (Component child : root.getComponents()) {
      if (child instanceof JComponent) {
        pushAction((JComponent) child, recursive, keyStroke, action);
      }
    }
  }

  public static void setupTableUI(JTable table, int visibleRows) {
    table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredScrollableViewportSize().width,
        table.getRowHeight() * visibleRows));
    Font font = table.getFont();
    table.setRowHeight(table.getFontMetrics(font).getHeight() + 5);
  }

  public static void setupHighlighters(JXTable table) {
    table.setHighlighters(HighlighterFactory.createAlternateStriping(Color.WHITE, Color.ORANGE.brighter()));

  }

  public static void setupTableUI(JTable table) {
    setupTableUI(table, 10);
  }

  /**
   * @return a {@link JXDatePicker} component with the default locale, images
   *         and date formats.
   */
  public static JXDatePicker createDatePicker(ActionListener listener) {
    JXDatePicker result = new JXDatePicker();
    result.setLocale(GanttLanguage.getInstance().getDateFormatLocale());
    result.addActionListener(listener);
    result.setFormats(GanttLanguage.getInstance().getLongDateFormat(), GanttLanguage.getInstance().getShortDateFormat());
    return result;
  }

}
