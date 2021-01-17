package services;

import commands.FileMessageCommand;
import commands.FileRequestCommand;
import controllers.MainWindowController;
import controllers.OperationTableController;
import javafx.scene.control.Alert;
import util.TransferItem;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransfer {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;
    private static final int LIMIT_OF_OPERATIONS = 300;

    private final ExecutorService executor;
    private static volatile FileTransfer instance;
    private ArrayBlockingQueue<TransferItem> mainQueue;
    private int totalOperations;
    private int performedOperations;
    private long totalOperationsSize;
    private long currentOperationsSize;
    private boolean isTransferActive = false;
    private TransferItem currentTransferItem;

    private OperationTableController operationTable;
    private MainWindowController mainWindow;

    private FileOutputStream fileWriter;

    private FileTransfer() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    public static FileTransfer getInstance() {
        if (instance == null) {
            synchronized (FileTransfer.class) {
                if (instance == null) {
                    instance = new FileTransfer();
                }
            }
        }
        return instance;
    }

    public void init(OperationTableController operationTable, MainWindowController mainWindow) {
        this.mainQueue = new ArrayBlockingQueue<>(LIMIT_OF_OPERATIONS, true);
        this.operationTable = operationTable;
        this.mainWindow = mainWindow;
        this.totalOperations = 0;
        this.performedOperations = 0;
        this.totalOperationsSize = 0;
        this.currentOperationsSize = 0;
        setInfoLabels();
    }

    public TransferItem.Operation getCurrentOperation() {
        if (currentTransferItem != null) {
            return currentTransferItem.getOperation();
        }
        return null;
    }

    public void addItemToQueue(List<TransferItem> list) {
        final int checkCounts = totalOperations + list.size();
        if (!checkQueueCapacity(checkCounts)) {
            return;
        }
        if (!isTransferActive) {
            totalOperationsSize = 0;
            currentOperationsSize = 0;
        }
        for (final TransferItem transferItem : list) {
            totalOperationsSize += transferItem.getFileSize();
            transferItem.blockTransfer();
        }
        totalOperations += list.size();
        operationTable.updateOperationTable(list);
        setInfoLabels();
        mainQueue.addAll(list);
        runWorkThread();
    }

    public void addItemToQueue(TransferItem item) {
        addItemToQueue(Collections.singletonList(item));
    }

    public void reloadTransferItem(TransferItem item) {
        currentOperationsSize -= item.getFileSize();
        performedOperations--;
        mainQueue.add(item);
        runWorkThread();
    }

    public boolean checkQueueCapacity(int countOfElements) {
        if (countOfElements > LIMIT_OF_OPERATIONS) {
            final String message = "Превышен предел операций %d/%d. Очистите очередь операций.";
            mainWindow.showInfoAlert(String.format(message, countOfElements, LIMIT_OF_OPERATIONS), Alert.AlertType.WARNING, true);
            return false;
        }
        return true;
    }

    public void clearOperationTable() {
        totalOperations = 0;
        performedOperations = 0;
        operationTable.setOperationsProgress(0);
        setInfoLabels();
    }

    public void deleteItemFromOperationTable(TransferItem item) {
        if (mainQueue.contains(item)) {
            mainQueue.remove(item);
            totalOperationsSize -= item.getFileSize();
        } else {
            performedOperations--;
        }
        totalOperations--;
        if (totalOperations == 0) {
            clearOperationTable();
            return;
        }
        setInfoLabels();
    }

    private void setInfoLabels() {
        operationTable.setCurrentOperationsCountLbl(performedOperations + "/" + totalOperations);
        operationTable.setTotalOperationsCountLbl(totalOperations + "/" + LIMIT_OF_OPERATIONS);
    }

    private void runWorkThread() {
        executor.execute(() -> {
            if (mainQueue.isEmpty()) {
                return;
            }
            mainWindow.disableButtons();
            operationTable.disableButtons();
            isTransferActive = true;
            while (!mainQueue.isEmpty()) {
                final TransferItem item = mainQueue.poll();
                currentTransferItem = item;
                currentTransferItem.blockProcessing();
                if (item.getOperation().equals(TransferItem.Operation.UPLOAD)) {
                    sendFileToServer(item);
                } else if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                    getFileFromServer(item);
                }
            }
            operationTable.getItemsList().forEach(TransferItem::unBlockTransfer);
            currentTransferItem = null;
            mainWindow.enableButtons();
            operationTable.enableButtons();
            mainWindow.refreshServerFilesList();
            isTransferActive = false;
        });
    }

    public void getFileFromServer(TransferItem transferItem) {
        System.out.printf("Начало скачивания файла \"%s\" с сервера%n", transferItem.getSourcePath());
        NetworkClient.getInstance().sendCommandToServer(new FileRequestCommand(transferItem.getSourcePath().toString()));
        try {
            final Path dstFilePath = Paths.get(transferItem.getDstPath().toString(), transferItem.getFileName());
            Files.createDirectories(dstFilePath.getParent());
            if (Files.exists(dstFilePath)) {
                Files.delete(dstFilePath);
            }
            this.fileWriter = new FileOutputStream(dstFilePath.toFile(), true);
            operationTable.scrollToElement(transferItem);
            while (true) {
                final FileMessageCommand command = (FileMessageCommand) NetworkClient.getInstance().readCommandFromServer();

                System.out.printf("Часть %d из %d файла %s%n", command.getPartNumber(), command.getPartsOfFile(), command.getFileName());

                fileWriter.write(command.getData());
                currentOperationsSize += command.getData().length;

                operationTable.setOperationsProgress((float) currentOperationsSize / totalOperationsSize);
                transferItem.setProgressIndicator((float) command.getPartNumber() / command.getPartsOfFile());
                if (command.getPartNumber() == command.getPartsOfFile()) {
                    break;
                }
            }
            System.out.printf("Файл %s успешно скачен с сервера%n", transferItem.getSourcePath());
            fileWriter.close();
            transferItem.setOnSuccessful();
            performedOperations++;
            operationTable.setCurrentOperationsCountLbl(performedOperations + "/" + totalOperations);
            mainWindow.refreshClientFilesList();
        } catch (IOException e) {
            transferItem.setOnUnsuccessful();
            System.out.printf("Ошибка скачивания файла %s с сервера%n", transferItem.getSourcePath());
            e.printStackTrace();
        }
    }

    public void sendFileToServer(TransferItem transferItem) {
        System.out.printf("Начало передачи файла \"%s\" на сервер%n", transferItem.getSourcePath());
        final long fileSize = transferItem.getSourcePath().toFile().length();
        long partsOfFile = fileSize / DEFAULT_BUFFER_SIZE;
        if (fileSize % DEFAULT_BUFFER_SIZE != 0 || fileSize == 0) {
            partsOfFile++;
        }
        System.out.printf("Размер файла %d, количество частей %d%n", fileSize, partsOfFile);
        final FileMessageCommand fileToServerCommand = new FileMessageCommand(
                transferItem.getSourcePath().getFileName().toString(),
                transferItem.getDstPath().toString(),
                fileSize,
                partsOfFile,
                0,
                fileSize < DEFAULT_BUFFER_SIZE ? new byte[(int) fileSize] : new byte[DEFAULT_BUFFER_SIZE]
        );
        operationTable.scrollToElement(transferItem);
        try (FileInputStream fileReader = new FileInputStream(transferItem.getSourcePath().toFile())) {
            int readBytes;
            long partsSend = 0;
            do {
                readBytes = fileReader.read(fileToServerCommand.getData());
                fileToServerCommand.setPartNumber(partsSend + 1);
                if (readBytes < fileToServerCommand.getData().length) {
                    fileToServerCommand.setData(Arrays.copyOfRange(fileToServerCommand.getData(), 0, Math.max(readBytes, 0)));
                }
                NetworkClient.getInstance().sendCommandToServer(fileToServerCommand);
                partsSend++;
                System.out.printf("Отправлена часть %d из %d файла %s%n", partsSend, partsOfFile, transferItem.getFileName());
                currentOperationsSize += readBytes;
                operationTable.setOperationsProgress((float) currentOperationsSize / totalOperationsSize);
                transferItem.setProgressIndicator((float) partsSend / partsOfFile);
            } while (partsSend != partsOfFile);
            System.out.printf("Файл %s успешно отправлен на сервер%n", transferItem.getSourcePath());
            transferItem.setOnSuccessful();
            performedOperations++;
            operationTable.setCurrentOperationsCountLbl(performedOperations + "/" + totalOperations);
        } catch (IOException e) {
            transferItem.setOnUnsuccessful();
            System.out.printf("Ошибка отправки файла %s на сервер%n", transferItem.getSourcePath());
            e.printStackTrace();
        }
    }

    public void cancelDownload() {
        try {
            fileWriter.close();
            final Path deletePath = currentTransferItem.getDstPath().resolve(currentTransferItem.getFileName());
            Files.delete(deletePath);
            System.out.printf("Выполнено удаление частично скаченного файла %s%n", deletePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
