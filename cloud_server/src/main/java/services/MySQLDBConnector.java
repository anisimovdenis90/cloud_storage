package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDBConnector implements DBConnector {

    private final String dbDriver = "com.mysql.cj.jdbc.Driver";
    private final String dbUrl = "jdbc:mysql://localhost:3306/";
    private final String dbUsername = "root";
    private final String dbPassword = "gtr120519";
    private final String dbName = "cloud_users";

    private final String timeZoneConfiguration = "?serverTimezone=Europe/Moscow&useSSL=false";

    public void start() {
        try {
            Class.forName(dbDriver);
            System.out.println("Сервер авторизации запущен");
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка загрузки драйвера базы данных!");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(dbUrl + dbName + timeZoneConfiguration, dbUsername, dbPassword);
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных!");
            e.printStackTrace();
        }
        return null;
    }

    public void closeConnection(Connection connection) {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия соединения с базой данных");
            e.printStackTrace();
        }
    }
}
