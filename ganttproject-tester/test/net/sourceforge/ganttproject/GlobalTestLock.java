package net.sourceforge.ganttproject;

import junit.framework.TestCase;

// this lock is shared between the different test classes that make use of the gui. This is important as there
// are multiple checks for concurrent modifications

public abstract class GlobalTestLock extends TestCase {
    // global lock for UI tests, 1 means lock is available, 0 means lock is taken
    private int lock = 1;
    public static GanttProject myproject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        while(lock == 0){}

        // take lock
        lock = 0;

        if(GlobalTestLock.myproject == null){
            AppKt.main(new String[]{});
            while(GlobalTestLock.myproject == null){
                GlobalTestLock.myproject = AppKt.getMainWindow().get();
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        GlobalTestLock.myproject.setModified(false);
        GlobalTestLock.myproject.quitApplication();
        while(GlobalTestLock.myproject.isVisible()){}
        GlobalTestLock.myproject = null;
        lock = 1;
    }
}

