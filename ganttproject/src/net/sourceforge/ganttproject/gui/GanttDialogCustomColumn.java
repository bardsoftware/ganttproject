package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.BooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultBooleanOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;

/**
 * Dialog opened to create a new customColumn.
 * 
 * @author bbaranne Mar 2, 2005
 */
public class GanttDialogCustomColumn  {
    private static GanttLanguage language = GanttLanguage.getInstance();

    private static String cardInteger = language.getText("integer");

    private static String cardText = language.getText("text");

    private static String cardDouble = language.getText("double");

    private static String cardDate = language.getText("date");

    private static String cardBoolean = language.getText("boolean");

    /**
     * Created CustomColumn.
     */
    private CustomColumn customColumn = null;

    private JPanel panelDefaultValue = null;

    private CardLayout cardLayoutDefaultValue = null;

    private JPanel panelText = null;

    private JPanel panelInteger = null;

    private JPanel panelDouble = null;

    private Component panelDate = null;

    private JComponent panelBoolean = null;

    private JTextArea textArea = null;

    private GTextField fieldInteger = null;

    private GTextField fieldDouble = null;

    private JRadioButton radioTrue = null;

    private JRadioButton radioFalse = null;

    //private GanttCalendar date = null;
    private DateOption myDate = new DefaultDateOption("taskProperties.customColumn.defaultDate") {
        public void setValue(Date value) {
            super.setValue(value);
            commit();
            lock();
        }
        
    };
    private StringOption myName = new DefaultStringOption("taskProperties.customColumn.name") {
        public void setValue(String value) {
            super.setValue(value);
            commit();
            lock();
        }
    };

    private EnumerationOption myType = new DefaultEnumerationOption("taskProperties.customColumn.type", CustomColumnsStorage.availableTypes) {
        public void setValue(String value) {
            super.setValue(value);
            commit();
            int selectedIndex = getSelectedIndex(value);
            switch (selectedIndex) {
            case 0:
                cardLayoutDefaultValue.show(panelDefaultValue, cardText);
                break;
            case 1:
                cardLayoutDefaultValue.show(panelDefaultValue, cardInteger);
                break;
            case 2:
                cardLayoutDefaultValue.show(panelDefaultValue, cardDouble);
                break;
            case 3:
                cardLayoutDefaultValue.show(panelDefaultValue, cardDate);
                break;
            case 4:
                cardLayoutDefaultValue.show(panelDefaultValue, cardBoolean);
                break;
            }
            lock();
        }
        private int getSelectedIndex(String value) {
            return getSelectedType(value);
        }
    };
    
    private int getSelectedType(String typeName) {
        for (int i=0; i<CustomColumnsStorage.availableTypes.size(); i++) {
            if (CustomColumnsStorage.availableTypes.get(i).equals(typeName)) {
                return i;
            }
        }
        return -1;
        
    }
    
    private BooleanOption myDefaultValue = new DefaultBooleanOption("taskProperties.customColumn.defaultValue") {
		public void toggle() {
			super.toggle();
			commit();
			lock();
			GanttDialogCustomColumn.this.setDefaultValuePanelEnabled(isChecked());
		}
    	
    };
    private final UIFacade myUIFacade;
    private final GPOption[] myOptions = new GPOption[] {myName, myType};
    private final GPOptionGroup myOptionGroup = new GPOptionGroup("taskProperties.customColumn", myOptions);
    private final GPOptionGroup myDefaultValueOptionGroup = 
        new GPOptionGroup("taskProperties.customColumn.defaultValue", new GPOption[] {myDefaultValue});
	private boolean isOk;
    
    public GanttDialogCustomColumn(UIFacade uiFacade, CustomColumn customCol) {
        myUIFacade = uiFacade;
        customColumn = customCol;
        myOptionGroup.lock();
        myDate.lock();
        myDate.setValue(new Date());
        myName.setValue("");
        myOptionGroup.setTitled(false);
        myDefaultValueOptionGroup.setTitled(false);
        myDefaultValueOptionGroup.lock();
        isOk = false;
    }

    protected void setDefaultValuePanelEnabled(boolean enabled) {
    	UIUtil.setEnabledTree(panelDefaultValue, enabled);
	}

	public void setVisible(boolean visible) {
        Component rootComponent = getComponent();
        getUIFacade().showDialog(rootComponent, new Action[] {
                new OkAction() {
                    public void actionPerformed(ActionEvent e) {
                        myOptionGroup.commit();
                        myDate.commit();
                        GanttDialogCustomColumn.this.ok();
                    }}, 
                new CancelAction() {
                    public void actionPerformed(ActionEvent e) {
                        myOptionGroup.rollback();
                        myDate.rollback();
                    }
        }});
    }
    
