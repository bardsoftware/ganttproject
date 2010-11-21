package net.sourceforge.ganttproject.gui.options.model;

public interface GP1XOptionConverter {
    String getTagName();
    String getAttributeName();
    void loadValue(String legacyValue);
}
