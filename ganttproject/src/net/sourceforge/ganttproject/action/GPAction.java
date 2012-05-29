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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.util.PropertiesUtil;

/**
 * @author bard
 */
public abstract class GPAction extends AbstractAction implements GanttLanguage.Listener {
  public enum IconSize {
    NO_ICON(null), MENU("16"), TOOLBAR_SMALL("24"), TOOLBAR_BIG("24");

    private final String mySize;

    IconSize(String size) {
      mySize = size;
    }

    public String asString() {
      return mySize;
    }
  }

  public static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  /** Location of the icon files */
  public static final String ICON_FILE_DIRECTORY = "/icons";

  protected boolean iconVisible = true;

  private Icon myIcon = null;

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

  public static final Icon getIcon(String iconSize, String iconFileName) {
    URL resource = GPAction.class.getResource(MessageFormat.format("{0}/{1}x{1}/{2}", ICON_FILE_DIRECTORY, iconSize, iconFileName));
    return resource == null ? null : new ImageIcon(resource);

  }
  /**
   * @return translation of "ID.description" if available, otherwise translation
   *         of "ID"
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

  /** @return translation of ID */
  protected String getLocalizedName() {
    return getID() == null ? null : getI18n(getID());
  }

  public String getID() {
    return myName;
  }

  protected String getActionName() {
    String name = getLocalizedDescription();
    return name == null ? "" : language.correctLabel(name);
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
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(Character.toUpperCase(localizedName.charAt(bucksPos))));
      }
    }
  }

  /**
   * Updates the action. Can be called when external influences resulted in
   * changed action name and/or description
   */
  public void updateAction() {
    updateName();
    updateTooltip();
  }

  protected void updateTooltip() {
    putValue(Action.SHORT_DESCRIPTION, "<html><body bgcolor=#EAEAEA>" + getLocalizedDescription() + "</body></html>");
  }

  public void isIconVisible(boolean isNull) {
    setIconVisible(isNull);
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

  public static List<KeyStroke> getAllKeyStrokes(String keystrokeID) {
    String text = getKeyStrokeText(keystrokeID);
    if (text == null) {
      return Collections.emptyList();
    }
    List<KeyStroke> result = new ArrayList<KeyStroke>();
    for (String ksText : text.split(",")) {
      result.add(KeyStroke.getKeyStroke(ksText));
    }
    return result;
  }

  public static KeyStroke getKeyStroke(String keystrokeID) {
    String keystrokeText = getKeyStrokeText(keystrokeID);
    return keystrokeText == null ? null : KeyStroke.getKeyStroke(keystrokeText);
  }

  public static String getKeyStrokeText(String keystrokeID) {
    if (ourKeyboardProperties == null) {
      ourKeyboardProperties = new Properties();
      PropertiesUtil.loadProperties(ourKeyboardProperties, "/keyboard.properties");
      PropertiesUtil.loadProperties(ourKeyboardProperties, "/mouse.properties");
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
