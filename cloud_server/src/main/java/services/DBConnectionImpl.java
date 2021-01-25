package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnectionImpl implements DBConnection {

    private final String dbDriver;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;

    public DBConnectionImpl(Properties properties) {
        this.dbDriver = properties.getProperty("server.db.driver");
        this.dbUrl = properties.getProperty("server.db.url");
        this.dbUsername = properties.getProperty("server.db.username");
        this.dbPassword = properties.getProperty("server.db.password");
        if (dbDriver != null && !dbDriver.isEmpty()) {
            start();
        }
    }

    private void start() {
        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка загрузки драйвера базы данных!");
            e.printStackTrace();
        }
    }

    @Override
    public Connection createConnection() throws SQLException {
        if (dbUsername != null && !dbUsername.isEmpty()) {
            return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        }
        return DriverManager.getConnection(dbUrl);
    }
}
