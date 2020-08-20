package controllers;

import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import util.TransferItem;

public class OperationTableController {

    protected TableView<TransferItem> operationTable;
    protected TableColumn<TransferItem, String> operationColumn;
    protected TableColumn<TransferItem, Button> infoColumn;
    protected TableColumn<TransferItem, String> fileNameColumn;
    protected TableColumn<TransferItem, ProgressIndicator> progressColumn;
    protected TableColumn<TransferItem, Button> goToFileColumn;
    protected TableColumn<TransferItem, Button> deleteItemColumn;

    private MainWindowController mainWindowController;
    private ClientPanelController clientPanel;
    private ServerPanelController serverPanel;

    public OperationTableController(TableView<TransferItem> operationTable,
                                    TableColumn<TransferItem, String> operationColumn,
                                    TableColumn<TransferItem, Button> infoColumn,
                                    TableColumn<TransferItem, String> fileNameColumn,
                                    TableColumn<TransferItem, ProgressIndicator> progressColumn,
                                    TableColumn<TransferItem, Button> goToFileColumn,
                                    TableColumn<TransferItem, Button> deleteItemColumn,
                                    MainWindowController mainWindowController,
                                    ClientPanelController clientPanel,
                                    ServerPanelController serverPanel
                                    ) {
        this.operationTable = operationTable;
        this.operationColumn = operationColumn;
        this.infoColumn = infoColumn;
        this.fileNameColumn = fileNameColumn;
        this.progressColumn = progressColumn;
        this.goToFileColumn = goToFileColumn;
        this.deleteItemColumn = deleteItemColumn;
        this.mainWindowController = mainWindowController;
        this.clientPanel = clientPanel;
        this.serverPanel = serverPanel;
        initialize();
    }

    private void initialize() {
        operationColumn.setCellValueFactory(new PropertyValueFactory<>("operationImage"));
        infoColumn.setCellValueFactory(new PropertyValueFactory<>("infoButton"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressIndicator"));
        goToFileColumn.setCellValueFactory(new PropertyValueFactory<>("goToFileButton"));
        deleteItemColumn.setCellValueFactory(new PropertyValueFactory<>("deleteItemButton"));
        addAutoScroll();
    }

    private void addAutoScroll() {
        operationTable.getItems().addListener((ListChangeListener<TransferItem>) c -> {
            c.next();
            final int size = operationTable.getItems().size();
            if (size > 0) {
                operationTable.scrollTo(size - 1);
            }
        });
    }

    public void updateOperationTable(TransferItem item) {
        setDeleteItemButtonAction(item);
        setGoToFileButtonAction(item);
        setInfoButtonAction(item);
        operationTable.getItems().add(item);
    }

    private void setDeleteItemButtonAction(TransferItem item) {
        item.getDeleteItemButton().setOnAction(event -> operationTable.getItems().remove(item));
    }
    private void setInfoButtonAction(TransferItem item) {
        item.getInfoButton().setOnAction(event -> {
            if (item.isSuccess()) {
                String message;
                if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                    message = "Файл " + item.getFileName() + " успешно скачен с сервера в папку " + item.getDstFile();
                } else {
                    message = "Файл " + item.getFileName() + " успешно загружен на сервер в папку " + item.getDstFile();
                }
                mainWindowController.showInfoAlert(message, Alert.AlertType.INFORMATION, false);
            } else {
                mainWindowController.showInfoAlert("Ошибка передачи файла " + item.getFileName(), Alert.AlertType.INFORMATION, false);
            }
        });
    }

    private void setGoToFileButtonAction(TransferItem item) {
        item.getGoToFileButton().setOnAction(event -> {
            if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                clientPanel.updateList(item.getDstFile());
            } else if (item.getOperation().equals(TransferItem.Operation.UPLOAD)) {
                serverPanel.updateList(item.getDstFile());
            }
        });
    }
}
