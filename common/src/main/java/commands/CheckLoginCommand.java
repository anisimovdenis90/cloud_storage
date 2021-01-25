package commands;

import java.io.Serializable;

public class CheckLoginCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String login;

    private boolean free;
    private String message;

    public CheckLoginCommand(String login) {
        this.login = login;
        this.free = false;
    }

    public String getLogin() {
        return login;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
