import netty.NetworkServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerApp {

    private static final String PROPERTIES_FILE = "./server.properties";

    private static final Properties properties = new Properties();

    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(fis);
            new NetworkServer(properties).run();
        } catch (IOException e) {
            System.err.println("Запуск сервера невозможен: ошибка чтения файла конфигурации: " + PROPERTIES_FILE);
            e.printStackTrace();
        }
    }
}
