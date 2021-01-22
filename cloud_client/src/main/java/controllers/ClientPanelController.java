package controllers;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import util.FileInfo;
import util.FileInfoImageViewSetter;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ClientPanelController extends PanelController {

    private final ExecutorService fileIconExecutor;
    private Desktop desktop;

    private static final Predicate<Path> pathPredicate = p -> {
        try {
            return !Files.isHidden(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    };

    public ClientPanelController(TableView<FileInfo> table,
                                 TableColumn<FileInfo, String> iconFileColumn,
                                 TableColumn<FileInfo, String> fileTypeColumn,
                                 TableColumn<FileInfo, String> fileNameColumn,
                                 TableColumn<FileInfo, Long> fileSizeColumn,
                                 TableColumn<FileInfo, String> fileDateColumn,
                                 TextField pathField) {
        super(table, iconFileColumn, fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn, pathField);
        fileIconExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void updateList(Path path) {
        try {
            final List<FileInfo> fileInfoList = Files.list(path)
                    .filter(pathPredicate)
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
            Platform.runLater(() -> {
                pathField.setText(path.normalize().toAbsolutePath().toString());
                table.getItems().clear();
                table.getItems().addAll(fileInfoList);
                table.sort();
                table.scrollTo(0);
            });
            fileIconExecutor.execute(() -> FileInfoImageViewSetter.setImageViewFromFile(fileInfoList, table::refresh));
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
