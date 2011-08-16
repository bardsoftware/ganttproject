/*
 * Created on 12.03.2004
 *
 */
package net.sourceforge.ganttproject.document;

/**
 * All types of documents, that wish to be handled by the URLChooser-GUI, have
 * to extend this abstract class.
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
public abstract class AbstractURLDocument extends AbstractDocument {

    /**
     * "URLDocuments" don't return null on calls to getURLPath().
     */
    public String getURLPath() {
        return getPath();
    }

}
