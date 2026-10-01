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
package net.sourceforge.ganttproject.document;

import biz.ganttproject.app.DefaultLocalizer;
import biz.ganttproject.app.DummyLocalizer;
import biz.ganttproject.app.InternationalizationCoreKt;
import biz.ganttproject.app.Localizer;
import biz.ganttproject.app.Translation;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import javafx.beans.property.SimpleObjectProperty;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.xml.sax.SAXParseException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the message which the user is shown when reading a document fails. The message itself
 * lives in the translation bundle, so the tests hand in a localizer which answers each of the five
 * keys with a marker of its own: a test which expects one of those markers says both which key the
 * code asked for and that the text really comes from the localizer. The markers are spelled out
 * here on purpose, as are the keys they belong to: a test which asks the code under test for the
 * expected value would follow along with any change and would never go red.
 */
public class ProxyDocumentReadFailureMessageTest {
  @Test
  public void rejectedAuthenticationIsNotReportedAsABrokenFile() {
    // This is the chain which a WebDAV server answering 401 produces: the milton client raises
    // NotAuthorizedException, MiltonResourceImpl wraps it into a WebDavException and
    // assertExists() wraps that into a WebDavRuntimeException.
    Throwable failure = new WebDavRuntimeException(
        "Resource /project.gan does not exist on dav.example.com",
        new WebDavException(
            "User natalie is not authorized to access dav.example.com",
            new NotAuthorizedException("Unauthorized", null)));

    assertEquals("[authenticationRejected]", ProxyDocument.getReadFailureMessage(failure, MARKERS));
  }

  @Test
  public void aRejectedAuthenticationIsRecognisedAtTheTopOfTheChainAsWell() {
    assertEquals("[authenticationRejected]",
        ProxyDocument.getReadFailureMessage(new NotAuthorizedException("Unauthorized", null), MARKERS));
  }

  @Test
  public void anUnknownHostIsReportedAsAnUnreachableServer() {
    Throwable failure = new WebDavRuntimeException(
        "Resource /project.gan does not exist on dav.example.com",
        new WebDavException(
            "I/O problems when accessing dav.example.com",
            new UnknownHostException("dav.example.com")));

    assertEquals("[serverUnreachable]", ProxyDocument.getReadFailureMessage(failure, MARKERS));
  }

  @Test
  public void aRefusedConnectionIsReportedAsAnUnreachableServer() {
    Throwable failure = new DocumentException(
        "Unable to open the file: Connection refused",
        new IOException(new ConnectException("Connection refused")));

    assertEquals("[serverUnreachable]", ProxyDocument.getReadFailureMessage(failure, MARKERS));
  }

  @Test
  public void aHostWithoutARouteIsReportedAsAnUnreachableServer() {
    Throwable failure = new DocumentException(
        "Unable to open the file: No route to host",
        new IOException(new NoRouteToHostException("No route to host")));

    assertEquals("[serverUnreachable]", ProxyDocument.getReadFailureMessage(failure, MARKERS));
  }

  @Test
  public void aReadTimeoutIsNotReportedAsAnUnreachableServer() {
    // A server which is reachable but does not answer in time sends the user to a different place
    // than one which cannot be reached at all, so it must not share the message with the latter.
    Throwable failure = new WebDavRuntimeException(
        "Resource /project.gan does not exist on dav.example.com",
        new WebDavException(
            "I/O problems when accessing dav.example.com",
            new SocketTimeoutException("Read timed out")));

    assertEquals("[timedOut]", ProxyDocument.getReadFailureMessage(failure, MARKERS));
  }

  @Test
  public void aRejectedCertificateIsReportedAsAConnectionWhichCouldNotBeSecured() {
    // SSLHandshakeException is the subclass which an untrusted certificate produces; the code
    // matches the SSLException base class, so the subclass has to be recognised as well.
    Throwable failure = new WebDavRuntimeException(
        "Resource /project.gan does not exist on dav.example.com",
        new WebDavException(
            "I/O problems when accessing dav.example.com",
            new SSLHandshakeException("PKIX path building failed: unable to find valid certification path")));

    assertEquals("[insecureConnection]", ProxyDocument.getReadFailureMessage(failure, MARKERS));
  }

  @Test
  public void aSecureConnectionFailureIsRecognisedAtTheTopOfTheChainAsWell() {
    assertEquals("[insecureConnection]", ProxyDocument.getReadFailureMessage(
        new SSLException("Unsupported or unrecognized SSL message"), MARKERS));
  }

  @Test
  public void aBrokenFileKeepsTheParseFailureMessage() {
    // The chain which a malformed project file produces: XmlParser turns the SAXException into
    // an IOException and doParse() wraps that into a DocumentException.
    assertEquals("[parseFailure]", ProxyDocument.getReadFailureMessage(
        new DocumentException(
            "Unable to open the file: Content is not allowed in prolog.",
            new IOException("Content is not allowed in prolog.")), MARKERS));
  }

