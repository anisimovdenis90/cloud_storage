package services;

import commands.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import util.FinishedCallBack;
import util.TransferItem;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;

public class NetworkClient {

    private static final String DEFAULT_SERVER_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 8189;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;

    private static NetworkClient instance;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;
    private Socket socket;
    private String userId;
    private volatile boolean connectionSuccess = false;

    private NetworkClient() {

    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String newUserId) {
        userId = newUserId;
    }

    public void start() {
        try {
            socket = new Socket(DEFAULT_SERVER_ADDRESS, DEFAULT_PORT);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(), 1024 * 1024 * 100);
            connectionSuccess = true;
            System.out.println("Соединение с сервером установленно");
        } catch (IOException e) {
            System.out.println("Ошибка связи с сервером.");
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T> void sendCommandToServer(T command) {
        try {
            out.writeObject(command);
        } catch (IOException e) {
            System.out.printf("Ошибка отправки команды %s на сервер%n", command.toString());
            e.printStackTrace();
        }
    }

    public void getFileFromServer(TransferItem transferItem, FinishedCallBack callBack) {
        sendCommandToServer(new FileRequestCommand(transferItem.getSourceFile().toString()));
        new Thread(() -> {
            try {
                FileOutputStream fileWriter = new FileOutputStream(transferItem.getDstFile() + "/" + transferItem.getSourceFile().getFileName(),
                        true
                );
                while (true) {
                    FileMessageCommand command = (FileMessageCommand) in.readObject();
                    final long totalPartsProgress = command.getPartsOfFile();
                    final long actualPart = command.getPartNumber();
                    fileWriter.write(command.getData());
                    Platform.runLater(() -> transferItem.setProgressIndicator((double) actualPart / totalPartsProgress));
                    if (command.getPartNumber() == command.getPartsOfFile()) {
                        break;
                    }
                }
                System.out.printf("Файл %s успешно скачен с сервера%n", transferItem.getSourceFile());
                fileWriter.close();
                transferItem.setSuccess(true);
                transferItem.enableButtons();
                callBack.call(transferItem.getDstFile());
            } catch (IOException | ClassNotFoundException e) {
                transferItem.setSuccess(false);
                System.out.printf("Ошибка скачивания файла %s с сервера%n", transferItem.getSourceFile());
                e.printStackTrace();
            }
        }).start();
    }

    public void sendFileToServer(TransferItem transferItem, Consumer<Path> callBack) {
        new Thread(() -> {
            System.out.println("Начало передачи файла " + transferItem.getSourceFile() + " на сервер");
            final long fileSize = transferItem.getSourceFile().toFile().length();
            long partsOfFile = fileSize / DEFAULT_BUFFER_SIZE;
            if (fileSize % DEFAULT_BUFFER_SIZE != 0) {
                partsOfFile++;
            }
            System.out.println("Размер файла " + fileSize + "  Количество частей " + partsOfFile);
            final long totalPartsProgress = partsOfFile;
            FileMessageCommand fileToServerCommand = new FileMessageCommand(
                    transferItem.getSourceFile().getFileName().toString(),
                    transferItem.getDstFile().toString(),
                    fileSize,
                    partsOfFile,
                    0,
                    new byte[DEFAULT_BUFFER_SIZE]
            );
            try (FileInputStream fileReader = new FileInputStream(transferItem.getSourceFile().toFile())) {
                int readBytes;
                long partsSend = 0;
                do {
                    readBytes = fileReader.read(fileToServerCommand.getData());
                    fileToServerCommand.setPartNumber(partsSend + 1);
                    if (readBytes < DEFAULT_BUFFER_SIZE) {
                        fileToServerCommand.setData(Arrays.copyOfRange(fileToServerCommand.getData(), 0, readBytes));
                    }
                    out.writeObject(fileToServerCommand);
                    partsSend++;
                    final long actualPart = partsSend;
                    Platform.runLater(() -> transferItem.setProgressIndicator((double) actualPart / totalPartsProgress));
                } while (partsSend != partsOfFile);
                System.out.printf("Файл %s успешно отправлен на сервер%n", transferItem.getSourceFile());
                transferItem.setSuccess(true);
                transferItem.enableButtons();
                callBack.accept(transferItem.getDstFile());
            } catch (IOException e) {
                transferItem.setSuccess(false);
                System.out.printf("Ошибка отправки файла %s на сервер%n", transferItem.getSourceFile());
                e.printStackTrace();
            }
        }).start();
    }

    public void deleteFileFromServer(Path fileName) {
        sendCommandToServer(new DeleteFileCommand(fileName.toString()));
    }

    public void renameFileOnServer(String oldFileName, String newFileName) {
        sendCommandToServer(new RenameFileCommand(oldFileName, newFileName));
    }

    public void createNewFolderOnServer(String currentServerDir, String newFolderName) {
        sendCommandToServer(new CreateFolderCommand(Paths.get(currentServerDir, newFolderName).toString()));
    }

    public Object readCommandFromServer() {
        try {
            return in.readObject();
        } catch (Exception e) {
            System.out.println("Ошибка чтения команды от сервера");
            e.printStackTrace();
        }
        return null;
    }
}
