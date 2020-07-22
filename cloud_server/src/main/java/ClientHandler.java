import java.io.*;
import java.net.Socket;

public class ClientHandler {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int PARTS_OF_FILE_PROGRESS = 10;

    private static int clientId = 0;
    private final Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;

    private String clientDirPrefix = "./server/client";
    private String clientDirectory;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        clientId++;
        System.out.println(String.format("Клиент № %d подключился", clientId));
        clientDirectory = clientDirPrefix + clientId;
    }

    public void start() {
        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            new Thread(() -> {
                try {
                    sendMessage(Integer.toString(clientId));
                    createDirectory(clientDirectory);
                    readMessages();
                } catch (IOException e) {
                    System.out.println("Ошибка соединения с клиентом");
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        try {
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("upload ")) {
                String[] data = message.split("\\s+");
                System.out.println("Запрос на загрузку файла от клиента № " + clientId + ": " + message);
                getFileFromClient(data[1], Long.parseLong(data[2]));
            } else if (message.startsWith("download ")) {
                System.out.println("Запрос на скачивание файла с сервера от клиента № " + clientId + ": " + message);
                sendFileToClient(message.split("\\s+")[1]);
            } else if (message.startsWith("getfiles")) {
                sendFilesList();
            } else if (message.startsWith("end")) {
                closeConnection();
                System.out.println("Клиент отключился");
            } else {
                sendMessage("Неправильная команда");
            }
        }
    }

    private void sendFilesList() throws IOException {
        File folder = new File(clientDirectory);
        String[] files = folder.list();
        if (files.length == 0) {
            sendMessage("Файлы на сервере отсутствуют");
            return;
        }
        sendMessage("Список файлов на сервере:");
        for (String file : files) {
            sendMessage(file);
        }
        sendMessage("Конец списка файлов");
    }

    private void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    private void getFileFromClient(String fileName, long fileSize) throws IOException {
        File file = new File(clientDirectory + "/" + fileName);
        file.createNewFile();
        System.out.println("Начало загрузки файла " + fileName);
        final double fileSizeByBuffer = (double) fileSize / DEFAULT_BUFFER_SIZE;
        final int counts = (int) Math.ceil(fileSizeByBuffer);
        final int temp = (int) fileSizeByBuffer;
        final int countsOfProgress = temp / PARTS_OF_FILE_PROGRESS;
        int readParts = 0;
        final int bytesOfLastPartOfFile;
        if (temp == 0) {
            bytesOfLastPartOfFile = (int) fileSize;
        } else {
            long longSize = fileSize - temp * DEFAULT_BUFFER_SIZE;
            bytesOfLastPartOfFile = (int) longSize;
        }
        try (FileOutputStream fileWriter = new FileOutputStream(file)) {
            final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            System.out.print("/");
            for (int i = 1; i <= counts; i++) {
                if (i < counts) {
                    in.read(buffer);
                    fileWriter.write(buffer);
                } else {
                    in.read(buffer, 0, bytesOfLastPartOfFile);
                    fileWriter.write(buffer, 0, bytesOfLastPartOfFile);
                }

                if (countsOfProgress != 0) {
                    readParts++;
                    if (readParts % countsOfProgress == 0) {
                        System.out.print("=");
                    }
                } else {
                    System.out.print("==========");
                }
            }
            System.out.println("/");
            System.out.println("Файл " + fileName + " успешно получен");
        }
    }

    private void sendFileToClient(String fileName) throws IOException {
        final File file = new File(clientDirectory + "/" + fileName);
        if (!file.exists()) {
            sendMessage("Указан несуществующий файл на сервере: " + fileName);
            System.out.println("Неправильное имя файла");
            return;
        }
        try (InputStream fileReader = new FileInputStream(file)) {
            final long size = file.length();
            final int count = (int) (size / DEFAULT_BUFFER_SIZE) / PARTS_OF_FILE_PROGRESS;
            int readParts = 0;
            final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            sendMessage("download " + file.getName() + " " + size);
            System.out.print("/");
            while (fileReader.available() > 0) {
                int readBytes = fileReader.read(buffer);
                if (count != 0) {
                    readParts++;
                    if (readParts % count == 0) {
                        System.out.print("=");
                    }
                } else {
                    System.out.print("==========");
                }
                out.write(buffer, 0, readBytes);
            }
            out.flush();
            System.out.println("/");
            System.out.println("Файл " + fileName + " успешно отправлен");
        }
    }

    private void createDirectory(String dirName) throws IOException {
        File file = new File(dirName);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
