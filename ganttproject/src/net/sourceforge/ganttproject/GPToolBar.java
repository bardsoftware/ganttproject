/*
 * Created on 29.09.2005
 */
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class GPToolBar extends JToolBar {
    public static final Object SEPARATOR_OBJECT = new Object() {
        private final String myString = GanttLanguage.getInstance().getText("separator");
        public String toString() {
            return myString;
        }
    };
    
    private final GanttOptions options;
    private List<TestGanttRolloverButton> myButtons;
    
    public GPToolBar(String title, int toolBarPosition, GanttOptions options) {
        super(title, toolBarPosition);
        setBorderPainted(true);
        setRollover(true);
        setFloatable(true);        
        this.options = options;
    }
    
    void populate(List/*<JButton>*/ buttons) {
        removeAll();
        myButtons = new ArrayList<TestGanttRolloverButton>(buttons.size());
        for (int i = 0; i < buttons.size(); i++) {
            Object nextButton = buttons.get(i);
            if (GPToolBar.SEPARATOR_OBJECT.equals(nextButton)) {
                // int size = Integer.parseInt(options.getIconSize());
                // toolBar.addSeparator(new Dimension(size, size));
                ImageIcon icon;
                if (getOrientation() == JToolBar.HORIZONTAL) {
                    icon = new ImageIcon(getClass().getResource(
                            "/icons/sepV_16.png"));
                }
                else {
                    icon = new ImageIcon(getClass().getResource(
                            "/icons/sepH_16.png"));
                }
                add(new JLabel(icon));
            } else {
                add((AbstractButton) nextButton);
                if (nextButton instanceof TestGanttRolloverButton) {
                    myButtons.add((TestGanttRolloverButton) nextButton);
                }
            }
        }
        invalidate();
    }

    void updateButtonsLook() {
        for (int i = 0; i < myButtons.size(); i++) {
            TestGanttRolloverButton nextButton = myButtons.get(i);
            nextButton.setIconHidden(options.getButtonShow() == GanttOptions.TEXT);
            nextButton.setTextHidden(options.getButtonShow() == GanttOptions.ICONS);
        }
        invalidate();
    }
}
