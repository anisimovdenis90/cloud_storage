package commands;

import java.io.Serializable;

public class SignUpCommand implements Serializable {

    private static final long serialVersionUID = 1l;

    private final String login;
    private final String password;

    private boolean isSignUp;
    private String message;

    public SignUpCommand(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public void setSignUp(boolean signUp) {
        isSignUp = signUp;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSignUp() {
        return isSignUp;
    }

    public String getMessage() {
        return message;
    }
}