package controllers;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import util.FileInfo;
import util.FileSizeLongToStringFormatter;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class PanelController {

    protected final TableView<FileInfo> table;
    protected final TableColumn<FileInfo, String> iconFileColumn;
    protected final TableColumn<FileInfo, String> fileTypeColumn;
    protected final TableColumn<FileInfo, String> fileNameColumn;
    protected final TableColumn<FileInfo, Long> fileSizeColumn;
    protected final TableColumn<FileInfo, String> fileDateColumn;

    protected final TextField pathField;

    protected Path rootPath;

    public PanelController(TableView<FileInfo> table,
                           TableColumn<FileInfo, String> iconFileColumn,
                           TableColumn<FileInfo, String> fileTypeColumn,
                           TableColumn<FileInfo, String> fileNameColumn,
                           TableColumn<FileInfo, Long> fileSizeColumn,
                           TableColumn<FileInfo, String> fileDateColumn,
                           TextField pathField
    ) {
        this.table = table;
        this.iconFileColumn = iconFileColumn;
        this.fileTypeColumn = fileTypeColumn;
        this.fileNameColumn = fileNameColumn;
        this.fileSizeColumn = fileSizeColumn;
        this.fileDateColumn = fileDateColumn;
        this.pathField = pathField;
        initialize();
    }

    public void setRootPath(String rootPathStr) {
        this.rootPath = Paths.get(rootPathStr);
    }

    private void initialize() {
        iconFileColumn.setCellValueFactory(new PropertyValueFactory<>("fileIcon"));
        fileTypeColumn.setCellValueFactory(new PropertyValueFactory<>("typeName"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        fileDateColumn.setCellValueFactory(new PropertyValueFactory<>("lastModified"));

        table.setOnSort(event -> {
            if (!table.getSortOrder().contains(fileTypeColumn)) {
                table.getSortOrder().add(fileTypeColumn);
            }
            if (!table.getSortOrder().contains(fileNameColumn)) {
                table.getSortOrder().add(fileNameColumn);
            }
        });

        fileSizeColumn.setCellFactory(new Callback<TableColumn<FileInfo, Long>, TableCell<FileInfo, Long>>() {
            @Override
            public TableCell<FileInfo, Long> call(TableColumn<FileInfo, Long> column) {
                return new TableCell<FileInfo, Long>() {
                    @Override
                    protected void updateItem(Long item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else if (item == -1L) {
                            setText("-");
                        } else {
                            final String text = FileSizeLongToStringFormatter.format(item);
                            setText(text);
                        }
                    }
                };
            }
        });
        table.setPlaceholder(new Label("Отсутствуют файлы для отображения"));
        table.getSortOrder().add(fileTypeColumn);
        table.getSortOrder().add(fileNameColumn);
        table.sort();
        setMouseOnTableAction();
    }

    public abstract void updateList(Path path);

    public abstract void setMouseOnTableAction();

    public void updateList() {
        updateList(rootPath);
    }

    public void buttonPathUpAction() {
        final Path upperPath = Paths.get(getCurrentPathStr()).getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }
    }

    public String getCurrentPathStr() {
        return pathField.getText();
    }

    public String getSelectedFileNameStr() {
        if (checkSelectedItemNotNull()) {
            return table.getSelectionModel().getSelectedItem().getFileName();
        }
        return null;
    }

    public FileInfo getSelectedItem() {
        if (checkSelectedItemNotNull()) {
            return table.getSelectionModel().getSelectedItem();
        }
        return null;
    }

    protected boolean checkSelectedItemNotNull() {
        return table.isFocused() && table.getSelectionModel() != null && table.getSelectionModel().getSelectedItem() != null;
    }
}
