package services;

import util.FinishedCallBack;
import commands.DeleteFileCommand;
import commands.FileMessageCommand;
import commands.FileRequestCommand;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class NetworkClient {

    private static final String DEFAULT_SERVER_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 8189;
    private static final String CLIENT_DIR_PREFIX = "./client";
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 10;

    private static NetworkClient instance;
    private Socket socket;
    private String userId;
    private String clientDirectory;

    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    private NetworkClient() {

    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getClientDirectory() {
        return clientDirectory;
    }

    public void start() {
        try {
            socket = new Socket(DEFAULT_SERVER_ADDRESS, DEFAULT_PORT);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(), 1024 * 1024 * 100);
            System.out.println("Соединение с сервером установленно");
        } catch (IOException e) {
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
            System.out.println("Ошибка отправки команды на сервер");
            e.printStackTrace();
        }
    }

    public void getFileFromServer(String fileName, ProgressBar progressBar, FinishedCallBack callBack) {
        sendCommandToServer(new FileRequestCommand(fileName));
        new Thread(() -> {
            try {
                FileOutputStream fileWriter = new FileOutputStream(clientDirectory + "/" + fileName, true);
                while (true) {
                    FileMessageCommand command = (FileMessageCommand) in.readObject();
                    fileWriter.write(command.getData());
                    if (command.getPartNumber() == command.getPartsOfFile()) {
                        break;
                    }
                    Platform.runLater(() -> progressBar.setProgress((double) command.getPartNumber() / command.getPartsOfFile()));
                }
                System.out.println(String.format("Файл %s успешно скачен с сервера", fileName));
                Platform.runLater(() -> progressBar.setProgress(0.0));
                fileWriter.close();
                callBack.call();
            } catch (ClassNotFoundException e) {
                System.out.println("Получена неверная команда от сервера");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println(String.format("Ошибка скачивания файла %s с сервера", fileName));
                e.printStackTrace();
            }
        }).start();
    }

    public void sendFileToServer(String fileName, ProgressBar progressBar, FinishedCallBack callBack) {
        new Thread(() -> {
            final File file = new File(clientDirectory + "/" + fileName);
            int partsOfFile = (int) file.length() / DEFAULT_BUFFER_SIZE;
            if (file.length() % DEFAULT_BUFFER_SIZE != 0) {
                partsOfFile++;
            }
            final int totalPartsProgress = partsOfFile;
            FileMessageCommand fileToServer = new FileMessageCommand(
                    file.getName(),
                    file.length(),
                    partsOfFile,
                    0,
                    new byte[DEFAULT_BUFFER_SIZE]
            );
            try (FileInputStream fileReader = new FileInputStream(file)) {
                int readBytes;
                int partsSend = 0;
                for (int i = 0; i < partsOfFile; i++) {
                    readBytes = fileReader.read(fileToServer.getData());
                    fileToServer.setPartNumber(i + 1);
                    if (readBytes < DEFAULT_BUFFER_SIZE) {
                        fileToServer.setData(Arrays.copyOfRange(fileToServer.getData(), 0, readBytes));
                    }
                    out.writeObject(fileToServer);
                    partsSend++;
                    final int actualPart = partsSend;
                    Platform.runLater(() -> progressBar.setProgress((double) actualPart / totalPartsProgress));
                }
                System.out.println(String.format("Файл %s успешно отправлен на сервер", fileName));
                Platform.runLater(() -> progressBar.setProgress(0.0));
                callBack.call();
            } catch (IOException e) {
                System.out.println(String.format("Ошибка отправки файла %s на сервер", fileName));
                e.printStackTrace();
            }
        }) .start();
    }

    public void deleteFileFromServer(String fileName) {
        sendCommandToServer(new DeleteFileCommand(fileName));
    }

    public void createClientDir() {
        clientDirectory = CLIENT_DIR_PREFIX + userId;
        final Path clientFolder = Paths.get(clientDirectory);
        if (!Files.exists(clientFolder)) {
            try {
                Files.createDirectories(clientFolder);
            } catch (IOException e) {
                System.out.println("Ошибка создания папки клиента");
                e.printStackTrace();
            }
        }
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
