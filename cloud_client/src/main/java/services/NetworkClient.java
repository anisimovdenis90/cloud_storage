package services;

import commands.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NetworkClient {

    private static final String DEFAULT_SERVER_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 8189;

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
