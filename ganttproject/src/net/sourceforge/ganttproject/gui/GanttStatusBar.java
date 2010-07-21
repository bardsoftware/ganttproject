/***************************************************************************
 GanttStatusBar.java 
 ------------------------------------------
 begin                : 5 juil. 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas@ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.gui;

import java.awt.AlphaComposite;
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
import org.eclipse.core.runtime.Platform;

/**
 * @author athomas Simulate a status bar under the main frame
 */
public class GanttStatusBar extends JPanel implements Runnable {
    protected MessagePanel message0;

    /** Message 1. */
    protected MessagePanel message1;

    /** Message 2. */
    protected MessagePanel message2;

    private static final int NO_MESSAGE = 1;

    private static final int MESSAGE_1 = 0;

    private static final int MESSAGE_2 = 1;

    private static final int PROGRESS_FINISH = 2;

    int mode = NO_MESSAGE;

    boolean bRunning = false;

	private JFrame myMainFrame;

	private Runnable myErrorNotifier;

	private ErrorNotificationPanel myErrorNotificationPanel;

    /** Default constructor. */
    public GanttStatusBar(JFrame mainFrame) {
        super(new BorderLayout());
        myMainFrame = mainFrame;
        //pbp = new ProgressBarPanel();
        message0 = new MessagePanel(215, false);
        message1 = new MessagePanel(400, true);
        message2 = new MessagePanel(250, true);
        myErrorNotificationPanel = new ErrorNotificationPanel();
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
        setFirstText(GanttLanguage.getInstance().getText("welcome"), 500);
    }

    public IProgressMonitor createProgressMonitor() {
        return new ProgressMonitorImpl();
        
    }

    public void setFirstText(String text) {
        message1.setText(text);
    }

    public void setSecondText(String text) {
        message2.setText(text);
    }

    public void setFirstText(String text, int mlTimer) {
        if (!isVisible())
            return;
        message1.setText(text, mlTimer);
        mode = MESSAGE_1;
        if (!bRunning) {
            bRunning = true;
            new Thread(this).start();
        }
    }

    public void setSecondText(String text, int mlTimer) {
        if (!isVisible())
            return;
        message2.setText(text, mlTimer);
        mode = MESSAGE_2;
        if (!bRunning) {
            bRunning = true;
            new Thread(this).start();
        }
    }

    /** @return the preferred size of this component. */
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), 24);
    }

    public void run() {
        try {
            // while(true){
            switch (mode) {
            case MESSAGE_1:
                Thread.sleep(message1.getTimer());
                message1.hideText();
                message1.setText("");
                break;
            case MESSAGE_2:
                Thread.sleep(message2.getTimer());
                message2.hideText();
                message2.setText("");
                break;
            }
            mode = NO_MESSAGE;
            // }
        } catch (Exception e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
        }
        bRunning = false;
    }

    // ! Class to display a message
    private class MessagePanel extends JPanel {
        JLabel message;

        Color textColor = Color.BLACK;

        int timer = 0;

        /** Constructor. */
        public MessagePanel(int size, boolean separator) {
            super(new FlowLayout());
            message = new JLabel() {
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
            if (separator)
                add(new JLabel("|"));
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
                Color cBlack = Color.BLACK;

                int step = 50;

                float dRed = (float) cPanel.getRed() / (float) step;
                float dGreen = (float) cPanel.getGreen() / (float) step;
                float dBlue = (float) cPanel.getBlue() / (float) step;

                for (int i = 0; i < step; i++) {
                    textColor = new Color((int) (dRed * (float) i),
                            (int) (dGreen * (float) i),
                            (int) (dBlue * (float) i));
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

    private static class ErrorNotificationPanel extends JPanel {
    	private JLabel myLabel;

		ErrorNotificationPanel() {
    		super(new BorderLayout());
    	}
    	
    	void enableNotifier(final Runnable notifier) {
    		myLabel = new JLabel("<html><body><b>Errors happened. Click here to see the details</b></body></html>");
    		myLabel.addMouseListener(new MouseAdapter() {
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
		private IProgressMonitor myProgressMonitor;


		private ProgressBarDialog(IProgressMonitor progressMonitor) {
    		super(myMainFrame, true);
    		myProgressMonitor = progressMonitor;
    	}
    	
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
		}


		void start(String task, int totalWork) {
			myProgressBar.setMaximum(totalWork);
			myProgressBar.setMinimum(0);
			myTask = task;
			myLabel.setText(getLabelText());
			pack();
			setSize(400, 60);
			DialogAligner.center(this, myMainFrame);
    		setVisible(true);
    	}
		
		void setProgress(int work) {
			myProgressBar.setValue(work);
			myLabel.setText(getLabelText());
		}

		void done() {
			dispose();
		}
		
		private String getLabelText() {
			return "<html><body><b>"+myTask+" ... "+myProgressBar.getValue()*100/myProgressBar.getMaximum()+"%</b></body></html>";
		}
		
    }
    private class ProgressMonitorImpl implements IProgressMonitor {
    	private int myWorked;
    	//private CancelableProgressPanel myProgressPanel;
    	//ProgressBarPanel myProgressPanel;
    	ProgressBarDialog myProgressDialog;
    	String myTaskName;
	    private int myTotalWork;
		private boolean isCanceled;

    	ProgressMonitorImpl() {
    		//myProgressPanel = new ProgressBarPanel();
    		myProgressDialog = new ProgressBarDialog(this);
    	}
        public void beginTask(final String name, final int totalWork)  {
            try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
				        //pbp.reset(name, totalWork);
				        //pbp.setVisible(true);
				    	myTaskName = name;
				    	myTotalWork = totalWork;
						//myMainFrame.setGlassPane(myProgressPanel);
						//myProgressPanel.setVisible(true);
						//myMainFrame.getRootPane().revalidate();
						//myProgressPanel.start();
				    	myProgressDialog.start(name, totalWork);
				        GPLogger.log("[ProgressMonitorImpl] beginTask: name="+name);
				    }
				});
			} /*catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			finally {
				
			}
        }

        public void done() {
        	SwingUtilities.invokeLater(new Runnable() {
				public void run() {
        	//myProgressPanel.stop();
        	//myProgressPanel.setVisible(false);
					myProgressDialog.done();
//		            pbp.reset();
//		            pbp.setVisible(false);
				}
        	});
        }

        public void internalWorked(double work) {
        }

        public boolean isCanceled() {
            return isCanceled;
        }

        public void setCanceled(boolean value) {
        	myProgressDialog.done();
        	isCanceled = value;
        }

        public void setTaskName(String name) {
        }

        public void subTask(String name) {
            throw new UnsupportedOperationException();
        }

        public void worked(final int work) {
        	try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
				    	myWorked += work;
				    	myProgressDialog.setProgress(myWorked);
					}
				});
			} /*catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			finally {
				
			}
        }
        
    }
	public void setErrorNotifier(Runnable notifier) {
		if (notifier==null && myErrorNotifier!=null) {
			clearErrorNotification();
			return;
		}
		if (myErrorNotifier==null) {
			createErrorNotification(notifier);
		}
		myErrorNotifier = notifier;
	}

	private void clearErrorNotification() {
		myErrorNotificationPanel.disableNotifier();
	}

	private void createErrorNotification(Runnable notifier) {
		myErrorNotificationPanel.enableNotifier(notifier);
	}
}
