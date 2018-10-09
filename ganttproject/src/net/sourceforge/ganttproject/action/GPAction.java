/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import com.google.common.base.Strings;
import net.sourceforge.ganttproject.DesktopIntegration;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.util.PropertiesUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author bard
 */
public abstract class GPAction extends AbstractAction implements GanttLanguage.Listener {
  private final String myIconSize;
  private String myFontAwesomeLabel;

  public enum IconSize {
    NO_ICON(null), SMALL("8"), MENU("16"), TOOLBAR_SMALL("24"), TOOLBAR_BIG("24");

    private final String mySize;

    IconSize(String size) {
      mySize = size;
    }

    public String asString() {
      return mySize;
    }
  }

  public static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  /**
   * Location of the icon files
   */
  public static final String ICON_FILE_DIRECTORY = "/icons";

  protected boolean iconVisible = true;

  private final String myName;

  private KeyStroke myKeyStroke;

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
    myIconSize = iconSize;
    if (iconSize != null) {
      updateIcon(iconSize);
    }
    updateName();
    updateTooltip();
    language.addListener(this);
    if (name != null) {
      myKeyStroke = getKeyStroke(name);
      putValue(Action.ACCELERATOR_KEY, myKeyStroke);
    }
  }

  protected GPAction(String name, IconSize size) {
    this(name, size.asString());
  }

  public GPAction withIcon(IconSize size) {
    final GPAction result = new GPAction(myName, size) {
      @Override
      public void actionPerformed(ActionEvent e) {
        GPAction.this.actionPerformed(e);
      }

      @Override
      public boolean isEnabled() {
        return GPAction.this.isEnabled();
      }

    };
    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        result.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
      }
    });
    return result;
  }

  public KeyStroke getKeyStroke() {
    return myKeyStroke;
  }

  private void updateIcon(String iconSize) {
    Icon icon = createIcon(iconSize);
    if (icon != null) {
      putValue(Action.SMALL_ICON, icon);
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

  public static final Icon getIcon(String iconSize, String iconFileName) {
    URL resource = GPAction.class.getResource(MessageFormat.format("{0}/{1}x{1}/{2}", ICON_FILE_DIRECTORY, iconSize, iconFileName));
    return resource == null ? null : new ImageIcon(resource);

  }

  /**
   * @return translation of "ID.description" if available, otherwise translation
   * of "ID"
   */
  protected String getLocalizedDescription() {
    if (getID() == null) {
      return null;
    }
    String description = getI18n(getID() + ".description");
    if (description == null) {
      description = language.correctLabel(getLocalizedName());
    }
    return description == null ? "" : description;
  }

  /**
   * @return translation of ID
   */
  protected String getLocalizedName() {
    return getID() == null ? null : getI18n(getID());
  }

  public String getID() {
    return myName;
  }

  protected static String getI18n(String key) {
    return language.getText(key);
  }

  protected String getIconFilePrefix() {
    return null;
  }

  protected final void updateName() {
    if (getFontawesomeLabel() != null) {
      putValue(Action.NAME, getFontawesomeLabel());
      return;
    }
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
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(Character.toUpperCase(localizedName.charAt(bucksPos))));
      }
    }
  }

  /**
   * Updates the action. Can be called when external influences resulted in
   * changed action name and/or description
   */
  public void updateAction() {
    if (IconSize.TOOLBAR_SMALL.asString().equals(myIconSize) && getFontawesomeLabel() != null) {
      putValue(Action.SMALL_ICON, null);
      putValue(Action.NAME, getFontawesomeLabel());
    } else {
      updateName();
    }
    updateTooltip();
  }

  protected boolean calledFromAppleScreenMenu(ActionEvent e) {
    if (e == null) {
      return false;
    }
    if (String.valueOf(e.getSource()).indexOf("JMenu") == -1) {
      return false;
    }
    if (e.getModifiers() == 0) {
      return false;
    }
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
      if (stackTrace[i].getClassName().indexOf("ScreenMenuItem") > 0) {
        return true;
      }
    }
    return false;
  }

  protected void updateTooltip() {
    String description = getLocalizedDescription();
    putValue(Action.SHORT_DESCRIPTION, Strings.isNullOrEmpty(description) ? null : description);
  }

  @Override
  public void languageChanged(Event event) {
    updateAction();
  }

  private String getCustomIconPath() {
    if (getID() == null) {
      return null;
    }
    if (ourIconProperties == null) {
      ourIconProperties = new Properties();
      PropertiesUtil.loadProperties(ourIconProperties, "/icons.properties");
    }
    return (String) ourIconProperties.get(getID());
  }

  public String getFontawesomeLabel() {
    return myFontAwesomeLabel;
  }

  protected void setFontAwesomeLabel(String label) {
    myFontAwesomeLabel = label;
    updateAction();
  }

  public GPAction asToolbarAction() {
    return this;
  }

  public static List<KeyStroke> getAllKeyStrokes(String keystrokeID) {
    String text = getKeyStrokeText(keystrokeID);
    if (text == null) {
      return Collections.emptyList();
    }
    List<KeyStroke> result = new ArrayList<KeyStroke>();
    for (String ksText : text.split(",")) {
      KeyStroke ks = parseKeyStroke(ksText);
      if (ks != null) {
        result.add(ks);
      }
    }
    return result;
  }

  public static KeyStroke getKeyStroke(String keystrokeID) {
    String keystrokeText = getKeyStrokeText(keystrokeID);
    if (keystrokeText == null) {
      return null;
    }
    return parseKeyStroke(keystrokeText);
  }

  private static KeyStroke parseKeyStroke(String keystrokeText) {
    KeyStroke keyStroke = KeyStroke.getKeyStroke(keystrokeText);
    if (keyStroke == null) {
      return null;
    }
    if ((keyStroke.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
        || (keyStroke.getModifiers() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK) {
      int modifiers = keyStroke.getModifiers() & (0xffffffff ^ KeyEvent.CTRL_MASK) & (0xffffffff ^ KeyEvent.CTRL_DOWN_MASK) | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      keyStroke = KeyStroke.getKeyStroke(keyStroke.getKeyCode(), modifiers, keyStroke.isOnKeyRelease());
    }
    return keyStroke;
  }

  public static String getKeyStrokeText(String keystrokeID) {
    if (ourKeyboardProperties == null) {
      ourKeyboardProperties = new Properties();
      PropertiesUtil.loadProperties(ourKeyboardProperties, "/keyboard.properties");
      if (DesktopIntegration.isMacOs()) {
        PropertiesUtil.loadProperties(ourKeyboardProperties, "/mouse.macos.properties");
        PropertiesUtil.loadProperties(ourKeyboardProperties, "/keyboard.macos.properties");
      } else {
        PropertiesUtil.loadProperties(ourKeyboardProperties, "/mouse.properties");
      }
    }
    return (String) ourKeyboardProperties.get(keystrokeID);
  }

  public static GPAction createVoidAction(String key) {
    return new GPAction(key) {
      @Override
      public void actionPerformed(ActionEvent e) {
        // No action
      }
    };
  }
}
