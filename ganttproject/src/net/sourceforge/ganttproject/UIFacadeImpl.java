/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject team

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
package net.sourceforge.ganttproject;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.gui.GanttLookAndFeelInfo;
import net.sourceforge.ganttproject.gui.GanttLookAndFeels;
import net.sourceforge.ganttproject.gui.GanttStatusBar;
import net.sourceforge.ganttproject.gui.NotificationChannel;
import net.sourceforge.ganttproject.gui.NotificationItem;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.NotificationManagerImpl;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TaskSelectionContext;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.ViewLogDialog;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.I18N;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManagerImpl;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.ShortDateFormatOption;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskView;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;

import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;

import com.google.common.base.Objects;

class UIFacadeImpl extends ProgressProvider implements UIFacade {
  private static final ImageIcon LOGO = new ImageIcon(UIFacadeImpl.class.getResource("/icons/big.png"));
  private final JFrame myMainFrame;
  private final ScrollingManager myScrollingManager;
  private final ZoomManager myZoomManager;
  private final GanttStatusBar myStatusBar;
  private final UIFacade myFallbackDelegate;
  private final TaskSelectionManager myTaskSelectionManager;
  private final GPOptionGroup myOptions;
  private final LafOption myLafOption;
  private final GPOptionGroup myLogoOptions;
  private final DefaultStringOption myLogoOption;
  private final NotificationManagerImpl myNotificationManager;
  private final TaskView myTaskView = new TaskView();
  private final DialogBuilder myDialogBuilder;

