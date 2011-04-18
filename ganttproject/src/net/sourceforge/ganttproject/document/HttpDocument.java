/*
 * Created on 18.08.2003
 *
 */
package net.sourceforge.ganttproject.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlException;

import net.sourceforge.ganttproject.GPLogger;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.webdav.lib.WebdavResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This class implements the interface Document for file access on HTTP-servers
 * and WebDAV-enabled-servers.
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
public class HttpDocument extends AbstractURLDocument {

    private String url;

    private String lastError;

    private HttpURL httpURL;

    private WebdavResource webdavResource;

    private boolean locked = false;

    private boolean malformedURL = false;

    private String myUsername;

    private String myPassword;

    private static int lockDAVMinutes = 240;

    public HttpDocument(String url, String user, String pass) {
        this.url = url;
        myUsername = user;
        myPassword = pass;
        try {
            if (url.startsWith("https")) {
                httpURL = new HttpsURL(url);
            } else {
                httpURL = new HttpURL(url);
            }
        } catch (URIException e) {
            lastError = e.getMessage();
            malformedURL = true;
        }
    }

    WebdavResource getWebdavResource() {
        if (null == webdavResource)
            try {
                Credentials credentials = new UsernamePasswordCredentials(myUsername, myPassword);
                webdavResource = new WebdavResource(httpURL, credentials, WebdavResource.NOACTION, 0);
                webdavResource.setFollowRedirects(true);
            } catch (HttpException e) {
                lastError = e.getMessage() + "(" + e.getReasonCode() + ")";
            } catch (IOException e) {
                lastError = e.getMessage();
            }
        return webdavResource;
    }

    public String getDescription() {
        String description = httpURL.toString();
        return (description != null) ? description : url;
    }

    public boolean canRead() {
        WebdavResource res = getWebdavResource();
        return (null == res) ? false : (res.exists() && !res.isCollection());
    }

    public IStatus canWrite() {
        WebdavResource res = getWebdavResource();
        if (null == res) {
            return new Status(IStatus.ERROR, Document.PLUGIN_ID,
                Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(), lastError, null);
        }

        try {
            res.setProperties(0);
        } catch (HttpException e) {
            if (404 != e.getReasonCode()) {
                return new Status(IStatus.ERROR, Document.PLUGIN_ID,
                    Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(), e.getMessage(), e);
            }
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Document.PLUGIN_ID,
                Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(), e.getMessage(), e);
        }

        if (res.exists()) {
            return (res.isCollection())
                ? new Status(
                    IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.IS_DIRECTORY.ordinal(), res.getPath(),  null)
                : Status.OK_STATUS;
        } else {
            try {
                HttpURL parentURL = httpURL.toString().startsWith("https:") ? new HttpsURL(httpURL.toString()) : new HttpURL(httpURL.toString());
                String user = httpURL.getUser();
                String pass = httpURL.getPassword();
                if (user != null)
                    parentURL.setUserinfo(user, pass);
                String currentHierPath = httpURL.getCurrentHierPath();
                if (!currentHierPath.endsWith("/"))
                    currentHierPath = currentHierPath + "/";
                parentURL.setPath(currentHierPath);
                WebdavResource parentRes = new WebdavResource(parentURL);
                if (!parentRes.isCollection()) {
                    return new Status(
                        IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_DIRECTORY.ordinal(),
                        parentRes.getPath(),  null);
                }
                return Status.OK_STATUS;
            } catch (Exception e) {
                return new Status(IStatus.ERROR, Document.PLUGIN_ID,
                    Document.ErrorCode.GENERIC_NETWORK_ERROR.ordinal(), e.getMessage(), e);
            }
        }
    }

    public boolean isValidForMRU() {
        return (!malformedURL);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.ganttproject.document.Document#acquireLock(java.lang.String)
     */
    public boolean acquireLock() {
        if (locked)
            return true;
        if (null == getWebdavResource())
            return false;
        try {
            String userName = " (GanttProject)";
            try {
                userName = " (" + System.getProperty("user.name")
                        + "@GanttProject)";
            } catch (AccessControlException e) {
                if (!GPLogger.log(e)) {
                    e.printStackTrace(System.err);
                }
            }
            if (lockDAVMinutes < 0) {
                return true;
            }
            locked = getWebdavResource().lockMethod(httpURL.getUser() + userName, lockDAVMinutes * 60);
            return locked;
        } catch (HttpException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        } catch (IOException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.ganttproject.document.Document#releaseLock()
     */
    public void releaseLock() {
        if (null == getWebdavResource()) {
            return;
        }
        try {
            locked = false;
            if (!getWebdavResource().isLocked()) {
                return;
            }
            getWebdavResource().unlockMethod();
        } catch (HttpException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        } catch (IOException e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
        }
    }

    public InputStream getInputStream() throws IOException {
        if (null == getWebdavResource())
            throw new IOException(lastError);
        try {
            return getWebdavResource().getMethodData();
        } catch (HttpException e) {
            throw new IOException(e.getMessage() + "(" + e.getReasonCode()
                    + ")");
        } catch (IOException e) {
            throw new IOException("Status code=" + getWebdavResource().getStatusCode(), e);
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (null == getWebdavResource()) {
            throw new IOException(lastError);
        }
        return new HttpDocumentOutputStream(this);
    }

    public String getPath() {
        return getDescription();
    }

    public String getURLPath() {
        return getPath();
    }

    public String getUsername() {
        return myUsername;
    }

    public String getPassword() {
        return myPassword;
    }

    public String getLastError() {
        return lastError;
    }

    public static void setLockDAVMinutes(int i) {
        lockDAVMinutes = i;
    }

    public void write() throws IOException {
        // TODO Auto-generated method stub

    }

    public URI getURI() {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public boolean isLocal() {
        return false;
    }


}
