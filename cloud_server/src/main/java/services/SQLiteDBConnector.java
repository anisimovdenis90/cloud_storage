package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

public class SQLiteDBConnector implements DBConnector {

    private static final int CONNECTIONS_LIMIT = 5;

    private final Semaphore semaphore = new Semaphore(CONNECTIONS_LIMIT, true);
    private final String URL = "jdbc:sqlite:./cloud_storage_db.db";

    public SQLiteDBConnector() {
        System.out.println("Сервер базы данных запущен");
    }

    public Connection getConnection() {
        try {
            semaphore.acquire();
            return DriverManager.getConnection(URL);
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
