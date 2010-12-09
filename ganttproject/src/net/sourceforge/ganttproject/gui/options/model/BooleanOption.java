package net.sourceforge.ganttproject.gui.options.model;

public interface BooleanOption extends GPOption {
    boolean isChecked();

    void toggle();
}
