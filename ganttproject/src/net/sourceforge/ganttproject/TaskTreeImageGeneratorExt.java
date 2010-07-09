package net.sourceforge.ganttproject;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.List;

import javax.swing.table.JTableHeader;

import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.Task;

public class TaskTreeImageGeneratorExt extends TaskTreeImageGenerator {
    TaskTreeImageGeneratorExt(GanttTree2 treeView, UIConfiguration uiConfiguration) {
        super(treeView, uiConfiguration);
    }

    @Override
    protected Dimension calculateDimension(List taskNodes) {
        Dimension d = super.calculateDimension(taskNodes);
        return new Dimension(getTree().getTreeTable().getWidth(), d.height);
    }

    @Override
    protected void paint(Image image, Dimension d, List taskNodes) {
        super.paint(image, d, taskNodes);
        // Insert a bitmap of the Table Header region to complete the
        // generation of the Task tree image.
        JTableHeader ganttTaskHeader = getTree().getTable().getTableHeader();
        image.getGraphics().translate(0, HEADER_OFFSET);
        ganttTaskHeader.paint(image.getGraphics());
    }

    @Override
    protected void paintTask(Graphics g, PaintState state, Task t) {
        super.paintTask(g, state, t);
    }
}
