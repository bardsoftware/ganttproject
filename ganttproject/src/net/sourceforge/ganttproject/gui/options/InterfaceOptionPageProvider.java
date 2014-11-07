/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import org.apache.commons.io.FileUtils;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.collect.Pair;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.GPOptionGroup;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class InterfaceOptionPageProvider extends OptionPageProviderBase {
  public static final String ID = "ui.general";
  private JEditorPane myLabel;

  public InterfaceOptionPageProvider() {
    super(ID);
  }

  @Override
  public void init(IGanttProject project, final UIFacade uiFacade) {
    super.init(project, uiFacade);
    uiFacade.getLanguageOption().addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        Locale selectedLocale = uiFacade.getLanguageOption().getSelectedValue();
        if (selectedLocale != null && "gl".equals(selectedLocale.getLanguage().toLowerCase()) && myLabel != null) {
          Pair<Boolean, File> localeTest = checkLocale(selectedLocale);
          if (!localeTest.first() && localeTest.second() != null) {
            GanttLanguage i18n = GanttLanguage.getInstance();
            myLabel.setVisible(true);
            myLabel.setText(i18n.formatText("optionPage.ui.general.localeInstallText",
                i18n.getText("optionPage.ui.general.localeInstallUrl"), localeTest.second().getAbsolutePath()));
          }
        } else if (myLabel != null && myLabel.isVisible()) {
          myLabel.setVisible(false);
        }
      }
    });
  }


  @Override
  public GPOptionGroup[] getOptionGroups() {
    List<GPOptionGroup> groups = Lists.newArrayList();
    groups.addAll(Arrays.asList(getUiFacade().getOptions()));
    return groups.toArray(new GPOptionGroup[groups.size()]);
  }

  @Override
  public boolean hasCustomComponent() {
    return true;
  }

  @Override
  public Component buildPageComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    builder.setUiFacade(getUiFacade());
    Component component = builder.buildPage(getOptionGroups(), getPageID());
    myLabel = UIUtil.createHtmlPane("", NotificationManager.DEFAULT_HYPERLINK_LISTENER);
    myLabel.setBorder(BorderFactory.createEmptyBorder());
    myLabel.setVisible(false);

    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(component, BorderLayout.NORTH);
    wrapper.add(myLabel, BorderLayout.SOUTH);
    return wrapper;
  }

  private static Pair<Boolean, File> checkLocale(Locale l) {
      if (Arrays.asList(DateFormat.getAvailableLocales()).contains(l)) {
        return Pair.create(Boolean.TRUE, null);
      }
      File extDir = getExtDir();
      if (!extDir.exists()) {
        return Pair.create(Boolean.FALSE, null);
      }
      if (!extDir.isDirectory()) {
        return Pair.create(Boolean.FALSE, null);
      }
      if (extDir.canWrite()) {
        GPLogger.logToLogger("Java extensions directory " + extDir + " is writable");
        URL libUrl = InterfaceOptionPageProvider.class.getResource("lib");
        if (libUrl != null) {
          try {
            File galicianLocaleJar = new File(new File(libUrl.toURI()), "javagalician.jar");
            File targetJar = new File(extDir, galicianLocaleJar.getName());
            GPLogger.logToLogger("Locale extension " + galicianLocaleJar);
            if (galicianLocaleJar.exists() && !targetJar.exists()) {
              GPLogger.logToLogger("Exists. Installing now");
              FileUtils.copyFileToDirectory(galicianLocaleJar, extDir);
              return Pair.create(Boolean.TRUE, extDir);
            }
          } catch (IOException e) {
            GPLogger.log(e);
          } catch (URISyntaxException e) {
            GPLogger.log(e);
          }
        }
        return Pair.create(Boolean.FALSE, extDir);
      } else {
        GPLogger.logToLogger("Java extensions directory " + extDir + " is not writable");
      }
      return Pair.create(Boolean.FALSE, extDir);
  }

  private static File getExtDir() {
    File fallback = new File(System.getProperty("java.home"), Joiner.on(File.separatorChar).join("lib", "ext"));
    String extDirsProperty = System.getProperty("java.ext.dirs");
    if (Strings.isNullOrEmpty(extDirsProperty)) {
      return fallback;
    }
    for (String s : extDirsProperty.split(File.pathSeparator)) {
      File file = new File(s);
      if (!file.exists()) {
        continue;
      }
      if (!file.isDirectory()) {
        continue;
      }
      if (file.canWrite()) {
        return file;
      }
      fallback = file;
    }
    return fallback;
  }
}
