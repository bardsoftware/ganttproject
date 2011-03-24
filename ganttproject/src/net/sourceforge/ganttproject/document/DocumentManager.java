/*
 * Created on 12.03.2005
 */
package net.sourceforge.ganttproject.document;

import java.io.File;

import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.IntegerOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;

/**
 * @author bard
 */
public interface DocumentManager {
    Document getDocument(String path);

    void addToRecentDocuments(Document document);

    Document getDocument(String path, String userName, String password);

    void changeWorkingDirectory(File parentFile);

    String getWorkingDirectory();

    GPOptionGroup getOptionGroup();
    FTPOptions getFTPOptions();
    GPOptionGroup[] getNetworkOptionGroups();
    StringOption getLastWebDAVDocumentOption();
    IntegerOption getWebDavLockTimeoutOption();

    abstract class FTPOptions extends GPOptionGroup {
        public FTPOptions(String id, GPOption[] options) {
            super(id, options);
        }
        public abstract StringOption getServerName();
        public abstract StringOption getUserName();
        public abstract StringOption getDirectoryName();
        public abstract StringOption getPassword();
    }
}
