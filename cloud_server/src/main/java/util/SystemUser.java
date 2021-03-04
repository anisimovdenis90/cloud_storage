package util;

public class SystemUser {

    private String id;
    private String hashedPassword;

    public SystemUser(String id, String hashedPassword) {
        this.id = id;
        this.hashedPassword = hashedPassword;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }
}