    private UIFacade getUIFacade() {
        return myUIFacade;
    }
    private Component getComponent() {
        OptionsPageBuilder builder = new OptionsPageBuilder();
        OptionsPageBuilder.I18N i18n = new OptionsPageBuilder.I18N() {
			public String getOptionLabel(GPOptionGroup group, GPOption option) {
				if (option==myDefaultValue) { 
					return language.getText("defaultValue");
				}
				if (option==myDate) {
				    return "";
				}
				return super.getOptionLabel(group, option);
			}
        };
        builder.setI18N(i18n);
        {
            // Text
            textArea = new JTextArea();
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setColumns(25);
            textArea.setRows(5);
            JScrollPane jsp = new JScrollPane(textArea);
            panelText = new JPanel(new BorderLayout());
            panelText.add(jsp, BorderLayout.CENTER);
        }
        {
            // Boolean
            radioTrue = new JRadioButton(language.getText("yes"));
            radioFalse = new JRadioButton(language.getText("no"));
            ButtonGroup group = new ButtonGroup();
            group.add(radioFalse);
            group.add(radioTrue);
            radioTrue.setSelected(true);
            
            Box box = Box.createVerticalBox();
            box.add(radioTrue);
            box.add(radioFalse);
            panelBoolean = box;
        }
        {
            // Integer
            fieldInteger = new GTextField();
            fieldInteger.setPattern(GTextField.PATTERN_INTEGER);
            fieldInteger.setColumns(10);
            fieldInteger.setText("0");
            panelInteger = new JPanel(new BorderLayout());
            panelInteger.add(fieldInteger, BorderLayout.NORTH);
        }
        {
            // Double
            fieldDouble = new GTextField();
            fieldDouble.setPattern(GTextField.PATTERN_DOUBLE);
            fieldDouble.setColumns(10);
            fieldDouble.setText("0.0");
            panelDouble = new JPanel(new BorderLayout());
            panelDouble.add(fieldDouble, BorderLayout.NORTH);
        }
        {
            panelDate = builder.createStandaloneOptionPanel(myDate);
            cardLayoutDefaultValue = new CardLayout();
            panelDefaultValue = new JPanel(cardLayoutDefaultValue);
            panelDefaultValue.add(cardText, panelText);
            panelDefaultValue.add(cardBoolean, panelBoolean);
            panelDefaultValue.add(cardInteger, panelInteger);
            panelDefaultValue.add(cardDouble, panelDouble);
            panelDefaultValue.add(cardDate, panelDate);
        }
        
        Component optionsComponent = builder.createGroupComponent(myOptionGroup);
        
        
        Box result = Box.createVerticalBox();
        result.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        //result.setLayout(new BorderLayout());
        result.add(optionsComponent);
        result.add(Box.createVerticalStrut(10));
        result.add(builder.createGroupComponent(myDefaultValueOptionGroup));
        result.add(panelDefaultValue);
        setDefaultValuePanelEnabled(false);
        return result;
    } // TODO change the default value for custom columns.

    private void ok() {
        //Mediator.getGanttProjectSingleton().setAskForSave(true);
        //String colName = fieldName.getText().trim();
        String colName = myName.getValue();
        Object defValue = null;

        if (colName.length() != 0) {
            Class colClass;
            int colType = getSelectedType(myType.getValue());
            switch (colType) {
            case 0:
                colClass = String.class;
                defValue = textArea.getText();
                break;
            case 1:
                colClass = Integer.class;
                String ti = fieldInteger.getText();
                if (ti.trim().length() > 0) {
                    defValue = new Integer(Integer.parseInt(ti));
                }
                break;
            case 2:
                colClass = Double.class;
                String td = fieldDouble.getText();
                if (td.trim().length() > 0) {
                    defValue = new Double(Double.parseDouble(td));
                }
                break;
            case 3:
                colClass = GregorianCalendar.class;
                defValue = myDate.getValue()==null ? null : new GanttCalendar(myDate.getValue());
                break;
            case 4:
                colClass = Boolean.class;
                defValue = new Boolean(radioTrue.isSelected());
                break;
            default: // normally never reached.
                colClass = String.class;
                defValue = "default";
            }

            if (customColumn != null) {
                customColumn.setName(colName);
                customColumn.setType(colClass);
                if (myDefaultValue.isChecked() && defValue!=null) {
                	customColumn.setDefaultValue(defValue);
                }
            }
            isOk = true;
        }
    }
    
    public boolean isOk() {
    	return isOk;
    }
}
