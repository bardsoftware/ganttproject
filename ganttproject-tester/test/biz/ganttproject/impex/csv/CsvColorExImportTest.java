package biz.ganttproject.impex.csv;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import com.google.common.base.Supplier;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManagerImpl;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.TaskManager;

import java.awt.*;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class CsvColorExImportTest extends TestCase {

    private static final String FILENAME_TEMPLATE = "test_gp_color_export_%d.csv";

    private File testfile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TaskDefaultColumn.setLocaleApi(new TaskDefaultColumn.LocaleApi() {
            @Override
            public String i18n(String key) {
                return GanttLanguage.getInstance().getText(key);
            }
        });
        GanttLanguage.getInstance().setShortDateFormat(new SimpleDateFormat("dd/MM/yy"));
        long timestamp = System.currentTimeMillis();
        String tmpdir = System.getProperty("java.io.tmpdir");
        testfile = new File(tmpdir, String.format(FILENAME_TEMPLATE, timestamp));
        if (testfile.exists()) {
            throw new FileAlreadyExistsException("The output file already exists");
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        testfile.delete();
    }

    public void testExportImportWithColor() throws IOException {
        TaskManager outTaskManager = TestSetupHelper.newTaskManagerBuilder().build();
        GanttTask task0 = outTaskManager.createTask();
        GanttTask task1 = outTaskManager.createTask();
        GanttTask task2 = outTaskManager.createTask();
        int task0id = task0.getTaskID();
        int task1id = task1.getTaskID();
        int task2id = task2.getTaskID();
        task0.setName("task0");
        task1.setName("task1");
        task2.setName("task2");
        task0.setColor(Color.RED);
        task1.setColor(Color.GREEN);
        task2.setColor(new Color(42, 42, 42));

        CSVOptions csvOptions = new CSVOptions();
        //Without this, the test fails with an UnsupportedOperationException. Taken and adapted from GPCsvExportTest
        for (Map.Entry<String, BooleanOption> entry : csvOptions.getTaskOptions().entrySet()) {
            if (TaskDefaultColumn.find(entry.getKey()) == TaskDefaultColumn.OUTLINE_NUMBER) {
                entry.getValue().setValue(false);
            }
        }
        GanttCSVExport exporter = new GanttCSVExport(
                outTaskManager,
                new HumanResourceManager(null, new CustomColumnsManager()),
                new RoleManagerImpl(),
                csvOptions
        );
        OutputStream os = new FileOutputStream(testfile);
        exporter.save(os);
        os.close();

        TestSetupHelper.TaskManagerBuilder inBuilder = TestSetupHelper.newTaskManagerBuilder();
        TaskManager inTaskManager = inBuilder.build();
        GanttCSVOpen importer = new GanttCSVOpen(
                new Supplier<Reader>() {
                    @Override
                    public Reader get() {
                        try {
                            return new FileReader(testfile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                },
                inTaskManager,
                null,
                null,
                inBuilder.getTimeUnitStack()
        );
        importer.load();

        assertEquals(3, inTaskManager.getTaskCount());
        assertEquals(Color.RED, inTaskManager.getTask(task0id).getColor());
        assertEquals(Color.GREEN, inTaskManager.getTask(task1id).getColor());
        assertEquals(new Color(42, 42, 42), inTaskManager.getTask(task2id).getColor());

    }

}
