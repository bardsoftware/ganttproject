package com.googlecode.ant_deb_task;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.*;
import org.apache.tools.tar.TarOutputStream;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import java.security.MessageDigest;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Task that creates a Debian package.
 *
 * @antTaskName deb
 */
public class Deb extends Task
{
    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9+\\-.]+");

    public static class Description extends ProjectComponent
    {
        private String _synopsis;
        private String _extended = "";

        public String getSynopsis ()
        {
            return _synopsis;
        }

        public void setSynopsis (String synopsis)
        {
            _synopsis = synopsis.trim ();
        }

        public void addText (String text)
        {
            _extended += getProject ().replaceProperties (text);
        }

        public String getExtended ()
        {
            return _extended;
        }

        public String getExtendedFormatted ()
        {
            StringBuffer buffer = new StringBuffer (_extended.length ());

            String lines[] = _extended.split ("\n");

            int start = 0;

            for (int i = 0; i < lines.length; i++)
            {
                String line = lines[i].trim ();

                if (line.length () > 0)
                    break;

                start++;
            }

            int end = lines.length;

            for (int i = lines.length - 1; i >= 0; i--)
            {
                String line = lines[i].trim ();

                if (line.length () > 0)
                    break;

                end--;
            }

            for (int i = start; i < end; i++)
            {
                String line = lines[i].trim ();

                buffer.append (' ');
                buffer.append (line.length () == 0 ? "." : line);
                buffer.append ('\n');
            }

            buffer.deleteCharAt (buffer.length () - 1);

            return buffer.toString ();
        }
    }

    public static class Version extends ProjectComponent
    {
        private static final Pattern UPSTREAM_VERSION_PATTERN = Pattern.compile("[0-9][A-Za-z0-9.+\\-:~]*");
        private static final Pattern DEBIAN_VERSION_PATTERN = Pattern.compile("[A-Za-z0-9+.~]+");

        private int _epoch = 0;
        private String _upstream;
        private String _debian = "1";

        public void setEpoch(int epoch)
        {
            _epoch = epoch;
        }

        public void setUpstream(String upstream)
        {
            _upstream = upstream.trim ();

            if (!UPSTREAM_VERSION_PATTERN.matcher (_upstream).matches ())
                throw new BuildException("Invalid upstream version number!");
        }

        public void setDebian(String debian)
        {
            _debian = debian.trim ();

            if (_debian.length() > 0 && !DEBIAN_VERSION_PATTERN.matcher (_debian).matches ())
                throw new BuildException("Invalid debian version number!");
        }

        public String toString()
        {
            StringBuffer version = new StringBuffer();

            if (_epoch > 0)
            {
                version.append(_epoch);
                version.append(':');
            }
            else if (_upstream.indexOf(':') > -1)
                throw new BuildException("Upstream version can contain colons only if epoch is specified!");

            version.append(_upstream);

            if (_debian.length() > 0)
            {
                version.append('-');
                version.append(_debian);
            }
            else if (_upstream.indexOf('-') > -1)
                throw new BuildException("Upstream version can contain hyphens only if debian version is specified!");

            return version.toString();
        }
    }

    public static class Maintainer extends ProjectComponent
    {
        private String _name;
        private String _email;

        public void setName (String name)
        {
            _name = name.trim ();
        }

        public void setEmail (String email)
        {
            _email = email.trim ();
        }

        public String toString()
        {
            if (_name == null || _name.length () == 0)
                return _email;

            StringBuffer buffer = new StringBuffer (_name);

            buffer.append (" <");
            buffer.append (_email);
            buffer.append (">");

            return buffer.toString ();
        }
    }

    public static class Changelog extends ProjectComponent
    {
        public static class Format extends EnumeratedAttribute
        {
            public String[] getValues ()
            {
                // XML format will be added when supported
                return new String[] {"plain" /* , "xml" */};
            }
        }

        private static final String STANDARD_FILENAME = "changelog.gz";
        private static final String DEBIAN_FILENAME = "changelog.Debian.gz";

        private String _file;
        private Changelog.Format _format;
        private boolean _debian;

        public Changelog()
        {
            _debian = false;
            _format = new Changelog.Format();
            _format.setValue("plain");
        }

        public void setFile (String file)
        {
            _file = file.trim ();
        }

        public String getFile()
        {
            return _file;
        }

        public void setFormat (Changelog.Format format)
        {
            _format = format;
        }

        public Changelog.Format getFormat()
        {
            return _format;
        }

        public void setDebian (boolean debian)
        {
            _debian = debian;
        }

        public boolean isDebian ()
        {
            return _debian;
        }

        public String getTargetFilename ()
        {
            return _debian ? DEBIAN_FILENAME : STANDARD_FILENAME;
        }
    }

    public static class Section extends EnumeratedAttribute
    {
        private static final String[] PREFIXES = new String[] {"", "contrib/", "non-free/"};
        private static final String[] BASIC_SECTIONS = new String[] {"admin", "base", "comm", "devel", "doc", "editors", "electronics", "embedded", "games", "gnome", "graphics", "hamradio", "interpreters", "kde", "libs", "libdevel", "mail", "math", "misc", "net", "news", "oldlibs", "otherosfs", "perl", "python", "science", "shells", "sound", "tex", "text", "utils", "web", "x11"};

        private List sections = new ArrayList (PREFIXES.length * BASIC_SECTIONS.length);

        public Section ()
        {
            for (int i = 0; i < PREFIXES.length; i++)
            {
                String prefix = PREFIXES[i];

                for (int j = 0; j < BASIC_SECTIONS.length; j++)
                {
                    String basicSection = BASIC_SECTIONS[j];

                    sections.add (prefix + basicSection);
                }
            }
        }

        public String[] getValues ()
        {
            return (String[]) sections.toArray (new String[sections.size()]);
        }
    }

    public static class Priority extends EnumeratedAttribute
    {
        public String[] getValues ()
        {
            return new String[] {"required", "important", "standard", "optional", "extra"};
        }
    }

    private File _toDir;

    private String _debFilenameProperty = "";

    private String _package;
    private String _version;
    private Deb.Version _versionObj;
    private String _section;
    private String _priority = "extra";
    private String _architecture = "all";
    private String _depends;
    private String _preDepends;
    private String _recommends;
    private String _suggests;
    private String _enhances;
    private String _conflicts;
    private String _provides;
    private String _replaces;
    private String _maintainer;
    private URL _homepage;
    private Deb.Maintainer _maintainerObj;
    private Deb.Description _description;

    private Set _conffiles = new HashSet ();
    private Set _changelogs = new HashSet ();
    private List _data = new ArrayList();

    private File _preinst;
    private File _postinst;
    private File _prerm;
    private File _postrm;
    private File _config;
    private File _templates;
    private File _triggers;

    private File _tempFolder;

    private long _installedSize = 0;
    private SortedSet _dataFolders;

    private static final Tar.TarCompressionMethod GZIP_COMPRESSION_METHOD = new Tar.TarCompressionMethod ();
    private static final Tar.TarLongFileMode GNU_LONGFILE_MODE = new Tar.TarLongFileMode ();

    static
    {
        GZIP_COMPRESSION_METHOD.setValue ("gzip");
        GNU_LONGFILE_MODE.setValue(Tar.TarLongFileMode.GNU);
    }

    public void setToDir (File toDir)
    {
        _toDir = toDir;
    }

    public void setDebFilenameProperty(String debFilenameProperty)
    {
        _debFilenameProperty = debFilenameProperty.trim();
    }

    public void setPackage (String packageName)
    {
        if (!PACKAGE_NAME_PATTERN.matcher(packageName).matches())
            throw new BuildException("Invalid package name!");

        _package = packageName;
    }

    public void setVersion (String version)
    {
        _version = version;
    }

    public void setSection (Section section)
    {
        _section = section.getValue();
    }

    public void setPriority (Priority priority)
    {
        _priority = priority.getValue();
    }

    public void setArchitecture (String architecture)
    {
        _architecture = sanitize(architecture, _architecture);
    }

    public void setDepends (String depends)
    {
        _depends = sanitize(depends);
    }

    public void setPreDepends (String preDepends)
    {
        _preDepends = sanitize(preDepends);
    }

    public void setRecommends (String recommends)
    {
        _recommends = sanitize(recommends);
    }

    public void setSuggests (String suggests)
    {
        _suggests = sanitize(suggests);
    }

    public void setEnhances (String enhances)
    {
        _enhances = sanitize(enhances);
    }

    public void setConflicts (String conflicts)
    {
        _conflicts = sanitize(conflicts);
    }

    public void setProvides (String provides)
    {
   	    _provides = sanitize(provides);
    }

    public void setReplaces(String replaces)
    {
   	    _replaces = sanitize(replaces);
    }

    public void setMaintainer (String maintainer)
    {
        _maintainer = sanitize(maintainer);
    }

    public void setHomepage (String homepage)
    {
        try
        {
            _homepage = new URL (homepage);
        }
        catch (MalformedURLException e)
        {
            throw new BuildException ("Invalid homepage, must be a URL: " + homepage, e);
        }
    }

    public void setPreinst (File preinst)
    {
        _preinst = preinst;
    }

    public void setPostinst (File postinst)
    {
        _postinst = postinst;
    }

    public void setPrerm (File prerm)
    {
        _prerm = prerm;
    }

    public void setPostrm (File postrm)
    {
        _postrm = postrm;
    }

    public void setConfig (File config)
    {
        _config = config;
    }

    public void setTemplates (File templates)
    {
        _templates = templates;
    }

    public void setTriggers(File triggers)
    {
        _triggers = triggers;
    }

    public void addConfFiles (TarFileSet conffiles)
    {
        _conffiles.add (conffiles);
        _data.add (conffiles);
    }

    public void addChangelog(Deb.Changelog changelog)
    {
        _changelogs.add (changelog);
    }

    public void addDescription (Deb.Description description)
    {
        _description = description;
    }

    public void add (TarFileSet resourceCollection)
    {
        _data.add(resourceCollection);
    }

    public void addVersion(Deb.Version version)
    {
        _versionObj = version;
    }

    public void addMaintainer(Deb.Maintainer maintainer)
    {
        _maintainerObj = maintainer;
    }

    private void writeControlFile (File controlFile, long installedSize) throws FileNotFoundException
    {
        log ("Generating control file to: " + controlFile.getAbsolutePath (), Project.MSG_VERBOSE);

        PrintWriter control = new UnixPrintWriter (controlFile);

        control.print ("Package: ");
        control.println (_package);

        control.print ("Version: ");
        control.println (_version);

        if (_section != null)
        {
            control.print ("Section: ");
            control.println (_section);
        }

        if (_priority != null)
        {
            control.print ("Priority: ");
            control.println (_priority);
        }

        control.print ("Architecture: ");
        control.println (_architecture);

        if (_depends != null)
        {
            control.print ("Depends: ");
            control.println (_depends);
        }

        if (_preDepends != null)
        {
            control.print ("Pre-Depends: ");
            control.println (_preDepends);
        }

        if (_recommends != null)
        {
            control.print ("Recommends: ");
            control.println (_recommends);
        }

        if (_suggests != null)
        {
            control.print ("Suggests: ");
            control.println (_suggests);
        }

        if (_enhances != null)
        {
            control.print ("Enhances: ");
            control.println (_enhances);
        }

        if (_conflicts != null)
        {
            control.print ("Conflicts: ");
            control.println (_conflicts);
        }

        if (_provides != null)
        {
       	    control.print ("Provides: ");
       	    control.println (_provides);
        }

        if (_replaces != null)
        {
       	    control.print ("Replaces: ");
       	    control.println (_replaces);
        }

        if (installedSize > 0)
        {
            control.print ("Installed-Size: ");
            control.println (installedSize / 1024L);
        }

        control.print ("Maintainer: ");
        control.println (_maintainer);

        if (_homepage != null)
        {
            control.print ("Homepage: ");
            control.println(_homepage.toExternalForm());
        }

        control.print ("Description: ");
        control.println (_description.getSynopsis ());
        control.println (_description.getExtendedFormatted ());

        control.close ();
    }

    private File createMasterControlFile () throws IOException
    {
        File controlFile = new File (_tempFolder, "control");

        writeControlFile (controlFile, _installedSize);

        File md5sumsFile = new File (_tempFolder, "md5sums");
        File conffilesFile = new File (_tempFolder, "conffiles");

        File masterControlFile = new File (_tempFolder, "control.tar.gz");

        Tar controlTar = new Tar ();
        controlTar.setProject (getProject ());
        controlTar.setTaskName (getTaskName ());
        controlTar.setDestFile (masterControlFile);
        controlTar.setCompression (GZIP_COMPRESSION_METHOD);

        addFileToTar (controlTar, controlFile, "control", "644");
        addFileToTar (controlTar, md5sumsFile, "md5sums", "644");

        if (conffilesFile.length () > 0)
            addFileToTar (controlTar, conffilesFile, "conffiles", "644");

        if (_preinst != null)
            addFileToTar (controlTar, _preinst, "preinst", "755");

        if (_postinst != null)
            addFileToTar (controlTar, _postinst, "postinst", "755");

        if (_prerm != null)
            addFileToTar (controlTar, _prerm, "prerm", "755");

        if (_postrm != null)
            addFileToTar (controlTar, _postrm, "postrm", "755");

        if (_config != null)
            addFileToTar (controlTar, _config, "config", "755");

        if (_templates != null)
            addFileToTar (controlTar, _templates, "templates", "644");

        if (_triggers != null)
            addFileToTar (controlTar, _triggers, "triggers", "644");

        controlTar.perform ();

        deleteFileCheck(controlFile);

        return masterControlFile;
    }

    private void addFileToTar(Tar tar, File file, String fullpath, String fileMode)
    {
        TarFileSet controlFileSet = tar.createTarFileSet ();

        controlFileSet.setFile (file);
        controlFileSet.setFullpath ("./" + fullpath);

        controlFileSet.setFileMode (fileMode);
        controlFileSet.setUserName ("root");
        controlFileSet.setGroup ("root");
    }

    public void execute () throws BuildException
    {
        try
        {
            if (_versionObj != null)
                _version = _versionObj.toString ();

            if (_maintainerObj != null)
                _maintainer = _maintainerObj.toString ();

            _tempFolder = createTempFolder();

            processChangelogs ();
            scanData ();

            File debFile = new File (_toDir, _package + "_" + _version + "_" + _architecture + ".deb");

            File dataFile = createDataFile ();

            File masterControlFile = createMasterControlFile ();

            log ("Writing deb file to: " + debFile.getAbsolutePath());
            BuildDeb.buildDeb (debFile, masterControlFile, dataFile);

            if (_debFilenameProperty.length() > 0)
                getProject().setProperty(_debFilenameProperty, debFile.getAbsolutePath());

            deleteFileCheck(masterControlFile);
            deleteFileCheck(dataFile);

            deleteFolderCheck(_tempFolder);
        }
        catch (IOException e)
        {
            throw new BuildException (e);
        }
    }

    private File createDataFile () throws IOException
    {
        File dataFile = new File (_tempFolder, "data.tar.gz");

        Tar dataTar = new Tar ();
        dataTar.setProject (getProject ());
        dataTar.setTaskName (getTaskName ());
        dataTar.setDestFile (dataFile);
        dataTar.setCompression (GZIP_COMPRESSION_METHOD);
        dataTar.setLongfile(GNU_LONGFILE_MODE);

        if ( _data.size () > 0 )
        {
            // add folders
            for (Iterator dataFoldersIter = _dataFolders.iterator (); dataFoldersIter.hasNext ();)
            {
                String targetFolder = (String) dataFoldersIter.next ();

                TarFileSet targetFolderSet = dataTar.createTarFileSet ();

                targetFolderSet.setFile (_tempFolder);
                targetFolderSet.setFullpath (targetFolder);
                targetFolderSet.setUserName ("root");
                targetFolderSet.setGroup ("root");
            }

            // add actual data
	        for (int i = 0; i < _data.size (); i++)
	        {
	            TarFileSet data = (TarFileSet) _data.get (i);
	
	            if (data.getUserName() == null || data.getUserName().trim().length() == 0)
	                data.setUserName ("root");
	
	            if (data.getGroup() == null || data.getGroup().trim().length() == 0)
	                data.setGroup ("root");
	
	            dataTar.add (data);
	        }
	
	        dataTar.execute ();
        }
        else
        {
        	// create an empty data.tar.gz file which is still a valid tar
        	TarOutputStream tarStream = new TarOutputStream(
        		new GZipOutputStream(
        			new BufferedOutputStream(new FileOutputStream(dataFile)),
        			Deflater.BEST_COMPRESSION
	        	)
        	);
        	tarStream.close();
        }

        return dataFile;
    }

    private File createTempFolder() throws IOException
    {
        File tempFile = File.createTempFile ("deb", ".dir");
        String tempFolderName = tempFile.getAbsolutePath ();
        deleteFileCheck(tempFile);

        tempFile = new File (tempFolderName, "removeme");

        if (!tempFile.mkdirs ())
            throw new IOException("Cannot create folder(s): " + tempFile.getAbsolutePath());

        deleteFileCheck(tempFile);

        log ("Temp folder: " + tempFolderName, Project.MSG_VERBOSE);

        return new File (tempFolderName);
    }

    private void scanData()
    {
        try
        {
            Set existingDirs = new HashSet ();

            _installedSize = 0;
            PrintWriter md5sums = new UnixPrintWriter (new File (_tempFolder, "md5sums"));
            PrintWriter conffiles = new UnixPrintWriter (new File (_tempFolder, "conffiles"));
            _dataFolders = new TreeSet ();

            Iterator filesets = _data.iterator();
            while (filesets.hasNext())
            {
                TarFileSet fileset = (TarFileSet) filesets.next();

                normalizeTargetFolder(fileset);

                String fullPath = fileset.getFullpath (getProject ());
                String prefix = fileset.getPrefix (getProject ());

                String [] fileNames = getFileNames(fileset);
                for (int i = 0; i < fileNames.length; i++)
                {
                    String targetName;
                    String fileName = fileNames[i];

                    File file = new File (fileset.getDir (getProject ()), fileName);

                    if (fullPath.length () > 0)
                        targetName = fullPath;
                    else
                        targetName = prefix + fileName;

                    if (file.isDirectory ())
                    {
                        log ("existing dir: " + targetName, Project.MSG_DEBUG);
                        existingDirs.add (targetName);
                    }
                    else
                    {
                        // calculate installed size in bytes
                        _installedSize += file.length ();

                        // calculate and collect md5 sums
                        md5sums.print (getFileMd5 (file));
                        md5sums.print (' ');
                        md5sums.println (targetName.substring(2));

                        // get target folder names, and collect them (to be added to _data)
                        File targetFile = new File(targetName);
                        File parentFolder = targetFile.getParentFile ();
                        while (parentFolder != null)
                        {
                            String parentFolderPath = parentFolder.getPath ();

                            if (".".equals(parentFolderPath))
                                parentFolderPath = "./";

                            parentFolderPath = parentFolderPath.replace('\\', '/');

                            if (!existingDirs.contains (parentFolderPath) && !_dataFolders.contains (parentFolderPath))
                            {
                                log ("adding dir: " + parentFolderPath + " for " + targetName, Project.MSG_DEBUG);
                                _dataFolders.add (parentFolderPath);
                            }

                            parentFolder = parentFolder.getParentFile ();
                        }

                        // write conffiles
                        if (_conffiles.contains (fileset))
                        {
                            // NOTE: targetName is "./path/file" so substring(1) removes the leading '.'
                            conffiles.println (targetName.substring(1));
                        }
                    }
                }
            }

            for (Iterator iterator = existingDirs.iterator (); iterator.hasNext ();)
            {
                String existingDir = (String) iterator.next ();

                if (_dataFolders.contains (existingDir))
                {
                    log ("removing existing dir " + existingDir, Project.MSG_DEBUG);
                    _dataFolders.remove (existingDir);
                }
            }

            md5sums.close ();
            conffiles.close ();
        }
        catch (Exception e)
        {
            throw new BuildException (e);
        }
    }

    private void normalizeTargetFolder(TarFileSet fileset)
    {
        String fullPath = fileset.getFullpath (getProject ());
        String prefix = fileset.getPrefix (getProject ());

        if (fullPath.length() > 0)
        {
            if (fullPath.startsWith("/"))
                fullPath = "." + fullPath;
            else if (!fullPath.startsWith("./"))
                fullPath = "./" + fullPath;

            fileset.setFullpath(fullPath.replace('\\', '/'));
        }

        if (prefix.length() > 0)
        {
            if (!prefix.endsWith ("/"))
                prefix += '/';

            if (prefix.startsWith("/"))
                prefix = "." + prefix;
            else if (!prefix.startsWith("./"))
                prefix = "./" + prefix;

            fileset.setPrefix(prefix.replace('\\', '/'));
        }
    }

    private String[] getFileNames(FileSet fs)
    {
        DirectoryScanner ds = fs.getDirectoryScanner(fs.getProject());

        String[] directories = ds.getIncludedDirectories();
        String[] filesPerSe = ds.getIncludedFiles();

        String[] files = new String [directories.length + filesPerSe.length];

        System.arraycopy(directories, 0, files, 0, directories.length);
        System.arraycopy(filesPerSe, 0, files, directories.length, filesPerSe.length);

        return files;
    }

    private String getFileMd5(File file)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance ("MD5");
            FileInputStream inputStream = new FileInputStream (file);
            byte[] buffer = new byte[1024];

            while (true)
            {
                int read = inputStream.read (buffer);

                if (read == -1)
                    break;

                md5.update (buffer, 0, read);
            }

            inputStream.close();

            byte[] md5Bytes = md5.digest ();
            StringBuffer md5Buffer = new StringBuffer (md5Bytes.length * 2);
            for (int i = 0; i < md5Bytes.length; i++)
            {
                String hex = Integer.toHexString (md5Bytes[i] & 0x00ff);

                if (hex.length () == 1)
                    md5Buffer.append ('0');

                md5Buffer.append (hex);
            }

            return md5Buffer.toString ();
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }

    private void processChangelogs() throws IOException
    {
        for (Iterator iter = _changelogs.iterator (); iter.hasNext (); )
        {
            processChangelog ((Deb.Changelog) iter.next ());
        }
    }

    private void processChangelog (Deb.Changelog changelog) throws IOException
    {
        // Compress file
        File file = new File(changelog.getFile ());
        File temp = File.createTempFile ("changelog", ".gz");
        gzip(file, temp, Deflater.BEST_COMPRESSION, GZipOutputStream.FS_UNIX);

        // Determine path
        StringBuffer path = new StringBuffer ("usr/share/doc/");
        path.append (_package).append ('/');
        path.append (changelog.getTargetFilename ());

        // Add file to data
        TarFileSet fileSet = new TarFileSet ();
        fileSet.setProject (getProject ());
        fileSet.setFullpath (path.toString ());
        fileSet.setFile (temp);
        fileSet.setFileMode ("0644");
        _data.add (fileSet);
    }

    private static void gzip (File input, File output, int level, byte fileSystem)
        throws IOException
    {
        GZipOutputStream out = null;
        InputStream in = null;

        try
        {
            out = new GZipOutputStream (new FileOutputStream (output), level);
            out.setFileSystem (fileSystem);

            in = new FileInputStream(input);
            byte[] buffer = new byte[8 * 1024];

            int len;
            while ((len = in.read (buffer, 0, buffer.length)) > 0)
            {
                out.write(buffer, 0, len);
            }

            out.finish ();
        }
        finally
        {
            if (in != null)
            {
                in.close ();
            }

            if (out != null)
            {
                out.close ();
            }
        }
    }

    private boolean deleteFolder(File folder)
    {
        if (folder.isDirectory())
        {
            File[] children = folder.listFiles();
            for (int i = 0; i < children.length; i++)
            {
                if (!deleteFolder(children[i]))
                    return false;
            }
        }

        return folder.delete();
    }

    private void deleteFolderCheck(File folder) throws IOException
    {
        if (!deleteFolder(folder))
            throw new IOException("Cannot delete file: " + folder.getAbsolutePath());
    }

    private void deleteFileCheck(File file) throws IOException
    {
        if (!file.delete ())
            throw new IOException("Cannot delete file: " + file.getAbsolutePath());
    }

    private String sanitize(String value) {
        return sanitize(value, null);
    }

    private String sanitize(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        value = value.trim();

        return value.length() == 0 ? defaultValue : value;
    }
}
