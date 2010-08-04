package net.sourceforge.ganttproject.shape;

/*
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */

import java.awt.Paint;

import javax.swing.JComboBox;

public class JPaintCombo extends JComboBox {
    private Paint[] myList = null;

    public JPaintCombo(Paint[] list) {
        super(list);
        myList = list;
        setRenderer(new PaintCellRenderer());
    }

    public Paint getSelectedPaint() {
        return (Paint) getSelectedItem();
    }

}
