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
import org.xml.sax.SAXParseException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the message which the user is shown when reading a document fails. The expected
 * strings are spelled out here on purpose: a test which asks the code under test for the
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

    assertEquals("Authentication was rejected by the server", ProxyDocument.getReadFailureMessage(failure));
  }

  @Test
  public void aRejectedAuthenticationIsRecognisedAtTheTopOfTheChainAsWell() {
    assertEquals("Authentication was rejected by the server",
        ProxyDocument.getReadFailureMessage(new NotAuthorizedException("Unauthorized", null)));
  }

  @Test
  public void anUnknownHostIsReportedAsAnUnreachableServer() {
    Throwable failure = new WebDavRuntimeException(
        "Resource /project.gan does not exist on dav.example.com",
        new WebDavException(
            "I/O problems when accessing dav.example.com",
            new UnknownHostException("dav.example.com")));

    assertEquals("The server could not be reached", ProxyDocument.getReadFailureMessage(failure));
  }

  @Test
  public void aRefusedConnectionIsReportedAsAnUnreachableServer() {
    Throwable failure = new DocumentException(
        "Unable to open the file: Connection refused",
        new IOException(new ConnectException("Connection refused")));

    assertEquals("The server could not be reached", ProxyDocument.getReadFailureMessage(failure));
  }

  @Test
  public void aHostWithoutARouteIsReportedAsAnUnreachableServer() {
    Throwable failure = new DocumentException(
        "Unable to open the file: No route to host",
        new IOException(new NoRouteToHostException("No route to host")));

    assertEquals("The server could not be reached", ProxyDocument.getReadFailureMessage(failure));
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

    assertEquals("The server did not answer in time", ProxyDocument.getReadFailureMessage(failure));
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

    assertEquals("The secure connection to the server could not be established",
        ProxyDocument.getReadFailureMessage(failure));
  }

  @Test
  public void aSecureConnectionFailureIsRecognisedAtTheTopOfTheChainAsWell() {
    assertEquals("The secure connection to the server could not be established",
        ProxyDocument.getReadFailureMessage(new SSLException("Unsupported or unrecognized SSL message")));
  }

  @Test
  public void aBrokenFileKeepsTheParseFailureMessage() {
    // The chain which a malformed project file produces: XmlParser turns the SAXException into
    // an IOException and doParse() wraps that into a DocumentException.
    assertEquals("Failed to parse document", ProxyDocument.getReadFailureMessage(
        new DocumentException(
            "Unable to open the file: Content is not allowed in prolog.",
            new IOException("Content is not allowed in prolog."))));
  }

  @Test
  public void aParserFailureOnItsOwnKeepsTheParseFailureMessage() {
    assertEquals("Failed to parse document", ProxyDocument.getReadFailureMessage(
        new SAXParseException("Element type \"tsk\" must be followed by either attribute specifications, \">\" or \"/>\".", null)));
  }

  @Test
  public void anEmptyOrUnreadableDocumentKeepsTheParseFailureMessage() {
    assertEquals("Failed to parse document",
        ProxyDocument.getReadFailureMessage(new DocumentException("Can't open document")));
  }

  @Test
  public void aFailureWithoutACauseKeepsTheParseFailureMessage() {
    assertEquals("Failed to parse document",
        ProxyDocument.getReadFailureMessage(new RuntimeException("something went wrong")));
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

    assertEquals("Failed to parse document", ProxyDocument.getReadFailureMessage(failure));
  }

  @Test
  public void aFailureWhichIsItsOwnCauseDoesNotHangTheLoop() {
    assertEquals("Failed to parse document",
        ProxyDocument.getReadFailureMessage(new SelfCausedException("this exception is its own cause")));
  }

  @Test
  public void everyMessageIsTakenFromTheTranslationWhenTheKeyIsThere() {
    // The keys are spelled out instead of being read from the code under test: a test which asks
    // the code for the key it uses would follow along with a renamed key and would never go red.
    Localizer i18n = localizerWith(Map.of(
        "document.error.read.authenticationRejected", "[authenticationRejected]",
        "document.error.read.insecureConnection", "[insecureConnection]",
        "document.error.read.timedOut", "[timedOut]",
        "document.error.read.serverUnreachable", "[serverUnreachable]",
        "document.error.read.parseFailure", "[parseFailure]"));

    assertEquals("[authenticationRejected]",
        ProxyDocument.getReadFailureMessage(new NotAuthorizedException("Unauthorized", null), i18n));
    assertEquals("[insecureConnection]",
        ProxyDocument.getReadFailureMessage(new SSLException("handshake_failure"), i18n));
    assertEquals("[timedOut]",
        ProxyDocument.getReadFailureMessage(new SocketTimeoutException("Read timed out"), i18n));
    assertEquals("[serverUnreachable]",
        ProxyDocument.getReadFailureMessage(new ConnectException("Connection refused"), i18n));
    assertEquals("[parseFailure]",
        ProxyDocument.getReadFailureMessage(new RuntimeException("something went wrong"), i18n));
  }

  @Test
  public void aMissingKeyLeavesTheEnglishTextRatherThanTheBareKey() {
    // The keys are not in the translation bundle yet. Until they are, the user has to be shown a
    // sentence and not "document.error.read.serverUnreachable" or an empty line.
    Localizer nothingTranslated = localizerWith(Map.of());

    assertEquals("Authentication was rejected by the server",
        ProxyDocument.getReadFailureMessage(new NotAuthorizedException("Unauthorized", null), nothingTranslated));
    assertEquals("The secure connection to the server could not be established",
        ProxyDocument.getReadFailureMessage(new SSLException("handshake_failure"), nothingTranslated));
    assertEquals("The server did not answer in time",
        ProxyDocument.getReadFailureMessage(new SocketTimeoutException("Read timed out"), nothingTranslated));
    assertEquals("The server could not be reached",
        ProxyDocument.getReadFailureMessage(new ConnectException("Connection refused"), nothingTranslated));
    assertEquals("Failed to parse document",
        ProxyDocument.getReadFailureMessage(new RuntimeException("something went wrong"), nothingTranslated));
  }

  @Test
  public void aKeyWithAnEmptyValueLeavesTheEnglishTextAsWell() {
    // The bundle does contain keys with no value at all, and an error dialog with no text in it
    // tells the user less than an untranslated one.
    Localizer emptyValue = localizerWith(Map.of("document.error.read.serverUnreachable", ""));

    assertEquals("The server could not be reached",
        ProxyDocument.getReadFailureMessage(new ConnectException("Connection refused"), emptyValue));
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
