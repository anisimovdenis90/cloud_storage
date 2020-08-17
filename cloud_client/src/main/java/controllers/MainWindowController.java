package controllers;

import commands.ErrorCommand;
import commands.FilesListCommand;
import commands.GetFilesListCommand;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;
import services.NetworkClient;
import util.FileInfo;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {

    private ClientPanelController clientTable;
    private ServerPanelController serverTable;

    @FXML
    private ComboBox<String> logicalDisksBox;

    @FXML
    private TextField clientPathField;

    @FXML
    private Button pathUpButton;

    @FXML
    private TextField serverPathField;

    @FXML
    private TableView<FileInfo> clientTableView;

    @FXML
    private TableColumn<FileInfo, String> iconColumnClient;

    @FXML
    private TableColumn<FileInfo, String> nameColumnClient;

    @FXML
    private TableColumn<FileInfo, Long> sizeColumnClient;

    @FXML
    private TableColumn<FileInfo, String> dateColumnClient;


    @FXML
    private TableView<FileInfo> serverTableView;

    @FXML
    private TableColumn<FileInfo, String> iconColumnServer;

    @FXML
    private TableColumn<FileInfo, String> nameColumnServer;

    @FXML
    private TableColumn<FileInfo, Long> sizeColumnServer;

    @FXML
    private TableColumn<FileInfo, String> dateColumnServer;

    @FXML
    private Button pathUpButtonServer;

    @FXML
    private Button downloadButton;

    @FXML
    private Button uploadButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button renameButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressBarLabel;

    @FXML
    private Label mainWindowLabel;

    private String rootServerDir;
    private String currentServerDir;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clientTable = new ClientPanelController(
                clientTableView,
                iconColumnClient,
                nameColumnClient,
                sizeColumnClient,
                dateColumnClient,
                clientPathField
        );
        serverTable = new ServerPanelController(
                serverTableView,
                iconColumnServer,
                nameColumnServer,
                sizeColumnServer,
                dateColumnServer,
                serverPathField
        );
        clientTable.setRootPath(".");
        clientTable.updateList();
        prepareLogicalDisksBox();
        prepareServerTableContent();
    }

    private void prepareServerTableContent() {
        NetworkClient.getInstance().sendCommandToServer(new GetFilesListCommand(null));
        FilesListCommand command = (FilesListCommand) NetworkClient.getInstance().readCommandFromServer();
        serverTable.setRootPath(command.getRootServerPath());
        serverTable.updateList(command.getFilesList());
    }

    public void buttonPathUpAction(ActionEvent actionEvent) {
        clientTable.buttonPathUpAction(actionEvent);
    }

    public void buttonPathUpServerAction(ActionEvent actionEvent) {
        serverTable.buttonPathUpAction(actionEvent);
    }

    private void prepareLogicalDisksBox() {
        logicalDisksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            logicalDisksBox.getItems().add(p.toString());
        }
        logicalDisksBox.getSelectionModel().select(0);
    }

    @FXML
    private void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        clientTable.updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    private void disableButtons() {
        renameButton.setDisable(true);
        deleteButton.setDisable(true);
        downloadButton.setDisable(true);
        uploadButton.setDisable(true);
    }

    private void enableButtons() {
        renameButton.setDisable(false);
        deleteButton.setDisable(false);
        downloadButton.setDisable(false);
        uploadButton.setDisable(false);
    }

    public void downloadButtonAction() {
        FileInfo fileInfo = serverTable.getSelectedItem();
        if (fileInfo == null || fileInfo.getType() == FileInfo.FileType.DIRECTORY) {
            String message = "Выберите файл, для скачивания с сервера";
            showInfoAlert(message, Alert.AlertType.WARNING, true);
            return;
        }
        String currentServerDir = serverTable.getCurrentPath();
        Path sourcePath = Paths.get(currentServerDir, fileInfo.getFileName());
        Path destPath = Paths.get(clientTable.getCurrentPath());
        NetworkClient.getInstance().getFileFromServer(sourcePath, destPath, progressBar, this::refreshClientFilesList);
        disableButtons();
    }

    public void uploadButtonAction() {
        String fileName = clientTable.getSelectedFileName();
        if (fileName == null) {
            String message = "Выберите файл, для отправки на сервер";
            showInfoAlert(message, Alert.AlertType.WARNING, true);
            return;
        }
        String currentDir = clientTable.getCurrentPath();
        Path sourcePath = Paths.get(currentDir, fileName);
        if (Files.isDirectory(sourcePath)) {
            String message = "Выберите файл, для отправки на сервер";
            showInfoAlert(message, Alert.AlertType.WARNING, true);
            return;
        }
        Path destPath = Paths.get(serverTable.getCurrentPath());
        NetworkClient.getInstance().sendFileToServer(sourcePath, destPath, progressBar, this::refreshServerFilesList);
        disableButtons();
    }

    public void refreshServerFilesList(String fileName, String destDir) {
        Platform.runLater(() -> {
            enableButtons();
            if (destDir.equals(serverTable.getCurrentPath())) {
                serverTable.updateList(Paths.get(destDir));
            }
            String message = "Файл " + fileName + " успешно загружен на сервер в папку: " + destDir;
            showInfoAlert(message, Alert.AlertType.INFORMATION, false);
            PauseTransition ps = new PauseTransition();
            ps.setDuration(Duration.millis(500));
            ps.setOnFinished(event -> progressBar.setProgress(0.0));
            ps.play();
        });
    }

    public void refreshClientFilesList(String fileName, String destDir) {
        Platform.runLater(() -> {
            enableButtons();
            if (destDir.equals(clientTable.getCurrentPath())) {
                clientTable.updateList(Paths.get(destDir));
            }
            String message = "Файл " + fileName + " успешно скачен с сервера в папку: " + destDir;
            showInfoAlert(message, Alert.AlertType.INFORMATION, false);
            PauseTransition ps = new PauseTransition();
            ps.setDuration(Duration.millis(500));
            ps.setOnFinished(event -> progressBar.setProgress(0.0));
            ps.play();
        });
    }

    public void deleteButtonAction() {
        Path deletePath;
        if (clientTable.getSelectedFileName() == null && serverTable.getSelectedItem() == null) {
            showInfoAlert("Выберите файл для удаления", Alert.AlertType.WARNING, false);
            return;
        }
        if (clientTable.getSelectedFileName() != null) {
            deletePath = Paths.get(clientTable.getCurrentPath(), clientTable.getSelectedFileName());
            if (checkIsDirectory(deletePath)) {
                showInfoAlert("Выберите файл для удаления", Alert.AlertType.WARNING, false);
                return;
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Вы уверены, что хотите удалить файл " + deletePath.getFileName());
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                try {
                    Files.delete(deletePath);
                    clientTable.updateList(deletePath.getParent());
                } catch (IOException e) {
                    String message = "Ошибка удаления! Файл " + deletePath.getFileName() + " занят другим процессом";
                    System.out.println(message);
                    showInfoAlert(message, Alert.AlertType.WARNING, true);
                }
            }
        } else if (serverTable.getSelectedItem() != null) {
            deletePath = Paths.get(serverTable.getCurrentPath(), serverTable.getSelectedItem().getFileName());
            if (checkIsDirectory(deletePath)) {
                showInfoAlert("Выберите файл для удаления", Alert.AlertType.WARNING, false);
                return;
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Вы уверены, что хотите удалить файл " + deletePath.getFileName() + " с сервера");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                NetworkClient.getInstance().deleteFileFromServer(deletePath);
                if (NetworkClient.getInstance().readCommandFromServer() instanceof ErrorCommand) {
                    String message = "Ошибка удаления! Файл " + deletePath.getFileName() + " занят другим процессом";
                    System.out.println(message);
                    showInfoAlert(message, Alert.AlertType.WARNING, true);
                } else {
                    serverTable.updateList(deletePath.getParent());
                }
            }
        }
    }

    public void renameButtonAction() {

    }

    private void showInfoAlert(String message, Alert.AlertType type, boolean needWait) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        if (needWait) {
            alert.showAndWait();
        } else {
            alert.show();
        }
    }

    private boolean checkIsDirectory(Path path) {
        return Files.isDirectory(path);
    }

    private boolean checkClientPanel() {
        return clientTable.getSelectedFileName() == null;
    }

    private boolean checkServerPanel() {
        return serverTable.getSelectedFileName() == null;
    }
}
