package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDBConnection implements DBConnection {

    private final String dbDriver = "com.mysql.cj.jdbc.Driver";
    private final String dbUrl = "jdbc:mysql://localhost:3306/";
    private final String dbUsername = "root";
    private final String dbPassword = "gtr120519";
    private final String dbName = "cloud_users";

    private final String timeZoneConfiguration = "?serverTimezone=Europe/Moscow&useSSL=false";

    public MySQLDBConnection() {
        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка загрузки драйвера базы данных!");
            e.printStackTrace();
        }
    }

    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl + dbName + timeZoneConfiguration, dbUsername, dbPassword);
    }
}
