/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart.overview;

import biz.ganttproject.core.chart.render.TextLengthCalculatorImpl;
import biz.ganttproject.core.option.IntegerOption;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

/**
 * This class is a standard way of building toolbars in GanttProject.
 *
 * @author Dmitry Barashev (dbarashev@ganttproject.biz)
 */
public class ToolbarBuilder {

  public static class Gaps {

    public static Supplier<Component> VDASH = new Supplier<Component>() {
      @Override
      public Component get() {
        return new JLabel(" | ");
      }
    };
    public static Supplier<Component> RIGID = new Supplier<Component>() {
      @Override
      public Component get() {
        return Box.createRigidArea(new Dimension(3, 0));
      }
    };
  }
  private final JPanel myToolbar;
  private Border myBorder = BorderFactory.createEmptyBorder(2,2,2,2);
  private Color myBackground;
  private IntegerOption myDpiOption;
  private final java.util.List<TestGanttRolloverButton> myButtons = Lists.newArrayList();
  private Supplier<Component> myGapFactory;
  private int myButtonWidth = 48;

  public ToolbarBuilder() {
    myToolbar = new JPanel();
    myToolbar.setLayout(new BoxLayout(myToolbar, BoxLayout.LINE_AXIS));
    myToolbar.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    myBackground = myToolbar.getBackground();
  }

  public ToolbarBuilder withBackground(Color background) {
    myBackground = background;
    return this;
  }

  public ToolbarBuilder withBorder(Border border) {
    myBorder = border;
    return this;
  }

  public ToolbarBuilder withButtonWidth(int width) {
    myButtonWidth = width;
    return this;
  }
  public ToolbarBuilder withDpiOption(IntegerOption dpiOption) {
    myDpiOption = dpiOption;
    return this;
  }

  public ToolbarBuilder withGapFactory(Supplier<Component> gapFactory) {
    myGapFactory = Preconditions.checkNotNull(gapFactory);
    return this;
  }

  public ToolbarBuilder addButton(TestGanttRolloverButton button) {
    //button.setIcon(new ImageIcon(getClass().getResource("/icons/blank_big.gif")));
    button.setIcon(null);
    button.setRolloverIcon(null);
    button.setHorizontalTextPosition(SwingConstants.CENTER);
    button.setVerticalTextPosition(SwingConstants.CENTER);
    button.setTextHidden(false);
    button.setAlignmentY(Component.CENTER_ALIGNMENT);
    button.setMargin(new Insets(0, 0, 0, 0));
//    System.out.println(String.format("Width of %s is %s", button.getText(), SwingUtilities.computeStringWidth(button.getGraphics().getFontMetrics(button.getFont()), button.getText())));
    myButtons.add(button);
    addGap();
    myToolbar.add(button);
    return this;
  }

  public ToolbarBuilder addButton(Action action) {
    return addButton(new TestGanttRolloverButton(action));
  }

  public ToolbarBuilder addPanel(JPanel panel) {
    addGap();
    panel.setAlignmentY(Component.CENTER_ALIGNMENT);
    myToolbar.add(panel);
    return this;
  }

  private void addGap() {
    if (myToolbar.getComponentCount() != 0 && myGapFactory != null) {
      myToolbar.add(myGapFactory.get());
    }
  }

  public ToolbarBuilder addWhitespace() {
    float scale = myDpiOption == null ? 1.0f : myDpiOption.getValue().floatValue() / UIFacade.DEFAULT_DPI;
    int whitespaceWidth = (int)(myButtonWidth * scale / 1.62f);
    myToolbar.add(Box.createRigidArea(new Dimension(whitespaceWidth, 0)));
    return this;
  }


  public ToolbarBuilder addComboBox(final Action[] actions, final Action selected) {
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
        Graphics2D g = (Graphics2D) getGraphics();
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
        int width = (int) (maxLength * 1.1) + 16 + getIconTextGap();
        Insets insets = getInsets();
        myPreferredSize = new Dimension(width + insets.left + insets.right, d.height);
        return myPreferredSize;
      }
    }
    final MyComboBox button = new MyComboBox(actions);
    addGap();
    myToolbar.add(button);
    return this;
  }

  public GPToolbar build() {
    UIUtil.setBackgroundTree(myToolbar, myBackground);
    GPToolbar result = new GPToolbar(myToolbar, myButtons, myButtonWidth, myDpiOption);
    result.getToolbar().setBorder(myBorder);
    return result;
  }
}
