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
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.DialogAligner;
import net.sourceforge.ganttproject.gui.NotificationComponent.AnimationView;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade.Centering;
import net.sourceforge.ganttproject.gui.UIFacade.Dialog;
import net.sourceforge.ganttproject.gui.UIUtil;

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

  private static class NotificationViewImpl implements AnimationView {
    private final JDialog myDlg;
    private BalloonTip myBalloon;
    private Runnable myOnHide;
    private final JButton myNotificationOwner;

    public NotificationViewImpl(JDialog dlg, JButton notificationOwner) {
      myDlg = dlg;
      myNotificationOwner = notificationOwner;
      notificationOwner.setAction(new AbstractAction("Error") {
        @Override
        public void actionPerformed(ActionEvent e) {
          showBalloon();
        }
      });
    }
    protected void showBalloon() {
      myBalloon.setVisible(true);
    }
    @Override
    public boolean isReady() {
      return myDlg.isVisible();
    }

    @Override
    public boolean isVisible() {
      return myBalloon != null && myBalloon.isVisible();
    }

    @Override
    public void setComponent(final JComponent component, JComponent owner, final Runnable onHide) {
      myNotificationOwner.setVisible(true);
      myBalloon = new BalloonTip(myNotificationOwner, component, new EdgedBalloonStyle(Color.WHITE, Color.BLACK),
          BalloonTip.Orientation.LEFT_ABOVE, BalloonTip.AttachLocation.ALIGNED, 30, 10, false);
      myBalloon.setVisible(false);
      myOnHide = onHide;
    }

    @Override
    public void close() {
      if (myBalloon != null) {
        myBalloon.setVisible(false);
      }
      myOnHide.run();
      myNotificationOwner.setVisible(false);
    }
  }

  private static class DialogImpl implements Dialog {
    /** Original animation view, used to set it back when the dialog is closed again */
    private AnimationView myOriginalAnimationView;
    private final JDialog myDlg;
    private final JFrame myMainFrame;
    private final NotificationManager myNotificationManager;
    private JButton myButton;

    DialogImpl(JDialog dlg, JFrame mainFrame, NotificationManager notificationManager) {
      myDlg = dlg;
      myMainFrame = mainFrame;
      myNotificationManager = notificationManager;
    }
    @Override
    public void hide() {
      if (myDlg.isVisible()) {
        myDlg.setVisible(false);
        myDlg.dispose();
      }
      myNotificationManager.setAnimationView(myOriginalAnimationView);
    }

    @Override
    public void show() {
      myOriginalAnimationView = myNotificationManager.setAnimationView(new NotificationViewImpl(myDlg, myButton));
      center(Centering.WINDOW);
      myDlg.setVisible(true);
    }

    @Override
    public void layout() {
      myDlg.pack();
    }

    @Override
    public void center(Centering centering) {
      DialogAligner.center(myDlg, myMainFrame, centering);
    }

    void setNotificationOwner(JButton button) {
      myButton = button;
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
  Dialog createDialog(Component content, Action[] buttonActions, String title, final NotificationManager notificationManager) {
    final JDialog dlg = new JDialog(myMainFrame, true);
    final DialogImpl result = new DialogImpl(dlg, myMainFrame, notificationManager);
    dlg.setTitle(title);
    final Commiter commiter = new Commiter();
    Action cancelAction = null;
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

    JButton errorButton = new JButton("Error");
    errorButton.setBackground(UIUtil.ERROR_BACKGROUND);
    //errorLabel.setBorder(BorderFactory.createCompoundBorder(errorLabel.getBorder(), BorderFactory.createEmptyBorder(2,2,2,2)));
    JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
    buttonPanel.add(buttonBox, BorderLayout.EAST);
    buttonPanel.add(errorButton, BorderLayout.WEST);
    dlg.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    result.setNotificationOwner(errorButton);
    errorButton.setVisible(false);

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
