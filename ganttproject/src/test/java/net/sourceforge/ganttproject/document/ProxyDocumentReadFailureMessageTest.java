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

import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
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
