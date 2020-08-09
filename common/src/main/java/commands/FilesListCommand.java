package commands;

import java.io.Serializable;
import java.util.List;

public class FilesListCommand implements Serializable {

    private static final long serialVersionUID = 1l;

    private final List<String> filesList;

    public FilesListCommand(List<String> filesList) {
        this.filesList = filesList;
    }

    public List<String> getFilesList() {
        return filesList;
    }
}
