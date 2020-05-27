package net.sourceforge.ganttproject;

import junit.framework.TestCase;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GlobalTestLock extends TestCase {
    public GanttProject myproject;
    static final AtomicBoolean turn = new AtomicBoolean(true);
    protected final CountDownLatch conditionSucceeded = new CountDownLatch(1);
    private Timer timer = new Timer();

    static class CheckConditionTurn extends TimerTask{
        CountDownLatch latch;

        public CheckConditionTurn(CountDownLatch latch){
            this.latch = latch;
        }

        public void run(){
            if(AppKt.getMainWindow().get() != null){
                latch.countDown();
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        boolean success = false;
        while (!GlobalTestLock.turn.compareAndSet(true, false)) {}
        while(!success){
            AppKt.main(new String[]{});
            CheckConditionTurn task = new CheckConditionTurn(conditionSucceeded);
            timer.schedule(task, 0, 2000);
            success = conditionSucceeded.await(4, TimeUnit.SECONDS);
        }
        GPLogger.log("UI started, starting tests!");
        myproject = AppKt.getMainWindow().get();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        GPLogger.log("tests have finished!");
        AppKt.getMainWindow().set(null);
        GPLogger.log("Starting to close UI.");
        myproject.close();
        myproject.setVisible(false);
        myproject = null;
        GlobalTestLock.turn.set(true);
        GPLogger.log("UI closed!");
    }
}

