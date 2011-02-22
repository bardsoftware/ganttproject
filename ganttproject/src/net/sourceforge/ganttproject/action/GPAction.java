/*
 * Created on 26.03.2005
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
    public static final int MENU_MASK = Toolkit.getDefaultToolkit()
            .getMenuShortcutKeyMask();

    protected boolean iconVisible = true;
    private Icon myIcon = null;

    private final String myKey;

    protected GPAction() {
        this(null, "16");
    }

    protected GPAction(String name, String iconSize) {
        super(name);
        myKey = name;
        updateIcon(iconSize);
        updateName();
        updateTooltip();
        GanttLanguage.getInstance().addListener(this);
    }

    public GPAction(String name) {
        this(name, "16");
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
        String resourcePath = getIconResource();
        if (resourcePath == null) {
            resourcePath = getIconFileDirectory() + "/" + getIconFilePrefix() + iconSize + ".gif";
        } else {
            resourcePath = MessageFormat.format("{0}/{1}x{1}/{2}", getIconFileDirectory(), iconSize, resourcePath);
        }
        URL resource = getClass().getResource(resourcePath);
        return resource == null ? null : new ImageIcon(resource);
    }

    protected final String getIconFileDirectory() {
        return "/icons";
    }

    protected String getLocalizedName() {
        return getKey()==null ? null : getI18n(getKey());
    }

    protected String getKey() {
        return myKey;
    }
    protected String getTooltipText() {
        String localizedName = getLocalizedName();
        return localizedName == null ? "" : GanttLanguage.getInstance().correctLabel(getLocalizedName());
    }

    protected String getI18n(String key) {
        return GanttLanguage.getInstance().getText(key);
    }

    protected String getIconFilePrefix() {
        return null;
    }

    protected final void setIconVisible(boolean isVisible) {
        iconVisible = isVisible;
        putValue(Action.SMALL_ICON, iconVisible ? myIcon : null);
    }

    private void updateName() {
        String localizedName = getLocalizedName();
        if (localizedName == null) {
            localizedName = String.valueOf(getValue(Action.NAME));
        }
        if (localizedName != null) {
            int bucksPos = localizedName.indexOf('$');
            if (bucksPos>=0) {
                localizedName = new StringBuffer(localizedName).deleteCharAt(bucksPos).toString();
            }
            putValue(Action.NAME, localizedName);
            if (bucksPos>=0) {
                putValue(Action.MNEMONIC_KEY, new Integer(Character.toLowerCase(localizedName.charAt(bucksPos))));
            }
        }
    }

    protected void updateAction() {
        updateName();
        updateTooltip();
    }

    private void updateTooltip() {
        putValue(Action.SHORT_DESCRIPTION, "<html><body bgcolor=#EAEAEA>" + getTooltipText() + "</body></html>");
    }
    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

    public void languageChanged(Event event) {
        updateName();
        updateTooltip();
    }

    private String getIconResource() {
        if (getKey() == null) {
            return null;
        }
        if (ourIconProperties == null) {
            ourIconProperties = loadProperties("/icons.properties");
        }
        return (String)ourIconProperties.get(getKey());
    }

    public static KeyStroke getKeyStroke(String keystrokeID) {
        String keystrokeText = getKeyStrokeText(keystrokeID);
        return keystrokeText == null ? null : KeyStroke.getKeyStroke(keystrokeText);
    }

    public static String getKeyStrokeText(String keystrokeID) {
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

    private static Properties ourKeyboardProperties;
    private static Properties ourIconProperties;
}
