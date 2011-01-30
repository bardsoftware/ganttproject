package net.sourceforge.ganttproject.document;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class NetworkOptionPageProvider implements OptionPageProvider{

    private IGanttProject myProject;
    private UIFacade myUiFacade;

    @Override
    public void init(IGanttProject project, UIFacade uiFacade) {
        myProject = project;
        myUiFacade = uiFacade;
    }

    public GPOptionGroup[] getOptionGroups() {
        return myProject.getDocumentManager().getNetworkOptionGroups();
    }

    public String getPageID() {
        return "ftpexport";
    }

    public String toString() {
        return "FTP";
    }

    public boolean hasCustomComponent() {
        return true;
    }

    public Component buildPageComponent() {
        OptionsPageBuilder builder = new OptionsPageBuilder();
        builder.setI18N(new OptionsPageBuilder.I18N() {

            public String getPageTitle(String pageID) {
                return getValue("ftpexport");            
            }

            public String getPageDescription(String pageID) {
                return getValue("settingsFTPExport");
            }

            public String getOptionGroupLabel(GPOptionGroup group) {
                return getValue("ftpexport");
            }

            public String getOptionLabel(GPOptionGroup group, GPOption option) {
                final String id = option.getID();
                if (DocumentCreator.DIRECTORYNAME_OPTION_ID.equals(id)) {
                    return getValue("ftpdirectory");
                }
                if (DocumentCreator.PASSWORD_OPTION_ID.equals(id)) {
                    return getValue("ftppwd");
                }
                if (DocumentCreator.SERVERNAME_OPTION_ID.equals(id)) {
                    return getValue("ftpserver");
                }
                if (DocumentCreator.USERNAME_OPTION_ID.equals(id)) {
                    return getValue("ftpuser");
                }
                return super.getOptionLabel(group, option);
            }
        });
        final GPOptionGroup ftpGroup = myProject.getDocumentManager().getNetworkOptionGroups()[0];
        final DefaultStringOption usernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.USERNAME_OPTION_ID);
        final DefaultStringOption servernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.SERVERNAME_OPTION_ID);
        final DefaultStringOption dirnameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.DIRECTORYNAME_OPTION_ID);
        final DefaultStringOption passwordOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.PASSWORD_OPTION_ID);
 
        final JComponent optionsPane = builder.buildPage(myProject.getDocumentManager().getNetworkOptionGroups(), getPageID());
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
                    myUiFacade.showErrorDialog(e2);
//                    JOptionPane.showMessageDialog(, GanttLanguage
//                            .getInstance().getText("errorFTPConnection"),
//                            GanttLanguage.getInstance().getText("error"),
//                            JOptionPane.ERROR_MESSAGE);
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
        Box result = Box.createVerticalBox();
        result.add(optionsPane);
        result.add(new JButton(testConnectionAction));
        return result;
    }
    
    private boolean canEnableTestAction(GPOptionGroup ftpGroup) {
        final DefaultStringOption usernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.USERNAME_OPTION_ID);
        final DefaultStringOption servernameOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.SERVERNAME_OPTION_ID);
        final DefaultStringOption passwordOption = (DefaultStringOption) ftpGroup.getOption(DocumentCreator.PASSWORD_OPTION_ID);
        return servernameOption.getUncommitedValue()!=null && usernameOption.getUncommitedValue()!=null && passwordOption.getUncommitedValue()!=null;
    }

}
