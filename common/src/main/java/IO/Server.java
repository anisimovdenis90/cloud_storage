package IO;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    DataInputStream is;
    DataOutputStream os;
    ServerSocket serverSocket;

    public Server() throws IOException {
        serverSocket = new ServerSocket(8189);
        Socket socket = serverSocket.accept();
        System.out.println("Client accepted");
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        String fileName = is.readUTF();
        System.out.println("filename" + fileName);
        File file = new File("./common/server/" + fileName);
        file.createNewFile();
        try (FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            while (true) {
                int r = is.read(buffer);
                if (r < 0) {
                    break;
                }
                os.write(buffer, 0, r);
            }
        }
        System.out.println("File uploaded");
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }
}
