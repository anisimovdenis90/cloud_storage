package util;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

import java.nio.file.Path;
import java.text.DecimalFormat;

public class TransferItem {

    public enum Operation {
        DOWNLOAD,
        UPLOAD
    }

    private final Path sourcePath;
    private final Path dstPath;

    private final Operation operation;
    private ImageView operationImage;
    private final Button operationButton;
    private final ProgressIndicator progressIndicator;
    private final String fileName;
    private String fileSize;
    private final Button filePathButton;
    private final Button deleteItemButton;
    private boolean isSuccess = false;

    public TransferItem(Operation operation, Path sourceFile, Path dstFile) {
        this.sourcePath = sourceFile;
        this.dstPath = dstFile;
        this.operation = operation;
        if (operation.equals(Operation.DOWNLOAD)) {
            operationImage = new ImageView("img/download.png");
        } else if (operation.equals(Operation.UPLOAD)) {
            operationImage = new ImageView("img/upload.png");
        }
        this.operationButton = new Button("", operationImage);
        this.fileName = sourceFile.getFileName().toString();
        this.fileSize = sizeToStringFormatter(sourceFile.toFile().length());
        this.filePathButton = new Button(dstFile.toString());
        this.progressIndicator = new ProgressIndicator();
        this.deleteItemButton = new Button("", new ImageView("img/delete.png"));
        disableButtons();
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

    public void setFileSize(long fileSize) {
        this.fileSize = sizeToStringFormatter(fileSize);
    }

    public Button getOperationButton() {
        return operationButton;
    }

    public String getFileSize() {
        return fileSize;
    }

    public Button getFilePathButton() {
        return filePathButton;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public ImageView getOperationImage() {
        return operationImage;
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

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    public void setProgressIndicator(double progressValue) {
        Platform.runLater(() -> progressIndicator.setProgress(progressValue));

    }

    public void disableButtons() {
        deleteItemButton.setDisable(true);
        filePathButton.setDisable(true);
        operationButton.setDisable(true);
    }

    public void enableButtons() {
        deleteItemButton.setDisable(false);
        filePathButton.setDisable(false);
        operationButton.setDisable(false);
    }
}
