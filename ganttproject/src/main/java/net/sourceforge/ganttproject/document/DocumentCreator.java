/*
 * Created on 20.08.2003
 *
 */
package net.sourceforge.ganttproject.document;

import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.StringOption;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.storage.DocumentKt;
import biz.ganttproject.storage.DocumentUri;
import biz.ganttproject.storage.cloud.GPCloudDocument;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.webdav.HttpDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.document.webdav.WebDavStorageImpl;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.ParserFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is a helper class, to create new instances of Document easily. It
 * chooses the correct implementation based on the given path.
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
public class DocumentCreator implements DocumentManager {
  private final IGanttProject myProject;

  private final UIFacade myUIFacade;

  private final ParserFactory myParserFactory;

  private final WebDavStorageImpl myWebDavStorage;
  private final StringOption myWorkingDirectory = new StringOptionImpl("working-dir", "working-dir", "dir");

  private final GPOptionGroup myOptionGroup;

  private final GPOptionGroup myWebDavOptionGroup;
  private final Logger myLogger = GPLogger.getLogger(DocumentManager.class);
  /** List containing the Most Recent Used documents */
  private final DocumentsMRU myMRU = new DocumentsMRU(50);
  private final File myUserDir;
  private final GPOptionGroup myLocalStorageOptions;

  public DocumentCreator(IGanttProject project, UIFacade uiFacade, ParserFactory parserFactory) {
    myProject = project;
    myUIFacade = uiFacade;
    myParserFactory = parserFactory;
    myWebDavStorage = new WebDavStorageImpl(project, uiFacade);
    myOptionGroup = new GPOptionGroup("", new GPOption[] {
        myWorkingDirectory,
        myWebDavStorage.getLegacyLastWebDAVDocumentOption(),
        myWebDavStorage.getWebDavLockTimeoutOption()
    });
    myLocalStorageOptions = new GPOptionGroup("localStorage", DocumentKt.getDefaultLocalFolderOption());
    myWebDavOptionGroup = new GPOptionGroup("webdav", new GPOption[] {
        myWebDavStorage.getServersOption(),
        myWebDavStorage.getLastWebDavDocumentOption(),
        myWebDavStorage.getWebDavReleaseLockOption(),
        myWebDavStorage.getProxyOption()
    });
    myUserDir = DocumentKt.getUserDir();
  }

  /**
   * Creates an HttpDocument if path starts with "http://" or "https://";
   * creates a FileDocument otherwise.
   *
   * @param path
   *          path to the document
   * @return an implementation of the interface Document
   */
  private Document createDocument(String path) {
    return createDocument(path, myUserDir, null, null);
  }

  /**
   * Creates an HttpDocument if path starts with "http://" or "https://";
   * creates a FileDocument otherwise.
   *
   * @param path
   *          path to the document
   * @param user
   *          username
   * @param pass
   *          password
   * @return an implementation of the interface Document
   * @throws Exception when the specified protocol is not supported
   */
  private Document createDocument(String path, File relativePathRoot, String user, String pass) {
    assert path != null;
    path = path.trim();
    String lowerPath = path.toLowerCase();
    if (lowerPath.startsWith("http://") || lowerPath.startsWith("https://")) {
      try {
        if (user == null && pass == null) {
          WebDavServerDescriptor server = myWebDavStorage.findServer(path);
          if (server != null) {
            user = server.getUsername();
            pass = server.getPassword();
          }
        }
        return new HttpDocument(path, user, pass, myWebDavStorage.getProxyOption());
      } catch (IOException e) {
        GPLogger.log(e);
        return null;
      } catch (WebDavException e) {
        GPLogger.log(e);
        return null;
      }
    } else if (lowerPath.startsWith("cloud://")) {
      var patchedUrl = DocumentKt.asDocumentUrl(lowerPath);
      if (patchedUrl.component2().equals("cloud")) {
        return new GPCloudDocument(
          null,
          DocumentUri.LocalDocument.createPath(patchedUrl.component1().getPath()).getParent().getFileName(),
          patchedUrl.component1().getHost(),
          DocumentUri.LocalDocument.createPath(patchedUrl.component1().getPath()).getFileName(),
          null
        );
      }
    } else if (lowerPath.startsWith("ftp:")) {
      return new FtpDocument(path, myFtpUserOption, myFtpPasswordOption);
    } else if (!lowerPath.startsWith("file://") && path.contains("://")) {
      // Generate error for unknown protocol
      throw new RuntimeException("Unknown protocol: " + path.substring(0, path.indexOf("://")));
    }
    File file = new File(path);
    if (file.toPath().isAbsolute()) {
      return new FileDocument(file);
    }
    File relativeFile = new File(relativePathRoot, path);
    return new FileDocument(relativeFile);
  }

