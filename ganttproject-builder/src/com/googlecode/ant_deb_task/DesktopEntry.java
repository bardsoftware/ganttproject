package com.googlecode.ant_deb_task;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.io.*;
import java.util.*;

/**
 * Task that generates a desktop entry file used to add menu entries.
 *
 * @antTaskName desktopentry
 */
public class DesktopEntry extends Task
{
    private Properties _categories = new Properties ();
    private Properties _keys = new Properties ();

    private File _toFile;

    private Map _entries;
    private List _localizedEntries = new ArrayList ();

    private static final Set VALID_ONLY_SHOW_IN = new HashSet (
        Arrays.asList (new String[] {"GNOME", "KDE", "ROX", "XFCE", "Old", "Unity", "LXDE", "MATE", "Cinnamon"}));

    public static class LocalizedEntry
    {
        private String _key;
        private String _lang;
        private String _country;
        private String _encoding;
        private String _modifier;
        private String _value;

        public LocalizedEntry (String key)
        {
            _key = key;
        }

        public void setLang (String lang)
        {
            _lang = lang;
        }

        public void setCountry (String country)
        {
            _country = country;
        }

        public void setEncoding (String encoding)
        {
            _encoding = encoding;
        }

        public void setModifier (String modifier)
        {
            _modifier = modifier;
        }

        public void setValue (String value)
        {
            _value = value;
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer (_key);

            if (_lang != null)
            {
                buffer.append ('[');

                buffer.append (_lang.toLowerCase ());

                if (_country != null)
                {
                    buffer.append ('_');
                    buffer.append (_country.toUpperCase ());
                }

                if (_encoding != null)
                {
                    buffer.append ('.');
                    buffer.append (_encoding);
                }

                if (_modifier != null)
                {
                    buffer.append ('@');
                    buffer.append (_modifier);
                }

                buffer.append (']');
            }

            return buffer.toString ();
        }

        public String getValue ()
        {
            return _value;
        }
    }

    public static class Name extends LocalizedEntry
    {
        public Name ()
        {
            super ("Name");
        }
    }

    public static class GenericName extends LocalizedEntry
    {
        public GenericName ()
        {
            super ("GenericName");
        }
    }

    public static class Comment extends LocalizedEntry
    {
        public Comment ()
        {
            super ("Comment");
        }
    }

    public static class Icon extends LocalizedEntry
    {
        public Icon ()
        {
            super ("Icon");
        }
    }

    public static class Type extends EnumeratedAttribute
    {
        public String[] getValues ()
        {
            return new String[] {"Application", "Link", "Directory"};
        }
    }

    /**
     * The file where the desktop entry will be generated. The file name extension should be .desktop.
     *
     * @param toFile The file name, if exisits it will be overwritten.
     * @antTaskParamRequired true
     */
    public void setToFile(File toFile)
    {
        _toFile = toFile;
    }

    public void setType (Type type)
    {
        String typeValue = type.getValue();

        _entries.put("Type", typeValue);

        if (!typeValue.equals("Application"))
            _entries.remove("Terminal");
    }

    public void setName(String name)
    {
        _entries.put("Name", name);
    }

    public void setGenericName(String genericName)
    {
        _entries.put("GenericName", genericName);
    }

    public void setNoDisplay(boolean noDisplay)
    {
        _entries.put("NoDisplay", noDisplay ? "true" : "false");
    }

    public void setComment(String comment)
    {
        _entries.put("Comment", comment);
    }

    public void setIcon(String icon)
    {
        _entries.put("Icon", icon);
    }

    public void setOnlyShowIn(String onlyShowIn)
    {
        if (_entries.containsKey ("NotShowIn"))
            throw new BuildException("Only one of either OnlyShowIn or NotShowIn can be set!");

        String [] categories = onlyShowIn.split (";");
        for (int i = 0; i < categories.length; i++)
        {
            String category = categories[i];

            if (!VALID_ONLY_SHOW_IN.contains (category))
                throw new BuildException(category + " is not a valid OnlyShowIn category!");
        }

        _entries.put("OnlyShowIn", onlyShowIn);
    }

    public void setNotShowIn(String notShowIn)
    {
        if (_entries.containsKey ("OnlyShowIn"))
            throw new BuildException("Only one of either OnlyShowIn or NotShowIn can be set!");

        String [] categories = notShowIn.split (";");
        for (int i = 0; i < categories.length; i++)
        {
            String category = categories[i];

            if (!VALID_ONLY_SHOW_IN.contains (category))
                throw new BuildException(category + " is not a valid NotShowIn category!");
        }

        _entries.put("NotShowIn", notShowIn);
    }

    public void setTryExec(String tryExec)
    {
        _entries.put("TryExec", tryExec);
    }

    public void setExec(String exec)
    {
        _entries.put("Exec", exec);
    }

    public void setPath(String path)
    {
        _entries.put("Path", path);
    }

    public void setTerminal(boolean terminal)
    {
        _entries.put("Terminal", terminal ? "true" : "false");
    }

    public void setMimeType(String mimeType)
    {
        _entries.put("MimeType", mimeType);
    }

    public void setCategories(String categories)
    {
        validateCategories (categories);

        _entries.put("Categories", categories);
    }

    public void setUrl(String url)
    {
        _entries.put("URL", url);
    }

    public void addName(DesktopEntry.Name name)
    {
        _localizedEntries.add (name);
    }

