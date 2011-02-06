/*
 * Created on 18.08.2003
 *
 */
package net.sourceforge.ganttproject.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.webdav.lib.WebdavResource;

/**
 * This class implements an OutputStream for documents on
 * WebDAV-enabled-servers. It is a helper class for HttpDocument.
 * 
 * @see HttpDocument
 * @author Michael Haeusler (michael at akatose.de)
 */
class HttpDocumentOutputStream extends ByteArrayOutputStream {

    private WebdavResource webdavResource;

    public HttpDocumentOutputStream(WebdavResource webdavResource) {
        super();
        this.webdavResource = webdavResource;
    }

    public void close() throws IOException {
        try {
            super.close();
        } finally {
            try {
                webdavResource.putMethod(toByteArray());
            } catch (HttpException e) {
                throw new IOException(e.getMessage() + "(" + e.getReasonCode()
                        + ")");
            }
        }
    }

}
