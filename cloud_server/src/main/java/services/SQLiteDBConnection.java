package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDBConnection implements DBConnection {

    private final String URL = "jdbc:sqlite:./cloud_storage_db.db";

    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
