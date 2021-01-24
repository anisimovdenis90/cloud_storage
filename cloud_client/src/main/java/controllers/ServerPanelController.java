package controllers;

import commands.ErrorCommand;
import commands.FilesListCommand;
import commands.GetFilesListCommand;
import javafx.application.Platform;
import javafx.scene.control.*;
import services.NetworkClient;
import util.FileInfo;
import util.FileInfoImageViewSetter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerPanelController extends PanelController {

    private final ExecutorService fileIconExecutor;

    public ServerPanelController(TableView<FileInfo> table,
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
        NetworkClient.getInstance().sendCommandToServer(new GetFilesListCommand(path));
        System.out.println("Запрос на списка файлов отправлен на сервер");
        final Object receivedCommand = NetworkClient.getInstance().readCommandFromServer();
        if (receivedCommand instanceof FilesListCommand) {
            System.out.println("Список файлов с сервера получен");
            final FilesListCommand command = (FilesListCommand) receivedCommand;
            final List<FileInfo> list = command.getFilesList();
            Platform.runLater(() -> {
                pathField.setText(command.getCurrentServerPath());
                table.getItems().clear();
                table.getItems().addAll(list);
                table.sort();
                table.scrollTo(0);
            });
            fileIconExecutor.execute(() -> FileInfoImageViewSetter.setSimpleImageView(list, () -> Platform.runLater(table::refresh)));
        } else if (receivedCommand instanceof ErrorCommand) {
            final String message = ((ErrorCommand) receivedCommand).getErrorMessage();
            final Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
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
            table.getItems().addAll(filesList);
            table.sort();
            table.scrollTo(0);
        });
        fileIconExecutor.execute(() -> FileInfoImageViewSetter.setSimpleImageView(filesList, () -> Platform.runLater(table::refresh)));
    }
}
