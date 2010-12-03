package net.sourceforge.ganttproject.shape;

import java.awt.Paint;

import javax.swing.JComboBox;

/**
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */
public class JPaintCombo extends JComboBox {
    public JPaintCombo(Paint[] list) {
        super(list);
        setRenderer(new PaintCellRenderer());
    }

    public Paint getSelectedPaint() {
        return (Paint) getSelectedItem();
    }
}
