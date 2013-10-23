package net.sourceforge.ganttproject.document.webdav;

import io.milton.httpclient.ProxyDetails;
import biz.ganttproject.core.option.DefaultStringOption;
import junit.framework.TestCase;

public class WebDavProxyTest extends TestCase {
  public void testProxyOptionParsing() {
    assertNull(MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", null)));
    assertNull(MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", "")));

    {
      ProxyDetails proxyDetails = MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", "system"));
      assertTrue(proxyDetails.isUseSystemProxy());
    }
    {
      ProxyDetails proxyDetails = MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", "proxy"));
      assertFalse(proxyDetails.isUseSystemProxy());
      assertEquals("proxy", proxyDetails.getProxyHost());
    }
    {
      ProxyDetails proxyDetails = MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", "proxy:3128"));
      assertFalse(proxyDetails.isUseSystemProxy());
      assertEquals("proxy", proxyDetails.getProxyHost());
      assertEquals(3128, proxyDetails.getProxyPort());
    }
    {
      ProxyDetails proxyDetails = MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", "user@proxy:3128"));
      assertFalse(proxyDetails.isUseSystemProxy());
      assertEquals("proxy", proxyDetails.getProxyHost());
      assertEquals(3128, proxyDetails.getProxyPort());
      assertEquals("user", proxyDetails.getUserName());
    }
    {
      ProxyDetails proxyDetails = MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", "user:password@proxy:3128"));
      assertFalse(proxyDetails.isUseSystemProxy());
      assertEquals("proxy", proxyDetails.getProxyHost());
      assertEquals(3128, proxyDetails.getProxyPort());
      assertEquals("user", proxyDetails.getUserName());
      assertEquals("password", proxyDetails.getPassword());
    }
    {
      ProxyDetails proxyDetails = MiltonResourceFactory.getProxyDetails(new DefaultStringOption("", "user%40server.com:pass%3Aword@proxy:3128"));
      assertFalse(proxyDetails.isUseSystemProxy());
      assertEquals("proxy", proxyDetails.getProxyHost());
      assertEquals(3128, proxyDetails.getProxyPort());
      assertEquals("user@server.com", proxyDetails.getUserName());
      assertEquals("pass:word", proxyDetails.getPassword());
    }

  }

}
