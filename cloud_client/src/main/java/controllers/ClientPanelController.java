package controllers;

import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import util.FileInfo;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientPanelController extends PanelController {

    private Desktop desktop;

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
            pathField.setText(path.normalize().toAbsolutePath().toString());
            table.getItems().clear();
            table.getItems().addAll(Files.list(path)
                    .filter(p -> {
                        try {
                            return !Files.isHidden(p);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    })
                    .map((Path path1) -> new FileInfo(path1, true))
                    .collect(Collectors.toList())
            );
            table.sort();
        } catch (IOException e) {
            if (path.getParent() != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "");
                alert.setTitle("Подтверждение операции");
                alert.setHeaderText("Не удалось обновить список файлов по текущему пути. Желаете перейти на каталог выше?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                    updateList(path.getParent());
                }
            } else {
                final Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно обновить список файлов по текущему пути!");
                alert.showAndWait();
            }
//            e.printStackTrace();
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
