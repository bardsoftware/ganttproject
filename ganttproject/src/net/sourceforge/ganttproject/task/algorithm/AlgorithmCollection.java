package net.sourceforge.ganttproject.task.algorithm;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class AlgorithmCollection {
    private final FindPossibleDependeesAlgorithm myFindPossibleDependeesAlgorithm;

    private final RecalculateTaskScheduleAlgorithm myRecalculateTaskScheduleAlgorithm;

    private final AdjustTaskBoundsAlgorithm myAdjustTaskBoundsAlgorithm;

    private final RecalculateTaskCompletionPercentageAlgorithm myCompletionPercentageAlgorithm;

    private final ProjectBoundsAlgorithm myProjectBoundsAlgorithm;

    private final ShiftTaskTreeAlgorithm myShiftTaskTreeAlgorithm;

    private final CriticalPathAlgorithm myCriticalPathAlgorithm;

    public AlgorithmCollection(
            FindPossibleDependeesAlgorithm myFindPossibleDependeesAlgorithm,
            RecalculateTaskScheduleAlgorithm recalculateTaskScheduleAlgorithm,
            AdjustTaskBoundsAlgorithm adjustTaskBoundsAlgorithm,
            RecalculateTaskCompletionPercentageAlgorithm completionPercentageAlgorithm,
            ProjectBoundsAlgorithm projectBoundsAlgorithm,
            CriticalPathAlgorithm criticalPathAlgorithm) {
        this.myFindPossibleDependeesAlgorithm = myFindPossibleDependeesAlgorithm;
        myRecalculateTaskScheduleAlgorithm = recalculateTaskScheduleAlgorithm;
        myAdjustTaskBoundsAlgorithm = adjustTaskBoundsAlgorithm;
        myCompletionPercentageAlgorithm = completionPercentageAlgorithm;
        myProjectBoundsAlgorithm = projectBoundsAlgorithm;
        myShiftTaskTreeAlgorithm = new ShiftTaskTreeAlgorithm();
        myCriticalPathAlgorithm = criticalPathAlgorithm;
    }

    public FindPossibleDependeesAlgorithm getFindPossibleDependeesAlgorithm() {
        return myFindPossibleDependeesAlgorithm;
    }

    public RecalculateTaskScheduleAlgorithm getRecalculateTaskScheduleAlgorithm() {
        return myRecalculateTaskScheduleAlgorithm;
    }

    public AdjustTaskBoundsAlgorithm getAdjustTaskBoundsAlgorithm() {
        return myAdjustTaskBoundsAlgorithm;
    }

    public RecalculateTaskCompletionPercentageAlgorithm getRecalculateTaskCompletionPercentageAlgorithm() {
        return myCompletionPercentageAlgorithm;
    }

    public ProjectBoundsAlgorithm getProjectBoundsAlgorithm() {
        return myProjectBoundsAlgorithm;
    }

    public ShiftTaskTreeAlgorithm getShiftTaskTreeAlgorithm() {
        return myShiftTaskTreeAlgorithm;
    }
    
    public CriticalPathAlgorithm getCriticalPathAlgorithm() {
        return myCriticalPathAlgorithm;
    }

}
