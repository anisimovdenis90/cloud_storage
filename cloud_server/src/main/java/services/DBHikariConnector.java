package services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBHikariConnector implements DBConnector {

    private final HikariDataSource dataSource;

    public DBHikariConnector(String propertyFile) {
        this.dataSource = new HikariDataSource(new HikariConfig(propertyFile));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void closeConnection(Connection connection) {
        // do nothing
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
