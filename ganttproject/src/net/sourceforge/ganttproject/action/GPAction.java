/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action;

import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.eclipse.core.runtime.Platform;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

/**
 * @author bard
 */
public abstract class GPAction extends AbstractAction implements GanttLanguage.Listener {
    public static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    /** Location of the icon files */
    public static final String ICON_FILE_DIRECTORY = "/icons";

    protected boolean iconVisible = true;

    private Icon myIcon = null;

    private final String myName;

    private static Properties ourKeyboardProperties;

    private static Properties ourIconProperties;

    private static GanttLanguage language = GanttLanguage.getInstance();

    protected GPAction() {
        this(null);
    }

    public GPAction(String name) {
        // TODO use icon size given in options as default size
        this(name, "16");
    }

    protected GPAction(String name, String iconSize) {
        super(name);
        myName = name;
        updateIcon(iconSize);
        updateName();
        updateTooltip();
        language.addListener(this);
        if(name != null) {
            putValue(Action.ACCELERATOR_KEY, getKeyStroke(name));
        }
    }

    public Icon getIconOnMouseOver() {
        return (Icon) getValue(Action.SMALL_ICON);
    }

    private void updateIcon(String iconSize) {
        Icon icon = createIcon(iconSize);
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
            myIcon = icon;
        }
    }

    protected final Icon createIcon(String iconSize) {
        if (iconSize == null || false == iconVisible) {
            return null;
        }
        String customIcon = getCustomIconPath();
        String resourcePath;
        if (customIcon == null) {
            resourcePath = MessageFormat.format("{0}/{1}{2}.gif", ICON_FILE_DIRECTORY, getIconFilePrefix(), iconSize);
        } else {
            resourcePath = MessageFormat.format("{0}/{1}x{1}/{2}", ICON_FILE_DIRECTORY, iconSize, customIcon);
        }
        URL resource = getClass().getResource(resourcePath);
        return resource == null ? null : new ImageIcon(resource);
    }

    protected String getLocalizedName() {
        return getID() == null ? null : getI18n(getID());
    }

    protected String getID() {
        return myName;
    }

    protected String getActionName() {
        String localizedName = getLocalizedName();
        return localizedName == null ? "" : language.correctLabel(localizedName);
    }

    protected static String getI18n(String key) {
        return language.getText(key);
    }

    protected String getIconFilePrefix() {
        return null;
    }

    protected final void setIconVisible(boolean isVisible) {
        iconVisible = isVisible;
        putValue(Action.SMALL_ICON, iconVisible ? myIcon : null);
    }

    protected final void updateName() {
        String localizedName = getLocalizedName();
        if (localizedName == null) {
            localizedName = String.valueOf(getValue(Action.NAME));
        }
        if (localizedName != null) {
            int bucksPos = localizedName.indexOf('$');
            if (bucksPos >= 0) {
                // Get name without the $ in it
                localizedName = new StringBuffer(localizedName).deleteCharAt(bucksPos).toString();
            }
            putValue(Action.NAME, localizedName);
            if (bucksPos >= 0) {
                // Activate mnemonic key
                putValue(Action.MNEMONIC_KEY, new Integer(Character.toLowerCase(localizedName.charAt(bucksPos))));
            }
        }
    }

    protected void updateAction() {
        updateName();
        updateTooltip();
    }

    private void updateTooltip() {
        putValue(Action.SHORT_DESCRIPTION, "<html><body bgcolor=#EAEAEA>" + getActionName() + "</body></html>");
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

    public void languageChanged(Event event) {
        updateName();
        updateTooltip();
    }

    private String getCustomIconPath() {
        if (getID() == null) {
            return null;
        }
        if (ourIconProperties == null) {
            ourIconProperties = loadProperties("/icons.properties");
        }
        return (String) ourIconProperties.get(getID());
    }

    public static KeyStroke getKeyStroke(String keystrokeID) {
        String keystrokeText = getKeyStrokeText(keystrokeID);
        return keystrokeText == null ? null : KeyStroke.getKeyStroke(keystrokeText);
    }

    private static String getKeyStrokeText(String keystrokeID) {
        if (ourKeyboardProperties == null) {
            ourKeyboardProperties = loadProperties("/keyboard.properties");
        }
        return (String) ourKeyboardProperties.get(keystrokeID);
    }

    private static Properties loadProperties(String resource) {
        Properties result = new Properties();
        URL url = GPAction.class.getResource(resource);
        if (url == null) {
            return null;
        }
        URL resolvedUrl;
        try {
            resolvedUrl = Platform.resolve(url);
            result.load(resolvedUrl.openStream());
            return result;
        } catch (IOException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
            return null;
        }
    }
}
