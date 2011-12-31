/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Alexandre Thomas, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Simulate a status bar under the main frame
 *
 * @author athomas
 */
public class GanttStatusBar extends JPanel implements Runnable {
    protected MessagePanel message0;

    /** Panel containing first message */
    protected MessagePanel message1;

    /** Panel containing second Message */
    protected MessagePanel message2;

    private static final int NO_MESSAGE = 1;

    private static final int MESSAGE_1 = 0;

    private static final int MESSAGE_2 = 1;

    // private static final int PROGRESS_FINISH = 2;

    int mode = NO_MESSAGE;

    boolean bRunning = false;

    private JFrame myMainFrame;

    private Runnable myErrorNotifier;

    private JPanel myErrorNotificationPanel;

    private static IProgressMonitor ourMonitor;

    public GanttStatusBar(JFrame mainFrame) {
        super(new BorderLayout());
        myMainFrame = mainFrame;
        //pbp = new ProgressBarPanel();
        message0 = new MessagePanel(215, false);
        message1 = new MessagePanel(400, true);
        message2 = new MessagePanel(250, true);
        myErrorNotificationPanel = new JPanel();
        //myMainFrame.setGlassPane(pbp);

        //pbp.setPreferredSize(new Dimension(160, 16));
        // message1.setPreferredSize(new Dimension(360,25));
        // message2.setPreferredSize(new Dimension(210,25));

        //add(pbp, BorderLayout.WEST);
        //add(message0, BorderLayout.WEST);
        add(myErrorNotificationPanel, BorderLayout.WEST);
        add(message1, BorderLayout.CENTER);
        add(message2, BorderLayout.EAST);

        message0.setText("GanttProject.biz (" + GanttProject.version + ")");
        //pbp.setValue(0);

        setFirstText(GanttLanguage.getInstance().getText("welcome"), 5000);
    }

    public IProgressMonitor createProgressMonitor() {
        if(ourMonitor == null) {
            ourMonitor = new ProgressMonitorImpl();
        }
        return ourMonitor;
    }

    public void setFirstText(String text) {
        message1.setText(text);
    }

    public void setSecondText(String text) {
        message2.setText(text);
    }

    /**
     * Show the given text in the first message area for the given amount of
     * milliseconds
     *
     * @param text to show
     * @param milliseconds amount of milliseconds to show the text
     */
    public void setFirstText(String text, int milliseconds) {
        if (!isVisible()) {
            return;
        }
        message1.setText(text, milliseconds);
        mode = MESSAGE_1;
        if (!bRunning) {
            bRunning = true;
            new Thread(this).start();
        }
    }

    /**
     * Show the given text in the second message area for the given amount of
     * milliseconds
     *
     * @param text to show
     * @param milliseconds amount of milliseconds to show the text
     */
    public void setSecondText(String text, int milliseconds) {
        if (!isVisible()) {
            return;
        }
        message2.setText(text, milliseconds);
        mode = MESSAGE_2;
        if (!bRunning) {
            bRunning = true;
            new Thread(this).start();
        }
    }

    /** @return the preferred size of this component. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), 24);
    }

    @Override
    public void run() {
        try {
            switch (mode) {
            case MESSAGE_1:
                Thread.sleep(message1.getTimer());
                message1.hideText();
                message1.clear();
                break;
            case MESSAGE_2:
                Thread.sleep(message2.getTimer());
                message2.hideText();
                message2.clear();
                break;
            }
            mode = NO_MESSAGE;
        } catch (InterruptedException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        }
        bRunning = false;
    }

    /** Class to display a message */
    private class MessagePanel extends JPanel {
        private JLabel message;

        private Color textColor = Color.BLACK;

        private int timer = 0;

        public MessagePanel(int size, boolean separator) {

            super(new FlowLayout());
            message = new JLabel() {
                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(textColor);
                    g2.drawString(getText(), 0, 12);
                }
            };
            if (size != -1) {
                message.setPreferredSize(new Dimension(size, 16));
                message.setMaximumSize(new Dimension(size, 16));
                message.setMaximumSize(new Dimension(size, 16));
            }
            if (separator) {
                add(new JLabel("|"));
            }
            add(message);
        }

