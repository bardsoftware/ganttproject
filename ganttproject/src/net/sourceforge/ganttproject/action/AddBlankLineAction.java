package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class AddBlankLineAction extends GPAction {

    private GanttTree2 myGanttTree;

    public AddBlankLineAction(GanttTree2 ganttTree) {
        myGanttTree = ganttTree;

    }

    protected String getIconFilePrefix() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        final DefaultMutableTreeNode selectedNode = myGanttTree
                .getSelectedNode();
        Mediator.getUndoManager().undoableEdit("add blank line",
                new Runnable() {
                    public void run() {
                        myGanttTree.addBlankLine(selectedNode,
                                (selectedNode == null ? -1 : selectedNode
                                        .getParent().getIndex(selectedNode)));
                    }
                });

    }

    protected String getLocalizedName() {
        return GanttLanguage.getInstance().getText("addBlankLine");
    }

}
