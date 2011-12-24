package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Label;
import net.sourceforge.ganttproject.util.TextLengthCalculator;

public interface TextSelector {
    Label[] getLabels(TextLengthCalculator textLengthCalculator);

    class Default {
        public static TextSelector singleChoice(final String text) {
            return new TextSelector() {
                @Override
                public Label[] getLabels(TextLengthCalculator textLengthCalculator) {
                    return new Label[] { new Label(text, textLengthCalculator.getTextLength(text)) };
                }
            };
        }
    }
}
