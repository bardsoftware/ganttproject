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

import net.sourceforge.ganttproject.gui.options.GPOptionChoicePanel;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.projectwizard.WizardImpl;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.Preferences;

public abstract class FileChooserPageBase implements WizardPage {
    public static final int FILE_SOURCE = 0;
    public static final int URL_SOURCE = 1;
    protected static final String PREF_SELECTED_FILE = "selected_file";
    private static final String PREF_SELECTED_URL = "selected_url";

    private JPanel myComponent;
    private TextFieldAndFileChooserComponent myChooser;
    private JTextField myUrlField;
    private final OptionsPageBuilder myOptionsBuilder;
    private final JPanel mySecondaryOptionsComponent;
    private int ourSelectedSource = FileChooserPageBase.FILE_SOURCE;
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
        myChooser = new TextFieldAndFileChooserComponent(
                GanttLanguage.getInstance().getText("file")+":", getFileChooserTitle()) {
            protected void onFileChosen(File file) {
                IStatus status = FileChooserPageBase.this.onSelectedFileChange(file);
                if (status.isOK()) {
                    setFile(file);
                } else {
                    onSelectedUrlChange(null);
                }
                FileChooserPageBase.setStatus(myFileLabel, status);
            }
        };
        myChooser.setFileSelectionMode(getFileChooserSelectionMode());
        JComponent contentPanel = new JPanel(new BorderLayout());
        if (!isUrlChooserEnabled) {
            contentPanel.add(myChooser.getComponent(), BorderLayout.NORTH);
        } else {
            final UrlFetcher urlFetcher = new UrlFetcher() {
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
                    ourSelectedSource = FileChooserPageBase.FILE_SOURCE;
                    myChooser.tryFile();
                }
            };
            Action urlSourceAction = new AbstractAction(GanttLanguage.getInstance().getText("url")) {
                public void actionPerformed(ActionEvent e) {
                    ourSelectedSource = FileChooserPageBase.URL_SOURCE;
                    urlFetcher.setStatusLabel(myUrlLabel);
                    urlFetcher.setUrl(getSelectedUrl());
                    onSelectedUrlChange(getSelectedUrl());
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
        }
        contentPanel.add(mySecondaryOptionsComponent, BorderLayout.CENTER);
        myComponent.add(contentPanel, BorderLayout.NORTH);
        return myComponent;
    }

    protected void loadPreferences() {
        if (myPreferences.get(FileChooserPageBase.PREF_SELECTED_FILE, null) != null) {
            myChooser.setFile(new File(myPreferences.get(FileChooserPageBase.PREF_SELECTED_FILE, null)));
        }
        if (myUrlField != null && myPreferences.get(FileChooserPageBase.PREF_SELECTED_URL, null) != null) {
            myUrlField.setText(myPreferences.get(FileChooserPageBase.PREF_SELECTED_URL, null));
        }
    }

    public void setActive(boolean b) {
        GPOptionGroup[] optionGroups = getOptionGroups();
        if (b == false) {
            for (int i=0; i<optionGroups.length; i++) {
                optionGroups[i].commit();
            }
            if (myChooser.getFile() != null) {
                myPreferences.put(FileChooserPageBase.PREF_SELECTED_FILE, myChooser.getFile().getAbsolutePath());
            }
            if (myUrlField != null) {
                myPreferences.put(FileChooserPageBase.PREF_SELECTED_URL, myUrlField.getText());
            }
        } else {
            for (int i=0; i<optionGroups.length; i++) {
                optionGroups[i].lock();
            }
            if (mySecondaryOptionsComponent!=null){
                mySecondaryOptionsComponent.removeAll();
            }
            mySecondaryOptionsComponent.add(createSecondaryOptionsPanel(), BorderLayout.NORTH);
            myChooser.setFileFilter(createFileFilter());
            loadPreferences();
            onSelectedUrlChange(getSelectedUrl());
            UIUtil.repackWindow(myComponent);
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

    private URL getSelectedUrl() {
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


    protected void onSelectedUrlChange(URL selectedUrl) {
        myWizard.adjustButtonState();
    }

    protected IStatus setSelectedFile(File file) {
        try {
            onSelectedUrlChange(new URL("file://" + file.getAbsolutePath()));
            return new Status(IStatus.OK, "foo", "  ");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return new Status(IStatus.ERROR, "foo", e.getMessage());
        }
    }

    protected IStatus onSelectedFileChange(File file) {
        if (file == null) {
            return new Status(IStatus.ERROR, "foo", "File does not exist");
        }
        if (!file.exists()) {
            return new Status(IStatus.ERROR, "foo", "File does not exist");
        }
        if (!file.canRead()) {
            return new Status(IStatus.ERROR, "foo", "File read error");
        }
        return setSelectedFile(file);
    }

    private static void setStatus(JLabel label, IStatus status) {
        label.setOpaque(true);
        if (status.isOK()) {
            label.setForeground(Color.BLACK);
        }
        else {
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
                        setStatus(new Status(
                                IStatus.OK, "foo", IStatus.OK,
                                MessageFormat.format("Successfully fetched from {0}", new Object[] {myUrl}),
                                null));
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
                setStatus(new Status(
                        IStatus.ERROR, "foo", IStatus.ERROR,
                        MessageFormat.format("Failed to fetch from {0}\n{1}", new Object[] {
                                myUrl, e.getMessage()
                        }),
                        e));
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
            if (!onSelectedFileChange(file).isOK()) {
                onSelectedUrlChange(null);
            }
        }

        private void reschedule() {
            if (myUrl == null || myUrl.getHost() == null || myUrl.getHost().length()==0) {
                onFetchComplete(null);
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
