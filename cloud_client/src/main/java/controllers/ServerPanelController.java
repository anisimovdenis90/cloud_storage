package controllers;

import commands.ErrorCommand;
import commands.FilesListCommand;
import commands.GetFilesListCommand;
import javafx.application.Platform;
import javafx.event.ActionEvent;
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
        NetworkClient.getInstance().sendCommandToServer(new GetFilesListCommand(path));
        System.out.println("Запрос на список файлов отправлен на сервер");
        final Object receivedCommand = NetworkClient.getInstance().readCommandFromServer();
        if (receivedCommand instanceof FilesListCommand) {
            System.out.println("Список файлов получен");
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

    @Override
    public void setMouseOnTableAction() {
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (!checkSelectedItemNotNull()) {
                    return;
                }
                if (table.getSelectionModel().getSelectedItem().getType() == FileInfo.FileType.DIRECTORY) {
                    Path path = Paths.get(pathField.getText()).resolve(table.getSelectionModel().getSelectedItem().getFileName());
                    updateList(path);
                }
            }
        });
    }
}
