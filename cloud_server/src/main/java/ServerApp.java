import netty.NetworkServer;

public class ServerApp {

    private static final int DEFAULT_PORT = 8189;
//    private static final Logger logger = Logger.getLogger(netty.NetworkServer.class);

    public static void main(String[] args) {
        int port = getServerPort(args);
        new NetworkServer(port).run();
    }

    private static int getServerPort(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Некорректный формат порта, использован порт по умолчанию");
            }
        }
        return port;
    }
}
