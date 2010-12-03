/*
 * Created on 01.05.2005
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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
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
public abstract class TextFieldAndFileChooserComponent {
    private JButton myChooserButton;

    private JTextField myTextField;

    private File myFile;

    private FileFilter myFileFilter;

    private String myDialogCaption;

    private JComponent myComponent;

//    private Component myParentComponent;

    private int myFileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES;

    private boolean myProcessTextEventEnabled = true;

    private UIFacade myUiFacade;

//    public TextFieldAndFileChooserComponent(final Component parentComponent,
//            UIFacade uiFacade,
//            String dialogCaption) {
//        myDialogCaption = dialogCaption;
//        myParentComponent = parentComponent;
//        myUiFacade = uiFacade;
//        initComponents();
//    }

    public TextFieldAndFileChooserComponent(UIFacade uiFacade, String label, String dialogCaption) {
        Box innerBox = Box.createHorizontalBox();
//        innerBox.add(new JLabel(label));
//        innerBox.add(Box.createHorizontalStrut(5));
        myUiFacade = uiFacade;
//        myParentComponent = innerBox;
        myDialogCaption = dialogCaption;
        initComponents();
        innerBox.add(myComponent);
        myComponent = innerBox;
    }

    private void initComponents() {
        myChooserButton = new JButton(new AbstractAction(GanttLanguage.getInstance().getText("fileChooser.browse")) {
            public void actionPerformed(ActionEvent e) {
                showFileChooser();
            }
        });
        myTextField = new JTextField();
        //myTextField.setColumns(40);
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
        Box box = Box.createHorizontalBox();
        box.add(myTextField);
        box.add(Box.createHorizontalStrut(3));
        box.add(myChooserButton);
        myComponent = box;
    }

    public JComponent getComponent() {
        return myComponent;
    }

    public File getFile() {
        return myFile;
    }

    public void setFile(File file) {
        myFile = file;
        myTextField.setText(file == null ? "" : file.getAbsolutePath());
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
        /*
        Action[] actions = new Action[] {
                new AbstractAction("Your project file directory") {
                    public void actionPerformed(ActionEvent e) {
                    }
                },
                new AbstractAction("Last used location") {
                    public void actionPerformed(ActionEvent e) {
                    }
                },
                new AbstractAction("Select a new file") {
                    public void actionPerformed(ActionEvent arg0) {
                    }
                }};
        JComponent[] components = new JComponent[] {new JLabel("/foo/bar"), new JLabel("/tmp/foo"), fc};
        GPOptionChoicePanel filePanel = new GPOptionChoicePanel();

        JComponent filePanelComponent = filePanel.getComponent(actions, components, 0);
        filePanelComponent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        */
        Action[] dialogActions = new Action [] {
        		new OkAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        myFile = fc.getSelectedFile();
                        myTextField.setText(myFile.getAbsolutePath());
                        onFileChosen(myFile);
                    }
                },
                new CancelAction() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
					}
				}
        };
        myUiFacade.showDialog(fc, dialogActions);
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
