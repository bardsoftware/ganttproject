package net.sourceforge.ganttproject.resource;

import net.sourceforge.ganttproject.resource.HumanResourceMerger.MergeResourcesOption;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import junit.framework.TestCase;

public class TestImportResources extends TestCase {

    public void testMergeResourcesByName() {
        MergeResourcesOption mergeOption = new MergeResourcesOption();
        mergeOption.setValue(MergeResourcesOption.BY_NAME);

        HumanResourceManager mergeTo = new HumanResourceManager(null, new CustomColumnsManager());
        mergeTo.add(new HumanResource("joe", 1, mergeTo));
        mergeTo.add(new HumanResource("john", 2, mergeTo));

        HumanResourceManager mergeFrom = new HumanResourceManager(null, new CustomColumnsManager());
        mergeFrom.add(new HumanResource("jack", 1, mergeFrom));
        mergeFrom.add(new HumanResource("joe", 2, mergeFrom));

        mergeTo.importData(mergeFrom, new OverwritingMerger(mergeOption));

        assertEquals(3, mergeTo.getResources().size());
        assertEquals("joe", mergeTo.getById(1).getName());
        assertEquals("john", mergeTo.getById(2).getName());
        assertEquals("jack", mergeTo.getById(3).getName());
    }

    public void testMergeByID() {
        MergeResourcesOption mergeOption = new MergeResourcesOption();
        mergeOption.setValue(MergeResourcesOption.BY_ID);

        HumanResourceManager mergeTo = new HumanResourceManager(null, new CustomColumnsManager());
        mergeTo.add(new HumanResource("joe", 1, mergeTo));
        mergeTo.add(new HumanResource("john", 2, mergeTo));

        HumanResourceManager mergeFrom = new HumanResourceManager(null, new CustomColumnsManager());
        mergeFrom.add(new HumanResource("jack", 1, mergeFrom));
        mergeFrom.add(new HumanResource("joe", 3, mergeFrom));

        mergeTo.importData(mergeFrom, new OverwritingMerger(mergeOption));

        assertEquals(3, mergeTo.getResources().size());
        assertEquals("jack", mergeTo.getById(1).getName());
        assertEquals("john", mergeTo.getById(2).getName());
        assertEquals("joe", mergeTo.getById(3).getName());
    }


}
