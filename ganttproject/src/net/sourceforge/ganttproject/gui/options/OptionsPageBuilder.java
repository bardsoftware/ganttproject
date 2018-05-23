/*
 * Created on 02.04.2005
 */
package net.sourceforge.ganttproject.gui.options;

import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DateOption;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.DoubleOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.FileOption;
import biz.ganttproject.core.option.FontOption;
import biz.ganttproject.core.option.FontSpec;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;
import biz.ganttproject.core.option.MoneyOption;
import biz.ganttproject.core.option.StringOption;
import biz.ganttproject.core.option.ValidationException;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.GPColorChooser;
import net.sourceforge.ganttproject.gui.TextFieldAndFileChooserComponent;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIFacade.Dialog;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author bard
 */
public class OptionsPageBuilder {
  I18N myi18n = new I18N();
  private Component myParentComponent;
  private final LayoutApi myLayoutApi;
  private UIFacade myUiFacade;
  private DecimalFormat myFormat;

  public static interface LayoutApi {
    void layout(JPanel panel, int componentsCount);
  }

  public static LayoutApi TWO_COLUMN_LAYOUT = new LayoutApi() {
    @Override
    public void layout(JPanel panel, int componentsCount) {
      panel.setLayout(new SpringLayout());
      SpringUtilities.makeCompactGrid(panel, componentsCount, 2, 0, 0, 5, 3);
    }
  };

  public static LayoutApi ONE_COLUMN_LAYOUT = new LayoutApi() {
    @Override
    public void layout(JPanel panel, int componentsCount) {
      panel.setLayout(new SpringLayout());
      SpringUtilities.makeCompactGrid(panel, componentsCount*2, 1, 0, 0, 5, new Function<Integer, Integer>() {
        @Override
        public Integer apply(Integer input) {
          return input % 2 == 0 ? 5 : 3;
        }
      });
    }
  };

  public OptionsPageBuilder() {
    this(null, TWO_COLUMN_LAYOUT);
  }

  public OptionsPageBuilder(Component parentComponent, LayoutApi layoutApi) {
    myParentComponent = parentComponent;
    myLayoutApi = layoutApi;
  }

  public void setUiFacade(UIFacade uiFacade) {
    myUiFacade = uiFacade;
  }

  public void setI18N(I18N i18n) {
    myi18n = i18n;
  }

  public I18N getI18N() {
    return myi18n;
  }

  public void setOptionKeyPrefix(String optionKeyPrefix) {
    myi18n.myOptionKeyPrefix = optionKeyPrefix;
  }


  public JComponent buildPage(GPOptionGroup[] optionGroups, String pageID) {
    JComponent topPanel = TopPanel.create(myi18n.getPageTitle(pageID), "");
    JComponent planePage = buildPlanePage(optionGroups);
    return UIUtil.createTopAndCenter(topPanel, planePage);
  }

