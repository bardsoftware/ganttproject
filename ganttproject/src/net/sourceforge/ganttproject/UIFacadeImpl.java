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

import biz.ganttproject.core.option.*;
import biz.ganttproject.core.option.FontSpec.Size;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.gui.*;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.I18N;
import net.sourceforge.ganttproject.gui.options.SettingsDialog2;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManagerImpl;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.LanguageOption;
import net.sourceforge.ganttproject.language.ShortDateFormatOption;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskView;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

class UIFacadeImpl extends ProgressProvider implements UIFacade {
  private final JFrame myMainFrame;
  private final ScrollingManager myScrollingManager;
  private final ZoomManager myZoomManager;
  private final GanttStatusBar myStatusBar;
  private final UIFacade myFallbackDelegate;
  private final TaskSelectionManager myTaskSelectionManager;
  private final List<GPOptionGroup> myOptionGroups = Lists.newArrayList();
  private final GPOptionGroup myOptions;
  private final LafOption myLafOption;
  private final GPOptionGroup myLogoOptions;
  private final DefaultFileOption myLogoOption;
  private final NotificationManagerImpl myNotificationManager;
  private final TaskView myTaskView = new TaskView();
  private final DialogBuilder myDialogBuilder;
  private final Map<String, Font> myOriginalFonts = Maps.newHashMap();
  private final List<Runnable> myOnUpdateComponentTreeUiCallbacks = Lists.newArrayList();
  private float myLastScale = 0;

  private static Map<FontSpec.Size, String> getSizeLabels() {
    Map<FontSpec.Size, String> result = Maps.newHashMap();
    for (FontSpec.Size size : FontSpec.Size.values()) {
      result.put(size, GanttLanguage.getInstance().getText("optionValue.ui.appFontSpec." + size.toString() + ".label"));
    }
    return result;
  }

  private final DefaultFontOption myAppFontOption = new DefaultFontOption(
      "appFontSpec", null, Arrays.asList(getFontFamilies())) {
    @Override
    public Map<FontSpec.Size, String> getSizeLabels() {
      return UIFacadeImpl.getSizeLabels();
    }
  };
  private final DefaultFontOption myChartFontOption = new DefaultFontOption(
      "chartFontSpec", new FontSpec("Dialog", FontSpec.Size.NORMAL), Arrays.asList(getFontFamilies())) {
    @Override
    public Map<Size, String> getSizeLabels() {
      return UIFacadeImpl.getSizeLabels();
    }
  };
  private final DefaultIntegerOption myDpiOption = new DefaultIntegerOption("screenDpi", DEFAULT_DPI);
  @Override
  public IntegerOption getDpiOption() {
    return myDpiOption;
  }
  public GPOption<String> getLafOption() {
    return myLafOption;
  }


  private ChangeValueListener myAppFontValueListener;
  private final LanguageOption myLanguageOption;
  private final IGanttProject myProject;
  private FontSpec myLastFontSpec;

