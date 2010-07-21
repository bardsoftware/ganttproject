/*
 * Informations of the project
 */
package net.sourceforge.ganttproject;

/**
 * @author athomas Class to store the project informations
 */
public class PrjInfos {
    /** The name of the project */
    public String _sProjectName = new String();

    /** A short description of it */
    public String _sDescription = new String();

    /** The name of the organisation */
    public String _sOrganization = new String();

    /** Web link for the project or for the company */
    public String _sWebLink = new String();

    /** Default constructor with no parameters. */
    public PrjInfos() {
        this._sProjectName = "Untitled Gantt Project";
        this._sDescription = "";
        this._sOrganization = "";
        this._sWebLink = "http://";
    }

    /** Constructor. */
    public PrjInfos(String sProjectName, String sDescription,
            String sOrganization, String sWebLink) {
        this._sProjectName = sProjectName;
        this._sDescription = sDescription;
        this._sOrganization = sOrganization;
        this._sWebLink = sWebLink;
    }

    /** @return the name of the project. */
    public String getName() {
        return _sProjectName;
    }

    /** @return the description of the project. */
    public String getDescription() {
        return _sDescription;
    }

    /** @return the organization of the project. */
    public String getOrganization() {
        return _sOrganization;
    }

    /** @return the web link for the project or for the company. */
    public String getWebLink() {
        return _sWebLink;
    }
}
