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

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.DialogAligner;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade.Centering;
import net.sourceforge.ganttproject.gui.UIFacade.Dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Builds standard dialog windows in GanttProject
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class DialogBuilder {
  private static class Commiter {
    private boolean isCommited;

    void commit() {
      isCommited = true;
    }

    boolean isCommited() {
      return isCommited;
    }
  }

  private static class DialogImpl implements Dialog {
    /** Original animation view, used to set it back when the dialog is closed again */
    private final JDialog myDlg;
    private final JFrame myMainFrame;
    private boolean isEscCloseEnabled = true;

    DialogImpl(JDialog dlg, JFrame mainFrame) {
      myDlg = dlg;
      myMainFrame = mainFrame;
    }
    @Override
    public void hide() {
      if (myDlg.isVisible()) {
        myDlg.setVisible(false);
        myDlg.dispose();
      }
    }

    @Override
    public void show() {
      myDlg.pack();
      center(Centering.WINDOW);
      myDlg.setVisible(true);
    }

    @Override
    public void onShown(Runnable onShown) {
      myDlg.addWindowListener(new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
          onShown.run();
        }
      });
    }

    @Override
    public void onClosed(Runnable onClosed) {
      myDlg.addWindowListener(new WindowAdapter() {

        @Override
        public void windowClosed(WindowEvent e) {
          onClosed.run();
        }
      });
    }

    @Override
    public void layout() {
      myDlg.validate();
    }

    @Override
    public void center(Centering centering) {
      DialogAligner.center(myDlg, myMainFrame, centering);
    }

    @Override
    public boolean isEscCloseEnabled() {
      return isEscCloseEnabled;
    }

    @Override
    public void setEscCloseEnabled(boolean value) {
      isEscCloseEnabled = value;
    }
  }
  private final JFrame myMainFrame;

  public DialogBuilder(JFrame mainFrame) {
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
  public Dialog createDialog(Component content, Action[] buttonActions, String title) {
    final JDialog dlg = new JDialog(myMainFrame, true);
    final DialogImpl result = new DialogImpl(dlg, myMainFrame);
    dlg.setTitle(title);
    dlg.getContentPane().setLayout(new BorderLayout());
    dlg.getContentPane().add(content, BorderLayout.CENTER);

    final Commiter commiter = new Commiter();
    Action cancelAction = null;
    int buttonCount = 0;
    if (buttonActions.length > 0) {
      JPanel buttonBox = new JPanel(new GridLayout(1, buttonActions.length, 5, 0));
      for (final Action nextAction : buttonActions) {
        JButton nextButton = null;
        if (nextAction instanceof OkAction) {
          final JButton _btn = new JButton();
          final AbstractAction _delegate = (AbstractAction) nextAction;
          OkAction proxy = new OkAction() {
            // These two steps handel the case when focus is somewhere in text input
            // and user hits Ctrl+Enter
            // First we want to move focus to OK button to allow focus listeners, if any,
            // to catch focusLost event
            // Second, we want it to happen before original OkAction runs
            // So we wrap original OkAction into proxy which moves focus and schedules "later" command
            // which call the original action. Between them EDT sends out focusLost events.
            final Runnable myStep2 = new Runnable() {
              @Override
              public void run() {
                result.hide();
                commiter.commit();
                nextAction.actionPerformed(null);
                _delegate.removePropertyChangeListener(myDelegateListener);
              }
            };
            final Runnable myStep1 = new Runnable() {
              @Override
              public void run() {
                _btn.requestFocus();
                SwingUtilities.invokeLater(myStep2);
              }
            };

            @Override
            public void actionPerformed(final ActionEvent e) {
              SwingUtilities.invokeLater(myStep1);
            }

            private void copyValues() {
              for (Object key : _delegate.getKeys()) {
                putValue(key.toString(), _delegate.getValue(key.toString()));
              }
              setEnabled(_delegate.isEnabled());
            }

            private PropertyChangeListener myDelegateListener = new PropertyChangeListener() {
              @Override
              public void propertyChange(PropertyChangeEvent evt) {
                copyValues();
              }
            };

            {
              _delegate.addPropertyChangeListener(myDelegateListener);
              copyValues();
            }
          };
          _btn.setAction(proxy);
          nextButton = _btn;

          if (((OkAction) nextAction).isDefault()) {
            dlg.getRootPane().setDefaultButton(nextButton);
          }

        }
        else if (nextAction instanceof CancelAction) {
          cancelAction = nextAction;
          Boolean hasButton = (Boolean) nextAction.getValue(GPAction.HAS_DIALOG_BUTTON);
          if (hasButton == null) {
            hasButton = true;
          }
          if (hasButton) {
            nextButton = new JButton(nextAction);
            nextButton.addActionListener(e -> {
              result.hide();
              commiter.commit();
            });
          }
          dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), nextAction.getValue(Action.NAME));
          dlg.getRootPane().getActionMap().put(nextAction.getValue(Action.NAME), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
              nextAction.actionPerformed(e);
              if (result.isEscCloseEnabled()) {
                result.hide();
              }
            }
          });
        }
        else if (nextButton == null) {
          nextButton = new JButton(nextAction);
        }
        if (nextButton != null) {
          buttonBox.add(nextButton);
          buttonCount += 1;
        }
        KeyStroke accelerator = (KeyStroke) nextAction.getValue(Action.ACCELERATOR_KEY);
        if (accelerator != null) {
          dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(accelerator, nextAction);
          dlg.getRootPane().getActionMap().put(nextAction, nextAction);
        }
      }
      if (buttonCount > 0) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
        buttonPanel.add(buttonBox, BorderLayout.EAST);
        dlg.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
      }
    }

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
