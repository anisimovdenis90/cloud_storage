package commands;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessageCommand implements Serializable {

    private static final long serialVersionUID = 1l;

    private final String fileName;
    private final long fileSize;
    private final int partsOfFile;

    private int partNumber;
    private byte[] data;


    public FileMessageCommand(String fileName, long fileSize, int partsOfFile, int partNumber, byte[] data) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.partsOfFile = partsOfFile;
        this.partNumber = partNumber;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public byte[] getData() {
        return data;
    }

    public int getPartsOfFile() {
        return partsOfFile;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }
}
