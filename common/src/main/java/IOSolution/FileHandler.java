package IOSolution;

import java.io.*;
import java.net.Socket;

public class FileHandler implements Runnable {

    private String serverFilesPath = "./common/src/main/resources/serverFiles";
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private boolean isRunning = true;
    private static int cnt = 1;

    public FileHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
        String userName = "user" + cnt;
        cnt++;
        serverFilesPath += "/" + userName;
        File dir = new File(serverFilesPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                String command = is.readUTF();
                if (command.equals("./download")) {
                    String fileName = is.readUTF();
                    System.out.println("Find file with name:" + fileName);
                    File file = new File(serverFilesPath + "/" + fileName);
                    if (file.exists()) {
                        os.writeUTF("OK");
                        long len = file.length();
                        os.writeLong(len);
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        while (fis.available() > 0) {
                            int count = fis.read(buffer);
                            os.write(buffer, 0, count);
                        }
                    } else {
                        os.writeUTF("File not exists");
                    }
                } else {

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