  @Test
  public void aParserFailureOnItsOwnKeepsTheParseFailureMessage() {
    assertEquals("[parseFailure]", ProxyDocument.getReadFailureMessage(
        new SAXParseException("Element type \"tsk\" must be followed by either attribute specifications, \">\" or \"/>\".", null),
        MARKERS));
  }

  @Test
  public void anEmptyOrUnreadableDocumentKeepsTheParseFailureMessage() {
    assertEquals("[parseFailure]",
        ProxyDocument.getReadFailureMessage(new DocumentException("Can't open document"), MARKERS));
  }

  @Test
  public void aFailureWithoutACauseKeepsTheParseFailureMessage() {
    assertEquals("[parseFailure]",
        ProxyDocument.getReadFailureMessage(new RuntimeException("something went wrong"), MARKERS));
  }

  @Test
  public void aMissingResourceKeepsTheParseFailureMessage() {
    // 404 is a transport failure too, but it does not send the user to a different place than
    // the generic message does, so it deliberately keeps the old text.
    Throwable failure = new WebDavRuntimeException(
        "Resource /project.gan does not exist on dav.example.com",
        new WebDavException(
            "Resource /project.gan is not found on dav.example.com",
            new NotFoundException("Not Found")));

    assertEquals("[parseFailure]", ProxyDocument.getReadFailureMessage(failure, MARKERS));
  }

  // The four tests below walk cause chains which contain a loop. Without a guard the walk never
  // reaches its end, so they are given a timeout which does not wait for the method to return: a
  // same-thread timeout is only reported once the test method is over, and a test which never ends
  // would take the whole test run with it.

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  public void aFailureWhichIsItsOwnCauseDoesNotHangTheWalk() {
    assertEquals("[parseFailure]",
        ProxyDocument.getReadFailureMessage(new SelfCausedException("this exception is its own cause"), MARKERS));
  }

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  public void twoExceptionsWhichCauseEachOtherDoNotHangTheWalk() {
    // A loop of two is not caught by comparing an exception with its own cause: neither of these
    // two reports itself, they report each other.
    RuntimeException first = new RuntimeException("first");
    RuntimeException second = new RuntimeException("second", first);
    first.initCause(second);

    assertEquals("[parseFailure]", ProxyDocument.getReadFailureMessage(first, MARKERS));
  }

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  public void aLoopWhichTheChainOnlyEntersFurtherDownDoesNotHangTheWalk() {
    // The exception the walk starts from is not part of the loop here, so a guard which only
    // compares against the first exception of the chain would not find it either.
    RuntimeException inLoop = new RuntimeException("in the loop");
    RuntimeException alsoInLoop = new RuntimeException("also in the loop", inLoop);
    inLoop.initCause(alsoInLoop);

    assertEquals("[parseFailure]",
        ProxyDocument.getReadFailureMessage(new RuntimeException("entry point", inLoop), MARKERS));
  }

  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  public void aTransportFailureInsideALoopIsStillRecognised() {
    // Stopping at the first exception seen twice still looks at every exception of the chain, so a
    // loop must not cost the user the message which names the actual problem.
    ConnectException refused = new ConnectException("Connection refused");
    RuntimeException wrapper = new RuntimeException("wrapper", refused);
    refused.initCause(wrapper);

    assertEquals("[serverUnreachable]", ProxyDocument.getReadFailureMessage(wrapper, MARKERS));
  }

  @Test
  public void theMessageIsLookedUpInTheRootLocalizer() {
    DefaultLocalizer savedRootLocalizer = InternationalizationCoreKt.getRootLocalizer();
    try {
      InternationalizationCoreKt.setRootLocalizer(
          localizerWith(Map.of("document.error.read.serverUnreachable", "[serverUnreachable]")));

      assertEquals("[serverUnreachable]",
          ProxyDocument.getReadFailureMessage(new ConnectException("Connection refused")));
    } finally {
      InternationalizationCoreKt.setRootLocalizer(savedRootLocalizer);
    }
  }

  /**
   * A localizer which answers each of the five keys of this code path with a marker of its own and
   * knows nothing else.
   */
  private static final DefaultLocalizer MARKERS = localizerWith(Map.of(
      "document.error.read.authenticationRejected", "[authenticationRejected]",
      "document.error.read.insecureConnection", "[insecureConnection]",
      "document.error.read.timedOut", "[timedOut]",
      "document.error.read.serverUnreachable", "[serverUnreachable]",
      "document.error.read.parseFailure", "[parseFailure]"));

  /**
   * A localizer which knows exactly the given keys and nothing else.
   */
  private static DefaultLocalizer localizerWith(Map<String, String> key2text) {
    return new DefaultLocalizer("", () -> DummyLocalizer.INSTANCE, null,
        new SimpleObjectProperty<Translation>()) {
      @Override
      public String formatTextOrNull(String key, Object... args) {
        return key2text.get(key);
      }
    };
  }

  /**
   * Not every exception reports a null cause when there is none: a class which overrides
   * getCause() may return itself, and walking such a chain never terminates.
   */
  private static class SelfCausedException extends RuntimeException {
    SelfCausedException(String message) {
      super(message);
    }

    @Override
    public synchronized Throwable getCause() {
      return this;
    }
  }
}
