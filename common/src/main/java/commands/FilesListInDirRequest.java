package commands;

import util.FileInfo;

import java.io.Serializable;
import java.util.List;

public class FilesListInDirRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String serverPath;

    private List<FileInfo> filesList;

    public FilesListInDirRequest(String currentServerPath) {
        this.serverPath = currentServerPath;
    }

    public void setFilesList(List<FileInfo> filesList) {
        this.filesList = filesList;
    }

    public String getServerPath() {
        return serverPath;
    }

    public List<FileInfo> getFilesList() {
        return filesList;
    }
}