        /** set a new text to the message. */
        public void setText(String text) {
            message.setText(text);
            timer = 0;
        }

        /** set a new text to the message. */
        public void setText(String text, int mltimer) {
            message.setText(text);
            timer = mltimer;
        }

        /** clear the panel. */
        public void clear() {
            message.setText("");
        }

        /** Hide the text by decrease the color. */
        public void hideText() {
            try {
                Color cPanel = getBackground();

                int step = 50;

                float dRed = (float) cPanel.getRed() / (float) step;
                float dGreen = (float) cPanel.getGreen() / (float) step;
                float dBlue = (float) cPanel.getBlue() / (float) step;

                for (int i = 0; i < step; i++) {
                    textColor = new Color(dRed * i, dGreen * i, dBlue * i);
                    repaint();
                    Thread.sleep(20);
                }
                textColor = Color.BLACK;
            } catch (Exception e) {
                // exception for the thread.sleep
            }
        }

        /** @return the timer. */
        public int getTimer() {
            return timer;
        }
    }

    // TODO class is never used, remove?
    private static class ErrorNotificationPanel extends JPanel {
        private JLabel myLabel;

        ErrorNotificationPanel() {
            super(new BorderLayout());
        }

        void enableNotifier(final Runnable notifier) {
            myLabel = new JLabel("<html><body><b>Errors happened. Click here to see the details</b></body></html>");
            myLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    notifier.run();
                }
            });
            add(myLabel);
            revalidate();
        }

        void disableNotifier() {
            remove(myLabel);
            revalidate();
        }
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
            myProgressBar.setMinimumSize(new Dimension(400,50));
            myProgressBar.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            myLabel = new JLabel();
            myLabel.setFont(Fonts.GENERAL_DIALOG_FONT.deriveFont(14));
            myLabel.setBorder(BorderFactory.createEmptyBorder(0,5,5,5));
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(myProgressBar, BorderLayout.CENTER);
            JPanel labelAndButton = new JPanel(new BorderLayout());
            labelAndButton.add(myLabel, BorderLayout.CENTER);
            JButton cancelButton = new JButton(new CancelAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    myProgressMonitor.setCanceled(true);
                    //System.err.println("\n\n"+Platform.getJobManager().currentJob().getName()+"\n\n");
                    //Platform.getJobManager().currentJob().cancel();
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
            return "<html><body><b>" + (mySubTask == null ? myTask : mySubTask) + " ... " + myWorked * 100
                    / myTotalWork + "%</b></body></html>";
        }
    }

    private class ProgressMonitorImpl implements IProgressMonitor {
        private int myWorked;
        //private CancelableProgressPanel myProgressPanel;
        //ProgressBarPanel myProgressPanel;
        ProgressBarDialog myProgressDialog;
        private boolean isCanceled;

        ProgressMonitorImpl() {
            //myProgressPanel = new ProgressBarPanel();
            myProgressDialog = new ProgressBarDialog(this);
        }
        @Override
        public void beginTask(final String name, final int totalWork)  {
            isCanceled = false;
            myWorked = 0;
            GPLogger.log("[ProgressMonitorImpl] begin Task: name=" + name);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // pbp.reset(name, totalWork);
                    // pbp.setVisible(true);
                    // myMainFrame.setGlassPane(myProgressPanel);
                    // myProgressPanel.setVisible(true);
                    // myMainFrame.getRootPane().revalidate();
                    // myProgressPanel.start();
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
            //myProgressPanel.stop();
            //myProgressPanel.setVisible(false);
                    myProgressDialog.done();
//                    pbp.reset();
//                    pbp.setVisible(false);
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

    public void setErrorNotifier(Runnable notifier) {
        if (notifier == null && myErrorNotifier != null) {
            clearErrorNotification();
            return;
        }
        if (myErrorNotifier == null) {
            createErrorNotification(notifier);
        }
        myErrorNotifier = notifier;
    }

    private void clearErrorNotification() {
//        myErrorNotificationPanel.disableNotifier();
    }

    private void createErrorNotification(Runnable notifier) {
//        myErrorNotificationPanel.enableNotifier(notifier);
    }

    public void setNotificationManager(NotificationManagerImpl notificationManager) {
        myErrorNotificationPanel.add(notificationManager.getChannelButtons());
    }
}
