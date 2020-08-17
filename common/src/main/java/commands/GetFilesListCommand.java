package commands;

import java.io.Serializable;

public class GetFilesListCommand implements Serializable {

    private static final long serialVersionUID = 1l;

    private String path;

    public GetFilesListCommand() {
        this.path = null;
    }

    public GetFilesListCommand(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