  UIFacadeImpl(JFrame mainFrame, GanttStatusBar statusBar, NotificationManagerImpl notificationManager,
      IGanttProject project, UIFacade fallbackDelegate) {
    myMainFrame = mainFrame;
    myDialogBuilder = new DialogBuilder(mainFrame);
    myScrollingManager = new ScrollingManagerImpl();
    myZoomManager = new ZoomManager(project.getTimeUnitStack());
    myStatusBar = statusBar;
    myStatusBar.setNotificationManager(notificationManager);
    myFallbackDelegate = fallbackDelegate;
    Job.getJobManager().setProgressProvider(this);
    myTaskSelectionManager = new TaskSelectionManager();
    myNotificationManager = notificationManager;

    myLafOption = new LafOption(this);
    final ShortDateFormatOption shortDateFormatOption = new ShortDateFormatOption();
    final DefaultStringOption dateSampleOption = new DefaultStringOption("ui.dateFormat.sample");
    dateSampleOption.setWritable(false);
    final DefaultBooleanOption dateFormatSwitchOption = new DefaultBooleanOption("ui.dateFormat.switch", true);

    final LanguageOption languageOption = new LanguageOption();
    languageOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        // Language changed...
        if (dateFormatSwitchOption.isChecked()) {
          // ... update default date format option
          Locale selected = languageOption.getSelectedValue();
          shortDateFormatOption.setSelectedLocale(selected);
        }
      }
    });
    dateFormatSwitchOption.addChangeValueListener(new ChangeValueListener() {
      private String customFormat;

      @Override
      public void changeValue(ChangeValueEvent event) {
        shortDateFormatOption.setWritable(!dateFormatSwitchOption.isChecked());
        if (dateFormatSwitchOption.isChecked()) {
          customFormat = shortDateFormatOption.getValue();
          // Update to default date format
          Locale selected = languageOption.getSelectedValue();
          shortDateFormatOption.setSelectedLocale(selected);
          dateSampleOption.setValue(shortDateFormatOption.formatDate(new Date()));
        } else if (customFormat != null) {
          shortDateFormatOption.setValue(customFormat);
        }
      }
    });
    shortDateFormatOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        // Update date sample
        dateSampleOption.setValue(shortDateFormatOption.formatDate(new Date()));
      }
    });
    GPOption[] options = new GPOption[] { myLafOption, languageOption, dateFormatSwitchOption, shortDateFormatOption,
        dateSampleOption };
    myOptions = new GPOptionGroup("ui", options);
    I18N i18n = new OptionsPageBuilder.I18N();
    myOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLafOption), "looknfeel");
    myOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(languageOption), "language");
    myOptions.setTitled(false);

    myLogoOption = new DefaultStringOption("ui.logo");
    myLogoOptions = new GPOptionGroup("ui2", myLogoOption);
    myLogoOptions.setTitled(false);
  }

  @Override
  public ScrollingManager getScrollingManager() {
    return myScrollingManager;
  }

  @Override
  public ZoomManager getZoomManager() {
    return myZoomManager;
  }

  @Override
  public GPUndoManager getUndoManager() {
    return myFallbackDelegate.getUndoManager();
  }

  @Override
  public ZoomActionSet getZoomActionSet() {
    return myFallbackDelegate.getZoomActionSet();
  }

  @Override
  public Choice showConfirmationDialog(String message, String title) {
    String yes = GanttLanguage.getInstance().getText("yes");
    String no = GanttLanguage.getInstance().getText("no");
    String cancel = GanttLanguage.getInstance().getText("cancel");
    int result = JOptionPane.showOptionDialog(myMainFrame, message, title, JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE, null, new String[] { yes, no, cancel }, yes);
    switch (result) {
    case JOptionPane.YES_OPTION:
      return Choice.YES;
    case JOptionPane.NO_OPTION:
      return Choice.NO;
    case JOptionPane.CANCEL_OPTION:
      return Choice.CANCEL;
    case JOptionPane.CLOSED_OPTION:
      return Choice.CANCEL;
    default:
      return Choice.CANCEL;
    }
  }

  @Override
  public void showPopupMenu(Component invoker, Action[] actions, int x, int y) {
    showPopupMenu(invoker, Arrays.asList(actions), x, y);
  }

  @Override
  public void showPopupMenu(Component invoker, Collection<Action> actions, int x, int y) {
    JPopupMenu menu = new JPopupMenu();
    for (Action action : actions) {
      if (action == null) {
        menu.addSeparator();
      } else {
        Boolean isSelected = (Boolean)action.getValue(Action.SELECTED_KEY);
        if (isSelected == null) {
          menu.add(action);
        } else {
          menu.add(new JCheckBoxMenuItem(action));
        }
      }
    }
    menu.applyComponentOrientation(getLanguage().getComponentOrientation());
    menu.show(invoker, x, y);
  }

  @Override
  public Dialog createDialog(Component content, Action[] buttonActions, String title) {
    return myDialogBuilder.createDialog(content, buttonActions, title, myNotificationManager);
  }

  @Override
  public void setStatusText(String text) {
    myStatusBar.setFirstText(text, 2000);
  }

  @Override
  public void showOptionDialog(int messageType, String message, Action[] actions) {
    JOptionPane optionPane = new JOptionPane(message, messageType);
    Object[] options = new Object[actions.length];
    Object defaultOption = null;
    for (int i = 0; i < actions.length; i++) {
      options[i] = actions[i].getValue(Action.NAME);
      if (actions[i].getValue(Action.DEFAULT) != null) {
        defaultOption = options[i];
      }
    }
    optionPane.setOptions(options);
    if (defaultOption != null) {
      optionPane.setInitialValue(defaultOption);
    }
    JDialog dialog = optionPane.createDialog(myMainFrame, "");
    dialog.setVisible(true);
    Object choice = optionPane.getValue();
    for (Action a : actions) {
      if (a.getValue(Action.NAME).equals(choice)) {
        a.actionPerformed(null);
        break;
      }
    }
  }

  @Override
  public NotificationManager getNotificationManager() {
    return myNotificationManager;
  }

  /** Show and log the exception */
  @Override
  public void showErrorDialog(Throwable e) {
    GPLogger.logToLogger(e);
    showErrorNotification(buildMessage(e));
  }

  private static String buildMessage(Throwable e) {
    StringBuilder result = new StringBuilder();
    String lastMessage = null;
    while (e != null) {
      if (e.getMessage() != null && !Objects.equal(lastMessage, e.getMessage())) {
        result.append(e.getMessage()).append("<br>");
        lastMessage = e.getMessage();
      }
      e = e.getCause();
    }
    return result.toString();
  }

  @Override
  public void showErrorDialog(String errorMessage) {
    GPLogger.log(errorMessage);
    showErrorNotification(errorMessage);
  }

  private void showErrorNotification(String message) {
    getNotificationManager().addNotifications(
        NotificationChannel.ERROR,
        Collections.singletonList(new NotificationItem(i18n("error.channel.itemTitle"),
            GanttLanguage.getInstance().formatText("error.channel.itemBody", message), new HyperlinkListener() {
              @Override
              public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() != EventType.ACTIVATED) {
                  return;
                }
                if ("localhost".equals(e.getURL().getHost()) && "/log".equals(e.getURL().getPath())) {
                  onViewLog();
                } else {
                  NotificationManager.DEFAULT_HYPERLINK_LISTENER.hyperlinkUpdate(e);
                }
              }
            })));
  }

  protected void onViewLog() {
    ViewLogDialog.show(this);
  }

  private static String i18n(String key) {
    return GanttLanguage.getInstance().getText(key);
  }

  void resetErrorLog() {
  }

  @Override
  public GanttChart getGanttChart() {
    return myFallbackDelegate.getGanttChart();
  }

  @Override
  public TimelineChart getResourceChart() {
    return myFallbackDelegate.getResourceChart();
  }

  @Override
  public Chart getActiveChart() {
    return myFallbackDelegate.getActiveChart();
  }

  @Override
  public int getViewIndex() {
    return myFallbackDelegate.getViewIndex();
  }

  @Override
  public void setViewIndex(int viewIndex) {
    myFallbackDelegate.setViewIndex(viewIndex);
  }

  @Override
  public int getGanttDividerLocation() {
    return myFallbackDelegate.getGanttDividerLocation();
  }

  @Override
  public void setGanttDividerLocation(int location) {
    myFallbackDelegate.setGanttDividerLocation(location);
  }

  @Override
  public int getResourceDividerLocation() {
    return myFallbackDelegate.getResourceDividerLocation();
  }

  @Override
  public void setResourceDividerLocation(int location) {
    myFallbackDelegate.setResourceDividerLocation(location);
  }

  @Override
  public void refresh() {
    myFallbackDelegate.refresh();
  }

  @Override
  public Frame getMainFrame() {
    return myMainFrame;
  }

  private static GanttLanguage getLanguage() {
    return GanttLanguage.getInstance();
  }

  static String getExceptionReport(Throwable e) {
    StringBuffer result = new StringBuffer();
    result.append(e.getMessage());
    if (e instanceof DocumentException == false) {
      result.append("\n\n");
      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      e.printStackTrace(writer);
      writer.close();
      result.append(stringWriter.getBuffer().toString());
    }
    return result.toString();
  }

  @Override
  public void setWorkbenchTitle(String title) {
    myMainFrame.setTitle(title);
  }

  @Override
  public IProgressMonitor createMonitor(Job job) {
    return myStatusBar.createProgressMonitor();
  }

  @Override
  public IProgressMonitor createProgressGroup() {
    return myStatusBar.createProgressMonitor();
  }

  @Override
  public IProgressMonitor createMonitor(Job job, IProgressMonitor group, int ticks) {
    return group;
  }

  @Override
  public IProgressMonitor getDefaultMonitor() {
    return null;
  }

  @Override
  public TaskView getCurrentTaskView() {
    return myTaskView;
  }

  @Override
  public TaskTreeUIFacade getTaskTree() {
    return myFallbackDelegate.getTaskTree();
  }

  @Override
  public ResourceTreeUIFacade getResourceTree() {
    return myFallbackDelegate.getResourceTree();
  }

  @Override
  public TaskSelectionContext getTaskSelectionContext() {
    return myTaskSelectionManager;
  }

  @Override
  public TaskSelectionManager getTaskSelectionManager() {
    return myTaskSelectionManager;
  }

  @Override
  public GanttLookAndFeelInfo getLookAndFeel() {
    return myLafOption.getLookAndFeel();
  }

  @Override
  public void setLookAndFeel(final GanttLookAndFeelInfo laf) {
    if (laf == null) {
      return;
    }
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (!doSetLookAndFeel(laf)) {
          doSetLookAndFeel(GanttLookAndFeels.getGanttLookAndFeels().getDefaultInfo());
        }
      }
    });
  }

  private boolean doSetLookAndFeel(GanttLookAndFeelInfo laf) {
    try {
      UIManager.setLookAndFeel(laf.getClassName());
      SwingUtilities.updateComponentTreeUI(myMainFrame);
      return true;
    } catch (Exception e) {
      GPLogger.getLogger(UIFacade.class).log(Level.SEVERE,
          "Can't find the LookAndFeel\n" + laf.getClassName() + "\n" + laf.getName(), e);
      return false;
    }
  }

  static class LafOption extends DefaultEnumerationOption<GanttLookAndFeelInfo> implements GP1XOptionConverter {
    private final UIFacade myUiFacade;

    LafOption(UIFacade uiFacade) {
      super("laf", GanttLookAndFeels.getGanttLookAndFeels().getInstalledLookAndFeels());
      myUiFacade = uiFacade;
    }

    public GanttLookAndFeelInfo getLookAndFeel() {
      return GanttLookAndFeels.getGanttLookAndFeels().getInfoByName(getValue());
    }

    @Override
    protected String objectToString(GanttLookAndFeelInfo laf) {
      return laf.getName();
    }

    @Override
    public void commit() {
      super.commit();
      myUiFacade.setLookAndFeel(GanttLookAndFeels.getGanttLookAndFeels().getInfoByName(getValue()));
    }

    @Override
    public String getTagName() {
      return "looknfeel";
    }

    @Override
    public String getAttributeName() {
      return "name";
    }

    @Override
    public void loadValue(String legacyValue) {
      setValue(legacyValue, true);
      myUiFacade.setLookAndFeel(GanttLookAndFeels.getGanttLookAndFeels().getInfoByName(legacyValue));
    }
  }

  static class LanguageOption extends DefaultEnumerationOption<Locale> implements GP1XOptionConverter {
    public LanguageOption() {
      this("language", GanttLanguage.getInstance().getAvailableLocales().toArray(new Locale[0]));
    }

    private LanguageOption(String id, Locale[] locales) {
      super(id, locales);
    }

    @Override
    protected String objectToString(Locale locale) {
      String englishName = locale.getDisplayLanguage(Locale.US);
      String localName = locale.getDisplayLanguage(locale);
      if ("en".equals(locale.getLanguage()) || "zh".equals(locale.getLanguage())) {
        if (!locale.getCountry().isEmpty()) {
          englishName += " - " + locale.getDisplayCountry(Locale.US);
          localName += " - " + locale.getDisplayCountry(locale);
        }
      }
      if (localName.equals(englishName)) {
        return englishName;
      }
      return englishName + " (" + localName + ")";
    }

    @Override
    public void commit() {
      super.commit();
      applyLocale(stringToObject(getValue()));
    }

    protected void applyLocale(Locale locale) {
      if (locale == null) {
        // Selected Locale was not available, so use default Locale
        locale = Locale.getDefault();
      }
      GanttLanguage.getInstance().setLocale(locale);
    }

    @Override
    public String getTagName() {
      return "language";
    }

    @Override
    public String getAttributeName() {
      return "selection";
    }

    @Override
    public void loadValue(String legacyValue) {
      loadPersistentValue(legacyValue);
    }

    @Override
    public String getPersistentValue() {
      Locale l = stringToObject(getValue());
      if (l == null) {
        l = GanttLanguage.getInstance().getLocale();
      }
      assert l != null;
      String result = l.getLanguage();
      if (!l.getCountry().isEmpty()) {
        result += "_" + l.getCountry();
      }
      return result;
    }

    @Override
    public void loadPersistentValue(String value) {
      String[] lang_country = value.split("_");
      Locale l;
      if (lang_country.length == 2) {
        l = new Locale(lang_country[0], lang_country[1]);
      } else {
        l = new Locale(lang_country[0]);
      }
      value = objectToString(l);
      if (value != null) {
        setValue(value, true);
        applyLocale(l);
      }
    }
  }

  @Override
  public GPOptionGroup[] getOptions() {
    return new GPOptionGroup[] {myOptions, myLogoOptions};
  }

  @Override
  public Image getLogo() {
    if (myLogoOption.getValue() == null) {
      return LOGO.getImage();
    }
    try {
      File imageFile = new File(myLogoOption.getValue());
      if (imageFile.exists() && imageFile.canRead()) {
        return ImageIO.read(imageFile);
      }
      GPLogger.logToLogger("File=" + myLogoOption.getValue() + " does not exist or is not readable");
    } catch (IOException e) {
      GPLogger.logToLogger(e);
    }
    return LOGO.getImage();
  }
}
