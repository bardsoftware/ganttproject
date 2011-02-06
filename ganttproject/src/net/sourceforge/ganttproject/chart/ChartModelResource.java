package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.ColorOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultColorOption;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeUnitStack;

public class ChartModelResource extends ChartModelBase {

    private final HumanResourceManager myManager;

    private final GPOptionGroup myColorOptions;

    private final ColorOption myResourceNormalLoadOption;

    private final ColorOption myResourceOverloadOption;

    private final ColorOption myResourceUnderloadOption;

    private final ColorOption myDayOffOption;

    private final ResourceChart myResourceChart;

    private static class ResourceLoadOption extends DefaultColorOption implements GP1XOptionConverter {
        private final String myTagName;
        private final String myAttributeName;

        ResourceLoadOption(String id, String tagName, String attributeName) {
            super(id);
            myTagName = tagName;
            myAttributeName = attributeName;
        }

        public String getTagName() {
            return myTagName;
        }

        public String getAttributeName() {
            return myAttributeName;
        }

        public void loadValue(String legacyValue) {
            loadPersistentValue(legacyValue);
        }
    }
    public ChartModelResource(TaskManager taskManager,
            HumanResourceManager resourceManager, TimeUnitStack timeUnitStack,
            final UIConfiguration projectConfig, ResourceChart resourceChart) {
        super(taskManager, timeUnitStack, projectConfig);
        myResourceChart = resourceChart;
        ResourceLoadRenderer resourceLoadRenderer = new ResourceLoadRenderer(this, resourceChart);
        addRenderer(resourceLoadRenderer);
        myManager = resourceManager;
        {
            myResourceNormalLoadOption = new ResourceLoadOption("resourceChartColors.normalLoad", "colors", "resources") {
                public void commit() {
                    super.commit();
                    projectConfig.setResourceColor(getValue());
                }
            };
            myResourceNormalLoadOption.lock();
            myResourceNormalLoadOption.setValue(new Color(140, 182, 206));
            myResourceNormalLoadOption.commit();
        }
        {
            myResourceOverloadOption = new ResourceLoadOption("resourceChartColors.overLoad", "colors", "resourceOverload") {
                public void commit() {
                    super.commit();
                    projectConfig.setResourceOverloadColor(getValue());
                }
            };
            myResourceOverloadOption.lock();
            myResourceOverloadOption.setValue(new Color(229, 50, 50));
            myResourceOverloadOption.commit();
        }
        {
            myResourceUnderloadOption = new DefaultColorOption("resourceChartColors.underLoad") {
                public void commit() {
                    super.commit();
                    projectConfig.setResourceUnderloadColor(getValue());
                }
            };
            myResourceUnderloadOption.lock();
            myResourceUnderloadOption.setValue(new Color(50, 229, 50));
            myResourceUnderloadOption.commit();
        }
        {
            myDayOffOption = new DefaultColorOption("resourceChartColors.dayOff") {
                public void commit() {
                    super.commit();
                    projectConfig.setDayOffColor(getValue());
                }
            };
            myDayOffOption.lock();
            myDayOffOption.setValue(new Color(0.9f, 1f, 0.17f));
            myDayOffOption.commit();
        }
        myColorOptions = new GPOptionGroup("resourceChartColors",
                new GPOption[] { myResourceNormalLoadOption,
                        myResourceOverloadOption, myResourceUnderloadOption,
                        myDayOffOption });
    }

//    public void paint(Graphics g) {
//        super.paint(g);
//        myResourceLoadRenderer.render();
//        myResourceLoadRenderer.getPrimitiveContainer().paint(getPainter(), g);
//    }

	public HumanResource[] getVisibleResources() {
		return myManager.getResources().toArray(new HumanResource[0]);
	}

    public GPOptionGroup[] getChartOptionGroups() {
        List<GPOptionGroup> result = new ArrayList<GPOptionGroup>();
        // FIXME Need to add superGroups to result?? (or what else is the point of fetching them)
        GPOptionGroup[] superGroups = super.getChartOptionGroups();
        result.add(myColorOptions);
        return result.toArray(new GPOptionGroup[result.size()]);
    }

    @Override
    protected int getRowCount() {
        return getVisibleResources().length;
    }

    @Override
    public ChartModelBase createCopy() {
        ChartModelBase result = new ChartModelResource(myTaskManager,
                myManager, myTimeUnitStack, getProjectConfig(), myResourceChart);
        super.setupCopy(result);
        return result;
    }

    public Task findTaskWithCoordinates(int x, int y) {
        // TODO Auto-generated method stub
        return null;
    }

    public Rectangle getBoundingRectangle(Task task) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setTaskContainment(TaskContainmentHierarchyFacade taskContainment) {
        // TODO Auto-generated method stub
    }

    public void setVisibleTasks(List<Task> visibleTasks) {
        // TODO Auto-generated method stub
    }

    public int calculateRowHeight() {
        return getChartUIConfiguration().getRowHeight();
    }
}