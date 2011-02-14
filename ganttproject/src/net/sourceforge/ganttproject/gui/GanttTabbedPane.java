package net.sourceforge.ganttproject.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import net.sourceforge.ganttproject.ChartTabContentPanel;
import net.sourceforge.ganttproject.util.TextLengthCalculator;
import net.sourceforge.ganttproject.util.TextLengthCalculatorImpl;
import net.sourceforge.ganttproject.util.collect.Pair;

public class GanttTabbedPane extends JTabbedPane {

    private Map<Component, Object> myUserObjectsMap = new HashMap<Component, Object>();

    private class ToolbarCaptionApiImpl implements ChartTabContentPanel.ToolbarCaptionApi {
        private Map<String, Pair<JLabel, Point>> myCaptions = new HashMap<String, Pair<JLabel, Point>>();
        @Override
        public Component getContainer() {
            return GanttTabbedPane.this;
        }

        @Override
        public void addCaption(JLabel label, Point pos) {
            Pair<JLabel, Point> existing = myCaptions.get(label.getText());
            if (existing == null || !existing.second().equals(pos)) {
                myCaptions.put(label.getText(), Pair.create(label, pos));
                repaint();
            }
        }

        private void paint(Graphics g) {
            //g = g.create();
            int dx = 0;
            int dy = 0;
            for (Pair<JLabel, Point> labelAndPoint : myCaptions.values()) {
                Point pos = labelAndPoint.second();
                JLabel label = labelAndPoint.first();
//                dx += labelAndPoint.second().x;
//                dy = labelAndPoint.second().y;
//                g.translate(labelAndPoint.second().x, labelAndPoint.second().y);
//                labelAndPoint.first().paint(g);
                g.setFont(label.getFont());
                TextLengthCalculator calc = new TextLengthCalculatorImpl(g);
                int textLength = calc.getTextLength(label.getText()) + 4;
                int textHeight = calc.getTextHeight(label.getText()) + 4;
                g.setColor(label.getBackground());
                g.fillRect(pos.x - 2, pos.y - textHeight + 2, textLength, textHeight);
                g.setColor(label.getBackground().darker());
                g.drawRect(pos.x - 2, pos.y - textHeight + 2, textLength, textHeight);
                g.setColor(label.getForeground());
                g.drawString(labelAndPoint.first().getText(), pos.x, pos.y);
            }
            g.translate(-dx, -dy);
        }
    }

    private ToolbarCaptionApiImpl myToolbarCaptionApi = new ToolbarCaptionApiImpl();

    public GanttTabbedPane() {
        super();
    }

    public GanttTabbedPane(int tabPlacement) {
        super(tabPlacement);
    }

    public GanttTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);

    }

    public void addTab(String title, Component component, Object userObject) {
        super.addTab(title, component);
        myUserObjectsMap.put(component, userObject);
    }

    public void addTab(String title, Icon icon, Component component,
            Object userObject) {
        super.addTab(title, icon, component);
        myUserObjectsMap.put(component, userObject);
    }

    public void addTab(String title, Icon icon, Component component,
            String tip, Object userObject) {
        super.addTab(title, icon, component, tip);
        myUserObjectsMap.put(component, userObject);
    }

    public Object getSelectedUserObject() {
        Object selectedComp = this.getSelectedComponent();
        return myUserObjectsMap.get(selectedComp);
    }

    public ToolbarCaptionApiImpl getToolbarCaptionsApi() {
        return myToolbarCaptionApi;
    }

        @Override
        protected void paintChildren(Graphics g) {
            // TODO Auto-generated method stub
            super.paintChildren(g);
            myToolbarCaptionApi.paint(g);
        }


}
