package controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import util.FileInfo;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientPanelController extends PanelController {

    private Desktop desktop;

    public ClientPanelController(TableView<FileInfo> table,
                                 TableColumn<FileInfo, String> typeFileColumn,
                                 TableColumn<FileInfo, String> fileNameColumn,
                                 TableColumn<FileInfo, Long> fileSizeColumn,
                                 TableColumn<FileInfo, String> fileDateColumn,
                                 TextField pathField) {
        super(table, typeFileColumn, fileNameColumn, fileSizeColumn, fileDateColumn, pathField);
    }

    @Override
    public void buttonPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "");
            alert.setTitle("Подтверждение операции");
            alert.setHeaderText("Не удалось обновить список файлов по текущему пути. Желаете перейти на каталог выше?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                updateList(path.getParent());
            }
//            e.printStackTrace();
        }
    }

    @Override
    public void setMouseOnTableAction() {
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (!checkSelectedItemNotNull()) {
                    return;
                }
                Path path = Paths.get(pathField.getText()).resolve(table.getSelectionModel().getSelectedItem().getFileName());
                if (Files.isDirectory(path)) {
                    updateList(path);
                } else {
                    try {
                        if (desktop == null) {
                            desktop = Desktop.getDesktop();
                        }
                        desktop.open(path.toFile());
                    } catch (IOException e) {
                        System.out.println("Ошибка открытия файла " + path.getFileName().toString());
                    }
                }
            }
        });
    }
}
