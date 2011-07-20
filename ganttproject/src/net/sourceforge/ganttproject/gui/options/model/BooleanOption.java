package net.sourceforge.ganttproject.gui.options.model;

public interface BooleanOption extends GPOption<Boolean> {
    boolean isChecked();

    void toggle();
}
