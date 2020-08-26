package commands;

import java.io.Serializable;

public class CreateFolderCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String folderName;

    public CreateFolderCommand(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}
