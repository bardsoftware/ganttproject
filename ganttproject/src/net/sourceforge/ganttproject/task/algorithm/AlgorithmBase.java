package net.sourceforge.ganttproject.task.algorithm;

public class AlgorithmBase {

    protected boolean isEnabled = true;

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    protected boolean isEnabled() {
        return isEnabled;
    }

}
