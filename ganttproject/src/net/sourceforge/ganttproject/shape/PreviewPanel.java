package net.sourceforge.ganttproject.shape;

/*
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class PreviewPanel extends JPanel {
    protected ShapePaint pattern = ShapeConstants.DEFAULT;

    public PreviewPanel() {
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory
                .createTitledBorder("Preview"), BorderFactory
                .createEmptyBorder(0, 4, 8, 4)));
        setPreferredSize(new Dimension(70, 70));
    }

    public ShapePaint getPattern() {
        return new ShapePaint(pattern, getForeground(), getBackground());
    }

    public void setPattern(ShapePaint pattern) {
        this.pattern = pattern;
    }

    public void paintComponent(Graphics gc) {
        Graphics2D g = (Graphics2D) gc;
        int w = getSize().width;
        int h = getSize().height;
        g.setColor(getParent().getBackground());
        g.fillRect(0, 0, w, h);
        if (pattern == null)
            return;
        Insets insets = getInsets();
        Rectangle rect = new Rectangle(insets.left, insets.top, w
                - (insets.left + insets.right), h
                - (insets.top + insets.bottom));
        g.setPaint(new ShapePaint(pattern, getForeground(), getBackground()));
        g.fill(rect);

    }
}
