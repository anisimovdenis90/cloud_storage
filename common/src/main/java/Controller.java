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
    public TextField txt;
    public Button send;
    public ListView<String> lv;
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private String clientFilesPath = "./common/src/main/resources/clientFiles";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            String clientId = readMessageFromServer();
            clientFilesPath = clientFilesPath + "/" + clientId;
            File folder = new File(clientFilesPath);
            if (!folder.exists()) {
                folder.mkdirs();
            } else {
                if (folder.list().length >=0) {
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
        String command = txt.getText();
        String[] op = command.split(" ");
        try {
            if (op[0].equals("./download")) {
                os.write((op[0] + " " + op[1]).getBytes());
                String response = readMessageFromServer();
                System.out.println("Response " + response);
                if (response.equals("OK")) {
                    File file = new File(clientFilesPath + "/" + op[1]);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    long len = Long.parseLong(readMessageFromServer());
                    byte[] buffer = new byte[1024];
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        if (len < 1024) {
                            int count = is.read(buffer);
                            fos.write(buffer, 0, count);
                        } else {
                            for (int i = 0; i < (len / 1024) + 1; i++) {
                                int count = is.read(buffer);
                                fos.write(buffer, 0, count);
                            }
                        }
                    }
                    lv.getItems().add(op[1]);
                } else {
                    System.out.println("Файл отсутствует на сервере");
                }
            } else if (op[0].equals("./upload")) {
                File file = new File(clientFilesPath + "/" + op[1]);
                if (!file.exists()) {
                    System.out.println("Указан неверный файл");
                    return;
                }
                String fileSize = String.valueOf(file.length());
                os.write((op[0] + " " + op[1] + " " + fileSize).getBytes());
                String response = readMessageFromServer();
                if (response.equals("OK")) {
                    try (FileInputStream fileReader = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        while (fileReader.available() > 0) {
                            int count = fileReader.read(buffer);
                            os.write(buffer, 0, count);
                        }
                    }
                }
                System.out.println("Файл передан на сервер");
            } else {
                System.out.println("Неверная команда");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readMessageFromServer() throws IOException {
        byte[] buffer = new byte[1024];
        int count = is.read(buffer);
        String response = new String(buffer, 0, count);
        return response;
    }
}
