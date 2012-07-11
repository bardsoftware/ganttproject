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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.table.TableColumnExt;

import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class UIUtil {
  public static final Highlighter ZEBRA_HIGHLIGHTER = new ColorHighlighter(new HighlightPredicate() {
    @Override
    public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
      return adapter.row % 2 == 1;
    }
  }, new Color(0xf0, 0xf0, 0xe0), null);

  public static final Color ERROR_BACKGROUND = new Color(255, 191, 207);

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

  public static Dimension autoFitColumnWidth(JTable table, TableColumn tableColumn) {
    final int margin = 5;

    Dimension headerFit = getHeaderDimension(table, tableColumn);
    int width = headerFit.width;
    int height = 0;

    int order = table.convertColumnIndexToView(tableColumn.getModelIndex());
    // Get maximum width of column data
    for (int r = 0; r < table.getRowCount(); r++) {
      TableCellRenderer renderer = table.getCellRenderer(r, order);
      Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, order), false,
          false, r, order);
      width = Math.max(width, comp.getPreferredSize().width);
      height += comp.getPreferredSize().height;
    }
    // Add margin
    width += 2 * margin;
    // Set the width
    return new Dimension(width, height);
  }

  public static Dimension getHeaderDimension(JTable table, TableColumn tableColumn) {
    TableCellRenderer renderer = tableColumn.getHeaderRenderer();
    if (renderer == null) {
      renderer = table.getTableHeader().getDefaultRenderer();
    }
    Component comp = renderer.getTableCellRendererComponent(table, tableColumn.getHeaderValue(), false, false, 0, 0);
    return comp.getPreferredSize();
  }

  public static JComponent createButtonBar(JButton[] leftButtons, JButton[] rightButtons) {
    Box leftBox = Box.createHorizontalBox();
    for (JButton button : leftButtons) {
      leftBox.add(button);
      leftBox.add(Box.createHorizontalStrut(3));
    }

    Box rightBox = Box.createHorizontalBox();
    for (JButton button : Lists.reverse(Arrays.asList(rightButtons))) {
      rightBox.add(Box.createHorizontalStrut(3));
      rightBox.add(button);
    }

    JPanel result = new JPanel(new BorderLayout());
    result.add(leftBox, BorderLayout.WEST);
    result.add(rightBox, BorderLayout.EAST);
    return result;
  }
}
