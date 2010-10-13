/**
 * 
 */
package net.sourceforge.ganttproject.chart.overview;

import javax.swing.JLabel;

class PanelBorder extends JLabel {
    PanelBorder() {
        super("<html><b>&nbsp;&nbsp;&nbsp;</b></html>");
        setOpaque(true);
        setBackground(HighlightOnMouseOver.getSelectionBackground());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(HighlightOnMouseOver.getSelectionBackground());
    }


}