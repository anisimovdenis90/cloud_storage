package util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.ehcache.Cache;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static javafx.embed.swing.SwingFXUtils.toFXImage;

public class FileInfoImageViewSetter {

    private static final Cache<String, Image> imageCash = CacheHelper.createCache("imageCache", String.class, Image.class, 100);
    private static final String KEY_FOR_DIR = "directory";
    private static final Image folderIcon = new Image("img/folder.png");
    private static final Image fileIcon = new Image("img/file.png");

    public static void setImageViewFromFile(List<FileInfo> list, Callback callback) {
        list.forEach(fileInfo -> {
            final Path path = fileInfo.getPath();
            if (fileInfo.getType().equals(FileInfo.FileType.DIRECTORY)) {
                fileInfo.setFileIcon(new ImageView(getImageFromCashOrCreateFromPath(KEY_FOR_DIR, path)));
            } else {
                final String extension = getFileExtensionFromFileName(path.getFileName().toString());
                fileInfo.setFileIcon(new ImageView(getImageFromCashOrCreateFromPath(extension, path)));
            }
            if (callback != null) {
                callback.callback();
            }
        });
    }

    public static void setSimpleImageView(List<FileInfo> list, Callback callback) {
        list.forEach(fileInfo -> {
            if (fileInfo.getType().equals(FileInfo.FileType.DIRECTORY)) {
                Image image = imageCash.get(KEY_FOR_DIR);
                if (image == null) {
                    image = folderIcon;
                }
                fileInfo.setFileIcon(new ImageView(image));
            } else if (fileInfo.getType().equals(FileInfo.FileType.FILE)) {
                final String extension = getFileExtensionFromFileName(fileInfo.getFileName());
                Image image = imageCash.get(extension);
                if (image == null) {
                    image = fileIcon;
                }
                fileInfo.setFileIcon(new ImageView(image));
            }
            if (callback != null) {
                callback.callback();
            }
        });
    }

    private static Image getImageFromCashOrCreateFromPath(String key, Path path) {
        Image image = imageCash.get(key);
        if (image == null) {
            image = createImageFromPath(path);
            imageCash.put(key, image);
        }
        return image;
    }

    private static String getFileExtensionFromFileName(String fileName) {
        final int index = fileName.lastIndexOf(".");
        if (index > 0) {
            final String s = fileName.substring(index);
            if (s.equals(".exe")) {
                return fileName;
            }
            return s;
        }
        return "";
    }

    private static Image createImageFromPath(Path path) {
        final ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(path.toFile());
        final BufferedImage bimg = (BufferedImage) icon.getImage();
        return toFXImage(bimg, null);
    }
}
