/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.TextFieldAndFileChooserComponent;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.GPOptionChoicePanel;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;

import com.google.common.base.Objects;

import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.GPOptionGroup;

public abstract class AbstractFileChooserPage implements WizardPage {
  public static final int FILE_SOURCE = 0;
  public static final int URL_SOURCE = 1;
  protected static final String PREF_SELECTED_FILE = "selected_file";
  private static final String PREF_SELECTED_URL = "selected_url";
  private static final FileFilter ACCEPT_ALL = new FileFilter() {
    @Override
    public String getDescription() {
      return null;
    }
    @Override
    public boolean accept(File f) {
      return true;
    }
  };

  private JPanel myComponent;
  private TextFieldAndFileChooserComponent myChooser;
  private JTextField myUrlField;
  private final OptionsPageBuilder myOptionsBuilder;
  private final JPanel mySecondaryOptionsComponent;
  private int ourSelectedSource = AbstractFileChooserPage.FILE_SOURCE;
  private final boolean isUrlChooserEnabled;
  private final JLabel myFileLabel = new JLabel("  ");
  private final JLabel myUrlLabel = new JLabel("  ");
  private final Preferences myPreferences;
  private final UIFacade myUiFacade;
  private final FileFilter myFileFilter;
  private final GPOptionGroup[] myOptions;
  private final String myTitle;
  private AbstractWizard myWizard;

  protected AbstractFileChooserPage(UIFacade uiFacade, Preferences prefs, String title, FileFilter fileFilter, GPOptionGroup[] options, boolean enableUrlChooser) {
    myUiFacade = uiFacade;
    myPreferences = prefs;
    myTitle = title;
    myFileFilter = Objects.firstNonNull(fileFilter, ACCEPT_ALL);
    myOptions = Objects.firstNonNull(options, new GPOptionGroup[0]);
    isUrlChooserEnabled = enableUrlChooser;
    myOptionsBuilder = new OptionsPageBuilder();
    mySecondaryOptionsComponent = new JPanel(new BorderLayout());
    mySecondaryOptionsComponent.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
  }

  protected int getFileChooserSelectionMode() {
    return JFileChooser.FILES_AND_DIRECTORIES;
  }

