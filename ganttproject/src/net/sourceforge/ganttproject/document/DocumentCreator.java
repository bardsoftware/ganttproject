/*
 * Created on 20.08.2003
 *
 */
package net.sourceforge.ganttproject.document;

import java.io.File;
import java.io.IOException;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.options.model.DefaultIntegerOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.IntegerOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.parser.ParserFactory;

/**
 * This is a helper class, to create new instances of Document easily. It
 * chooses the correct implementation based on the given path.
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
public class DocumentCreator implements DocumentManager {
    private IGanttProject myProject;

    private UIFacade myUIFacade;

    private ParserFactory myParserFactory;

    public DocumentCreator(IGanttProject project, UIFacade uiFacade,
            ParserFactory parserFactory) {
        myProject = project;
        myUIFacade = uiFacade;
        setParserFactory(parserFactory);
    }

    /**
     * Creates an HttpDocument if path starts with "http://" or "https://";
     * creates a FileDocument otherwise.
     *
     * @param path
     *            path to the document
     * @return an implementation of the interface Document
     */
    private Document createDocument(String path) {
        return createDocument(path, null, null);
    }

    /**
     * Creates an HttpDocument if path starts with "http://" or "https://";
     * creates a FileDocument otherwise.
     *
     * @param path
     *            path to the document
     * @param user
     *            username
     * @param pass
     *            password
     * @return an implementation of the interface Document
     * @throws an Exception when the specified protocol is not supported
     */
    private Document createDocument(String path, String user, String pass) {
        assert path!=null;
        path = path.trim();
        String lowerPath = path.toLowerCase();
        if (lowerPath.startsWith("http://")
                || lowerPath.startsWith("https://")) {
            return new HttpDocument(path, user, pass);
        }
        else if (lowerPath.startsWith("ftp:")) {
            return new FtpDocument(path, myFtpUserOption, myFtpPasswordOption);
        }
        else if (!lowerPath.startsWith("file://") && path.contains("://")) {
            // Generate error for unknown protocol
            throw new RuntimeException("Unknown protocol: " + path.substring(0, path.indexOf("://")));
        }
        return new FileDocument(new File(path));
    }

    public Document getDocument(String path) {
        Document physicalDocument = createDocument(path);
        Document proxyDocument = new ProxyDocument(this, physicalDocument, myProject,
                myUIFacade, getVisibleFields(), getParserFactory());
        return proxyDocument;
    }

    public Document getDocument(String path, String userName, String password) {
        Document physicalDocument = createDocument(path, userName, password);
        Document proxyDocument = new ProxyDocument(this, physicalDocument, myProject, myUIFacade, getVisibleFields(), getParserFactory());
        return proxyDocument;
    }

    protected TableHeaderUIFacade getVisibleFields() {
        return null;
    }

    public void addToRecentDocuments(Document document) {
        // TODO Auto-generated method stub

    }

    protected void setParserFactory(ParserFactory myParserFactory) {
        this.myParserFactory = myParserFactory;
    }

    protected ParserFactory getParserFactory() {
        return myParserFactory;
    }

    String createTemporaryFile() throws IOException {
        File tempFile = File.createTempFile("project", ".gan", getWorkingDirectoryFile());
        return tempFile.getAbsolutePath();
    }

    public void changeWorkingDirectory(File directory) {
        assert directory.isDirectory();
        myWorkingDirectory.lock();
        myWorkingDirectory.setValue(directory.getAbsolutePath());
        myWorkingDirectory.commit();
    }
    public String getWorkingDirectory() {
        return myWorkingDirectory.getValue();
    }
    public StringOption getLastWebDAVDocumentOption() {
        return myLastWebDAVDocument;
    }
    public IntegerOption getWebDavLockTimeoutOption() {
        return myWebDavLockTimeoutOption;
    }

    private File getWorkingDirectoryFile() {
        return new File(getWorkingDirectory());
    }
    public GPOptionGroup getOptionGroup() {
        return myOptionGroup;
    }
    public FTPOptions getFTPOptions() {
        return myFtpOptions;
    }
    public GPOptionGroup[] getNetworkOptionGroups() {
        return new GPOptionGroup[] {myFtpOptions};
    }


    private final StringOption myWorkingDirectory = new StringOptionImpl("working-dir", "working-dir", "dir");
    private final StringOption myLastWebDAVDocument = new StringOptionImpl("last-webdav-document", "", "");
    private final IntegerOption myWebDavLockTimeoutOption = new LockTimeoutOption();

    private final GPOptionGroup myOptionGroup = new GPOptionGroup("",
        new GPOption[] {myWorkingDirectory, myLastWebDAVDocument, myWebDavLockTimeoutOption});

    private final StringOption myFtpUserOption = new StringOptionImpl("user-name", "ftp", "ftpuser");
    private final StringOption myFtpServerNameOption = new StringOptionImpl("server-name", "ftp", "ftpurl");
    private final StringOption myFtpDirectoryNameOption = new StringOptionImpl("directory-name", "ftp", "ftpdir");
    private final StringOption myFtpPasswordOption = new StringOptionImpl("password", "ftp", "ftppwd");
    private final FTPOptions myFtpOptions = new FTPOptions("ftp", new GPOption[] {myFtpUserOption, myFtpServerNameOption, myFtpDirectoryNameOption, myFtpPasswordOption}) {
        @Override
        public StringOption getDirectoryName() {
            return myFtpDirectoryNameOption;
        }

        @Override
        public StringOption getPassword() {
            return myFtpPasswordOption;
        }

        @Override
        public StringOption getServerName() {
            return myFtpServerNameOption;
        }

        @Override
        public StringOption getUserName() {
            return myFtpUserOption;
        }

    };

    static final String USERNAME_OPTION_ID = "user-name";
    static final String SERVERNAME_OPTION_ID = "server-name";
    static final String DIRECTORYNAME_OPTION_ID = "directory-name";
    static final String PASSWORD_OPTION_ID = "password";

    private static class StringOptionImpl extends DefaultStringOption implements GP1XOptionConverter {
        private final String myLegacyTagName;
        private final String myLegacyAttrName;

        private StringOptionImpl(String optionName, String legacyTagName, String legacyAttrName) {
            super(optionName);
            myLegacyTagName = legacyTagName;
            myLegacyAttrName = legacyAttrName;
        }
        public String getTagName() {
            return myLegacyTagName;
        }

        public String getAttributeName() {
            return myLegacyAttrName;
        }

        public void loadValue(String legacyValue) {
            loadPersistentValue(legacyValue);
        }
    }

    private static class LockTimeoutOption extends DefaultIntegerOption implements GP1XOptionConverter {
        public LockTimeoutOption() {
            super("webdav.lockTimeout", -1);
        }
        @Override
        public String getTagName() {
            return "lockdavminutes";
        }

        @Override
        public String getAttributeName() {
            return "value";
        }

        @Override
        public void loadValue(String legacyValue) {
            try {
                setValue(Integer.parseInt(legacyValue), true);
            } catch (NumberFormatException e) {
                GPLogger.log(e);
                setValue(-1, true);
            }
        }

    }

}
