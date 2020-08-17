package commands;

import java.io.Serializable;

public class FileRequestCommand implements Serializable {

    private static final long serialVersionUID = 1l;

    private final String fileName;

    public FileRequestCommand(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
