package net.sourceforge.ganttproject.roles;

public class RolePersistentID {
    private static final String ROLESET_DELIMITER = ":";

    private final String myRoleSetID;

    private final int myRoleID;

    public RolePersistentID(String persistentID) {
        int posDelimiter = persistentID.lastIndexOf(ROLESET_DELIMITER);
        String rolesetName = posDelimiter == -1 ? null : persistentID
                .substring(0, posDelimiter);
        String roleIDasString = posDelimiter == -1 ? persistentID
                : persistentID.substring(posDelimiter + 1);
        int roleID;
        try {
            roleID = Integer.parseInt(roleIDasString);
        } catch (NumberFormatException e) {
            roleID = 0;
        }
        myRoleID = roleID;
        myRoleSetID = rolesetName;

    }

    public String getRoleSetID() {
        return myRoleSetID;
    }

    public int getRoleID() {
        return myRoleID;
    }

    public String asString() {
        return myRoleSetID + ROLESET_DELIMITER + myRoleID;
    }

}
