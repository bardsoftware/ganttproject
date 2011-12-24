package net.sourceforge.ganttproject.util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import net.sourceforge.ganttproject.GPLogger;

import org.eclipse.core.runtime.Platform;

public class PropertiesUtil {
    public static void loadProperties(Properties result, String resource) {
        URL url = PropertiesUtil.class.getResource(resource);
        if (url == null) {
            return;
        }
        URL resolvedUrl;
        try {
            resolvedUrl = Platform.resolve(url);
            result.load(resolvedUrl.openStream());
        } catch (IOException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        }
    }

}
