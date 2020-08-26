package util;

import commands.FileMessageCommand;
import commands.FileRequestCommand;
import controllers.MainWindowController;
import controllers.OperationTableController;
import javafx.scene.control.Alert;
import services.NetworkClient;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

public class FileTransfer {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;
    private static final int LIMIT_OF_ELEMENT = 300;
    private final ArrayBlockingQueue<TransferItem> mainQueue;
    private Thread workThread;
    private ArrayList<TransferItem> toProcessingList;
    private int count;
    private boolean isTransferActive = false;

    private OperationTableController operationTable;
    private MainWindowController mainWindow;

    public FileTransfer(OperationTableController operationTable, MainWindowController mainWindow) {
        this.mainQueue = new ArrayBlockingQueue<>(LIMIT_OF_ELEMENT, true);
        this.operationTable = operationTable;
        this.mainWindow = mainWindow;
    }

    public boolean isTransferActive() {
        return isTransferActive;
    }

    public void addItemToQueue(List<TransferItem> list) {
        if (!checkQueueCapacity(list.size())) {
            mainWindow.showInfoAlert("Очистите очередь операций, не более 300 элементов", Alert.AlertType.WARNING, true);
            return;
        }
        operationTable.updateOperationTable(list);
        mainQueue.addAll(list);
        runWorkThread();
    }

    public void addItemToQueue(TransferItem item) {
        if (!checkQueueCapacity(1)) {
            mainWindow.showInfoAlert("Очистите очередь операций, не более 300 элементов", Alert.AlertType.WARNING, true);
            return;
        }
        operationTable.updateOperationTable(item);
        mainQueue.add(item);
        runWorkThread();
    }

    private void runWorkThread() {
        if (workThread != null && !workThread.getState().equals(Thread.State.TERMINATED)) {
            return;
        }
        workThread = new Thread(() -> {
            mainWindow.disableButtons();
            while (mainQueue.size() != 0) {
                operationTable.disableButtons();
                isTransferActive = true;
                TransferItem item = mainQueue.poll();
                if (item.getOperation().equals(TransferItem.Operation.UPLOAD)) {
                    sendFileToServer(item);
                } else if (item.getOperation().equals(TransferItem.Operation.DOWNLOAD)) {
                    getFileFromServer(item, destDir -> mainWindow.refreshClientFilesList());
                }
            }
            mainWindow.enableButtons();
            mainWindow.refreshServerFilesList();
            isTransferActive = false;
            operationTable.enableButtons();
        });
        workThread.setDaemon(true);
        workThread.start();
    }

    private boolean checkQueueCapacity(int countOfElements) {
        return mainQueue.remainingCapacity() >= countOfElements;
    }


    public void getFileFromServer(TransferItem transferItem, Consumer<Path> callBack) {
        System.out.println("Начало скачивания файла " + transferItem.getSourcePath() + " с сервера");
        NetworkClient.getInstance().sendCommandToServer(new FileRequestCommand(transferItem.getSourcePath().toString()));
        try {
            final Path dstFilePath = Paths.get(transferItem.getDstPath().toString(), transferItem.getFileName());
            Files.createDirectories(dstFilePath.getParent());
            final FileOutputStream fileWriter = new FileOutputStream(dstFilePath.toFile(), true);
            operationTable.scrollToElement(transferItem);
            while (true) {
                final FileMessageCommand command = (FileMessageCommand) NetworkClient.getInstance().readCommandFromServer();
                final long totalPartsProgress = command.getPartsOfFile();
                final long actualPart = command.getPartNumber();

                System.out.println("Часть " + actualPart + " из " + totalPartsProgress);

                fileWriter.write(command.getData());
                transferItem.setProgressIndicator((double) actualPart / totalPartsProgress);
                if (command.getPartNumber() == command.getPartsOfFile()) {
                    break;
                }
            }
            System.out.printf("Файл %s успешно скачен с сервера%n", transferItem.getSourcePath());
            fileWriter.close();
            transferItem.setSuccess(true);
            transferItem.enableButtons();
            System.out.println("Путь обновления " + transferItem.getDstPath());
            callBack.accept(transferItem.getDstPath());
        } catch (IOException e) {
            transferItem.setSuccess(false);
            System.out.printf("Ошибка скачивания файла %s с сервера%n", transferItem.getSourcePath());
            e.printStackTrace();
        }
    }

    public void sendFileToServer(TransferItem transferItem) {
        System.out.println("Начало передачи файла " + transferItem.getSourcePath() + " на сервер");
        final long fileSize = transferItem.getSourcePath().toFile().length();
        long partsOfFile = fileSize / DEFAULT_BUFFER_SIZE;
        if (fileSize % DEFAULT_BUFFER_SIZE != 0 || fileSize == 0) {
            partsOfFile++;
        }
        System.out.println("Размер файла " + fileSize + " Количество частей " + partsOfFile);
        final long totalPartsProgress = partsOfFile;
        FileMessageCommand fileToServerCommand = new FileMessageCommand(
                transferItem.getSourcePath().getFileName().toString(),
                transferItem.getDstPath().toString(),
                fileSize,
                partsOfFile,
                0,
                new byte[DEFAULT_BUFFER_SIZE]
        );
        operationTable.scrollToElement(transferItem);
        try (FileInputStream fileReader = new FileInputStream(transferItem.getSourcePath().toFile())) {
            int readBytes;
            long partsSend = 0;
            do {
                readBytes = fileReader.read(fileToServerCommand.getData());
                fileToServerCommand.setPartNumber(partsSend + 1);
                if (readBytes < DEFAULT_BUFFER_SIZE) {
                    fileToServerCommand.setData(Arrays.copyOfRange(fileToServerCommand.getData(), 0, Math.max(readBytes, 0)));
                }
                NetworkClient.getInstance().sendCommandToServer(fileToServerCommand);
                partsSend++;
                final long actualPart = partsSend;
                System.out.println("Отправлена часть " + partsSend + " из " + partsOfFile);
                transferItem.setProgressIndicator((double) actualPart / totalPartsProgress);
            } while (partsSend != partsOfFile);
            System.out.printf("Файл %s успешно отправлен на сервер%n", transferItem.getSourcePath());
            transferItem.setSuccess(true);
            transferItem.enableButtons();
        } catch (IOException e) {
            transferItem.setSuccess(false);
            System.out.printf("Ошибка отправки файла %s на сервер%n", transferItem.getSourcePath());
            e.printStackTrace();
        }
    }
}