  @Override
  public Document getDocument(String path) {
    Document physicalDocument = createDocument(path);
    Document proxyDocument = new ProxyDocument(this, physicalDocument, myProject, myUIFacade, getVisibleFields(),
        getResourceVisibleFields(), getParserFactory());
    return proxyDocument;
  }

  @Override
  public ProxyDocument getProxyDocument(Document physicalDocument) {
    return new ProxyDocument(this, physicalDocument, myProject, myUIFacade, getVisibleFields(),
        getResourceVisibleFields(), getParserFactory());
  }

  @Override
  public Document newUntitledDocument() throws IOException {
    for (int i = 1;; i++) {
      String filename = GanttLanguage.getInstance().formatText("document.storage.untitledDocument", i);
      File untitledFile = new File(DocumentKt.getDefaultLocalFolder(), filename);
      if (untitledFile.exists()) {
        continue;
      }
      return getDocument(untitledFile.getAbsolutePath());
    }
  }

  @Override
  public Document newDocument(String path) throws IOException {
    return createDocument(path, DocumentKt.getDefaultLocalFolder(), null, null);
  }

  protected ColumnList getVisibleFields() {
    return null;
  }

  protected ColumnList getResourceVisibleFields() {
    return null;
  }

  protected ParserFactory getParserFactory() {
    return myParserFactory;
  }

  String createTemporaryFile() throws IOException {
    return getWorkingDirectoryFile().getAbsolutePath();
  }

  @Override
  public void changeWorkingDirectory(File directory) {
    assert directory.isDirectory();
    myWorkingDirectory.lock();
    myWorkingDirectory.setValue(directory.getAbsolutePath());
    myWorkingDirectory.commit();
  }

  @Override
  public String getWorkingDirectory() {
    return myWorkingDirectory.getValue();
  }


  private File getWorkingDirectoryFile() {
    return new File(getWorkingDirectory());
  }

  @Override
  public GPOptionGroup getOptionGroup() {
    return myOptionGroup;
  }

  @Override
  public FTPOptions getFTPOptions() {
    return myFtpOptions;
  }

  @Override
  public GPOptionGroup[] getNetworkOptionGroups() {
    return new GPOptionGroup[] { myFtpOptions, myOptionGroup, myWebDavOptionGroup, myLocalStorageOptions };
  }

  @Override
  public DocumentStorageUi getWebDavStorageUi() {
    return myWebDavStorage;
  }

  private final StringOption myFtpUserOption = new StringOptionImpl("user-name", "ftp", "ftpuser");
  private final StringOption myFtpServerNameOption = new StringOptionImpl("server-name", "ftp", "ftpurl");
  private final StringOption myFtpDirectoryNameOption = new StringOptionImpl("directory-name", "ftp", "ftpdir");
  private final StringOption myFtpPasswordOption = new StringOptionImpl("password", "ftp", "ftppwd");
  private final FTPOptions myFtpOptions = new FTPOptions("ftp", new GPOption[] { myFtpUserOption,
      myFtpServerNameOption, myFtpDirectoryNameOption, myFtpPasswordOption }) {
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

    @Override
    public String getTagName() {
      return myLegacyTagName;
    }

    @Override
    public String getAttributeName() {
      return myLegacyAttrName;
    }

    @Override
    public void loadValue(String legacyValue) {
      loadPersistentValue(legacyValue);
    }
  }

  @Override
  public List<String> getRecentDocuments() {
    return Lists.newArrayList(myMRU.iterator());
  }

  @Override
  public void addListener(DocumentMRUListener listener) {
    myMRU.addListener(listener);
  }

  @Override
  public void addToRecentDocuments(Document document) {
    myMRU.add(document.getPath(), true);
  }

  @Override
  public void addToRecentDocuments(String value) {
    myMRU.add(value, false);
  }

  @Override
  public void clearRecentDocuments() {
    myMRU.clear();
  }
}