    public void addGenericName(GenericName genericName)
    {
        _localizedEntries.add (genericName);
    }

    public void addComment(Comment comment)
    {
        _localizedEntries.add (comment);
    }

    public void addIcon(Icon icon)
    {
        _localizedEntries.add (icon);
    }

    public void init() throws BuildException
    {
        _entries = new LinkedHashMap();

        _entries.put("Version", "1.0");
        _entries.put("Type", "Application");
        _entries.put("Terminal", "false");

        try
        {
            InputStream categoriesStream = getClass ().getResourceAsStream ("DesktopEntryCategories.properties");
            _categories.load (categoriesStream);
            log ("Loaded " + _categories.size () + " properties from DesktopEntryCategories", Project.MSG_VERBOSE);

            InputStream keysStream = getClass ().getResourceAsStream ("DesktopEntryKeys.properties");
            _keys.load (keysStream);
            log ("Loaded " + _keys.size () + " properties from DesktopEntryKeys", Project.MSG_VERBOSE);
        }
        catch (Exception e)
        {
            throw new BuildException("Cannot load properties file!", e);
        }
    }

    public void execute() throws BuildException
    {
        try
        {
            _toFile.getParentFile().mkdirs();
            log ("Generating desktop entry to: " + _toFile.getAbsolutePath ());

            for (int i = 0; i < _localizedEntries.size (); i++)
            {
                LocalizedEntry localizedEntry = (LocalizedEntry) _localizedEntries.get (i);

                _entries.put (localizedEntry.toString (), localizedEntry.getValue ());
            }

            validateKeys ();

            PrintWriter out = new UnixPrintWriter(_toFile);

            out.println("[Desktop Entry]");

            Iterator keys = _entries.keySet().iterator();
            while (keys.hasNext())
            {
                String key = (String) keys.next();
                String value = (String) _entries.get(key);

                out.print(key);
                out.print('=');
                out.println(value);
            }

            out.close();
        }
        catch (FileNotFoundException e)
        {
            throw new BuildException(e);
        }
    }

    private void validateCategories(String categories)
    {
        int mainCnt = 0;

        Set categorySet = new HashSet (Arrays.asList (categories.split (";")));
        for (Iterator iterator = categorySet.iterator (); iterator.hasNext ();)
        {
            String category = (String) iterator.next ();

            String type = _categories.getProperty ("category." + category);

            if (type == null)
                throw new BuildException("Unknown category: " + category);

            if ("main".equals (type))
                mainCnt++;

            String require = _categories.getProperty ("category." + category + ".require");
            if (require != null)
            {
                if (!categorySet.contains (require))
                    throw new BuildException("Category " + category + " also requires " + require);
            }
            else
            {
                require = _categories.getProperty ("category." + category + ".require.and");

                if (require != null)
                {
                    String [] requireAnd = require.split (";");
                    for (int j = 0; j < requireAnd.length; j++)
                    {
                        if (!categorySet.contains (requireAnd[j]))
                            throw new BuildException("Category " + category + " also requires " + requireAnd[j]);
                    }
                }
                else
                {
                    require = _categories.getProperty ("category." + category + ".require.or");

                    if (require != null)
                    {
                        boolean found = false;
                        String [] requireOr = require.split (";");
                        for (int j = 0; j < requireOr.length; j++)
                        {
                            if (categorySet.contains (requireOr[j]))
                            {
                                found = true;
                                break;
                            }
                        }

                        if (!found)
                            throw new BuildException("Category " + category + " also requires one of " + require);
                    }
                }
            }
        }

        if (mainCnt == 0)
            throw new BuildException ("At least one main category should be specified!");

        if (mainCnt > 1)
            log ("Multiple main categories specified, the entry may appear under multiple menus!");
    }

    private void validateKeys()
    {
        String type = (String) _entries.get ("Type");
        Iterator keys = _entries.keySet ().iterator ();
        while (keys.hasNext ())
        {
            String key = (String) keys.next ();

            if ("Type".equals (key))
                continue;

            int idxSquare = key.indexOf ('[');

            if (idxSquare != -1)
            {
                String baseKey = key.substring (0, idxSquare);

                if (!_entries.containsKey (baseKey))
                    throw new BuildException ("Unlocalized key " + baseKey + " also needed for " + key);

                continue;
            }

            boolean found = false;
            String [] typeArr = _keys.getProperty ("key." + key).split (",");
            for (int i = 0; i < typeArr.length; i++)
            {
                String allowedType = typeArr[i];

                if (type.equals (allowedType))
                {
                    found = true;
                    break;
                }
            }

            if (!found)
                throw new BuildException("Key " + key + " not allowed for desktop entry of type " + type);
        }

        Iterator propertyKeys = _keys.keySet ().iterator ();
        while (propertyKeys.hasNext ())
        {
            String property = (String) propertyKeys.next ();

            if (property.endsWith (".required"))
            {
                String [] requiredTypeArr = _keys.getProperty (property).split (",");

                for (int i = 0; i < requiredTypeArr.length; i++)
                {
                    String requiredType = requiredTypeArr[i];

                    if (type.equals (requiredType))
                    {
                        String key = property.substring (4, property.length () - 9);

                        if (!_entries.containsKey (key))
                            throw new BuildException("Key " + key + " is required for desktop entries of type " + type);

                        break;
                    }
                }
            }
        }
    }
}

