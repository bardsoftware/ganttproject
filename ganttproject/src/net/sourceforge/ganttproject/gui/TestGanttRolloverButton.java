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

/**
 * This class is from jedit.org
 * RolloverButton.java - Class for buttons that implement rollovers
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Kris Kopicki
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import net.sourceforge.ganttproject.action.GPAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * Special button for tests on TaskPropertiesBeans
 */
public class TestGanttRolloverButton extends JButton {

  private boolean myTextHidden = false;
  private boolean isFontAwesome = false;
  private Font myBaseFont;
  private String myFontAwesomeLabel;
  private Rectangle myRect = new Rectangle();
  private float myYshift = 0f;
  private Dimension myPreferredSize = null;

  public TestGanttRolloverButton() {
    setBorderPainted(false);
    setContentAreaFilled(false);
    setMargin(new Insets(0, 0, 0, 0));

    addMouseListener(new MouseOverHandler());
    setHorizontalTextPosition(SwingConstants.CENTER);
    setVerticalTextPosition(SwingConstants.BOTTOM);
    setRolloverEnabled(true);
  }

  public TestGanttRolloverButton(Action action) {
    this();
    setAction(action);
    if (!setupFontAwesome()) {
      Icon smallIcon = (Icon) action.getValue(Action.SMALL_ICON);
      if (smallIcon != null) {
        setIcon(smallIcon);
        setTextHidden(true);
      }
    }
  }

  public TestGanttRolloverButton(Icon icon) {
    this();
    setIcon(icon);
  }

  public void setTextHidden(boolean isHidden) {
    myTextHidden = isHidden;
    if (isHidden) {
      setText("");
    } else {
      Action action = getAction();
      if (action != null) {
        setText(String.valueOf(action.getValue(Action.NAME)));
      }
    }
  };

  @Override
  public void setIcon(Icon icon) {
    if (icon != null && !isFontAwesome) {
      setRolloverIcon(icon);
    }
    super.setIcon(icon);
  }

  @Override
  public void setText(String text) {
    // Only set/update text if no icon is present
    if (myTextHidden) {
      super.setText("");
    } else {
      super.setText(text);
    }
  }

  public void setScale(float scale) {
    if (isFontAwesome) {
      Font baseFont = myBaseFont;
      Font scaledFont = baseFont.deriveFont(baseFont.getSize() * scale);
      setFont(scaledFont);
    }
  }
  private boolean setupFontAwesome() {
    Action action = getAction();
    if (action instanceof GPAction) {
      String fontawesomeLabel = ((GPAction) action).getFontawesomeLabel();
      if (fontawesomeLabel != null && UIUtil.FONTAWESOME_FONT != null) {
        isFontAwesome = true;
        action.putValue(Action.SMALL_ICON, null);
        float iconScale = UIUtil.getFontawesomeScale((GPAction) action);
        myYshift = UIUtil.getFontawesomeYShift((GPAction) action);
        myBaseFont = (iconScale == 1f)
            ? UIUtil.FONTAWESOME_FONT
            : UIUtil.FONTAWESOME_FONT.deriveFont(UIUtil.FONTAWESOME_FONT.getSize() * iconScale);

        setFont(myBaseFont);
        action.putValue(Action.NAME, null);
        myFontAwesomeLabel = fontawesomeLabel;
        setText(null);
        setTextHidden(true);
        setIcon(null);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setForeground(UIUtil.PATINA_FOREGROUND);
        setBackground(UIManager.getColor("Label.background"));
        return true;
      }
    }
    return false;
  }

  @Override
  public void paintComponent(Graphics graphics) {
    if (isFontAwesome) {
      Graphics2D g2 = (Graphics2D) graphics;
      Rectangle innerArea = SwingUtilities.calculateInnerArea(this, myRect);
      Font f = getFont();
      FontMetrics fontMetrics = g2.getFontMetrics(f);
      Rectangle2D bounds = fontMetrics.getStringBounds(myFontAwesomeLabel, graphics);
      int h = (int) bounds.getHeight();
      int w = (int) bounds.getWidth();
      setTextHidden(true);
      super.paintComponent(graphics);
      g2.setColor(isEnabled() ? UIUtil.PATINA_FOREGROUND : Color.GRAY);
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING,
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.drawString(myFontAwesomeLabel,
          innerArea.x + (innerArea.width - w)/2,
          innerArea.y + innerArea.height - (innerArea.height - h)/2 + (h * myYshift));
      Dimension buttonSize = getSize();
      if (UIUtil.isFontawesomeSizePreferred() || h > buttonSize.height || w > buttonSize.width) {
        int maxDim = 10 + Math.max(h, w);
        if (myPreferredSize == null || myPreferredSize.width != maxDim) {
          myPreferredSize = new Dimension(maxDim, maxDim);
        }
      }
    }
    else {
      super.paintComponent(graphics);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return (myPreferredSize == null) ? super.getPreferredSize() : myPreferredSize;
  }

  class MouseOverHandler extends MouseAdapter {
    @Override
    public void mouseEntered(MouseEvent e) {
      if (isEnabled()) {
        setBorderPainted(true);
        setContentAreaFilled(true);
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setBorderPainted(false);
      setContentAreaFilled(false);
    }
  }
}
