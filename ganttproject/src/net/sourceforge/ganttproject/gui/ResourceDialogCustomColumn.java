package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultEnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.ResourceColumn;

/**
 * Dialog opened to create a new Resource CustomColumn.
 * 
 * @author bbaranne Mar 2, 2005 (modified Nov 2005, nokiljevic)
 * 
 */
public class ResourceDialogCustomColumn  {
    private static GanttLanguage language = GanttLanguage.getInstance();

    private static String cardInteger = language.getText("integer");

    private static String cardText = language.getText("text");

    private static String cardDouble = language.getText("double");

    private static String cardDate = language.getText("date");

    private static String cardBoolean = language.getText("boolean");

    public static String[] availableTypes = {cardText,cardInteger,cardDouble,cardDate,cardBoolean};

    /**
     * Created CustomColumn.
     */
    private ResourceColumn resourceColumn = null;
    
    private JPanel panelDefaultValue = null;

    private CardLayout cardLayoutDefaultValue = null;

    private JPanel panelText = null;

    private JPanel panelInteger = null;

    private JPanel panelDouble = null;

    private Component panelDate = null;

    private JPanel panelBoolean = null;

    private JTextArea textArea = null;

    private GTextField fieldInteger = null;

    private GTextField fieldDouble = null;

    private JRadioButton radioTrue = null;

    private JRadioButton radioFalse = null;

    private JLabel labelDefaultValue = null;

    //private GanttCalendar date = null;
    // TODO all the options use the translations for task columns. should create new properties (resourceProperties) and the translations for every one of them
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

    private EnumerationOption myType = new DefaultEnumerationOption("taskProperties.customColumn.type", availableTypes) {
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
        for (int i=0; i<availableTypes.length; i++) {
            if (availableTypes[i].equals(typeName)) {
                return i;
            }
        }
        return -1;
        
    }
    private final UIFacade myUIFacade;
    private final GPOption[] myOptions = new GPOption[] {myName, myType};
    private final GPOptionGroup myOptionGroup = new GPOptionGroup("taskProperties.customColumn", myOptions);

	private boolean isOk = false;
    
    public ResourceDialogCustomColumn(UIFacade uiFacade, ResourceColumn col) {
    	myUIFacade = uiFacade;
        resourceColumn = col;
        myOptionGroup.lock();
        myDate.lock();
        myOptionGroup.setTitled(false);
    }

    public void setVisible(boolean visible) {
        Component rootComponent = getComponent();
        getUIFacade().showDialog(rootComponent, new Action[] {
                new OkAction() {
                    public void actionPerformed(ActionEvent e) {
                        myOptionGroup.commit();
                        myDate.commit();
                        ResourceDialogCustomColumn.this.ok();
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
        JPanel result = new JPanel();
        GridBagConstraints constraints = new GridBagConstraints();

        OptionsPageBuilder builder = new OptionsPageBuilder();
        // TODO Label is never added to the UI!
        labelDefaultValue = new JLabel(language.getText("defaultValue") + ": ");

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
            panelBoolean = new JPanel(new GridBagLayout());
            constraints.gridx = 0;
            constraints.gridy = 0;
            panelBoolean.add(radioTrue, constraints);
            constraints.gridx = 0;
            constraints.gridy = 1;
            panelBoolean.add(radioFalse, constraints);
        }
        {
            // Integer
            fieldInteger = new GTextField();
            fieldInteger.setPattern(GTextField.PATTERN_INTEGER);
            fieldInteger.setColumns(10);
            panelInteger = new JPanel(new GridBagLayout());
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            panelInteger.add(fieldInteger, constraints);
        }
        {
            // Double
            fieldDouble = new GTextField();
            fieldDouble.setPattern(GTextField.PATTERN_DOUBLE);
            fieldDouble.setColumns(10);
            panelDouble = new JPanel(new GridBagLayout());
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            panelDouble.add(fieldDouble, constraints);
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
        
        Component optionsComponent = builder.buildPlanePage(new GPOptionGroup[] {myOptionGroup});
        result.setLayout(new BorderLayout());
        result.add(optionsComponent, BorderLayout.CENTER);
        result.add(panelDefaultValue, BorderLayout.SOUTH);
        return result;
    } // TODO change the default value for custom columns.

    private void ok() {
        Mediator.getGanttProjectSingleton().setAskForSave(true);
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
                if (ti.trim().length() == 0)
                    ti = "0";
                defValue = new Integer(Integer.parseInt(ti));
                break;
            case 2:
                colClass = Double.class;
                String td = fieldDouble.getText();
                if (td.trim().length() == 0)
                    td = "0.0";
                defValue = new Double(Double.parseDouble(td));
                break;
            case 3:
                colClass = GregorianCalendar.class;
                if (myDate.getValue() == null)
                	defValue = new  GanttCalendar();
                else
                	defValue = new GanttCalendar(myDate.getValue());
                break;
            case 4:
                colClass = Boolean.class;
                defValue = new Boolean(radioTrue.isSelected());
                break;
            default: // normally never reached.
                colClass = String.class;
                defValue = "default";
            }

            if (resourceColumn != null) {
            	resourceColumn.setTitle(colName);
            	resourceColumn.setType(colClass);
            	resourceColumn.setDefaultVal(defValue);
            }
            isOk = true;
        }/* else
        {
            fieldName.requestFocus();
            // nothing (the dialog stays opened)
        }
        */
    }
    
    public ResourceColumn getColumn(){
    	return this.resourceColumn;
    }

	public boolean isOk() {
		return isOk;
	}
    
}
