import java.io.*;
import java.net.Socket;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8189;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int PARTS_OF_FILE_PROGRESS = 10;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private BufferedReader consoleReader;

    private String directoryName = "client";

    private String commands = "Доступные команды:" + System.lineSeparator() +
            "download [имя файла] - отправить файл на сервер;" + System.lineSeparator() +
            "upload [имя файла] - скачать файл с сервера;" + System.lineSeparator() +
            "getfiles server - получить список файлов на сервере;" + System.lineSeparator() +
            "end - завершение работы;" + System.lineSeparator() +
            "help или ? - вывод списка команд.";

    public static void main(String[] args) {
        new Client();
    }

    public Client() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Соединение с сервером установлено");
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            directoryName = directoryName + in.readUTF();
            createDirectory(directoryName);
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Ваша папка: " + directoryName);
            System.out.println(commands);
            runReadMessage(in);
            runSendMessage(out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void runReadMessage(DataInputStream in) {
        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("download")) {
                        final String[] data = message.split("\\s+");
                        getFileFromServer(data[1], Long.parseLong(data[2]));
                    } else if (message.startsWith("help") || message.startsWith("?")){
                        System.out.println(commands);
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runSendMessage(DataOutputStream out) {
        try {
            while (true) {
                String message = consoleReader.readLine();
                if (message.startsWith("upload ")) {
                    String fileName = message.split("\\s+")[1];
                    sendFileToServer(fileName);
                } else if (message.startsWith("download ")) {
                    String fileName = message.split("\\s+")[1];
                    sendMessage("download " + fileName);
                } else  {
                    sendMessage(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFileFromServer(String fileName, long fileSize) throws IOException {
        File file = new File("./" + directoryName + "/" + fileName);
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

    private void sendFileToServer(String fileName) throws IOException {
        final File file = new File("./" + directoryName + "/" + fileName);
        if (!file.exists()) {
            System.out.println("Указан неправильный файл: " + fileName);
            return;
        }
        final long fileSize = file.length();
        sendMessage("upload " + fileName + " " + fileSize);
        try (InputStream fileReader = new FileInputStream(file)) {
            final int count = (int) (fileSize / DEFAULT_BUFFER_SIZE) / PARTS_OF_FILE_PROGRESS;
            int readParts = 0;
            final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
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
            System.out.println("Файл " + fileName + " успешно отправлен на сервер");

        }

    }

    private void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDirectory(String dirName) throws IOException {
        File file = new File("./" + dirName);
        if (!file.exists()) {
            file.mkdir();
        }
    }
}
