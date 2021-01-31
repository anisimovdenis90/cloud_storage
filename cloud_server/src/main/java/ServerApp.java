import netty.NetworkServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerApp {

    private static final int DEFAULT_PORT = 8189;
    private static final String PROPERTIES_FILE = "./server.properties";
    private static final String PROPERTIES_SERVER_PORT = "server.port";
    private static final Properties properties = new Properties();

    public static void main(String[] args) {
        int port;
        try (FileInputStream fis = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(fis);
            port = Integer.parseInt(properties.getProperty(PROPERTIES_SERVER_PORT));
        } catch (IOException | NumberFormatException e) {
            port = DEFAULT_PORT;
            e.printStackTrace();
        }
        new NetworkServer(port, properties).run();
    }
}
