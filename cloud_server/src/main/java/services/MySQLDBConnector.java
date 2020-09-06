package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

public class MySQLDBConnector implements DBConnector {

    private static final int CONNECTIONS_LIMIT = 10;

    private final Semaphore semaphore = new Semaphore(CONNECTIONS_LIMIT, true);
    private final String dbDriver = "com.mysql.cj.jdbc.Driver";
    private final String dbUrl = "jdbc:mysql://localhost:3306/";
    private final String dbUsername = "root";
    private final String dbPassword = "gtr120519";
    private final String dbName = "cloud_users";

    private final String timeZoneConfiguration = "?serverTimezone=Europe/Moscow&useSSL=false";

    public MySQLDBConnector() {
        try {
            Class.forName(dbDriver);
            System.out.println("Сервер базы данных запущен");
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка загрузки драйвера базы данных!");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            semaphore.acquire();
            return DriverManager.getConnection(dbUrl + dbName + timeZoneConfiguration, dbUsername, dbPassword);
        } catch (SQLException | InterruptedException e) {
            System.err.println("Ошибка создания подключения к базе данных!");
            semaphore.release();
            e.printStackTrace();
        }
        return null;
    }

    public void closeConnection(Connection connection) {
        try {
            if (connection != null) connection.close();
            semaphore.release();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия соединения с базой данных");
            e.printStackTrace();
        }
    }
}
