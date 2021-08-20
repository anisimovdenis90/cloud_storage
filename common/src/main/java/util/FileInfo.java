package util;

import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class FileInfo implements Serializable {

    public enum FileType {
        FILE("F"),
        DIRECTORY("D");

        private final String name;

        FileType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long serialVersionUID = 1L;
    private transient ImageView fileIcon;
    private transient Path path;
    private final String lastModified;
    private final FileType type;
    private final String typeName;
    private String fileName;
    private String fileDir;
    private long fileSize;

    public FileInfo(Path path, ImageView fileIcon) {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            this.path = path;
            this.fileIcon = fileIcon;
            this.fileName = path.getFileName().toString();
            this.fileSize = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.fileSize = -1L;
            }
            this.typeName = type.getName();
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3)).format(dtf);
        } catch (IOException e) {
            throw new RuntimeException("Невозможно создать список файлов из папки " + path);
        }
    }

    public FileInfo(Path path) {
        this(path, null);
    }

    public void setFileDir(String fileDir) {
        this.fileDir = fileDir;
    }

    public String getFileDir() {
        return fileDir;
    }

    public ImageView getFileIcon() {
        return fileIcon;
    }

    public void setFileIcon(ImageView fileIcon) {
        this.fileIcon = fileIcon;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTypeName() {
        return typeName;
    }

    public Path getPath() {
        return path;
    }

    public FileType getType() {
        return type;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getLastModified() {
        return lastModified;
    }
}

