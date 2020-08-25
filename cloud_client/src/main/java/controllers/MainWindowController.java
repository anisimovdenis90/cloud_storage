package controllers;

import commands.ErrorCommand;
import commands.FilesListCommand;
import commands.GetFilesListCommand;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import services.NetworkClient;
import util.FileInfo;
import util.TransferItem;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {

    private ClientPanelController clientTable;
    private ServerPanelController serverTable;

    @FXML
    private Button refreshButtonServer;

    @FXML
    private Button refreshButtonClient;

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
    private Button newFolderButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button renameButton;

    @FXML
    private TableView<TransferItem> operationTable;

    @FXML
    private TableColumn<TransferItem, String> operationColumn;

    @FXML
    private TableColumn<TransferItem, Button> infoColumn;

    @FXML
    private TableColumn<TransferItem, String> fileNameColumn;

    @FXML
    private TableColumn<TransferItem, ProgressIndicator> progressColumn;

    @FXML
    private TableColumn<TransferItem, Button> goToFileColumn;

    @FXML
    private TableColumn<TransferItem, Button> deleteItemColumn;

    private OperationTableController operationTableController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setTooltips();
        setContextMenusToTables();
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
        operationTableController = new OperationTableController(
                operationTable,
                operationColumn,
                infoColumn,
                fileNameColumn,
                progressColumn,
                goToFileColumn,
                deleteItemColumn,
                this,
                clientTable,
                serverTable
        );
        prepareLogicalDisksBox();
        prepareServerTableContent();
    }

    private void setContextMenusToTables() {
        ContextMenu clientContextMenu = new ContextMenu();
        ContextMenu serverContextMenu = new ContextMenu();
        MenuItem downloadItem = new MenuItem("Скачать");
        MenuItem uploadItem = new MenuItem("Отправить в облако");
        MenuItem newCatalogClientItem = new MenuItem("Создать папку");
        MenuItem newCatalogServerItem = new MenuItem("Создать папку");
        MenuItem renameClientItem = new MenuItem("Переименовать");
        MenuItem renameServerItem = new MenuItem("Переименовать");
        MenuItem deleteClientItem = new MenuItem("Удалить");
        MenuItem deleteServerItem = new MenuItem("Удалить");
        MenuItem refreshClientItem = new MenuItem("Обновить");
        MenuItem refreshServerItem = new MenuItem("Обновить");
        MenuItem pathUpClientItem = new MenuItem("Вверх");
        MenuItem pathUpServerItem = new MenuItem("Вверх");

        downloadItem.setOnAction(event -> {
            if (!downloadButton.isDisabled()) {
                downloadButtonAction();
            }
        });
        uploadItem.setOnAction(event -> {
            if (!uploadButton.isDisabled()) {
                uploadButtonAction();
            }
        });
        renameClientItem.setOnAction(event -> {
            if (!renameButton.isDisabled()) {
                renameButtonAction();
            }
        });
        renameServerItem.setOnAction(event -> {
            if (!renameButton.isDisabled()) {
                renameButtonAction();
            }
        });
        deleteClientItem.setOnAction(event -> {
            if (!deleteButton.isDisabled()) {
                deleteButtonAction();
            }
        });
        deleteServerItem.setOnAction(event -> {
            if (!deleteButton.isDisabled()) {
                deleteButtonAction();
            }
        });
        newCatalogClientItem.setOnAction(event -> {
            if (!newFolderButton.isDisabled()) {
                createNewDirAction();
            }
        });
        newCatalogServerItem.setOnAction(event -> {
            if (!newFolderButton.isDisabled()) {
                createNewDirAction();
            }
        });
        refreshClientItem.setOnAction(event -> refreshClientFilesList());
        refreshServerItem.setOnAction(event -> refreshServerFilesList());
        pathUpClientItem.setOnAction(event -> buttonPathUpAction(event));
        pathUpServerItem.setOnAction(event -> buttonPathUpServerAction(event));

        clientContextMenu.getItems().addAll(
                pathUpClientItem,
                refreshClientItem,
                new SeparatorMenuItem(),
                uploadItem,
                new SeparatorMenuItem(),
                newCatalogClientItem,
                renameClientItem,
                deleteClientItem
        );
        serverContextMenu.getItems().addAll(
                pathUpServerItem,
                refreshServerItem,
                new SeparatorMenuItem(),
                downloadItem,
                new SeparatorMenuItem(),
                newCatalogServerItem,
                renameServerItem,
                deleteServerItem
        );

        clientTableView.setContextMenu(clientContextMenu);
        serverTableView.setContextMenu(serverContextMenu);
    }

    private void setTooltips() {
        Tooltip refreshButtonTooltip = new Tooltip("Обновить текущий каталог");
        refreshButtonClient.setTooltip(refreshButtonTooltip);
        refreshButtonServer.setTooltip(refreshButtonTooltip);

        Tooltip pathUpButtonTooltip = new Tooltip("Перейти в предыдущий каталог");
        pathUpButton.setTooltip(pathUpButtonTooltip);
        pathUpButtonServer.setTooltip(pathUpButtonTooltip);

        logicalDisksBox.setTooltip(new Tooltip("Выбрать диск"));
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
        newFolderButton.setDisable(true);
    }

    private void enableButtons() {
        renameButton.setDisable(false);
        deleteButton.setDisable(false);
        downloadButton.setDisable(false);
        uploadButton.setDisable(false);
        newFolderButton.setDisable(false);
    }

    public void downloadButtonAction() {
        FileInfo fileInfo = (FileInfo) serverTable.getSelectedItem();
        if (fileInfo == null || fileInfo.getType() == FileInfo.FileType.DIRECTORY) {
            String message = "Выберите файл, для скачивания с сервера";
            showInfoAlert(message, Alert.AlertType.WARNING, true);
            return;
        }
        Path checkedPath = Paths.get(clientTable.getCurrentPathStr(), fileInfo.getFileName());
        if (Files.exists(checkedPath)) {
            if (showConfirmAlert("Файл " + fileInfo.getFileName() + " уже существует, желаете перезаписать?")) {
                try {
                    Files.delete(checkedPath);
                } catch (IOException e) {
                    showInfoAlert("Невозможно перезаписать файл, " + fileInfo.getFileName() + "нет доступа", Alert.AlertType.WARNING, true);
                    return;
                }
            } else {
                return;
            }
        }
        String currentServerDir = serverTable.getCurrentPathStr();
        Path sourcePath = Paths.get(currentServerDir, fileInfo.getFileName());
        Path destPath = Paths.get(clientTable.getCurrentPathStr());

        TransferItem item = new TransferItem(TransferItem.Operation.DOWNLOAD, sourcePath, destPath);
        operationTableController.updateOperationTable(item);

        NetworkClient.getInstance().getFileFromServer(item, this::refreshClientFilesList);
        disableButtons();
    }

    public void uploadButtonAction() {
        String errorMessage = "Выберите файл, для отправки на сервер";
        String fileName = clientTable.getSelectedFileNameStr();
        if (fileName == null) {
            showInfoAlert(errorMessage, Alert.AlertType.WARNING, true);
            return;
        }
        String currentDir = clientTable.getCurrentPathStr();
        Path sourcePath = Paths.get(currentDir, fileName);
        if (Files.isDirectory(sourcePath)) {
            showInfoAlert(errorMessage, Alert.AlertType.WARNING, true);
            return;
        }
        Path destPath = Paths.get(serverTable.getCurrentPathStr());

        TransferItem item = new TransferItem(TransferItem.Operation.UPLOAD, sourcePath, destPath);
        operationTableController.updateOperationTable(item);

        NetworkClient.getInstance().sendFileToServer(item, this::refreshServerFilesList);
        disableButtons();
    }

    public void refreshServerFilesList(Path destDir) {
        Platform.runLater(() -> {
            enableButtons();
            if (destDir.toString().equals(serverTable.getCurrentPathStr())) {
                serverTable.updateList(destDir);
            }
        });
    }

    public void refreshClientFilesList(Path destDir) {
        Platform.runLater(() -> {
            enableButtons();
            if (destDir.toString().equals(clientTable.getCurrentPathStr())) {
                clientTable.updateList(destDir);
            }
        });
    }

    public Path getSelectedPathFromClientPanel() {
        if (clientTable.getSelectedFileNameStr() == null) {
            return null;
        }
        return Paths.get(clientTable.getCurrentPathStr(), clientTable.getSelectedFileNameStr());
    }

    public Path getSelectedPathFromServerPanel() {
        if (serverTable.getSelectedFileNameStr() == null) {
            return null;
        }
        return Paths.get(serverTable.getCurrentPathStr(), ((FileInfo) serverTable.getSelectedItem()).getFileName());
    }

    public void deleteButtonAction() {
        Path clientPath = getSelectedPathFromClientPanel();
        Path serverPath = getSelectedPathFromServerPanel();
        if (clientPath == null && serverPath == null) {
            showInfoAlert("Выберите файл для удаления", Alert.AlertType.WARNING, false);
            return;
        }
        if (clientPath != null) {
            if (Files.isDirectory(clientPath)) {
                deleteDirectoryWithConfirmation(clientPath);
            } else if (showConfirmAlert("Вы уверены, что хотите удалить файл " + clientPath.getFileName())) {
                try {
                    Files.delete(clientPath);
                    clientTable.updateList(clientPath.getParent());
                } catch (IOException e) {
                    String message = "Ошибка удаления! Файл " + clientPath.getFileName() + " занят другим процессом";
                    System.out.println(message);
                    e.printStackTrace();
                    showInfoAlert(message, Alert.AlertType.WARNING, true);
                }
            }
        } else {
            if (((FileInfo) serverTable.getSelectedItem()).getType().equals(FileInfo.FileType.DIRECTORY)) {
                if (showConfirmAlert("Вы уверены, что хотите удалить папку " + serverPath.getFileName() + " с сервера со всем содержимым?")) {
                    deletePathFromServer(serverPath);
                }
            } else if (showConfirmAlert("Вы уверены, что хотите удалить файл " + serverPath.getFileName() + " с сервера")) {
                deletePathFromServer(serverPath);
            }
        }
    }

    private void deletePathFromServer(Path serverPath) {
        NetworkClient.getInstance().deleteFileFromServer(serverPath);
        if (NetworkClient.getInstance().readCommandFromServer() instanceof ErrorCommand) {
            String message = "Ошибка удаления! Файл " + serverPath.getFileName() + " занят другим процессом";
            System.out.println(message);
            showInfoAlert(message, Alert.AlertType.WARNING, true);
        } else {
            serverTable.updateList(serverPath.getParent());
        }
    }

    private void deleteDirectoryWithConfirmation(Path clientPath) {
        if (showConfirmAlert("Вы уверены, что хотите удалить папку " + clientPath.getFileName() + " со всем содержимым?")) {
            try {
                Files.walkFileTree(clientPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        System.out.println("Удален файл: " + file.toString());
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        System.out.println("Удален каталог: " + dir.toString());
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                clientTable.updateList(clientPath.getParent());
            } catch (IOException e) {
                System.out.println("Ошибка удаления папки " + clientPath);
                e.printStackTrace();
            }
        }
    }

    public void renameButtonAction() {
        Path clientPath = getSelectedPathFromClientPanel();
        Path serverPath = getSelectedPathFromServerPanel();
        if (clientPath == null && serverPath == null) {
            showInfoAlert("Выберите файл для переименования", Alert.AlertType.WARNING, false);
            return;
        }
        if (clientPath != null) {
            String newName = showTextInputDialog(clientPath.getFileName().toString(),
                    "Переименование файла " + clientPath.getFileName(),
                    "Введите новое имя файла: "
            );
            if (newName != null) {
                try {
                    Files.move(clientPath, Paths.get(clientPath.getParent().toString(), newName));
                    clientTable.updateList(clientPath.getParent());
                } catch (IOException e) {
                    String message = "Ошибка переименования файла " + clientPath.getFileName() + ". Файл с таким именем уже существует.";
                    System.out.println(message);
                    showInfoAlert(message, Alert.AlertType.WARNING, true);
                }
            }
        } else {
            String newName = showTextInputDialog(serverPath.getFileName().toString(),
                    "Переименование файла " + serverPath.getFileName() + " на сервере",
                    "Введите новое имя файла: "
            );
            if (newName != null) {
                Path newPath = Paths.get(serverPath.getParent().toString(), newName);
                NetworkClient.getInstance().renameFileOnServer(serverPath.toString(), newPath.toString());
                if (NetworkClient.getInstance().readCommandFromServer() instanceof ErrorCommand) {
                    String message = "Ошибка переименования файла " + serverPath.getFileName() + ". Файл с таким именем уже существует.";
                    System.out.println(message);
                    showInfoAlert(message, Alert.AlertType.WARNING, true);
                } else {
                    serverTable.updateList(serverPath.getParent());
                }
            }
        }
    }

    public void createNewDirAction() {
        if (!clientTableView.isFocused() && !serverTableView.isFocused()) {
            showInfoAlert("Для создания папки выберите окно", Alert.AlertType.INFORMATION, false);
            return;
        }
        if (clientTableView.isFocused()) {
            String newFolderName = showTextInputDialog("Новая папка",
                    "Создать каталог на клиенте ",
                    "Введите название папки: "
            );
            if (newFolderName != null) {
                try {
                    Files.createDirectories(Paths.get(clientTable.getCurrentPathStr(), newFolderName));
                    clientTable.updateList(Paths.get(clientTable.getCurrentPathStr()));
                } catch (IOException e) {
                    String message = "Папка с именем " + newFolderName + " уже существует в текущем расположении";
                    System.out.println(message);
                    showInfoAlert(message, Alert.AlertType.WARNING, true);
                }
            }
        } else {
            String newFolderName = showTextInputDialog("Новая папка",
                    "Создать каталог на сервере ",
                    "Введите название папки: "
            );
            if (newFolderName != null) {
                NetworkClient.getInstance().createNewFolderOnServer(serverTable.getCurrentPathStr(), newFolderName);
                if (NetworkClient.getInstance().readCommandFromServer() instanceof ErrorCommand) {
                    String message = "Папка с именем " + newFolderName + " уже существует в текущем расположении сервера";
                    System.out.println(message);
                    showInfoAlert(message, Alert.AlertType.WARNING, true);
                } else {
                    serverTable.updateList(Paths.get(serverTable.getCurrentPathStr()));
                }
            }
        }
    }

    public boolean showConfirmAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "");
        alert.setTitle("Подтверждение операции");
        alert.setHeaderText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public String showTextInputDialog(String filename, String headerText, String contentText) {
        TextInputDialog dialog = new TextInputDialog(filename);
        dialog.setTitle("Введите данные");
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public void showInfoAlert(String message, Alert.AlertType type, boolean needWait) {
        Alert alert = new Alert(type, "", ButtonType.OK);
        alert.setTitle("Информационное окно");
        alert.setHeaderText(message);
        if (needWait) {
            alert.showAndWait();
        } else {
            alert.show();
        }
    }

    public void refreshServerFilesList() {
        serverTable.updateList(Paths.get(serverTable.getCurrentPathStr()));
    }

    public void refreshClientFilesList() {
        clientTable.updateList(Paths.get(clientTable.getCurrentPathStr()));
    }
}
