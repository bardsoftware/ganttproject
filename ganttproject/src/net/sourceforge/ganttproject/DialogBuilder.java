/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.DialogAligner;
import net.sourceforge.ganttproject.gui.UIFacade.Centering;
import net.sourceforge.ganttproject.gui.UIFacade.Dialog;

/**
 * Builds standard dialog windows in GanttProject
 *
 * @author dbarashev (Dmitry Barashev)
 */
class DialogBuilder {
  private static class Commiter {
    private boolean isCommited;

    void commit() {
      isCommited = true;
    }

    boolean isCommited() {
      return isCommited;
    }
  }
  private final JFrame myMainFrame;

  DialogBuilder(JFrame mainFrame) {
    myMainFrame = mainFrame;
  }

  /**
   * Creates a dialog given its {@code title}, {@code content} component and an array
   * of actions which are represented as buttons in the bottom of the dialog. Actions
   * which extend {@link OkAction} or {@link CancelAction} will automatically close
   * dialog when invoked.
   *
   * @param content dialog content component
   * @param buttonActions actions for the button row
   * @param title dialog title
   * @return dialog object
   */
  Dialog createDialog(Component content, Action[] buttonActions, String title) {
    final JDialog dlg = new JDialog(myMainFrame, true);
    final Dialog result = new Dialog() {
      @Override
      public void hide() {
        if (dlg.isVisible()) {
          dlg.setVisible(false);
          dlg.dispose();
        }
      }

      @Override
      public void show() {
        center(Centering.WINDOW);
        dlg.setVisible(true);
      }

      @Override
      public void layout() {
        dlg.pack();
      }

      @Override
      public void center(Centering centering) {
        DialogAligner.center(dlg, myMainFrame, centering);
      }
    };
    dlg.setTitle(title);
    final Commiter commiter = new Commiter();
    Action cancelAction = null;
    JPanel buttonBox = new JPanel(new GridLayout(1, buttonActions.length, 5, 0));
    for (final Action nextAction : buttonActions) {
      JButton nextButton = null;
      if (nextAction instanceof OkAction) {
        nextButton = new JButton(nextAction);
        nextButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            result.hide();
            commiter.commit();
          }
        });
        if (((OkAction)nextAction).isDefault()) {
          dlg.getRootPane().setDefaultButton(nextButton);
        }
      }
      if (nextAction instanceof CancelAction) {
        cancelAction = nextAction;
        nextButton = new JButton(nextAction);
        nextButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            result.hide();
            commiter.commit();
          }
        });
        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), nextAction.getValue(Action.NAME));
        dlg.getRootPane().getActionMap().put(nextAction.getValue(Action.NAME), new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            nextAction.actionPerformed(e);
            result.hide();
          }
        });
      }
      if (nextButton == null) {
        nextButton = new JButton(nextAction);
      }
      buttonBox.add(nextButton);
      KeyStroke accelerator = (KeyStroke) nextAction.getValue(Action.ACCELERATOR_KEY);
      if (accelerator != null) {
        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accelerator, nextAction);
        dlg.getRootPane().getActionMap().put(nextAction, nextAction);
      }
    }
    dlg.getContentPane().setLayout(new BorderLayout());
    dlg.getContentPane().add(content, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
    buttonPanel.add(buttonBox, BorderLayout.EAST);
    dlg.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

    dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    final Action localCancelAction = cancelAction;
    dlg.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        if (localCancelAction != null && !commiter.isCommited()) {
          localCancelAction.actionPerformed(null);
        }
      }
    });
    dlg.pack();
    return result;
  }
}