  UIFacadeImpl(JFrame mainFrame, GanttStatusBar statusBar, NotificationManagerImpl notificationManager,
      final IGanttProject project, UIFacade fallbackDelegate) {
    myMainFrame = mainFrame;
    myProject = project;
    myDialogBuilder = new DialogBuilder(mainFrame);
    myScrollingManager = new ScrollingManagerImpl();
    myZoomManager = new ZoomManager(project.getTimeUnitStack());
    myStatusBar = statusBar;
    myStatusBar.setNotificationManager(notificationManager);
    myFallbackDelegate = fallbackDelegate;
    Job.getJobManager().setProgressProvider(this);
    myTaskSelectionManager = new TaskSelectionManager(Suppliers.memoize(new Supplier<TaskManager>() {
      public TaskManager get() {
        return project.getTaskManager();
      }
    }));
    myNotificationManager = notificationManager;

    myLafOption = new LafOption(this);
    final ShortDateFormatOption shortDateFormatOption = new ShortDateFormatOption();
    final DefaultStringOption dateSampleOption = new DefaultStringOption("ui.dateFormat.sample");
    dateSampleOption.setWritable(false);
    final DefaultBooleanOption dateFormatSwitchOption = new DefaultBooleanOption("ui.dateFormat.switch", true);

    myLanguageOption = new LanguageOption() {
      {
        GanttLanguage.getInstance().addListener(new GanttLanguage.Listener() {
          @Override
          public void languageChanged(GanttLanguage.Event event) {
            Locale selected = getSelectedValue();
            reloadValues(GanttLanguage.getInstance().getAvailableLocales());
            setSelectedValue(selected);
          }
        });
      }
      @Override
      protected void applyLocale(Locale locale) {
        if (locale == null) {
          // Selected Locale was not available, so use default Locale
          locale = Locale.getDefault();
        }
        GanttLanguage.getInstance().setLocale(locale);
      }
    };
    myLanguageOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        // Language changed...
        if (dateFormatSwitchOption.isChecked()) {
          // ... update default date format option
          Locale selected = myLanguageOption.getSelectedValue();
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
          Locale selected = myLanguageOption.getSelectedValue();
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

//    myFontSizeOption = new DefaultIntegerOption("ui.appFontSize");
//    myFontSizeOption.setHasUi(false);

    GPOption[] options = new GPOption[] { myLafOption, myAppFontOption, myChartFontOption, myDpiOption, myLanguageOption, dateFormatSwitchOption, shortDateFormatOption,
        dateSampleOption };
    myOptions = new GPOptionGroup("ui", options);
    I18N i18n = new OptionsPageBuilder.I18N();
    myOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLafOption), "looknfeel");
    myOptions.setI18Nkey(i18n.getCanonicalOptionLabelKey(myLanguageOption), "language");
    myOptions.setTitled(false);

