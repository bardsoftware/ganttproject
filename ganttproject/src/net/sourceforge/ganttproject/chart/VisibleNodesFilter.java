package net.sourceforge.ganttproject.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class VisibleNodesFilter {
    public List/* <Task> */getVisibleNodes(JTree jtree, int minHeight,
            int maxHeight, int nodeHeight) {
        List preorderedNodes = Collections.list(((DefaultMutableTreeNode) jtree
                .getModel().getRoot()).preorderEnumeration());
        List result = new ArrayList();
        int currentHeight = 0;
        for (int i = 1; i < preorderedNodes.size(); i++) {
            DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode) preorderedNodes
                    .get(i);
            if (false==nextNode.getUserObject() instanceof Task) {
                continue;
            }
            if ((currentHeight+nodeHeight) > minHeight
                    && jtree.isVisible(new TreePath(nextNode.getPath()))) {
                result.add(nextNode.getUserObject());
            }
            if (jtree.isVisible(new TreePath(nextNode.getPath()))) {
                currentHeight += nodeHeight;
            }
            if(currentHeight > minHeight + maxHeight) {
                break;
            }
        }
        return result;
    }

}
