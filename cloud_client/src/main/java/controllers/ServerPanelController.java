package controllers;

import commands.ErrorCommand;
import commands.FilesListCommand;
import commands.GetFilesListCommand;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.NetworkClient;
import util.FileInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ServerPanelController extends PanelController {

    public ServerPanelController(TableView<FileInfo> table,
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
        NetworkClient.getInstance().sendCommandToServer(new GetFilesListCommand(path));
        System.out.println("Запрос на списка файлов отправлен на сервер");
        final Object receivedCommand = NetworkClient.getInstance().readCommandFromServer();
        if (receivedCommand instanceof FilesListCommand) {
            System.out.println("Список файлов с сервера получен");
            final FilesListCommand command = (FilesListCommand) receivedCommand;
            Platform.runLater(() -> {
                pathField.setText(command.getCurrentServerPath());
                table.getItems().clear();
                table.getItems().addAll(setFileIconFromImage(command.getFilesList()));
                table.sort();
            });
        } else if (receivedCommand instanceof ErrorCommand) {
            String message = ((ErrorCommand) receivedCommand).getErrorMessage();
            Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
            alert.showAndWait();
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
        if (getSelectedItem().getType().equals(FileInfo.FileType.DIRECTORY)) {
            final Path path = Paths.get(getCurrentPathStr()).resolve(getSelectedFileNameStr());
            updateList(path);
        }
    }

    public void updateList(List<FileInfo> filesList) {
        Platform.runLater(() -> {
            pathField.setText(rootPath.toString());
            table.getItems().clear();
            table.getItems().addAll(setFileIconFromImage(filesList));
            table.sort();
        });
    }

    private List<FileInfo> setFileIconFromImage(List<FileInfo> list) {
        Image folderIcon = new Image("img/folder.png");
        Image fileIcon = new Image("img/file.png");
        list.forEach(fileInfo -> {
            if (fileInfo.getType().equals(FileInfo.FileType.DIRECTORY)) {
                fileInfo.setFileIcon(new ImageView(folderIcon));
            } else {
                fileInfo.setFileIcon(new ImageView(fileIcon));
            }
        });
        return list;
    }
}
