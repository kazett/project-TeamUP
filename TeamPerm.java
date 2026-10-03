package id.teamup.model;

public enum TeamPerm {
    OPEN_MENU("openmenu"),
    MANAGE_MEMBERS("managemembers"),
    VIEW_ONLINE("viewonline"),
    CHAT("chat"),
    RANK("rank"),
    VAULT("vault");

    private final String key;

    TeamPerm(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
