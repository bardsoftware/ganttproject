package net.sourceforge.ganttproject.plugins;

import java.lang.reflect.Array;
import java.util.ArrayList;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.export.Exporter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * Very basic Plugin Manager
 * @author bbaranne
 */
public class PluginManager {

    private static final String EXTENSION_POINT_ID_CHART = "net.sourceforge.ganttproject.chart";

    private static final String EXTENSION_POINT_ID_EXPORTER = "net.sourceforge.ganttproject.exporter";

    private Chart[] myCharts;

    private Exporter[] myExporters;

    public Object[] getExtensions(Class extensionPointInterface) {
        String extensionPointID = extensionPointInterface.getName();
        return getExtensions(extensionPointID, extensionPointInterface);
    }

    public Object[] getExtensions(String extensionPointID, Class extensionPointInterface) {
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] configElements = extensionRegistry
                .getConfigurationElementsFor(extensionPointID);

        ArrayList<Object> extensions = new ArrayList<Object>();
        for (int i = 0; i < configElements.length; i++) {
            try {
                Object nextExtension = configElements[i]
                        .createExecutableExtension("class");
                assert nextExtension!=null && extensionPointInterface.isAssignableFrom(nextExtension.getClass());
                extensions.add(nextExtension);
            } catch (CoreException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            }
        }
        return extensions.toArray((Object[])Array.newInstance(extensionPointInterface, 0));
    }

    public Chart[] getCharts() {
        if (myCharts == null) {
            myCharts = (Chart[]) getExtensions(EXTENSION_POINT_ID_CHART, Chart.class);
        }
        return myCharts;
    }

    public Exporter[] getExporters() {
        if (myExporters == null) {
            myExporters = (Exporter[]) getExtensions(EXTENSION_POINT_ID_EXPORTER, Exporter.class);
        }
        return myExporters;

    }
}
