package net.sourceforge.ganttproject.shape;

import java.awt.Paint;

import javax.swing.JComboBox;

/**
 *@author Etienne L'kenfack (etienne.lkenfack@itcogita.com)
 */
public class JPaintCombo extends JComboBox {
    ///TODO Field is never read... Remove?
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
