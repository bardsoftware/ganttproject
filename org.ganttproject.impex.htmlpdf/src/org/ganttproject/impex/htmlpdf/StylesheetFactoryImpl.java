/*
 * Created on 22.09.2005
 */
package org.ganttproject.impex.htmlpdf;

import java.net.URL;

import net.sourceforge.ganttproject.GPLogger;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

abstract class StylesheetFactoryImpl {
    Stylesheet[] createStylesheets(Class<? extends Stylesheet> stylesheetInterface) {
        IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
        IConfigurationElement[] configElements = extensionRegistry
                .getConfigurationElementsFor(stylesheetInterface.getName());
        Stylesheet[] result = (Stylesheet[])java.lang.reflect.Array.newInstance(
                stylesheetInterface, configElements.length);
        for (int i = 0; i < configElements.length; i++) {
            try {
                //Object nextExtension = configElements[i].createExecutableExtension("class");
                //assert nextExtension!=null && nextExtension instanceof HTMLStylesheet : "Extension="+nextExtension+" is expected to be instance of HTMLStylesheet";
                String localizedName = configElements[i].getAttribute("name");
                String pluginRelativeUrl = configElements[i].getAttribute("url");
                String namespace = configElements[i].getDeclaringExtension().getNamespaceIdentifier();
                URL stylesheetUrl = Platform.getBundle(namespace).getResource(pluginRelativeUrl);
                assert stylesheetUrl != null : "Failed to resolve url=" + pluginRelativeUrl;
                URL resolvedUrl = Platform.resolve(stylesheetUrl);
                assert resolvedUrl != null : "Failed to resolve URL=" + stylesheetUrl;
                result[i] = newStylesheet(resolvedUrl, localizedName);
            }
            catch(Exception e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            }
        }
        return result;
    }

    protected abstract Stylesheet newStylesheet(URL url, String localizedName);
}
