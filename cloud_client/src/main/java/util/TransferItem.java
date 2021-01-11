package util;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

import java.nio.file.Path;

public class TransferItem {

    public enum Operation {
        DOWNLOAD,
        UPLOAD
    }

    private final String fileName;
    private final Path sourcePath;
    private final Path dstPath;
    private final Operation operation;
    private final Button operationButton;
    private final ProgressIndicator progressIndicator;
    private final Button filePathButton;
    private final Button deleteItemButton;
    private long fileSize;
    private Button performAgainItemButton;
    private boolean isSuccess = false;

    public TransferItem(Operation operation, Path sourceFile, Path dstFile) {
        this.sourcePath = sourceFile;
        this.dstPath = dstFile;
        this.operation = operation;
        this.operationButton = new Button("");
        operationButton.setFocusTraversable(false);
        if (operation.equals(Operation.DOWNLOAD)) {
            operationButton.setGraphic(new ImageView("img/download.png"));
        } else if (operation.equals(Operation.UPLOAD)) {
            operationButton.setGraphic(new ImageView("img/upload.png"));
        }
        this.fileName = sourceFile.getFileName().toString();
        this.fileSize = sourceFile.toFile().length();
        this.filePathButton = new Button(dstFile.toString());
        filePathButton.setFocusTraversable(false);
        this.progressIndicator = new ProgressIndicator();
        this.deleteItemButton = new Button("", new ImageView("img/delete.png"));
        deleteItemButton.setFocusTraversable(false);
        this.performAgainItemButton = new Button("", new ImageView("img/reset.png"));
        performAgainItemButton.setFocusTraversable(false);
        performAgainItemButton.setMaxHeight(20);
        performAgainItemButton.setMaxWidth(20);
        performAgainItemButton.setMinHeight(20);
        performAgainItemButton.setMinWidth(20);
        performAgainItemButton.setPrefHeight(20);
        performAgainItemButton.setPrefWidth(20);
        performAgainItemButton.setVisible(false);
        disableButtons();
    }

    public Button getOperationButton() {
        return operationButton;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setPerformAgainItemButton(Button performAgainItemButton) {
        this.performAgainItemButton = performAgainItemButton;
    }

    public Button getPerformAgainItemButton() {
        return performAgainItemButton;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Button getFilePathButton() {
        return filePathButton;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public Path getDstPath() {
        return dstPath;
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

    public void setProgressIndicator(double progressValue) {
        Platform.runLater(() -> progressIndicator.setProgress(progressValue));
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    public void setOnUnsuccessful() {
        isSuccess = false;
        if (operation.equals(Operation.DOWNLOAD)) {
            Platform.runLater(() -> operationButton.setGraphic(new ImageView("img/error_download.png")));
        } else if (operation.equals(Operation.UPLOAD)) {
            Platform.runLater(() -> operationButton.setGraphic(new ImageView("img/error_upload.png")));
        }
        filePathButton.setDisable(true);
        performAgainItemButton.setVisible(true);
        enableButtons();
    }

    public void setOnSuccessful() {
        isSuccess = true;
        performAgainItemButton = null;
        enableButtons();
    }

    public void blockProcessing() {
        deleteItemButton.setDisable(true);
    }

    public void blockTransfer() {
        filePathButton.setDisable(true);
    }

    public void unBlockTransfer() {
        filePathButton.setDisable(false);
    }

    private void disableButtons() {
        operationButton.setDisable(true);
        filePathButton.setDisable(true);
    }

    private void enableButtons() {
        operationButton.setDisable(false);
        filePathButton.setDisable(false);
        deleteItemButton.setDisable(false);
    }
}
