/*
 * Created on 20.08.2003
 *
 */
package net.sourceforge.ganttproject.document;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.webdav.HttpDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.document.webdav.WebDavStorageImpl;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.parser.ParserFactory;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.StringOption;
import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.time.CalendarFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

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
  private final DocumentsMRU myMRU = new DocumentsMRU(5);

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
    myWebDavOptionGroup = new GPOptionGroup("webdav", new GPOption[] {
        myWebDavStorage.getServersOption(),
        myWebDavStorage.getLastWebDavDocumentOption(),
        myWebDavStorage.getWebDavReleaseLockOption(),
        myWebDavStorage.getProxyOption()
    });
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
    return createDocument(path, null, null);
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
   * @throws an
   *           Exception when the specified protocol is not supported
   */
  private Document createDocument(String path, String user, String pass) {
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
    } else if (lowerPath.startsWith("ftp:")) {
      return new FtpDocument(path, myFtpUserOption, myFtpPasswordOption);
    } else if (!lowerPath.startsWith("file://") && path.contains("://")) {
      // Generate error for unknown protocol
      throw new RuntimeException("Unknown protocol: " + path.substring(0, path.indexOf("://")));
    }
    return new FileDocument(new File(path));
  }

  @Override
  public Document getDocument(String path) {
    Document physicalDocument = createDocument(path);
    Document proxyDocument = new ProxyDocument(this, physicalDocument, myProject, myUIFacade, getVisibleFields(),
        getResourceVisibleFields(), getParserFactory());
    return proxyDocument;
  }

  @Override
  public Document getProxyDocument(Document physicalDocument) {
    Document proxyDocument = new ProxyDocument(this, physicalDocument, myProject, myUIFacade, getVisibleFields(),
        getResourceVisibleFields(), getParserFactory());
    return proxyDocument;
  }

  @Override
  public Document newAutosaveDocument() throws IOException {
    File tempFile = File.createTempFile("_ganttproject_autosave", ".gan");
    return getDocument(tempFile.getAbsolutePath());
  }

  public static Runnable createAutosaveCleanup() {
    long now = CalendarFactory.newCalendar().getTimeInMillis();
    final File tempDir = getTempDir();
    final long cutoff;
    try {
      File optionsFile = GanttOptions.getOptionsFile();
      if (!optionsFile.exists()) {
        return null;
      }
      GPLogger.log("Options file:" + optionsFile.getAbsolutePath());
      BasicFileAttributes attrs = Files.readAttributes(optionsFile.toPath(), BasicFileAttributes.class);
      FileTime accessTime = attrs.lastAccessTime();
      cutoff = Math.min(accessTime.toMillis(), now);
    } catch (IOException e) {
      GPLogger.log(e);
      return null;
    }
    return new Runnable() {
      @Override
      public void run() {
        GPLogger.log("Deleting old auto-save files");
        deleteAutosaves();
      }

      private void deleteAutosaves() {
        // Let's find autosaves created before launch of this GP instance
        File[] previousAutosaves = tempDir.listFiles(new FileFilter() {
          @Override
          public boolean accept(File file) {
            return file.getName().startsWith("_ganttproject_autosave") && file.lastModified() < cutoff;
          }
        });
        for (File f : previousAutosaves) {
          f.deleteOnExit();
        }
      }
    };
  }

  private FileSystem getAutosaveZipFs() {
    try {
      File tempDir = getTempDir();
      if (tempDir == null) {
        return null;
      }
      File autosaveFile = new File(tempDir, "_ganttproject_autosave.zip");
      if (autosaveFile.exists() && !autosaveFile.canWrite()) {
        myLogger.warning(String.format(
            "Autosave file %s is not writable", autosaveFile.getAbsolutePath()));
        return null;
      }
      URI uri = new URI("jar:file:" + autosaveFile.toURI().getPath());
      return FileSystems.newFileSystem(uri, ImmutableMap.<String, Object>of("create", "true"));
    } catch (Throwable e) {
      myLogger.log(Level.SEVERE, "Failure when creating ZIP FS for autosaves", e);
      return null;
    }
  }

  private static File getTempDir() {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    if (tempDir.exists() && tempDir.isDirectory() && tempDir.canWrite()) {
      return tempDir;
    }
    try {
      File tempFile = File.createTempFile("_ganttproject_autosave", ".empty");
      tempDir = tempFile.getParentFile();
      if (tempDir.exists() && tempDir.isDirectory() && tempDir.canWrite()) {
        return tempDir;
      }
    } catch (IOException e) {
      GPLogger.getLogger(DocumentManager.class).log(Level.WARNING, "Can't get parent of the temp file", e);
    }
    GPLogger.getLogger(DocumentManager.class).warning("Failed to find temporary directory");
    return null;
  }

  @Override
  public Document getLastAutosaveDocument(Document priorTo) throws IOException {
    File f = File.createTempFile("tmp", "");
    File directory = f.getParentFile();
    File files[] = directory.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File f, String arg1) {
        return arg1.startsWith("_ganttproject_autosave");
      }
    });
    Arrays.sort(files, new Comparator<File>() {
      @Override
      public int compare(File left, File right) {
        return Long.valueOf(left.lastModified()).compareTo(Long.valueOf(right.lastModified()));
      }
    });
    if (files.length == 0) {
      return null;
    }
    if (priorTo == null) {
      return getDocument(files[files.length - 1].getAbsolutePath());
    }
    for (int i = files.length - 1; i >= 0; i--) {
      if (files[i].getName().equals(priorTo.getFileName())) {
        return i > 0 ? getDocument(files[i - 1].getAbsolutePath()) : null;
      }
    }
    return null;
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
    File tempFile = File.createTempFile("project", ".gan", getWorkingDirectoryFile());
    return tempFile.getAbsolutePath();
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
    return new GPOptionGroup[] { myFtpOptions, myOptionGroup, myWebDavOptionGroup };
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
