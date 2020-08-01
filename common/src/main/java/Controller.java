import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private static final int DEFAULT_BUFFER_SIZE = 1024;
    public TextField txt;
    public Button send;
    public ListView<String> lv;
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buffer;
    private String clientFilesPath = "./common/src/main/resources/clientFiles";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            buffer = new byte[DEFAULT_BUFFER_SIZE];
            String clientId = readMessageFromServer();
            clientFilesPath = clientFilesPath + "/" + clientId;
            File folder = new File(clientFilesPath);
            if (!folder.exists()) {
                folder.mkdirs();
            } else {
                if (folder.list().length >= 0) {
                    for (String file : folder.list()) {
                        lv.getItems().add(file);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(ActionEvent actionEvent) {
        String command = txt.getText().trim();
        String[] op = command.split(" ");
        try {
            if ("./download".equals(op[0])) {
                sendMessageToServer(command);
                String response = readMessageFromServer();
                if ("OK".equals(response)) {
                    System.out.println("Начало загрузки файла с сервера " + op[1]);
                    File file = new File(clientFilesPath + "/" + op[1]);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    getFileFromServer(file);
                    lv.getItems().add(op[1]);
                    System.out.println("Загрузка файла с сервера завершена");
                } else {
                    System.out.println("Файл отсутствует на сервере");
                }
            } else if ("./upload".equals(op[0])) {
                File file = new File(clientFilesPath + "/" + op[1]);
                if (!file.exists()) {
                    System.out.println("Указан неверный файл");
                    return;
                }
                sendMessageToServer(command);
                String response = readMessageFromServer();
                if ("OK".equals(response)) {
                    System.out.println("Начало отправки файла на сервер " + op[1]);
                    sendFileToServer(file);
                }
                System.out.println("Файл передан на сервер");
            } else {
                System.out.println("Неверная команда");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToServer(String message) throws IOException {
        byte[] byteMessage = message.getBytes();
        os.writeInt(byteMessage.length);
        os.write(byteMessage);
    }

    private String readMessageFromServer() throws IOException {
        int messageLength = is.readInt();
        byte[] byteMessage = new byte[messageLength];
        is.read(byteMessage,0, messageLength);
        String response = new String(byteMessage);
        System.out.println("Сообщение от сервера: " + response);
        return response;
    }

    private void getFileFromServer(File file) throws IOException {
        long fileSize = is.readLong();
        int totalBytes = 0;
        int readBytes;
        int lastBytes;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (fileSize < buffer.length) {
                is.read(buffer, 0, (int) fileSize);
                fos.write(buffer, 0, (int) fileSize);
            } else {
                while (totalBytes < fileSize) {
                    if ((lastBytes = (int) fileSize - totalBytes) < buffer.length) {
                        is.read(buffer, 0, lastBytes);
                        fos.write(buffer, 0, lastBytes);
                        break;
                    } else {
                        readBytes = is.read(buffer);
                        fos.write(buffer, 0, readBytes);
                        totalBytes += readBytes;
                    }
                }
            }
        }
    }

    private void sendFileToServer(File file) throws IOException {
        os.writeLong(file.length());
        try (FileInputStream fileReader = new FileInputStream(file)) {
            while (fileReader.available() > 0) {
                int count = fileReader.read(buffer);
                os.write(buffer, 0, count);
            }
        }
    }
}
