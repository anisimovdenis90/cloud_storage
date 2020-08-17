package commands;

import java.io.Serializable;

public class MessageCommand implements Serializable {

    private static final long serialVersionUID = 1l;

    private final String message;

    public MessageCommand(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
