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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
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
import com.google.common.base.Predicate;
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

  public static void setEnabledTree(JComponent root, final boolean isEnabled) {
    walkComponentTree(root, new Predicate<JComponent>() {
      @Override
      public boolean apply(JComponent input) {
        input.setEnabled(isEnabled);
        return true;
      }
    });
  }

  public static void setBackgroundTree(JComponent root, final Color background) {
    walkComponentTree(root, new Predicate<JComponent>() {
      @Override
      public boolean apply(JComponent input) {
        input.setBackground(background);
        return true;
      }
    });
  }

  public static void walkComponentTree(JComponent root, Predicate<JComponent> visitor) {
    if (visitor.apply(root)) {
      Component[] components = root.getComponents();
      for (int i = 0; i < components.length; i++) {
        if (components[i] instanceof JComponent) {
          walkComponentTree((JComponent) components[i], visitor);
        }
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
  public static JXDatePicker createDatePicker(final ActionListener listener) {
    final JXDatePicker result = new JXDatePicker();
    result.setLocale(GanttLanguage.getInstance().getDateFormatLocale());
    result.addActionListener(listener);

    result.getEditor().addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        try {
          if (result.getEditor().getValue() != null) {
            result.commitEdit();
            listener.actionPerformed(new ActionEvent(result, ActionEvent.ACTION_PERFORMED, ""));
          }
        } catch (ParseException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }
    });
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

  public static JComponent createTopAndCenter(JComponent top, JComponent center) {
    JPanel result = new JPanel(new BorderLayout());
    top.setAlignmentX(Component.LEFT_ALIGNMENT);
    result.add(top, BorderLayout.NORTH);

    JPanel planePageWrapper = new JPanel(new BorderLayout());
    planePageWrapper.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    center.setAlignmentX(Component.LEFT_ALIGNMENT);
    planePageWrapper.add(center, BorderLayout.NORTH);
    result.add(planePageWrapper, BorderLayout.CENTER);
    return result;

  }

  public static JMenu createTooltiplessJMenu(Action action) {
    JMenu result = new JMenu(action) {
      @Override
      public JMenuItem add(Action a) {
        JMenuItem result = super.add(a);
        result.setToolTipText(null);
        return result;
      }
    };
    result.setToolTipText(null);
    return result;
  }
}
