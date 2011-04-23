package net.sourceforge.ganttproject.document;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.I18N;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class NetworkOptionPageProvider extends OptionPageProviderBase {

    public NetworkOptionPageProvider() {
        super("impex.ftp");
    }

    public GPOptionGroup[] getOptionGroups() {
        return getProject().getDocumentManager().getNetworkOptionGroups();
    }

    public boolean hasCustomComponent() {
        return true;
    }

    public Component buildPageComponent() {
        OptionsPageBuilder builder = new OptionsPageBuilder();
        final GPOptionGroup ftpGroup = getProject().getDocumentManager().getNetworkOptionGroups()[0];
        ftpGroup.setTitled(false);
        I18N i18n = new OptionsPageBuilder.I18N();

        final DefaultStringOption usernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.USERNAME_OPTION_ID);
        ftpGroup.setI18Nkey(i18n.getCanonicalOptionLabelKey(usernameOption), "ftpuser");

        final DefaultStringOption servernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.SERVERNAME_OPTION_ID);
        ftpGroup.setI18Nkey(i18n.getCanonicalOptionLabelKey(servernameOption), "ftpserver");

        final DefaultStringOption dirnameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.DIRECTORYNAME_OPTION_ID);
        ftpGroup.setI18Nkey(i18n.getCanonicalOptionLabelKey(dirnameOption), "ftpdirectory");

        final DefaultStringOption passwordOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.PASSWORD_OPTION_ID);
        ftpGroup.setI18Nkey(i18n.getCanonicalOptionLabelKey(passwordOption), "ftppwd");

        final JComponent optionsPane = builder.buildPage(getProject().getDocumentManager().getNetworkOptionGroups(), getPageID());
        final Action testConnectionAction = new AbstractAction() {
            {
                putValue(Action.NAME, GanttLanguage.getInstance().getText("testFTPConnection"));
                setEnabled(canEnableTestAction(ftpGroup));
            }
            public void actionPerformed(ActionEvent e) {
                StringBuffer urlString = new StringBuffer();
                urlString.append("ftp://");
                urlString.append(usernameOption.getUncommitedValue()==null ? "":usernameOption.getUncommitedValue());
                urlString.append(passwordOption.getUncommitedValue()==null ? "" : ":"+passwordOption.getUncommitedValue());
                urlString.append("@");
                urlString.append(servernameOption.getUncommitedValue());
                urlString.append("/");
                urlString.append(dirnameOption.getUncommitedValue());
                urlString.append("/");
                URL url = null;
                try {
                    url = new URL(urlString.toString() + "test.txt");
                    URLConnection urlc = url.openConnection();
                    OutputStream os = urlc.getOutputStream();
                    os.write(("This is GanttProject +++ I was here!")
                            .getBytes());
                    os.close();
                    JOptionPane.showMessageDialog(optionsPane, GanttLanguage
                            .getInstance().getText("successFTPConnection"),
                            GanttLanguage.getInstance().getText("success"),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e2) {
                    getUiFacade().showErrorDialog(e2);
                } finally {

                }
            }
        };
        ChangeValueListener listener = new ChangeValueListener() {
            public void changeValue(ChangeValueEvent event) {
                testConnectionAction.setEnabled(canEnableTestAction(ftpGroup));
            }
        };
        servernameOption.addChangeValueListener(listener);
        usernameOption.addChangeValueListener(listener);
        passwordOption.addChangeValueListener(listener);
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(new EmptyBorder(5, 5, 5, 5));
        result.add(optionsPane, BorderLayout.NORTH);
        JButton testConnectionButton = new JButton(testConnectionAction);
        testConnectionButton.setAlignmentX(SwingConstants.RIGHT);

        JPanel connectionWrapper = new JPanel(new BorderLayout());
        connectionWrapper.add(testConnectionButton, BorderLayout.NORTH);

        result.add(connectionWrapper, BorderLayout.CENTER);
        return result;
    }

    private boolean canEnableTestAction(GPOptionGroup ftpGroup) {
        final DefaultStringOption usernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.USERNAME_OPTION_ID);
        final DefaultStringOption servernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.SERVERNAME_OPTION_ID);
        final DefaultStringOption passwordOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.PASSWORD_OPTION_ID);
        return servernameOption.getUncommitedValue()!=null && usernameOption.getUncommitedValue()!=null && passwordOption.getUncommitedValue()!=null;
    }

}