    myLogoOption = new DefaultFileOption("ui.logo");
    myLogoOptions = new GPOptionGroup("ui2", myLogoOption);
    myLogoOptions.setTitled(false);
    addOptions(myOptions);
    addOptions(myLogoOptions);
  }

  private String[] getFontFamilies() {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
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
    showNotificationDialog(NotificationChannel.ERROR, buildMessage(e));
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
    showNotificationDialog(NotificationChannel.ERROR, errorMessage);
  }

  @Override
  public void showNotificationDialog(NotificationChannel channel, String message) {
    String i18nPrefix = channel.name().toLowerCase() + ".channel.";
    getNotificationManager().addNotifications(
        channel,
        Collections.singletonList(new NotificationItem(i18n(i18nPrefix + "itemTitle"),
            GanttLanguage.getInstance().formatText(i18nPrefix + "itemBody", message), new HyperlinkListener() {
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

  @Override
  public void showSettingsDialog(String pageID) {
    SettingsDialog2 dialog = new SettingsDialog2(myProject, this, "settings.app.pageOrder");
    dialog.show(pageID);
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
        if (myAppFontValueListener == null) {
          myAppFontValueListener = new ChangeValueListener() {
            @Override
            public void changeValue(ChangeValueEvent event) {
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  updateFonts();
                  updateComponentTreeUI();
                }
              });
            }
          };
          myAppFontOption.addChangeValueListener(myAppFontValueListener);
          myDpiOption.addChangeValueListener(new ChangeValueListener() {
            @Override
            public void changeValue(ChangeValueEvent event) {
              if (myDpiOption.getValue() >= UIFacade.DEFAULT_DPI) {
                updateFonts();
              }
            }
          });
        }
      }
    });
  }

  private boolean doSetLookAndFeel(GanttLookAndFeelInfo laf) {
    try {
      UIManager.setLookAndFeel(laf.getClassName());
      updateFonts();
      updateComponentTreeUI();
      return true;
    } catch (Exception e) {
      GPLogger.getLogger(UIFacade.class).log(Level.SEVERE,
          "Can't find the LookAndFeel\n" + laf.getClassName() + "\n" + laf.getName(), e);
      return false;
    }
  }

  private void updateComponentTreeUI() {
    SwingUtilities.updateComponentTreeUI(myMainFrame);
    //myMainFrame.pack();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        for (Runnable r : myOnUpdateComponentTreeUiCallbacks) {
          r.run();
        }
        getGanttChart().reset();
        getResourceChart().reset();
      }
    });
  }

  private void updateFonts() {
    if (myOriginalFonts.isEmpty()) {
      UIDefaults defaults = UIManager.getDefaults();
      for (Enumeration<Object> keys = defaults.keys(); keys.hasMoreElements();) {
        String key = String.valueOf(keys.nextElement());
        Object obj = UIManager.get(key);
        if (obj instanceof Font) {
          Font f = (Font) obj;
          myOriginalFonts.put(key, f);
        }
      }
    }
    FontSpec currentSpec = myAppFontOption.getValue();
    float dpiScale = myDpiOption.getValue().floatValue() / DEFAULT_DPI;
    if (currentSpec != null && (!currentSpec.equals(myLastFontSpec) || dpiScale != myLastScale)) {
      for (Map.Entry<String, Font> font : myOriginalFonts.entrySet()) {
        float newSize = (font.getValue().getSize() * currentSpec.getSize().getFactor() * dpiScale);
        Font newFont;
        if (Strings.isNullOrEmpty(currentSpec.getFamily())) {
          newFont = font.getValue().deriveFont(newSize);
        } else {
          newFont = new FontUIResource(currentSpec.getFamily(), font.getValue().getStyle(), (int)newSize);
        }
        UIManager.put(font.getKey(), newFont);
      }
      myLastFontSpec = currentSpec;
      myLastScale = dpiScale;
    }
  }

  static class LafOption extends DefaultEnumerationOption<GanttLookAndFeelInfo> implements GP1XOptionConverter {
    private final UIFacade myUiFacade;

    LafOption(UIFacade uiFacade) {
      super("laf", GanttLookAndFeels.getGanttLookAndFeels().getInstalledLookAndFeels());
      myUiFacade = uiFacade;
      if (!System.getProperty("os.name").toLowerCase().contains("os x")) {
        setValue("Plastic");
      }
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
      resetValue(legacyValue, true);
      myUiFacade.setLookAndFeel(GanttLookAndFeels.getGanttLookAndFeels().getInfoByName(legacyValue));
    }
  }

  public DefaultEnumerationOption<Locale> getLanguageOption() {
    return myLanguageOption;
  }

  @Override
  public GPOptionGroup[] getOptions() {
    return myOptionGroups.toArray(new GPOptionGroup[myOptionGroups.size()]);
  }

  @Override
  public void addOnUpdateComponentTreeUi(Runnable callback) {
    myOnUpdateComponentTreeUiCallbacks.add(callback);
  }

  @Override
  public Image getLogo() {
    if (myLogoOption.getValue() == null) {
      return DEFAULT_LOGO.getImage();
    }
    try {
      File imageFile = new File(myLogoOption.getValue());
      if (imageFile.exists() && imageFile.canRead()) {
        return Objects.firstNonNull(ImageIO.read(imageFile), DEFAULT_LOGO.getImage());
      }
      GPLogger.logToLogger("File=" + myLogoOption.getValue() + " does not exist or is not readable");
    } catch (IOException e) {
      GPLogger.logToLogger(e);
    }
    return DEFAULT_LOGO.getImage();
  }

  void addOptions(GPOptionGroup options) {
    myOptionGroups.add(options);
  }

  FontOption getChartFontOption() {
    return myChartFontOption;
  }

  FontOption getAppFontOption() {
    return myAppFontOption;
  }
}
