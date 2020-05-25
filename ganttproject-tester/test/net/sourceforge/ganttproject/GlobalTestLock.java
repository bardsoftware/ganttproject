package net.sourceforge.ganttproject;

import junit.framework.TestCase;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GlobalTestLock extends TestCase {
    public GanttProject myproject;
    static final AtomicBoolean turn = new AtomicBoolean(true);

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        while(!GlobalTestLock.turn.compareAndSet(true, false)){}
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
        GlobalTestLock.turn.set(true);
    }
}

