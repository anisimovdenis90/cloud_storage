package util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

import java.nio.file.Path;

public class TransferItem {

    public enum Operation {
        DOWNLOAD,
        UPLOAD
    }

    private Operation operation;
    private ImageView operationImage;
    private Path sourceFile;
    private String fileName;
    private Path dstFile;
    private ProgressIndicator progressIndicator;
    private Button infoButton;
    private Button deleteItemButton;
    private Button goToFileButton;
    private boolean isSuccess = false;

    public TransferItem(Operation operation, Path sourceFile, Path dstFile) {
        this.operation = operation;
        if (operation.equals(Operation.DOWNLOAD)) {
            operationImage = new ImageView("img/download.png");
        } else if (operation.equals(Operation.UPLOAD)) {
            operationImage = new ImageView("img/upload.png");
        }
        this.sourceFile = sourceFile;
        this.fileName = sourceFile.getFileName().toString();
        this.dstFile = dstFile;
        this.progressIndicator = new ProgressIndicator();
        this.infoButton = new Button("Инфо");
        this.deleteItemButton = new Button("", new ImageView("img/delete.png"));
        this.goToFileButton = new Button("", new ImageView("img/link.png"));
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public ImageView getOperationImage() {
        return operationImage;
    }

    public Button getGoToFileButton() {
        return goToFileButton;
    }

    public Path getDstFile() {
        return dstFile;
    }

    public Button getInfoButton() {
        return infoButton;
    }

    public Button getDeleteItemButton() {
        return deleteItemButton;
    }

    public String getFileName() {
        return fileName;
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    public void setProgressIndicator(double progressValue) {
        progressIndicator.setProgress(progressValue);
    }
}
