package net.sourceforge.ganttproject.test.time;

import biz.ganttproject.core.time.impl.GregorianTimeUnitStack;
import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 *
 * @author bard Date: 01.02.2004
 */
public class GregorianTimeStackTest extends TestCase {
    public GregorianTimeStackTest(String name) {
        super(name);
    }

    public void testDayContains1Day() throws Exception {
        assertTrue("Day isn't constructed from days :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.DAY));
        assertEquals("Unexpected days count in one day", 1,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.DAY));
    }

    public void testDayContains24Hours() throws Exception {
        assertTrue("Day isn't constructed from hours :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.HOUR));
        assertEquals("Unexpected hours count in one day", 24,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.HOUR));
    }

    public void testDayContains1440Minutes() throws Exception {
        assertTrue("Day isn't constructed from minutes :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.MINUTE));
        assertEquals("Unexpected minutes count in one day", 1440,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.MINUTE));
    }

    public void testDayContains86400Seconds() throws Exception {
        assertTrue("Day isn't constructed from seconds :(",
                GregorianTimeUnitStack.DAY
                        .isConstructedFrom(GregorianTimeUnitStack.SECOND));
        assertEquals("Unexpected minutes count in one day", 86400,
                GregorianTimeUnitStack.DAY
                        .getAtomCount(GregorianTimeUnitStack.SECOND));
    }
}
