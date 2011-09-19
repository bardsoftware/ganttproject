package net.sourceforge.ganttproject.task;

import javax.swing.tree.DefaultMutableTreeNode;

public class BlankLineNode extends DefaultMutableTreeNode {

    public static final String BLANK_LINE = "";

    public BlankLineNode(){
        super(BLANK_LINE);
    }
}
