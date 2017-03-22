/*
Copyright (C) 2017 Dmitry Barashev, BarD Software s.r.o.

This file is part of GanttProject, an open-source project management tool.

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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author dbarashev@bardsoftware.com (Dmitry Barashev)
 */
public class ActionUtil {
  /**
   * Sets up repetitive action invocation on the given button until mouse is released.
   *
   * @param button button to set up
   * @param intervalMs initial repeat delay. Susbequent delays will be twice smaller than the initial.
   */
  public static void setupAutoRepeat(final JButton button, final int intervalMs) {
    class MouseListenerImpl extends MouseAdapter implements ActionListener {
      private Timer myTimer;
      private MouseEvent myEvent;

      @Override
      public void mousePressed(MouseEvent e) {
        if (myTimer == null) {
          myEvent = e;
          myTimer = new Timer(intervalMs, this);
          myTimer.setInitialDelay(intervalMs);
          myTimer.setDelay(intervalMs / 2);
          myTimer.setRepeats(true);
          myTimer.start();
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (myTimer != null) {
          myTimer.stop();
          myTimer = null;
          myEvent = null;
        }
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, button.getActionCommand(),
            EventQueue.getMostRecentEventTime(), myEvent.getModifiers());
        button.getAction().actionPerformed(event);
      }
    }
    button.addMouseListener(new MouseListenerImpl());
  }
}
