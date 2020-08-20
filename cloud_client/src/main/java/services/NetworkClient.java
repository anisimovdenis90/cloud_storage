package services;

import commands.RenameFileCommand;
import controllers.AuthWindowsController;
import util.TransferItem;
import util.FinishedCallBack;
import commands.DeleteFileCommand;
import commands.FileMessageCommand;
import commands.FileRequestCommand;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;

public class NetworkClient {

    private static final String DEFAULT_SERVER_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 8189;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;

    private static NetworkClient instance;
    private Socket socket;
    private String userId;

    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;

    private volatile boolean connectionSuccess = false;

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    private NetworkClient() {

    }

    public void setUserId(String newUserId) {
        userId = newUserId;
    }

    public String getUserId() {
        return userId;
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

    public void getFileFromServer(Path sourcePath, Path destPath, TransferItem item, FinishedCallBack callBack) {
        sendCommandToServer(new FileRequestCommand(sourcePath.toString()));
        new Thread(() -> {
            try {
                FileOutputStream fileWriter = new FileOutputStream(destPath.toString() + "/" + sourcePath.getFileName().toString(), true);
                while (true) {
                    FileMessageCommand command = (FileMessageCommand) in.readObject();
                    final int totalPartsProgress = command.getPartsOfFile();
                    final int actualPart = command.getPartNumber();
                    fileWriter.write(command.getData());
                    Platform.runLater(() -> item.setProgressIndicator((double) actualPart / totalPartsProgress));
                    if (command.getPartNumber() == command.getPartsOfFile()) {
                        break;
                    }
                }
                System.out.printf("Файл %s успешно скачен с сервера%n", sourcePath);
                fileWriter.close();
                item.setSuccess(true);
                callBack.call(destPath);
            } catch (IOException | ClassNotFoundException e) {
                item.setSuccess(false);
                System.out.printf("Ошибка скачивания файла %s с сервера%n", sourcePath);
                e.printStackTrace();
            }
        }).start();
    }

    public void sendFileToServer(Path sourcePath, Path destPath, TransferItem item, FinishedCallBack callBack) {
        new Thread(() -> {
            final long fileSize = sourcePath.toFile().length();
            int partsOfFile = (int) fileSize / DEFAULT_BUFFER_SIZE;
            if (fileSize % DEFAULT_BUFFER_SIZE != 0) {
                partsOfFile++;
            }
            final int totalPartsProgress = partsOfFile;
            FileMessageCommand fileToServerCommand = new FileMessageCommand(
                    sourcePath.getFileName().toString(),
                    destPath.toString(),
                    fileSize,
                    partsOfFile,
                    0,
                    new byte[DEFAULT_BUFFER_SIZE]
            );
            try (FileInputStream fileReader = new FileInputStream(sourcePath.toFile())) {
                int readBytes;
                int partsSend = 0;
                for (int i = 0; i < partsOfFile; i++) {
                    readBytes = fileReader.read(fileToServerCommand.getData());
                    fileToServerCommand.setPartNumber(i + 1);
                    if (readBytes < DEFAULT_BUFFER_SIZE) {
                        fileToServerCommand.setData(Arrays.copyOfRange(fileToServerCommand.getData(), 0, readBytes));
                    }
                    out.writeObject(fileToServerCommand);
                    partsSend++;
                    final int actualPart = partsSend;
                    Platform.runLater(() -> item.setProgressIndicator((double) actualPart / totalPartsProgress));
                }
                System.out.printf("Файл %s успешно отправлен на сервер%n", sourcePath);
                item.setSuccess(true);
                callBack.call(destPath);
            } catch (IOException e) {
                item.setSuccess(false);
                System.out.printf("Ошибка отправки файла %s на сервер%n", sourcePath);
                e.printStackTrace();
            }
        }) .start();
    }

    public void deleteFileFromServer(Path fileName) {
        sendCommandToServer(new DeleteFileCommand(fileName.toString()));
    }

    public void renameFileOnServer(String oldFileName, String newFileName) {
        sendCommandToServer(new RenameFileCommand(oldFileName, newFileName));
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
