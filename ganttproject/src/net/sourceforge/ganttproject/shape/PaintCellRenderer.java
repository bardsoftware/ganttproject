package net.sourceforge.ganttproject.shape;

/*
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class PaintCellRenderer extends JPanel implements ListCellRenderer {
    protected Border normalBorder = new LineBorder(Color.white, 2);

    protected Border selectBorder = new LineBorder(Color.black, 2);

    protected Border focusBorder = new LineBorder(Color.blue, 2);

    protected Paint paint;

    public PaintCellRenderer() {
        setPreferredSize(new Dimension(70, 16));
    }

    @Override
    public void paintComponent(Graphics gc) {
        Graphics2D g = (Graphics2D) gc;
        int w = getSize().width;
        int h = getSize().height;
        Insets insets = getInsets();
        Rectangle rect = new Rectangle(insets.left, insets.top, w
                - (insets.left + insets.right), h
                - (insets.top + insets.bottom));
        g.setPaint(paint);
        g.fill(rect);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean hasFocus) {
        Border border = isSelected ? selectBorder : normalBorder;
        setBorder(hasFocus ? focusBorder : border);
        if (value instanceof Paint) {
            paint = (Paint) value;
        }
        return this;
    }
}
