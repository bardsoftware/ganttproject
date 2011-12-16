package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.util.TextLengthCalculator;

public interface TextSelector {
    String getText(TextLengthCalculator textLengthCalculator);

    class Default {
        public static TextSelector singleChoice(final String text) {
            return new TextSelector() {
                @Override
                public String getText(TextLengthCalculator textLengthCalculator) {
                    return text;
                }
            };
        }
    }
}