  public JComponent buildPlanePage(GPOptionGroup[] optionGroups) {
    final JComponent optionsPanel = new JPanel(new SpringLayout());
    for (int i = 0; i < optionGroups.length; i++) {
      optionsPanel.add(createGroupComponent(optionGroups[i]));
    }
    SpringUtilities.makeCompactGrid(optionsPanel, optionGroups.length, 1, 0, 0, 5, 15);
    JPanel resultPanel = new JPanel(new BorderLayout());
    resultPanel.add(optionsPanel, BorderLayout.NORTH);
    resultPanel.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        optionsPanel.getComponent(0).requestFocus();
      }

    });
    return resultPanel;
  }

  public JComponent createLabeledComponent(GPOption<?> option) {
    GPOptionGroup fake = new GPOptionGroup("", new GPOption[] { option });
    fake.setTitled(false);
    return createGroupComponent(fake);
  }

  public JComponent createGroupComponent(GPOptionGroup group) {
    GPOption<?>[] options = group.getOptions();
    final JComponent optionsPanel = createGroupComponent(group, options);
    if (group.isTitled()) {
      UIUtil.createTitle(optionsPanel, myi18n.getOptionGroupLabel(group));
    }
    JPanel result = new JPanel(new BorderLayout());
    result.add(optionsPanel, BorderLayout.NORTH);
    result.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        optionsPanel.requestFocus();
      }
    });
    return result;
  }

  public JComponent createGroupComponent(GPOptionGroup group, GPOption<?>... options) {
    JPanel optionsPanel = new JPanel();

    int hasUiCount = 0;
    for (int i = 0; i < options.length; i++) {
      GPOption<?> nextOption = options[i];
      if (!nextOption.hasUi()) {
        continue;
      }
      hasUiCount++;
      final Component nextComponent = createOptionComponent(group, nextOption);
      if (needsLabel(group, nextOption)) {
        Component nextLabel = createOptionLabel(group, options[i]);
        optionsPanel.add(nextLabel);
        optionsPanel.add(nextComponent);
      } else {
        optionsPanel.add(nextComponent);
        optionsPanel.add(new JPanel());
      }
      if (i == 0) {
        optionsPanel.addFocusListener(new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            super.focusGained(e);
            nextComponent.requestFocus();
          }

        });
      }
    }
    if (hasUiCount > 0) {
      myLayoutApi.layout(optionsPanel, hasUiCount);
    }
    return optionsPanel;
  }

  private boolean needsLabel(GPOptionGroup group, GPOption<?> nextOption) {
    // if (nextOption instanceof BooleanOption) {
    // return !isCheckboxOption(group, nextOption);
    // }
    return true;
  }

  public Component createStandaloneOptionPanel(GPOption<?> option) {
    JPanel optionPanel = new JPanel(new BorderLayout());
    Component optionComponent = createOptionComponent(null, option);
    if (needsLabel(null, option)) {
      optionPanel.add(createOptionLabel(null, option), BorderLayout.WEST);
      optionPanel.add(optionComponent, BorderLayout.CENTER);
    } else {
      optionPanel.add(optionComponent, BorderLayout.WEST);
    }
    JPanel result = new JPanel(new BorderLayout());
    result.add(optionPanel, BorderLayout.NORTH);
    return result;
  }

  public Component createWaitIndicatorComponent(DefaultBooleanOption controller) {
    final JProgressBar progressBar = new JProgressBar();
    JPanel placeholder = new JPanel();
    final JPanel result = new JPanel(new CardLayout());
    result.add(placeholder, "placeholder");
    result.add(progressBar, "progressBar");
    controller.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (Boolean.TRUE.equals(event.getNewValue())) {
          progressBar.setIndeterminate(true);
          ((CardLayout) result.getLayout()).show(result, "progressBar");
        } else {
          progressBar.setIndeterminate(false);
          ((CardLayout) result.getLayout()).show(result, "placeholder");
        }
      }
    });
    return result;
  }

  public Component createOptionLabel(GPOptionGroup group, GPOption<?> option) {
    JLabel nextLabel = new JLabel(myi18n.getOptionLabel(group, option));
    nextLabel.setVerticalAlignment(SwingConstants.TOP);
    nextLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    return nextLabel;
  }

  public Component createOptionComponent(GPOptionGroup group, GPOption<?> option) {
    Component result = null;
    if (option instanceof EnumerationOption) {
      result = createEnumerationComponent((EnumerationOption) option, group);
    } else if (option instanceof FileOption) {
      result = createFileComponent((FileOption) option);
    } else if (option instanceof BooleanOption) {
      result = createBooleanComponent(group, (BooleanOption) option);
    } else if (option instanceof ColorOption) {
      result = createColorComponent((ColorOption) option).getJComponent();
    } else if (option instanceof DateOption) {
      result = createDateComponent((DateOption) option);
    } else if (option instanceof GPOptionGroup) {
      result = createButtonComponent((GPOptionGroup) option);
    } else if (option instanceof StringOption) {
      result = createStringComponent((StringOption) option);
    } else if (option instanceof IntegerOption) {
      result = createValidatingComponent((IntegerOption) option, new ValueValidator<Integer>() {
        @Override
        public Integer parse(String text) {
          return Integer.valueOf(text);
        }
      });
    } else if (option instanceof DoubleOption) {
      result = createValidatingComponent((DoubleOption) option, new ValueValidator<Double>() {
        @Override
        public Double parse(String text) {
          return Double.valueOf(text);
        }
      });
    } else if (option instanceof MoneyOption) {
      result = createValidatingComponent((MoneyOption) option, new ValueValidator<BigDecimal>() {
        private NumberFormat myFormat;
        {
          DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(GanttLanguage.getInstance().getLocale());
          format.setParseBigDecimal(true);
          myFormat = format;
        }
        @Override
        public BigDecimal parse(String text) throws ValidationException {
          try {
            return Strings.isNullOrEmpty(text) ? BigDecimal.ZERO : (BigDecimal) myFormat.parse(text);
          } catch (ParseException e) {
            e.printStackTrace();
            throw new ValidationException(e);
          }
        }
      });
    } else if (option instanceof FontOption) {
      result = createFontComponent((FontOption)option);
    }
    if (result == null) {
      result = new JLabel("Unknown option class=" + option.getClass());
    }
    result.setEnabled(option.isWritable());
    final Component finalResult = result;
    option.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("isWritable".equals(evt.getPropertyName())) {
          assert evt.getNewValue() instanceof Boolean : "Unexpected value of property isWritable: " + evt.getNewValue();
          finalResult.setEnabled((Boolean) evt.getNewValue());
        }
      }
    });
    return result;
  }

  private static Color getValidFieldColor() {
    return UIUtil.getValidFieldColor();
  }

  private Component createFileComponent(final FileOption option) {
    final TextFieldAndFileChooserComponent result = new TextFieldAndFileChooserComponent(myUiFacade, myi18n.getValue(myi18n.myOptionKeyPrefix + option.getID() + ".dialogTitle")) {
      @Override
      protected void onFileChosen(File file) {
        option.setValue(file.getAbsolutePath());
      }
    };
    if (option.getValue() != null) {
      result.setFile(new File(option.getValue()));
    }
    option.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        result.setFile(new File(String.valueOf(event.getNewValue())));
      }
    });
    return result;
  }

  private static void updateTextField(final JTextField textField, final DocumentListener listener,
                                      final ChangeValueEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        textField.getDocument().removeDocumentListener(listener);
        if (!textField.getText().equals(event.getNewValue())) {
          String newText = String.valueOf(event.getNewValue());
          textField.setText(newText);
        }
        textField.getDocument().addDocumentListener(listener);
      }
    });
  }

  private Component createStringComponent(final StringOption option) {
    final JTextField result = option.isScreened() ? new JPasswordField(option.getValue()) : new JTextField(option.getValue());

    final DocumentListener documentListener = new DocumentListener() {
      private void saveValue() {
        try {
          option.setValue(result.getText(), result);
          result.setBackground(getValidFieldColor());
        } catch (ValidationException ex) {
          result.setBackground(UIUtil.INVALID_FIELD_COLOR);
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
    result.getDocument().addDocumentListener(documentListener);
    option.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(final ChangeValueEvent event) {
        if (event.getTriggerID() != result) {
          updateTextField(result, documentListener, event);
        }
      }
    });
    return result;
  }

  private Component createButtonComponent(GPOptionGroup optionGroup) {
    Action action = new AbstractAction(myi18n.getAdvancedActionTitle()) {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.err.println("[OptionsPageBuilder] createButtonComponent: ");
      }

    };
    JButton result = new JButton(action);
    return result;
  }

  private Component createBooleanComponent(GPOptionGroup group, final BooleanOption option) {
    if (!isCheckboxOption(group, option)) {
      return createRadioButtonBooleanComponent(group, option);
    }
    final JCheckBox result = new JCheckBox(new BooleanOptionAction(option));
    String trailingLabel = getTrailingLabel(option);
    if (trailingLabel != null) {
      result.setText(trailingLabel);
    }
    result.setHorizontalAlignment(JCheckBox.LEFT);
    result.setHorizontalTextPosition(SwingConstants.TRAILING);
    result.setSelected(option.isChecked());
    ComponentOrientation componentOrientation = GanttLanguage.getInstance().getComponentOrientation();
    result.setComponentOrientation(componentOrientation);
    option.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        result.setSelected(option.getValue());
      }
    });
    return result;
  }

  private String getTrailingLabel(BooleanOption option) {
    String trailingLabelKey = myi18n.getCanonicalOptionLabelKey(option) + ".trailing";
    return myi18n.hasValue(trailingLabelKey) ? myi18n.getValue(trailingLabelKey) : null;
  }

  private boolean isCheckboxOption(GPOptionGroup group, GPOption<?> option) {
    String yesKey = myi18n.getCanonicalOptionLabelKey(option) + ".yes";
    if ((group == null || group.getI18Nkey(yesKey) == null) && !myi18n.hasValue(yesKey)) {
      return true;
    }
    String noKey = myi18n.getCanonicalOptionLabelKey(option) + ".no";
    if ((group == null || group.getI18Nkey(noKey) == null) && !myi18n.hasValue(noKey)) {
      return true;
    }
    return false;
  }

  public static class BooleanOptionRadioUi {
    private final JRadioButton myYesButton;
    private final JRadioButton myNoButton;

    private BooleanOptionRadioUi(final BooleanOption option) {
      myYesButton = new JRadioButton(new AbstractAction("") {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!option.isChecked()) {
            option.setValue(true);
          }
        }
      });
      myYesButton.setVerticalAlignment(SwingConstants.CENTER);
      myYesButton.setSelected(option.isChecked());

      myNoButton = new JRadioButton(new AbstractAction("") {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (option.isChecked()) {
            option.setValue(false);
          }
        }
      });
      myNoButton.setSelected(!option.isChecked());

      ButtonGroup buttonGroup = new ButtonGroup();
      buttonGroup.add(myYesButton);
      buttonGroup.add(myNoButton);

      option.addChangeValueListener(new ChangeValueListener() {
        @Override
        public void changeValue(ChangeValueEvent event) {
          if (Boolean.TRUE.equals(event.getNewValue())) {
            myYesButton.setSelected(true);
          } else {
            myNoButton.setSelected(true);
          }
        }
      });
    }

    public JRadioButton getYesButton() {
      return myYesButton;
    }

    public JRadioButton getNoButton() {
      return myNoButton;
    }

    public Component getComponent() {
      Box result = Box.createVerticalBox();
      result.add(myYesButton);
      result.add(Box.createVerticalStrut(2));
      result.add(myNoButton);
      result.add(Box.createVerticalGlue());
      return result;
    }
  }

  public static BooleanOptionRadioUi createBooleanOptionRadioUi(BooleanOption option) {
    return new BooleanOptionRadioUi(option);
  }

  private Component createRadioButtonBooleanComponent(GPOptionGroup group, final BooleanOption option) {
    BooleanOptionRadioUi radioUi = createBooleanOptionRadioUi(option);
    radioUi.getYesButton().setText(myi18n.getValue(group, myi18n.getCanonicalOptionLabelKey(option) + ".yes"));
    radioUi.getNoButton().setText(myi18n.getValue(group, myi18n.getCanonicalOptionLabelKey(option) + ".no"));
    return radioUi.getComponent();
  }

  private JComboBox createEnumerationComponent(final EnumerationOption option, final GPOptionGroup group) {
    return createEnumerationComponent(option, new Supplier<EnumerationOptionComboBoxModel>() {
      @Override
      public EnumerationOptionComboBoxModel get() {
        return new EnumerationOptionComboBoxModel(option, group);
      }
    });
  }

  private JComboBox createEnumerationComponent(final EnumerationOption option, final Supplier<EnumerationOptionComboBoxModel> modelFactory) {
    final JComboBox result = new JComboBox(modelFactory.get());
    option.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        EnumerationOptionComboBoxModel model = (EnumerationOptionComboBoxModel) result.getModel();
        model.onValueChange();
        result.setSelectedItem(model.getSelectedItem());
      }
    });
    option.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (EnumerationOption.VALUE_SET.equals(evt.getPropertyName())) {
          EnumerationOptionComboBoxModel model = modelFactory.get();
          result.setModel(model);
          result.setSelectedItem(model.getSelectedItem());
        }
      }
    });
    return result;
  }

  public static interface ColorComponent {
    JComponent getJComponent();
    void openChooser();
    void setOnCancelCallback(Runnable onCancel);
    void setOnOkCallback(Runnable runnable);
  }
  public ColorComponent createColorComponent(final ColorOption option) {
    final JXHyperlink colorButton = new JXHyperlink();
    final JPanel label = new JPanel();
    label.setPreferredSize(new Dimension(16, 16));
    label.setBackground(option.getValue());

    option.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        label.setBackground(option.getValue());
      }
    });
    final AtomicReference<Runnable> onOk = new AtomicReference<>();
    final AtomicReference<Runnable> onCancel = new AtomicReference<>();
    final Action action = new AbstractAction(myi18n.getColorButtonText(option)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        final GPColorChooser colorChooser = new GPColorChooser();
        OkAction okAction = new OkAction() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            Color color = colorChooser.getColor();
            label.setBackground(color);
            option.setValue(color);
            List<Color> recentColors = GPColorChooser.getRecentColors();
            // If we already have such color in the recent list then we remove it
            // If we failed to remove selected color (=> it was not in the list)
            // and the list is long enough then we remove colors from the tail
            if (!recentColors.remove(color) && recentColors.size() == 10) {
              recentColors.remove(9);
            }
            recentColors.add(0,  color);
            GPColorChooser.setRecentColors(recentColors);
            if (onOk.get() != null) {
              onOk.get().run();
            }
          }
        };
        final CancelAction cancelAction = new CancelAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            if (onCancel.get() != null) {
              onCancel.get().run();
            }
          }
        };
        colorChooser.setColor(colorButton.getBackground());
        Dialog dialog = myUiFacade.createDialog(
            colorChooser.buildComponent(),
            new Action[] {okAction, cancelAction},
            myi18n.getColorChooserTitle(option));
        dialog.show();
      };
    };
    colorButton.setAction(action);

    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    buttonPanel.add(label);
    buttonPanel.add(new JLabel(" "));
    buttonPanel.add(colorButton);
    return new ColorComponent() {
      @Override
      public void openChooser() {
        action.actionPerformed(null);
      }
      @Override
      public JComponent getJComponent() {
        return buttonPanel;
      }
      @Override
      public void setOnCancelCallback(Runnable runnable) {
        onCancel.set(runnable);
      }
      @Override
      public void setOnOkCallback(Runnable runnable) {
        onOk.set(runnable);
      }
    };
  }

  public Component createFontComponent(final FontOption option) {
    final Object CLIENT_ID = new Object();
    final DefaultEnumerationOption<String> familiesOption = new DefaultEnumerationOption<>("", option.getFontFamilies());
    if (option.getValue() != null) {
      familiesOption.setValue(option.getValue().getFamily());
    }

    JComboBox comboBox = createEnumerationComponent(familiesOption, new Supplier<EnumerationOptionComboBoxModel>() {
      @Override
      public EnumerationOptionComboBoxModel get() {
        return new EnumerationOptionComboBoxModel(familiesOption, option.getFontFamilies().toArray(new String[0]));
      }
    });
    final JSlider slider = new JSlider(0, 4);
    slider.setPaintTicks(false);
    Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
    for (Map.Entry<FontSpec.Size, String> label : option.getSizeLabels().entrySet()) {
      labels.put(label.getKey().ordinal(), new JLabel(label.getValue()));
    }
    slider.setLabelTable(labels);
    slider.setPaintLabels(true);
    if (option.getValue() != null) {
      slider.setValue(option.getValue().getSize().ordinal());
    }

    ChangeValueListener uiChangeListener = new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        option.setValue(new FontSpec(familiesOption.getValue(), FontSpec.Size.values()[slider.getValue()]), CLIENT_ID);
      }
    };
    familiesOption.addChangeValueListener(uiChangeListener);
    slider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent arg0) {
        option.setValue(new FontSpec(familiesOption.getValue(), FontSpec.Size.values()[slider.getValue()]), CLIENT_ID);
      }
    });

    final Box buttonPanel = Box.createVerticalBox();
    buttonPanel.add(comboBox);
    buttonPanel.add(slider);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
    return buttonPanel;
  }


  public JComponent createDateComponent(final DateOption option) {
    class OptionValueUpdater implements ActionListener, PropertyChangeListener {
      @Override
      public void actionPerformed(ActionEvent e) {
        option.setValue(((JXDatePicker) e.getSource()).getDate());
      }

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof Date && !evt.getNewValue().equals(option.getValue())) {
          option.setValue((Date) evt.getNewValue());
        }
      }
    }
    OptionValueUpdater valueUpdater = new OptionValueUpdater();
    final JXDatePicker result = UIUtil.createDatePicker();
    UIUtil.setupDatePicker(result, option.getValue(), null, valueUpdater);
    result.setDate(option.getValue());
    result.getEditor().addPropertyChangeListener("value", valueUpdater);

    option.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        assert event.getNewValue() == null || event.getNewValue() instanceof Date : "value=" + event.getNewValue();
        result.setDate((Date) event.getNewValue());
      }
    });

    return result;
  }

  public interface ValueValidator<T> {
    T parse(String text) throws ValidationException;
  }

  /**
   * Create JTextField component in options that allows user to input only
   * integer values.
   *
   * @param option
   * @return
   */
  public static <T extends Number> Component createValidatingComponent(final GPOption<T> option, final ValueValidator<T> parser) {
    final JTextField result = new JTextField(String.valueOf(option.getValue()));
    final DocumentListener listener = UIUtil.attachValidator(result, parser, option);
    option.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(final ChangeValueEvent event) {
        if (!Objects.equal(parser, event.getTriggerID())) {
          updateTextField(result, listener, event);
        }
      }
    });
    return result;
  }

  public static class I18N {
    private String myOptionKeyPrefix = "option.";
    private String myOptionGroupKeyPrefix = "optionGroup.";
    private String myOptionPageKeyPrefix = "optionPage.";

    public I18N() {
    }

    protected boolean hasValue(String key) {
      return GanttLanguage.getInstance().getText(key) != null;
    }
    protected String getValue(String key) {
      String result = GanttLanguage.getInstance().getText(key);
      return result == null ? key : result;
    }

    public String getValue(GPOptionGroup group, String canonicalKey) {
      String key = group == null ? null : group.getI18Nkey(canonicalKey);
      return getValue(key == null ? canonicalKey : key);
    }

    public String getPageTitle(String pageID) {
      return getValue(getCanonicalOptionPageTitleKey(pageID));
    }

    public String getPageDescription(String pageID) {
      return GanttLanguage.getInstance().getText(myOptionPageKeyPrefix + pageID + ".description");
    }

    public String getOptionGroupLabel(GPOptionGroup group) {
      String canonicalKey = getCanonicalOptionGroupLabelKey(group);
      return getValue(group, canonicalKey);
    }

    public String getOptionLabel(GPOptionGroup group, GPOption<?> option) {
      String result = null;
      if (group != null) {
        String keyWithGroup = String.format("%s%s.%s.label", myOptionKeyPrefix, group.getID(), option.getID());
        result = getValue(group, keyWithGroup);
        if (!Objects.equal(result, keyWithGroup)) {
          return result;
        }
      }
      String canonicalKey = getCanonicalOptionLabelKey(option);
      return getValue(group, canonicalKey);
    }

    public final String getCanonicalOptionPageLabelKey(String pageID) {
      return myOptionPageKeyPrefix + pageID + ".label";
    }

    public final String getCanonicalOptionPageTitleKey(String pageID) {
      return myOptionPageKeyPrefix + pageID + ".title";
    }

    public String getCanonicalOptionPageDescriptionKey(String pageID) {
      return myOptionPageKeyPrefix + pageID + ".description";
    }

    public final String getCanonicalOptionGroupLabelKey(GPOptionGroup group) {
      return myOptionGroupKeyPrefix + group.getID() + ".label";
    }

    public final String getCanonicalOptionLabelKey(GPOption<?> option) {
      return myOptionKeyPrefix + option.getID() + ".label";
    }

    public static final String getCanonicalOptionValueLabelKey(String valueID) {
      return "optionValue." + valueID + ".label";
    }

    String getAdvancedActionTitle() {
      return GanttLanguage.getInstance().getText("optionAdvanced.label");
    }

    String getColorButtonText(ColorOption colorOption) {
      return GanttLanguage.getInstance().getText("colorButton");
    }

    String getColorChooserTitle(ColorOption colorOption) {
      return GanttLanguage.getInstance().getText("selectColor");
    }
  }

  //private static GPColorChooser ourColorChooser = new GPColorChooser(ImmutableList.of(Color.BLACK, Color.RED, Color.GREEN, Color.BLUE));
}
