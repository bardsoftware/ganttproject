package net.sourceforge.ganttproject.util;

public interface TextLengthCalculator {
    int getTextLength(String text);
    int getTextHeight(String text);
    Object getState();
}
