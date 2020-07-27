package IOSolution;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerIO {

    private boolean isRunning = true;

    public void stop() {
        isRunning = false;
    }

    public ServerIO() {
        try (ServerSocket server = new ServerSocket(8189)) {
            System.out.println("Server started!");
            while (isRunning) {
                Socket conn = server.accept();
                System.out.println("Client accepted");
                new Thread(new FileHandler(conn)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ServerIO();
    }
}
