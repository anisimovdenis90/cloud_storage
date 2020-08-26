package controllers;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import util.TransferItem;

import java.util.List;

public class OperationTableController {

    private final TableView<TransferItem> operationTable;
    private final TableColumn<TransferItem, Button> operationColumn;
    private final TableColumn<TransferItem, ProgressIndicator> progressColumn;
    private final TableColumn<TransferItem, String> fileNameColumn;
    private final TableColumn<TransferItem, String> fileSizeColumn;
    private final TableColumn<TransferItem, Button> filePathColumn;
    private final TableColumn<TransferItem, Button> deleteColumn;
    private final Button clearQueue;

    private final MainWindowController mainWindowController;
    private final ClientPanelController clientPanel;
    private final ServerPanelController serverPanel;

    public OperationTableController(TableView<TransferItem> operationTable,
                                    TableColumn<TransferItem, Button> operationColumn,
                                    TableColumn<TransferItem, ProgressIndicator> progressColumn,
                                    TableColumn<TransferItem, String> fileNameColumn,
                                    TableColumn<TransferItem, String> fileSizeColumn,
                                    TableColumn<TransferItem, Button> filePathColumn,
                                    TableColumn<TransferItem, Button> deleteColumn,
                                    Button clearQueue,
                                    MainWindowController mainWindowController,
                                    ClientPanelController clientPanel,
                                    ServerPanelController serverPanel
    ) {
        this.operationTable = operationTable;
        this.operationColumn = operationColumn;
        this.progressColumn = progressColumn;
        this.fileNameColumn = fileNameColumn;
        this.fileSizeColumn = fileSizeColumn;
        this.filePathColumn = filePathColumn;
        this.deleteColumn = deleteColumn;
        this.clearQueue = clearQueue;
        this.mainWindowController = mainWindowController;
        this.clientPanel = clientPanel;
        this.serverPanel = serverPanel;
        initialize();
    }

    private void initialize() {
        operationColumn.setCellValueFactory(new PropertyValueFactory<>("operationButton"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressIndicator"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        filePathColumn.setCellValueFactory(new PropertyValueFactory<>("filePathButton"));
        deleteColumn.setCellValueFactory(new PropertyValueFactory<>("deleteItemButton"));
        operationTable.setPlaceholder(new Label("Отсутствуют элементы для отображения"));

        clearQueue.setTooltip(new Tooltip("Очистить очередь операций"));
        ContextMenu contextMenu = new ContextMenu();
        MenuItem clearAllItems = new MenuItem("Очистить очередь");
        clearAllItems.setOnAction(event -> {
            if(!clearQueue.isDisabled()) {
                clearOperationTable();
            }
        });
        contextMenu.getItems().add(clearAllItems);
        operationTable.setContextMenu(contextMenu);
    }

    public void clearOperationTable() {
        Platform.runLater(() -> operationTable.getItems().clear());
    }

    public void disableButtons() {
        clearQueue.setDisable(true);
    }

    public void enableButtons() {
        clearQueue.setDisable(false);
    }

    public void scrollToElement(TransferItem scrollToItem) {
        Platform.runLater(() -> operationTable.scrollTo(scrollToItem));
    }

    public void updateOperationTable(TransferItem item) {
        setDeleteItemButtonAction(item);
        setGoToFilePathButtonAction(item);
        setOperationButtonAction(item);
        operationTable.getItems().add(item);
    }

    public void updateOperationTable(List<TransferItem> list) {
        Platform.runLater(() -> list.forEach(this::updateOperationTable));
    }

    private void setDeleteItemButtonAction(TransferItem item) {
        item.getDeleteItemButton().setOnAction(event -> operationTable.getItems().remove(item));
        item.getDeleteItemButton().setTooltip(new Tooltip("Удалить из истории"));
    }

    private void setOperationButtonAction(TransferItem item) {
        item.getOperationButton().setOnAction(event -> {
            if (item.isSuccess()) {
                String message;
                if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                    message = "Файл " + item.getSourcePath().getFileName() + " успешно скачен с сервера в папку " + item.getDstPath();
                } else {
                    message = "Файл " + item.getSourcePath().getFileName() + " успешно загружен на сервер в папку " + item.getDstPath();
                }
                mainWindowController.showInfoAlert(message, Alert.AlertType.INFORMATION, false);
            } else {
                mainWindowController.showInfoAlert("Ошибка передачи файла " + item.getFileName(), Alert.AlertType.INFORMATION, false);
            }
        });
        item.getOperationButton().setTooltip(new Tooltip("Информация об операции"));
    }

    private void setGoToFilePathButtonAction(TransferItem item) {
        item.getFilePathButton().setOnAction(event -> {

            if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                clientPanel.updateList(item.getDstPath());
            } else if (item.getOperation().equals(TransferItem.Operation.UPLOAD)) {
                serverPanel.updateList(item.getDstPath());
            }
        });
        item.getFilePathButton().setTooltip(new Tooltip("Перейти в каталог с файлом"));
    }
}
