package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
abstract class CommonPanel implements InternalStateListener {
    private final GanttLanguage language;

    private JLabel nameLabel3;

    private JLabel durationLabel3;

    private JTextField nameField3;

    private JTextField durationField3;

    private JPanel firstRowPanel3;

    private FlowLayout flowL = new FlowLayout(FlowLayout.LEFT, 10, 10);

    private GridBagConstraints gbc = new GridBagConstraints();

    private final Task myTask;

    public CommonPanel(Task task) {
        language = GanttLanguage.getInstance();
        myTask = task;
    }

    protected void addUsingGBL(Container container, Component component,

    GridBagConstraints gbc, int x,

    int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.weighty = 0;
        container.add(component, gbc);
        container.applyComponentOrientation(ComponentOrientation
                .getOrientation(Locale.getDefault()));
    }

    /** set the first row in all the tabbed pane. thus give them a common look */

    protected void setFirstRow(Container container, GridBagConstraints gbc,
            JLabel nameLabel, JTextField nameField, JLabel durationLabel,
            JTextField durationField) {
        container.setLayout(new GridBagLayout());
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 15;
        gbc.insets.left = 10;
        gbc.insets.top = 10;
        addUsingGBL(container, nameLabel, gbc, 0, 0, 1, 1);
        addUsingGBL(container, nameField, gbc, 1, 0, 1, 1);
        addUsingGBL(container, durationLabel, gbc, 2, 0, 1, 1);
        gbc.weightx = 1;
        addUsingGBL(container, durationField, gbc, 3, 0, 1, 1);
    }

    public void nameChanged(String newName) {
        nameField3.setText(newName);
    }

    public void durationChanged(int newDuration) {
        durationField3.setText("" + newDuration);
    }

    protected JPanel setupCommonFields(boolean onlyOneTask) {
        nameLabel3 = new JLabel(getLanguage().getText("name") + ":");
        nameField3 = new JTextField(20);
        nameField3.setText(getTask().getName());
        durationLabel3 = new JLabel(getLanguage().getText("length") + ":");
        durationField3 = new JTextField(8);
        durationField3.setText("" + getTask().getDuration().getLength());
        nameField3.setEditable(false);
        durationField3.setEditable(false);
        firstRowPanel3 = new JPanel(flowL);
        setFirstRow(firstRowPanel3, gbc, nameLabel3, nameField3,
                durationLabel3, durationField3);
        if (!onlyOneTask) {
            nameLabel3.setVisible(false);
            nameField3.setVisible(false);
        }
        return firstRowPanel3;
    }

    protected GanttLanguage getLanguage() {
        return language;
    }

    protected Task getTask() {
        return myTask;
    }
}
