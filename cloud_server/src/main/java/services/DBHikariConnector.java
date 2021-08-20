package services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBHikariConnector implements DBConnector {

    private final HikariDataSource dataSource;

    public DBHikariConnector(Properties properties) {
        this.dataSource = new HikariDataSource(createConfig(properties));
    }

    private HikariConfig createConfig(Properties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getProperty(DBPropertyNames.URL.name));
        config.setUsername(properties.getProperty(DBPropertyNames.USERNAME.name));
        config.setPassword(properties.getProperty(DBPropertyNames.PASSWORD.name));
        config.setMaximumPoolSize(Integer.parseInt(properties.getProperty(DBPropertyNames.MAXIMUM_POOL_SIZE.name, "5")));
        return config;
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
