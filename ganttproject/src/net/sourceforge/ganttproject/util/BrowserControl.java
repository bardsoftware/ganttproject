/*
 * Based on Steven Spencer's Java Tip in JavaWorld:
 * http://www.javaworld.com/javaworld/javatips/jw-javatip66.html
 */

package net.sourceforge.ganttproject.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * <p>
 * A simple, static class to display an URL in the system browser.
 * </p>
 * <p>
 * Under Windows, this will bring up the default browser, usually either
 * Netscape or Microsoft IE. The default browser is determined by the OS. This
 * has been tested under: Windows 95/98/NT/2000.
 * </p>
 * <p>
 * Under MacOS, this will bring up the default browser. The default browser is
 * determined by the OS. This has been tested under: n/a
 * </p>
 * <p>
 * Under (K)Ubuntu, Debian and other *nix platforms, try sensible-browser. It
 * checks $BROWSER and the variable is not available <a
 * href="http://wiki.debian.org/DebianAlternatives">Debian Alternatives</a>. If
 * that fails, fallback on the '<em>other platforms</em>' methods. This has been
 * tested under: Kubuntu 11.10
 * </p>
 * <p>
 * In other platforms, a range of known browsers is invoked.
 * </p>
 * <p>
 * Examples:
 * <ul>
 * <li>BrowserControl.displayURL("http://www.javaworld.com")</li>
 * <li>BrowserControl.displayURL("file://c:\\docs\\index.html")</li>
 * <li>BrowserContorl.displayURL("file:///user/joe/index.html")</li>
 * </p>
 * <p>
 * Note - you must include the url type -- either "http://" or "file://".
 * </p>
 */
public class BrowserControl {

