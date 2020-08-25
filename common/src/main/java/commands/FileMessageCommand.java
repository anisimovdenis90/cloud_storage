package commands;

import java.io.Serializable;

public class FileMessageCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final String destPath;
    private final long fileSize;
    private final long partsOfFile;

    private long partNumber;
    private byte[] data;


    public FileMessageCommand(String fileName, String destPath, long fileSize, long partsOfFile, long partNumber, byte[] data) {
        this.fileName = fileName;
        this.destPath = destPath;
        this.fileSize = fileSize;
        this.partsOfFile = partsOfFile;
        this.partNumber = partNumber;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDestPath() {
        return destPath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getPartsOfFile() {
        return partsOfFile;
    }

    public long getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(long partNumber) {
        this.partNumber = partNumber;
    }
}
