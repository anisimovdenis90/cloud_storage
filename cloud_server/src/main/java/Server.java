import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int DEFAULT_PORT = 8189;

    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT)) {
            System.out.println("Сервер запущен");
            while (true) {
                System.out.println("Ожидание подключения клиента");
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка сервера");
            e.printStackTrace();
        }
    }
}
