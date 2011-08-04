/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.eclipse.core.runtime.IStatus;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author bard
 */
public abstract class TextFieldAndFileChooserComponent extends JPanel {
    private final JButton myChooserButton;

    private final JTextField myTextField;

    private File myFile;

    private FileFilter myFileFilter;

    private final String myDialogCaption;

    private int myFileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES;

    private boolean myProcessTextEventEnabled = true;

    private final UIFacade myUiFacade;

    public TextFieldAndFileChooserComponent(UIFacade uiFacade, String label, String dialogCaption) {
        myUiFacade = uiFacade;
        myDialogCaption = dialogCaption;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        myChooserButton = new JButton(new AbstractAction(GanttLanguage.getInstance().getText("fileChooser.browse")) {
            public void actionPerformed(ActionEvent e) {
                showFileChooser();
            }
        });

        myTextField = new JTextField();

        myTextField.getDocument().addDocumentListener(new DocumentListener() {
            private final Timer myTimer = new Timer();
            private TimerTask myTimerTask = null;

            public void removeUpdate(DocumentEvent e) {
                onChange();
            }
            public void insertUpdate(DocumentEvent e) {
                onChange();
            }
            public void changedUpdate(DocumentEvent arg0) {
                onChange();
            }
            private void onChange() {
                if (myTimerTask == null && myProcessTextEventEnabled) {
                    myTimerTask = new TimerTask() {
                        public void run() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    tryFile();
                                }
                            });
                            myTimerTask = null;
                        }
                    };
                    myTimer.schedule(myTimerTask, 1000);
                }
            }
        });

        add(myTextField);
        add(Box.createHorizontalStrut(3));
        add(myChooserButton);
    }

    public File getFile() {
        return myFile;
    }

	/**
	 * Updates the file path
	 * 
	 * @param file if this is point to a directory, the previously used file name
	 *            is added
	 */
    public void setFile(File file) {
    	if(file == null) {
    		// Empty the files
    		myFile = null;
    		myTextField.setText("");
    	}

    	if(file.isDirectory()) {
    		// Add previously used file name because we need/like to select files
    		myFile = new File(file, myFile.getName());
    	} else {
    		myFile = file;
    	}
        myTextField.setText(file.getAbsolutePath());
    }

    public void setFileFilter(FileFilter filter) {
        myFileFilter = filter;
    }

    public void showFileChooser() {
        final JFileChooser fc = new JFileChooser(new File(myTextField.getText()));
        fc.setDialogTitle(myDialogCaption);
        fc.setControlButtonsAreShown(false);
        fc.setApproveButtonToolTipText(myDialogCaption);
        fc.setFileSelectionMode(myFileSelectionMode);

        // Remove the possibility to use a file filter for all files
        FileFilter[] filefilters = fc.getChoosableFileFilters();
        for (int i = 0; i < filefilters.length; i++) {
            fc.removeChoosableFileFilter(filefilters[i]);
        }

        fc.addChoosableFileFilter(myFileFilter);

        Action[] dialogActions = new Action [] {
                new OkAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setFile(fc.getSelectedFile());
                        onFileChosen(myFile);
                    }
                },
                new CancelAction() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                    }
                }
        };
        myUiFacade.createDialog(fc, dialogActions, "").show();
    }

    public void tryFile() {
        myProcessTextEventEnabled = false;
        onFileChosen(new File(myTextField.getText()));
        myProcessTextEventEnabled = true;
    }

    protected abstract void onFileChosen(File file);

    public void setFileSelectionMode(int mode) {
        myFileSelectionMode = mode;
    }

    public URL getSelectedURL() {
        try {
            return new URL("file://" + myTextField.getText());
        } catch (MalformedURLException e) {
            GPLogger.log(e);
            return null;
        }
    }

    protected void showFileStatus(IStatus status) {
    }
}
