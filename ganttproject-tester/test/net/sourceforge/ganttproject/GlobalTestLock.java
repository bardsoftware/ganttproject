package net.sourceforge.ganttproject;

import junit.framework.TestCase;

public abstract class GlobalTestLock extends TestCase {
    public GanttProject myproject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        AppKt.main(new String[]{});
        while(myproject == null) {
            myproject = AppKt.getMainWindow().get();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        AppKt.getMainWindow().set(null);
        myproject.close();
        myproject.setVisible(false);
        myproject.dispose();
        myproject = null;
    }
}

