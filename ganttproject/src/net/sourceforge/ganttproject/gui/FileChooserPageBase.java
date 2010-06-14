package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;


import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.options.GPOptionChoicePanel;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;
import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class FileChooserPageBase implements WizardPage {
    public static final int FILE_SOURCE = 0;
    public static final int URL_SOURCE = 1;
    private static final String PREF_SELECTED_FILE = "selected_file";
    private static final String PREF_SELECTED_URL = "selected_url";

    private JPanel myComponent;
    private TextFieldAndFileChooserComponent myChooser;
    private JTextField myUrlField;
    private OptionsPageBuilder myOptionsBuilder;
    private JPanel mySecondaryOptionsComponent;
    private int ourSelectedSource = FILE_SOURCE;
    private final WizardImpl myWizard;
    private final boolean isUrlChooserEnabled;
    private final JLabel myFileLabel = new JLabel("  ");
    private final JLabel myUrlLabel = new JLabel("  ");
    private final Preferences myPreferences;
    protected FileChooserPageBase(WizardImpl wizard, Preferences prefs) {
        this(wizard, prefs, true);
    }

    protected FileChooserPageBase(WizardImpl wizard, Preferences prefs, boolean enableUrlChooser) {
        myPreferences = prefs;
        isUrlChooserEnabled = enableUrlChooser;
        myWizard = wizard;
        myOptionsBuilder = new OptionsPageBuilder();
        mySecondaryOptionsComponent = new JPanel(new BorderLayout());
        mySecondaryOptionsComponent.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    }

    protected abstract String getFileChooserTitle();

    protected int getFileChooserSelectionMode() {
        return JFileChooser.FILES_AND_DIRECTORIES;
    }

    public Component getComponent() {
        myComponent = new JPanel(new BorderLayout());
        myChooser = new TextFieldAndFileChooserComponent(GanttLanguage.getInstance().getText("file")+":",
                getFileChooserTitle()) {
            protected void onFileChosen(File file) {
                FileChooserPageBase.this.onFileChosen(file);
                super.onFileChosen(file);
            }
            protected void showFileStatus(IStatus status) {
                FileChooserPageBase.setStatus(myFileLabel, status);
            }
        };
        myChooser.setFileSelectionMode(getFileChooserSelectionMode());
        if (myPreferences.get(PREF_SELECTED_FILE, null) != null) {
            myChooser.setFile(new File(myPreferences.get(PREF_SELECTED_FILE, null)));
        }
        JComponent contentPanel = new JPanel(new BorderLayout());
        if (!isUrlChooserEnabled) {
            contentPanel.add(myChooser.getComponent(), BorderLayout.NORTH);
        } else {
            final UrlFetcher urlFetcher = new UrlFetcher() {
                protected void onFetchComplete(File file) {
                    super.onFetchComplete(file);
                    onFileChosen(file);
                }
            };
            myUrlField = new JTextField();
            if (myPreferences.get(PREF_SELECTED_URL, null) != null) {
                myUrlField.setText(myPreferences.get(PREF_SELECTED_URL, null));
            }
            Box urlBox = Box.createVerticalBox();
            urlBox.add(myUrlField);
            urlBox.add(myUrlLabel);
            myUrlField.getDocument().addDocumentListener(new DocumentListener() {
                public void removeUpdate(DocumentEvent e) {
                    onChange();
                }
                public void insertUpdate(DocumentEvent e) {
                    onChange();
                }
                public void changedUpdate(DocumentEvent e) {
                    onChange();
                }
                private void onChange() {
                    urlFetcher.setUrl(getSelectedUrl());
                }
            });

            Box fileBox = Box.createVerticalBox();
            fileBox.add(myChooser.getComponent());
            fileBox.add(myFileLabel);

            Action fileSourceAction = new AbstractAction(GanttLanguage.getInstance().getText("file")) {
                public void actionPerformed(ActionEvent e) {
                    ourSelectedSource = FILE_SOURCE;
                }
            };
            Action urlSourceAction = new AbstractAction(GanttLanguage.getInstance().getText("url")) {
                public void actionPerformed(ActionEvent e) {
                    ourSelectedSource = URL_SOURCE;
                    urlFetcher.setStatusLabel(myUrlLabel);
                }
            };
            Action[] importSourceActions = new Action[] {fileSourceAction, urlSourceAction};
            JComponent[] importSourceComponents = new JComponent[] {fileBox, urlBox};
            GPOptionChoicePanel sourceChoicePanel = new GPOptionChoicePanel();

            Box sourceBox = Box.createVerticalBox();
            sourceBox.add(sourceChoicePanel.getComponent(
                    importSourceActions, importSourceComponents, ourSelectedSource));
            sourceBox.add(Box.createVerticalStrut(5));
            sourceBox.add(urlFetcher.getComponent());
            contentPanel.add(sourceBox, BorderLayout.NORTH);
            contentPanel.add(mySecondaryOptionsComponent, BorderLayout.CENTER);
        }
        myComponent.add(contentPanel, BorderLayout.NORTH);
        return myComponent;
    }

    public void setActive(boolean b) {
        GPOptionGroup[] optionGroups = getOptionGroups();
        if (b == false) {
            for (int i=0; i<optionGroups.length; i++) {
                optionGroups[i].commit();
            }
            if (myChooser.getFile() != null) {
                myPreferences.put(PREF_SELECTED_FILE, myChooser.getFile().getAbsolutePath());
            }
            myPreferences.put(PREF_SELECTED_URL, myUrlField.getText());
        } else {
            for (int i=0; i<optionGroups.length; i++) {
                optionGroups[i].lock();
            }
            if (mySecondaryOptionsComponent!=null){
                mySecondaryOptionsComponent.removeAll();
            }
            mySecondaryOptionsComponent.add(createSecondaryOptionsPanel(), BorderLayout.NORTH);
            myChooser.setFileFilter(createFileFilter());
            UIUtil.repackWindow(myComponent);
        }
    }

    protected Component createSecondaryOptionsPanel() {
        return myOptionsBuilder.buildPlanePage(getOptionGroups());
    }

    protected void setSelectedUrl(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                myChooser.setFile(new File(url.toURI()));
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            myUrlField.setText(url.toString());
        }
    }

    protected URL getSelectedUrl() {
        try {
            switch (ourSelectedSource) {
            case FILE_SOURCE:
                return myChooser.getSelectedURL();
            case URL_SOURCE: {
                return new URL(myUrlField.getText());
            }
            default: {
                assert false : "Should not be here";
                return null;
            }
            }
        } catch (MalformedURLException e) {
            reportMalformedUrl(e);
            return null;
        }
    }

    private void reportMalformedUrl(Exception e) {
    }

    protected void setOptionsEnabled(boolean enabled) {
        if (mySecondaryOptionsComponent!=null) {
            setEnabledTree(mySecondaryOptionsComponent, enabled);
        }
    }

    private void setEnabledTree(JComponent root, boolean isEnabled) {
        UIUtil.setEnabledTree(root, isEnabled);
    }


    protected abstract FileFilter createFileFilter();

    protected abstract GPOptionGroup[] getOptionGroups();

    protected void onFileChosen(File file) {
        myWizard.adjustButtonState();
    }

    private static void setStatus(JLabel label, IStatus status) {
        label.setOpaque(true);
        if (status.isOK()) {
            label.setForeground(Color.BLACK);
            //myStatusLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN.darker(), 1));
        }
        else {
            label.setForeground(Color.RED);
            //myStatusLabel.setBorder(BorderFactory.createLineBorder(Color.RED.darker(), 1));
        }
        label.setText(status.getMessage());
    }

    static class UrlFetcher {
        private DefaultBooleanOption myProgressOption = new DefaultBooleanOption("");
        private JLabel myStatusLabel;
        private Timer myTimer = new Timer();
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
            synchronized(myTimer) {
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
                        long total = 0;
                        while (true) {
                          int r = from.read(buf);
                          if (r == -1) {
                            break;
                          }
                          to.write(buf, 0, r);
                          total += r;
                        }
                        myFetchedFile = tempFile;
                        setStatus(new Status(IStatus.OK, "foo",
                                MessageFormat.format("Successfully fetched from {0}", myUrl)));
                    }
                    finally {
                        to.flush();
                        to.close();
                    }
                }
                finally {
                    from.close();
                }
            }
            catch (IOException e) {
                setStatus(new Status(IStatus.ERROR, "foo",
                        MessageFormat.format("Failed to fetch from {0}\n{1}", myUrl, e.getMessage())));
            }
            finally {
                isFetching = false;
                myProgressOption.lock();
                myProgressOption.toggle();
                myProgressOption.commit();
                onFetchComplete(myFetchedFile);
            }
        }

        private void setStatus(IStatus status) {
            FileChooserPageBase.setStatus(myStatusLabel, status);
        }

        protected void onFetchComplete(File file) {
        }

        private void reschedule() {
            if (myUrl == null || myUrl.getHost() == null || myUrl.getHost().length()==0) {
                return;
            }
            myFetchedFile = null;
            onFetchComplete(null);
            isFetching = true;
            myTimer.schedule(new TimerTask() {
                final URL myUrlAtStart = myUrl;
                public void run() {
                    synchronized(myTimer) {
                        if (!myUrlAtStart.equals(myUrl)) {
                            reschedule();
                        }
                        else {
                            fetch();
                        }
                    }
                }
            }, 3000);

        }

    }
}
