/*
 * Based on Steven Spencer's Java Tip in JavaWorld:
 * http://www.javaworld.com/javaworld/javatips/jw-javatip66.html
 */

package net.sourceforge.ganttproject.util;

import java.io.IOException;
import java.net.URL;

/**
 * A simple, static class to display an URL in the system browser. Under
 * Windows, this will bring up the default browser, usually either Netscape or
 * Microsoft IE. The default browser is determined by the OS. This has been
 * tested under: Windows 95/98/NT/2000. Under MacOS, this will bring up the
 * default browser. The default browser is determined by the OS. This has been
 * tested under: n/a In other cases (and under Unix), the system browser is
 * hard-coded to be 'netscape'. Netscape must be in your PATH for this to work.
 * This has been tested with the following platforms: AIX, HP-UX and Solaris.
 * Examples: * BrowserControl.displayURL("http://www.javaworld.com")
 * BrowserControl.displayURL("file://c:\\docs\\index.html")
 * BrowserContorl.displayURL("file:///user/joe/index.html"); Note - you must
 * include the url type -- either "http://" or "file://".
 */
public class BrowserControl {

    /**
     * Display an URL in the system browser. If you want to display a file, you
     * must include the absolute path name.
     * 
     * @param url
     *            the document's url (the url must start with either "http://"
     *            or "file://").
     */
    public static boolean displayURL(String url) {

        // Opening a browser, even when running sandbox-restricted
        // in JavaWebStart.
        try {
            Class serManClass = Class.forName("javax.jnlp.ServiceManager");
            Class basSerClass = Class.forName("javax.jnlp.BasicService");
            Class[] stringParam = { String.class };
            Class[] urlParam = { URL.class };

            Object basicService = serManClass.getMethod("lookup", stringParam)
                    .invoke(serManClass,
                            new Object[] { "javax.jnlp.BasicService" });
            basSerClass.getMethod("showDocument", urlParam).invoke(
                    basicService, new Object[] { new URL(url) });

            return true;
        } catch (Exception e) {
            // Not running in JavaWebStart or service is not supported.
            // We continue with the methods below ...
        }

        String[] cmd = null;

        switch (getPlatform()) {
        case (WIN_ID):
            return runCmdLine(replaceToken(WIN_CMDLINE, URLTOKEN, url));
        case (MAC_ID):
            return runCmdLine(replaceToken(MAC_CMDLINE, URLTOKEN, url));
        default:
            for (int i = 0; i < OTHER_CMDLINES.length; i++) {
                if (runCmdLine(replaceToken(OTHER_CMDLINES[i], URLTOKEN, url),
                        replaceToken(OTHER_FALLBACKS[i], URLTOKEN, url)))
                    return true;
            }
        }

        return false;
    }

    /**
     * Try to determine whether this application is running under Windows or
     * some other platform by examing the "os.name" property.
     * 
     * @return the ID of the platform
     */
    private static int getPlatform() {
        String os = System.getProperty("os.name");
        if (os != null && os.startsWith(WIN_PREFIX))
            return WIN_ID;
        if (os != null && os.startsWith(MAC_PREFIX))
            return MAC_ID;
        return OTHER_ID;
    }

    private static String connectStringArray(String[] a) {
        if (a == null)
            return null;

        String s = "";
        for (int i = 0; i < a.length; i++) {
            if (i > 0)
                s += " ";
            s += a[i];
        }

        return s;
    }

    private static String[] replaceToken(String[] target, String token,
            String replacement) {
        if (null == target)
            return null;
        String[] result = new String[target.length];

        for (int i = 0; i < target.length; i++)
            result[i] = target[i].replaceAll(token, replacement);

        return result;
    }

    private static boolean runCmdLine(String[] cmdLine) {
        return runCmdLine(cmdLine, null);
    }

    private static boolean runCmdLine(String[] cmdLine, String[] fallBackCmdLine) {
        try {

            System.err.println("Trying to invoke browser, cmd='"
                    + connectStringArray(cmdLine) + "' ... ");
            Process p = Runtime.getRuntime().exec(cmdLine);

            if (null != fallBackCmdLine) {
                // wait for exit code -- if it's 0, command worked,
                // otherwise we need to start fallBackCmdLine.
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    System.err.println(exitCode);
                    System.err.println();

                    System.err.println("Trying to invoke browser, cmd='"
                            + connectStringArray(fallBackCmdLine) + "' ...");
                    Runtime.getRuntime().exec(fallBackCmdLine);

                }
            }

            System.err.println();
            return true;

        } catch (InterruptedException e) {
            System.err.println("Caught: " + e);
        } catch (IOException e) {
            System.err.println("Caught: " + e);
        }

        System.err.println();
        return false;
    }

    // This token is a placeholder for the actual URL
    private static final String URLTOKEN = "%URLTOKEN%";

    // Used to identify the windows platform.
    private static final int WIN_ID = 1;

    // Used to discover the windows platform.
    private static final String WIN_PREFIX = "Windows";

    // The default system browser under windows.
    // Once upon a time:
    // for 'Windows 9' and 'Windows M': start
    // for 'Windows': cmd /c start
    private static final String[] WIN_CMDLINE = { "rundll32",
            "url.dll,FileProtocolHandler", URLTOKEN };

    // Used to identify the mac platform.
    private static final int MAC_ID = 2;

    // Used to discover the mac platform.
    private static final String MAC_PREFIX = "Mac";

    // The default system browser under mac.
    private static final String[] MAC_CMDLINE = { "open", URLTOKEN };

    // Used to identify the mac platform.
    private static final int OTHER_ID = -1;

    private static final String[][] OTHER_CMDLINES = {

    // Try to invoke the browser specified in the BROWSER environment variable
            // Comment because this method, because it use a deprecated method
            // and cause exception
            // GetEnv.GetEnvironement(URLTOKEN),

            // The first guess for a browser under other systems (and unix):
            // Remote controlling mozilla
            // (http://www.mozilla.org/unix/remote.html)
            { "mozilla", "-remote", "openURL(" + URLTOKEN + ",new-window)" },

            // The second guess for a browser under other systems (and unix):
            // The RedHat skript htmlview
            { "htmlview", URLTOKEN },

            // The third guess for a browser under KDE:
            // Remote controlling konqueror
            { "konqueror", URLTOKEN },

            // The fourth guess for a browser under other systems (and unix):
            // Remote controlling netscape
            // (http://wp.netscape.com/newsref/std/x-remote.html)
            { "netscape", "-remote", "openURL(" + URLTOKEN + ")" }

    };

    private static final String[][] OTHER_FALLBACKS = {

    // Fallback for remote controlling mozilla:
            // Starting up a new mozilla
            { "mozilla", URLTOKEN },

            // No fallback for htmlview
            null,

            // Fallback for remote controlling netscape:
            // Starting up a new netscape
            { "netscape", URLTOKEN }

    };

}
