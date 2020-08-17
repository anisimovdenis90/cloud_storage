package controllers;

import commands.FilesListCommand;
import commands.GetFilesListCommand;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import services.NetworkClient;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {

    @FXML
    private ListView<String> clientFilesList;

    @FXML
    private ListView<String> serverFilesList;

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
    @Override

    public void initialize(URL location, ResourceBundle resources) {
        prepareClientFilesLists();
        prepareServerFilesLists();
        disableButtons();
    }

    public void prepareServerFilesLists() {
        NetworkClient.getInstance().sendCommandToServer(new GetFilesListCommand());
        FilesListCommand command = (FilesListCommand) NetworkClient.getInstance().readCommandFromServer();
        for (String file : command.getFilesList()) {
            serverFilesList.getItems().add(file);
        }
    }

    public void prepareClientFilesLists() {
        File folder = new File(NetworkClient.getInstance().getClientDirectory());
        for (String file : folder.list()) {
            clientFilesList.getItems().add(file);
        }
    }

    public void refreshClientFilesList() {
        Platform.runLater(() -> {
            clientFilesList.getItems().clear();
            prepareClientFilesLists();
        });
    }

    public void refreshServerFilesList() {
        Platform.runLater(() -> {
            serverFilesList.getItems().clear();
            prepareServerFilesLists();
        });
    }

    public void checkServerFilesList() {
        if (serverFilesList.getSelectionModel().getSelectedItems().isEmpty()) {
            return;
        }
        uploadButton.setDisable(true);
        downloadButton.setDisable(false);
        renameButton.setDisable(false);
        deleteButton.setDisable(false);
    }

    public void checkClientFilesList() {
        if (clientFilesList.getSelectionModel().getSelectedItems().isEmpty()) {
            return;
        }
        downloadButton.setDisable(true);
        uploadButton.setDisable(false);
        renameButton.setDisable(false);
        deleteButton.setDisable(false);
    }

    private void disableButtons() {
        renameButton.setDisable(true);
        deleteButton.setDisable(true);
        downloadButton.setDisable(true);
        uploadButton.setDisable(true);
    }

    public void downloadButtonAction() {
        String fileName = serverFilesList.getSelectionModel().getSelectedItem();
        NetworkClient.getInstance().getFileFromServer(fileName, progressBar, this::refreshClientFilesList);
        checkClientFilesList();
        checkServerFilesList();
    }

    public void uploadButtonAction() {
        String fileName = clientFilesList.getSelectionModel().getSelectedItem();
        NetworkClient.getInstance().sendFileToServer(fileName, progressBar, this::refreshServerFilesList);
        checkClientFilesList();
        checkServerFilesList();
    }

    public void deleteButtonAction() {

    }

    public void renameButtonAction() {
    }
}
