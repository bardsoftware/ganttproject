/***************************************************************************
 GeneralDialog.java
 ------------------------------------------
 begin                : 29 juin 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * General options dialog box
 *
 * @author athomas
 */
public abstract class GeneralDialog extends JDialog implements ActionListener,
        TreeSelectionListener {

    /** Language of the application. */
    protected static final GanttLanguage language = GanttLanguage.getInstance();

    protected final GanttProject appli;

    /** The tree to set the menus. */
    protected final JTree treeSections;

    /** The root node of the Tree */
    protected final DefaultMutableTreeNode rootNode;

    /** The model for the JTree */
    protected final DefaultTreeModel treeModel;

    // some buttons
    protected final JButton okButton, cancelButton;

    /** The panel to create a new settings page. */
    protected GeneralOptionPanel settingPanel;

    /** The main panel of the dialog. */
    protected final JPanel mainPanel2 = new JPanel();

    protected final JPanel southPanel;

    public GeneralDialog(GanttProject parent, String title, boolean modal,
            GeneralOptionPanel firstPanel) {
        super(parent, language.correctLabel(title), modal);

        appli = parent;

        // tree section.
        rootNode = new DefaultMutableTreeNode("Options");
        treeModel = new DefaultTreeModel(rootNode);
        treeSections = new JTree(treeModel);
        treeSections.setEditable(false);
        treeSections.setBackground(new Color(1.0f, 1.0f, 1.0f));
        treeSections.setShowsRootHandles(true);
        treeSections.setRowHeight(20);
        treeSections.setRootVisible(false);
        treeSections.setCellRenderer(new SettinsDefaultTreeCellRenderer());
        treeSections.addTreeSelectionListener(this);
        JScrollPane scrollpane = new JScrollPane(treeSections);
        JPanel treePanel = new JPanel();
        treePanel.add(scrollpane);

        JPanel mainPanel = new JPanel(new BorderLayout());
        if (language.getComponentOrientation() == ComponentOrientation.LEFT_TO_RIGHT) {
            mainPanel.add(treePanel, BorderLayout.WEST);
        } else {
            mainPanel.add(scrollpane, BorderLayout.EAST);
        }
        scrollpane.setPreferredSize(new Dimension(150, 450));

        // construct the south panel
        southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 50, 10));

        okButton = new JButton(language.getText("ok"));
        okButton.addActionListener(this);
        southPanel.add(okButton);

        cancelButton = new JButton(language.getText("cancel"));
        cancelButton.addActionListener(this);

        southPanel.add(cancelButton);
        southPanel.setPreferredSize(new Dimension(600, 50));
        Box southBox = Box.createVerticalBox();
        southBox.add(new JSeparator());
        southBox.add(southPanel);
        mainPanel.add(southBox, BorderLayout.SOUTH);

        // set the ok button as default action for enter
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(okButton);
        }

        // add the main panel into the dialog
        JPanel tmpPanel = new JPanel(new BorderLayout());
        tmpPanel.add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(tmpPanel);

        // construct the sections
        constructSections();

        settingPanel = firstPanel;

        Box vb = Box.createVerticalBox();
        vb.add(TopPanel.create("  " + settingPanel.getTitle(), settingPanel
                .getComment()));
        vb.add(settingPanel);
        settingPanel.initialize();

        // - add the settingPanel into the main Panel
        mainPanel2.add(vb, 0);
        mainPanel.add(mainPanel2);

        // position the window
        this.pack();
        // setResizable(false);
        DialogAligner.center(this, getParent());

        applyComponentOrientation(language.getComponentOrientation());
    }

    /** Construct the menu settings. */
    public abstract void constructSections();

    /** Add a sub section. */
    public DefaultMutableTreeNode addObject(Object child,
            DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
        if (parent == null)
            parent = rootNode;
        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
        treeSections.scrollPathToVisible(new TreePath(childNode.getPath()));
        return childNode;
    }

    /** action performed for listeners. */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == okButton) {
            // okButton clicked
            settingPanel.applyChanges(false);
            this.setVisible(false);
            getUIFacade().getGanttChart().reset();
            dispose();
        } else if (event.getSource() == cancelButton) {
            // cancelButton clicked
            settingPanel.rollback();
            this.setVisible(false);
            dispose();
        }
    }

    /** Class to render the tree */
    public class SettinsDefaultTreeCellRenderer extends DefaultTreeCellRenderer
            implements TreeCellRenderer {

        public SettinsDefaultTreeCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {

            String text = String.valueOf(((DefaultMutableTreeNode) value)
                    .getUserObject());
            setFont(Fonts.GENERAL_DIALOG_FONT);
            setIcon(new ImageIcon(getClass().getResource("/icons/blank.gif")));
            setText(text);
            setBackground(selected ? new Color((float) 0.290, (float) 0.349,
                    (float) 0.643) : Color.white);
            setForeground(selected ? Color.white : Color.black);
            return this;
        }
    }

    protected IGanttProject getProject() {
        return appli;
    }

    protected UIFacade getUIFacade() {
        return appli.getUIFacade();
    }
}
