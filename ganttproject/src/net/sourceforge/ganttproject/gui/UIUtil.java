/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import javafx.embed.swing.JFXPanel;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.ValueValidator;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.util.PropertiesUtil;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public abstract class UIUtil {
  public static final Highlighter ZEBRA_HIGHLIGHTER = new ColorHighlighter(new HighlightPredicate() {
    @Override
    public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
      return adapter.row % 2 == 1;
    }
  }, new Color(0xf0, 0xf0, 0xe0), null);

  public static final Color ERROR_BACKGROUND = new Color(255, 191, 207);
  public static final Color INVALID_VALUE_BACKGROUND = new Color(255, 125, 125);
  public static final Color INVALID_FIELD_COLOR = Color.RED.brighter();
  public static final Color PATINA_FOREGROUND = new Color(102, 153, 153);
  public static Font FONTAWESOME_FONT = null;
  private static Properties FONTAWESOME_PROPERTIES = new Properties();
  private static Properties ourUiProperties = new Properties();

  static {
    ImageIcon calendarImage = new ImageIcon(UIUtil.class.getResource("/icons/calendar_16.gif"));
    ImageIcon nextMonth = new ImageIcon(UIUtil.class.getResource("/icons/nextmonth.gif"));
    ImageIcon prevMonth = new ImageIcon(UIUtil.class.getResource("/icons/prevmonth.gif"));
    UIManager.put("JXDatePicker.arrowDown.image", calendarImage);
    UIManager.put("JXMonthView.monthUp.image", prevMonth);
    UIManager.put("JXMonthView.monthDown.image", nextMonth);
    UIManager.put("JXMonthView.monthCurrent.image", calendarImage);

    try (InputStream is = UIUtil.class.getResourceAsStream("/fontawesome-webfont.ttf")) {
      Font font = Font.createFont(Font.TRUETYPE_FONT, is);
      FONTAWESOME_FONT = font.deriveFont(Font.PLAIN, 24f);
    } catch (IOException | FontFormatException e) {
      FONTAWESOME_FONT = null;
    }
    FONTAWESOME_PROPERTIES = new Properties();
    PropertiesUtil.loadProperties(FONTAWESOME_PROPERTIES, "/fontawesome.properties");
    PropertiesUtil.loadProperties(ourUiProperties, "/ui.properties");
  }

  public static String getUiProperty(String key) {
    return ourUiProperties.getProperty(key);
  }

  public static void setEnabledTree(JComponent root, final boolean isEnabled) {
    walkComponentTree(root, new Predicate<JComponent>() {
      @Override
      public boolean apply(JComponent input) {
        input.setEnabled(isEnabled);
        return true;
      }
    });
  }

  public static void setBackgroundTree(JComponent root, final Color background) {
    walkComponentTree(root, new Predicate<JComponent>() {
      @Override
      public boolean apply(JComponent input) {
        input.setBackground(background);
        return true;
      }
    });
  }

  public static void walkComponentTree(JComponent root, Predicate<JComponent> visitor) {
    if (visitor.apply(root)) {
      Component[] components = root.getComponents();
      for (int i = 0; i < components.length; i++) {
        if (components[i] instanceof JComponent) {
          walkComponentTree((JComponent) components[i], visitor);
        }
      }
    }
  }
  public static void createTitle(JComponent component, String title) {
    Border lineBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK);
    component.setBorder(BorderFactory.createTitledBorder(lineBorder, title));
  }

  public static void registerActions(JComponent component, boolean recursive, GPAction... actions) {
    for (GPAction action : actions) {
      for (KeyStroke ks : GPAction.getAllKeyStrokes(action.getID())) {
        pushAction(component, recursive, ks, action);
      }
    }
  }

  public static void pushAction(JComponent root, boolean recursive, KeyStroke keyStroke, GPAction action) {
    root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, action.getID());
    root.getActionMap().put(action.getID(), action);
    for (Component child : root.getComponents()) {
      if (child instanceof JComponent) {
        pushAction((JComponent) child, recursive, keyStroke, action);
      }
    }
  }

  public static void setupTableUI(JTable table, int visibleRows) {
    table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredScrollableViewportSize().width,
        table.getRowHeight() * visibleRows));
    Font font = table.getFont();
    table.setRowHeight(table.getFontMetrics(font).getHeight() + 5);
  }

  public static void setupHighlighters(JXTable table) {
    table.setHighlighters(HighlighterFactory.createAlternateStriping(Color.WHITE, Color.ORANGE.brighter()));

  }

  public static void setupTableUI(JTable table) {
    setupTableUI(table, 10);
  }

  public static <T> DocumentListener attachValidator(final JTextField textField, final OptionsPageBuilder.ValueValidator<T> validator, final GPOption<T> option) {
    final DocumentListener listener = new DocumentListener() {
      private void saveValue() {
        try {
          T oldValue = option == null ? null : option.getValue();
          T value = validator.parse(textField.getText());
          if (option != null && !Objects.equal(oldValue, value)) {
            option.setValue(value, validator);
          }
          textField.setBackground(getValidFieldColor());
        }
        /* If value in text filed is not integer change field color */
        catch (NumberFormatException ex) {
          textField.setBackground(INVALID_FIELD_COLOR);
        } catch (ValidationException ex) {
          textField.setBackground(INVALID_FIELD_COLOR);
        }
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        saveValue();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        saveValue();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        saveValue();
      }
    };
    textField.getDocument().addDocumentListener(listener);
    return listener;
  }


  public static interface DateValidator extends Function<Date, Pair<Boolean, String>> {
    class Default {
      public static DateValidator aroundProjectStart(final Date projectStart) {
        return dateInRange(projectStart, 1000);
      }

      public static DateValidator dateInRange(final Date center, final int yearDiff) {
        return new DateValidator() {
          @Override
          public Pair<Boolean, String> apply(Date value) {
            int diff = Math.abs(value.getYear() - center.getYear());
            if (diff > yearDiff) {
              return Pair.create(Boolean.FALSE, String.format(
                    "Date %s is far away (%d years) from expected date %s. Any mistake?", value, diff, center));
            }
            return Pair.create(Boolean.TRUE, null);
          }
        };
      }
    }
  }

  private static Date tryParse(DateFormat dateFormat, String text) {
    try {
      return dateFormat.parse(text);
    } catch (ParseException e) {
      return null;
    }
  }

  public static ValueValidator<Date> createStringDateValidator(final DateValidator dv, final Supplier<List<DateFormat>> formats) {
    return new ValueValidator<Date>() {
      @Override
      public Date parse(String text) throws ValidationException {
        if (Strings.isNullOrEmpty(text)) {
          throw new ValidationException();
        }
        Date parsed = null;
        for (DateFormat df : formats.get()) {
          parsed = tryParse(df, text);
          if (parsed != null) {
            break;
          }
        }
        if (parsed == null) {
          throw new ValidationException("Can't parse value=" + text + "as date");
        }
        if (dv != null) {
          Pair<Boolean, String> validationResult = dv.apply(parsed);
          if (!validationResult.first()) {
            throw new ValidationException(validationResult.second());
          }
        }
        return parsed;
      }
    };
  }

  public static void setupDatePicker(
      final JXDatePicker picker, final Date initialDate, final DateValidator dv, final ActionListener listener) {
    Supplier<List<DateFormat>> formatSupplier = new Supplier<List<DateFormat>>() {
      @Override
      public List<DateFormat> get() {
        return ImmutableList.<DateFormat>of(
            GanttLanguage.getInstance().getLongDateFormat(),
            GanttLanguage.getInstance().getShortDateFormat());
      }
    };
    ValueValidator<Date> parseValidator = createStringDateValidator(dv, formatSupplier);
    DatePickerEditCommiter commiter = setupDatePicker(picker, initialDate, dv, parseValidator, listener);
    commiter.attachOnFocusLost(listener);
  }

  // This class is responsible for committing user input in a text editor component of a date picker
  private static class DatePickerEditCommiter {
    private final JFormattedTextField myTextEditor;
    private final JXDatePicker myDatePicker;
    private final Date myInitialDate;
    private final DateValidator myDateValidator;
    private final ValueValidator<Date> myParseValidator;

    private DatePickerEditCommiter(JXDatePicker datePicker, JFormattedTextField textEditor,
        DateValidator dateValidator,  ValueValidator<Date> parseValidator) {
      myTextEditor = Preconditions.checkNotNull(textEditor);
      myDatePicker = Preconditions.checkNotNull(datePicker);
      myInitialDate = myDatePicker.getDate();
      myDateValidator = dateValidator;
      myParseValidator = parseValidator;
    }

    // We need listen focus lost in a dialog, but we don't need it in the table view,
    // so listener is optional
    void attachOnFocusLost(final ActionListener onSuccess) {
      myTextEditor.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          try {
            tryCommit();
            onSuccess.actionPerformed(new ActionEvent(myDatePicker, ActionEvent.ACTION_PERFORMED, ""));
            return;
          } catch (ValidationException | ParseException ex) {
            // We probably don't want to log parse/validation exceptions
            // If user input is not valid we reset value to the initial one
            resetValue();
          }
        }
      });
    }

    void resetValue() {
      myTextEditor.setBackground(getValidFieldColor());
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          if (myInitialDate != null) {
            myDatePicker.setDate(myInitialDate);
          }
        }
      });
    }

    // Tries to finish editing and set the result value into the date picker
    void tryCommit() throws ParseException, ValidationException {
      myTextEditor.commitEdit();
      final Date dateValue;
      if (myTextEditor.getValue() instanceof Date) {
        if (myDateValidator != null) {
          Pair<Boolean, String> validation = myDateValidator.apply((Date)myTextEditor.getValue());
          if (!validation.first()) {
            throw new ValidationException(validation.second());
          }
        }
        dateValue = (Date) myTextEditor.getValue();
      } else {
        dateValue = myParseValidator.parse(String.valueOf(myTextEditor.getText()));
        if (dateValue == null) {
          throw new ValidationException();
        }
      }
      myDatePicker.setDate(dateValue);
      myTextEditor.setBackground(getValidFieldColor());
      return;
    }
  }
  public static DatePickerEditCommiter setupDatePicker(final JXDatePicker picker, final Date initialDate, final DateValidator dv, final ValueValidator<Date> parseValidator, final ActionListener listener) {
    if (dv == null) {
      picker.addActionListener(listener);
    } else {
      picker.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Date date = ((JXDatePicker) e.getSource()).getDate();
          if (date != null) {
            Pair<Boolean, String> validation = dv.apply(date);
            if (!validation.first()) {
              throw new ValidationException(validation.second());
            }
          }
        }
      });
    }
    final JFormattedTextField editor = picker.getEditor();
    UIUtil.attachValidator(editor, parseValidator, null);
    if (initialDate != null) {
      picker.setDate(initialDate);
    }
    return new DatePickerEditCommiter(picker, editor, dv, parseValidator);
  }
  /**
   * @return a {@link JXDatePicker} component with the default locale, images
   *         and date formats.
   */
  public static JXDatePicker createDatePicker() {
    return createDatePicker(GanttLanguage.getInstance().getLongDateFormat(), GanttLanguage.getInstance().getShortDateFormat());
  }
  public static JXDatePicker createDatePicker(DateFormat... dateFormats) {
    final JXDatePicker result = new JXDatePicker();
    result.setLocale(GanttLanguage.getInstance().getDateFormatLocale());
    result.setFormats(dateFormats);
    return result;
  }

  public static Dimension autoFitColumnWidth(JTable table, TableColumn tableColumn) {
    final int margin = 5;

    Dimension headerFit = getHeaderDimension(table, tableColumn);
    int width = headerFit.width;
    int height = 0;

    int order = table.convertColumnIndexToView(tableColumn.getModelIndex());
    // Get maximum width of column data
    for (int r = 0; r < table.getRowCount(); r++) {
      TableCellRenderer renderer = table.getCellRenderer(r, order);
      Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, order), false,
          false, r, order);
      width = Math.max(width, comp.getPreferredSize().width);
      height += comp.getPreferredSize().height;
    }
    // Add margin
    width += 2 * margin;
    // Set the width
    return new Dimension(width, height);
  }

  public static Dimension getHeaderDimension(JTable table, TableColumn tableColumn) {
    TableCellRenderer renderer = tableColumn.getHeaderRenderer();
    if (renderer == null) {
      renderer = table.getTableHeader().getDefaultRenderer();
    }
    Component comp = renderer.getTableCellRendererComponent(table, tableColumn.getHeaderValue(), false, false, 0, 0);
    return comp.getPreferredSize();
  }

  public static JComponent createButtonBar(JButton[] leftButtons, JButton[] rightButtons) {
    Box leftBox = Box.createHorizontalBox();
    for (JButton button : leftButtons) {
      leftBox.add(button);
      leftBox.add(Box.createHorizontalStrut(3));
    }

    Box rightBox = Box.createHorizontalBox();
    for (JButton button : Lists.reverse(Arrays.asList(rightButtons))) {
      rightBox.add(Box.createHorizontalStrut(3));
      rightBox.add(button);
    }

    JPanel result = new JPanel(new BorderLayout());
    result.add(leftBox, BorderLayout.WEST);
    result.add(rightBox, BorderLayout.EAST);
    return result;
  }

  public static JComponent createTopAndCenter(JComponent top, JComponent center) {
    JPanel result = new JPanel(new BorderLayout());
    top.setAlignmentX(Component.LEFT_ALIGNMENT);
    result.add(top, BorderLayout.NORTH);

    JPanel planePageWrapper = new JPanel(new BorderLayout());
    planePageWrapper.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
    center.setAlignmentX(Component.LEFT_ALIGNMENT);
    planePageWrapper.add(center, BorderLayout.NORTH);
    result.add(planePageWrapper, BorderLayout.CENTER);
    return result;

  }

  public static JMenu createTooltiplessJMenu(Action action) {
    JMenu result = new JMenu(action) {
      @Override
      public JMenuItem add(Action a) {
        JMenuItem result = super.add(a);
        result.setToolTipText(null);
        return result;
      }
    };
    result.setToolTipText(null);
    return result;
  }

  public static Color getValidFieldColor() {
    return UIManager.getColor("TextField.background");
  }

  public static JEditorPane createHtmlPane(String html, HyperlinkListener hyperlinkListener) {
    JEditorPane htmlPane = new JEditorPane();
    htmlPane.setEditorKit(new HTMLEditorKit());
    htmlPane.setEditable(false);
    // htmlPane.setPreferredSize(new Dimension(400, 290));
    htmlPane.addHyperlinkListener(hyperlinkListener);
    //htmlPane.setBackground(Color.YELLOW);
    htmlPane.setText(html);
    return htmlPane;
  }

  public static TableCellEditor newDateCellEditor(IGanttProject project, boolean showDatePicker) {
    Supplier<List<DateFormat>> supplier = new Supplier<List<DateFormat>>() {
      @Override
      public List<DateFormat> get() {
        return Collections.<DateFormat>singletonList(GanttLanguage.getInstance().getShortDateFormat());
      }
    };
    return new GPDateCellEditor(project, showDatePicker, null, supplier);
  }

  public static class GPDateCellEditor extends DefaultCellEditor implements ActionListener, GanttLanguage.Listener {
    private Date myDate;
    private final JXDatePicker myDatePicker;
    private final boolean myShowDatePicker;
    private DatePickerEditCommiter myCommitter;

    public GPDateCellEditor(IGanttProject project, boolean showDatePicker, ValueValidator<Date> parseValidator, Supplier<List<DateFormat>> dateFormats) {
      super(new JTextField());
      myDatePicker = UIUtil.createDatePicker(dateFormats.get().toArray(new DateFormat[0]));
      myShowDatePicker = showDatePicker;
      if (parseValidator == null) {
        parseValidator = UIUtil.createStringDateValidator(null, dateFormats);
      }
      myCommitter = UIUtil.setupDatePicker(myDatePicker, null, null, parseValidator, getActionListener());
      GanttLanguage.getInstance().addListener(this);
    }

    @Override
    public Component getTableCellEditorComponent(JTable arg0, Object value, boolean arg2, int arg3, int arg4) {
      if (value instanceof GanttCalendar) {
        myDatePicker.setDate(((GanttCalendar)value).getTime());
      } else if (value instanceof Date) {
        myDatePicker.setDate((Date) value);
      } else if (value instanceof CalendarEvent) {
        myDatePicker.setDate(((CalendarEvent)value).myDate);
      }
      return myShowDatePicker ? myDatePicker : myDatePicker.getEditor();
    }

    private ActionListener getActionListener() {
      return this;
    }

    @Override
    public Object getCellEditorValue() {
      return CalendarFactory.createGanttCalendar(myDate == null ? new Date() : myDate);
    }

    @Override
    public boolean stopCellEditing() {
      try {
        myCommitter.tryCommit();
      } catch (ValidationException | ParseException e) {
        myCommitter.resetValue();
        return false;
      }
      myDate = myDatePicker.getDate();
      getComponent().setBackground(null);
      super.fireEditingStopped();
      return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      stopCellEditing();
    }

    public void languageChanged(Event event) {
      myDatePicker.setFormats(GanttLanguage.getInstance().getShortDateFormat());
    }
  }

  public static JComponent newColorComponent(Color value) {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    final JPanel label = new JPanel();
    label.setPreferredSize(new Dimension(16, 16));
    label.setBackground(value);
    buttonPanel.add(label);
//    buttonPanel.add(new JXHyperlink(new AbstractAction("choose") {
//      public void actionPerformed(ActionEvent e) {
//        System.err.println("Clicked!");
//
//      }
//    }));
    return buttonPanel;
  }
  public static TableCellRenderer newColorRenderer(final Supplier<Integer> rowCount) {
    return new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
          int row, int column) {
        JComponent def = (JComponent) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        if (row >= rowCount.get()) {
          return def;
        }
        JComponent buttonPanel = newColorComponent((Color)value);
        buttonPanel.setBackground(def.getBackground());
        buttonPanel.setBorder(def.getBorder());
        return buttonPanel;
      }
    };
  }
  public static JComponent contentPaneBorder(JComponent component) {
    return border(component, 5, TOP | LEFT | BOTTOM | RIGHT);
  }

  public static final int TOP = 1, LEFT = 1 << 1, BOTTOM = 1 << 2, RIGHT = 1 << 3;
  public static JComponent border(JComponent component, int width, int mask) {
    component.setBorder(BorderFactory.createEmptyBorder(
        width * (mask & TOP), width * (mask & LEFT) >> 1, width * (mask & BOTTOM) >> 2, width * (mask & RIGHT) >> 3));
    return component;
  }

  public static String formatPathForLabel(File file) {
    Path path = Paths.get(file.toURI());
    if (path.getNameCount() <= 4) {
      return file.getAbsolutePath();
    }
    Path prefix = path.subpath(0, 3);
    Path suffix = path.getFileName();
    return path.getRoot().resolve(Paths.get(prefix.toString(), "...", suffix.toString())).toString();
  }

  public static void setupErrorLabel(JLabel label, String errorMessage) {
    label.setIcon(GPAction.getIcon("8", "label-red-exclamation.png"));
    label.setText(errorMessage);
    label.setForeground(Color.RED);
  }

  public static void clearErrorLabel(JLabel label) {
    label.setIcon(null);
    label.setForeground(UIManager.getColor("Label.foreground"));
  }

  public static void initJavaFx(final Runnable andThen) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
          new JFXPanel(); // initializes JavaFX environment
          andThen.run();
      }
    });
  }

  public static class MultiscreenFitResult {
    public final double totalVisibleArea;
    public final double maxVisibleArea;
    public final GraphicsConfiguration  argmaxVisibleArea;

    public MultiscreenFitResult(double totalVisibleArea, double maxVisibleArea, GraphicsConfiguration argmaxVisibleArea) {
      this.totalVisibleArea = totalVisibleArea;
      this.maxVisibleArea = maxVisibleArea;
      this.argmaxVisibleArea = argmaxVisibleArea;
    }
  }

  public static MultiscreenFitResult multiscreenFit(Rectangle bounds)  {
    double visibleAreaTotal = 0.0;
    double maxVisibleArea = 0.0;
    GraphicsConfiguration argmaxVisibleArea = null;
    GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();

    // Iterate through the screen devices and calculate how much of the stored
    // rectangle fits on every device. Also calculate the total visible area.
    for (GraphicsDevice gd : e.getScreenDevices()) {
      GraphicsConfiguration gc = gd.getDefaultConfiguration();
      if (bounds.intersects(gc.getBounds())) {
        Rectangle visibleHere = bounds.intersection(gc.getBounds());
        double visibleArea = 1.0 * visibleHere.height * visibleHere.width / (bounds.height * bounds.width);
        visibleAreaTotal += visibleArea;
        if (visibleArea > maxVisibleArea) {
          argmaxVisibleArea = gc;
          maxVisibleArea = visibleArea;
        }
      }
    }
    return new MultiscreenFitResult(visibleAreaTotal, maxVisibleArea, argmaxVisibleArea);
  }

  public static String getFontawesomeLabel(GPAction action) {
    if (action.getID() == null) {
      return null;
    }
    Object value = FONTAWESOME_PROPERTIES.get(action.getID());
    return value == null ? null : String.valueOf(value);
  }

  public static boolean isFontawesomeSizePreferred() {
    String laf = UIManager.getLookAndFeel().getName().toLowerCase();
    return laf.contains("macosx") || laf.contains("mac os x");
  }

  public static float getFontawesomeScale(GPAction action) {
    float defaultScale = Float.valueOf(FONTAWESOME_PROPERTIES.get(".scale").toString());
    if (action.getID() == null) {
      return defaultScale;
    }
    Object value = FONTAWESOME_PROPERTIES.get(action.getID() + ".scale");
    return value == null ? defaultScale : defaultScale * Float.valueOf(value.toString());
  }

  private static final float DEFAULT_YSHIFT = Float.valueOf(FONTAWESOME_PROPERTIES.get(".yshift").toString());
  public static float getFontawesomeYShift(GPAction action) {
    if (action.getID() == null) {
      return DEFAULT_YSHIFT;
    }
    Object value = FONTAWESOME_PROPERTIES.get(action.getID() + ".yshift");
    return DEFAULT_YSHIFT + (value == null ? 0f : Float.valueOf(value.toString()));
  }

}
