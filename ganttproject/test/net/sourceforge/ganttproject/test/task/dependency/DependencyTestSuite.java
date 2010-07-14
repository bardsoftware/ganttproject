/*
 * Created on 12.11.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.test.task.dependency;

import junit.framework.TestSuite;

public class DependencyTestSuite extends TestSuite{
	public DependencyTestSuite() {
		addTestSuite(TestTaskDependencyCommon.class);
		addTestSuite(TestSupertaskAdjustment.class);
		addTestSuite(TestRecalculateTaskScheduleAlgorithm.class);
		addTestSuite(TestDependencyActivityBinding.class);
		addTestSuite(TestDependencyCycle.class);
	}
	
	public void testFoo() {
		
	}
}