  private static boolean displayUrlWithDesktopApi(String url) {
    if (!java.awt.Desktop.isDesktopSupported()) {
      return false;
    }
    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
    if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
      return false;
    }
    try {
      desktop.browse(new URI(url));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private static boolean displayUrlWithJnlpApi(String url) {
    // Opening a browser, even when running sandbox-restricted
    // in JavaWebStart.
    try {
      Class<?> serManClass = Class.forName("javax.jnlp.ServiceManager");
      Class<?> basSerClass = Class.forName("javax.jnlp.BasicService");
      Class<?>[] stringParam = { String.class };
      Class<?>[] urlParam = { URL.class };

      Object basicService = serManClass.getMethod("lookup", stringParam).invoke(serManClass,
          new Object[] { "javax.jnlp.BasicService" });
      basSerClass.getMethod("showDocument", urlParam).invoke(basicService, new Object[] { new URL(url) });

      return true;
    } catch (Exception e) {
      return false;
      // Not running in JavaWebStart or service is not supported.
      // We continue with the methods below ...
    }
  }

  /**
   * Display an URL in the system browser. If you want to display a file, you
   * must include the absolute path name.
   * 
   * @param url
   *          the document's url (the url must start with either "http://" or
   *          "file://").
   * @return true when the method succeeded in displaying the URL in the system
   *         browser
   */
  public static boolean displayURL(String url) {
    if (displayUrlWithDesktopApi(url)) {
      return true;
    }
    if (displayUrlWithJnlpApi(url)) {
      return true;
    }
    Platforms platform = getPlatform();
    switch (platform) {
    case WINDOWS:
      return runCmdLine(replaceToken(WIN_CMDLINE, URLTOKEN, url));
    case MAC:
      return runCmdLine(replaceToken(MAC_CMDLINE, URLTOKEN, url));
    case LINUX:
      if (runCmdLine(replaceToken(LINUX_CMDLINE, URLTOKEN, url))) {
        // Succeeded
        return true;
      }
      // Fallback on 'brute-force' method
    }

    // Try out a series of commands and hope one is recognized...
    assert OTHER_CMDLINES.length == OTHER_FALLBACKS.length;
    for (int i = 0; i < OTHER_CMDLINES.length; i++) {
      if (runCmdLine(replaceToken(OTHER_CMDLINES[i], URLTOKEN, url), replaceToken(OTHER_FALLBACKS[i], URLTOKEN, url))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Try to determine whether this application is running under Windows or some
   * other platform by examining the "os.name" property.
   * 
   * @return the ID of the platform
   */
  private static Platforms getPlatform() {
    String os = System.getProperty("os.name");
    if (os != null && os.startsWith(WIN_PREFIX)) {
      return Platforms.WINDOWS;
    }
    if (os != null && os.startsWith(MAC_PREFIX)) {
      return Platforms.MAC;
    }
    if (os != null && os.startsWith(LINUX_PREFIX)) {
      return Platforms.LINUX;
    }
    return Platforms.OTHER;
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

  private static String[] replaceToken(String[] target, String token, String replacement) {
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

  // TODO Maybe make method a little less chatty...
  private static boolean runCmdLine(String[] cmdLine, String[] fallBackCmdLine) {
    try {
      System.err.println("Trying to invoke browser, cmd='" + connectStringArray(cmdLine) + "' ... ");
      Process p = Runtime.getRuntime().exec(cmdLine);

      int exitCode = p.waitFor();
      if (exitCode == 0) {
        // Succeeded!
        System.err.println();
        return true;
      }
      System.err.println(exitCode);
      System.err.println();

    } catch (InterruptedException e) {
      System.err.println("Caught: " + e);
    } catch (IOException e) {
      System.err.println("Caught: " + e);
    }

    // Failed, caught exception or exitCode indicated an error
    if (null != fallBackCmdLine) {
      // Start fallBackCmdLine
      try {
        System.err.println("Trying to invoke browser, cmd='" + connectStringArray(fallBackCmdLine) + "' ...");
        Process p = Runtime.getRuntime().exec(fallBackCmdLine);
        int exitCode = p.waitFor();
        if (exitCode == 0) {
          // Succeeded!
          System.err.println();
          return true;
        }
      } catch (InterruptedException e) {
        System.err.println("Caught: " + e);
      } catch (IOException e) {
        System.err.println("Caught: " + e);
      }
    }

    System.err.println();
    return false;
  }

  /** Available/Supported platforms for opening URLs in browsers */
  private enum Platforms {
    /** Used to identify the Windows platform. */
    WINDOWS,
    /** Used to identify the <ac platform. */
    MAC,
    /** Used to identify the (generic) Linux platform. */
    LINUX,
    /** Unable to identify the platform */
    OTHER
  };

  // This token is a place holder for the actual URL
  private static final String URLTOKEN = "%URLTOKEN%";

  // Used to discover the windows platform.
  private static final String WIN_PREFIX = "Windows";

  // The default system browser under windows.
  // Once upon a time:
  // for 'Windows 9' and 'Windows M': start
  // for 'Windows': cmd /c start
  private static final String[] WIN_CMDLINE = { "rundll32", "url.dll,FileProtocolHandler", URLTOKEN };

  // Used to discover the mac platform.
  private static final String MAC_PREFIX = "Mac";

  // The default system browser under mac.
  private static final String[] MAC_CMDLINE = { "open", URLTOKEN };

  // Used to discover the Linux platform.
  private static final String LINUX_PREFIX = "Linux";

  // The default definition for the preferred browser under Linux
  private static final String[] LINUX_CMDLINE = { "sensible-browser", URLTOKEN };

  private static final String[][] OTHER_CMDLINES = {

      // Try to invoke the browser specified in the BROWSER environment
      // variable
      // Comment because this method, because it use a deprecated method
      // and cause exception
      // GetEnv.GetEnvironement(URLTOKEN),

      // The first guess for a browser under other systems (and unix):
      // Remote controlling mozilla
      // (http://www.mozilla.org/unix/remote.html)
      { "mozilla", "-remote", "openURL(" + URLTOKEN + ",new-window)" },

      // Next guess for a browser under other systems (and unix):
      // The RedHat script htmlview
      { "htmlview", URLTOKEN },

      // Next guess, try Opera (if a user installed it, it is probably
      // 'more-wanted' than the default browser)
      // See /usr/share/applications/opera-browser.desktop
      { "opera", "-remote", "openURL(" + URLTOKEN + ")" },

      // Next guess for a browser under Gnome: try FireFox (
      // See /usr/share/applications/firefox.desktop
      { "firefox", URLTOKEN },

      // Next guess for a browser under KDE4: rekonq
      // See /usr/share/applications/kde4/rekonq.desktop
      { "rekonq", URLTOKEN },

      // Next guess for a browser under other systems (and unix):
      // Remote controlling netscape
      // (http://wp.netscape.com/newsref/std/x-remote.html)
      { "netscape", "-remote", "openURL(" + URLTOKEN + ")" } };

  private static final String[][] OTHER_FALLBACKS = {
      // Fallback for remote controlling mozilla:
      // Starting up a new mozilla
      { "mozilla", URLTOKEN },

      // No fallback for htmlview
      null,

      // Fallback for Opera: Opera-Next (alpha/development version
      // of Opera, can be separately used next to Opera)
      // See /usr/share/applications/opera-next-browser.desktop
      { "opera-next", "-remote", "openURL(" + URLTOKEN + ")" },

      // No fallback for FireFox
      null,

      // Fallback for rekonq: old KDE browser is konqueror
      { "konqueror", URLTOKEN },

      // Fallback for remote controlling netscape:
      // Starting up a new netscape
      { "netscape", URLTOKEN } };
}
