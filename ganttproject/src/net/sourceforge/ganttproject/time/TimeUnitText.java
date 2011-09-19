package net.sourceforge.ganttproject.time;

import net.sourceforge.ganttproject.util.TextLengthCalculator;

public class TimeUnitText {
    private String myLongText;

    private String myMediumText;

    private String myShortText;

    private int myMinimaxLong = -1;

    private int myMinimaxMedium = -1;

    private int myMinimaxShort = -1;

    private Object myCalculatorState;

    public TimeUnitText(String longText, String mediumText, String shortText) {
        myLongText = longText;
        myMediumText = mediumText;
        myShortText = shortText;
    }

    public TimeUnitText(String mediumText) {
        myMediumText = mediumText;
        myLongText = mediumText;
        myShortText = mediumText;
    }

    public String getText(int maxLength) {
        return myMediumText;
    }

    public String getText(int requestedMaxLength,
            TextLengthCalculator calculator) {
        String result = null;
        if (!calculator.getState().equals(myCalculatorState)) {
            myCalculatorState = calculator.getState();
            myMinimaxLong = calculator.getTextLength(myLongText);
            myMinimaxMedium = calculator.getTextLength(myMediumText);
            myMinimaxShort = calculator.getTextLength(myShortText);
        }
        result = getCachedText(requestedMaxLength);
        return result == null ? "" : result;
    }

    private String getCachedText(int maxLength) {
        if (myMinimaxLong >= 0 && myMinimaxLong <= maxLength) {
            return myLongText;
        }
        if (myMinimaxMedium >= 0 && myMinimaxMedium <= maxLength) {
            return myMediumText;
        }
        if (myMinimaxShort >= 0 && myMinimaxShort <= maxLength) {
            return myShortText;
        }
        return null;
    }

    @Override
    public String toString() {
        return "long=" + myLongText + ", medium=" + myMediumText + ", short="
                + myShortText;
    }
}
