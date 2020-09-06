package controllers;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import services.FileTransfer;
import util.FileSizeLongToStringFormatter;
import util.TransferItem;

import java.util.List;

public class OperationTableController {

    private final TableView<TransferItem> operationTable;
    private final TableColumn<TransferItem, Button> resetTransferColumn;
    private final TableColumn<TransferItem, Button> operationColumn;
    private final TableColumn<TransferItem, ProgressIndicator> progressColumn;
    private final TableColumn<TransferItem, String> fileNameColumn;
    private final TableColumn<TransferItem, Long> fileSizeColumn;
    private final TableColumn<TransferItem, Button> filePathColumn;
    private final TableColumn<TransferItem, Button> deleteColumn;

    private final Button clearQueue;
    private final Label currentOperationsCountLbl;
    private final Label totalOperationsCountLbl;
    private final ProgressIndicator operationsProgress;

    private final MainWindowController mainWindowController;
    private final ClientPanelController clientPanel;
    private final ServerPanelController serverPanel;

    public OperationTableController(TableView<TransferItem> operationTable,
                                    TableColumn<TransferItem, Button> resetTransferColumn,
                                    TableColumn<TransferItem, Button> operationColumn,
                                    TableColumn<TransferItem, ProgressIndicator> progressColumn,
                                    TableColumn<TransferItem, String> fileNameColumn,
                                    TableColumn<TransferItem, Long> fileSizeColumn,
                                    TableColumn<TransferItem, Button> filePathColumn,
                                    TableColumn<TransferItem, Button> deleteColumn,
                                    Button clearQueue,
                                    Label currentOperationsCount,
                                    Label totalOperationsCount,
                                    ProgressIndicator operationsProgress,
                                    MainWindowController mainWindowController,
                                    ClientPanelController clientPanel,
                                    ServerPanelController serverPanel
    ) {
        this.operationTable = operationTable;
        this.resetTransferColumn = resetTransferColumn;
        this.operationColumn = operationColumn;
        this.progressColumn = progressColumn;
        this.fileNameColumn = fileNameColumn;
        this.fileSizeColumn = fileSizeColumn;
        this.filePathColumn = filePathColumn;
        this.deleteColumn = deleteColumn;
        this.clearQueue = clearQueue;
        this.currentOperationsCountLbl = currentOperationsCount;
        this.totalOperationsCountLbl = totalOperationsCount;
        this.mainWindowController = mainWindowController;
        this.operationsProgress = operationsProgress;
        this.clientPanel = clientPanel;
        this.serverPanel = serverPanel;
        initialize();
    }

    private void initialize() {
        resetTransferColumn.setCellValueFactory(new PropertyValueFactory<>("performAgainItemButton"));
        operationColumn.setCellValueFactory(new PropertyValueFactory<>("operationButton"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressIndicator"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        filePathColumn.setCellValueFactory(new PropertyValueFactory<>("filePathButton"));
        deleteColumn.setCellValueFactory(new PropertyValueFactory<>("deleteItemButton"));

        fileSizeColumn.setCellFactory(new Callback<TableColumn<TransferItem, Long>, TableCell<TransferItem, Long>>() {
            @Override
            public TableCell<TransferItem, Long> call(TableColumn<TransferItem, Long> column) {
                return new TableCell<TransferItem, Long>() {
                    @Override
                    protected void updateItem(Long item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            String text = FileSizeLongToStringFormatter.format(item);
                            setText(text);
                        }
                    }
                };
            }
        });

        operationTable.setPlaceholder(new Label("Очередь операций пуста"));

        ContextMenu contextMenu = new ContextMenu();
        MenuItem clearAllItems = new MenuItem("Очистить очередь");
        clearAllItems.setOnAction(event -> {
            if (!clearQueue.isDisabled()) {
                clearOperationTable();
            }
        });
        contextMenu.getItems().add(clearAllItems);
        operationTable.setContextMenu(contextMenu);
    }

    public List<TransferItem> getItemsList() {
        return operationTable.getItems();
    }

    public void setOperationsProgress(double value) {
        Platform.runLater(() -> operationsProgress.setProgress(value));
    }

    public void setCurrentOperationsCountLbl(String text) {
        Platform.runLater(() -> currentOperationsCountLbl.setText(text));
    }

    public void setTotalOperationsCountLbl(String text) {
        Platform.runLater(() -> totalOperationsCountLbl.setText(text));
    }

    public void clearOperationTable() {
        Platform.runLater(() -> operationTable.getItems().clear());
        FileTransfer.getInstance().clearOperationTable();
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
        setActionsOnTransferItemButtons(item);
        operationTable.getItems().add(item);
    }

    private void setActionsOnTransferItemButtons(TransferItem item) {
        item.getPerformAgainItemButton().setOnAction(event -> FileTransfer.getInstance().reloadTransferItem(item));
        item.getPerformAgainItemButton().setTooltip(new Tooltip("Выполнить повторно"));

        item.getDeleteItemButton().setOnAction(event -> {
            operationTable.getItems().remove(item);
            FileTransfer.getInstance().deleteItemFromOperationTable(item);
        });
        item.getDeleteItemButton().setTooltip(new Tooltip("Удалить из истории"));

        item.getFilePathButton().setOnAction(event -> {
            if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                clientPanel.updateList(item.getDstPath());
            } else if (item.getOperation().equals(TransferItem.Operation.UPLOAD)) {
                serverPanel.updateList(item.getDstPath());
            }
        });
        item.getFilePathButton().setTooltip(new Tooltip("Перейти в каталог с файлом"));

        item.getOperationButton().setOnAction(event -> {
            if (item.isSuccess()) {
                String message;
                if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                    message = "Файл \"" + item.getSourcePath().getFileName() + "\" успешно скачен с сервера в папку \"" + item.getDstPath() + "\"";
                } else {
                    message = "Файл \"" + item.getSourcePath().getFileName() + "\" успешно загружен на сервер в папку " + item.getDstPath() + "\"";
                }
                mainWindowController.showInfoAlert(message, Alert.AlertType.INFORMATION, false);
            } else {
                mainWindowController.showInfoAlert("Ошибка передачи файла \"" + item.getFileName() + "\"", Alert.AlertType.INFORMATION, false);
            }
        });
        item.getOperationButton().setTooltip(new Tooltip("Информация об операции"));
    }

    public void updateOperationTable(List<TransferItem> list) {
        list.forEach(this::updateOperationTable);
    }
}
