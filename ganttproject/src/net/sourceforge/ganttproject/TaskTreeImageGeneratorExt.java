package net.sourceforge.ganttproject;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.List;

import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.task.Task;

public class TaskTreeImageGeneratorExt extends TaskTreeImageGenerator {
    TaskTreeImageGeneratorExt(GanttTree2 treeView, UIConfiguration uiConfiguration) {
        super(treeView, uiConfiguration);
    }

    @Override
    protected Dimension calculateDimension(List taskNodes) {
        return super.calculateDimension(taskNodes);
    }

    @Override
    protected void paint(Image image, Dimension d, List taskNodes) {
        super.paint(image, d, taskNodes);
    }

    @Override
    protected void paintTask(Graphics g, PaintState state, Task t) {
        super.paintTask(g, state, t);
    }
}
