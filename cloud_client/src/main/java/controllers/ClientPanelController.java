package controllers;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import util.FileInfo;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javafx.embed.swing.SwingFXUtils.toFXImage;

public class ClientPanelController extends PanelController {

    private static final Map<String, Image> imageCash = new HashMap<>();
    private Desktop desktop;

    private static final Predicate<Path> pathPredicate = p -> {
        try {
            return !Files.isHidden(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    };

    private static Function<Path, FileInfo> fileInfoFunction = path1 -> {
        if (Files.isDirectory(path1)) {
            return new FileInfo(path1, getImageFromCash("directory", path1));
        } else {
            String extension = getFileExtensionFromPath(path1);
            return new FileInfo(path1, getImageFromCash(extension, path1));
        }
    };

    public ClientPanelController(TableView<FileInfo> table,
                                 TableColumn<FileInfo, String> iconFileColumn,
                                 TableColumn<FileInfo, String> fileTypeColumn,
                                 TableColumn<FileInfo, String> fileNameColumn,
                                 TableColumn<FileInfo, Long> fileSizeColumn,
                                 TableColumn<FileInfo, String> fileDateColumn,
                                 TextField pathField) {
        super(table, iconFileColumn, fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn, pathField);
    }

    @Override
    public void updateList(Path path) {
        try {
            final List<FileInfo> fileInfoList = Files.list(path)
                    .filter(pathPredicate)
                    .map(fileInfoFunction)
                    .collect(Collectors.toList());
            Platform.runLater(() -> {
                pathField.setText(path.normalize().toAbsolutePath().toString());
                table.getItems().clear();
                table.getItems().addAll(fileInfoList);
                table.sort();
            });
        } catch (IOException e) {
            if (path.getParent() != null) {
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "");
                alert.setTitle("Подтверждение операции");
                alert.setHeaderText("Не удалось обновить список файлов по текущему пути. Желаете перейти на каталог выше?");
                final Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                    updateList(path.getParent());
                }
            } else {
                final Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно обновить список файлов по текущему пути!");
                alert.showAndWait();
            }
        }
    }

    private static Image getImageFromCash(String key, Path path) {
        Image image = imageCash.get(key);
        if (image == null) {
            image = createImageFromPath(path);
            imageCash.put(key, image);
        }
        return image;
    }

    private static String getFileExtensionFromPath(Path path) {
        final int i = path.getFileName().toString().lastIndexOf(".");
        if (i > 0) {
            String s = path.getFileName().toString().substring(i);
            if (s.equals(".exe")) {
                return path.getFileName().toString();
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

    @Override
    public void setMouseOnTableAction() {
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                doubleClickAction();
            }
        });
    }

    public void doubleClickAction() {
        if (!checkSelectedItemNotNull()) {
            return;
        }
        final Path path = Paths.get(getCurrentPathStr()).resolve(getSelectedFileNameStr());
        if (Files.isDirectory(path)) {
            updateList(path);
        } else {
            try {
                if (desktop == null) {
                    desktop = Desktop.getDesktop();
                }
                desktop.open(path.toFile());
            } catch (IOException e) {
                final String message = "Ошибка открытия файла \"%s\".";
                System.out.printf(message + "%n", path);
                final Alert alert = new Alert(Alert.AlertType.ERROR, String.format(message, path.getFileName()));
                alert.showAndWait();
            }
        }
    }

    public void openFile(Path path) {
        if (desktop == null) {
            desktop = Desktop.getDesktop();
        }
        try {
            if (!Files.isDirectory(path)) {
                desktop.open(path.getParent().toFile());
            } else {
                desktop.open(path.toFile());
            }
        } catch (IOException e) {
            final String message = "Ошибка запуска штатного файлового менеджера.";
            System.out.println(message);
            final Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.showAndWait();
        }
    }
}
