/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;

import com.google.common.base.Objects;

/**
 * @author bard
 */
public abstract class TextFieldAndFileChooserComponent extends JPanel {
  private final JButton myChooserButton;

  private final JTextField myTextField;

  private File myFile;

  private FileFilter myFileFilter;

  private final String myDialogCaption;

  private int myFileSelectionMode = JFileChooser.FILES_ONLY;

  private boolean myProcessTextEventEnabled = true;

  private final SpringLayout myLayout = new SpringLayout();

  private final UIFacade myUiFacade;

  public TextFieldAndFileChooserComponent(UIFacade uiFacade, String dialogCaption) {
    myUiFacade = uiFacade;
    myDialogCaption = dialogCaption;

    setLayout(myLayout);

    myChooserButton = new JButton(new GPAction("fileChooser.browse") {
      @Override
      public void actionPerformed(ActionEvent e) {
        showFileChooser();
      }
    });

    myTextField = new JTextField();

    myTextField.getDocument().addDocumentListener(new DocumentListener() {
      private final Timer myTimer = new Timer();
      private TimerTask myTimerTask = null;

      @Override
      public void removeUpdate(DocumentEvent e) {
        onChange();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        onChange();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        onChange();
      }

      private void onChange() {
        if (myTimerTask == null && myProcessTextEventEnabled) {
          myTimerTask = new TimerTask() {
            @Override
            public void run() {
              SwingUtilities.invokeLater(new Runnable() {
                @Override
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

    // myChooserPanel = new JPanel(new BorderLayout());
    add(myTextField);
    add(myChooserButton);
    // add(myChooserPanel);
    myLayout.putConstraint(SpringLayout.WEST, myTextField, 0, SpringLayout.WEST, this);
    myLayout.putConstraint(SpringLayout.NORTH, myTextField, 0, SpringLayout.NORTH, this);
    myLayout.putConstraint(SpringLayout.WEST, myChooserButton, 3, SpringLayout.EAST, myTextField);
    myLayout.putConstraint(SpringLayout.NORTH, myChooserButton, 0, SpringLayout.NORTH, this);
    myLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, myChooserButton);
    // myLayout.putConstraint(SpringLayout.NORTH, myChooserPanel, 3,
    // SpringLayout.SOUTH, myChooserButton);
    // myLayout.putConstraint(SpringLayout.WEST, myChooserPanel, 0,
    // SpringLayout.WEST, myTextField);
    // myLayout.putConstraint(SpringLayout.EAST, myChooserPanel, 0,
    // SpringLayout.EAST, myChooserButton);
    myLayout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, myChooserButton);
  }

  public File getFile() {
    return myFile;
  }

  /**
   * Updates the file path
   *
   * @param file
   *          if this is point to a directory, the previously used file name is
   *          added
   */
  public void setFile(File file) {
    if (file == null) {
      return;
    }

    if (file.isDirectory()) {
      // Add previously used file name because we need/like to select files
      myFile = new File(file, (myFile == null ? "out" : myFile.getName()));
    } else {
      myFile = file;
    }
    if (myProcessTextEventEnabled) {
      myTextField.setText(file.getAbsolutePath());
    }
  }

  public void setFileFilter(FileFilter filter) {
    myFileFilter = filter;
  }

  // public void setChooserHeight(int height) {
  // Rectangle bounds = myChooserPanel.getBounds();
  // myChooserPanel.setMinimumSize(new Dimension(bounds.width, height));
  // myChooserPanel.setMaximumSize(new Dimension(bounds.width, height));
  // myChooserPanel.setPreferredSize(new Dimension(bounds.width, height));
  // this.doLayout();
  // myChooserPanel.doLayout();
  // myWizard.getDialog().layout();
  // }

  // public int getChooserHeight() {
  // return getHeight();
  // }

  // protected void hideFileChooser() {
  // Timeline t = new Timeline(this);
  // t.addPropertyToInterpolate("chooserHeight", 0, myChooserPanel.getHeight());
  // t.playReverse();
  // myChooserPanel.removeAll();
  // myFileChooser = null;
  // }

  public void showFileChooser() {
    final JFileChooser fc = new JFileChooser(new File(myTextField.getText()));

    File selectedFile = getFile();
    fc.setCurrentDirectory(selectedFile == null ? getWorkingDir() : selectedFile.getParentFile());
    fc.setDialogType(JFileChooser.SAVE_DIALOG);
    fc.setDialogTitle(myDialogCaption);
    fc.setControlButtonsAreShown(false);
    fc.setApproveButtonToolTipText(myDialogCaption);
//    fc.setFileSelectionMode(myFileSelectionMode);
//
    // Remove the possibility to use a file filter for all files
    FileFilter[] filefilters = fc.getChoosableFileFilters();
    for (int i = 0; i < filefilters.length; i++) {
      fc.removeChoosableFileFilter(filefilters[i]);
    }

    fc.addChoosableFileFilter(myFileFilter);
    for (FileFilter ff : filefilters) {
      fc.addChoosableFileFilter(ff);
    }
    fc.doLayout();

//    fc.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener() {
//      @Override
//      public void propertyChange(PropertyChangeEvent evt) {
//        File f = (File) evt.getNewValue();
//        if (f != null) {
//          setFile(f);
//        }
//      }
//    });
//
    // myChooserPanel.add(fc, BorderLayout.CENTER);
    // Timeline t = new Timeline(this);
    // t.addPropertyToInterpolate("chooserHeight", 0,
    // fc.getPreferredSize().height);
    // t.play();
    // myFileChooser = fc;
    Action[] dialogActions = new Action[] { new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        File selected = fc.getSelectedFile();
        if (selected == null || Objects.equal(selected, fc.getCurrentDirectory())) {
          JTextComponent textField = getTextField(fc);
          if (textField != null) {
            String filename = textField.getText();
            File currentDirectory = fc.getCurrentDirectory();
            selected = new File(currentDirectory, filename);
          }
        }
        setFile(selected);
        onFileChosen(myFile);
      }

      private JTextComponent getTextField(Container container) {
        JTextComponent result = null;
        for (Component c : container.getComponents()) {
          if (c instanceof JTextComponent) {
            result = (JTextComponent) c;
            return result;
          }
          if (c instanceof Container) {
            result = getTextField((Container) c);
          }
          if (result != null) {
            return result;
          }
        }
        return null;
      }
    }, CancelAction.EMPTY };
//    int result = fc.showSaveDialog(myUiFacade.getMainFrame());
//    if (result == JFileChooser.APPROVE_OPTION) {
//      setFile(fc.getSelectedFile());
//      onFileChosen(myFile);
//    }
    myUiFacade.createDialog(fc, dialogActions, "").show();
  }

  private File getWorkingDir() {
    return new File(System.getProperty("user.dir"));
  }

  public void tryFile() {
    myProcessTextEventEnabled = false;
    myFile = new File(myTextField.getText());
    onFileChosen(myFile);
    myProcessTextEventEnabled = true;
  }

  protected abstract void onFileChosen(File file);

  public void setFileSelectionMode(int mode) {
    myFileSelectionMode = mode;
  }

  public URL getSelectedURL() {
    try {
      return myFile == null ? new URL("file://" + myTextField.getText()) : myFile.toURI().toURL();
    } catch (MalformedURLException e) {
      GPLogger.log(e);
      return null;
    }
  }
}
