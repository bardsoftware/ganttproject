/*
Copyright 2026 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.document.webdav;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Regression test for issue #2818 -- concurrent WebDAV changes were silently overwritten.
 *
 * <p>These tests assert the HTTP conversation, not a decision made inside the client, and that is
 * the whole point. The defect was in the <em>input</em>: {@code File.getEtag()} always returned
 * null because Milton's default PROPFIND never asks for {@code getetag}, so every write went out
 * unconditionally. A test that feeds an ETag to the decision logic and checks the verdict passes
 * against the broken code as well and proves nothing about this issue.
 *
 * <p>The stub server therefore returns {@code getetag} only when the request actually asked for
 * it, exactly as a real server does. Anything else would hand the client a value it never
 * requested and hide the very bug under test.
 */
public class MiltonResourceEtagTest extends TestCase {
  private static final String FILE_NAME = "haus.gan";
  private static final byte[] FILE_CONTENT = "<project/>".getBytes(StandardCharsets.UTF_8);
  /** Recorded in place of a missing If-Match header, so that "absent" is visible in a diff. */
  private static final String NO_IF_MATCH = "<no If-Match header>";

  private HttpServer myServer;
  private MiltonResourceFactory myFactory;
  private final List<String> myPropFindBodies = Collections.synchronizedList(new ArrayList<String>());
  private final List<String> myPutIfMatchHeaders = Collections.synchronizedList(new ArrayList<String>());
  /** The ETag the server currently reports. Changing it stands for somebody else's write. */
  private volatile String myEtag = "\"v1\"";
  private volatile int myRefusedPuts = 0;
  private volatile int myAcceptedPuts = 0;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    myServer.createContext("/", this::handle);
    myServer.start();
    myFactory = new MiltonResourceFactory();
  }

  @Override
  protected void tearDown() throws Exception {
    myServer.stop(0);
    super.tearDown();
  }

  private MiltonResourceImpl createResource() {
    String url = "http://127.0.0.1:" + myServer.getAddress().getPort() + "/" + FILE_NAME;
    return myFactory.createResource(new WebDavUri(url));
  }

  private static void read(MiltonResourceImpl resource) throws Exception {
    try (InputStream is = resource.getInputStream()) {
      ByteArrayOutputStream sink = new ByteArrayOutputStream();
      byte[] buffer = new byte[512];
      for (int count = is.read(buffer); count > 0; count = is.read(buffer)) {
        sink.write(buffer, 0, count);
      }
    }
  }

  private boolean anyPropFindAsksForEtag() {
    synchronized (myPropFindBodies) {
      for (String body : myPropFindBodies) {
        if (body.contains("getetag")) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * THE regression test for #2818: without this request the ETag never arrives and every write is
   * unconditional, no matter how good the logic downstream of it is.
   */
  public void testTheEtagIsRequestedWhenTheFileIsRead() throws Exception {
    read(createResource());
    assertTrue(
        "No PROPFIND asked for DAV:getetag, so the ETag can never arrive and every write stays"
            + " unconditional. Bodies seen: " + myPropFindBodies,
        anyPropFindAsksForEtag());
  }

  /** The remembered ETag has to reach the wire, otherwise the server cannot refuse a stale write. */
  public void testTheWriteCarriesTheEtagSeenWhenReading() throws Exception {
    MiltonResourceImpl resource = createResource();
    read(resource);
    resource.write(FILE_CONTENT);
    assertEquals(
        "The PUT went out without the ETag the read had seen, so the server had nothing to check"
            + " it against.",
        Collections.singletonList("\"v1\""),
        myPutIfMatchHeaders);
  }

  /**
   * The symptom from the issue report: somebody else changes the file between read and save. The
   * save must not go through.
   */
  public void testAForeignChangeBetweenReadAndWriteStopsTheWrite() throws Exception {
    MiltonResourceImpl resource = createResource();
    read(resource);
    myEtag = "\"v2\"";
    try {
      resource.write(FILE_CONTENT);
      fail("The write went through although the file had been changed on the server in the"
          + " meantime. Accepted PUTs: " + myAcceptedPuts + ", If-Match headers: "
          + myPutIfMatchHeaders);
    } catch (WebDavResource.WebDavException e) {
      // The type is checked by name rather than with instanceof on purpose: it keeps this whole
      // test class compilable against the unfixed code, so that reverting the fix produces
      // honest test failures instead of a compile error.
      assertEquals("A 412 was reported as a transport error, so the caller cannot tell a genuine"
              + " conflict from a broken connection.",
          "WebDavConflictException", e.getClass().getSimpleName());
    }
    assertEquals("The server did not refuse anything, so the PUT cannot have been conditional.",
        1, myRefusedPuts);
    assertEquals("Somebody else's version was overwritten -- this is the defect from #2818.",
        0, myAcceptedPuts);
  }

  // ---------------------------------------------------------------- the stub server

  private void handle(HttpExchange exchange) throws IOException {
    try {
      String method = exchange.getRequestMethod();
      String path = exchange.getRequestURI().getPath();
      String body = new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
      if ("PROPFIND".equals(method)) {
        myPropFindBodies.add(body);
        String depth = exchange.getRequestHeaders().getFirst("Depth");
        // A real server answers with the properties that were asked for, and with those only.
        boolean withEtag = body.contains("getetag") || body.contains("allprop");
        respondMultistatus(exchange, path, !"0".equals(depth), withEtag);
      } else if ("GET".equals(method)) {
        exchange.getResponseHeaders().add("ETag", myEtag);
        respond(exchange, 200, FILE_CONTENT);
      } else if ("PUT".equals(method)) {
        String ifMatch = exchange.getRequestHeaders().getFirst("If-Match");
        myPutIfMatchHeaders.add(ifMatch == null ? NO_IF_MATCH : ifMatch);
        if (ifMatch != null && !ifMatch.equals(myEtag)) {
          myRefusedPuts++;
          respond(exchange, 412, new byte[0]);
        } else {
          myAcceptedPuts++;
          myEtag = "\"v-after-put\"";
          respond(exchange, 204, new byte[0]);
        }
      } else if ("OPTIONS".equals(method) || "HEAD".equals(method)) {
        exchange.getResponseHeaders().add("DAV", "1,2");
        exchange.getResponseHeaders().add("Allow", "OPTIONS, GET, HEAD, PUT, PROPFIND");
        respond(exchange, 200, new byte[0]);
      } else {
        respond(exchange, 405, new byte[0]);
      }
    } finally {
      exchange.close();
    }
  }

  private void respondMultistatus(HttpExchange exchange, String path, boolean deep, boolean withEtag)
      throws IOException {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    xml.append("<D:multistatus xmlns:D=\"DAV:\">");
    if ("/".equals(path)) {
      appendCollection(xml);
      if (deep) {
        appendFile(xml, withEtag);
      }
    } else {
      appendFile(xml, withEtag);
    }
    xml.append("</D:multistatus>");
    exchange.getResponseHeaders().add("Content-Type", "text/xml; charset=\"utf-8\"");
    respond(exchange, 207, xml.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void appendCollection(StringBuilder xml) {
    xml.append("<D:response><D:href>/</D:href><D:propstat><D:prop>");
    xml.append("<D:displayname>root</D:displayname>");
    xml.append("<D:getlastmodified>Wed, 02 Sep 2026 10:00:00 GMT</D:getlastmodified>");
    xml.append("<D:resourcetype><D:collection/></D:resourcetype>");
    xml.append("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response>");
  }

  private void appendFile(StringBuilder xml, boolean withEtag) {
    xml.append("<D:response><D:href>/").append(FILE_NAME).append("</D:href><D:propstat><D:prop>");
    xml.append("<D:displayname>").append(FILE_NAME).append("</D:displayname>");
    xml.append("<D:getcontentlength>").append(FILE_CONTENT.length).append("</D:getcontentlength>");
    xml.append("<D:getcontenttype>application/xml</D:getcontenttype>");
    xml.append("<D:getlastmodified>Wed, 02 Sep 2026 10:00:00 GMT</D:getlastmodified>");
    xml.append("<D:resourcetype/>");
    if (withEtag) {
      xml.append("<D:getetag>").append(myEtag).append("</D:getetag>");
    }
    xml.append("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response>");
  }

  private static void respond(HttpExchange exchange, int code, byte[] content) throws IOException {
    if (content.length == 0) {
      exchange.sendResponseHeaders(code, -1);
    } else {
      exchange.sendResponseHeaders(code, content.length);
      exchange.getResponseBody().write(content);
    }
  }

  private static byte[] readAll(InputStream is) throws IOException {
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    byte[] buffer = new byte[512];
    for (int count = is.read(buffer); count > 0; count = is.read(buffer)) {
      sink.write(buffer, 0, count);
    }
    return sink.toByteArray();
  }
}
