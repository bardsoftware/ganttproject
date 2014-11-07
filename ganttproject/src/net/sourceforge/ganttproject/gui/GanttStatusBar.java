/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Alexandre Thomas, GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.font.Fonts;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Status bar (located below the main frame)
 * 
 * @author athomas
 */
public class GanttStatusBar extends JPanel {
  boolean bRunning = false;

  private JFrame myMainFrame;

  private static IProgressMonitor ourMonitor;

  public GanttStatusBar(JFrame mainFrame) {
    super(new BorderLayout());
    myMainFrame = mainFrame;
    add(new JPanel(), BorderLayout.CENTER);
  }

  public IProgressMonitor createProgressMonitor() {
    if (ourMonitor == null) {
      ourMonitor = new ProgressMonitorImpl();
    }
    return ourMonitor;
  }

  public void setFirstText(String text) {
  }

  public void setSecondText(String text) {
  }

  /**
   * Show the given text in the first message area for the given amount of
   * milliseconds
   * 
   * @param text
   *          to show
   * @param milliseconds
   *          amount of milliseconds to show the text
   */
  public void setFirstText(String text, int milliseconds) {
  }

  /**
   * Show the given text in the second message area for the given amount of
   * milliseconds
   * 
   * @param text
   *          to show
   * @param milliseconds
   *          amount of milliseconds to show the text
   */
  public void setSecondText(String text, int milliseconds) {
  }

  private class ProgressBarDialog extends JDialog {
    private JProgressBar myProgressBar;
    private JLabel myLabel;
    private String myTask;
    private String mySubTask;
    private IProgressMonitor myProgressMonitor;
    private int myWorked;
    private int myTotalWork;

    private ProgressBarDialog(IProgressMonitor progressMonitor) {
      super(myMainFrame, true);
      myProgressMonitor = progressMonitor;
    }

    @Override
    protected void dialogInit() {
      super.dialogInit();
      myProgressBar = new JProgressBar();
      myProgressBar.setMinimumSize(new Dimension(400, 50));
      myProgressBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      myLabel = new JLabel();
      myLabel.setFont(Fonts.GENERAL_DIALOG_FONT.deriveFont(14));
      myLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(myProgressBar, BorderLayout.CENTER);
      JPanel labelAndButton = new JPanel(new BorderLayout());
      labelAndButton.add(myLabel, BorderLayout.CENTER);
      JButton cancelButton = new JButton(new CancelAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myProgressMonitor.setCanceled(true);
          // System.err.println("\n\n"+Platform.getJobManager().currentJob().getName()+"\n\n");
          // Platform.getJobManager().currentJob().cancel();
        }
      });
      labelAndButton.add(cancelButton, BorderLayout.EAST);
      getContentPane().add(labelAndButton, BorderLayout.SOUTH);
      setResizable(false);
      this.setUndecorated(true);
      pack();
      setSize(400, 60);
      DialogAligner.center(this, myMainFrame);
    }

    void start(String task, int totalWork) {
      myTask = task;
      myWorked = 0;
      myTotalWork = totalWork;
      myProgressBar.setMinimum(0);
      myProgressBar.setMaximum(totalWork);
      myLabel.setText(getLabelText());
      setVisible(true);
    }

    void setSubTask(String subTask) {
      mySubTask = subTask;
      myLabel.setText(getLabelText());
    }

    void setProgress(int work) {
      assert myWorked <= myTotalWork;
      myWorked = work;
      myProgressBar.setValue(work);
      myLabel.setText(getLabelText());
    }

    void done() {
      // Reset bar to 0, otherwise it is briefly at 100% for a next job
      myProgressBar.setValue(0);
      dispose();
    }

    private String getLabelText() {
      return "<html><body><b>" + (mySubTask == null ? myTask : mySubTask) + " ... " + myWorked * 100 / myTotalWork
          + "%</b></body></html>";
    }
  }

  private class ProgressMonitorImpl implements IProgressMonitor {
    private int myWorked;
    ProgressBarDialog myProgressDialog;
    private boolean isCanceled;

    ProgressMonitorImpl() {
      myProgressDialog = new ProgressBarDialog(this);
    }

    @Override
    public void beginTask(final String name, final int totalWork) {
      isCanceled = false;
      myWorked = 0;
      GPLogger.log("[ProgressMonitorImpl] begin Task: name=" + name);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          myProgressDialog.start(name, totalWork);
        }
      });
    }

    @Override
    public void done() {
      GPLogger.log("[ProgressMonitorImpl] finished Task");
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          myProgressDialog.done();
        }
      });
    }

    @Override
    public void internalWorked(double work) {
    }

    @Override
    public boolean isCanceled() {
      return isCanceled;
    }

    @Override
    public void setCanceled(boolean value) {
      myProgressDialog.done();
      isCanceled = value;
    }

    @Override
    public void setTaskName(String name) {
    }

    @Override
    public void subTask(final String name) {
      if (name == null) {
        GPLogger.log("[ProgressMonitorImpl] finished subTask");
      } else {
        GPLogger.log("[ProgressMonitorImpl] begin subTask: name=" + name);
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          myProgressDialog.setSubTask(name);
        }
      });
    }

    @Override
    public void worked(final int work) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          myWorked += work;
          myProgressDialog.setProgress(myWorked);
        }
      });
    }
  }

  public void setNotificationManager(NotificationManagerImpl notificationManager) {
    add(notificationManager.getChannelButtons(), BorderLayout.EAST);
  }
}
