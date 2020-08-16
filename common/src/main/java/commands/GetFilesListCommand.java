package commands;

import java.io.Serializable;
import java.nio.file.Path;

public class GetFilesListCommand implements Serializable {

    private static final long serialVersionUID = 1l;

    private String currentPath;

    public GetFilesListCommand(Path currentDir) {
        if (currentDir == null) {
            this.currentPath = null;
        } else {
            this.currentPath = currentDir.toString();
        }
    }

    public String getCurrentPath() {
        return currentPath;
    }
}