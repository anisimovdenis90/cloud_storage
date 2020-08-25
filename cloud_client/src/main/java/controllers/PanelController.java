package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import util.FileInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Comparator;

public abstract class PanelController {

    protected TableView<FileInfo> table;
    protected TableColumn<FileInfo, String> typeFileColumn;
    protected TableColumn<FileInfo, String> fileNameColumn;
    protected TableColumn<FileInfo, Long> fileSizeColumn;
    protected TableColumn<FileInfo, String> fileDateColumn;

    protected TextField pathField;

    protected Path rootPath;

    public PanelController(TableView<FileInfo> table,
                           TableColumn<FileInfo, String> typeFileColumn,
                           TableColumn<FileInfo, String> fileNameColumn,
                           TableColumn<FileInfo, Long> fileSizeColumn,
                           TableColumn<FileInfo, String> fileDateColumn,
                           TextField pathField
    ) {
        this.table = table;
        this.typeFileColumn = typeFileColumn;
        this.fileNameColumn = fileNameColumn;
        this.fileSizeColumn = fileSizeColumn;
        this.fileDateColumn = fileDateColumn;
        this.pathField = pathField;
        initialize();
    }

    public void setRootPath(String rootPath) {
        this.rootPath = Paths.get(rootPath);
    }

    private void initialize() {
        typeFileColumn.setCellValueFactory(new PropertyValueFactory<>("fileIcon"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        fileDateColumn.setCellValueFactory(new PropertyValueFactory<>("lastModified"));

        fileSizeColumn.setCellFactory(column -> new TableCell<FileInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else if (item == -1L) {
                    setText("-");
                } else {
                    String text = sizeToStringFormatter(item);
                    setText(text);
                }
            }
        });
        table.setPlaceholder(new Label("Отсутствуют файлы для отображения"));
        setMouseOnTableAction();
    }

    public abstract void buttonPathUpAction(ActionEvent actionEvent);

    public abstract void updateList(Path path);

    public void updateList() {
        updateList(rootPath);
    }

    public abstract void setMouseOnTableAction();

    public String getCurrentPathStr() {
        return pathField.getText();
    }

    public String getSelectedFileNameStr() {
        if (checkSelectedItemNotNull()) {
            return table.getSelectionModel().getSelectedItem().getFileName();
        }
        return null;
    }

    protected boolean checkSelectedItemNotNull() {
        return table.isFocused() && table.getSelectionModel() != null && table.getSelectionModel().getSelectedItem() != null;
    }

    public Object getSelectedItem() {
        if (checkSelectedItemNotNull()) {
            return table.getSelectionModel().getSelectedItem();
        }
        return null;
    }

    private String sizeToStringFormatter(Long fileSize) {
        double doubleSize;
        DecimalFormat decimalFormat = new DecimalFormat( "#.##" );
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            doubleSize = (double) fileSize / 1024;
            return decimalFormat.format(doubleSize) + " KB";
        } else if (fileSize < 1024 * 1024 * 1024) {
            doubleSize = (double) fileSize / (1024 * 1024);
            return decimalFormat.format(doubleSize) + " MB";
        } else {
            doubleSize = (double) fileSize / (1024 * 1024 * 1024);
            return decimalFormat.format(doubleSize) + " GB";
        }
    }
}
