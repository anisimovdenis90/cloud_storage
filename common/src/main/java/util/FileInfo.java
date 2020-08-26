package util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static javafx.embed.swing.SwingFXUtils.toFXImage;

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
    private final FileType type;
    private final String typeName;
    private String fileName;
    private long fileSize;
    private final String lastModified;
    private String fileDir;

    public FileInfo(Path path, boolean needIcon) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            if (needIcon) {
                this.fileIcon = getIconImageFX(path);
            } else {
                fileIcon = null;
            }
            this.fileName = path.getFileName().toString();
            this.fileSize = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.fileSize = -1L;
            }
            this.typeName = type.getName();
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3)).format(dtf);
        } catch (IOException e) {
            throw new RuntimeException("Невозможно создать список файлов из папки " + path.toString());
        }
    }

    private ImageView getIconImageFX(Path path) {
        final ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(path.toFile());
        java.awt.Image img = icon.getImage();
        final BufferedImage bimg = (BufferedImage) img;
        final Image imgfx = toFXImage(bimg,null);
        return new ImageView(imgfx);
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

