package net.sourceforge.ganttproject.roles;

/**
 * Created by IntelliJ IDEA.
 *
 * @author bard Date: 25.01.2004
 */
public interface Role {
    int getID();

    String getName();

    void setName(String name);

    String getPersistentID();
}
