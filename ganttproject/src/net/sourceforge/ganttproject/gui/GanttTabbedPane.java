package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTabbedPane;

public class GanttTabbedPane extends JTabbedPane {

    private Map<Component, Object> myUserObjectsMap = new HashMap<Component, Object>();
    

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

}
