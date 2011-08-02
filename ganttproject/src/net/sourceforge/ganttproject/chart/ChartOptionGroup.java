package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.chart.ChartModelBase.OptionEventDispatcher;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

class ChartOptionGroup extends GPOptionGroup {

    private OptionEventDispatcher myEventDispatcher;

    public ChartOptionGroup(String id, GPOption[] options,
            ChartModelImpl.OptionEventDispatcher eventDispatcher) {
        super(id, options);
        myEventDispatcher = eventDispatcher;
    }

    public void commit() {
        super.commit();
        myEventDispatcher.optionsChanged();
    }

}
