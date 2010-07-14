package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.ResourcesTableModel;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.ResourceAssignmentCollection;
import net.sourceforge.ganttproject.task.Task;

import org.jdesktop.swing.JXTable;
import org.jdesktop.swing.decorator.Highlighter;
import org.jdesktop.swing.decorator.HighlighterPipeline;

/**
 * @author Dmitry.Barashev
 */
public class TaskAllocationsPanel extends CommonPanel {
    private JPanel resourcesPanel;

    private ResourcesTableModel myResourcesTableModel;

    private JXTable resourcesTable;

    private final HumanResourceManager myHRManager;

    private JScrollPane resourcesScrollPane;

    private final RoleManager myRoleManager;

    private boolean onlyOneTask = false;

    public TaskAllocationsPanel(Task task, HumanResourceManager hrManager,
            RoleManager roleMgr, boolean onlyOneTask) {
        super(task);
        myHRManager = hrManager;
        myRoleManager = roleMgr;
        this.onlyOneTask = onlyOneTask;
    }

    public JPanel getComponent() {
        if (resourcesPanel == null) {
            constructResourcesPanel(getTask().getAssignmentCollection());
        }
        return resourcesPanel;
    }

    private void constructResourcesPanel(
            ResourceAssignmentCollection assignments) {

        resourcesPanel = new JPanel(new GridBagLayout());
        myResourcesTableModel = new ResourcesTableModel(assignments);
        resourcesTable = new JXTable(myResourcesTableModel);
        setUpResourcesComboColumn(resourcesTable); // set column editor
        setUpCoordinatorBooleanColumn(resourcesTable);
        setUpRolesComboColumn(resourcesTable);
        resourcesTable.setRowHeight(23);
        resourcesTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        resourcesTable.getColumnModel().getColumn(1).setPreferredWidth(240);
        resourcesTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        resourcesTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        resourcesTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        resourcesTable.setHighlighters(new HighlighterPipeline(
                new Highlighter[] { new Highlighter(
                        new Color(0xFF, 0xFF, 0xFF), null) }));
        resourcesScrollPane = new JScrollPane(resourcesTable);
        resourcesScrollPane.setPreferredSize(new Dimension(500, 130));
        JPanel secondResourcesScrollPane = new JPanel();
        secondResourcesScrollPane.setBorder(new TitledBorder(
                new EtchedBorder(), GanttProject.correctLabel(getLanguage()
                        .getText("human"))));
        secondResourcesScrollPane.add(resourcesScrollPane);

        JButton bremove = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/delete_16.gif")));
        bremove.setToolTipText(GanttProject.getToolTip(getLanguage().getText(
                "removeResources")));
        bremove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int[] selectedRow = resourcesTable.getSelectedRows();
                for (int i = 0; i < selectedRow.length; ++i) {
                    resourcesTable.getModel().setValueAt(null, selectedRow[i],
                            1);
                }
            }
        });
        secondResourcesScrollPane.add(bremove);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 15;
        gbc.insets.left = 10;
        gbc.insets.top = 10;
        gbc.weighty = 0;

        JPanel commonFields = setupCommonFields(onlyOneTask);

        addUsingGBL(resourcesPanel, commonFields, gbc, 0, 0, 1, 1);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 8;
        gbc.gridheight = 1;
        gbc.weighty = 1;
        resourcesPanel.add(secondResourcesScrollPane, gbc);
    }

    public ResourcesTableModel getTableModel() {
        return myResourcesTableModel;
    }

    private void setUpCoordinatorBooleanColumn(final JTable resourceTable) {
        TableColumn resourcesColumn = resourceTable.getColumnModel().getColumn(
                3);
        resourcesColumn.setCellRenderer(new BooleanRenderer());
    }

    private void setUpResourcesComboColumn(final JTable resourceTable) {
        List resources = myHRManager.getResources();
        final JComboBox comboBox = new JComboBox();
        for (int i = 0; i < resources.size(); i++) {
            comboBox.addItem(resources.get(i));
        }

        TableColumn resourcesColumn = resourceTable.getColumnModel().getColumn(
                1);
        comboBox.setEditable(false);
        resourcesColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }

    private void setUpRolesComboColumn(final JTable resourceTable) {
        final Role[] roles = myRoleManager.getEnabledRoles();
        final JComboBox comboBox = new JComboBox();

        for (int i = 0; i < roles.length; i++) {
            Role role = (Role) roles[i];
            comboBox.addItem(role);
        }

        TableColumn rolesColumn = resourceTable.getColumnModel().getColumn(4);
        comboBox.setEditable(false);
        rolesColumn.setCellEditor(new DefaultCellEditor(comboBox));
        rolesColumn.setCellRenderer(new DefaultTableCellRenderer());
    }

    static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        public BooleanRenderer() {
            super();
            setHorizontalAlignment(JLabel.CENTER);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            if (!value.getClass().equals(Boolean.class))
                setSelected(false);
            else
                setSelected((value != null && ((Boolean) value).booleanValue()));

            return this;
        }
    }
}