  @Override
  public JComponent getComponent() {
    myComponent = new JPanel(new BorderLayout());
    myChooser = new TextFieldAndFileChooserComponent(myUiFacade, myTitle) {
      @Override
      protected void onFileChosen(File file) {
        AbstractFileChooserPage.this.onSelectedFileChange(file);
      }
    };
    myChooser.setFileSelectionMode(getFileChooserSelectionMode());
    JComponent contentPanel = new JPanel(new BorderLayout());
    if (!isUrlChooserEnabled) {
      contentPanel.add(myChooser, BorderLayout.NORTH);
    } else {
      final UrlFetcher urlFetcher = new UrlFetcher() {
        @Override
        protected void onFetchComplete(File file) {
          super.onFetchComplete(file);
          onSelectedFileChange(file);
        }
      };
      myUrlField = new JTextField();
      Box urlBox = Box.createVerticalBox();
      urlBox.add(myUrlField);
      urlBox.add(myUrlLabel);
      myUrlField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
          onChange();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
          onChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          onChange();
        }

        private void onChange() {
          urlFetcher.setUrl(getSelectedUrl());
        }
      });

      Box fileBox = Box.createVerticalBox();
      fileBox.add(myChooser);
      fileBox.add(myFileLabel);

      Action fileSourceAction = new AbstractAction(GanttLanguage.getInstance().getText("file")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          ourSelectedSource = AbstractFileChooserPage.FILE_SOURCE;
          myChooser.tryFile();
        }
      };
      Action urlSourceAction = new AbstractAction(GanttLanguage.getInstance().getText("url")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          ourSelectedSource = AbstractFileChooserPage.URL_SOURCE;
          urlFetcher.setStatusLabel(myUrlLabel);
          urlFetcher.setUrl(getSelectedUrl());
        }
      };
      Action[] importSourceActions = new Action[] { fileSourceAction, urlSourceAction };
      JComponent[] importSourceComponents = new JComponent[] { fileBox, urlBox };
      GPOptionChoicePanel sourceChoicePanel = new GPOptionChoicePanel();

      Box sourceBox = Box.createVerticalBox();
      sourceBox.add(sourceChoicePanel.getComponent(importSourceActions, importSourceComponents, ourSelectedSource));
      sourceBox.add(Box.createVerticalStrut(5));
      sourceBox.add(urlFetcher.getComponent());
      contentPanel.add(sourceBox, BorderLayout.NORTH);
    }
    contentPanel.add(mySecondaryOptionsComponent, BorderLayout.CENTER);
    myComponent.add(contentPanel, BorderLayout.NORTH);
    return myComponent;
  }

  protected void loadPreferences() {
    String oldFile = myPreferences.get(AbstractFileChooserPage.PREF_SELECTED_FILE, null);
    if (oldFile != null) {
      // Use the previously used path with the current filename for the default
      // name
      // The implementing classes can modify the file extension when desired
      File f = new File(oldFile);
      onSelectedFileChange(f);
      myChooser.setFile(f);
    }
    if (myUrlField != null && myPreferences.get(AbstractFileChooserPage.PREF_SELECTED_URL, null) != null) {
      myUrlField.setText(myPreferences.get(AbstractFileChooserPage.PREF_SELECTED_URL, null));
    }
  }

  @Override
  public void setActive(AbstractWizard wizard) {
    GPOptionGroup[] optionGroups = getOptionGroups();
    myWizard = wizard;
    if (wizard == null) {
      // Means that user switched to either previous or next page
      if (myChooser.getFile() != null) {
        myPreferences.put(AbstractFileChooserPage.PREF_SELECTED_FILE, myChooser.getFile().getAbsolutePath());
      }
      if (myUrlField != null) {
        myPreferences.put(AbstractFileChooserPage.PREF_SELECTED_URL, myUrlField.getText());
      }
    } else {
      if (mySecondaryOptionsComponent != null) {
        mySecondaryOptionsComponent.removeAll();
      }
      mySecondaryOptionsComponent.add(createSecondaryOptionsPanel(), BorderLayout.NORTH);
      myChooser.setFileFilter(myFileFilter);
      loadPreferences();
      wizard.getDialog().layout();
    }
  }

  protected Component createSecondaryOptionsPanel() {
    return myOptionsBuilder.buildPlanePage(getOptionGroups());
  }

  protected final Preferences getPreferences() {
    return myPreferences;
  }

  protected final TextFieldAndFileChooserComponent getChooser() {
    return myChooser;
  }

  protected AbstractWizard getWizard() {
    return myWizard;
  }

  private URL getSelectedUrl() {
    try {
      switch (ourSelectedSource) {
      case FILE_SOURCE:
        return myChooser.getSelectedURL();
      case URL_SOURCE:
        return new URL(myUrlField.getText());
      default:
        assert false : "Should not be here";
        return null;
      }
    } catch (MalformedURLException e) {
      reportMalformedUrl(e);
      return null;
    }
  }

  private void reportMalformedUrl(Exception e) {
  }

  protected void setOptionsEnabled(boolean enabled) {
    if (mySecondaryOptionsComponent != null) {
      setEnabledTree(mySecondaryOptionsComponent, enabled);
    }
  }

  private void setEnabledTree(JComponent root, boolean isEnabled) {
    UIUtil.setEnabledTree(root, isEnabled);
  }

  private GPOptionGroup[] getOptionGroups() {
    return myOptions;
  }

  protected abstract void setFile(File file);

  protected void onSelectedFileChange(File file) {
    IStatus status = getFileStatus(file);
    if (status.isOK()) {
      setFile(file);
    } else {
      setFile(null);
    }
    showStatus(myFileLabel, status);
  }

  private static IStatus getFileStatus(File file) {
    if (file == null) {
      return new Status(IStatus.ERROR, "foo", IStatus.ERROR, "File does not exist", null);
    }
    if (!file.exists()) {
      return new Status(IStatus.ERROR, "foo", IStatus.ERROR, "File does not exist", null);
    }
    if (!file.canRead()) {
      return new Status(IStatus.ERROR, "foo", IStatus.ERROR, "File read error", null);
    }
    return Status.OK_STATUS;
  }

  private static void showStatus(JLabel label, IStatus status) {
    label.setOpaque(true);
    if (status.isOK()) {
      label.setForeground(Color.BLACK);
    } else {
      label.setForeground(Color.RED);
    }
    label.setText(status.getMessage());
  }

  class UrlFetcher {
    private final DefaultBooleanOption myProgressOption = new DefaultBooleanOption("");
    private JLabel myStatusLabel;
    private final Timer myTimer = new Timer();
    private boolean isFetching;
    private URL myUrl;
    private File myFetchedFile = null;

    public UrlFetcher() {
    }

    public void setStatusLabel(JLabel label) {
      myStatusLabel = label;
    }

    Component getComponent() {
      OptionsPageBuilder builder = new OptionsPageBuilder();
      return builder.createWaitIndicatorComponent(myProgressOption);
    }

    void setUrl(final URL url) {
      synchronized (myTimer) {
        myUrl = url;
        if (isFetching) {
          return;
        }
        reschedule();
      }
    }

    private void fetch() {
      myProgressOption.lock();
      myProgressOption.toggle();
      myProgressOption.commit();
      try {
        URLConnection connection = myUrl.openConnection();
        connection.connect();
        File tempFile = File.createTempFile("gp-import-", "");
        tempFile.deleteOnExit();
        InputStream from = myUrl.openStream();
        try {
          OutputStream to = new FileOutputStream(tempFile);
          try {
            byte[] buf = new byte[1024];
            while (true) {
              int r = from.read(buf);
              if (r == -1) {
                break;
              }
              to.write(buf, 0, r);
            }
            myFetchedFile = tempFile;
            setStatus(new Status(IStatus.OK, "foo", IStatus.OK, MessageFormat.format("Successfully fetched from {0}",
                new Object[] { myUrl }), null));
          } finally {
            to.flush();
            to.close();
          }
        } finally {
          from.close();
        }
      } catch (IOException e) {
        setStatus(new Status(IStatus.ERROR, "foo", IStatus.ERROR, MessageFormat.format("Failed to fetch from {0}\n{1}",
            new Object[] { myUrl, e.getMessage() }), e));
      } finally {
        isFetching = false;
        myProgressOption.lock();
        myProgressOption.toggle();
        myProgressOption.commit();
        onFetchComplete(myFetchedFile);
      }
    }

    private void setStatus(IStatus status) {
      AbstractFileChooserPage.showStatus(myStatusLabel, status);
    }

    protected void onFetchComplete(File file) {
      onSelectedFileChange(file);
    }

    private void reschedule() {
      if (myUrl == null || myUrl.getHost() == null || myUrl.getHost().length() == 0) {
        onFetchComplete(null);
        return;
      }
      myFetchedFile = null;
      onFetchComplete(null);
      isFetching = true;
      myTimer.schedule(new TimerTask() {
        final URL myUrlAtStart = myUrl;

        @Override
        public void run() {
          synchronized (myTimer) {
            if (!myUrlAtStart.equals(myUrl)) {
              reschedule();
            } else {
              fetch();
            }
          }
        }
      }, 3000);
    }
  }
}
